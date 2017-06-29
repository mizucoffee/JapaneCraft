package com.wcaokaze.japanecraft

import com.wcaokaze.json.*
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
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.event.ServerChatEvent
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Mod(modid = "japanecraft", version = "0.4.0")
class JapaneCraftMod {
  private val kanjiConverter = KanjiConverter()
  private lateinit var romajiConverter: RomajiConverter
  private lateinit var timeFormatter: DateFormat
  private lateinit var variableExpander: VariableExpander

  @Mod.EventHandler
  fun preInit(event: FMLPreInitializationEvent) {
    val romajiTable = with (File("config/JapaneCraftRomajiTable.json")) {
      try {
        if (!exists()) createDefaultRomajiTableJson()

        class RomajiTableEntry(val input: String,
                               val output: String,
                               val nextInput: String)

        reader().buffered()
            .use {
              parseJson(it, list(
                  instance {
                    RomajiTableEntry(it["input", string],
                                     it["output", string],
                                     it["next_input", string, ""])
                  }
              ))
            }
            .map { it.input to RomajiConverter.Output(it.output, it.nextInput) }
            .toMap()
      } catch (e: IOException) {
        emptyMap<String, RomajiConverter.Output>()
      }
    }

    romajiConverter = RomajiConverter(romajiTable)

    loadConfig(File("config/JapaneCraft.cfg")) {
      val chatMsgFormat = it.getString("chat", "format",
          "<\$username> \$rawMessage\$n  §b\$convertedMessage",
          "The format for chat messages")

      variableExpander = VariableExpander(chatMsgFormat)

      val timeFormat = it.getString("time", "format",
          "HH:mm:ss", "The format for `\$time` in chat format")

      timeFormatter = SimpleDateFormat(timeFormat)
    }
  }

  @Mod.EventHandler
  fun init(event: FMLInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onServerChat(event: ServerChatEvent) {
    launch (CommonPool) {
      val (rawMessage, convertedMessage) = event.convertMessage()

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

  private suspend fun ServerChatEvent.convertMessage(): Pair<String, String> {
    val enMsg = message
    val jpMsg = enMsg.toJapanese()

    return when {
      //                                     raw   to converted
      enMsg.any { it >= 0x80.toChar() }   -> ""    to enMsg
      enMsg.filter { it != '`' } == jpMsg -> ""    to enMsg
      else                                -> enMsg to jpMsg
    }
  }

  suspend fun String.toJapanese(): String {
    try {
      class Chunk(val str: String, val shouldConvert: Boolean)

      val chunkList = LinkedList<Chunk>()

      for ((index, str) in split('`').withIndex()) {
        if (index % 2 != 0) {
          chunkList += Chunk(str, false)
        } else {
          for (word in str.split(' ')) {
            if (word.first().isUpperCase()) {
              chunkList += Chunk(word + ' ', false)
            } else {
              chunkList += Chunk(word, true)
            }
          }
        }
      }

      val convertedStrs = chunkList
          .filter { it.shouldConvert }
          .map { romajiConverter.convert(it.str) }
          .let { kanjiConverter.convert(it) }
          .await()

      val convertedStrIterator = convertedStrs.iterator()

      return chunkList
          .map {
            if (it.shouldConvert) {
              convertedStrIterator.next().kanjiList.first()
            } else {
              it.str
            }
          }
          .joinToString("")
    } catch (e: Exception) {
      return this
    }
  }

  private fun loadConfig(file: File, loadOperation: (Configuration) -> Unit) {
    val config = Configuration(file)
    config.load()
    loadOperation(config)
    config.save()
  }

  private fun File.createDefaultRomajiTableJson() = writeText("""
      [
        { "input": "-",          "output": "ー"                              },
        { "input": "~",          "output": "〜"                              },
        { "input": ".",          "output": "。"                              },
        { "input": ",",          "output": "、"                              },
        { "input": "z/",         "output": "・"                              },
        { "input": "z.",         "output": "…"                               },
        { "input": "z,",         "output": "‥"                               },
        { "input": "zh",         "output": "←"                               },
        { "input": "zj",         "output": "↓"                               },
        { "input": "zk",         "output": "↑"                               },
        { "input": "zl",         "output": "→"                               },
        { "input": "z-",         "output": "〜"                              },
        { "input": "z[",         "output": "『"                              },
        { "input": "z]",         "output": "』"                              },
        { "input": "[",          "output": "「"                              },
        { "input": "]",          "output": "」"                              },
        { "input": "va",         "output": "ゔぁ"                            },
        { "input": "vi",         "output": "ゔぃ"                            },
        { "input": "vu",         "output": "ゔ"                              },
        { "input": "ve",         "output": "ゔぇ"                            },
        { "input": "vo",         "output": "ゔぉ"                            },
        { "input": "vya",        "output": "ゔゃ"                            },
        { "input": "vyi",        "output": "ゔぃ"                            },
        { "input": "vyu",        "output": "ゔゅ"                            },
        { "input": "vye",        "output": "ゔぇ"                            },
        { "input": "vyo",        "output": "ゔょ"                            },
        { "input": "qq",         "output": "っ",       "next_input": "q"     },
        { "input": "vv",         "output": "っ",       "next_input": "v"     },
        { "input": "ll",         "output": "っ",       "next_input": "l"     },
        { "input": "xx",         "output": "っ",       "next_input": "x"     },
        { "input": "kk",         "output": "っ",       "next_input": "k"     },
        { "input": "gg",         "output": "っ",       "next_input": "g"     },
        { "input": "ss",         "output": "っ",       "next_input": "s"     },
        { "input": "zz",         "output": "っ",       "next_input": "z"     },
        { "input": "jj",         "output": "っ",       "next_input": "j"     },
        { "input": "tt",         "output": "っ",       "next_input": "t"     },
        { "input": "dd",         "output": "っ",       "next_input": "d"     },
        { "input": "hh",         "output": "っ",       "next_input": "h"     },
        { "input": "ff",         "output": "っ",       "next_input": "f"     },
        { "input": "bb",         "output": "っ",       "next_input": "b"     },
        { "input": "pp",         "output": "っ",       "next_input": "p"     },
        { "input": "mm",         "output": "っ",       "next_input": "m"     },
        { "input": "yy",         "output": "っ",       "next_input": "y"     },
        { "input": "rr",         "output": "っ",       "next_input": "r"     },
        { "input": "ww",         "output": "っ",       "next_input": "w"     },
        { "input": "www",        "output": "w",        "next_input": "ww"    },
        { "input": "cc",         "output": "っ",       "next_input": "c"     },
        { "input": "kya",        "output": "きゃ"                            },
        { "input": "kyi",        "output": "きぃ"                            },
        { "input": "kyu",        "output": "きゅ"                            },
        { "input": "kye",        "output": "きぇ"                            },
        { "input": "kyo",        "output": "きょ"                            },
        { "input": "gya",        "output": "ぎゃ"                            },
        { "input": "gyi",        "output": "ぎぃ"                            },
        { "input": "gyu",        "output": "ぎゅ"                            },
        { "input": "gye",        "output": "ぎぇ"                            },
        { "input": "gyo",        "output": "ぎょ"                            },
        { "input": "sya",        "output": "しゃ"                            },
        { "input": "syi",        "output": "しぃ"                            },
        { "input": "syu",        "output": "しゅ"                            },
        { "input": "sye",        "output": "しぇ"                            },
        { "input": "syo",        "output": "しょ"                            },
        { "input": "sha",        "output": "しゃ"                            },
        { "input": "shi",        "output": "し"                              },
        { "input": "shu",        "output": "しゅ"                            },
        { "input": "she",        "output": "しぇ"                            },
        { "input": "sho",        "output": "しょ"                            },
        { "input": "zya",        "output": "じゃ"                            },
        { "input": "zyi",        "output": "じぃ"                            },
        { "input": "zyu",        "output": "じゅ"                            },
        { "input": "zye",        "output": "じぇ"                            },
        { "input": "zyo",        "output": "じょ"                            },
        { "input": "tya",        "output": "ちゃ"                            },
        { "input": "tyi",        "output": "ちぃ"                            },
        { "input": "tyu",        "output": "ちゅ"                            },
        { "input": "tye",        "output": "ちぇ"                            },
        { "input": "tyo",        "output": "ちょ"                            },
        { "input": "cha",        "output": "ちゃ"                            },
        { "input": "chi",        "output": "ち"                              },
        { "input": "chu",        "output": "ちゅ"                            },
        { "input": "che",        "output": "ちぇ"                            },
        { "input": "cho",        "output": "ちょ"                            },
        { "input": "cya",        "output": "ちゃ"                            },
        { "input": "cyi",        "output": "ちぃ"                            },
        { "input": "cyu",        "output": "ちゅ"                            },
        { "input": "cye",        "output": "ちぇ"                            },
        { "input": "cyo",        "output": "ちょ"                            },
        { "input": "dya",        "output": "ぢゃ"                            },
        { "input": "dyi",        "output": "ぢぃ"                            },
        { "input": "dyu",        "output": "ぢゅ"                            },
        { "input": "dye",        "output": "ぢぇ"                            },
        { "input": "dyo",        "output": "ぢょ"                            },
        { "input": "tsa",        "output": "つぁ"                            },
        { "input": "tsi",        "output": "つぃ"                            },
        { "input": "tse",        "output": "つぇ"                            },
        { "input": "tso",        "output": "つぉ"                            },
        { "input": "tha",        "output": "てゃ"                            },
        { "input": "thi",        "output": "てぃ"                            },
        { "input": "t'i",        "output": "てぃ"                            },
        { "input": "thu",        "output": "てゅ"                            },
        { "input": "the",        "output": "てぇ"                            },
        { "input": "tho",        "output": "てょ"                            },
        { "input": "t'yu",       "output": "てゅ"                            },
        { "input": "dha",        "output": "でゃ"                            },
        { "input": "dhi",        "output": "でぃ"                            },
        { "input": "d'i",        "output": "でぃ"                            },
        { "input": "dhu",        "output": "でゅ"                            },
        { "input": "dhe",        "output": "でぇ"                            },
        { "input": "dho",        "output": "でょ"                            },
        { "input": "d'yu",       "output": "でゅ"                            },
        { "input": "twa",        "output": "とぁ"                            },
        { "input": "twi",        "output": "とぃ"                            },
        { "input": "twu",        "output": "とぅ"                            },
        { "input": "twe",        "output": "とぇ"                            },
        { "input": "two",        "output": "とぉ"                            },
        { "input": "t'u",        "output": "とぅ"                            },
        { "input": "dwa",        "output": "どぁ"                            },
        { "input": "dwi",        "output": "どぃ"                            },
        { "input": "dwu",        "output": "どぅ"                            },
        { "input": "dwe",        "output": "どぇ"                            },
        { "input": "dwo",        "output": "どぉ"                            },
        { "input": "d'u",        "output": "どぅ"                            },
        { "input": "nya",        "output": "にゃ"                            },
        { "input": "nyi",        "output": "にぃ"                            },
        { "input": "nyu",        "output": "にゅ"                            },
        { "input": "nye",        "output": "にぇ"                            },
        { "input": "nyo",        "output": "にょ"                            },
        { "input": "hya",        "output": "ひゃ"                            },
        { "input": "hyi",        "output": "ひぃ"                            },
        { "input": "hyu",        "output": "ひゅ"                            },
        { "input": "hye",        "output": "ひぇ"                            },
        { "input": "hyo",        "output": "ひょ"                            },
        { "input": "bya",        "output": "びゃ"                            },
        { "input": "byi",        "output": "びぃ"                            },
        { "input": "byu",        "output": "びゅ"                            },
        { "input": "bye",        "output": "びぇ"                            },
        { "input": "byo",        "output": "びょ"                            },
        { "input": "pya",        "output": "ぴゃ"                            },
        { "input": "pyi",        "output": "ぴぃ"                            },
        { "input": "pyu",        "output": "ぴゅ"                            },
        { "input": "pye",        "output": "ぴぇ"                            },
        { "input": "pyo",        "output": "ぴょ"                            },
        { "input": "fa",         "output": "ふぁ"                            },
        { "input": "fi",         "output": "ふぃ"                            },
        { "input": "fu",         "output": "ふ"                              },
        { "input": "fe",         "output": "ふぇ"                            },
        { "input": "fo",         "output": "ふぉ"                            },
        { "input": "fya",        "output": "ふゃ"                            },
        { "input": "fyu",        "output": "ふゅ"                            },
        { "input": "fyo",        "output": "ふょ"                            },
        { "input": "hwa",        "output": "ふぁ"                            },
        { "input": "hwi",        "output": "ふぃ"                            },
        { "input": "hwe",        "output": "ふぇ"                            },
        { "input": "hwo",        "output": "ふぉ"                            },
        { "input": "hwyu",       "output": "ふゅ"                            },
        { "input": "mya",        "output": "みゃ"                            },
        { "input": "myi",        "output": "みぃ"                            },
        { "input": "myu",        "output": "みゅ"                            },
        { "input": "mye",        "output": "みぇ"                            },
        { "input": "myo",        "output": "みょ"                            },
        { "input": "rya",        "output": "りゃ"                            },
        { "input": "ryi",        "output": "りぃ"                            },
        { "input": "ryu",        "output": "りゅ"                            },
        { "input": "rye",        "output": "りぇ"                            },
        { "input": "ryo",        "output": "りょ"                            },
        { "input": "n'",         "output": "ん"                              },
        { "input": "nn",         "output": "ん"                              },
        { "input": "n",          "output": "ん"                              },
        { "input": "xn",         "output": "ん"                              },
        { "input": "a",          "output": "あ"                              },
        { "input": "i",          "output": "い"                              },
        { "input": "u",          "output": "う"                              },
        { "input": "wu",         "output": "う"                              },
        { "input": "e",          "output": "え"                              },
        { "input": "o",          "output": "お"                              },
        { "input": "xa",         "output": "ぁ"                              },
        { "input": "xi",         "output": "ぃ"                              },
        { "input": "xu",         "output": "ぅ"                              },
        { "input": "xe",         "output": "ぇ"                              },
        { "input": "xo",         "output": "ぉ"                              },
        { "input": "la",         "output": "ぁ"                              },
        { "input": "li",         "output": "ぃ"                              },
        { "input": "lu",         "output": "ぅ"                              },
        { "input": "le",         "output": "ぇ"                              },
        { "input": "lo",         "output": "ぉ"                              },
        { "input": "lyi",        "output": "ぃ"                              },
        { "input": "xyi",        "output": "ぃ"                              },
        { "input": "lye",        "output": "ぇ"                              },
        { "input": "xye",        "output": "ぇ"                              },
        { "input": "ye",         "output": "いぇ"                            },
        { "input": "ka",         "output": "か"                              },
        { "input": "ki",         "output": "き"                              },
        { "input": "ku",         "output": "く"                              },
        { "input": "ke",         "output": "け"                              },
        { "input": "ko",         "output": "こ"                              },
        { "input": "xka",        "output": "ヵ"                              },
        { "input": "xke",        "output": "ヶ"                              },
        { "input": "lka",        "output": "ヵ"                              },
        { "input": "lke",        "output": "ヶ"                              },
        { "input": "ga",         "output": "が"                              },
        { "input": "gi",         "output": "ぎ"                              },
        { "input": "gu",         "output": "ぐ"                              },
        { "input": "ge",         "output": "げ"                              },
        { "input": "go",         "output": "ご"                              },
        { "input": "sa",         "output": "さ"                              },
        { "input": "si",         "output": "し"                              },
        { "input": "su",         "output": "す"                              },
        { "input": "se",         "output": "せ"                              },
        { "input": "so",         "output": "そ"                              },
        { "input": "ca",         "output": "か"                              },
        { "input": "ci",         "output": "し"                              },
        { "input": "cu",         "output": "く"                              },
        { "input": "ce",         "output": "せ"                              },
        { "input": "co",         "output": "こ"                              },
        { "input": "qa",         "output": "くぁ"                            },
        { "input": "qi",         "output": "くぃ"                            },
        { "input": "qu",         "output": "く"                              },
        { "input": "qe",         "output": "くぇ"                            },
        { "input": "qo",         "output": "くぉ"                            },
        { "input": "kwa",        "output": "くぁ"                            },
        { "input": "kwi",        "output": "くぃ"                            },
        { "input": "kwu",        "output": "くぅ"                            },
        { "input": "kwe",        "output": "くぇ"                            },
        { "input": "kwo",        "output": "くぉ"                            },
        { "input": "gwa",        "output": "ぐぁ"                            },
        { "input": "gwi",        "output": "ぐぃ"                            },
        { "input": "gwu",        "output": "ぐぅ"                            },
        { "input": "gwe",        "output": "ぐぇ"                            },
        { "input": "gwo",        "output": "ぐぉ"                            },
        { "input": "za",         "output": "ざ"                              },
        { "input": "zi",         "output": "じ"                              },
        { "input": "zu",         "output": "ず"                              },
        { "input": "ze",         "output": "ぜ"                              },
        { "input": "zo",         "output": "ぞ"                              },
        { "input": "ja",         "output": "じゃ"                            },
        { "input": "ji",         "output": "じ"                              },
        { "input": "ju",         "output": "じゅ"                            },
        { "input": "je",         "output": "じぇ"                            },
        { "input": "jo",         "output": "じょ"                            },
        { "input": "jya",        "output": "じゃ"                            },
        { "input": "jyi",        "output": "じぃ"                            },
        { "input": "jyu",        "output": "じゅ"                            },
        { "input": "jye",        "output": "じぇ"                            },
        { "input": "jyo",        "output": "じょ"                            },
        { "input": "ta",         "output": "た"                              },
        { "input": "ti",         "output": "ち"                              },
        { "input": "tu",         "output": "つ"                              },
        { "input": "tsu",        "output": "つ"                              },
        { "input": "te",         "output": "て"                              },
        { "input": "to",         "output": "と"                              },
        { "input": "da",         "output": "だ"                              },
        { "input": "di",         "output": "ぢ"                              },
        { "input": "du",         "output": "づ"                              },
        { "input": "de",         "output": "で"                              },
        { "input": "do",         "output": "ど"                              },
        { "input": "xtu",        "output": "っ"                              },
        { "input": "xtsu",       "output": "っ"                              },
        { "input": "ltu",        "output": "っ"                              },
        { "input": "ltsu",       "output": "っ"                              },
        { "input": "na",         "output": "な"                              },
        { "input": "ni",         "output": "に"                              },
        { "input": "nu",         "output": "ぬ"                              },
        { "input": "ne",         "output": "ね"                              },
        { "input": "no",         "output": "の"                              },
        { "input": "ha",         "output": "は"                              },
        { "input": "hi",         "output": "ひ"                              },
        { "input": "hu",         "output": "ふ"                              },
        { "input": "fu",         "output": "ふ"                              },
        { "input": "he",         "output": "へ"                              },
        { "input": "ho",         "output": "ほ"                              },
        { "input": "ba",         "output": "ば"                              },
        { "input": "bi",         "output": "び"                              },
        { "input": "bu",         "output": "ぶ"                              },
        { "input": "be",         "output": "べ"                              },
        { "input": "bo",         "output": "ぼ"                              },
        { "input": "pa",         "output": "ぱ"                              },
        { "input": "pi",         "output": "ぴ"                              },
        { "input": "pu",         "output": "ぷ"                              },
        { "input": "pe",         "output": "ぺ"                              },
        { "input": "po",         "output": "ぽ"                              },
        { "input": "ma",         "output": "ま"                              },
        { "input": "mi",         "output": "み"                              },
        { "input": "mu",         "output": "む"                              },
        { "input": "me",         "output": "め"                              },
        { "input": "mo",         "output": "も"                              },
        { "input": "xya",        "output": "ゃ"                              },
        { "input": "lya",        "output": "ゃ"                              },
        { "input": "ya",         "output": "や"                              },
        { "input": "wyi",        "output": "ゐ"                              },
        { "input": "xyu",        "output": "ゅ"                              },
        { "input": "lyu",        "output": "ゅ"                              },
        { "input": "yu",         "output": "ゆ"                              },
        { "input": "wye",        "output": "ゑ"                              },
        { "input": "xyo",        "output": "ょ"                              },
        { "input": "lyo",        "output": "ょ"                              },
        { "input": "yo",         "output": "よ"                              },
        { "input": "ra",         "output": "ら"                              },
        { "input": "ri",         "output": "り"                              },
        { "input": "ru",         "output": "る"                              },
        { "input": "re",         "output": "れ"                              },
        { "input": "ro",         "output": "ろ"                              },
        { "input": "xwa",        "output": "ゎ"                              },
        { "input": "lwa",        "output": "ゎ"                              },
        { "input": "wa",         "output": "わ"                              },
        { "input": "wi",         "output": "うぃ"                            },
        { "input": "we",         "output": "うぇ"                            },
        { "input": "wo",         "output": "を"                              },
        { "input": "wha",        "output": "うぁ"                            },
        { "input": "whi",        "output": "うぃ"                            },
        { "input": "whu",        "output": "う"                              },
        { "input": "whe",        "output": "うぇ"                            },
        { "input": "who",        "output": "うぉ"                            }
      ]
  """.trimIndent())
}
