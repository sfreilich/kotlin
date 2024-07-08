// MODULE: lib
// FILE: A.kt
class A {
    private operator fun plus(increment: Int): String = "OK"

    internal inline fun internalInlineMethod() = this + 1
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod()
}
