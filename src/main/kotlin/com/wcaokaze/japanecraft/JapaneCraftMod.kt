package com.wcaokaze.japanecraft

import com.kawakawaplanning.japanecraft.Kana
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.NetworkCheckHandler
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent

@Mod(modid = "japanecraft", version = "0.3.0")
class JapaneCraftMod {
  private var isServer = false

  @Mod.EventHandler
  fun init(event: FMLInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  fun onServerChat(event: ServerChatEvent) {
    if (isServer) return

    val k = Kana()
    k.line = event.message
    k.convert()

    val text = ChatComponentText("<${ event.username }> ${ event.message } (${ k.line })")
    FMLCommonHandler.instance().minecraftServerInstance.configurationManager.sendChatMsg(text)

    event.isCanceled = true
  }

  @NetworkCheckHandler
  fun netCheckHandler(mods: Map<String, String>, side: Side): Boolean {
    isServer = side.isServer
    return true
  }
}
