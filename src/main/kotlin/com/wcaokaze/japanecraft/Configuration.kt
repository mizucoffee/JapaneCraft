package com.wcaokaze.japanecraft

import com.wcaokaze.json.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import kotlin.reflect.KProperty
import net.minecraftforge.common.config.Configuration as ConfigLoader

class Configuration {
  val wordSeparators by autoReload(File("config/JapaneCraft.cfg")) {
    it.loadString(
        category = "advanced",
        key      = "wordSeparator",
        default  = "\t\n \"'()<>@[]{}",
        comment  = null)
        .toCharArray()
  }

  val romajiRegex by autoReload(File("config/JapaneCraft.cfg")) {
    val pattern = it.loadString(
        category = "advanced",
        key      = "romajiRegex",
        default  = "\\d*[a-z].*",
        comment  = null)

    Regex(pattern)
  }

  val romajiConverter by autoReload(File("config/JapaneCraftRomajiTable.json")) {
    if (!it.exists()) it.writeText(defaultRomajiTableJson())

    class RomajiTableEntry(val input: String,
                           val output: String,
                           val nextInput: String)

    val romajiTableEntry = instance {
      RomajiTableEntry(it["input", string],
                       it["output", string],
                       it["next_input", string, ""])
    }

    val romajiTableMap =
        loadJson(it, list(romajiTableEntry), defaultRomajiTableJson)
        .map { it.input to RomajiConverter.Output(it.output, it.nextInput) }
        .toMap()

    RomajiConverter(romajiTableMap)
  }

  val dictionary by autoReload(File("config/JapaneCraftDictionary.json")) {
    val dictionaryMap =
        loadJson(File("config/JapaneCraftDictionary.json"), map(string)) {
          """
            {
              "いし": "石",
              "すけさん": "スケさん",
              "くも": "クモ",
              "つるはし": "ツルハシ"
            }
          """.trimIndent()
        }

    Dictionary(dictionaryMap)
  }

  val kanjiConverterEnabled by autoReload(File("config/JapaneCraft.cfg")) {
    it.loadBoolean(
        category = "mode",
        key      = "enableConvertingToKanji",
        default  = true,
        comment  = "Whether to convert hiragana to kanji")
  }

  val timeFormatter by autoReload(File("config/JapaneCraft.cfg")) {
    val timeFormat = it.loadString(
        category = "format",
        key      = "time",
        default  = "HH:mm:ss",
        comment  = "The format for `\$time` in chat format")

    SimpleDateFormat(timeFormat)
  }

  val variableExpander by autoReload(File("config/JapaneCraft.cfg")) {
    val chatMsgFormat = it.loadString(
        category = "format",
        key      = "chat",
        default  = "<\$username> \$rawMessage\$n  §b\$convertedMessage",
        comment  = "The format for chat messages")

    VariableExpander(chatMsgFormat)
  }

  private fun <T> autoReload(file: File, loadOperation: (File) -> T) = object {
    private var loadDate = 0L
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
      if (!file.exists() || loadDate < file.lastModified()) {
        try {
          value = loadOperation(file)
        } catch (e: IOException) {
          if (value == null) throw e
        }

        loadDate = System.currentTimeMillis()
      }

      return value!!
    }
  }

  private fun File.loadString(category: String,
                              key: String,
                              default: String,
                              comment: String?): String
      = with (ConfigLoader(this)) {
        load()
        val value = getString(key, category, default, comment)
        save()
        return value
      }

  private fun File.loadBoolean(category: String,
                               key: String,
                               default: Boolean,
                               comment: String?): Boolean
      = with (ConfigLoader(this)) {
        load()
        val value = getBoolean(key, category, default, comment)
        save()
        return value
      }

  private fun <T> loadJson(file: File,
                           jsonConverter: JsonConverter<T>,
                           defaultLazy: () -> String): T
  {
    if (!file.exists()) file.writeText(defaultLazy())

    return file.reader().buffered().use { parseJson(it, jsonConverter) }
  }

  private val defaultRomajiTableJson = { """
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
        { "input": "cc",         "output": "っ",       "next_input": "c"     },
        { "input": "www",        "output": "w",        "next_input": "ww"    },
        { "input": "wwu",        "output": "っう"                            },
        { "input": "wwyi",       "output": "っゐ"                            },
        { "input": "wwye",       "output": "っゑ"                            },
        { "input": "wwa",        "output": "っわ"                            },
        { "input": "wwi",        "output": "っうぃ"                          },
        { "input": "wwe",        "output": "っうぇ"                          },
        { "input": "wwo",        "output": "っを"                            },
        { "input": "wwha",       "output": "っうぁ"                          },
        { "input": "wwhi",       "output": "っうぃ"                          },
        { "input": "wwhu",       "output": "っう"                            },
        { "input": "wwhe",       "output": "っうぇ"                          },
        { "input": "wwho",       "output": "っうぉ"                          },
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
  """.trimIndent() }
}
