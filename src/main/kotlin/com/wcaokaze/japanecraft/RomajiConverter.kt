package com.wcaokaze.japanecraft

import java.util.*

class RomajiConverter {
  private val romajiTable = buildRomajiTable {
    // | input | output | nextInput |
    // |-------+--------+-----------|
    add("-"    , "ー"               )
    add("zh"   , "←"               )
    add("qq"   , "っ"   , "q"       )
    add("ww"   , "っ"   , "w"       )
    add("www"  , "ｗ"   , "ww"      )
  }

  private class Output(val jpChar: String, val nextInput: CharSequence)

  private class RomajiTableBuilder {
    private val romajiData = LinkedList<Pair<String, Output>>()

    fun add(input: String, output: String, nextInput: CharSequence = "") {
      romajiData += Pair(input, Output(output, nextInput))
    }

    fun build() = trieOf(*romajiData.toTypedArray())
  }

  private fun buildRomajiTable(buildOperation: RomajiTableBuilder.() -> Unit)
      = RomajiTableBuilder().apply(buildOperation).build()
}
