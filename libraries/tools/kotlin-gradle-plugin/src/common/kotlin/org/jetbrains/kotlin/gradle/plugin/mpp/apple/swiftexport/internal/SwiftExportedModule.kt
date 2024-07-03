/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

// Exported module declaration
abstract class SwiftExportedModule {
    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    @get:Optional
    abstract val flattenPackage: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val library: RegularFileProperty
}