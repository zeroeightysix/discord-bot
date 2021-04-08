@file:Suppress("unused")

package me.zeroeightsix.bot.command

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.transaction
import me.zeroeightsix.bot.storage.Usages
import me.zeroeightsix.bot.storage.usages
import org.ktorm.dsl.eq
import org.ktorm.dsl.plus
import org.ktorm.entity.find
import org.ktorm.support.mysql.insertOrUpdate

object CountCommand {

    suspend fun execute(ctx: CommandContext) {
        val member = ctx.event.member ?: return
        val memberId = member.idLong

        val usage = transaction {
            insertOrUpdate(Usages) {
                set(it.memberId, memberId)
                set(it.commandUsages, 1)
                onDuplicateKey {
                    set(it.commandUsages, it.commandUsages + 1)
                }
            }

            usages.find { it.memberId eq memberId }!!
        }


        ctx.event.reply("You've counted to ${usage.commandUsages}!", ephemeral = true)
            .await()
    }

}