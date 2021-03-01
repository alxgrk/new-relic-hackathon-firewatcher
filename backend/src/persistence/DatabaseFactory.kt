package persistence

import de.alxgrk.monitoring.NewRelic
import de.alxgrk.persistence.PushSubscriptionRepo
import de.alxgrk.persistence.PushSubscriptionTable
import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

object DatabaseFactory {

    fun Application.init() {
        val jdbcUrl = environment.config.propertyOrNull("ktor.environment.jdbcDatabaseUrl")?.getString()
            ?: throw RuntimeException("Couldn't load JDBC_DATABASE_URL")
        Database.connect({ DriverManager.getConnection(jdbcUrl) })
        transaction {
            SchemaUtils.create(FireDataTable, PushSubscriptionTable)

            NewRelic.registry.gauge("initialFiresInDb", FireDataRepo().count())
            NewRelic.registry.gauge("initialSubscriptionsInDb", PushSubscriptionRepo().count())
        }
    }
}
