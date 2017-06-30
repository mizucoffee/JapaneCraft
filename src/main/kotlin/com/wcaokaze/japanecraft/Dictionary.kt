package com.wcaokaze.japanecraft

class Dictionary(map: Map<String, String>) {
  private val dictionary = map.toTrie()

  operator fun get(rawString: String): Pair<String, IntRange> {
    throw NotImplementedError()
  }
}
