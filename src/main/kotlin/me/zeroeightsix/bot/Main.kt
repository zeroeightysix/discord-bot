import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import me.zeroeightsix.bot.command.SlashCommandContext
import me.zeroeightsix.bot.command.XMLCommandLoader
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*

const val TOKEN_VAR_NAME = "DISCORD_TOKEN"

private val logger = KotlinLogging.logger {}

suspend fun main() {
    // Load commands
    logger.info { "Starting up" }

    val token = System.getenv(TOKEN_VAR_NAME) ?: return logger.error {
        "Failed to start bot. DISCORD_TOKEN environment variable is not configured!"
    }

    val testingGuildId = System.getenv("GUILD_ID") ?: return logger.error {
        "Failed to start bot. GUILD_ID environment variable is not configured!"
    }

    val (executor, commands) = (object {}).javaClass.getResourceAsStream("/commands.xml").use { stream ->
        val handler = XMLCommandLoader.load(stream)
        logger.info { "Loaded ${handler.commandCount} commands" }

        handler.createExecutor<SlashCommandContext>("me.zeroeightsix.bot.command") to handler.buildCommands()
    }

    val jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent::class.java))
        .injectKTX()
        .build()

    jda.listener<ReadyEvent> {
        jda.getGuildById(testingGuildId.toLong())!!.updateCommands().run {
            addCommands(commands)
            await()
        }
    }

    jda.listener<SlashCommandEvent> { event ->
        executor.execute(SlashCommandContext(event)).failure?.let {
            logger.error { "Failed to execute me.zeroeightsix.bot.command: $it (event: $event)" }

            // No reply to discord: this way the user gets a pretty error message from discord itself.
        }
    }
}