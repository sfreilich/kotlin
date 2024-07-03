import Foundation
import Char

let string = "AB0-Ыß☺🙂系" as NSString
func unichar(_ literal: Unicode.Scalar) -> unichar {
    return unichar(literal.value)
}

func testKotlinCharToFoundationUnichar() throws {
    try assertTrue(getCharAt(index: 4) == unichar("Ы"))
    try assertFalse(getCharAt(index: 5) == unichar("Ы"))

    for i in 0..<string.length {
        try assertEquals(actual: getCharAt(index: Int32(i)), expected: string.character(at: i))
    }
}

func testFoundationUnicharToKotlinChar() throws {
    try assertTrue(isEqualToCharAt(c: unichar("ß"), index: 5))
    try assertFalse(isEqualToCharAt(c: unichar("ß"), index: 4))
    try assertTrue(isEqualToCharAt(c: 0xD83D, index: 7))

    for i in 0..<string.length {
        try assertTrue(isEqualToCharAt(c: string.character(at: i), index: Int32(i)))
    }
}

class CharTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testKotlinCharToFoundationUnichar", method: withAutorelease(testKotlinCharToFoundationUnichar)),
            TestCase(name: "testFoundationUnicharToKotlinChar", method: withAutorelease(testFoundationUnicharToKotlinChar)),
        ]
    }
}
