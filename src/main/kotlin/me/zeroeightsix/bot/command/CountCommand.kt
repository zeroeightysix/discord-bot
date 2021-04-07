@file:Suppress("unused")

package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.storage.UsageEntry
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object CountCommand {

    suspend fun execute(ctx: SlashCommandContext) {
        val member = ctx.event.member ?: return
        val memberId = member.idLong

        val count = transaction {
            addLogger(Slf4jSqlDebugLogger)

            val usage = UsageEntry.findById(memberId) ?: UsageEntry.new(memberId) {}
            
            ++usage.commandUsages
        }
        
        ctx.event.reply("Count is at $count!").await()
    }

}