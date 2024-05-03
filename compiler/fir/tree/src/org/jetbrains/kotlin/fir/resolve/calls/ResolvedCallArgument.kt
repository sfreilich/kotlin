/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef

sealed class ResolvedCallArgument {
    abstract val arguments: List<FirExpression>

    object DefaultArgument : ResolvedCallArgument() {
        override val arguments: List<FirExpression>
            get() = emptyList()

    }

    class SimpleArgument(val callArgument: FirExpression) : ResolvedCallArgument() {
        override val arguments: List<FirExpression>
            get() = listOf(callArgument)

    }

    class VarargArgument(override val arguments: List<FirExpression>) : ResolvedCallArgument()

    class DataargArgument(val namedArguments: CallableReferenceMappedArguments) : ResolvedCallArgument() {
        override val arguments: List<FirExpression>
            get() = namedArguments.values.flatMap { it.arguments }
    }

    class SealedargArgument(val callArgument: FirExpression, val typeRef: FirTypeRef) : ResolvedCallArgument() {
        override val arguments: List<FirExpression>
            get() = listOf(callArgument)
    }
}

typealias CallableReferenceMappedArguments = Map<FirValueParameter, ResolvedCallArgument>