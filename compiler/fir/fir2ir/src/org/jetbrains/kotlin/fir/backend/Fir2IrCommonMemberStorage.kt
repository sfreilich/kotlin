/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns.Companion.BUILTIN_OPERATOR
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.concurrent.ConcurrentHashMap

/**
 * This storage contains the shared state of FIR2IR.
 *
 * State is shared between the conversions of different modules in the same platform compilation.
 *
 * See `/docs/fir/k2_kmp.md`
 */
class Fir2IrCommonMemberStorage(
    val firSignatureComposer: FirBasedSignatureComposer,
    irMangler: KotlinMangler.IrMangler,
) {
    val symbolTable = SymbolTable(signaturer = null, irFactory = IrFactoryImpl)

    val classCache: MutableMap<FirRegularClass, IrClassSymbol> = mutableMapOf()

    val typeParameterCache: MutableMap<FirTypeParameter, IrTypeParameter> = mutableMapOf()

    val enumEntryCache: MutableMap<FirEnumEntry, IrEnumEntrySymbol> = mutableMapOf()

    val localClassCache: MutableMap<FirClass, IrClass> = mutableMapOf()

    val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val dataClassGeneratedFunctionsCache: ConcurrentHashMap<FirClass, Fir2IrDeclarationStorage.DataClassGeneratedFunctionsStorage> = ConcurrentHashMap()

    val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructorSymbol> = ConcurrentHashMap()

    val fieldCache: ConcurrentHashMap<FirField, IrFieldSymbol> = ConcurrentHashMap()

    val propertyCache: ConcurrentHashMap<FirProperty, IrPropertySymbol> = ConcurrentHashMap()
    val syntheticPropertyCache: ConcurrentHashMap<FirFunction, IrPropertySymbol> = ConcurrentHashMap()
    val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> = ConcurrentHashMap()
    val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> = ConcurrentHashMap()
    val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> = ConcurrentHashMap()
    val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> = ConcurrentHashMap()

    val fakeOverridesInClass: MutableMap<IrClass, MutableMap<Fir2IrDeclarationStorage.FirOverrideKey, FirCallableDeclaration>> = mutableMapOf()

    val irForFirSessionDependantDeclarationMap: MutableMap<Fir2IrDeclarationStorage.FakeOverrideIdentifier, IrSymbol> = mutableMapOf()

    private val kotlinInternalIrPackageFragment: IrExternalPackageFragment = createEmptyExternalPackageFragment(, StandardClassIds.BASE_INTERNAL_IR_PACKAGE)
    private val irSignatureBuilder = PublicIdSignatureComputer(irMangler)

    val eqeqeqSymbol: IrSimpleFunctionSymbol
    val eqeqSymbol: IrSimpleFunctionSymbol
    val throwCceSymbol: IrSimpleFunctionSymbol
    val throwIseSymbol: IrSimpleFunctionSymbol
    val andandSymbol: IrSimpleFunctionSymbol
    val ororSymbol: IrSimpleFunctionSymbol
    val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
    val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
    val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
    val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol

    val checkNotNullSymbol: IrSimpleFunctionSymbol

    val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    private val anyType: IrType = StandardClassIds.Any.shortClassName.defaultTypeWithoutArguments()
    private val anyNType: IrType = anyType.makeNullable()
    private val booleanType: IrType = StandardClassIds.Boolean.shortClassName.defaultTypeWithoutArguments()
    private val nothingType: IrType = StandardClassIds.Nothing.shortClassName.defaultTypeWithoutArguments()
    private val intType: IrType = StandardClassIds.Int.shortClassName.defaultTypeWithoutArguments()
    private val stringType: IrType = StandardClassIds.String.shortClassName.defaultTypeWithoutArguments()
    private val floatType: IrType = StandardClassIds.Float.shortClassName.defaultTypeWithoutArguments()
    private val doubleType: IrType = StandardClassIds.Double.shortClassName.defaultTypeWithoutArguments()

    private fun Name.defaultTypeWithoutArguments(): IrSimpleType {
        return IrFactoryImpl.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = this,
            visibility = DescriptorVisibilities.PUBLIC,
            symbol = IrClassSymbolImpl(),
            kind = ClassKind.CLASS,
            modality = Modality.FINAL,
            isExternal = false,
            isCompanion = false,
            isInner = false,
            isData = false,
            isValue = false,
            isExpect = false,
            isFun = false,
            hasEnumEntries = false,
            source = SourceElement.NO_SOURCE,
        ).let {
            IrSimpleTypeImpl(
                kotlinType = null,
                classifier = it.symbol,
                nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
                arguments = emptyList(),
                annotations = emptyList()
            )
        }
    }

    private val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)

    init {
        with(this.kotlinInternalIrPackageFragment) {
            fun addBuiltinOperatorSymbol(
                name: String,
                returnType: IrType,
                vararg valueParameterTypes: Pair<String, IrType>,
                isIntrinsicConst: Boolean = false,
            ): IrSimpleFunctionSymbol {
                return createFunction(
                    name, returnType, valueParameterTypes,
                    origin = BUILTIN_OPERATOR,
                    isIntrinsicConst = isIntrinsicConst
                ).also {
                    // `kotlinInternalIrPackageFragment` definitely is not a lazy class
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    declarations.add(it)
                }.symbol
            }

            primitiveFloatingPointIrTypes.forEach { fpType ->
                _ieee754equalsFunByOperandType[fpType.classifierOrFail] = addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.IEEE754_EQUALS,
                    booleanType,
                    "arg0" to fpType.makeNullable(),
                    "arg1" to fpType.makeNullable(),
                    isIntrinsicConst = true
                )
            }
            eqeqeqSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQEQ, booleanType, "" to anyNType, "" to anyNType)
            eqeqSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQ, booleanType, "" to anyNType, "" to anyNType, isIntrinsicConst = true)
            throwCceSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_CCE, nothingType)
            throwIseSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_ISE, nothingType)
            andandSymbol =
                addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.ANDAND,
                    booleanType,
                    "" to booleanType,
                    "" to booleanType,
                    isIntrinsicConst = true
                )
            ororSymbol =
                addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.OROR,
                    booleanType,
                    "" to booleanType,
                    "" to booleanType,
                    isIntrinsicConst = true
                )
            noWhenBranchMatchedExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType)
            illegalArgumentExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, "" to stringType)
            dataClassArrayMemberHashCodeSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberHashCode", intType, "" to anyType)
            dataClassArrayMemberToStringSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberToString", stringType, "" to anyNType)

            checkNotNullSymbol = run {
                val typeParameter: IrTypeParameter = IrFactoryImpl.createTypeParameter(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = BUILTIN_OPERATOR,
                    name = Name.identifier("T0"),
                    symbol = IrTypeParameterSymbolImpl(),
                    variance = org.jetbrains.kotlin.types.Variance.INVARIANT,
                    index = 0,
                    isReified = true
                ).apply {
                    superTypes = listOf(anyType)
                }

                createFunction(
                    BuiltInOperatorNames.CHECK_NOT_NULL,
                    IrSimpleTypeImpl(typeParameter.symbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList()),
                    arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
                    typeParameters = listOf(typeParameter),
                    origin = BUILTIN_OPERATOR
                ).also {
                    // `kotlinInternalIrPackageFragment` definitely is not a lazy class
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    declarations.add(it)
                }.symbol
            }

            fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
                associate {
                    it.classifierOrFail to addBuiltinOperatorSymbol(
                        name,
                        booleanType,
                        "" to it,
                        "" to it,
                        isIntrinsicConst = true
                    )
                }

            lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS)
            lessOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS_OR_EQUAL)
            greaterOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER_OR_EQUAL)
            greaterFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER)

        }
    }

    private fun IrDeclarationParent.createFunction(
        name: String,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        typeParameters: List<IrTypeParameter> = emptyList(),
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        isInfix: Boolean = false,
        isIntrinsicConst: Boolean = false,
        postBuild: IrSimpleFunction.() -> Unit = {},
        build: IrFunctionBuilder.() -> Unit = {},
    ): IrSimpleFunction {

        fun makeWithSymbol(symbol: IrSimpleFunctionSymbol) = IrFunctionBuilder().run {
            this.name = Name.identifier(name)
            this.returnType = returnType
            this.origin = origin
            this.modality = modality
            this.isOperator = isOperator
            this.isInfix = isInfix
            build()
            IrFactoryImpl.createSimpleFunction(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = this.origin,
                name = this.name,
                visibility = visibility,
                isInline = isInline,
                isExpect = isExpect,
                returnType = this.returnType,
                modality = this.modality,
                symbol = symbol,
                isTailrec = isTailrec,
                isSuspend = isSuspend,
                isOperator = this.isOperator,
                isInfix = this.isInfix,
                isExternal = isExternal,
                containerSource = containerSource,
                isFakeOverride = isFakeOverride,
            )
        }.also { fn ->
            valueParameterTypes.forEachIndexed { index, (pName, irType) ->
                fn.addValueParameter(Name.identifier(pName.ifBlank { "arg$index" }), irType, origin)
            }
            fn.typeParameters = typeParameters
            typeParameters.forEach { it.parent = fn }
            if (isIntrinsicConst) {
                fn.annotations += intrinsicConstAnnotation
            }
            fn.parent = this@createFunction
            fn.postBuild()
        }

        val irFun4SignatureCalculation = makeWithSymbol(IrSimpleFunctionSymbolImpl())
        val signature = irSignatureBuilder.computeSignature(irFun4SignatureCalculation)
        return symbolTable.declareSimpleFunction(
            signature,
            { IrSimpleFunctionSymbolImpl(null, signature) },
            ::makeWithSymbol
        )
    }
}
