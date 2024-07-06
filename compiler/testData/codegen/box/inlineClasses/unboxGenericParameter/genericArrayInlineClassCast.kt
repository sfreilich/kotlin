// ISSUE: KT-67409
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: ANDROID
// ^ KT-52706
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class KoneArray<E>(val array: Array<out E>)

fun KoneArray<Int>.raw0(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<Int>.raw1(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<Int>.raw2(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<Int>.raw3(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun box(): String {
    val koneArray = KoneArray(Array(10) { it })
    koneArray.raw0().toList().let { require(it == (0..9).toList()) { it.toString() } }
    koneArray.raw1()?.toList().let { require(it == (0..9).toList()) { it.toString() } }
    koneArray.raw2()?.toList().let { require(it == (0..9).toList()) { it.toString() } }
    koneArray.raw3()?.toList().let { require(it == (0..9).toList()) { it.toString() } }
    return "OK"
}
