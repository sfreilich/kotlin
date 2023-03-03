/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.internal.UsesLibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.ir.DISABLE_PRE_IR
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_JS
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_UNZIPPED_KLIB
import org.jetbrains.kotlin.gradle.targets.js.ir.PRODUCE_ZIPPED_KLIB
import org.jetbrains.kotlin.gradle.tasks.internal.KotlinJsOptionsCompat
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.api.ClasspathChanges
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.utils.JsLibraryUtils
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class Kotlin2JsCompile @Inject constructor(
    override val compilerOptions: KotlinJsCompilerOptions,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : AbstractKotlinCompile<K2JSCompilerArguments>(objectFactory, workerExecutor),
    KotlinCompilationTask<KotlinJsCompilerOptions>,
    UsesLibraryFilterCachingService,
    KotlinJsCompile,
    K2MultiplatformCompilationTask {

    init {
        incremental = true
        compilerOptions.verbose.convention(logger.isDebugEnabled)
    }

    override val kotlinOptions: KotlinJsOptions = KotlinJsOptionsCompat(
        { this },
        compilerOptions
    )

    @get:Input
    internal var incrementalJsKlib: Boolean = true

    override fun isIncrementalCompilationEnabled(): Boolean {
        val freeArgs = enhancedFreeCompilerArgs.get()
        return when {
            PRODUCE_JS in freeArgs -> false

            PRODUCE_UNZIPPED_KLIB in freeArgs -> {
                KotlinBuildStatsService.applyIfInitialised {
                    it.report(BooleanMetrics.JS_KLIB_INCREMENTAL, incrementalJsKlib)
                }
                incrementalJsKlib
            }

            PRODUCE_ZIPPED_KLIB in freeArgs -> {
                KotlinBuildStatsService.applyIfInitialised {
                    it.report(BooleanMetrics.JS_KLIB_INCREMENTAL, incrementalJsKlib)
                }
                incrementalJsKlib
            }

            else -> incremental
        }
    }

    // Workaround to be able to use default value and change it later based on external input
    @get:Internal
    internal abstract val defaultDestinationDirectory: DirectoryProperty

    @Deprecated("Use destinationDirectory and moduleName instead")
    @get:Internal
    abstract val outputFileProperty: Property<File>

    // Workaround to add additional compiler args based on the exising one
    // Currently there is a logic to add additional compiler arguments based on already existing one.
    // And it is not possible to update compilerOptions.freeCompilerArgs using some kind of .map
    // or .flatMap call - this will cause StackOverlowException as upstream source will be updated
    // and .map will be called again.
    @get:Input
    internal abstract val enhancedFreeCompilerArgs: ListProperty<String>

    /**
     * Workaround for those "nasty" plugins that are adding 'freeCompilerArgs' on task execution phase.
     * With properties api it is not possible to update property value after task configuration is finished.
     *
     * Marking it as `@Internal` as anyway on the configuration phase, when Gradle does task inputs snapshot,
     * this input will always be empty.
     */
    @get:Internal
    internal var executionTimeFreeCompilerArgs: List<String>? = null

    @get:Nested
    override val multiplatformStructure: K2MultiplatformStructure = objectFactory.newInstance()

    override fun createCompilerArgs(): K2JSCompilerArguments =
        K2JSCompilerArguments()

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        KotlinJsCompilerOptionsHelper.fillDefaultValues(args)
        super.setupCompilerArgs(args, defaultsOnly = defaultsOnly, ignoreClasspathResolutionErrors = ignoreClasspathResolutionErrors)

        if (defaultsOnly) return

        KotlinJsCompilerOptionsHelper.fillCompilerArguments(compilerOptions, args)
        if (!args.sourceMapPrefix.isNullOrEmpty()) {
            args.sourceMapBaseDirs = sourceMapBaseDir.get().asFile.absolutePath
        }
        if (isIrBackendEnabled()) {
            val outputFilePath: String? = compilerOptions.outputFile.orNull
            if (outputFilePath != null) {
                val outputFile = File(outputFilePath)
                args.outputDir = (if (outputFile.extension == "") outputFile else outputFile.parentFile).normalize().absolutePath
                args.moduleName = outputFile.nameWithoutExtension
            } else {
                args.outputDir = destinationDirectory.get().asFile.normalize().absolutePath
                args.moduleName = compilerOptions.moduleName.get()
            }
        } else {
            args.outputFile = outputFileProperty.get().absoluteFile.normalize().absolutePath
        }

        args.configureMultiplatform(
            compilerOptions,
            k1CommonSources = commonSourceSet.asFileTree,
            k2MultiplatformFragments = multiplatformStructure
        )

        // Overriding freeArgs from compilerOptions with enhanced one + additional one set on execution phase
        // containing additional arguments based on the js compilation configuration
        val localExecutionTimeFreeCompilerArgs = executionTimeFreeCompilerArgs
        args.freeArgs = if (localExecutionTimeFreeCompilerArgs != null) localExecutionTimeFreeCompilerArgs else enhancedFreeCompilerArgs.get()
    }

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:Optional
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val friendDependencies: FileCollection = objectFactory
        .fileCollection()
        .from(friendPaths)
        .filter {
            // .jar files are not required for js compilation as friend modules
            // and, because of `@InputFiles` and different normalization strategy from `@Classpath`,
            // they produce build cache misses
            it.exists() && !it.name.endsWith(".jar") && libraryFilter(it)
        }

    @get:Internal
    internal val sourceMapBaseDir: Property<Directory> = objectFactory
        .directoryProperty()
        .value(project.layout.projectDirectory)

    private fun isHybridKotlinJsLibrary(file: File): Boolean =
        JsLibraryUtils.isKotlinJavascriptLibrary(file) && isKotlinLibrary(file)

    private val preIrBackendCompilerFlags = listOf(
        DISABLE_PRE_IR,
        PRODUCE_JS,
        PRODUCE_ZIPPED_KLIB
    )

    private fun isPreIrBackendDisabled(): Boolean = enhancedFreeCompilerArgs
        .get()
        .any { preIrBackendCompilerFlags.contains(it) }

    // see also isIncrementalCompilationEnabled
    private val irBackendCompilerFlags = listOf(
        PRODUCE_UNZIPPED_KLIB,
        PRODUCE_JS,
        PRODUCE_ZIPPED_KLIB
    )

    private fun isIrBackendEnabled(): Boolean = enhancedFreeCompilerArgs
        .get()
        .any { irBackendCompilerFlags.contains(it) }

    private val File.asLibraryFilterCacheKey: LibraryFilterCachingService.LibraryFilterCacheKey
        get() = LibraryFilterCachingService.LibraryFilterCacheKey(
            this,
            irEnabled = isIrBackendEnabled(),
            preIrDisabled = isPreIrBackendDisabled()
        )

    // Kotlin/JS can operate in 3 modes:
    //  1) purely pre-IR backend
    //  2) purely IR backend
    //  3) hybrid pre-IR and IR backend. Can only accept libraries with both JS and IR parts.
    private val libraryFilterBody: (File) -> Boolean
        get() = if (isIrBackendEnabled()) {
            if (isPreIrBackendDisabled()) {
                //::isKotlinLibrary
                // Workaround for KT-47797
                { isKotlinLibrary(it) }
            } else {
                ::isHybridKotlinJsLibrary
            }
        } else {
            JsLibraryUtils::isKotlinJavascriptLibrary
        }

    @get:Input
    internal val jsLegacyNoWarn: Provider<Boolean> = objectFactory.property(
        PropertiesProvider(project).jsCompilerNoWarn
    )

    @get:Internal
    protected val libraryFilter: (File) -> Boolean
        get() = { file ->
            libraryFilterCacheService.get().getOrCompute(file.asLibraryFilterCacheKey, libraryFilterBody)
        }

    override val incrementalProps: List<FileCollection>
        get() = super.incrementalProps + listOf(friendDependencies)

    open fun processArgs(
        args: K2JSCompilerArguments
    ) {

    }

    override fun callCompilerAsync(
        args: K2JSCompilerArguments,
        kotlinSources: Set<File>,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        logger.debug("Calling compiler")

        validateOutputDirectory()

        if (isIrBackendEnabled()) {
            logger.info(USING_JS_IR_BACKEND_MESSAGE)
        }

        val dependencies = libraries
            .filter { it.exists() && libraryFilter(it) }
            .map { it.normalize().absolutePath }

        args.libraries = dependencies.distinct().let {
            if (it.isNotEmpty())
                it.joinToString(File.pathSeparator) else
                null
        }

        args.friendModules = friendDependencies.files.joinToString(File.pathSeparator) { it.absolutePath }
        if (!isIrBackendEnabled()) {
            args.legacyDeprecatedNoWarn = true
            args.useDeprecatedLegacyCompiler = true
        }

        logger.kotlinDebug("compiling with args ${ArgumentUtils.convertArgumentsToStringList(args)}")

        val gradlePrintingMessageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val gradleMessageCollector = GradleErrorMessageCollector(gradlePrintingMessageCollector, kotlinPluginVersion = getKotlinPluginVersion(logger))
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                getChangedFiles(inputChanges, incrementalProps),
                ClasspathChanges.NotAvailableForJSCompiler,
                taskBuildCacheableOutputDirectory.get().asFile,
                multiModuleICSettings = multiModuleICSettings,
                preciseCompilationResultsBackup = preciseCompilationResultsBackup.get(),
                keepIncrementalCompilationCachesInMemory = keepIncrementalCompilationCachesInMemory.get(),
            )
        } else null

        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, compilerFacadeClasspath, gradleMessageCollector, outputItemCollector,
            outputFiles = allOutputFiles(),
            reportingSettings = reportingSettings(),
            incrementalCompilationEnvironment = icEnv
        )
        processArgs(args)
        compilerRunner.runJsCompilerAsync(
            kotlinSources.toList(),
            args,
            environment,
            taskOutputsBackup
        )
        compilerRunner.errorsFile?.also { gradleMessageCollector.flush(it) }

    }

    private val projectRootDir = project.rootDir

    private fun validateOutputDirectory() {
        val outputFile = outputFileProperty.get()
        val outputDir = outputFile.parentFile

        if (outputDir.isParentOf(projectRootDir)) {
            throw InvalidUserDataException(
                "The output directory '$outputDir' (defined by outputFile of ':$name') contains or " +
                        "matches the project root directory '${projectRootDir}'.\n" +
                        "Gradle will not be able to build the project because of the root directory lock.\n" +
                        "To fix this, consider using the default outputFile location instead of providing it explicitly."
            )
        }
    }
}