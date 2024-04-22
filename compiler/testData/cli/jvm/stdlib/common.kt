// ISSUE: KT-65841

@file:Suppress("EXPECTED_PROPERTY_INITIALIZER")

package kotlin

internal annotation class ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
expect open class Any() {
    public open operator fun equals(other: Any?): Boolean

    public open fun hashCode(): Int

    public open fun toString(): String
}

@ActualizeByJvmBuiltinProvider
expect class Boolean

@ActualizeByJvmBuiltinProvider
expect class Int {
    companion object {
        const val MIN_VALUE: Int = -2147483648
        const val MAX_VALUE: Int = 2147483647
    }
}

@ActualizeByJvmBuiltinProvider
expect class String

@ActualizeByJvmBuiltinProvider
public expect abstract class Enum<E : Enum<E>>(name: String, ordinal: Int) : Comparable<E> {
}

enum class TestEnumInCommon {
    A, B, C
}

@ActualizeByJvmBuiltinProvider
public expect open class Throwable() {
    public open val message: String?
    public open val cause: Throwable?

    public constructor(message: String?)

    public constructor(cause: Throwable?)
}

@ActualizeByJvmBuiltinProvider
public expect class IntArray(size: Int) {
    @Suppress("WRONG_MODIFIER_TARGET")
    public inline constructor(size: Int, init: (Int) -> Int)
}

annotation class AnnotationWithInt(val value: Int)

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInCommon

@ActualizeByJvmBuiltinProvider
public expect fun Any?.toString(): String

@ActualizeByJvmBuiltinProvider
public expect operator fun String?.plus(other: Any?): String

@ActualizeByJvmBuiltinProvider
@Suppress("EXPECT_ACTUAL_INCOMPATIBILITY") // Probably some backends require `inline` modifier
public expect fun intArrayOf(vararg elements: Int): IntArray

@ActualizeByJvmBuiltinProvider
@SinceKotlin("1.1")
public expect inline fun <reified T : Enum<T>> enumValues(): Array<T>

fun testStringPlusInCommon() = "asdf" + 42
fun testIntArrayOf() = intArrayOf(1, 2, 3)

fun testInCommon(): @ExtensionFunctionType (String.() -> Int) = null!!
