package com.wcaokaze.japanecraft

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DictionaryTest {
  private val dictionary = Dictionary(mapOf(
      "あか" to "赤",
      "あお" to "青",
      "あおかぜ" to "碧風"
  ))

  @Test fun simple()      = assert(dictionary("あか")       == "赤")
  @Test fun notFound()    = assert(dictionary("あい")       == "あい")
  @Test fun halfOnTrie()  = assert(dictionary("あお")       == "青")
  @Test fun full()        = assert(dictionary("あおかぜ")   == "碧風")
  @Test fun overrun()     = assert(dictionary("あおかぜの") == "碧風")
  @Test fun complicated() = assert(dictionary("あおい")     == "青")
}
