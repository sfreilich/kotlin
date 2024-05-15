// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST -DEPRECATION -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

// FILE: JavaSubIn.java

public class JavaSubIn<T> extends In<T> {}


// FILE: main.kt
open class Out<out K>
open class In<in K>

class ImplicitIn<T>(prop: T) : In<T>()

open class Super
class Sub: Super()

class SubJava<T> : JavaSubIn<T>()

fun <E> intersectIn(vararg x: In<E>): E = null!!
fun <E> intersectOut(vararg x: Out<E>): E = null!!

fun <E> In<E>.intersectWithExtension(vararg x: In<E>): E = null!!

fun <E> intersectWithValue(y: In<E>, vararg x: In<E>): E = null!!

open class Inv<K>

fun <E> intersectWithProjection(invariant: Inv<in E>, vararg x: In<E>): E = null!!
fun <E> intersectWithProjection1(invariant: Inv<in E>, vararg x: Inv<in E>): E = null!!

fun <E, A : E> sequentialTypeVariableDependency(vararg x: In<A>): E = null!!

fun <E, A : E, B : E> parallelTypeVariableDependency(y: In<A>, vararg x: In<B>): E = null!!

fun main(){
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectIn<!>(In<Int>(), In<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectIn<!>(SubJava<Int>(), In<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectIn<!>(SubJava<Int>(), SubJava<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectIn<!>(JavaSubIn<Int>(), In<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectIn<!>(ImplicitIn(1), In<String>())

    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>sequentialTypeVariableDependency<!>(In<Int>(), In<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>parallelTypeVariableDependency<!>(In<Int>(), In<String>())


    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectWithValue<!>(In<Int>(), In<String>())

    In<Int>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectWithExtension<!>(In<String>())


    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectWithProjection<!>(Inv<Int>(), In<String>())
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>intersectWithProjection1<!>(Inv<Int>(), Inv<String>())


    intersectIn(SubJava<Int>(), In<Int>())
    intersectIn(JavaSubIn<Int>(), In<Int>())
    intersectIn(ImplicitIn(1), In<Int>())
    intersectIn(ImplicitIn(1), In())
    intersectIn(In<Int>(), In<Int>())
    intersectIn(In<Int>(), In())
    intersectIn<Int>(In(), In())
    intersectIn<Sub>(In<Super>(), In())
    intersectIn<Sub>(In(), In<Super>())
    intersectIn<Sub>(In<Sub>(), In<Super>())
    intersectIn(In<Sub>(), In<Super>())

    sequentialTypeVariableDependency(In(), In<Int>())
    parallelTypeVariableDependency(In(), In<String>())


    intersectWithValue(In(), In<String>())

    In<Int>().intersectWithExtension(In())


    intersectWithProjection(Inv<Int>(), In())
    intersectWithProjection1(Inv(), Inv<String>())
}