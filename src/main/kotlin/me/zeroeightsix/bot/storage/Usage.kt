package me.zeroeightsix.bot.storage

import me.zeroeightsix.bot.MemberID
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long

object Usages : Table<Usage>("usages"), TableCreate {
    val memberId = long("member_id").primaryKey().bindTo { it.memberId }
    val commandUsages = int("command_usages").bindTo { it.commandUsages }

    override val tableDefinition: String = """
            member_id      bigint auto_increment
                primary key,
            command_usages int default 0 not null
    """.trimIndent()
}

interface Usage : Entity<Usage> {
    companion object : Entity.Factory<Usage>()

    val memberId: MemberID
    val commandUsages: Int
}

val Database.usages
    get() = this.sequenceOf(Usages)