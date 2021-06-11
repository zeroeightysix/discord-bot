@file:Suppress("unused", "NAME_SHADOWING")

package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.util.humanReadable
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
                event.reply(translate("must_supply_vc").err).await()
                return
            }

            val times = (if (channel == null) VoiceTracker.getTimes(memberId) else
                mapOf(channel.idLong to VoiceTracker.getTime(memberId, channel.idLong)).filterValues { it != null })

            if (times.isNotEmpty()) {
                event.replyEmbeds(Embed {
                    title = "Voice channel times"
                    times.forEach {
                        field {
                            inline = true
                            title = jda.getGuildChannelById(it.key)?.name ?: "Unknown channel"
                            value = (Duration.ofMillis(it.value ?: 0).humanReadable)
                        }
                    }
                })
            } else {
                event.reply((channel?.let { translate("not_in_vc_before", it.name) }
                    ?: translate("not_in_any_vc_before")).err)
            }.setEphemeral(true).await()
        }
    }

}