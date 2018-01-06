package com.wcaokaze.japanecraft

class RomajiConverter(romajiMap: Map<String, Output>) {
  private val romajiTable = romajiMap.toTrie()

  fun convert(romajiStr: String): String {
    val romajiBuffer = StringBuffer(romajiStr)

    fun StringBuffer.parseHeadRomaji(): String {
      val strBuffer = this

      fun loop(strIdx: Int, prevNode: Trie<Output>): String {
        val node = prevNode[strBuffer[strIdx]]

        fun confirm(strIdx: Int, node: Trie<Output>): String {
          val output = node.value

          val jpStr = output?.jpChar ?: strBuffer.substring(0..strIdx)

          strBuffer.delete(0, strIdx + 1)
          if (output != null) strBuffer.insert(0, output.nextInput)

          return jpStr
        }

        return when {
          node == null                  -> confirm(strIdx - 1, prevNode)
          node.childCount == 0          -> confirm(strIdx, node)
          strIdx == strBuffer.lastIndex -> confirm(strIdx, node)
          else                          -> return loop(strIdx + 1, node)
        }
      }

      val trieNode = romajiTable[romajiBuffer.first()]

      if (trieNode != null) {
        return loop(0, romajiTable)
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

  class Output(val jpChar: String, val nextInput: String)
}
