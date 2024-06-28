/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

/**
 * A scope that filters out `expect` callables that have matched `actual` callables.
 */
class FirActualizingMemberScope(private val delegate: FirScope) : FirScope() {
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> = delegate.scopeOwnerLookupNames

    private val callableCache: MutableMap<Name, MutableList<FirCallableSymbol<*>>> = mutableMapOf()

    // All matched `expect` callables should be preserved to make it possible to filter them out later when corresponding actuals are found
    private val ignoredExpectCallables: MutableSet<FirCallableSymbol<*>> = mutableSetOf()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        // No filtering is needed for classifiers because the first classifier always wins, and `actual` classifier is always first in platform source-set
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processCallableSymbolsByName(name, FirScope::processFunctionsByName, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processCallableSymbolsByName(name, FirScope::processPropertiesByName, processor)
    }

    private fun <S : FirCallableSymbol<*>> processCallableSymbolsByName(
        name: Name,
        processingFactory: FirScope.(Name, (S) -> Unit) -> Unit,
        processor: (S) -> Unit,
    ) {
        delegate.processingFactory(name) { symbol ->
            if (symbol.isActual) {
                val matchedExpectCallableSymbol = symbol.getSingleMatchedExpectForActualOrNull()
                if (matchedExpectCallableSymbol != null) {
                    val existingSymbols = callableCache[symbol.name]
                    existingSymbols?.removeAll { it === matchedExpectCallableSymbol } // Filter out matched found expects candidates
                    ignoredExpectCallables.add(matchedExpectCallableSymbol as FirCallableSymbol<*>)
                }
            } else if (symbol.isExpect) {
                if (ignoredExpectCallables.contains(symbol)) {
                    // Skip the found `expect` because there is already a matched actual
                    return@processingFactory
                }
            }

            processor(symbol)
            callableCache.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
        }
    }
}