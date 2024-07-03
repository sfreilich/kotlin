/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportAction
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportParameters
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraries: ConfigurableFileCollection

    @get:Nested
    abstract val parameters: SwiftExportParameters

    @TaskAction
    fun run() {
        val swiftExportQueue = workerExecutor.classLoaderIsolation { workerSpec ->
            workerSpec.classpath.from(swiftExportClasspath)
        }

        libraries.filter { it.extension == "klib" }.forEach {
            println("library: $it")
        }

        swiftExportQueue.submit(SwiftExportAction::class.java) { workParameters ->
            workParameters.bridgeModuleName.set(parameters.bridgeModuleName)
            workParameters.exportedModules.set(parameters.exportedModules)
            workParameters.konanDistribution.set(parameters.konanDistribution)
            workParameters.outputPath.set(parameters.outputPath)
            workParameters.stableDeclarationsOrder.set(parameters.stableDeclarationsOrder)
            workParameters.swiftModule.set(parameters.swiftModule)
            workParameters.swiftModulesFile.set(parameters.swiftModulesFile)
        }
    }
}