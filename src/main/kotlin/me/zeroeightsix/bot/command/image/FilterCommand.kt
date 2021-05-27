@file:Suppress("unused")

package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.BumpFilter
import com.sksamuel.scrimage.filter.ChromeFilter
import com.sksamuel.scrimage.filter.CrystallizeFilter
import com.sksamuel.scrimage.filter.DitherFilter
import com.sksamuel.scrimage.filter.EdgeFilter
import com.sksamuel.scrimage.filter.EmbossFilter
import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.filter.GlowFilter
import com.sksamuel.scrimage.filter.GothamFilter
import com.sksamuel.scrimage.filter.GrayscaleFilter
import com.sksamuel.scrimage.filter.InvertFilter
import com.sksamuel.scrimage.filter.KaleidoscopeFilter
import com.sksamuel.scrimage.filter.NashvilleFilter
import com.sksamuel.scrimage.filter.OffsetFilter
import com.sksamuel.scrimage.filter.PixelateFilter
import com.sksamuel.scrimage.filter.PrewittFilter
import com.sksamuel.scrimage.filter.RobertsFilter
import com.sksamuel.scrimage.filter.RylandersFilter
import com.sksamuel.scrimage.filter.SolarizeFilter
import com.sksamuel.scrimage.filter.VintageFilter
import com.sksamuel.scrimage.nio.GifSequenceWriter
import com.sksamuel.scrimage.nio.JpegWriter
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.image.BoxBlurFilter
import me.zeroeightsix.bot.image.GifReader
import me.zeroeightsix.bot.util.Conversation.nextFile
import me.zeroeightsix.bot.util.tryDelete
import me.zeroeightsix.bot.util.withoutFileExtension
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object FilterCommand {

    suspend fun CommandContext.filterImage(usrStrength: String?, filterSupplier: (Int, Int) -> Filter) {
        val reply = event.replyEmbeds(translate("supply_image").progressInput).await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }, reply) { event, attachment ->
            val workingMsg = getWorkingMsg(usrStrength)
            reply.editOriginalEmbeds(workingMsg.progressWorking).await()

            try {
                sendTransformedImage(attachment, reply, event) { inputStream ->
                    transformImage(attachment, inputStream, filterSupplier)
                }
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

    internal suspend fun CommandContext.sendTransformedImage(
        attachment: Message.Attachment,
        reply: InteractionHook,
        event: MessageReceivedEvent,
        transform: (InputStream) -> Pair<ByteArrayInputStream, String>?
    ) {
        val inputStream = attachment.retrieveInputStream().await()
        event.message.tryDelete().await()

        val (output, ext) = transform(inputStream) ?: return

        reply.editOriginal(
            output,
            "${attachment.fileName.withoutFileExtension}.$ext"
        ).await()
        reply.editOriginalEmbeds(translate("image_modified").progressSuccess).await()
    }

    private fun transformImage(
        attachment: Message.Attachment,
        inputStream: InputStream,
        filterSupplier: (Int, Int) -> Filter
    ) = if (attachment.fileName.endsWith(".gif")) {
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

    private fun CommandContext.getWorkingMsg(usrStrength: String?): String {
        var workingMsg = translate("molding_image")
        usrStrength?.let { workingMsg += "\n" + translate("filter_strength", it) }
        return workingMsg
    }

    object Blur {
        suspend fun CommandContext.execute(usrPercentage: Int?) {
            val percentage = (IntConstraint(min = 0, max = 100)(usrPercentage) {
                event.replyEmbeds(translate("blur_out_of_bounds").progressBorked).await()
                return
            } ?: 20) / 100f

            filterImage("$percentage%") { w, h ->
                val radius = min(w * percentage / 20f, h * percentage / 20f).roundToInt().toFloat()
                BoxBlurFilter(radius, radius, 5)
            }
        }
    }

    object Bump {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> BumpFilter() }
    }

    object Chrome {
        suspend fun CommandContext.execute(usrAmount: Int?, usrExposure: Int?) {
            val amount = (IntConstraint(min = 0, max = 100)(usrAmount) {
                event.replyEmbeds(translate("chrome_amount_out_of_bounds").progressBorked).await()
                return
            } ?: 50) / 100f

            val exposure = (IntConstraint(min = 0, max = 100)(usrExposure) {
                event.replyEmbeds(translate("chrome_exposure_out_of_bounds").progressBorked).await()
                return
            } ?: 100) / 100f

            filterImage("$usrAmount, $usrExposure") { _, _ -> ChromeFilter(amount, exposure) }
        }
    }

    object Crystallize {
        suspend fun CommandContext.execute(usrScale: Int?, usrThickness: Int?, usrRandomness: Int?) {
            val scale = (IntConstraint(min = 1)(usrScale) {
                event.replyEmbeds(translate("crystallize_scale_out_of_bounds").progressBorked).await()
                return
            } ?: 25) / 10.0
            val thickness = (IntConstraint(min = 1)(usrThickness) {
                event.replyEmbeds(translate("crystallize_thickness_out_of_bounds").progressBorked).await()
                return
            } ?: 25) / 10.0
            val randomness = (IntConstraint(min = 1)(usrRandomness) {
                event.replyEmbeds(translate("crystallize_randomness_out_of_bounds").progressBorked).await()
                return
            } ?: 25) / 10.0

            filterImage("$usrScale $usrThickness $usrRandomness") { _, _ -> CrystallizeFilter(scale, thickness, 0, randomness)}
        }
    }

    object Dither {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> DitherFilter() }
    }

    object Edge {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> EdgeFilter() }
    }

    object Emboss {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> EmbossFilter() }
    }

    object Glow {
        suspend fun CommandContext.execute(usrAmount: Int?) {
            val amount = (IntConstraint(min = 0, max = 100)(usrAmount) {
                event.replyEmbeds(translate("glow_amount_out_of_bounds").progressBorked).await()
                return
            } ?: 50) / 100f

            filterImage(null) { _, _ -> GlowFilter(amount) }
        }
    }

    object Gotham {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> GothamFilter() }
    }

    object Grayscale {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> GrayscaleFilter() }
    }

    object Invert {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> InvertFilter() }
    }

    object Kaleidoscope {
        suspend fun CommandContext.execute(usrSides: Int?) {
            val sides = IntConstraint(min = 3)(usrSides) {
                event.replyEmbeds(translate("kaleidoscope_sides_out_of_bounds").progressBorked).await()
                return
            } ?: 3

            filterImage("$sides sides") { _, _ -> KaleidoscopeFilter(sides) }
        }
    }

    object Offset {
        suspend fun CommandContext.execute(usrX: Int?, usrY: Int?) {
            filterImage("X: $usrX, Y: $usrY") { w, h ->
                OffsetFilter(usrX ?: Random.nextInt(w), usrY ?: Random.nextInt(h))
            }
        }
    }

    object Pixelate {
        suspend fun CommandContext.execute(usrBlockSize: Int?) {
            val blockSize = IntConstraint(min = 1, max = 100)(usrBlockSize) {
                event.replyEmbeds(translate("pixelate_block_size_out_of_bounds").progressBorked).await()
                return
            } ?: 5

            filterImage("block size = $blockSize") { _, _ -> PixelateFilter(blockSize)}
        }
    }

    object Prewitt {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> PrewittFilter() }
    }

    object Roberts {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> RobertsFilter() }
    }

    object Rylanders {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> RylandersFilter() }
    }

    object Sharpen {
        suspend fun CommandContext.execute(usrStrength: Int?) {
            val strength = (IntConstraint(min = 1, max = 100)(usrStrength) {
                event.replyEmbeds(translate("sharpen_strength_out_of_bounds").progressBorked).await()
                return
            } ?: 9) / 33f

            filterImage("strength = ${strength * 33f}") { _, _ -> SharpenFilter.added(strength)}
        }
    }

    object Solarize {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> SolarizeFilter() }
    }

    object Vintage {
        suspend fun CommandContext.execute() = filterImage(null) { _, _ -> VintageFilter() }
    }

}