package persistence

import de.alxgrk.input.Sources
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class FireDataRepo {

    fun create(fireData: FireData) {
        transaction {
            if (FireDataTable.select { whereCoordinate(fireData.coordinate) }.empty()) {
                FireDataTable.insert {
                    it[this.fireDataSource] = fireData.source
                    it[this.latitude] = fireData.coordinate.latitude
                    it[this.longitude] = fireData.coordinate.longitude
                    it[this.confidenceLevel] = fireData.confidenceLevel
                }
            }
        }
    }

    fun get(coordinate: Sources.Coordinate): FireData? = transaction {
        FireDataTable.select { whereCoordinate(coordinate) }
            .map {
                it.toFireData()
            }.firstOrNull()
    }

    fun getAll(): List<FireData> = transaction {
        FireDataTable.selectAll().map { it.toFireData() }
    }

    fun getAllCoordinates(): List<Sources.Coordinate> = transaction {
        FireDataTable.selectAll().map { it.toCoordinate() }
    }

    fun removeAll(firesRemoved: Set<Sources.Coordinate>) {
        transaction {
            firesRemoved.forEach {
                FireDataTable.deleteWhere { whereCoordinate(it) }
            }
        }
    }

    fun count(): Long = transaction {
        FireDataTable.slice(FireDataTable.id.countDistinct()).selectAll().count()
    }

    private fun SqlExpressionBuilder.whereCoordinate(coordinate: Sources.Coordinate) =
        (FireDataTable.latitude eq coordinate.latitude) and (FireDataTable.longitude eq coordinate.longitude)

    private fun ResultRow.toFireData(): FireData {
        return FireData(
            this[FireDataTable.fireDataSource],
            toCoordinate(),
            this[FireDataTable.confidenceLevel]
        )
    }

    private fun ResultRow.toCoordinate() =
        Sources.Coordinate(this[FireDataTable.latitude], this[FireDataTable.longitude])
}

data class FireData(
    val source: Sources,
    val coordinate: Sources.Coordinate,
    val confidenceLevel: Sources.ConfidenceLevel
)
