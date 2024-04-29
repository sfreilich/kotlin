package kotlin.collections

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
expect interface Iterator<out T> {
    public operator fun next(): T
    /**
     * Returns `true` if the iteration has more elements.
     */
    public operator fun hasNext(): Boolean
}

@ActualizeByJvmBuiltinProvider
expect abstract class ByteIterator : Iterator<Byte>

@ActualizeByJvmBuiltinProvider
expect abstract class CharIterator : Iterator<Char>

@ActualizeByJvmBuiltinProvider
expect abstract class ShortIterator : Iterator<Short>

@ActualizeByJvmBuiltinProvider
expect abstract class IntIterator : Iterator<Int> {
}

@ActualizeByJvmBuiltinProvider
expect abstract class LongIterator : Iterator<Long>

@ActualizeByJvmBuiltinProvider
expect abstract class FloatIterator : Iterator<Float>

@ActualizeByJvmBuiltinProvider
expect abstract class DoubleIterator : Iterator<Double>

@ActualizeByJvmBuiltinProvider
expect abstract class BooleanIterator : Iterator<Boolean>

class IndexedValue<out T>(public val index: Int, public val value: T)