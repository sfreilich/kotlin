/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property

open class BinaryenRootExtension(
    @Transient val rootProject: Project
) : AbstractSettings<BinaryenEnv>() {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    override val installationDirectory: DirectoryProperty = rootProject.objects.directoryProperty()
        .fileValue(gradleHome.resolve("binaryen"))

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .value("https://github.com/WebAssembly/binaryen/releases/download")

    override val versionProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("117")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = rootProject.objects.property<Boolean>()
        .convention(true)

    override val commandProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("wasm-opt")

    val setupTaskProvider: TaskProvider<BinaryenSetupTask>
        get() = rootProject.tasks.withType(BinaryenSetupTask::class.java).named(BinaryenSetupTask.NAME)

    override fun finalizeConfiguration(): BinaryenEnv {
        val platform = BinaryenPlatform.platform
        val version = versionProperty.get()
        val requiredVersionName = "binaryen-version_$version"
        val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
        val targetPath = cleanableStore[requiredVersionName].use()
        val isWindows = BinaryenPlatform.name == BinaryenPlatform.WIN

        val download = downloadProperty.get()
        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download)
                targetPath
                    .resolve("bin")
                    .resolve(finalCommand)
                    .absolutePath
            else
                finalCommand
        }

        return BinaryenEnv(
            download = download,
            downloadBaseUrl = downloadBaseUrlProperty.orNull,
            ivyDependency = "com.github.webassembly:binaryen:$version:$platform@tar.gz",
            executable = getExecutable("wasm-opt", commandProperty.get(), "exe"),
            dir = targetPath,
            cleanableStore = cleanableStore,
            isWindows = isWindows,
        )
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryen"
    }
}
