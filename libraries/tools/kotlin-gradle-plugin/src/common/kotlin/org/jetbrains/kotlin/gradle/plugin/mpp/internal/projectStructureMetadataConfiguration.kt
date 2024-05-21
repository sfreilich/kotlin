/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PSM_CONSUMABLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PSM_RESOLVABLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.extendsFromWithDependsOnClosureConfigurations
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

internal val psmAttribute = Attribute.of("psmFile", Boolean::class.javaObjectType)

internal fun setupProjectStructureMetadataConsumableConfiguration(project: Project) {
    val generateProjectStructureMetadata = project.locateOrRegisterGenerateProjectStructureMetadataTask()
    val psmConsumableConfiguration = maybeCreatePsmConsumableConfiguration(project)
    project.launch {
        val metadataTarget = project.multiplatformExtension.awaitMetadataTarget()
        psmConsumableConfiguration.extendsDependenciesOnly(
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
        extendsFromWithDependsOnClosureConfigurations(this)
        configurePsmDependenciesAttributes(project)
    }
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
}