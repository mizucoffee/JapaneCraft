package com.wcaokaze.japanecraft

class Dictionary(map: Map<String, String>) {
  private val trie = map.toTrie()

  operator fun get(rawString: String): Pair<String, IntRange>? {
    fun loop(prevNode: Trie<String>, index: Int): Pair<String, IntRange>? {
      if (index !in rawString.indices) return null
      val node = trie[rawString[index]] ?: return null

      if (node.value != null) {
        return loop(node, index + 1) ?: Pair(node.value!!, 0..index)
      } else {
        return loop(node, index + 1)
      }
    }

    return loop(trie, 0)
  }
}
