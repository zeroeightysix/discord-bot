package me.zeroeightsix.bot.storage

import me.zeroeightsix.bot.database
import org.ktorm.schema.Table

interface TableCreate {
    /** Everything between the parentheses in a table creation query. */
    val tableDefinition: String

    fun createIfNotExistsQuery(tableName: String) =
        "create table if not exists $tableName\n($tableDefinition);"
}

fun createTablesIfNotExist() =
    createTablesIfNotExist(listOf(Coins, Usages, VoiceChatTimes, LastClaims))

// figure you'd change this to a vararg?
// yeah, right, kotlinc
private fun <T> createTablesIfNotExist(tables: List<T>)
        where T : Table<*>, T : TableCreate {
    for (table in tables) {
        val query = table.createIfNotExistsQuery(table.tableName)

        database.useConnection { conn ->
            conn.prepareStatement(query).execute()
        }
    }
}