/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

open class NodeJsExtension(
    val project: Project,
) : AbstractSettings<NodeJsEnv>() {

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileValue(gradleHome.resolve("nodejs"))

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(true)

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .value("https://nodejs.org/dist")

    // Release schedule: https://github.com/nodejs/Release
    // Actual LTS and Current versions: https://nodejs.org/en/download/
    // Older versions and more information, e.g. V8 version inside: https://nodejs.org/en/download/releases/
    override val versionProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("22.0.0")

    override val commandProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("node")

    internal val platform: org.gradle.api.provider.Property<Platform> = project.objects.property<Platform>()

    override fun finalizeConfiguration(): NodeJsEnv {
        val name = platform.get().name
        val architecture = platform.get().arch

        val version = versionProperty.get()
        val nodeDirName = "node-v$version-$name-$architecture"
        val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
        val nodeDir = cleanableStore[nodeDirName].use()
        val isWindows = platform.get().isWindows()
        val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")
        val downloadValue = downloadProperty.get()

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (downloadValue) File(nodeBinDir, finalCommand).absolutePath else finalCommand
        }

        fun getIvyDependency(): String {
            val type = if (isWindows) "zip" else "tar.gz"
            return "org.nodejs:node:$version:$name-$architecture@$type"
        }

        return NodeJsEnv(
            download = downloadValue,
            cleanableStore = cleanableStore,
            dir = nodeDir,
            nodeBinDir = nodeBinDir,
            executable = getExecutable("node", commandProperty.get(), "exe"),
            platformName = name,
            architectureName = architecture,
            ivyDependency = getIvyDependency(),
            downloadBaseUrl = downloadBaseUrlProperty.orNull,
        )
    }

    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
