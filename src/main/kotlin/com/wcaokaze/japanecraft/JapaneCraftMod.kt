package com.wcaokaze.japanecraft

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent

@Mod(modid = "japanecraft", version = "0.3.0")
class JapaneCraftMod {
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

    val enMsg = event.message
    val jpMsg = RomajiConverter.convert(enMsg)

    sendChatMsg("<${ event.username }> $enMsg")
    sendChatMsg("  §b$jpMsg")

    event.isCanceled = true
  }
}
