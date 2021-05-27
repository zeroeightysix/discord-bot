package me.zeroeightsix.bot.util

import dev.minn.jda.ktx.CoroutineEventListener
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.ChannelID
import me.zeroeightsix.bot.MemberID
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.jda
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.time.Duration
import java.time.Instant

object Conversation {

    fun CommandContext.nextMessage(
        hook: InteractionHook? = null,
        expireTime: Duration = Duration.ofMinutes(10),
        block: suspend (event: MessageReceivedEvent, message: Message) -> Unit
    ) {
        val member = event.member ?: return

        MessageEventListener(member.idLong, event.channel.idLong, hook, block, Instant.now() + expireTime).also {
            jda.eventManager.handle(ConversationListenerAttachedEvent(it, member.idLong, event.channel.idLong))
        }
    }

    /**
     * Create a handler waiting for the member that caused this [CommandContext] to post an attachment in the same channel as this [CommandContext],
     * calling [block] when it happens.
     *
     * This handler will expire:
     * * After [expireTime] is reached
     * * If another handler of the same type is created for the same member in the same channel
     * * After a suitable attachment was uploaded by the member and [block] was called
     *
     * @param filter An optional filter on attachments. For example, if you only wish to receive images, use `{ it.isImage }`
     * @param hook A reference to a reply given to the user. If the handler created by [nextFile] expires, it will purge [hook]
     * @param expireTime The duration for which this handler is active
     * @param block The function to call when an attachment is received. This is called only once
     */
    fun CommandContext.nextFile(
        filter: (Message.Attachment) -> Boolean = { true },
        hook: InteractionHook? = null,
        expireTime: Duration = Duration.ofMinutes(1),
        block: suspend (event: MessageReceivedEvent, attachment: Message.Attachment) -> Unit
    ) {
        val member = event.member ?: return

        AttachmentEventListener(
            member.idLong,
            event.channel.idLong,
            filter,
            hook,
            block,
            Instant.now() + expireTime
        ).also {
            jda.eventManager.handle(ConversationListenerAttachedEvent(it, member.idLong, event.channel.idLong))
        }
    }

    private abstract class EphemeralEventListener<E, R>(
        private val consumer: suspend (E, R) -> Unit,
        private val dieAt: Instant?
    ) : CoroutineEventListener {
        init {
            @Suppress("LeakingThis")
            jda.addEventListener(this)
        }

        override suspend fun onEvent(event: GenericEvent) {
            if (Instant.now() > dieAt) {
                expire()
                return
            }

            val (ctx, ret) = extractFromEvent(event) ?: return
            unsubscribe()
            consumer(ctx, ret)
        }

        abstract suspend fun extractFromEvent(event: GenericEvent): Pair<E, R>?

        private fun unsubscribe() = jda.removeEventListener(this)

        open suspend fun expire() {
            this.unsubscribe()
        }
    }

    private abstract class ReplyDeletingEventListener<E, R>(
        private val hook: InteractionHook?,
        private val member: MemberID,
        private val channel: ChannelID,
        consumer: suspend (E, R) -> Unit,
        dieAt: Instant?
    ) : EphemeralEventListener<E, R>(consumer, dieAt) {
        override suspend fun onEvent(event: GenericEvent) {
            if (event is ConversationListenerAttachedEvent
                && event.listener !== this
                && event.channel == this.channel
                && event.member == this.member
            ) {
                this.expire()
                return
            }

            super.onEvent(event)
        }

        override suspend fun expire() {
            super.expire()
            // message might have been deleted by something else
            try {
                this.hook?.deleteOriginal()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class MessageEventListener(
        private val member: MemberID,
        private val channel: ChannelID,
        hook: InteractionHook?,
        consumer: suspend (event: MessageReceivedEvent, Message) -> Unit,
        dieAt: Instant?
    ) : ReplyDeletingEventListener<MessageReceivedEvent, Message>(hook, member, channel, consumer, dieAt) {
        override suspend fun extractFromEvent(event: GenericEvent): Pair<MessageReceivedEvent, Message>? {
            if (event !is MessageReceivedEvent
                || event.member?.idLong != this.member
                || event.channel.idLong != this.channel
            )
                return null

            return event to event.message
        }
    }

    private class AttachmentEventListener(
        private val member: MemberID,
        private val channel: ChannelID,
        private val filter: (Message.Attachment) -> Boolean,
        hook: InteractionHook?,
        consumer: suspend (event: MessageReceivedEvent, Message.Attachment) -> Unit,
        dieAt: Instant?
    ) : ReplyDeletingEventListener<MessageReceivedEvent, Message.Attachment>(hook, member, channel, consumer, dieAt) {
        override suspend fun extractFromEvent(event: GenericEvent): Pair<MessageReceivedEvent, Message.Attachment>? {
            if (event !is MessageReceivedEvent
                || event.member?.idLong != this.member
                || event.channel.idLong != this.channel
                || event.message.attachments.isEmpty()
            )
                return null

            return event to (event.message.attachments.asSequence()
                .filter(filter)
                .firstOrNull() ?: return null)
        }
    }
}

class ConversationListenerAttachedEvent internal constructor(
    val listener: CoroutineEventListener,
    val member: MemberID,
    val channel: ChannelID,
    private val responseNumber: Long = jda.responseTotal
) : GenericEvent {
    override fun getJDA(): JDA = jda
    override fun getResponseNumber() = this.responseNumber
}