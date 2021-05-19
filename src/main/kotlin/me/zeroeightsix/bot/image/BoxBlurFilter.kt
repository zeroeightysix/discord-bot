package me.zeroeightsix.bot.image

import com.sksamuel.scrimage.filter.BufferedOpFilter
import thirdparty.jhlabs.image.BoxBlurFilter
import java.awt.image.BufferedImageOp

class BoxBlurFilter(hRadius: Float, vRadius: Float, iterations: Int) : BufferedOpFilter() {
    private val filter = BoxBlurFilter(hRadius, vRadius, iterations)

    override fun op(): BufferedImageOp = filter
}