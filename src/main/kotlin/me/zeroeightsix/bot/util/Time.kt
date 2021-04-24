package me.zeroeightsix.bot.util

import java.time.Duration

val Duration.humanReadable: String
    get() {
        val days = this.toDaysPart()
        val hours = this.toHoursPart()
        val minutes = this.toHoursPart()
        val seconds = this.toSecondsPart()

        val builder = StringBuilder()
        if (days != 0L) builder.append(" ${days}d")
        if (hours != 0) builder.append(" ${hours}h")
        if (minutes != 0) builder.append(" ${minutes}m")
        if (seconds != 0) builder.append(" ${seconds}s")

        return builder.toString().trimStart()
    }