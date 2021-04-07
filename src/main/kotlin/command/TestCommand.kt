@file:Suppress("unused")

package command

import dev.minn.jda.ktx.await

object TestCommand {

    suspend fun execute(ctx: SlashCommandContext, toParrot: String) {
        ctx.event.reply(toParrot).await()
    }

}