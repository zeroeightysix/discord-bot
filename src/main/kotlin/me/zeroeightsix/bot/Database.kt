package me.zeroeightsix.bot

import org.ktorm.database.Database
import org.ktorm.dsl.QuerySource
import org.ktorm.dsl.from
import org.ktorm.schema.BaseTable

lateinit var database: Database
    private set

fun connectDatabase(user: String, password: String) {
    database = Database.connect(
        "jdbc:mysql://localhost:3306/test", driver = "com.mysql.jdbc.Driver",
        user, password
    )
}

inline fun <T> database(transaction: Database.() -> T): T = database.run(transaction)

inline fun <T> table(table: BaseTable<*>, transaction: QuerySource.() -> T) =
    database { from(table).run(transaction) }