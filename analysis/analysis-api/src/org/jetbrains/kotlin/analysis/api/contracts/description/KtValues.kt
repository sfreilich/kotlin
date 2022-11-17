/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionValue]
 */
public sealed class KtContractDescriptionValue(
    private val _name: String,
    override val token: KtLifetimeToken
) : KtContractDescriptionElement {
    public val name: String get() = withValidityAssertion { _name }
}

public sealed class KtAbstractConstantReference(name: String, token: KtLifetimeToken) : KtContractDescriptionValue(name, token) {
    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.ConstantReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference]
     */
    public class KtConstantReference(name: String, token: KtLifetimeToken) : KtAbstractConstantReference(name, token)

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference]
     */
    public sealed class KtBooleanConstantReference(name: String, token: KtLifetimeToken) :
        KtAbstractConstantReference(name, token), KtBooleanExpression {
        public class KtTrue(token: KtLifetimeToken) : KtBooleanConstantReference("TRUE", token)
        public class KtFalse(token: KtLifetimeToken) : KtBooleanConstantReference("FALSE", token)
    }
}

public sealed class KtAbstractValueParameterReference(
    private val _parameterIndex: Int,
    name: String,
    token: KtLifetimeToken
) : KtContractDescriptionValue(name, token) {
    public val parameterIndex: Int get() = withValidityAssertion { _parameterIndex }

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.VariableReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference]
     */
    public class KtValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtAbstractValueParameterReference(parameterIndex, name, token)

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanValueParameterReference]
     */
    public class KtBooleanValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtAbstractValueParameterReference(parameterIndex, name, token), KtBooleanExpression
}
