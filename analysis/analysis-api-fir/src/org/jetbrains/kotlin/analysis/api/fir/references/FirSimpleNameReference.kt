/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class KaFirSimpleNameReference(
    expression: KtSimpleNameExpression,
    val isRead: Boolean,
) : KtSimpleNameReference(expression), KaFirReference {

    private val isAnnotationCall: Boolean
        get() {
            val ktUserType = expression.parent as? KtUserType ?: return false
            val ktTypeReference = ktUserType.parent as? KtTypeReference ?: return false
            val ktConstructorCalleeExpression = ktTypeReference.parent as? KtConstructorCalleeExpression ?: return false
            return ktConstructorCalleeExpression.parent is KtAnnotationEntry
        }

    private fun KaSession.fixUpAnnotationCallResolveToCtor(resultsToFix: Collection<KaSymbol>): Collection<KaSymbol> {
        if (resultsToFix.isEmpty() || !isAnnotationCall) return resultsToFix

        return resultsToFix.map { targetSymbol ->
            if (targetSymbol is KaFirNamedClassOrObjectSymbol && targetSymbol.classKind == KaClassKind.ANNOTATION_CLASS) {
                targetSymbol.getMemberScope().getConstructors().firstOrNull() ?: targetSymbol
            } else targetSymbol
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return getImportAlias(alias.importDirective) != null
    }

    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        check(this is KaFirSession)
        val results = FirReferenceResolveHelper.resolveSimpleNameReference(this@KaFirSimpleNameReference, this)
        //This fix-up needed to resolve annotation call into annotation constructor (but not into the annotation type)
        return fixUpAnnotationCallResolveToCtor(results)
    }

    override fun getResolvedToPsi(analysisSession: KaSession): Collection<PsiElement> = with(analysisSession) {
        if (expression is KtLabelReferenceExpression) {
            val fir = expression.getOrBuildFir((analysisSession as KaFirSession).firResolveSession)
            if (fir is FirLoopJump) {
                return listOfNotNull(fir.target.labeledElement.psi)
            }
        }
        val referenceTargetSymbols = resolveToSymbols()
        val psiOfReferenceTarget = super.getResolvedToPsi(analysisSession, referenceTargetSymbols)
        if (psiOfReferenceTarget.isNotEmpty()) return psiOfReferenceTarget
        referenceTargetSymbols.flatMap { symbol ->
            when (symbol) {
                is KaFirSyntheticJavaPropertySymbol ->
                    if (isRead) {
                        listOfNotNull(symbol.javaGetterSymbol.psi)
                    } else {
                        if (symbol.javaSetterSymbol == null) listOfNotNull(symbol.javaGetterSymbol.psi)
                        else listOfNotNull(symbol.javaSetterSymbol?.psi)
                    }
                else -> listOfNotNull(symbol.psi)
            }
        }
    }

    override fun canBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    // Extension point used for deprecated Android Extensions. Not going to implement for FIR.
    override fun isReferenceToViaExtension(element: PsiElement): Boolean {
        return false
    }

    private fun getImportAlias(importDirective: KtImportDirective?): KtImportAlias? {
        val fqName = importDirective?.importedFqName ?: return null
        val codeFragment = KtPsiFactory(element.project).createExpressionCodeFragment(fqName.asString(), element)
        val contentElement = codeFragment.getContentElement()
        val importResults =
            when (contentElement) {
                is KtDotQualifiedExpression -> contentElement.selectorExpression?.mainReference?.multiResolve(false)
                is KtSimpleNameExpression -> contentElement.mainReference.multiResolve(false)
                else -> null
            } ?: return null
        val resolveResults = multiResolve(false)
        if (resolveResults.any { it in importResults }) {
            return importDirective.alias
        }
        val targets = resolveResults.mapNotNull { it.element }
        val adjustedImportTargets = importResults.mapNotNull {
            val e = it.element
            if (e is KtObjectDeclaration && e.isCompanion()) e.containingClassOrObject else null
        }
        if (adjustedImportTargets.any { it in targets }) {
            return importDirective.alias
        }
        return null
    }

    override fun getImportAlias(): KtImportAlias? {
        val name = element.getReferencedName()
        val file = element.containingKtFile
        return getImportAlias(file.findImportByAlias(name))
    }
}