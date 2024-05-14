/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

class ExpectTopLevelDeclarationCollector {
    companion object {
        fun collect(fragments: List<IrModuleFragment>): ExpectTopLevelDeclarations {
            val collector = ExpectTopLevelDeclarationCollector()
            collector.collect(fragments)
            return ExpectTopLevelDeclarations(collector.expectTopLevelClasses, collector.expectTopLevelCallables)
        }
    }

    private val expectTopLevelClasses = mutableMapOf<ClassId, IrClassSymbol>()
    private val expectTopLevelCallables = mutableMapOf<CallableId, MutableList<IrSymbol>>()

    fun collect(fragments: List<IrModuleFragment>) {
        for (fragment in fragments) {
            for (file in fragment.files) {
                for (declaration in file.declarations) {
                    if (declaration.isExpect && declaration.isTopLevel) {
                        fun addCallable(callableId: CallableId) {
                            val list = expectTopLevelCallables.getOrPut(callableId) { mutableListOf() }
                            list.add(declaration.symbol)
                        }

                        when (declaration) {
                            is IrClass -> expectTopLevelClasses[declaration.classIdOrFail] = declaration.symbol
                            is IrProperty -> addCallable(declaration.callableId)
                            is IrFunction -> addCallable(declaration.callableId)
                        }
                    }
                }
            }
        }
    }
}

class ExpectTopLevelDeclarations(val classes: Map<ClassId, IrClassSymbol>, val callables: Map<CallableId, List<IrSymbol>>)