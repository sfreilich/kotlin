/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.fir.backend.IrBuiltInsOverFir
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.CallableId

class IrBuiltinsExtraActualDeclarationExtractor(
    private val extraExpectBuiltinsDeclarations: List<IrDeclarationWithName>,
    private val platformIrBuiltIns: IrBuiltInsOverFir,
) : IrExtraActualDeclarationExtractor() {
    init {
        for (declaration in extraExpectBuiltinsDeclarations) {
            when (declaration) {
                is IrClass -> {
                    platformIrBuiltIns.findClass(declaration.name, declaration.packageFqName!!)

                }
            }
        }
    }

    /*private val extraActualClasses: Map<ClassId, IrClassSymbol> =
        extraActualDeclarations.filterIsInstance<IrClass>().associate { it.classIdOrFail to it.symbol }

    private val extraActualCallables: Map<CallableId, List<IrSymbol>> =
        buildMap<CallableId, MutableList<IrSymbol>> {
            for (expectActualDeclaration in extraActualDeclarations) {
                when (expectActualDeclaration) {
                    is IrFunction -> getOrPut(expectActualDeclaration.callableId) { mutableListOf() }
                        .add(expectActualDeclaration.symbol)
                    is IrProperty -> getOrPut(expectActualDeclaration.callableId) { mutableListOf() }
                        .add(expectActualDeclaration.symbol)
                    else -> null
                }
            }
        }

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        require(expectIrClass.isExpect)
        return extraActualClasses[expectIrClass.classIdOrFail]
    }

    override fun extract(
        expectTopLevelCallables: List<IrDeclarationWithName>,
        expectCallableId: CallableId,
    ): List<IrSymbol> {
        require(expectTopLevelCallables.all { it.isExpect && it.isTopLevel })
        return extraActualCallables[expectCallableId] ?: emptyList()
    }*/

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        TODO("Not yet implemented")
    }

    override fun extract(
        expectTopLevelCallables: List<IrDeclarationWithName>,
        expectCallableId: CallableId,
    ): List<IrSymbol> {
        TODO("Not yet implemented")
    }
}