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
import java.time.Duration
import java.time.Instant

object VoiceTracker {

    private val userTimeMap = mutableMapOf<MemberID, Instant>()

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

                userTimeMap[memberId] = Instant.now()
            }
        }
    }

    // TODO: Cache these? We can easily invalidate cache (or even just update) on every flush.
    fun databaseGetTime(member: MemberID) = database.voiceChatTimes.find { it.memberId eq member }

    fun databaseGetTime(member: MemberID, channelId: Long) =
        database.voiceChatTimes.filter {
            (it.memberId eq member) and (it.channelId eq channelId)
        }.toList()

    private fun flushTime(memberId: Long, channelId: Long) {
        val timeJoined = userTimeMap.remove(memberId) ?: return
        val timeSpent = Duration.between(timeJoined, Instant.now())
        val timeSpentMillis = timeSpent.toMillis()

        transaction {
            insertOrUpdate(VoiceChatTimes) {
                set(it.memberId, memberId)
                set(it.channelId, channelId)
                set(it.timeSpent, timeSpentMillis)
                onDuplicateKey {
                    set(it.timeSpent, it.timeSpent + timeSpentMillis)
                }
            }
        }
    }

}