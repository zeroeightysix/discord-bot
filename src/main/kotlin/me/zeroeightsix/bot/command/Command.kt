package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.util.BUNDLE
import me.zeroeightsix.bot.util.locale
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN
import net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL
import net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER
import net.dv8tion.jda.api.interactions.commands.OptionType.ROLE
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.OptionType.UNKNOWN
import net.dv8tion.jda.api.interactions.commands.OptionType.USER
import org.jetbrains.annotations.PropertyKey
import java.util.*
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

interface L10n {
    val locale: Locale

    infix fun translate(@PropertyKey(resourceBundle = BUNDLE) key: String) =
        me.zeroeightsix.bot.util.translate(this.locale, key)

    fun translate(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        me.zeroeightsix.bot.util.translate(this.locale, key, params)

}

class CommandContext(val event: SlashCommandEvent) : CommandOptions, L10n {
    override val name: String = event.name
    override val subGroupName: String? = event.subcommandGroup
    override val subName: String? = event.subcommandName
    override val locale: Locale
        get() = event.member?.locale ?: Locale.getDefault()

    override fun getOption(name: String, desiredType: KClass<out Any>): Any? {
        val option = event.getOption(name) ?: return null
        return when (option.type) {
            UNKNOWN, STRING -> option.asString
            INTEGER -> option.asLong.toInt()
            BOOLEAN -> option.asBoolean
            USER -> option.asUser
            CHANNEL -> option.asGuildChannel
            ROLE -> option.asRole
            else -> TODO()
        }
    }

    inline val String.err: String
        get() = ":warning: $this"

    inner class IntConstraint(
        val min: Int? = null,
        val max: Int? = null
    ) {
        inline operator fun invoke(value: Int, onError: (Int) -> Int): Int {
            min?.let { min -> if (value < min) return onError(value) }
            max?.let { min -> if (value > min) return onError(value) }
            return value
        }

        inline operator fun invoke(value: Int?, onError: (Int) -> Int): Int? =
            value?.let { invoke(it, onError) }
    }
    
    inner class TypeConstraint {
        inline operator fun <reified Has, reified Want> invoke(value: Has, onError: (Has) -> Want): Want =
            (value as? Want) ?: onError(value)

        @JvmName("invokeNullable")
        inline operator fun <reified Has, reified Want> invoke(value: Has?, onError: (Has) -> Want): Want? =
            value?.let { invoke(it, onError) }

        // This exists because the nullable `invoke` overload may not be called where expected as the firsts generic type can be nullable.
        inline fun <reified Has, reified Want> optional(value: Has?, onError: (Has) -> Want) =
            this.invoke(value, onError)
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