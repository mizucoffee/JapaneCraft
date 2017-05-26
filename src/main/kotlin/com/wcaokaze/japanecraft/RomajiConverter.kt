package com.wcaokaze.japanecraft

class RomajiConverter(romajiMap: Map<String, Output>) {
  private val romajiTable = trieOf(*romajiMap.map { it.toPair() }.toTypedArray())

  fun convert(romajiStr: String): String {
    val romajiBuffer = StringBuffer(romajiStr)

    fun StringBuffer.parseHeadRomaji(): String {
      val strBuffer = this

      fun loop(strIdx: Int, trieNode: Trie<Output>): String {
        fun confirm(): String {
          val output = trieNode.value

          val jpStr = output?.jpChar ?: strBuffer.substring(0..strIdx)

          strBuffer.delete(0, strIdx + 1)
          if (output != null) strBuffer.insert(0, output.nextInput)

          return jpStr
        }

        fun abort(): String {
          val remainingChars = strBuffer.toString()
          strBuffer.delete(0, strBuffer.length)
          return remainingChars
        }

        if (trieNode.childCount == 0)      return confirm()
        if (strIdx == strBuffer.lastIndex) return abort()

        val nextNode = trieNode[strBuffer[strIdx + 1]]

        if (nextNode != null) {
          return loop(strIdx + 1, nextNode)
        } else {
          return confirm()
        }
      }

      val trieNode = romajiTable[romajiBuffer.first()]

      if (trieNode != null) {
        return loop(0, trieNode)
      } else {
        val char = romajiBuffer.first()
        romajiBuffer.deleteCharAt(0)
        return String(charArrayOf(char))
      }
    }

    return buildString {
      while (romajiBuffer.isNotEmpty()) {
        append(romajiBuffer.parseHeadRomaji())
      }
    }
  }

  class Output(val jpChar: String, val nextInput: String = "")
}
