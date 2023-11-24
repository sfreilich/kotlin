// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

inline fun a(block: () -> Unit) {
    b(block)
}

inline fun b(block: () -> Unit) {
    block()
}

// FILE: 2.kt

fun box(): String {
    a {
        1
    }
    return "OK"
}