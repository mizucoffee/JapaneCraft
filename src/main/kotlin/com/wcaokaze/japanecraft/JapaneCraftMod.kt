package com.wcaokaze.japanecraft

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.NetworkCheckHandler
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@Mod(modid = "japanecraft", version = "0.3.2")
class JapaneCraftMod {
  private val timeFormatter = SimpleDateFormat("HH:mm:ss")

  private val variableExpander
      = VariableExpander("<\$username> \$rawMessage\$n  §b\$convertedMessage")

  @Mod.EventHandler
  fun init(event: FMLInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onServerChat(event: ServerChatEvent) {
    val variableMap: MutableMap<String, String> = HashMap()

    val (rawMessage, convertedMessage) = event.convertMessage()

    variableMap["username"]         = event.username
    variableMap["time"]             = timeFormatter.format(Date())
    variableMap["rawMessage"]       = rawMessage
    variableMap["convertedMessage"] = convertedMessage

    variableExpander.expand(variableMap).split('\n').forEach {
      FMLCommonHandler
          .instance()
          .minecraftServerInstance
          .configurationManager
          .sendChatMsg(ChatComponentText(it))
    }

    event.isCanceled = true
  }

  @NetworkCheckHandler
  fun netCheckHandler(mods: Map<String, String>, side: Side): Boolean {
    return true
  }

  private fun ServerChatEvent.convertMessage(): Pair<String, String> {
    val enMsg = message
    val jpMsg = enMsg.toJapanese()

    return when {
      //                                     raw   to converted
      enMsg.any { it >= 0x80.toChar() }   -> ""    to enMsg
      enMsg.filter { it != '`' } == jpMsg -> ""    to enMsg
      else                                -> enMsg to jpMsg
    }
  }

  private fun String.toJapanese(): String {
    val romajiStr = this

    return buildString {
      for ((index, str) in romajiStr.split('`').withIndex()) {
        if (index % 2 != 0) {
          append(str)
        } else {
          for (word in str.split(' ')) {
            when {
              word.isEmpty() -> {
                append(' ')
              }

              word.first().isUpperCase() -> {
                append(word)
                append(' ')
              }

              else -> {
                append(RomajiConverter.convert(word))
                append(' ')
              }
            }
          }

          deleteCharAt(lastIndex)
        }
      }
    }
  }
}
