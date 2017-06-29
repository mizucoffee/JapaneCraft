package com.wcaokaze.japanecraft

import com.wcaokaze.json.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.net.URL
import java.net.URLEncoder

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
        val encodedHiraganaList = hiraganaList
            .map { URLEncoder.encode(it, "UTF-8") }
            .joinToString(",")

        val googleCgiEntryList
            = URL("http://www.google.com/transliterate?langpair=ja-Hira|ja&text="
                  + encodedHiraganaList)
            .openStream().reader().buffered().use {
              parseJson(it, list(googleCgiEntry))
            }

        googleCgiEntryList.map { it.kanjiList.first() }
      }
}
