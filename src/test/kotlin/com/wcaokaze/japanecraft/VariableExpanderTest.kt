package com.wcaokaze.japanecraft

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariableExpanderTest {
  private val variableMap = mapOf(
      "apple"  to "りんご",
      "banana" to "バナナ",
      "cherry" to "さくらんぼ"
  )

  private fun test(str: String, expandedStr: String) {
    assertEquals(expandedStr, VariableExpander(str).expand(variableMap))
  }

  @Test fun testSimple()          = test("\$apple",   "りんご")
  @Test fun testBrace()           = test("\${apple}", "りんご")
  @Test fun testConst()           = test("abc",       "abc")
  @Test fun testUnclosedBrace()   = test("\${apple",  "\${apple")
  @Test fun testIdentifierBound() = test("[\$apple]", "[りんご]")

  @Test fun testComplex()
      = test("apple, \${banana}, \$cherry", "りんご, バナナ, さくらんぼ")
}
