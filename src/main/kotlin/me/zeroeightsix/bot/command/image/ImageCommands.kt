@file:Suppress("unused")

package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.nio.GifSequenceWriter
import com.sksamuel.scrimage.nio.JpegWriter
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.image.BoxBlurFilter
import me.zeroeightsix.bot.image.GifReader
import me.zeroeightsix.bot.util.BUNDLE
import me.zeroeightsix.bot.util.Conversation.nextFile
import me.zeroeightsix.bot.util.tryDelete
import me.zeroeightsix.bot.util.withoutFileExtension
import org.jetbrains.annotations.PropertyKey
import java.io.ByteArrayInputStream

abstract class ImageCommand(@PropertyKey(resourceBundle = BUNDLE) private val supplyTranslation: String) {

    abstract val filter: Filter

    suspend fun CommandContext.execute() {
        val reply = event.reply(translate(supplyTranslation).progressInput).await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }) { event, attachment ->
            reply.editOriginal("Molding image...".progressWorking).await()

            val inputStream = attachment.retrieveInputStream().await()
            try {
                val (output, ext) = if (attachment.fileName.endsWith(".gif")) {
                    val gif = GifReader.readGif(inputStream)
                    val images = gif.images.map { it.filter(filter) }.toTypedArray()
                    ByteArrayInputStream(GifSequenceWriter(gif.delay, gif.loop).bytes(images)) to "gif"
                } else {
                    val image = ImmutableImage.loader()
                        .fromStream(inputStream)
                        .filter(filter)
                    image.forWriter(JpegWriter.Default).stream() to "jpg"
                }

                reply.editOriginal(
                    output,
                    "${attachment.fileName.withoutFileExtension}.$ext"
                ).await()
                event.message.tryDelete().await()
                reply.editOriginal(translate("image_modified").progressSuccess).await()
            } catch (e: Exception) {
                e.printStackTrace()

                reply.editOriginal("""
                    ${translate("err_throwable_occurred", e.javaClass.simpleName)}
                    ${translate("err_contact_dev")}
                """.trimIndent().progressBorked).await()
            }
        }
    }

}

object BlurCommand : ImageCommand("supply_image") {
    override val filter: Filter
        get() = BoxBlurFilter(10.0f, 10.0f, 8)
}