import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets.commonMain.get().dependencies {
        implementation(project(":p2"))
    }
}

val customDependenciesTransformationTask: Task by tasks.creating {
    kotlin.sourceSets
        .map { it.name }
        .forEach { sourceSetName ->
            val configurationName = sourceSetName + "ProjectStructureMetadataResolvableConfiguration"
            inputs.files(project.provider {
                project.configurations.named(configurationName).get().incoming.artifactView { isLenient = true }.artifacts.artifactFiles
            })
        }
    doFirst {
        kotlin.sourceSets.withType<DefaultKotlinSourceSet>().forEach { sourceSet ->
            sourceSet.getDependenciesTransformation("unusedParameter")
        }
    }
}