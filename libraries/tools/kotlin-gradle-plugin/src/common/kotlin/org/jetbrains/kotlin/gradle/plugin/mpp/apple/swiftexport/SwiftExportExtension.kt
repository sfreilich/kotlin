/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.supportedTargets
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.StaticLibrary
import org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl
import javax.inject.Inject

@ExperimentalSwiftExportDsl
@Suppress("unused", "MemberVisibilityCanBePrivate") // Public API
abstract class SwiftExportExtension @Inject constructor(private val project: Project) {

    /**
     * Configure name of the swift export built from this project.
     */
    var name: String? = null

    /**
     * Configure package collapsing rule.
     */
    var flattenPackage: String? = null

    /**
     * Configure binaries of the Swift Export built from this project.
     */
    fun binaries(configure: AbstractNativeLibrary.() -> Unit) {
        forAllSwiftExportBinaries(configure)
    }

    /**
     * Configure binaries of the Swift Export built from this project.
     */
    fun binaries(configure: Action<AbstractNativeLibrary>) = binaries {
        configure.execute(this)
    }

    /**
     * Configure Swift Export modules export.
     */
    fun export(dependency: Any, configure: ModuleExport.() -> Unit) {
        val binaryDependencies = _allSwiftExportBinaries.mapNotNull { binary ->
            project.dependencies.add(binary.exportConfigurationName, dependency)
        }

        binaryDependencies.firstOrNull()?.let {
            project.objects.newInstance(ModuleExport::class.java, it.name).apply {
                configure()
                addToExportedModules(this)
            }
        }
    }

    /**
     * Configure Swift Export modules export.
     */
    fun export(dependency: Any, configure: Action<ModuleExport>) = export(dependency) {
        configure.execute(this)
    }

    /**
     * Returns a list of exported modules.
     */
    val exportedModules: NamedDomainObjectSet<ModuleExport>
        get() = _exportedModules

    internal var flattenPackageProvider: Provider<String> = project.provider { flattenPackage }
    internal val nameProvider: Provider<String> = project.provider { name ?: project.name }

    private val _exportedModules = project.container(ModuleExport::class.java)

    private fun addToExportedModules(module: ModuleExport) {
        check(_exportedModules.findByName(module.getName()) == null) { "Project already has Export module with name ${module.getName()}" }
        _exportedModules.add(module)
    }

    private val _allSwiftExportBinaries
        get() = project.multiplatformExtension.supportedTargets().flatMap { target ->
            target.binaries
                .matching { it.name.startsWith(SwiftExportDSLConstants.SWIFT_EXPORT_LIBRARY_PREFIX) }
                .withType(StaticLibrary::class.java)
        }

    private fun forAllSwiftExportBinaries(action: Action<in AbstractNativeLibrary>) {
        _allSwiftExportBinaries.forEach {
            action.execute(it)
        }
    }

    abstract class ModuleExport @Inject constructor(
        @get:Input val projectName: String,
    ) : Named {
        @get:Input
        @get:JvmName("getModuleName")
        var name: String = projectName.split(":").last()

        @get:Input
        var flattenPackage: String? = null

        @Input
        override fun getName(): String = projectName
    }
}