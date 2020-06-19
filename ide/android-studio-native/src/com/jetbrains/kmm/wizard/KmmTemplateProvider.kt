/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.android.tools.idea.wizard.template.*

class KmmTemplateProvider : WizardTemplateProvider() {
    override fun getTemplates(): List<Template> = listOf(KmmTemplate)
}