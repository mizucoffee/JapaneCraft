package com.wcaokaze.japanecraft

import com.wcaokaze.json.JsonParseException
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.NetworkCheckHandler
import cpw.mods.fml.relauncher.Side
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import java.util.*

@Mod(modid = "japanecraft", version = "1.1.2")
class JapaneCraftMod {
  private val configuration = Configuration()
  private val kanjiConverter = KanjiConverter()

  @Mod.EventHandler
  fun init(event: FMLInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onServerChat(event: ServerChatEvent) {
    launch (CommonPool) {
      val (rawMessage, convertedMessage) = convert(event.message)

      val variableMap = mapOf(
          "n"                to "\n",
          "$"                to "\$",
          "username"         to event.username,
          "time"             to configuration.timeFormatter.format(Date()),
          "rawMessage"       to rawMessage,
          "convertedMessage" to convertedMessage
      )

      configuration.variableExpander.expand(variableMap).split('\n').forEach {
        FMLCommonHandler
            .instance()
            .minecraftServerInstance
            .configurationManager
            .sendChatMsg(ChatComponentText(it))
      }
    }

    event.isCanceled = true
  }

  @NetworkCheckHandler
  fun netCheckHandler(mods: Map<String, String>, side: Side): Boolean {
    return true
  }

  private suspend fun convert(message: String): Pair<String, String> {
    if (message.any { it >= 0x80.toChar() }) return "" to message

    val enMsg = message
    val jpMsg = enMsg.toJapanese()

    if (enMsg.filter { it != '`' } == jpMsg) return "" to enMsg

    return enMsg to jpMsg
  }

  private enum class Language { ENGLISH, ROMAJI, HIRAGANA, KANJI }
  private data class Chunk(val word: String, val language: Language)

  private suspend fun String.toJapanese(): String {
    try {
      val chunkList = resolveIntoChunks(this)
          .map {
            when (it.language) {
              Language.ENGLISH -> it

              Language.ROMAJI -> {
                Chunk(configuration.romajiConverter.convert(it.word),
                      Language.HIRAGANA)
              }

              else -> throw AssertionError()
            }
          }
          .map { it.applyDictionary(configuration.dictionary) }
          .flatten()

      if (configuration.kanjiConverterEnabled) {
        val kanjiList = chunkList
            .filter { it.language == Language.HIRAGANA }
            .map { it.word }
            .let { kanjiConverter.convert(it).await() }
            .map { it.kanjiList.firstOrNull() ?: throw JsonParseException() }
            .toMutableList()

        if (chunkList.count { it.language == Language.HIRAGANA }
            != kanjiList.size)
        {
          throw JsonParseException()
        }

        val chunkListIterator = chunkList.listIterator()

        return buildString {
          for ((word, language) in chunkListIterator) {
            when (language) {
              Language.HIRAGANA -> append(kanjiList.removeAt(0))

              else -> {
                append(word)

                val nextLanguage = chunkListIterator.peekNextOrNull()?.language

                if (language     == Language.ENGLISH &&
                    nextLanguage == Language.ENGLISH) append(' ')
              }
            }
          }
        }
      } else {
        return chunkList.map { it.word }.joinToString(" ")
      }
    } catch (e: Exception) {
      return this
    }
  }

  private fun resolveIntoChunks(str: String)
      = str.split('`')
        .withIndex()
        .map {
          val isEnglishBlock = it.index % 2 != 0

          it.value
              .split(configuration.wordSeparators)
              .map { word ->
                val language = when {
                  isEnglishBlock                          -> Language.ENGLISH
                  word.matches(configuration.romajiRegex) -> Language.ROMAJI
                  else                                    -> Language.ENGLISH
                }

                Chunk(word, language)
              }
        }
        .flatten()

  private fun Chunk.applyDictionary(dictionary: Dictionary): List<Chunk> {
    val convertedChunkList = LinkedList<Chunk>()

    fun loop(rawString: String) {
      if (rawString.isEmpty()) return

      dictionary[rawString]?.let { (converted, indices) ->
        if (converted.isEmpty()) return

        val language = if (converted.any { it >= 0x80.toChar() }) {
          Language.KANJI
        } else {
          Language.ENGLISH
        }

        convertedChunkList += Chunk(converted, language)
        loop(rawString.removeRange(indices))
      } ?: run {
        convertedChunkList += Chunk(rawString, language)
      }
    }

    loop(word)

    return convertedChunkList
  }

  private fun String.split(separators: CharArray): List<String> {
    val splited = LinkedList<String>()

    var i = 0

    for (j in indices) {
      if (this[j] in separators) {
        if (j > i) splited += substring(i, j)

        if (!this[j].isWhitespace()) splited += substring(j, j + 1)

        i = j + 1
      }
    }

    if (i < lastIndex) splited += substring(i)

    return splited
  }

  private fun <T> ListIterator<T>.peekNextOrNull(): T? {
    if (!hasNext()) return null

    val next = next()
    previous()
    return next
  }
}
