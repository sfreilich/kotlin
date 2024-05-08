/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class GenerateProjectStructureMetadata : DefaultTask() {

    @get:Inject
    abstract internal val projectLayout: ProjectLayout

    @get:Internal
    internal lateinit var lazyKotlinProjectStructureMetadata: Lazy<KotlinProjectStructureMetadata>

    @get:Nested
    internal val kotlinProjectStructureMetadata: KotlinProjectStructureMetadata
        get() = lazyKotlinProjectStructureMetadata.value

    @Suppress("LeakingThis")
    @get:OutputFile
    val resultFileProvider: Provider<RegularFile> = projectLayout.buildDirectory.file(
        "kotlinProjectStructureMetadata/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME"
    )

    @get:Internal
    @Deprecated(
        message = "Non lazy property. Will be removed in next releases. Use `resultFileProvider` instead",
        replaceWith = ReplaceWith("resultFileProvider")
    )
    val resultFile: File
        get() = resultFileProvider.get().asFile

    @TaskAction
    fun generateMetadataXml() {
        resultFileProvider.get().asFile.parentFile.mkdirs()
        val resultString = kotlinProjectStructureMetadata.toJson()
        resultFileProvider.get().asFile.writeText(resultString)
    }
}

internal const val MULTIPLATFORM_PROJECT_METADATA_FILE_NAME = "kotlin-project-structure-metadata.xml"
internal const val MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME = "kotlin-project-structure-metadata.json"
