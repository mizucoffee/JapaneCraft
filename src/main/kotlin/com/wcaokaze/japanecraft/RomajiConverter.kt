package com.wcaokaze.japanecraft

class RomajiConverter {
  private val romajiTable = trieOf(
      "-"  to Output("ー"),
      "zh" to Output("←")
  )

  private class Output(val jpChar: String, val nextInput: CharSequence = "")
}
