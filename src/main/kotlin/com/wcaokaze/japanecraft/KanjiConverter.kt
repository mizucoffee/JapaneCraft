package com.wcaokaze.japanecraft

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.j2on.kotlin.JsonException
import org.j2on.kotlin.JsonParser
import java.net.URL
import java.net.URLEncoder

class KanjiConverter {
  class GoogleCgiEntry(val hiragana: String, val kanjiList: List<String>)

  suspend fun convert(hiraganaList: List<String>): Deferred<List<GoogleCgiEntry>>
      = async {
        if (hiraganaList.isEmpty()) return@async emptyList<GoogleCgiEntry>()

        val encodedHiraganaList = hiraganaList
            .joinToString(",") { URLEncoder.encode(it, "UTF-8") }

        URL("http://www.google.com/transliterate?langpair=ja-Hira|ja&text="
            + encodedHiraganaList + ','
        )
            .openStream().bufferedReader().use {
              JsonParser()
                  .parseList<Any>(it)
                  .map {
                    fun throwInvalidJsonException(): Nothing
                        = throw JsonException("Invalid JSON was returned")

                    if (it !is List<*>) throwInvalidJsonException()

                    val hiragana  = it[0] as? String  ?: throwInvalidJsonException()
                    val kanjiList = it[1] as? List<*> ?: throwInvalidJsonException()

                    if (kanjiList.any { it !is String }) throwInvalidJsonException()

                    @Suppress("UNCHECKED_CAST")
                    GoogleCgiEntry(hiragana, kanjiList as List<String>)
                  }
            }
      }
}
