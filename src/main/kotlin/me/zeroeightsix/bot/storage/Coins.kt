package me.zeroeightsix.bot.storage

import me.zeroeightsix.bot.MemberID
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.float
import org.ktorm.schema.long

object Coins : Table<Coin>("coins"), TableCreate {
    val memberId = long("member_id").primaryKey().bindTo { it.memberId }
    val balance = float("balance").bindTo { it.balance }

    override val tableDefinition = """
        member_id bigint not null
            primary key,
        balance float not null
    """.trimIndent()
}

interface Coin : Entity<Coin> {
    companion object : Entity.Factory<Coin>()

    val memberId: MemberID
    val balance: Float
}

object LastClaims : Table<LastClaim>("last_coin_claim"), TableCreate {
    val memberId = long("member_id").primaryKey().bindTo { it.memberId }
    val lastClaim = long("last_claim_time").bindTo { it.lastClaim }

    override val tableDefinition = """
        member_id bigint not null
            primary key,
        last_claim_time bigint not null
    """.trimIndent()
}

interface LastClaim : Entity<LastClaim> {
    companion object : Entity.Factory<LastClaim>()

    val memberId: MemberID
    val lastClaim: Long
}

val Database.coins
    get() = this.sequenceOf(Coins)
val Database.lastClaims
    get() = this.sequenceOf(LastClaims)