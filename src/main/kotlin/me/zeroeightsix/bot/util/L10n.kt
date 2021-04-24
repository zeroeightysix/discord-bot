package me.zeroeightsix.bot.util

import net.dv8tion.jda.api.entities.Member
import org.jetbrains.annotations.PropertyKey
import java.text.FieldPosition
import java.text.MessageFormat
import java.util.*

internal const val BUNDLE = "messages.bundle"

fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String): String =
    ResourceBundle.getBundle(BUNDLE, locale).getString(key)

fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String, params: Array<out Any>): String =
    MessageFormat(translate(locale, key), locale).format(params, StringBuffer(), FieldPosition(0)).toString()

// TODO
val Member.locale: Locale
    get() = Locale.getDefault()