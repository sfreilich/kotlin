@kotlin.SinceKotlin(version = "1.4")
@kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Provided for expect-actual matching")
@kotlin.internal.InlineOnly
public inline fun CancellationException(message: kotlin.String?, cause: kotlin.Throwable?): kotlin.coroutines.cancellation.CancellationException

@kotlin.SinceKotlin(version = "1.4")
@kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Provided for expect-actual matching")
@kotlin.internal.InlineOnly
public inline fun CancellationException(cause: kotlin.Throwable?): kotlin.coroutines.cancellation.CancellationException

@kotlin.SinceKotlin(version = "1.4")
public open class CancellationException : kotlin.IllegalStateException {
    public constructor CancellationException()

    public constructor CancellationException(message: kotlin.String?)

    public constructor CancellationException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor CancellationException(cause: kotlin.Throwable?)
}