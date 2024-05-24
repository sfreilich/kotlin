/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.ExpectTopLevelDeclarations
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.CallableId

class IrSyntheticExtraActualDeclarationExtractor(
    expectTopLevelDeclarations: ExpectTopLevelDeclarations,
    private val irBuiltIns: IrBuiltIns,
) : IrExtraActualDeclarationExtractor(expectTopLevelDeclarations) {
    override fun extract(expectIrClass: IrClass): IrClassSymbol {
        return irBuiltIns.findClass(expectIrClass.name, expectIrClass.packageFqName!!)!!
    }

    override fun extract(expectTopLevelCallables: List<IrDeclarationWithName>, expectCallableId: CallableId): List<IrSymbol> {
        return irBuiltIns.findFunctions(expectCallableId.callableName, expectCallableId.packageName).toList()
    }
}