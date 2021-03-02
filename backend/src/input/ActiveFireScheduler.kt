package de.alxgrk.input

import com.oripwk.micrometer.kotlin.coTimer
import de.alxgrk.input.Sources.Companion.parse
import de.alxgrk.monitoring.NewRelic
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.micrometer.core.instrument.ImmutableTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class ActiveFireScheduler(
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : AutoCloseable {

    fun scheduleAcquisition() {
        executor.scheduleAtFixedRate(this::fetchCsvData, 0, 1, TimeUnit.DAYS)
    }

    private fun fetchCsvData() {
        NewRelic.registry.timer("fetchCsvData").record {
            logger.info { "Running scheduled active fire acquisition." }

            try {
                // we are on a thread from executor
                HttpClient(CIO).use { client ->
                    runBlocking(Dispatchers.IO) {
                        supervisorScope {
                            val fires = Sources.values()
                                .map { source ->
                                    async {
                                        val response = client.get<HttpResponse>(source.url)
                                        NewRelic.registry.gauge(
                                            "sourceSize",
                                            listOf(ImmutableTag("source", source.name)),
                                            response.contentLength() ?: -1
                                        )

                                        val channel = response.content
                                        val entries = mutableMapOf<Sources.Coordinate, Sources.ConfidenceLevel>()

                                        NewRelic.registry.coTimer("parsingTime", "source", source.name).record {
                                            while (!channel.isClosedForRead) {
                                                val line = channel.readUTF8Line() ?: break
                                                if (line.startsWith("lat"))
                                                    continue

                                                val (coord, confidenceLevel) = parse(line)
                                                entries[coord] = confidenceLevel
                                            }
                                        }
                                        source to entries
                                    }
                                }
                                .mapNotNull { def ->
                                    try {
                                        def.await()
                                    } catch (e: Exception) {
                                        logger.error(e) { "Couldn't retrieve CSV data for one source - continuing." }
                                        null
                                    }
                                }
                                .toMap()

                            fires.entries.forEach { (source, map) ->
                                NewRelic.registry.gauge(
                                    "onlineSources",
                                    listOf(ImmutableTag("source", source.name)),
                                    1
                                )
                                NewRelic.registry.gauge(
                                    "dataPointsPerSource",
                                    listOf(ImmutableTag("source", source.name)),
                                    map.size
                                )
                            }

                            if (fires.isEmpty())
                                throw RuntimeException("No source was reachable, so now fire data could be provided.")

                            ActiveFires.store(fires)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Couldn't retrieve CSV data - will try it again in 5 minutes." }
                Thread.sleep(5 * 60 * 1000)
                fetchCsvData()
            }

            logger.info { "Finished acquiring fire data: ${ActiveFires.size()} fires in total." }
        }
    }

    override fun close() {
        executor.shutdownNow()

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.warn(e) { "Swallowing exception on ActiveFireScheduler closing." }
        }
    }
}
