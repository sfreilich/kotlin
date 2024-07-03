// KIND: STANDALONE
// MODULE: Char
// FILE: char.kt

private val string = "AB0-Ыß☺\uD83D\uDE42系"

fun getCharAt(index: Int): Char = string.get(index)

fun isEqualToCharAt(c: Char, index: Int): Boolean = (c == string.get(index))
