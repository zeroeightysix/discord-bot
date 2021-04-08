package me.zeroeightsix.bot.service

import dev.minn.jda.ktx.listener
import me.zeroeightsix.bot.MemberID
import me.zeroeightsix.bot.database
import me.zeroeightsix.bot.jda
import me.zeroeightsix.bot.storage.VoiceChatTimes
import me.zeroeightsix.bot.storage.voiceChatTimes
import me.zeroeightsix.bot.transaction
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.plus
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.support.mysql.insertOrUpdate

object VoiceChatTracker {

    private val userTimeMap = mutableMapOf<MemberID, Long>()

    init {
        jda.listener<GuildVoiceUpdateEvent> { event ->
            println(event)

            val memberId = event.entity.idLong
            if (event.channelJoined == null) {
                // User left
                val channelId = event.channelLeft?.idLong ?: return@listener

                flushTime(memberId, channelId)
            } else {
                event.channelLeft?.let { channelLeft ->
                    // left is nonnull AND joined is nonnull -> member moved channels

                    flushTime(memberId, channelLeft.idLong)
                }

                userTimeMap[memberId] = System.currentTimeMillis()
            }
        }
    }

    fun databaseGetTime(member: MemberID) = database.voiceChatTimes.find { it.memberId eq member }

    fun databaseGetTime(member: MemberID, channelId: Long) =
        database.voiceChatTimes.filter {
            (it.memberId eq member) and (it.channelId eq channelId)
        }.toList()

    private fun flushTime(memberId: Long, channelId: Long) {
        val temp = userTimeMap.remove(memberId)
        println("flush: $memberId - $temp")
        val timeJoined = temp ?: return
        val timeSpent = System.currentTimeMillis() - timeJoined

        transaction {
            insertOrUpdate(VoiceChatTimes) {
                set(it.memberId, memberId)
                set(it.channelId, channelId)
                set(it.timeSpent, timeSpent)
                onDuplicateKey {
                    set(it.timeSpent, it.timeSpent + timeSpent)
                }
            }
        }
    }

}