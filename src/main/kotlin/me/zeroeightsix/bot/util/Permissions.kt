package me.zeroeightsix.bot.util

import net.dv8tion.jda.api.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.CompletableFuture

fun Message.tryDelete(): CompletableFuture<Void> {
    return if (guild.selfMember.hasPermission(MESSAGE_MANAGE))
        delete().submit()
    else
        CompletableFuture()
}