package me.zeroeightsix.bot.image

import at.dhyan.open_imaging.GifDecoder
import com.sksamuel.scrimage.ImmutableImage
import java.io.InputStream

object GifReader {

    fun readGif(stream: InputStream): Gif {
        val gif = GifDecoder.read(stream.readAllBytes())
        return Gif(
            (0 until gif.frameCount).map { ImmutableImage.wrapAwt(gif.getFrame(it)) },
            gif.getDelay(0).toLong(),
            gif.repetitions != 0
        )
    }

    class Gif(val images: List<ImmutableImage>, val delay: Long, val loop: Boolean)

}