package me.zeroeightsix.bot.util

import dev.minn.jda.ktx.CoroutineEventListener
import me.zeroeightsix.bot.ChannelID
import me.zeroeightsix.bot.MemberID
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.jda
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant

object Conversation {

    fun CommandContext.nextMessage(block: suspend (event: MessageReceivedEvent, message: Message) -> Unit) {
        val member = event.member ?: return

        MessageEventListener(member.idLong, event.channel.idLong, block, Instant.now().plus(Duration.ofMinutes(15)))
    }

    fun CommandContext.nextFile(filter: (Message.Attachment) -> Boolean = { true }, block: suspend (event: MessageReceivedEvent, attachment: Message.Attachment) -> Unit) {
        val member = event.member ?: return

        AttachmentEventListener(member.idLong, event.channel.idLong, filter, block, Instant.now().plus(Duration.ofMinutes(15)))
    }

    private abstract class EphemeralEventListener<E, R>(private val consumer: suspend (E, R) -> Unit, private val dieAt: Instant? = null) : CoroutineEventListener {
        private fun unsubscribe() = jda.removeEventListener(this)

        init {
            jda.addEventListener(this)
        }

        override suspend fun onEvent(event: GenericEvent) {
            if (Instant.now() > dieAt)
                unsubscribe()

            val (ctx, ret) = extractFromEvent(event) ?: return
            unsubscribe()
            consumer(ctx, ret)
        }

        abstract suspend fun extractFromEvent(event: GenericEvent): Pair<E, R>?
    }

    private class MessageEventListener(
        private val member: MemberID,
        private val channel: ChannelID?,
        consumer: suspend (event: MessageReceivedEvent, Message) -> Unit,
        dieAt: Instant? = null
    ) : EphemeralEventListener<MessageReceivedEvent, Message>(consumer, dieAt) {
        override suspend fun extractFromEvent(event: GenericEvent): Pair<MessageReceivedEvent, Message>? {
            if (event !is MessageReceivedEvent
                || event.member?.idLong != this.member
                || (this.channel != null && event.channel.idLong != this.channel)
            )
                return null

            return event to event.message
        }
    }

    private class AttachmentEventListener(
        private val member: MemberID,
        private val channel: ChannelID?,
        private val filter: (Message.Attachment) -> Boolean,
        consumer: suspend (event: MessageReceivedEvent, Message.Attachment) -> Unit,
        dieAt: Instant? = null
    ) : EphemeralEventListener<MessageReceivedEvent, Message.Attachment>(consumer, dieAt) {
        override suspend fun extractFromEvent(event: GenericEvent): Pair<MessageReceivedEvent, Message.Attachment>? {
            if (event !is MessageReceivedEvent
                || event.member?.idLong != this.member
                || (this.channel != null && event.channel.idLong != this.channel)
                || event.message.attachments.isEmpty()
            )
                return null

            return event to (event.message.attachments.asSequence()
                .filter(filter)
                .firstOrNull() ?: return null)
        }
    }
}