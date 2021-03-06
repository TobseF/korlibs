package com.soywiz.korim.bitmap

import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class NinePatchBitmap32Test {
	@Test
	fun name() = suspendTest {
		val ninePatch = ResourcesVfs["bubble-chat.9.png"].readNinePatch(defaultImageFormats)

		assertEquals(
			listOf(false to 0..89, true to 90..156, false to 157..199),
			ninePatch.info.xsegments.map { it.scaled to it.range }
		)
		assertEquals(
			listOf(false to 0..55, true to 56..107, false to 108..199),
			ninePatch.info.ysegments.map { it.scaled to it.range }
		)
		assertEquals(202, ninePatch.bmp.width)
		assertEquals(202, ninePatch.bmp.height)

		assertEquals(200, ninePatch.info.width)
		assertEquals(200, ninePatch.info.height)

		assertEquals(133, ninePatch.info.fixedWidth)
		assertEquals(148, ninePatch.info.fixedHeight)

		assertEquals(
			"""
				IRectangle(x=0, y=0, width=512, height=256):
				 - IRectangle(x=0, y=0, width=90, height=56):0,0,90,56
				 - IRectangle(x=90, y=0, width=67, height=56):90,0,379,56
				 - IRectangle(x=157, y=0, width=43, height=56):469,0,43,56
				 - IRectangle(x=0, y=56, width=90, height=52):0,56,90,108
				 - IRectangle(x=90, y=56, width=67, height=52):90,56,379,108
				 - IRectangle(x=157, y=56, width=43, height=52):469,56,43,108
				 - IRectangle(x=0, y=108, width=90, height=92):0,164,90,92
				 - IRectangle(x=90, y=108, width=67, height=92):90,164,379,92
				 - IRectangle(x=157, y=108, width=43, height=92):469,164,43,92
				IRectangle(x=0, y=0, width=256, height=512):
				 - IRectangle(x=0, y=0, width=90, height=56):0,0,90,56
				 - IRectangle(x=90, y=0, width=67, height=56):90,0,123,56
				 - IRectangle(x=157, y=0, width=43, height=56):213,0,43,56
				 - IRectangle(x=0, y=56, width=90, height=52):0,56,90,364
				 - IRectangle(x=90, y=56, width=67, height=52):90,56,123,364
				 - IRectangle(x=157, y=56, width=43, height=52):213,56,43,364
				 - IRectangle(x=0, y=108, width=90, height=92):0,420,90,92
				 - IRectangle(x=90, y=108, width=67, height=92):90,420,123,92
				 - IRectangle(x=157, y=108, width=43, height=92):213,420,43,92
				IRectangle(x=0, y=0, width=100, height=100):
				 - IRectangle(x=0, y=0, width=90, height=56):0,0,45,28
				 - IRectangle(x=90, y=0, width=67, height=56):45,0,33,28
				 - IRectangle(x=157, y=0, width=43, height=56):78,0,21,28
				 - IRectangle(x=0, y=56, width=90, height=52):0,28,45,26
				 - IRectangle(x=90, y=56, width=67, height=52):45,28,33,26
				 - IRectangle(x=157, y=56, width=43, height=52):78,28,21,26
				 - IRectangle(x=0, y=108, width=90, height=92):0,54,45,46
				 - IRectangle(x=90, y=108, width=67, height=92):45,54,33,46
				 - IRectangle(x=157, y=108, width=43, height=92):78,54,21,46
				IRectangle(x=0, y=0, width=0, height=0):
				 - IRectangle(x=0, y=0, width=90, height=56):0,0,0,0
				 - IRectangle(x=90, y=0, width=67, height=56):0,0,0,0
				 - IRectangle(x=157, y=0, width=43, height=56):0,0,0,0
				 - IRectangle(x=0, y=56, width=90, height=52):0,0,0,0
				 - IRectangle(x=90, y=56, width=67, height=52):0,0,0,0
				 - IRectangle(x=157, y=56, width=43, height=52):0,0,0,0
				 - IRectangle(x=0, y=108, width=90, height=92):0,0,0,0
				 - IRectangle(x=90, y=108, width=67, height=92):0,0,0,0
				 - IRectangle(x=157, y=108, width=43, height=92):0,0,0,0
			""".trimIndent(),
			arrayListOf<String>().apply {
				val log = this
				for (rect in listOf(RectangleInt(0, 0, 512, 256), RectangleInt(0, 0, 256, 512), RectangleInt(0, 0, 100, 100), RectangleInt(0, 0, 0, 0))) {
					log += "$rect:"
					ninePatch.info.computeScale(rect) { seg, x, y, width, height ->
						log += " - ${seg.rect}:$x,$y,$width,$height"
					}
				}
			}.joinToString("\n")
		)

		//val bmp = NativeImage(512, 256)
		//val bmp = NativeImage(202, 202)

		//for (segment in ninePatch.segments.flatMap { it }) showImageAndWait(segment.bmp)

		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 202, 202))
		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 512, 202))
		//ninePatch.drawTo(bmp, RectangleInt.fromBounds(0, 0, 32, 202))

		//showImageAndWait(ninePatch.rendered(800, 800))
		//showImageAndWait(ninePatch.rendered(512, 256))
		//showImageAndWait(ninePatch.rendered(256, 512))
		//showImageAndWait(ninePatch.rendered(100, 100))
		//showImageAndWait(ninePatch.rendered(32, 100))
		//bmp.writeTo("/tmp/file.tga".uniVfs, defaultImageFormats)
	}
}