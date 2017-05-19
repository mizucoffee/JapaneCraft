package com.wcaokaze.japanecraft

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RomajiConverterTest {
  @Test fun testSimpleChar() {
    assertEquals("あ", RomajiConverter.convert("a"))
    assertEquals("か", RomajiConverter.convert("ka"))
    assertEquals("←", RomajiConverter.convert("zh"))
  }

  @Test fun testMultiChar() {
    assertEquals("きゃ", RomajiConverter.convert("kya"))
    assertEquals("どぅ", RomajiConverter.convert("d'u"))
  }

  @Test fun testString() {
    assertEquals("こんにちは",         RomajiConverter.convert("kon'nichiha"))
    assertEquals("おはようございます", RomajiConverter.convert("ohayougozaimasu"))
  }

  @Test fun testN() {
    assertEquals("んほ", RomajiConverter.convert("nho"))
    assertEquals("な",   RomajiConverter.convert("na"))
    assertEquals("んあ", RomajiConverter.convert("nna"))
    assertEquals("n",    RomajiConverter.convert("n"))
  }

  @Test fun testLtsu() {
    assertEquals("っば",   RomajiConverter.convert("bba"))
    assertEquals("っっば", RomajiConverter.convert("bbba"))
    assertEquals("ww",     RomajiConverter.convert("ww"))
    assertEquals("www",    RomajiConverter.convert("www"))
  }

  @Test fun testInvalidRomaji() {
    assertEquals("c",   RomajiConverter.convert("c"))
    assertEquals("ch",  RomajiConverter.convert("ch"))
    assertEquals("hこ", RomajiConverter.convert("hko"))
  }

  @Test fun testSpecialChar() {
    assertEquals("???「kzm〜〜〜〜〜……↓↓」",
        RomajiConverter.convert("???[kzm~~~~~z.z.zjzj]"))
  }
}
