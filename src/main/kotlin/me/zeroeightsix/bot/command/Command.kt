package me.zeroeightsix.bot.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Command.OptionType.BOOLEAN
import net.dv8tion.jda.api.entities.Command.OptionType.CHANNEL
import net.dv8tion.jda.api.entities.Command.OptionType.INTEGER
import net.dv8tion.jda.api.entities.Command.OptionType.ROLE
import net.dv8tion.jda.api.entities.Command.OptionType.STRING
import net.dv8tion.jda.api.entities.Command.OptionType.UNKNOWN
import net.dv8tion.jda.api.entities.Command.OptionType.USER
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import kotlin.reflect.KClass

@FunctionalInterface
interface CommandExecutor<T, F> {
    
    suspend fun execute(context: T): Result<F>

}

enum class CommandFailure {
    DOES_NOT_EXIST
}

interface CommandOptions {
    
    val name: String
    val subName: String?
    val subGroupName: String?
    
    fun getOption(name: String, desiredType: KClass<out Any>): Any?
    
}

class CommandContext(val event: SlashCommandEvent, val jda: JDA) : CommandOptions {
    override val name: String = event.name
    override val subGroupName: String? = event.subcommandGroup
    override val subName: String? = event.subcommandName

    override fun getOption(name: String, desiredType: KClass<out Any>): Any? {
        val option = event.getOption(name) ?: return null
        return when (option.type) {
            UNKNOWN, STRING -> option.asString
            INTEGER -> option.asLong.toInt()
            BOOLEAN -> option.asBoolean
            USER -> option.asUser
            CHANNEL -> option.asChannel
            ROLE -> option.asRole
            else -> TODO()
        }
    }

}

class Result<out F> private constructor(val failure: F?) {
    companion object {
        fun <F> failure(handler: F) = Result(handler)
        fun <F> success(): Result<F> = Result(null)
    }
    
    fun isFailure() = this.failure != null
}

fun SlashCommandEvent.reply(content: String, ephemeral: Boolean) =
    this.reply(content).setEphemeral(ephemeral)

fun SlashCommandEvent.reply(content: MessageEmbed, ephemeral: Boolean) =
    this.reply(content).setEphemeral(ephemeral)

fun SlashCommandEvent.reply(content: Message, ephemeral: Boolean) =
    this.reply(content).setEphemeral(ephemeral)