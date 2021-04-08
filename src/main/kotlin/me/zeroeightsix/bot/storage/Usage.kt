package me.zeroeightsix.bot.storage

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long

object Usages : Table<Usage>("usages") {
    val memberId = long("member_id").primaryKey().bindTo { it.memberId }
    val commandUsages = int("command_usages").bindTo { it.commandUsages }
}

interface Usage : Entity<Usage> {
    companion object : Entity.Factory<Usage>()

    val memberId: Long
    val commandUsages: Int
}

val Database.usages
    get() = this.sequenceOf(Usages)