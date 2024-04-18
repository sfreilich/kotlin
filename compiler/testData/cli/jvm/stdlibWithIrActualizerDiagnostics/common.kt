// ISSUE: KT-65841

@file:Suppress("INVISIBLE_REFERENCE")

package kotlin

import kotlin.internal.PureReifiable

internal annotation class ActualizeByJvmBuiltinProvider()

@ActualizeByJvmBuiltinProvider
expect class Any() {
    fun invalid()
}

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
@ActualizeByJvmBuiltinProvider
public expect fun <reified @PureReifiable T> arrayOfNulls(size: Int): Array<T?>