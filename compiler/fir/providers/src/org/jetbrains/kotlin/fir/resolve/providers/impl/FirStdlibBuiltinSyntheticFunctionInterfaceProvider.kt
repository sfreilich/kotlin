/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirStdlibBuiltinSyntheticFunctionInterfaceProvider(
    session: FirSession, moduleData: FirModuleData, kotlinScopeProvider: FirKotlinScopeProvider,
) : FirBuiltinSyntheticFunctionInterfaceProvider(session, moduleData, kotlinScopeProvider) {
    companion object {
        fun initializeIfStdlib(
            session: FirSession,
            moduleData: FirModuleData,
            kotlinScopeProvider: FirKotlinScopeProvider
        ): FirStdlibBuiltinSyntheticFunctionInterfaceProvider? {
            return runIf(session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                FirStdlibBuiltinSyntheticFunctionInterfaceProvider(
                    session,
                    moduleData,
                    kotlinScopeProvider,
                )
            }
        }
    }

    private val generatedClassesList = mutableListOf<FirRegularClass>()
    val generatedClasses: List<FirRegularClass>
        get() = generatedClassesList

    override fun createSyntheticFunctionInterface(classId: ClassId, kind: FunctionTypeKind): FirRegularClassSymbol? {
        return super.createSyntheticFunctionInterface(classId, kind)?.also { generatedClassesList.add(it.fir) }
    }

    override fun isExpect(): Boolean {
        return session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) && moduleData.isCommon
    }
}