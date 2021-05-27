package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.GifSequenceWriter
import com.sksamuel.scrimage.nio.JpegWriter
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.command.image.FilterCommand.sendTransformedImage
import me.zeroeightsix.bot.image.GifReader
import me.zeroeightsix.bot.util.Conversation.nextFile
import net.dv8tion.jda.api.entities.Message
import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("unused")
object ScaleimageCommand {

    private const val MAX_SCALE = 500

    suspend fun CommandContext.execute(usrScale: Int) {
        val scale = if (usrScale in 1..MAX_SCALE) {
            usrScale.toDouble() / 100.0
        } else {
            event.replyEmbeds(translate("scale_out_of_bounds", 0, MAX_SCALE).progressBorked).await()
            return
        }

        val reply = event.replyEmbeds(translate("supply_image").progressInput).await()
        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }, reply) { event, attachment ->
            try {
                sendTransformedImage(attachment, reply, event) { inputStream ->
                    scaleImage(attachment, inputStream, scale)
                }
            } catch (e: ScaleException) {
                reply.editOriginalEmbeds(translate("scale_too_small").progressBorked).await()
            } catch (e: Exception) {
                e.printStackTrace()

                reply.editOriginalEmbeds(
                    """
                    ${translate("err_throwable_occurred", e.javaClass.simpleName)}
                    ${translate("err_contact_dev")}
                    """.trimIndent().progressBorked
                ).await()
            }
        }
    }

    private fun scaleImage(
        attachment: Message.Attachment,
        inputStream: InputStream,
        scale: Double,
    ) = if (attachment.fileName.endsWith(".gif")) {
        val gif = GifReader.readGif(inputStream)
        val images = gif.images.map { it.scale(scale) }.toTypedArray()
        ByteArrayInputStream(GifSequenceWriter(gif.delay, gif.loop).bytes(images)) to "gif"
    } else {
        val image = ImmutableImage.loader()
            .fromStream(inputStream)

        if (image.width * scale < 3 || image.height * scale < 3)
            throw ScaleException

        image.scale(scale)
            .forWriter(JpegWriter.Default)
            .stream() to "jpg"
    }

}

object ScaleException: Exception()