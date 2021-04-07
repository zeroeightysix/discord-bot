package me.zeroeightsix.bot.storage

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Usage : LongIdTable(name = "usage", columnName = "user_id") {
    
    val commandUsages = integer("command_usages").default(0)
    
}

class UsageEntry(userId: EntityID<Long>) : LongEntity(userId) {
    companion object : LongEntityClass<UsageEntry>(Usage)

    var commandUsages by Usage.commandUsages
}