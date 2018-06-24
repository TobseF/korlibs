// @WARNING: File AUTOGENERATED by `korlibs/src/test/kotlin/KmemGenerator.kt` @ korlibs/kmem do not modify manually!
// @TODO: USELESS_CAST is required since it requires a cast to work, but IDE says that that cast is not necessary
@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER", "RedundantUnitReturnType", "FunctionName", "USELESS_CAST")
package com.soywiz.kmem

actual class MemBuffer(val size: Int)
actual fun MemBufferAlloc(size: Int): MemBuffer = TODO()
actual fun MemBufferWrap(array: ByteArray): MemBuffer = TODO()
actual inline val MemBuffer.size: Int get() = TODO()

actual fun MemBuffer._sliceInt8Buffer(offset: Int, size: Int): Int8Buffer = TODO()
actual fun MemBuffer._sliceInt16Buffer(offset: Int, size: Int): Int16Buffer = TODO()
actual fun MemBuffer._sliceInt32Buffer(offset: Int, size: Int): Int32Buffer = TODO()
actual fun MemBuffer._sliceFloat32Buffer(offset: Int, size: Int): Float32Buffer = TODO()
actual fun MemBuffer._sliceFloat64Buffer(offset: Int, size: Int): Float64Buffer = TODO()

actual typealias DataBuffer = MemBuffer
actual fun MemBuffer.getData(): DataBuffer = TODO()
actual fun DataBuffer.getByte(index: Int): Byte = TODO()
actual fun DataBuffer.setByte(index: Int, value: Byte): Unit = TODO()
actual fun DataBuffer.getShort(index: Int): Short = TODO()
actual fun DataBuffer.setShort(index: Int, value: Short): Unit = TODO()
actual fun DataBuffer.getInt(index: Int): Int = TODO()
actual fun DataBuffer.setInt(index: Int, value: Int): Unit = TODO()
actual fun DataBuffer.getFloat(index: Int): Float = TODO()
actual fun DataBuffer.setFloat(index: Int, value: Float): Unit = TODO()
actual fun DataBuffer.getDouble(index: Int): Double = TODO()
actual fun DataBuffer.setDouble(index: Int, value: Double): Unit = TODO()

actual class Int8Buffer(val mbuffer: MemBuffer)
actual val Int8Buffer.mem: MemBuffer get() = TODO()
actual val Int8Buffer.offset: Int get() = TODO()
actual val Int8Buffer.size: Int get() = TODO()
actual operator fun Int8Buffer.get(index: Int): Byte = TODO()
actual operator fun Int8Buffer.set(index: Int, value: Byte): Unit = TODO()

actual class Int16Buffer(val mbuffer: MemBuffer)
actual val Int16Buffer.mem: MemBuffer get() = TODO()
actual val Int16Buffer.offset: Int get() = TODO()
actual val Int16Buffer.size: Int get() = TODO()
actual operator fun Int16Buffer.get(index: Int): Short = TODO()
actual operator fun Int16Buffer.set(index: Int, value: Short): Unit = TODO()

actual class Int32Buffer(val mbuffer: MemBuffer)
actual val Int32Buffer.mem: MemBuffer get() = TODO()
actual val Int32Buffer.offset: Int get() = TODO()
actual val Int32Buffer.size: Int get() = TODO()
actual operator fun Int32Buffer.get(index: Int): Int = TODO()
actual operator fun Int32Buffer.set(index: Int, value: Int): Unit = TODO()

actual class Float32Buffer(val mbuffer: MemBuffer)
actual val Float32Buffer.mem: MemBuffer get() = TODO()
actual val Float32Buffer.offset: Int get() = TODO()
actual val Float32Buffer.size: Int get() = TODO()
actual operator fun Float32Buffer.get(index: Int): Float = TODO()
actual operator fun Float32Buffer.set(index: Int, value: Float): Unit = TODO()

actual class Float64Buffer(val mbuffer: MemBuffer)
actual val Float64Buffer.mem: MemBuffer get() = TODO()
actual val Float64Buffer.offset: Int get() = TODO()
actual val Float64Buffer.size: Int get() = TODO()
actual operator fun Float64Buffer.get(index: Int): Double = TODO()
actual operator fun Float64Buffer.set(index: Int, value: Double): Unit = TODO()

actual fun <T> arraycopy(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, size: Int): Unit = TODO()
actual fun  arraycopy(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int): Unit = TODO()
actual fun  arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Unit = TODO()

actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: ByteArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: ShortArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: IntArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: FloatArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: DoubleArray, srcPos: Int, dst: MemBuffer, dstPos: Int, size: Int): Unit = TODO()
actual fun arraycopy(src: MemBuffer, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Unit = TODO()

@PublishedApi internal actual fun <T> _fill(array: Array<T>, value: T, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun  _fill(array: BooleanArray, value: Boolean, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun  _fill(array: LongArray, value: Long, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun _fill(array: ByteArray, value: Byte, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun _fill(array: ShortArray, value: Short, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun _fill(array: IntArray, value: Int, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun _fill(array: FloatArray, value: Float, start: Int, end: Int): Unit = TODO()
@PublishedApi internal actual fun _fill(array: DoubleArray, value: Double, start: Int, end: Int): Unit = TODO()