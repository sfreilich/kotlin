package kotlin.collections

interface Iterator<out T>

class ByteIterator<Byte>
class CharIterator<Char>
class ShortIterator<Short>
class IntIterator<Int>
class LongIterator<Long>
class FloatIterator<Float>
class DoubleIterator<Double>
class BooleanIterator<Boolean>

public data class IndexedValue<out T>(public val index: Int, public val value: T)