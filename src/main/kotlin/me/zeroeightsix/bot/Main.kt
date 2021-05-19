package me.zeroeightsix.bot

import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.command.XMLCommandLoader
import me.zeroeightsix.bot.service.VoiceTracker
import me.zeroeightsix.bot.storage.createTablesIfNotExist
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE

const val TOKEN_VAR_NAME = "DISCORD_TOKEN"
const val GUILD_VAR_NAME = "GUILD_ID"
const val DB_USER_VAR_NAME = "DB_USER"
const val DB_PASS_VAR_NAME = "DB_PASSWORD"
const val DB_NAME = "DB_NAME"

private val logger = KotlinLogging.logger {}

lateinit var jda: JDA
    private set

typealias ID = Long
typealias MemberID = ID
typealias ChannelID = ID

suspend fun main() {
    logger.info { "Starting up" }

    // Collect environment variables
    fun missingEnv(vr: String) = logger.error { "Failed to start bot. $vr environment variable is not configured!" }

    val token = System.getenv(TOKEN_VAR_NAME) ?: return missingEnv(TOKEN_VAR_NAME)
    val testingGuildId = System.getenv(GUILD_VAR_NAME) ?: return missingEnv(GUILD_VAR_NAME)

    // Load commands
    val (executor, commands) = (object {}).javaClass.getResourceAsStream("/commands.xml")!!.use { stream ->
        val handler = XMLCommandLoader.load(stream)
        logger.info { "Loaded ${handler.commandCount} commands" }

        handler.createExecutor<CommandContext>("me.zeroeightsix.bot.command") to handler.buildCommands()
    }

    connectDatabase(
        System.getenv(DB_USER_VAR_NAME) ?: return missingEnv(DB_USER_VAR_NAME),
        System.getenv(DB_PASS_VAR_NAME) ?: return missingEnv(DB_PASS_VAR_NAME),
        System.getenv(DB_NAME) ?: "bot"
    )

    createTablesIfNotExist()

    // Connect to discord
    jda = JDABuilder.createLight(token, GUILD_VOICE_STATES)
        .enableCache(VOICE_STATE)
        .setMemberCachePolicy(MemberCachePolicy.VOICE)
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

    jda.listener<ShutdownEvent> {
        logger.info("Bye!")
    }

    initMustInit()
}

// I hate this function.
// But I don't want to do it by reflection.
private fun initMustInit() {
    VoiceTracker
}