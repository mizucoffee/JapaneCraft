package com.wcaokaze.japanecraft

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.NetworkCheckHandler
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.event.ServerChatEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Mod(modid = "japanecraft", version = "0.3.2")
class JapaneCraftMod {
  private val timeFormatter = SimpleDateFormat("HH:mm:ss")

  private lateinit var variableExpander: VariableExpander

  @Mod.EventHandler
  fun preInit(event: FMLPreInitializationEvent) {
    val config = Configuration(File("config/JapaneCraft.cfg"))

    config.load()

    val chatMsgFormat = config.getString("chat", "format",
        "<\$username> \$rawMessage\$n  §b\$convertedMessage",
        "The format for chat messages")

    variableExpander = VariableExpander(chatMsgFormat)

    config.save()
  }

  @Mod.EventHandler
  fun init(event: FMLInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onServerChat(event: ServerChatEvent) {
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
