package de.alxgrk.monitoring

import io.ktor.application.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.newrelic.NewRelicConfig
import io.micrometer.newrelic.NewRelicMeterRegistry

object NewRelic {

    lateinit var registry: NewRelicMeterRegistry

    fun Application.createGlobalRegistry() {
        registry = NewRelicMeterRegistry(
            object : NewRelicConfig {
                override fun accountId() =
                    environment.config.propertyOrNull("ktor.environment.newRelicAccountId")?.getString()

                override fun apiKey() =
                    environment.config.propertyOrNull("ktor.environment.newRelicInsertKey")?.getString()

                override fun get(k: String?) = null // accept the rest of the defaults
            },
            Clock.SYSTEM
        )
        Metrics.addRegistry(registry)
    }
}
