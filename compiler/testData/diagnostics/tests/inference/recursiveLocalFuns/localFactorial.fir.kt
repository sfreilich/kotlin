// FIR_IDE_IGNORE
// See KT-6271
fun foo() {
    fun fact(n: Int) = {
        if (n > 0) {
            <!ARGUMENT_TYPE_MISMATCH!><!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>fact(n - 1)<!> <!UNRESOLVED_REFERENCE!>*<!> n<!>
        }
        else {
            1
        }
    }
}
