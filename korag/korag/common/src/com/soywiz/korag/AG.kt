package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.std.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

interface AGFactory {
	val supportsNativeFrame: Boolean
	fun create(nativeControl: Any?): AG
	fun createFastWindow(title: String, width: Int, height: Int): AGWindow
}

interface AGContainer {
	val ag: AG
	//data class Resized(var width: Int, var height: Int) {
	//	fun setSize(width: Int, height: Int): Resized = this.apply {
	//		this.width = width
	//		this.height = height
	//	}
	//}

	fun repaint(): Unit
}

abstract class AGWindow : AGContainer {
	abstract override val ag: AG
}

abstract class AG : Extra by Extra.Mixin() {
	var contextVersion = 0
	abstract val nativeComponent: Any

	open val maxTextureSize = Size(2048, 2048)

	open val devicePixelRatio: Double = 1.0

	private val _onReadyDeferred = CompletableDeferred<AG>(Job())
	protected fun ready() {
		//println("AG.ready!")
		__ready()
	}

	fun __ready() {
		_onReadyDeferred.complete(this)
	}

	val onReady: Deferred<AG> = _onReadyDeferred
	val onRender = Signal<AG>()
	val onResized = Signal<Unit>()

	open fun offscreenRendering(callback: () -> Unit) {
		callback()
	}

	open fun repaint() {
	}

	open fun resized() {
		onResized(Unit)
	}

	open fun dispose() {
	}

	val viewport = intArrayOf(0, 0, 640, 480)

	open val backWidth: Int get() = viewport[2]
	open val backHeight: Int get() = viewport[3]

	protected fun getViewport(out: IntArray): IntArray {
		arraycopy(this.viewport, 0, out, 0, 4)
		return out
	}

	open fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		viewport[0] = x
		viewport[1] = y
		viewport[2] = width
		viewport[3] = height
	}

	protected fun setViewport(v: IntArray) = setViewport(v[0], v[1], v[2], v[3])

	enum class BlendEquation {
		ADD, SUBTRACT, REVERSE_SUBTRACT
	}

	enum class BlendFactor {
		DESTINATION_ALPHA,
		DESTINATION_COLOR,
		ONE,
		ONE_MINUS_DESTINATION_ALPHA,
		ONE_MINUS_DESTINATION_COLOR,
		ONE_MINUS_SOURCE_ALPHA,
		ONE_MINUS_SOURCE_COLOR,
		SOURCE_ALPHA,
		SOURCE_COLOR,
		ZERO;
	}

	data class Scissor(
		val x: Int, val y: Int,
		val width: Int, val height: Int
	) {
		val top get() = y
		val left get() = x
		val right get() = x + width
		val bottom get() = y + height
	}

	data class Blending(
		val srcRGB: BlendFactor,
		val dstRGB: BlendFactor,
		val srcA: BlendFactor = srcRGB,
		val dstA: BlendFactor = dstRGB,
		val eqRGB: BlendEquation = BlendEquation.ADD,
		val eqA: BlendEquation = eqRGB
	) {

		constructor(src: BlendFactor, dst: BlendFactor, eq: BlendEquation = BlendEquation.ADD) : this(
			src, dst,
			src, dst,
			eq, eq
		)

		val disabled: Boolean get() = srcRGB == BlendFactor.ONE && dstRGB == BlendFactor.ZERO && srcA == BlendFactor.ONE && dstA == BlendFactor.ZERO
		val enabled: Boolean get() = !disabled

		companion object {
			val NONE = Blending(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)
			val NORMAL = Blending(
				BlendFactor.SOURCE_ALPHA,
				BlendFactor.ONE_MINUS_SOURCE_ALPHA,
				BlendFactor.ONE,
				BlendFactor.ONE_MINUS_SOURCE_ALPHA
			)

			// http://www.learnopengles.com/tag/additive-blending/
			//val REPLACE = Blending(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)
			//val NORMAL = Blending(BlendFactor.SOURCE_ALPHA, BlendFactor.ONE_MINUS_SOURCE_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SOURCE_ALPHA)
			//val ADD = Blending(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE)
			//
			//val REPLACE_PREMULT = Blending(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)
			//val NORMAL_PREMULT = Blending(BlendFactor.ONE, BlendFactor.ONE_MINUS_SOURCE_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SOURCE_ALPHA)
			//val ADD_PREMULT = Blending(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE)
		}
	}

	interface BitmapSourceBase {
		val rgba: Boolean
		val width: Int
		val height: Int
	}

	class SyncBitmapSource(
		override val rgba: Boolean,
		override val width: Int,
		override val height: Int,
		val gen: () -> Bitmap?
	) : BitmapSourceBase {
		companion object {
			val NULL = SyncBitmapSource(true, 0, 0) { null }
		}

		override fun toString(): String = "SyncBitmapSource(rgba=$rgba, width=$width, height=$height)"
	}

	class AsyncBitmapSource(
		val coroutineContext: CoroutineContext,
		override val rgba: Boolean,
		override val width: Int,
		override val height: Int,
		val gen: suspend () -> Bitmap?
	) : BitmapSourceBase {
		companion object {
			val NULL = AsyncBitmapSource(EmptyCoroutineContext, true, 0, 0) { null }
		}
	}

	var lastTextureId = 0
	var createdTextureCount = 0
	var deletedTextureCount = 0

	enum class TextureKind { RGBA, LUMINANCE }

	open inner class Texture : Closeable {
		open val premultiplied = true
		var requestMipmaps = false
		var mipmaps = false; protected set
		var source: BitmapSourceBase = SyncBitmapSource.NULL
		private var uploaded: Boolean = false
		private var generating: Boolean = false
		private var generated: Boolean = false
		private var tempBitmap: Bitmap? = null
		var ready: Boolean = false; private set
		val texId = lastTextureId++

		init {
			createdTextureCount++
		}

		protected fun invalidate() {
			uploaded = false
			generating = false
			generated = false
		}

		fun upload(bmp: Bitmap?, mipmaps: Boolean = false): Texture {
			return upload(
				if (bmp != null) SyncBitmapSource(
					rgba = bmp.bpp > 8,
					width = bmp.width,
					height = bmp.height
				) { bmp } else SyncBitmapSource.NULL, mipmaps)
		}

		fun upload(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): Texture {
			// @TODO: Optimize to avoid copying?
			return upload(bmp?.extract(), mipmaps)
		}

		fun upload(source: BitmapSourceBase, mipmaps: Boolean = false): Texture = this.apply {
			this.source = source
			uploadedSource()
			invalidate()
			this.requestMipmaps = mipmaps
		}

		protected open fun uploadedSource() {
		}

		open fun bind() {
		}

		open fun unbind() {
		}

		fun manualUpload() = this.apply {
			uploaded = true
		}

		fun bindEnsuring() {
			bind()
			val source = this.source
			if (!uploaded) {
				if (!generating) {
					generating = true
					when (source) {
						is SyncBitmapSource -> {
							tempBitmap = source.gen()
							generated = true
						}
						is AsyncBitmapSource -> {
							asyncImmediately(source.coroutineContext) {
								tempBitmap = source.gen()
								generated = true
							}
						}
					}
				}

				if (generated) {
					uploaded = true
					generating = false
					generated = false
					actualSyncUpload(source, tempBitmap, requestMipmaps)
					tempBitmap = null
					ready = true
				}
			}
		}

		open fun actualSyncUpload(source: BitmapSourceBase, bmp: Bitmap?, requestMipmaps: Boolean) {
		}


		init {
			//Console.log("CREATED TEXTURE: $texId")
			//printTexStats()
		}

		private var alreadyClosed = false
		override fun close() {
			if (!alreadyClosed) {
				alreadyClosed = true
				source = SyncBitmapSource.NULL
				tempBitmap = null
				deletedTextureCount++
				//Console.log("CLOSED TEXTURE: $texId")
				//printTexStats()
			}
		}

		private fun printTexStats() {
			//Console.log("create=$createdCount, delete=$deletedCount, alive=${createdCount - deletedCount}")
		}
	}

	data class TextureUnit(
		var texture: AG.Texture? = null,
		var linear: Boolean = true
	)

	open class Buffer(val kind: Kind) : Closeable {
		enum class Kind { INDEX, VERTEX }

		var dirty = false
		protected var mem: KmlNativeBuffer? = null
		protected var memOffset: Int = 0
		protected var memLength: Int = 0

		open fun afterSetMem() {
		}

		fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size): Buffer {
			mem = KmlNativeBuffer(length)
			mem!!.setAlignedArrayInt8(0, data, offset, length)
			memOffset = 0
			memLength = length
			dirty = true
			afterSetMem()
			return this
		}

		fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size): Buffer {
			mem = KmlNativeBuffer(length * 4)
			mem!!.setAlignedArrayFloat32(0, data, offset, length)
			memOffset = 0
			memLength = length * 4
			dirty = true
			afterSetMem()
			return this
		}

		fun upload(data: IntArray, offset: Int = 0, length: Int = data.size): Buffer {
			mem = KmlNativeBuffer(length * 4)
			mem!!.setAlignedArrayInt32(0, data, offset, length)
			memOffset = 0
			memLength = length * 4
			dirty = true
			afterSetMem()
			return this
		}

		fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size): Buffer {
			mem = KmlNativeBuffer(length * 2)
			mem!!.setAlignedArrayInt16(0, data, offset, length)
			memOffset = 0
			memLength = length * 2
			dirty = true
			afterSetMem()
			return this
		}

		fun upload(data: KmlNativeBuffer, offset: Int = 0, length: Int = data.size): Buffer {
			mem = data
			memOffset = offset
			memLength = length
			dirty = true
			afterSetMem()
			return this
		}

		override fun close() {
			mem = null
			memOffset = 0
			memLength = 0
			dirty = true
		}
	}

	enum class DrawType {
		POINTS,
		LINE_STRIP,
		LINE_LOOP,
		LINES,
		TRIANGLES,
		TRIANGLE_STRIP,
		TRIANGLE_FAN,
	}

	val dummyTexture by lazy { createTexture() }

	fun createTexture(): Texture = createTexture(premultiplied = true)
	fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): Texture = createTexture(bmp.premult).upload(bmp, mipmaps)
	fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): Texture =
		createTexture(bmp.premult).upload(bmp, mipmaps)

	fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): Texture =
		createTexture(premultiplied).upload(bmp, mipmaps)

	open fun createTexture(premultiplied: Boolean): Texture = Texture()
	open fun createBuffer(kind: Buffer.Kind) = Buffer(kind)
	fun createIndexBuffer() = createBuffer(Buffer.Kind.INDEX)
	fun createVertexBuffer() = createBuffer(Buffer.Kind.VERTEX)

	fun createIndexBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset) =
		createIndexBuffer().apply {
			upload(data, offset, length)
		}

	fun createIndexBuffer(data: KmlNativeBuffer, offset: Int = 0, length: Int = data.size - offset) =
		createIndexBuffer().apply {
			upload(data, offset, length)
		}

	fun createVertexBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset) =
		createVertexBuffer().apply {
			upload(data, offset, length)
		}

	fun createVertexBuffer(data: KmlNativeBuffer, offset: Int = 0, length: Int = data.size - offset) =
		createVertexBuffer().apply {
			upload(data, offset, length)
		}

	enum class StencilOp {
		DECREMENT_SATURATE,
		DECREMENT_WRAP,
		INCREMENT_SATURATE,
		INCREMENT_WRAP,
		INVERT,
		KEEP,
		SET,
		ZERO;
	}

	enum class TriangleFace {
		FRONT, BACK, FRONT_AND_BACK, NONE;
	}

	enum class CompareMode {
		ALWAYS, EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, NEVER, NOT_EQUAL;
	}

	data class ColorMaskState(
		var red: Boolean = true,
		var green: Boolean = true,
		var blue: Boolean = true,
		var alpha: Boolean = true
	) {
		//val enabled = !red || !green || !blue || !alpha
	}

	data class RenderState(
		var depthFunc: CompareMode = CompareMode.ALWAYS,
		var depthMask: Boolean = true,
		var depthNear: Float = 0f,
		var depthFar: Float = 1f,
		var lineWidth: Float = 1f
	)

	data class StencilState(
		var enabled: Boolean = false,
		var triangleFace: TriangleFace = TriangleFace.FRONT_AND_BACK,
		var compareMode: CompareMode = CompareMode.ALWAYS,
		var actionOnBothPass: StencilOp = StencilOp.KEEP,
		var actionOnDepthFail: StencilOp = StencilOp.KEEP,
		var actionOnDepthPassStencilFail: StencilOp = StencilOp.KEEP,
		var referenceValue: Int = 0,
		var readMask: Int = 0xFF,
		var writeMask: Int = 0xFF
	)

	private val dummyRenderState = RenderState()
	private val dummyStencilState = StencilState()
	private val dummyColorMaskState = ColorMaskState()

	// @TODO: Keep for compatibility
	fun draw(
		vertices: Buffer,
		program: Program,
		type: DrawType,
		vertexLayout: VertexLayout,
		vertexCount: Int,
		indices: Buffer? = null,
		offset: Int = 0,
		blending: Blending = Blending.NORMAL,
		uniforms: UniformValues = UniformValues.EMPTY,
		stencil: StencilState = dummyStencilState,
		colorMask: ColorMaskState = dummyColorMaskState,
		scissor: Scissor? = null
	) = draw(
		vertices,
		program,
		type,
		vertexLayout,
		vertexCount,
		indices,
		offset,
		blending,
		uniforms,
		stencil,
		colorMask,
		dummyRenderState,
		scissor
	)

	open fun draw(
		vertices: Buffer,
		program: Program,
		type: DrawType,
		vertexLayout: VertexLayout,
		vertexCount: Int,
		indices: Buffer? = null,
		offset: Int = 0,
		blending: Blending = Blending.NORMAL,
		uniforms: UniformValues = UniformValues.EMPTY,
		stencil: StencilState = dummyStencilState,
		colorMask: ColorMaskState = dummyColorMaskState,
		renderState: RenderState = dummyRenderState,
		scissor: Scissor? = null
	) {
	}

	protected fun checkBuffers(vertices: AG.Buffer, indices: AG.Buffer) {
		if (vertices.kind != AG.Buffer.Kind.VERTEX) invalidOp("Not a VertexBuffer")
		if (indices.kind != AG.Buffer.Kind.INDEX) invalidOp("Not a IndexBuffer")
	}

	open fun disposeTemporalPerFrameStuff() = Unit

	val frameRenderBuffers = LinkedHashSet<RenderBuffer>()
	val renderBuffers = Pool<RenderBuffer>() { createRenderBuffer() }

	open inner class RenderBuffer : Closeable {
		private var cachedTexVersion = -1
		private var _tex: Texture? = null

		val tex: AG.Texture
			get() {
				if (cachedTexVersion != contextVersion) {
					cachedTexVersion = contextVersion
					_tex = this@AG.createTexture(premultiplied = false).manualUpload()
				}
				return _tex!!
			}

		open fun start(width: Int, height: Int) = Unit
		open fun end() = Unit
		fun readBitmap(bmp: Bitmap32) = this@AG.readColor(bmp)
		fun readDepth(width: Int, height: Int, out: FloatArray): Unit = this@AG.readDepth(width, height, out)
		override fun close() = Unit
	}

	open fun createRenderBuffer() = RenderBuffer()

	fun flip() {
		disposeTemporalPerFrameStuff()
		renderBuffers.free(frameRenderBuffers)
		if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
		flipInternal()
	}

	protected open fun flipInternal() = Unit

	open fun clear(
		color: RGBA = Colors.TRANSPARENT_BLACK,
		depth: Float = 0f,
		stencil: Int = 0,
		clearColor: Boolean = true,
		clearDepth: Boolean = true,
		clearStencil: Boolean = true
	) = Unit

	class RenderTexture(val tex: Texture, val width: Int, val height: Int, val closeAction: () -> Unit) : Closeable {
		override fun close() {
			closeAction()
		}
	}

	var renderingToTexture = false

	inline fun renderToTexture(width: Int, height: Int, callback: () -> Unit): RenderTexture {
		val oldRendering = renderingToTexture
		val oldWidth = backWidth
		val oldHeight = backHeight
		renderingToTexture = true
		try {
			return renderToTextureInternal(width, height, callback)
		} finally {
			renderingToTexture = oldRendering
		}
	}

	inline fun renderToTextureInternal(width: Int, height: Int, callback: () -> Unit): RenderTexture {
		val rb = renderBuffers.alloc()
		frameRenderBuffers += rb
		val oldRendering = renderingToTexture
		renderingToTexture = true

		rb.start(width, height)
		try {
			clear(Colors.TRANSPARENT_BLACK) // transparent
			callback()
		} finally {
			rb.end()
			renderingToTexture = oldRendering
		}
		return RenderTexture(rb.tex, width, height) {
			frameRenderBuffers -= rb
			renderBuffers.free(rb)
		}
	}

	inline fun renderToBitmap(bmp: Bitmap32, callback: () -> Unit) {
		val rb = renderBuffers.alloc()
		val oldRendering = renderingToTexture
		renderingToTexture = true

		rb.start(bmp.width, bmp.height)
		try {
			clear(Colors.TRANSPARENT_BLACK)
			callback()
		} finally {
			rb.readBitmap(bmp)
			rb.end()
			renderingToTexture = oldRendering
			renderBuffers.free(rb)
		}
	}

	inline fun renderToBitmapEx(bmp: Bitmap32, callback: RenderBuffer.() -> Unit) {
		val rb = renderBuffers.alloc()
		val oldRendering = renderingToTexture
		renderingToTexture = true

		rb.start(bmp.width, bmp.height)
		try {
			clear(Colors.TRANSPARENT_BLACK)
			callback(rb)
		} finally {
			rb.readBitmap(bmp)
			rb.end()
			renderingToTexture = oldRendering
			renderBuffers.free(rb)
		}
	}

	//private val drawBmpMat: Matrix4 = Matrix4().setToOrtho(0f, 1f, 1f, 0f, 0f, 1f)
	private val drawBmpMat: Matrix4 = Matrix4().setToOrtho(0f, 0f, 1f, 1f, 0f, 1f)
	private var drawBmpVB: Buffer? = null
	private var drawBmpIB: Buffer? = null
	private var drawBmpTex: Texture? = null
	private var drawBmpTexUnit: TextureUnit? = null
	private val drawBmp_VERTICES = floatArrayOf(
		0f, 0f, 0f, 0f, Float.fromBits(-1),
		1f, 0f, 1f, 0f, Float.fromBits(-1),
		0f, 1f, 0f, 1f, Float.fromBits(-1),
		1f, 1f, 1f, 1f, Float.fromBits(-1)
	)
	private val drawBmp_INDICES = shortArrayOf(0, 1, 2, 1, 2, 3)
	private var drawBmp_UNIFORMS: AG.UniformValues? = null

	fun drawBmp(bitmap: Bitmap32) {
		if (drawBmpVB == null) drawBmpVB = createVertexBuffer()
		if (drawBmpIB == null) drawBmpIB = createIndexBuffer()
		if (drawBmpTex == null) drawBmpTex = createTexture()
		if (drawBmpTexUnit == null) drawBmpTexUnit = AG.TextureUnit(drawBmpTex, linear = false)
		if (drawBmp_UNIFORMS == null) drawBmp_UNIFORMS = AG.UniformValues(
			DefaultShaders.u_ProjMat to drawBmpMat,
			DefaultShaders.u_Tex to drawBmpTexUnit!!
		)
		drawBmpVB?.upload(drawBmp_VERTICES)
		drawBmpIB?.upload(drawBmp_INDICES)
		drawBmpTex?.upload(bitmap, mipmaps = false)
		draw(
			vertices = drawBmpVB!!,
			indices = drawBmpIB!!,
			program = DefaultShaders.PROGRAM_TINTED_TEXTURE,
			type = AG.DrawType.TRIANGLES,
			vertexLayout = DefaultShaders.LAYOUT_DEFAULT,
			vertexCount = 6,
			blending = AG.Blending.NONE,
			uniforms = drawBmp_UNIFORMS!!
		)
	}

	open fun readColor(bitmap: Bitmap32): Unit = TODO()
	open fun readDepth(width: Int, height: Int, out: FloatArray): Unit = TODO()

	//var checkErrors = true
	var checkErrors = false

	class UniformValues() {
		@ThreadLocal
		companion object {
			val EMPTY = UniformValues()
		}

		private val _uniforms = arrayListOf<Uniform>()
		private val _values = arrayListOf<Any>()
		val uniforms = _uniforms as List<Uniform>

		val keys get() = uniforms
		val values = _values as List<Any>

		val size get() = _uniforms.size

		constructor(vararg pairs: Pair<Uniform, Any>) : this() {
			for (pair in pairs) put(pair.first, pair.second)
		}

		fun clear() {
			_uniforms.clear()
			_values.clear()
		}

		fun put(uniform: Uniform, value: Any) {
			for (n in 0 until _uniforms.size) {
				if (_uniforms[n].name == uniform.name) {
					_values[n] = value
					return
				}
			}

			_uniforms.add(uniform)
			_values.add(value)
		}

		fun remove(uniform: Uniform) {
			for (n in 0 until _uniforms.size) {
				if (_uniforms[n].name == uniform.name) {
					_uniforms.removeAt(n)
					_values.removeAt(n)
					return
				}
			}
		}

		override fun toString() = "{" + keys.zip(values).map { "${it.first}=${it.second}" }.joinToString(", ") + "}"
	}
}
