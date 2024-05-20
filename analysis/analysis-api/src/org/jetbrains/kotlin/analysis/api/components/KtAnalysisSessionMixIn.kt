/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

public interface KaAnalysisSessionMixIn : KaLifetimeOwner {
    public val analysisSession: KaSession
}

public typealias KtAnalysisSessionMixIn = KaAnalysisSessionMixIn