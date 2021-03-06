package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.*

inline fun Container.scaleView(
	width: Int, height: Int, scale: Double = 2.0, filtering: Boolean = false,
	callback: @ViewsDslMarker Container.() -> Unit = {}
) = ScaleView(width, height, scale, filtering).addTo(this).apply(callback)

class ScaleView(width: Int, height: Int, scale: Double = 2.0, var filtering: Boolean = false) :
	FixedSizeContainer(), View.Reference {
	init {
		this.width = width.toDouble()
		this.height = height.toDouble()
		this.scale = scale
	}

	//val once = Once()

	override fun render(ctx: RenderContext) {
		val iwidth = width.toInt()
		val iheight = height.toInt()

		ctx.renderToTexture(iwidth, iheight, renderToTexture = {
			super.render(ctx)
		}, use = { renderTexture ->
			ctx.batch.drawQuad(
				tex = renderTexture,
				x = 0f, y = 0f,
				width = iwidth.toFloat(),
				height = iheight.toFloat(),
				m = renderMatrix,
				colorMulInt = renderColorMulInt,
				colorAdd = renderColorAdd,
				filtering = filtering,
				blendFactors = renderBlendMode.factors
			)
			ctx.flush()
		})
	}
}
