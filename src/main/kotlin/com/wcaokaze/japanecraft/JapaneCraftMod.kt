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

@Mod(modid = "japanecraft", version = "0.3.1")
class JapaneCraftMod {
  private val timeFormatter = SimpleDateFormat("HH:mm:ss")

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

    var rawMessage = event.message
    var convertedMessage = rawMessage.toJapanese()

    if (rawMessage.any { it >= 0x80.toChar() } ||
        rawMessage == convertedMessage)
    {
      convertedMessage = rawMessage
      rawMessage = ""
    }

    val timeStr = timeFormatter.format(Date())

    sendChatMsg("<${ event.username }> [$timeStr] $rawMessage")
    sendChatMsg("  §b$convertedMessage")

    event.isCanceled = true
  }

  @NetworkCheckHandler
  fun netCheckHandler(mods: Map<String, String>, side: Side): Boolean {
    return true
  }

  private fun String.toJapanese(): String {
    return this
        .split('`')
        .mapIndexed { i, s ->
          if (i % 2 != 0) return@mapIndexed listOf(s)

          return@mapIndexed s
              .split(' ')
              .map {
                when {
                  // never satisfied because of the specification of minecraft.
                  it.isEmpty() -> " " + it
                  it.first().isUpperCase() -> " " + it
                  else -> " " + RomajiConverter.convert(it)
                }
              }
        }
        .flatten()
        .fold(StringBuffer()) { b, s -> b.append(s) }
        .toString()
        .drop(1)
  }
}
