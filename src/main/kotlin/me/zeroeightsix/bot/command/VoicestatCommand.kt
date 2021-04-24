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
        suspend fun CommandContext.execute(channel: AbstractChannel?) {
            val memberId = event.member?.idLong ?: return

            val channel: VoiceChannel? = TypeConstraint().optional(channel) {
                return reply(translate("must_supply_vc").err)
            }

            val times = (if (channel == null) VoiceTracker.getTimes(memberId) else
                mapOf(channel.idLong to VoiceTracker.getTime(memberId, channel.idLong)).filterValues { it != null })

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

                event.reply(times.entries.joinToString("\n") {
                    val channelName = jda.getVoiceChannelById(it.key)?.name ?: translate("unknown_channel")
                    "$channelName: ${Duration.ofMillis(it.value!!).humanReadable}"
                })
            } else {
                event.reply((channel?.let { translate("not_in_vc_before", it.name) }
                    ?: translate("not_in_any_vc_before")).err)
            }.setEphemeral(true).await()
        }
    }

}