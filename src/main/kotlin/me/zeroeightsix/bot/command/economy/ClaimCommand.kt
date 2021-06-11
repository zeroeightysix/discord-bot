package me.zeroeightsix.bot.command.economy

import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.MemberID
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.database
import me.zeroeightsix.bot.storage.Coin
import me.zeroeightsix.bot.storage.Coins
import me.zeroeightsix.bot.storage.coins
import me.zeroeightsix.bot.storage.lastClaims
import me.zeroeightsix.bot.transaction
import me.zeroeightsix.bot.util.cache
import me.zeroeightsix.bot.util.humanReadable
import org.ktorm.dsl.eq
import org.ktorm.dsl.plus
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.firstOrNull
import org.ktorm.support.mysql.insertOrUpdate
import java.time.Duration
import java.time.Instant

@Suppress("unused")
object ClaimCommand {

    private const val CLAIM_AMOUNT = 200f
    private val CLAIM_TIMEOUT = Duration.ofHours(6L)

    private val cache = cache<MemberID, Instant>()
        .name("claim time cache")
        .build()

    suspend fun CommandContext.execute() {
        val member = event.member ?: return

        val duration = getTimeSinceLastClaim(member.idLong)
        if (mayClaimAgain(duration)) {
            val newBalance = claimCoins(member.idLong)

            event.replyEmbeds(translate("claimed_and_now_have", CLAIM_AMOUNT, newBalance.balance).embedSuccess).await()
        } else {
            val timeToGo = CLAIM_TIMEOUT.minus(duration)

            event.replyEmbeds(translate("claimed_too_early", timeToGo.humanReadable).embedFailure).await()
        }
    }

    private fun mayClaimAgain(duration: Duration): Boolean {
        return duration > CLAIM_TIMEOUT
    }

    private fun getTimeSinceLastClaim(member: MemberID): Duration {
        val lastTime = cache.computeIfAbsent(member) { id ->
            Instant.ofEpochMilli(database.lastClaims.filter { it.memberId eq id }.firstOrNull()?.lastClaim ?: 0)
        }

        return Duration.between(lastTime, Instant.now())
    }

    /**
     * Provide [member] with [CLAIM_AMOUNT] coins.
     *
     * Does not check last claim.
     *
     * @return the member's new balance
     */
    private fun claimCoins(member: MemberID): Coin {
        val coins = transaction {
            insertOrUpdate(Coins) {
                set(it.memberId, member)
                set(it.balance, CLAIM_AMOUNT)
                onDuplicateKey {
                    set(it.balance, it.balance + CLAIM_AMOUNT)
                }
            }

            coins.find { it.memberId eq member }!!
        }

        cache.put(member, Instant.now())
        return coins
    }

}