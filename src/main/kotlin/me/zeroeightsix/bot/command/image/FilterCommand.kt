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
import com.sksamuel.scrimage.filter.HSBFilter
import com.sksamuel.scrimage.filter.InvertFilter
import com.sksamuel.scrimage.filter.KaleidoscopeFilter
import com.sksamuel.scrimage.filter.OffsetFilter
import com.sksamuel.scrimage.filter.PixelateFilter
import com.sksamuel.scrimage.filter.PrewittFilter
import com.sksamuel.scrimage.filter.RobertsFilter
import com.sksamuel.scrimage.filter.RylandersFilter
import com.sksamuel.scrimage.filter.SolarizeFilter
import com.sksamuel.scrimage.filter.TritoneFilter
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
import net.dv8tion.jda.api.interactions.commands.CommandHook
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

object FilterCommand {

    private const val BLUR_PART = 30f

    suspend fun CommandContext.execute(usrFilter: String, usrStrength: String?) {
        val filterSupplier = getFilter(usrFilter, getStrength(usrStrength)) ?: return
        val reply = event.reply(translate("supply_image").progressInput).await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }, reply) { event, attachment ->
            val workingMsg = getWorkingMsg(usrStrength)
            reply.editOriginal(workingMsg.progressWorking).await()

            try {
                sendTransformedImage(attachment, reply, event) { inputStream ->
                    transformImage(attachment, inputStream, filterSupplier)
                }
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

    internal suspend fun CommandContext.sendTransformedImage(
        attachment: Message.Attachment,
        reply: CommandHook,
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
        reply.editOriginal(translate("image_modified").progressSuccess).await()
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

    private suspend fun CommandContext.getFilter(
        usrFilter: String,
        modifier: Float
    ): ((Int, Int) -> Filter)? {
        return when (usrFilter) {
            "blur" -> { w, h ->
                val radius = min((w * modifier) / BLUR_PART, (h * modifier) / BLUR_PART).roundToInt().toFloat()
                BoxBlurFilter(radius, radius, 5)
            }
            "bump" -> { _, _ -> BumpFilter() }
            "chrome" -> { _, _ -> ChromeFilter(modifier, modifier) }
            "crystallize" -> { _, _ ->
                CrystallizeFilter(
                    (modifier + 1.0) * 5.0,
                    modifier.toDouble(),
                    0,
                    modifier.toDouble()
                )
            }
            "dither" -> { _, _ -> DitherFilter() }
            "edge" -> { _, _ -> EdgeFilter() }
            "emboss" -> { _, _ -> EmbossFilter() }
            "glow" -> { _, _ -> GlowFilter(modifier) }
            "gotham" -> { _, _ -> GothamFilter() }
            "grayscale" -> { _, _ -> GrayscaleFilter() }
            "hsb" -> { _, _ ->
                val random = Random()
                val rd =
                    { (modifier + random.nextFloat() * (1f - modifier) * modifier) * if (random.nextBoolean()) -1f else 1f }
                val h = rd()
                val s = rd() * 0.8f
                val b = rd() * 0.8f
                println("$h $s $b")
                HSBFilter(h, s, b)
            }
            "invert" -> { _, _ -> InvertFilter() }
            "kaleidoscope" -> { _, _ -> KaleidoscopeFilter(ceil(modifier * 20).toInt()) }
            "offset" -> { w, h ->
                val random = Random()
                val x = (random.nextDouble() * modifier * w).roundToInt()
                val y = (random.nextDouble() * modifier * h).roundToInt()
                OffsetFilter(x, y)
            }
            "pixelate" -> { _, _ -> PixelateFilter((modifier * 20).roundToInt()) }
            "prewitt" -> { _, _ -> PrewittFilter() }
            "roberts" -> { _, _ -> RobertsFilter() }
            "rylanders" -> { _, _ -> RylandersFilter() }
            "sharpen" -> { _, _ -> SharpenFilter.added(modifier * 3f) }
            "solarize" -> { _, _ -> SolarizeFilter() }
            "vintage" -> { _, _ -> VintageFilter() }
            else -> {
                event.reply(translate("unknown_filter").progressBorked).await()
                null
            }
        }
    }

    private fun getStrength(usrStrength: String?) = if (usrStrength == null) 0.25f else when (usrStrength) {
        "weak" -> 0.1f
        "strong" -> 0.5f
        "overkill" -> 1.0f
        else /*, 'normal'*/ -> 0.25f
    }

}