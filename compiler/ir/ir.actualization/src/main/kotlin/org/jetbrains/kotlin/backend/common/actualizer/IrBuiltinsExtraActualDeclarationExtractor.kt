/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.CallableId

/*
 * Extracts actual top-level declarations from the `irBuiltIns` for passed extra expect declarations
 * Currently it's used only for synthetic expect classes like FunctionN that are not declared in source code
 */
class IrBuiltinsExtraActualDeclarationExtractor(
    extraExpectTopLevelDeclarations: ExpectTopLevelDeclarations,
    private val irBuiltIns: IrBuiltIns,
) : IrExtraActualDeclarationExtractor(extraExpectTopLevelDeclarations) {
    override fun extract(expectIrClass: IrClass): IrClassSymbol {
        require(expectIrClass.isExpect)
        // Actual class is expected to be always existing because the extractor traverses over the passed `expectTopLevelDeclarations`
        return irBuiltIns.findClass(expectIrClass.name, expectIrClass.packageFqName!!)!!
    }

    override fun extract(expectTopLevelCallables: List<IrDeclarationWithName>, expectCallableId: CallableId): List<IrSymbol> {
        require(expectTopLevelCallables.all { it.isExpect && it.isTopLevel })
        return irBuiltIns.findFunctions(expectCallableId.callableName, expectCallableId.packageName).toList()
    }
}