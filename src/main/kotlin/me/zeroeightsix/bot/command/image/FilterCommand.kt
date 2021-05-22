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
import me.zeroeightsix.bot.util.Conversation.nextFile
import me.zeroeightsix.bot.util.tryDelete
import me.zeroeightsix.bot.util.withoutFileExtension
import java.io.ByteArrayInputStream
import kotlin.math.min
import kotlin.math.roundToInt

object FilterCommand {

    private const val BLUR_PART = 30f

    suspend fun CommandContext.execute(usrFilter: String, usrStrength: String?) {
        val modifier = if (usrStrength == null) 0.25f else when (usrStrength) {
            "weak" -> 0.1f
            "strong" -> 0.5f
            "overkill" -> 1.0f
            else /*, 'normal'*/ -> 0.25f
        }

        val filterSupplier: (width: Int, height: Int) -> Filter = when (usrFilter) {
            "blur" -> { w, h ->
                val radius = min((w * modifier) / BLUR_PART, (h * modifier) / BLUR_PART).roundToInt().toFloat()
                BoxBlurFilter(radius, radius, 5)
            }
            else -> {
                event.reply(translate("unknown_filter").progressBorked).await()
                return
            }
        }

        val reply = event.reply(translate("supply_image").progressInput).await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }, reply) { event, attachment ->
            var workingMsg = translate("molding_image")
            usrStrength?.let { workingMsg += "\n" + translate("filter_strength", it) }
            reply.editOriginal(workingMsg.progressWorking).await()

            val inputStream = attachment.retrieveInputStream().await()
            try {
                event.message.tryDelete().await()

                val (output, ext) = if (attachment.fileName.endsWith(".gif")) {
                    val gif = GifReader.readGif(inputStream)
                    val filter = filterSupplier(gif.width, gif.height)
                    val images = gif.images.map { it.filter(filter) }.toTypedArray()
                    ByteArrayInputStream(GifSequenceWriter(gif.delay, gif.loop).bytes(images)) to "gif"
                } else {
                    val image = ImmutableImage.loader()
                        .fromStream(inputStream)

                    image.filter(filterSupplier(image.width, image.height)).forWriter(JpegWriter.Default)
                        .stream() to "jpg"
                }

                reply.editOriginal(
                    output,
                    "${attachment.fileName.withoutFileExtension}.$ext"
                ).await()
                reply.editOriginal(translate("image_modified").progressSuccess).await()
            } catch (e: Exception) {
                e.printStackTrace()

                reply.editOriginal(
                    """
                    ${translate("err_throwable_occurred", e.javaClass.simpleName)}
                    ${translate("err_contact_dev")}
                    """.trimIndent().progressBorked
                ).await()
            }
        }
    }

}