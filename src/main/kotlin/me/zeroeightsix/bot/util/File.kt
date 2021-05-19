package me.zeroeightsix.bot.util

val String.withoutFileExtension: String
    get() = this.lastIndexOf('.').let {
        if (it == -1)
            this
        else
            this.substring(0 until it)
    }