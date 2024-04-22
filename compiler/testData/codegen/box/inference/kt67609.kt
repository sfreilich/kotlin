// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: WASM
class In<in K>

fun <E> intersect(vararg x: In<E>): E = null as E

fun box(): String { // Ignore K2 Wasm because it fails at runtime ¯\_(ツ)_/¯
    val a = intersect(In<Int>(), In<String>())
    val b = intersect(In<Int>(), *arrayOf(In<String>()))
    val c = intersect(x = arrayOf(In<Int>(), In<String>())) // K1: TYPE_MISMATCH
    val d = intersect(x = *arrayOf(In<Int>(), In<String>())) // K1: TYPE_MISMATCH
    val e = intersect(In<Int>(), *arrayOf(In<String>()), In<Long>())
    val f = intersect(*arrayOf(In<Int>()), In<String>(), *arrayOf(In<Long>()))
    return if (a == null && a == b && b == c && c == d && d == e && e == f) "OK" else "NOT_OK"
}
