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
    fun sendChatMsg(msg: String) = FMLCommonHandler
        .instance()
        .minecraftServerInstance
        .configurationManager
        .sendChatMsg(ChatComponentText(msg))

    val variableMap: MutableMap<String, String> = HashMap()

    variableMap["username"] = event.username

    val rawMessage = event.message
    val convertedMessage = rawMessage.toJapanese()

    if (rawMessage.any { it >= 0x80.toChar() } ||
        rawMessage.filter { it != '`' } == convertedMessage)
    {
      variableMap["rawMessage"] = ""
      variableMap["convertedMessage"] = rawMessage
    } else {
      variableMap["rawMessage"] = rawMessage
      variableMap["convertedMessage"] = convertedMessage
    }

    variableMap["time"] = timeFormatter.format(Date())

    variableExpander.expand(variableMap).split('\n').forEach {
      sendChatMsg(it)
    }

    event.isCanceled = true
  }

  @NetworkCheckHandler
  fun netCheckHandler(mods: Map<String, String>, side: Side): Boolean {
    return true
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
