package com.wcaokaze.japanecraft

import com.wcaokaze.json.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.net.URL

fun main(vararg args: String) = runBlocking {
  KanjiConverter().convert(listOf("りんご", "ばなな", "あるみかん")).await()
      .forEach(::println)
}

class KanjiConverter {
  private class GoogleCgiEntry(val hiragana: String, val kanjiList: List<String>)

  private val googleCgiEntry: JsonConverter<GoogleCgiEntry> = {
    if (this !is JsonDatum.List) throw JsonParseException()
    if (value.size != 2)         throw JsonParseException()

    GoogleCgiEntry(value[0].run(string),
                   value[1].run(list(string)))
  }

  suspend fun convert(hiraganaList: List<String>): Deferred<List<String>>
      = async (CommonPool) {
        val googleCgiEntryList = URL("http://www.google.com/transliterate?langpair=ja-Hira|ja&text=%E3%81%B8%E3%82%93%E3%81%8B%E3%82%93")
            .openStream().reader().buffered().use {
          parseJson(it, list(googleCgiEntry))
        }

        googleCgiEntryList.map { it.kanjiList.first() }
      }
}
