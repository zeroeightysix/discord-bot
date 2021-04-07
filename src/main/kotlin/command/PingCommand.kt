package command

import dev.minn.jda.ktx.await

object PingCommand {

    suspend fun execute(ctx: SlashCommandContext, ephemeral: Boolean?) {
        val ephemeral = ephemeral ?: false

        ctx.event.hook.setEphemeral(ephemeral)
        ctx.event.reply("Pong!").await()
    }
    
}