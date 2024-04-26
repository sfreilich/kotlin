// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// TODO (?): KT-67753
// ISSUE: KT-65841
// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION

// MODULE: common
// TARGET_PLATFORM: Common

// MODULE: platform()()(common)
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

fun initCauseInPlatform() = Throwable().initCause(Throwable()) // `initCause` is not visible in `common` but visible in `platform`

fun box() = "OK"
