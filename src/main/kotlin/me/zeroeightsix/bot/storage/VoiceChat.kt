package me.zeroeightsix.bot.storage

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long

object VoiceChatTimes : Table<VoiceChatTime>("voice_chat_time") {
    val memberId = long("member_id").primaryKey().bindTo { it.memberId }
    val channelId = long("channel_id").primaryKey().bindTo { it.channelId }
    
    val timeSpent = long("time_spent").bindTo { it.timeSpent }
}

interface VoiceChatTime : Entity<VoiceChatTime> {
    companion object : Entity.Factory<VoiceChatTime>()
    
    val memberId: Long
    val channelId: Long
    val timeSpent: Long
}

val Database.voiceChatTimes
    get() = this.sequenceOf(VoiceChatTimes)