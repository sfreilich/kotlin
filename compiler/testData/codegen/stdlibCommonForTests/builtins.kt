// Some companion objects in builtin classes contain constants.
// Despite the fact it's possible to declare `val` in `expect` class and
// `const val` in actual, we need those constants in expect classes anyway
// to get rid of potential errors with metadata compilation.
@file:Suppress("EXPECTED_PROPERTY_INITIALIZER")

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
expect interface Annotation

@ActualizeByJvmBuiltinProvider
expect open class Any() {
    public open operator fun equals(other: Any?): Boolean

    public open fun hashCode(): Int

    public open fun toString(): String
}

@ActualizeByJvmBuiltinProvider
expect class Boolean {
    public operator fun not(): Boolean
}

@ActualizeByJvmBuiltinProvider
expect class Int {
    companion object {
        const val MIN_VALUE: Int = -2147483648
        const val MAX_VALUE: Int = 2147483647
    }

    //@kotlin.internal.IntrinsicConstEvaluation
    public operator fun minus(other: Int): Int

    public operator fun rangeTo(other: kotlin.Int): kotlin.ranges.IntRange
}

@ActualizeByJvmBuiltinProvider
expect class String private constructor()

@ActualizeByJvmBuiltinProvider
public expect abstract class Enum<E : Enum<E>>(name: String, ordinal: Int) : Comparable<E> {
    override final fun compareTo(other: E): Int

    override final fun equals(other: Any?): Boolean

    override final fun hashCode(): Int
}

@ActualizeByJvmBuiltinProvider
public expect open class Throwable() {
    public open val message: String?
    public open val cause: Throwable?

    public constructor(message: String?)

    public constructor(cause: Throwable?)
}

@ActualizeByJvmBuiltinProvider
expect class Float

@ActualizeByJvmBuiltinProvider
expect class Double

@ActualizeByJvmBuiltinProvider
expect class Nothing

@ActualizeByJvmBuiltinProvider
expect class Char

@ActualizeByJvmBuiltinProvider
expect class Byte

@ActualizeByJvmBuiltinProvider
expect class Short

@ActualizeByJvmBuiltinProvider
expect class Long

object Unit

@ActualizeByJvmBuiltinProvider
expect interface CharSequence

@ActualizeByJvmBuiltinProvider
expect abstract class Number

@ActualizeByJvmBuiltinProvider
expect class Array<T> {
    final val size: kotlin.Int

    final operator fun get(index: kotlin.Int): T

    final operator fun iterator(): kotlin.collections.Iterator<T>
}

/*@ActualizeByJvmBuiltinProvider
public expect class IntArray(size: Int) {
    @Suppress("WRONG_MODIFIER_TARGET")
    public inline constructor(size: Int, init: (Int) -> Int)
}*/

@ActualizeByJvmBuiltinProvider
public expect fun Any?.toString(): String

@ActualizeByJvmBuiltinProvider
public expect operator fun String?.plus(other: Any?): String

@ActualizeByJvmBuiltinProvider
public expect fun intArrayOf(vararg elements: Int): IntArray

@ActualizeByJvmBuiltinProvider
@SinceKotlin("1.1")
public expect inline fun <reified T : Enum<T>> enumValues(): Array<T>
