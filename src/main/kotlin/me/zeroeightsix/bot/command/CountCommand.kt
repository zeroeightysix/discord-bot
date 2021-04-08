@file:Suppress("unused")

package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.storage.UsageEntry
import org.jetbrains.exposed.sql.transactions.transaction

object CountCommand {

    suspend fun execute(ctx: CommandContext) {
        val member = ctx.event.member ?: return
        val memberId = member.idLong

        val count = transaction {
            val usage = UsageEntry.findById(memberId) ?: UsageEntry.new(memberId) {}
            
            ++usage.commandUsages
        }

        ctx.event.reply("You've counted to $count!", ephemeral = true).await()
    }

}