package com.wcaokaze.japanecraft

import org.j2on.kotlin.JsonException
import org.j2on.kotlin.JsonParser
import java.net.URL
import java.net.URLEncoder
import java.util.*

class KanjiConverter {
  operator fun invoke(string: String): String {
    val words = string.split('`')
        .asSequence()
        .mapIndexed { idx, str ->
          if (idx % 2 == 1) {
            listOf(str)
          } else {
            Configuration.splitWords(str)
          }
        }
        .flatten()
        // Google's bug?
        // The server fails when it receives Strings like "wwwwwwwww".
        .map(::minceKusa)
        .flatten()

    val parameter = words
        .map { URLEncoder.encode(it, "UTF-8") }
        .joinToString(",")

    val parsedResponse = try {
      URL(
          "http://www.google.com/transliterate?langpair=ja-Hira|ja&text=$parameter"
      ).openStream().bufferedReader().use {
        val response = JsonParser().parseList<Any>(it)
        parseResponse(response)
      }
    } catch (e: Exception) {
      words.map { it to it }.toList()
    }

    val ENGLISH    = 0
    val JAPANESE   = 1
    val WHITESPACE = 2

    val wordTypeList = parsedResponse
        .map { (rawString, kanjiString) ->
          val wordType = when {
            rawString.all { it.isWhitespace() } -> WHITESPACE
            rawString.all { it < 0x80.toChar() } -> ENGLISH
            else -> JAPANESE
          }

          wordType to kanjiString
        }
        .toMutableList()

    val iterator = wordTypeList.listIterator()

    while (true) {
      // NOTE: The cursor of ListIterator points between elements.

      iterator.find { (type, _) -> type == JAPANESE } ?: break
      // [ ... , (current) JAPANESE, ... ]
      //                           ^

      iterator.previous()
      // [ ... , (current) JAPANESE, ... ]
      //       ^

      if (!iterator.hasPrevious()) {
        // [(current) JAPANESE, ... ]
        // ^

        iterator.next()
        // [(current) JAPANESE, ... ]
        //                    ^

        continue
      }

      val prevWord = iterator.findPrevious { (type, _) -> type != WHITESPACE }
      // [ ... , (prevWord) (ENGLISH|JAPANESE), ... , (current) JAPANESE, ... ]
      //       ^

      iterator.next()
      // [ ... , (prevWord) (ENGLISH|JAPANESE), ... , (current) JAPANESE, ... ]
      //                                      ^

      if (prevWord != null && prevWord.first == JAPANESE) {
        iterator.removeWhile { (type, _) -> type == WHITESPACE }
        // [ ... , (current) JAPANESE, ... ]
        //                           ^
      } else {
        iterator.find { (type, _) -> type == JAPANESE }
        // [ ... , (prevWord) (ENGLISH), ... , (current) JAPANESE, ... ]
        //                                                       ^
      }
    }

    return wordTypeList.joinToString("") { (_, word) -> word }
  }

  private fun <T> ListIterator<T>.find(condition: (T) -> Boolean): T? {
    while (hasNext()) {
      val next = next()

      if (condition(next)) return next
    }

    return null
  }

  private fun <T> ListIterator<T>.findPrevious(condition: (T) -> Boolean): T? {
    while (hasPrevious()) {
      val previous = previous()

      if (condition(previous)) return previous
    }

    return null
  }

  private fun <T> MutableListIterator<T>.removeWhile(condition: (T) -> Boolean) {
    while (hasNext()) {
      if (condition(next())) {
        remove()
      } else {
        return
      }
    }
  }

  private fun minceKusa(str: String): List<String> {
    val splitWords = LinkedList<String>()

    var startIdx = 0
    var i = -1

    while (++i <= str.length - 6) {
      if (str[i] >= 0x80.toChar()) continue

      val lower = str[i].toLowerCase()
      val upper = str[i].toUpperCase()

      if (str.subSequence(i + 1, i + 6).all { it == lower || it == upper }) {
        if (i > startIdx) {
          splitWords += str.substring(startIdx, i)
        }

        splitWords += str.substring(i, i + 6)
        startIdx = i + 6
        i += 5
      }
    }

    if (startIdx < str.length) {
      splitWords += str.substring(startIdx)
    }

    return splitWords
  }

  private fun parseResponse(list: List<*>): List<Pair<String, String>> {
    fun throwJsonException(): Nothing
        = throw JsonException("Invalid JSON was returned")

    if (list.isEmpty()) throwJsonException()

    return list.map {
      if (it !is List<*>) throwJsonException()
      if (it.size != 2)   throwJsonException()

      val rawString = it[0] as? String  ?: throwJsonException()
      val kanjiList = it[1] as? List<*> ?: throwJsonException()

      val kanjiString = kanjiList.firstOrNull() as? String ?: rawString

      rawString to kanjiString
    }
  }
}
