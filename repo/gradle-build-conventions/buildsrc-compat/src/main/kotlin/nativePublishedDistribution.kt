/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import javax.inject.Inject
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

internal enum class NativeDistributionArchiveKind {
    ZIP {
        override fun toString() = "zip"
    },
    TAR_GZ {
        override fun toString(): String = "tar.gz"
    },
}

internal abstract class ExtractPublishedNativeDistribution : TransformAction<ExtractPublishedNativeDistribution.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val archiveKind: Property<NativeDistributionArchiveKind>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val outputDir = outputs.dir(input.name)
        val archive = when (parameters.archiveKind.get()) {
            NativeDistributionArchiveKind.ZIP -> archiveOperations.zipTree(input)
            NativeDistributionArchiveKind.TAR_GZ -> archiveOperations.tarTree(input)
        }
        fileSystemOperations.sync {
            from(archive) {
                include("kotlin-native-prebuilt-*/**")
                eachFile {
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
            into(outputDir)
        }
    }
}

private enum class NativeDistributionHost {
    MACOS_AARCH64 {
        override fun toString(): String = "macos-aarch64"
    },
    MACOS_X64 {
        override fun toString(): String = "macos-x86_64"
    },
    LINUX_X64 {
        override fun toString(): String = "linux-x86_64"
    },
    WINDOWS_X64 {
        override fun toString(): String = "windows-x86_64"
        override val archiveKind: NativeDistributionArchiveKind
            get() = NativeDistributionArchiveKind.ZIP
    };

    open val archiveKind: NativeDistributionArchiveKind
        get() = NativeDistributionArchiveKind.TAR_GZ

    fun distributionDependencyNotation(version: String) = "org.jetbrains.kotlin:kotlin-native-prebuilt:$version:$this@$archiveKind"

    companion object {
        val CURRENT by lazy {
            val name = System.getProperty("os.name").run {
                when {
                    this == "Mac OS X" -> "macos"
                    this == "Linux" -> "linux"
                    startsWith("Windows") -> "windows"
                    else -> this
                }
            }
            val arch = System.getProperty("os.arch").run {
                when (this) {
                    "x86_64", "amd64" -> "x86_64"
                    "arm64", "aarch64" -> "aarch64"
                    else -> this
                }
            }
            when {
                name == "macos" && arch == "aarch64" -> MACOS_AARCH64
                name == "macos" && arch == "x86_64" -> MACOS_X64
                name == "linux" && arch == "x86_64" -> LINUX_X64
                name == "windows" && arch == "x86_64" -> WINDOWS_X64
                else -> error("Unsupported os.name=${name} os.arch=${arch}")
            }
        }
    }
}

internal class NativePublishedDistributionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.dependencies {
            registerTransform(ExtractPublishedNativeDistribution::class.java) {
                from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, NativeDistributionHost.CURRENT.archiveKind.toString())
                to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
                parameters {
                    archiveKind.set(NativeDistributionHost.CURRENT.archiveKind)
                }
            }
        }
    }
}

/**
 * Create a [Configuration] named [name] that resolves into a published Kotlin/Native distribution of [version] for the current host.
 *
 * Configuration will resolve into a single directory and will have an attribute [ArtifactTypeDefinition.DIRECTORY_TYPE].
 */
fun Project.nativePublishedDistribution(name: String, version: String): Configuration {
    apply<NativePublishedDistributionPlugin>()
    val configuration = configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
        }
        isTransitive = false
    }
    dependencies {
        // Declare every host for all to be included in verification-metadata.xml
        NativeDistributionHost.values().forEach {
            implicitDependencies(it.distributionDependencyNotation(version))
        }
        configuration(NativeDistributionHost.CURRENT.distributionDependencyNotation(version))
    }
    return configuration
}

/**
 * Create a [Configuration] that resolves into a published Kotlin/Native distribution of [version] for the current host.
 *
 * Configuration will resolve into a single directory and will have an attribute [ArtifactTypeDefinition.DIRECTORY_TYPE].
 */
fun Project.nativePublishedDistribution(version: String): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Configuration>> =
    PropertyDelegateProvider { _, property ->
        val configuration = nativePublishedDistribution(property.name, version)
        ReadOnlyProperty { _, _ -> configuration }
    }