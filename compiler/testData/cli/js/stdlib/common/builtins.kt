package kotlin

expect open class Any() {
    public open fun toString(): String
}

expect class Float private constructor()

expect class Double private constructor()

expect class Boolean private constructor()

expect class Nothing private constructor()

expect class String

expect class Int

expect class Char private constructor()

expect class Byte private constructor()

expect class Short private constructor()

expect class Long

object Unit

expect interface CharSequence

expect class Number

expect class Array<T>
