enum class TestEnumInCommon {
    A, B, C
}

annotation class AnnotationWithInt(val value: Int)

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInCommon // Currently it doesn't work with FIR2IR_FAKE_OVERRIDE_GENERATION (KT-67753)

fun testStringPlusInCommon() = "asdf" + 42
fun testIntArrayOf() = intArrayOf(1, 2, 3)