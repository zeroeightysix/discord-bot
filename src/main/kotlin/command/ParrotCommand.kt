@file:Suppress("unused")

package command

import dev.minn.jda.ktx.await

object ParrotCommand {

    object Once {
        suspend fun execute(ctx: SlashCommandContext, toParrot: String) {
            ctx.event.reply(toParrot).await()
        }
    }
    
    object Double {
        object Overload {
            suspend fun execute(ctx: SlashCommandContext, toParrot: String) {
                ctx.event.reply("$toParrot $toParrot").await()
            }
        }
    }

}