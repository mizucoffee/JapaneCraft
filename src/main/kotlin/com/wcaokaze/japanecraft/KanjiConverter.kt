package com.wcaokaze.japanecraft

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

class KanjiConverter {
  suspend fun convert(hiraganaList: Iterable<String>): Deferred<List<String>>
      = async (CommonPool) {
      }
}
