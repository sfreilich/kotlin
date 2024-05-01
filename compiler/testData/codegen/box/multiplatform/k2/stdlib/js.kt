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

// FILE: builtins.kt

@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "MUST_BE_INITIALIZED_OR_BE_ABSTRACT", "NON_MEMBER_FUNCTION_NO_BODY", "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED")

package kotlin

actual interface Annotation

actual open class Any actual constructor() {
    actual open operator fun equals(other: Any?): Boolean

    actual open fun hashCode(): Int

    actual open fun toString(): String
}

actual class Boolean

actual class Int {
    actual companion object {
        actual const val MIN_VALUE: Int = -2147483648
        actual const val MAX_VALUE: Int = 2147483647
    }
}

actual class String

actual interface Comparable<in T> {
    actual operator fun compareTo(other: T): Int
}

actual abstract class Enum<E : Enum<E>> actual constructor(name: String, ordinal: Int) : Comparable<E> {
    actual override final fun compareTo(other: E): Int

    actual override final fun equals(other: Any?): Boolean

    actual override final fun hashCode(): Int
}

actual open class Throwable actual constructor() {
    actual open val message: String?
    actual open val cause: Throwable?

    actual constructor(message: String?)

    actual constructor(cause: Throwable?)
}

actual class IntArray actual constructor (size: Int) {
    @Suppress("WRONG_MODIFIER_TARGET")
    actual inline constructor(size: Int, init: (Int) -> Int)
}

actual fun Any?.toString(): String

actual operator fun String?.plus(other: Any?): String

actual fun intArrayOf(vararg elements: Int): IntArray

@SinceKotlin("1.1")
actual inline fun <reified T : Enum<T>> enumValues(): Array<T>

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
