package com.wcaokaze.japanecraft

import com.wcaokaze.json.JsonParseException
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.NetworkCheckHandler
import cpw.mods.fml.relauncher.Side
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Mod(modid = "japanecraft", version = "1.0.0")
class JapaneCraftMod {
  private var kanjiConverter: KanjiConverter? = null
  private lateinit var romajiConverter: RomajiConverter
  private lateinit var timeFormatter: DateFormat
  private lateinit var variableExpander: VariableExpander

  @Mod.EventHandler
  fun preInit(event: FMLPreInitializationEvent) {
    val configuration = Configuration.load()

    romajiConverter = RomajiConverter(configuration.romajiTable)
    variableExpander = VariableExpander(configuration.chatMsgFormat)
    timeFormatter = SimpleDateFormat(configuration.timeFormat)
    if (configuration.kanjiConverterEnabled) kanjiConverter = KanjiConverter()
  }

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
          "time"             to timeFormatter.format(Date()),
          "rawMessage"       to rawMessage,
          "convertedMessage" to convertedMessage
      )

      variableExpander.expand(variableMap).split('\n').forEach {
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

  private class Chunk(val str: String, val shouldConvert: Boolean)

  private suspend fun String.toJapanese(): String {
    try {
      val chunkList = resolveIntoChunks(this)

      if (kanjiConverter != null) {
        val kanjiList = chunkList
            .filter { it.shouldConvert }
            .map { romajiConverter.convert(it.str) }
            .let { kanjiConverter!!.convert(it).await() }
            .map { it.kanjiList.firstOrNull() ?: throw JsonParseException() }

        if (chunkList.count { it.shouldConvert } != kanjiList.size) {
          throw JsonParseException()
        }

        val chunkListIterator = chunkList.listIterator()
        val kanjiListIterator = kanjiList.iterator()

        return buildString {
          for (chunk in chunkListIterator) {
            if (chunk.shouldConvert) {
              append(kanjiListIterator.next())
            } else {
              append(chunk.str)

              val shouldInsertSpace = run {
                if (!chunkListIterator.hasNext()) return@run false

                val nextIsAlsoEnglish = !chunkListIterator.next().shouldConvert
                chunkListIterator.previous()

                return@run nextIsAlsoEnglish
              }

              if (shouldInsertSpace) append(' ')
            }
          }
        }
      } else {
        return chunkList.map {
          if (it.shouldConvert) romajiConverter.convert(it.str) else it.str
        } .joinToString(" ")
      }
    } catch (e: Exception) {
      return this
    }
  }

  private fun resolveIntoChunks(str: String): List<Chunk> {
    val chunkList = LinkedList<Chunk>()

    for ((index, surroundedStr) in str.split('`').withIndex()) {
      if (index % 2 != 0) {
        chunkList += Chunk(surroundedStr, false)
      } else {
        surroundedStr
            .split(' ')
            .filter(String::isNotEmpty)
            .forEach { word ->
              chunkList += Chunk(word, word.first().isLowerCase())
            }
      }
    }

    return chunkList
  }
}
