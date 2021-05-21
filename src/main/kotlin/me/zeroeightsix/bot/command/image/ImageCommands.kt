@file:Suppress("unused")

package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.nio.JpegWriter
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.image.BoxBlurFilter
import me.zeroeightsix.bot.util.BUNDLE
import me.zeroeightsix.bot.util.Conversation.nextFile
import me.zeroeightsix.bot.util.tryDelete
import me.zeroeightsix.bot.util.withoutFileExtension
import org.jetbrains.annotations.PropertyKey

abstract class ImageCommand(@PropertyKey(resourceBundle = BUNDLE) private val supplyTranslation: String) {

    abstract val filter: Filter

    suspend fun CommandContext.execute() {
        val reply = event.reply(translate(supplyTranslation).progressInput).await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }) { event, attachment ->
            reply.editOriginal("Molding image...".progressWorking).await()

            val image = ImmutableImage.loader().fromStream(attachment.retrieveInputStream().await())
                // TODO: expensive blocking call can be parallelized
                .filter(filter)

            reply.editOriginal(
                image.forWriter(JpegWriter.Default).stream(),
                "${attachment.fileName.withoutFileExtension}.png"
            ).await()
            event.message.tryDelete().await()
            reply.editOriginal(translate("image_modified").progressSuccess).await()
        }
    }

}

object BlurCommand : ImageCommand("supply_image") {
    override val filter: Filter
        get() = BoxBlurFilter(10.0f, 10.0f, 8)
}