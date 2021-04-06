import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.lang.RuntimeException

const val TOKEN_VAR_NAME = "DISCORD_TOKEN"

private val logger = KotlinLogging.logger {}

fun main() {
    val token = System.getenv(TOKEN_VAR_NAME)
        ?: throw RuntimeException("$TOKEN_VAR_NAME environment variable not specified!").also {
            logger.error(it) { "Failed to start bot" }
        }

    val jda = JDABuilder.createLight(token)
        .injectKTX()
        .build()

    jda.listener<MessageReceivedEvent> { event ->
        val member = event.member ?: return@listener

        println("${event.guild.name} ~ ${member.effectiveName}: ${event.message.contentRaw}")
    }
}

