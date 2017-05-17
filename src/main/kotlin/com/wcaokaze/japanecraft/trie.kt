package com.wcaokaze.japanecraft

import java.util.*

interface Trie<out V> {
  val value: V?
  operator fun get(char: Char): Trie<V>?
}

private class MutableTrie<V>(override var value: V?) : Trie<V> {
  private val children: MutableMap<Char, MutableTrie<V>> = TreeMap()

  override operator fun get(char: Char): MutableTrie<V>? = children[char]

  operator fun set(char: Char, child: MutableTrie<V>) {
    children[char] = child
  }
}

fun <V> trieOf(vararg pairs: Pair<String, V>): Trie<V> {
  fun MutableTrie<V>.getOrCreateChild(char: Char): MutableTrie<V> {
    var child = this[char]

    if (child == null) {
      child = MutableTrie(null)
      this[char] = child
    }

    return child
  }

  val trie = MutableTrie<V>(null)

  for ((str, value) in pairs) {
    str.fold (trie) { t, c -> t.getOrCreateChild(c) } .value = value
  }

  return trie
}
