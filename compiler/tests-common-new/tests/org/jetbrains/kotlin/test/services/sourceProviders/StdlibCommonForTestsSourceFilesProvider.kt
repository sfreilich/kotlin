/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class StdlibCommonForTestsSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private const val HELPERS_PATH = "./compiler/testData/codegen/stdlibCommonForTests"
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        if (!module.targetPlatform.isCommon()) return emptyList()

        return File(HELPERS_PATH).walkTopDown().mapNotNull {
            if (it.isDirectory) return@mapNotNull null
            it.toTestFile()
        }.toList()
    }
}