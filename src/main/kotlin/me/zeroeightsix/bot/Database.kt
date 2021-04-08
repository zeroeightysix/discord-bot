package me.zeroeightsix.bot

import org.ktorm.database.Database

lateinit var database: Database
    private set

fun connectDatabase(user: String, password: String) {
    database = Database.connect(
        "jdbc:mysql://localhost:3306/test", driver = "com.mysql.jdbc.Driver",
        user, password
    )
}

inline fun <T> transaction(transaction: Database.() -> T): T = database.run(transaction)