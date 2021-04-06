import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Command
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction
import java.util.*

const val TOKEN_VAR_NAME = "DISCORD_TOKEN"

private val logger = KotlinLogging.logger {}

suspend fun main() {
    val token = System.getenv(TOKEN_VAR_NAME) ?: return logger.error {
         "Failed to start bot. DISCORD_TOKEN environment variable is not configured!"
    }
    
    val testingGuildId = System.getenv("GUILD_ID") ?: return logger.error {
        "Failed to start bot. GUILD_ID environment variable is not configured!"
    }

    val jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent::class.java))
        .injectKTX()
        .build()

    jda.listener<ReadyEvent> {
        jda.getGuildById(testingGuildId.toLong())!!.updateCommands().run {
            addCommands(CommandUpdateAction.CommandData("test", "Testing command!")
                .addOption(CommandUpdateAction.OptionData(Command.OptionType.STRING, "to_echo", "The text to echo").setRequired(true)))

            await()
        }
    }

    jda.listener(::onSlashCommand)
}

suspend fun onSlashCommand(event: SlashCommandEvent) {
    event.guild ?: return

    when (event.name) {
        "test" -> {
            event.reply("Parroting: '${event.getOption("to_echo")!!.asString}'").await()
        }
    }
}

