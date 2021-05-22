package me.zeroeightsix.bot.command.image

import com.sksamuel.scrimage.filter.BufferedOpFilter
import thirdparty.jhlabs.image.ConvolveFilter
import java.awt.image.BufferedImageOp

class SharpenFilter private constructor(matrix: FloatArray) : BufferedOpFilter() {

    private val op = ConvolveFilter(matrix)

    override fun op(): BufferedImageOp = this.op

    companion object {
        fun added(strength: Float) = SharpenFilter(
            floatArrayOf(
                0.0f, -0.2f - strength, 0.0f,
                -0.2f - strength, 1.8f + 4 * strength, -0.2f - strength,
                0.0f, -0.2f - strength, 0.0f,
            )
        )
    }

}