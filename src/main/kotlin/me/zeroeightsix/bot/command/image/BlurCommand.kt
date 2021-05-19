@file:Suppress("unused")

package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import dev.minn.jda.ktx.await
import me.zeroeightsix.bot.command.CommandContext
import me.zeroeightsix.bot.image.BoxBlurFilter
import me.zeroeightsix.bot.util.Conversation.nextFile
import me.zeroeightsix.bot.util.withoutFileExtension

object BlurCommand {

    suspend fun CommandContext.execute() {
        event.reply("Supply an image to blur.").await()

        @Suppress("BlockingMethodInNonBlockingContext")
        nextFile({ it.isImage }) { event, attachment ->
            val image = ImmutableImage.loader().fromStream(attachment.retrieveInputStream().await())
                // TODO: expensive blocking call can be parallelized
                .filter(BoxBlurFilter(10.0f, 10.0f, 10))

            event.channel.sendFile(
                image.forWriter(PngWriter.MaxCompression).stream(),
                "${attachment.fileName.withoutFileExtension}.png"
            ).await()
        }
    }

}