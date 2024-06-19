/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins.INLINED_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

@PhaseDescription(
    name = "CreateSeparateCallForInlinedLambdasLowering",
    description = "This lowering will create separate call `singleArgumentInlineFunction` with previously inlined lambda as argument",
    prerequisite = [JvmIrInliner::class]
)
class CreateSeparateCallForInlinedLambdasLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.config.enableIrInliner) {
            irFile.transformChildrenVoid()
        }
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock): IrExpression {
        if (inlinedBlock.isFunctionInlining()) {
            val newCalls = inlinedBlock.getOnlyInlinableArguments().map { arg ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.ir.symbols.singleArgumentInlineFunction)
                    .also { it.putValueArgument(0, arg.transform(this, null)) }
            }

            // we don't need to transform body of original function, just arguments that were extracted as variables
            inlinedBlock.getAdditionalStatementsFromInlinedBlock().forEach { it.transformChildrenVoid() }
            val newBlock = IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, INLINED_FUNCTION_ARGUMENTS)
                .apply { statements += newCalls }
            inlinedBlock.statements.add(0, newBlock)
            return inlinedBlock
        }
        return super.visitInlinedFunctionBlock(inlinedBlock)
    }

    private fun IrInlinedFunctionBlock.getOnlyInlinableArguments(): List<IrExpression> {
        return this.inlineCall.getArgumentsWithIr()
            .filter { (param, arg) -> param.isInlineParameter() && arg.isInlinableExpression() }
            .map { it.second }
    }

    private fun IrExpression.isInlinableExpression(): Boolean {
        return this is IrFunctionExpression || this is IrFunctionReference || this is IrPropertyReference
                || (this is IrBlock && origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)
    }
}
