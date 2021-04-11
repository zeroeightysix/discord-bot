@file:Suppress("unused")

package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.humanReadable
import me.zeroeightsix.bot.jda
import me.zeroeightsix.bot.service.VoiceTracker
import net.dv8tion.jda.api.entities.AbstractChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import java.time.Duration

object VoicestatCommand {

    object Me {
        suspend fun execute(ctx: CommandContext, channel: AbstractChannel?) {
            val memberId = ctx.event.member?.idLong ?: return

            val channel = channel?.let {
                (it as? VoiceChannel) ?: run {
                    ctx.event.reply("You must supply a voice channel.", ephemeral = true).await()
                    return
                }
            }

            val times =
                (if (channel == null) listOf(VoiceTracker.databaseGetTime(memberId))
                else VoiceTracker.databaseGetTime(memberId, channel.idLong)).filterNotNull()

            if (times.isNotEmpty()) {
                // TODO: Embeds can not be ephemeral.
//                ctx.event.reply(Embed {
//                    title = "Voice channel times"
//                    times.forEach {
//                        field {
//                            inline = true
//                            title = jda.getGuildChannelById(it.channelId)?.name ?: "Unknown channel"
//                            value = "${(it.timeSpent / 1000)}s"
//                        }
//                    }
//                })

                ctx.event.reply(times.joinToString("\n") {
                    val channelName = jda.getVoiceChannelById(it.channelId)?.name ?: "Unknown channel"
                    "$channelName: ${Duration.ofMillis(it.timeSpent).humanReadable}"
                })
            } else {
                ctx.event.reply("I haven't seen you in a voice channel before.")
            }.setEphemeral(true).await()
        }
    }

}