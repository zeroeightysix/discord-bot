import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.command.XMLCommandLoader
import me.zeroeightsix.bot.storage.Usage
import mu.KotlinLogging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

const val TOKEN_VAR_NAME = "DISCORD_TOKEN"
const val GUILD_VAR_NAME = "GUILD_ID"
const val DB_USER_VAR_NAME = "DB_USER"
const val DB_PASS_VAR_NAME = "DB_PASSWORD"

private val logger = KotlinLogging.logger {}

suspend fun main() {
    logger.info { "Starting up" }

    // Collect environment variables
    fun missingEnv(vr: String) = logger.error { "Failed to start bot. $vr environment variable is not configured!" }

    val token = System.getenv(TOKEN_VAR_NAME) ?: return missingEnv(TOKEN_VAR_NAME)
    val testingGuildId = System.getenv(GUILD_VAR_NAME) ?: return missingEnv(GUILD_VAR_NAME)

    initDatabase(
        System.getenv(DB_USER_VAR_NAME) ?: return missingEnv(DB_USER_VAR_NAME),
        System.getenv(DB_PASS_VAR_NAME) ?: return missingEnv(DB_PASS_VAR_NAME)
    )

    // Load commands
    val (executor, commands) = (object {}).javaClass.getResourceAsStream("/commands.xml")!!.use { stream ->
        val handler = XMLCommandLoader.load(stream)
        logger.info { "Loaded ${handler.commandCount} commands" }

        handler.createExecutor<CommandContext>("me.zeroeightsix.bot.command") to handler.buildCommands()
    }

    // Connect to discord
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
        executor.execute(CommandContext(event)).failure?.let {
            logger.error { "Failed to execute command: $it (event: $event)" }

            // No reply to discord: this way the user gets a pretty error message from discord itself.
        }
    }
}

private fun initDatabase(dbUser: String, dbPassword: String) {
    Database.connect(
        "jdbc:mysql://localhost:3306/test", driver = "com.mysql.jdbc.Driver",
        user = dbUser, password = dbPassword
    )

    transaction {
        SchemaUtils.create(Usage)
    }
}