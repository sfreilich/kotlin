/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.regression

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
class MppGranularMetadataTransformationIT : KGPBaseTest() {

    @DisplayName("KT58319: ProjectMetadataProviderImpl - supports single target projects")
    @TestMetadata("KT58319-mpp-with-single-target-project")
    @GradleTest
    fun testKT58319(gradleVersion: GradleVersion) {

        project("KT58319-mpp-with-single-target-project", gradleVersion) {
            /*
            Regression failure reported:
            Caused by: java.lang.IllegalStateException: Unexpected source set 'commonMain'
              at org.jetbrains.kotlin.gradle.plugin.mpp.ProjectMetadataProviderImpl.getSourceSetCompiledMetadata(ProjectMetadataProviderImpl.kt:42)
              at org.jetbrains.kotlin.gradle.plugin.mpp.TransformMetadataLibrariesKt.transformMetadataLibrariesForIde(transformMetadataLibraries.kt:26)
            > at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation$kotlin_gradle_plugin_common(DefaultKotlinSourceSet.kt:178)
              at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation(DefaultKotlinSourceSet.kt:151)
            */
            build("customDependenciesTransformationTask")
        }
    }
}