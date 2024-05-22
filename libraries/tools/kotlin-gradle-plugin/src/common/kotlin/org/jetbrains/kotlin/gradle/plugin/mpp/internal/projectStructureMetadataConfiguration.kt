/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compileDependenciesConfigurations
import org.jetbrains.kotlin.gradle.plugin.mpp.extendsFromWithDependsOnClosureConfigurations
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

internal val psmAttribute = Attribute.of("psmFile", Boolean::class.javaObjectType)

internal fun setupProjectStructureMetadataConsumableConfiguration(project: Project) {
    val generateProjectStructureMetadata = project.locateOrRegisterGenerateProjectStructureMetadataTask()
    val psmConsumableConfiguration = maybeCreatePsmConsumableConfiguration(project)
    project.launch {
        val metadataTarget = project.multiplatformExtension.awaitMetadataTarget()
        psmConsumableConfiguration.extendsProjectDependenciesOnly(
            project,
            project.configurations.getByName(metadataTarget.apiElementsConfigurationName)
        )
    }
    project.artifacts.add(
        psmConsumableConfiguration.name,
        generateProjectStructureMetadata.map { task -> task.resultFile }
    )
}

internal val InternalKotlinSourceSet.projectStructureMetadataConfiguration: Configuration by extrasStoredProperty {
    project.configurations.maybeCreateResolvable(projectStructureMetadataConfigurationName) {
        extendsProjectDependenciesOnlyFromWithDependsOnClosureConfigurations(this)
        configurePsmDependenciesAttributes(project)
    }
}

private fun InternalKotlinSourceSet.extendsProjectDependenciesOnlyFromWithDependsOnClosureConfigurations(configuration: Configuration) {
    withDependsOnClosure.forAll { sourceSet ->
        val extenders = sourceSet.internal.compileDependenciesConfigurations
        configuration.extendsProjectDependenciesOnly(project, *extenders.toTypedArray())
    }

    /**
     * Adding dependencies from associate compilations using a listProvider, since we would like to defer
     * the call to 'getVisibleSourceSetsFromAssociateCompilations' as much as possible (changes to the model might significantly
     * change the result of this visible source sets)
     */
    val associatedConfigurations = getVisibleSourceSetsFromAssociateCompilations(this).flatMap { sourceSet ->
        sourceSet.internal.compileDependenciesConfigurations
    }
    configuration.extendsProjectDependenciesOnly(project, *associatedConfigurations.toTypedArray())

}

private val InternalKotlinSourceSet.projectStructureMetadataConfigurationName: String
    get() = disambiguateName(lowerCamelCaseName(PSM_RESOLVABLE_CONFIGURATION_NAME))

private fun maybeCreatePsmConsumableConfiguration(project: Project): Configuration {
    return project.configurations.maybeCreateConsumable(PSM_CONSUMABLE_CONFIGURATION_NAME) {
        configurePsmDependenciesAttributes(project)
    }
}

private fun Configuration.configurePsmDependenciesAttributes(project: Project) {
    attributes.setAttribute(psmAttribute, true)
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM))
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}