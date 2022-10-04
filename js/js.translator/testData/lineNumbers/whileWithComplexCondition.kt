fun box() {
    var i = 0
    while(
        try {
           i++
        }
        finally {
            println("finally: $i")
        } < i
    ) {
        println("body: $i")
    }
}

// LINES(JS):    1 13 5 5 4 2 2 3 5 5       8 8 * 3 4 9 3 11 11
// LINES(JS_IR): 1          2 2 * 5 5 5 5 5 8 8 *   4 9 * 11 11
