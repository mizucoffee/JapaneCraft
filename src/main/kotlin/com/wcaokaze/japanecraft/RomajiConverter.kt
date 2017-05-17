package com.wcaokaze.japanecraft

import java.util.*

class RomajiConverter {
  private val romajiTable = trieOf(
    "-"   to Output("ー"),
    "zh"  to Output("←"),
    "qq"  to Output("っ", nextInput = "q"),
    "ww"  to Output("っ", nextInput = "w"),
    "www" to Output("ｗ", nextInput = "ww")
  )

  private class Output(val jpChar: String, val nextInput: CharSequence = "")
}
