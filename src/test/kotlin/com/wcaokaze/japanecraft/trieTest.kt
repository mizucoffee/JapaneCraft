package com.wcaokaze.japanecraft

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TrieTest {
  @Test fun test() {
    val trie = trieOf(
        "serve"  to "仕える",
        "server" to "サーバー"
    )

    assertNotNull(trie['s'])
    assertNull(trie['b'])

    assertNull(trie['s']!!.value)

    val node = trie['s']!!['e']!!['r']!!['v']!!['e']!!
    assertEquals(node.value, "仕える")
    assertEquals(node['r']!!.value, "サーバー")
  }
}
