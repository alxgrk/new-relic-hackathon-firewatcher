package de.alxgrk.input

import de.alxgrk.input.Sources.Companion.parse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class ActiveFireScheduler(
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : AutoCloseable {

    fun scheduleAcquisition() {
        executor.scheduleAtFixedRate(this::fetchCsvData, 0, 1, TimeUnit.DAYS)
    }

    private fun fetchCsvData() {
        logger.info { "Running scheduled active fire acquisition." }

        try {
            // we are on a thread from executor
            HttpClient(CIO).use { client ->
                runBlocking(Dispatchers.IO) {

                    val fires = Sources.values()
                        .map { source ->
                            async {
                                val channel = client.get<HttpResponse>(source.url).content
                                val entries = mutableListOf<Pair<Sources.Coordinate, Sources.ConfidenceLevel>>()
                                while (!channel.isClosedForRead) {
                                    val line = channel.readUTF8Line() ?: break
                                    if (line.startsWith("lat"))
                                        continue

                                    entries += parse(line)
                                }
                                entries
                            }
                        }
                        .awaitAll()
                        .flatten()
                        .toMap()
                    ActiveFires.store(fires)
                }
            }
        } catch (e: Exception) {
            logger.error { "Couldn't retrieve CSV data - will try it again in 5 minutes." }
            Thread.sleep(5 * 60 * 1000)
            fetchCsvData()
        }

        logger.info { "Finished acquiring fire data: ${ActiveFires.size()} fires in total." }
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
