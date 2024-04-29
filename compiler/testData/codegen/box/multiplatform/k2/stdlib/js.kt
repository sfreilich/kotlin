// TARGET_BACKEND: JS
// LANGUAGE: +MultiPlatformProjects
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// TODO (?): KT-67753
// ISSUE: KT-65841
// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION

// MODULE: common
// TARGET_PLATFORM: Common

// MODULE: platform()()(common)

// FILE: arrays.kt

package kotlin

import kotlin.ranges.CharProgression

public actual class BooleanArray

public actual class CharArray

public actual class ByteArray

public actual class ShortArray

public actual class IntArray

public actual class LongArray

public actual class FloatArray

public actual class DoubleArray

// FILE: builtins.kt

@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "MUST_BE_INITIALIZED_OR_BE_ABSTRACT")

package kotlin

actual open class Any actual constructor() {
    public actual open operator fun equals(other: Any?): Boolean

    public actual open fun hashCode(): Int

    public actual open fun toString(): String
}

actual class Float private constructor()

actual class Double private constructor()

//@kotlin.internal.IntrinsicConstEvaluation
actual class Boolean private constructor() {
    actual operator fun not(): Boolean
}

actual class Nothing private constructor()

actual class String

actual class Int private constructor() {
    actual companion object {
        actual const val MIN_VALUE: Int = -2147483648
        actual const val MAX_VALUE: Int = 2147483647
    }

    //@kotlin.internal.IntrinsicConstEvaluation
    public actual operator fun minus(other: Int): Int

    public actual operator fun rangeTo(other: kotlin.Int): kotlin.ranges.IntRange
}

actual class Char private constructor()

actual class Byte private constructor()

actual class Short private constructor()

actual class Long

actual interface CharSequence

actual abstract class Number

actual class Array<T> {
    actual final val size: kotlin.Int

    actual final operator fun get(index: kotlin.Int): T

    actual final operator fun iterator(): kotlin.collections.Iterator<T>
}

// FILE: collections.kt

@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")

package kotlin.collections

actual interface Iterator<out T> {
    public actual operator fun next(): T
    /**
     * Returns `true` if the iteration has more elements.
     */
    public actual operator fun hasNext(): Boolean
}

actual abstract class ByteIterator : Iterator<Byte>

actual abstract class CharIterator : Iterator<Char>

actual abstract class ShortIterator : Iterator<Short>

actual abstract class IntIterator : Iterator<Int> {
    /*actual final override fun next(): Int = nextInt()

    abstract fun nextInt(): Int*/
}

actual abstract class LongIterator : Iterator<Long>

actual abstract class FloatIterator : Iterator<Float>

actual abstract class DoubleIterator : Iterator<Double>

actual abstract class BooleanIterator : Iterator<Boolean>

// FILE: ranges.kt

package kotlin.ranges

actual interface ClosedRange<T: Comparable<T>>

//actual class CharRange(start: Char, endInclusive: Char) : CharProgression(start, endInclusive, 1)

actual class IntRange(start: Int, endInclusive: Int) : IntProgression(start, endInclusive, 1)

//actual class LongRange(start: Long, endInclusive: Long) : LongProgression(start, endInclusive, 1)

/*actual open class CharProgression
    internal constructor
        (
        start: Char,
        endInclusive: Char,
        step: Int
    ) : Iterable<Char> {
    actual open override operator fun iterator(): CharIterator = CharProgressionIterator(first, last, step)
}*/

actual open class IntProgression
    internal constructor
        (
        start: Int,
        endInclusive: Int,
        step: Int
    ) : Iterable<Int> {

    public val first: Int = start

    public val last: Int = endInclusive

    public val step: Int = step

    actual open override operator fun iterator(): IntIterator = IntProgressionIterator(first, last, step)
}

/*actual open class LongProgression
    internal constructor
        (
        start: Long,
        endInclusive: Long,
        step: Long
    ) : Iterable<Long> {
    actual open override operator fun iterator(): LongIterator = LongProgressionIterator(first, last, step)
}*/

internal actual class IntProgressionIterator actual constructor(first: Int, last: Int, val step: Int) : kotlin.collections.IntIterator() {
    /*private val finalElement: Int = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next: Int = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }*/

    override fun hasNext(): Boolean = false

    override fun next(): Int = 0
}

// FILE: testPlatform.kt

enum class TestEnumInPlatform {
    D, E, F
}

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInPlatform

fun any() = Any()
fun string() = String() + 1
fun boolean() = true
fun int() = 42
fun intArray() = intArrayOf(1, 2, 3)

fun box() = "OK"
