/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpressionCopy
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.PostponedResolvedAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

class Candidate(
    symbol: FirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    override var dispatchReceiver: FirExpression?,
    // In most cases, it contains zero or single element
    // More than one, only in case of context receiver group
    val givenExtensionReceiverOptions: List<FirExpression>,
    override val explicitReceiverKind: ExplicitReceiverKind,
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    private val baseSystem: ConstraintStorage,
    override val callInfo: CallInfo,
    val originScope: FirScope?,
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    inferenceSession: FirInferenceSession,
) : AbstractCandidate() {

    override var symbol: FirBasedSymbol<*> = symbol
        private set


    /**
     * Please avoid updating symbol in the candidate whenever it's possible.
     * The only case when currently it seems to be unavoidable is at
     * [org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer.refineSubstitutedMemberIfReceiverContainsTypeVariable]
     */
    @RequiresOptIn
    annotation class UpdatingSymbol

    @UpdatingSymbol
    fun updateSymbol(symbol: FirBasedSymbol<*>) {
        this.symbol = symbol
    }

    val usedOuterCs: Boolean get() = system.usesOuterCs

    private var systemInitialized: Boolean = false
    val system: NewConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()

        val outerCs = inferenceSession.outerCSForCandidate(this)
        if (outerCs != null) {
            system.addOuterSystem(outerCs, usesOuterCs = true)

            require(baseSystem.outerSystemVariablesPrefixSize == 0)
            system.addOtherSystem(baseSystem)
        } else {
            system.setBaseSystem(baseSystem)
        }

        systemInitialized = true
        system
    }

    override val errors: List<ConstraintSystemError>
        get() = system.errors

    /**
     * Substitutor from declared type parameters to type variables created for that candidate
     */
    lateinit var substitutor: ConeSubstitutor
    lateinit var freshVariables: List<ConeTypeVariable>
    var resultingTypeForCallableReference: ConeKotlinType? = null
    var outerConstraintBuilderEffect: (ConstraintSystemOperation.() -> Unit)? = null
    var usesSAM: Boolean = false

    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        set(value) {
            field = value
            usesFunctionConversion = value?.suspendConversionStrategy is CallableReferenceConversionStrategy.CustomConversion
            if (value != null) {
                numDefaults = value.defaults
            }
        }

    var usesFunctionConversion: Boolean = false

    var argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>? = null
    var numDefaults: Int = 0
    lateinit var typeArgumentMapping: TypeArgumentMapping
    val postponedAtoms = mutableListOf<PostponedResolvedAtom>()
    val postponedCalls = mutableListOf<FirStatement>()
    val postponedAccesses = mutableListOf<FirExpression>()
    val updateDeclarations = mutableListOf<() -> Unit>()

    var currentApplicability = CandidateApplicability.RESOLVED
        private set

    override var chosenExtensionReceiver: FirExpression? = givenExtensionReceiverOptions.singleOrNull()

    var contextReceiverArguments: List<FirExpression>? = null

    override val applicability: CandidateApplicability
        get() = currentApplicability

    private val _diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()
    override val diagnostics: List<ResolutionDiagnostic>
        get() = _diagnostics

    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        _diagnostics += diagnostic
        if (diagnostic.applicability < currentApplicability) {
            currentApplicability = diagnostic.applicability
        }
    }

    val isSuccessful: Boolean
        get() = currentApplicability.isSuccess && (!systemInitialized || !system.hasContradiction)

    var passedStages: Int = 0

    private var sourcesWereUpdated = false

    // FirExpressionStub can be located here in case of callable reference resolution
    fun dispatchReceiverExpression(): FirExpression {
        return dispatchReceiver?.takeIf { it !is FirExpressionStub } ?: FirNoReceiverExpression
    }

    // FirExpressionStub can be located here in case of callable reference resolution
    fun chosenExtensionReceiverExpression(): FirExpression {
        return chosenExtensionReceiver?.takeIf { it !is FirExpressionStub } ?: FirNoReceiverExpression
    }

    fun contextReceiverArguments(): List<FirExpression> {
        return contextReceiverArguments ?: emptyList()
    }

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    fun updateSourcesOfReceivers() {
        //require(!sourcesWereUpdated)
        sourcesWereUpdated = true

        dispatchReceiver = dispatchReceiver?.tryToSetSourceForImplicitReceiver()
        chosenExtensionReceiver = chosenExtensionReceiver?.tryToSetSourceForImplicitReceiver()
        contextReceiverArguments = contextReceiverArguments?.map { it.tryToSetSourceForImplicitReceiver() }
    }

    private fun FirExpression.tryToSetSourceForImplicitReceiver(): FirExpression {
        return when {
            this is FirSmartCastExpression -> {
                this.apply { replaceOriginalExpression(this.originalExpression.tryToSetSourceForImplicitReceiver()) }
            }
            this is FirThisReceiverExpression && isImplicit -> {
                buildThisReceiverExpressionCopy(this) {
                    source = callInfo.callSite.source?.fakeElement(KtFakeSourceElementKind.ImplicitReceiver)
                }
            }
            else -> this
        }
    }

    var hasVisibleBackingField = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun toString(): String {
        val okOrFail = if (applicability.isSuccess) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}

val Candidate.fullyAnalyzed: Boolean
    get() = passedStages == callInfo.callKind.resolutionSequence.size
