class A {
    companion object
}

fun test() {
    <expr>A</expr>::class.java.getDeclaredField("name")
}