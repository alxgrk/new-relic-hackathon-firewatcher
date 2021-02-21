package de.alxgrk.input

import org.apache.lucene.util.SloppyMath
import java.math.BigDecimal

enum class Sources(val url: String) {
    MODIS("https://firms.modaps.eosdis.nasa.gov/data/active_fire/c6/csv/MODIS_C6_Global_24h.csv"),
    VIIRS_SNPP("https://firms.modaps.eosdis.nasa.gov/data/active_fire/suomi-npp-viirs-c2/csv/SUOMI_VIIRS_C2_Global_24h.csv"),
    VIIRS_NOAH20("https://firms.modaps.eosdis.nasa.gov/data/active_fire/noaa-20-viirs-c2/csv/J1_VIIRS_C2_Global_24h.csv");

    data class Coordinate(val latitude: BigDecimal, val longitude: BigDecimal) {

        init {
            require(latitude in BigDecimal.valueOf(-90)..BigDecimal.valueOf(90)) { "latitude must lie between -90 and 90" }
            require(longitude in BigDecimal.valueOf(-180)..BigDecimal.valueOf(180)) { "latitude must lie between -90 and 90" }
        }

        fun haversinDistance(other: Coordinate): BigDecimal =
            SloppyMath.haversinMeters(
                this.latitude.toDouble(),
                this.longitude.toDouble(),
                other.latitude.toDouble(),
                other.longitude.toDouble()
            ).toBigDecimal()
    }

    enum class ConfidenceLevel {
        LOW, NOMINAL, HIGH
    }

    companion object {
        fun parse(line: String): Pair<Coordinate, ConfidenceLevel> {
            val values = line.split(',')
            val latitude = values[0].toBigDecimal()
            val longitude = values[1].toBigDecimal()
            val confidenceLevel = values[8].let {
                when (it) {
                    "low" -> ConfidenceLevel.LOW
                    "nominal" -> ConfidenceLevel.NOMINAL
                    "high" -> ConfidenceLevel.HIGH
                    else -> null
                }
                    ?: when (it.toInt()) {
                        in 0..33 -> ConfidenceLevel.LOW
                        in 34..66 -> ConfidenceLevel.NOMINAL
                        else -> ConfidenceLevel.HIGH
                    }
            }

            return Coordinate(latitude, longitude) to confidenceLevel
        }
    }
}
