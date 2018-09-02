package com.soywiz.std

import kotlin.native.*
import kotlin.native.concurrent.*

actual typealias ThreadLocal = kotlin.native.ThreadLocal

// Until in kotlin.native. Temporarily From: https://github.com/JetBrains/kotlin-native/pull/1769

internal object UNINITIALIZED
internal object INITIALIZING

//@kotlin.native.internal.Frozen
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
	private val initializer_ = kotlin.native.concurrent.AtomicReference<Function0<T>?>(initializer.freeze())
	private val value_ = kotlin.native.concurrent.AtomicReference<Any?>(UNINITIALIZED)

	override val value: T
		get() {
			if (value_.compareAndSwap(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
				// We execute exclusively here.
				val ctor = initializer_.get()
				if (ctor != null && initializer_.compareAndSwap(ctor, null) === ctor) {
					value_.compareAndSwap(INITIALIZING, ctor().freeze())
				} else {
					// Something wrong.
					assert(false)
				}
			}
			var result: Any?
			do {
				result = value_.get()
			} while (result === INITIALIZING)

			assert(result !== UNINITIALIZED && result != INITIALIZING)
			@Suppress("UNCHECKED_CAST")
			return result as T
		}

	// Racy!
	override fun isInitialized(): Boolean = value_.get() !== UNINITIALIZED

	override fun toString(): String = if (isInitialized())
		value_.get().toString() else "Lazy value not initialized yet."
}

/**
 * Atomic lazy initializer, could be used in frozen objects, freezes initializing lambda,
 * so use very carefully.
 */
actual public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)

actual typealias AtomicReference<T> = kotlin.native.concurrent.AtomicReference<T>

actual fun <T> NewAtomicReference(value: T): AtomicReference<T> = kotlin.native.concurrent.AtomicReference<T>(value)

actual fun AtomicInt.compareAndSet(expected: Int, newValue: Int): Boolean =
	compareAndSwap(expected, newValue) == expected

actual fun <T> AtomicReference<T>.set(value: T) {
	val fvalue = value.freeze()
	if (this.get() != fvalue) {
		while (this.compareAndSwap(this.get(), fvalue) != fvalue) {
			// spinlock
		}
	}

}

actual fun <T> AtomicReference<T>.get(): T {
	return (this as kotlin.native.concurrent.AtomicReference<T>).get() as T
}



actual typealias AtomicInt = kotlin.native.concurrent.AtomicInt

actual fun NewAtomicInt(value: Int): AtomicInt = kotlin.native.concurrent.AtomicInt(value)

actual fun AtomicInt.addAndGet(delta: Int): Int {
	return (this as kotlin.native.concurrent.AtomicInt).addAndGet(delta)
}

actual fun AtomicInt.set(value: Int) {
	(this as kotlin.native.concurrent.AtomicInt).compareAndSwap(this.get(), value)
}
actual fun AtomicInt.get(): Int {
	return (this as kotlin.native.concurrent.AtomicInt).get()
}




actual typealias AtomicLong = kotlin.native.concurrent.AtomicLong

actual fun NewAtomicLong(value: Long): AtomicLong = kotlin.native.concurrent.AtomicLong(value)

actual fun AtomicLong.addAndGet(delta: Long): Long {
	return (this as kotlin.native.concurrent.AtomicLong).addAndGet(delta)
}

actual fun AtomicLong.set(value: Long) {
	(this as kotlin.native.concurrent.AtomicLong).compareAndSwap(this.get(), value)
}
actual fun AtomicLong.get(): Long {
	return (this as kotlin.native.concurrent.AtomicLong).get()
}


actual val isNative: Boolean = true
actual val isJs: Boolean = false
actual val isJvm: Boolean = false

//@UseExperimental(ExperimentalContracts::class)
actual inline fun <R> synchronized2(lock: Any, block: () -> R): R {
	//contract {
	//	callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	//}
	return block()
}
