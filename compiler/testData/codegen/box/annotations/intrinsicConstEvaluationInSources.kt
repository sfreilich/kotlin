// ISSUE: KT-65415, KT-66432

// FILE: IntrinsicConstEvaluation.kt
package kotlin.internal

annotation class IntrinsicConstEvaluation

// FILE: usage.kt
import kotlin.internal.IntrinsicConstEvaluation

@IntrinsicConstEvaluation
fun test(): String = "OK"

fun box(): String = test()
