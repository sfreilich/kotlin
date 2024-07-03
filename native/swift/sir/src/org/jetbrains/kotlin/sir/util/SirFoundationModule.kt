/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias

/**
 * A module representing the Apple Foundation library
 */
object SirFoundationModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()

    override val name: String get() = "Foundation"

    val unichar = createTypealias("unichar", SirNominalType(SirSwiftModule.uint16))

    override var declarations: MutableList<SirDeclaration> = mutableListOf(
        unichar,
    )
}

private fun createTypealias(name: String, type: SirType) = buildTypealias {
    origin = SirOrigin.ExternallyDefined("Foundation.$name")
    visibility = SirVisibility.PUBLIC
    this.name = name
    this.type = type
}.also { it.parent = SirFoundationModule }
