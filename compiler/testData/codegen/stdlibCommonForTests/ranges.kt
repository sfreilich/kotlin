package kotlin.ranges

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
expect interface ClosedRange<T: Comparable<T>>

@ActualizeByJvmBuiltinProvider
expect class CharRange : CharProgression

@ActualizeByJvmBuiltinProvider
expect class IntRange : IntProgression

@ActualizeByJvmBuiltinProvider
expect class LongRange : LongProgression

/*@ActualizeByJvmBuiltinProvider
expect open class CharProgression : Iterable<Char> {
    override operator fun iterator(): CharIterator
}*/

@ActualizeByJvmBuiltinProvider
expect open class IntProgression : Iterable<Int> {
    override operator fun iterator(): IntIterator
}

/*@ActualizeByJvmBuiltinProvider
expect open class LongProgression : Iterable<Long> {
    override operator fun iterator(): LongIterator
}*/

@ActualizeByJvmBuiltinProvider
expect internal class IntProgressionIterator(first: Int, last: Int, step: Int) : IntIterator
