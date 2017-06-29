package com.wcaokaze.japanecraft

import java.util.*

interface Trie<out V> {
  val childCount: Int
  val value: V?
  operator fun get(char: Char): Trie<V>?
}

private class MutableTrie<V>(override var value: V?) : Trie<V> {
  private val children: MutableMap<Char, MutableTrie<V>> = TreeMap()

  override val childCount get() = children.size

  override operator fun get(char: Char): MutableTrie<V>? = children[char]

  operator fun set(char: Char, child: MutableTrie<V>) {
    children[char] = child
  }
}

fun <V> trieOf(vararg pairs: Pair<String, V>): Trie<V> = mapOf(*pairs).toTrie()

fun <V> Map<String, V>.toTrie(): Trie<V> {
  fun MutableTrie<V>.getOrCreateChild(char: Char): MutableTrie<V> {
    var child = this[char]

    if (child == null) {
      child = MutableTrie(null)
      this[char] = child
    }

    return child
  }

  val trie = MutableTrie<V>(null)

  for ((str, value) in this) {
    str.fold (trie) { t, c -> t.getOrCreateChild(c) } .value = value
  }

  return trie
}
