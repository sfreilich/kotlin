/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.config

import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.TypeRefWithNullability
import org.jetbrains.kotlin.generators.tree.config.AbstractElementConfigurator
import org.jetbrains.kotlin.generators.tree.getDefaultFieldKind
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.sir.tree.generator.model.ListField
import org.jetbrains.kotlin.sir.tree.generator.model.SimpleField

abstract class AbstractSwiftIrTreeBuilder : AbstractElementConfigurator<Element, Field, Nothing?>() {

    override fun createElement(name: String, propertyName: String, category: Nothing?): Element =
        Element(name, propertyName)

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = false,
        kind: AbstractField.Kind? = null,
        initializer: SimpleField.() -> Unit = {}
    ): SimpleField {
        return SimpleField(
            name = name,
            typeRef = type.copy(nullable),
            isMutable = mutable,
            kind = kind ?: getDefaultFieldKind(type)
        ).apply(initializer)
    }

    protected fun listField(
        name: String,
        baseType: TypeRef,
        kind: AbstractField.Kind? = null,
        isMutableList: Boolean = false,
        initializer: ListField.() -> Unit = {}
    ): ListField {
        return ListField(
            name = name,
            baseType = baseType,
            isMutableList = isMutableList,
            isMutable = false,
            kind = kind ?: getDefaultFieldKind(baseType),
        ).apply(initializer)
    }
}
