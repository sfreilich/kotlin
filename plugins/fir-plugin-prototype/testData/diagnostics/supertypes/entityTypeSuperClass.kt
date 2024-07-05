package foo

import org.jetbrains.kotlin.fir.plugin.GeneratedEntityType

class EntityType<Self>

<!FINAL_SUPERTYPE!>@GeneratedEntityType
class WithImplicitAny<!>

interface Inter

<!FINAL_SUPERTYPE!>@GeneratedEntityType
class WithExplicitInterface : Inter<!>

open class SomeClass

<!FINAL_SUPERTYPE, MANY_CLASSES_IN_SUPERTYPE_LIST!>@GeneratedEntityType
class WithExplicitClass : SomeClass()<!>
