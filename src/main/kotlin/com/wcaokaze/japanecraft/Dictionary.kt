package com.wcaokaze.japanecraft

class Dictionary(map: Map<String, String>) {
  private val trie = map.toTrie()

  operator fun invoke(rawString: String): String {
    fun loop(prevNode: Trie<String>, index: Int): String? {
      if (index > rawString.lastIndex) return null
      val node = prevNode[rawString[index]] ?: return null

      return loop(node, index + 1) ?: node.value
    }

    return loop(trie, 0) ?: rawString
  }
}
