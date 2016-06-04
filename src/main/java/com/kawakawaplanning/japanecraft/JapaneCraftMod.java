package com.kawakawaplanning.japanecraft;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;

import java.util.Map;

@Mod(modid = JapaneCraftMod.MODID, version = JapaneCraftMod.VERSION)
public class JapaneCraftMod {
    public static final String MODID = "JapaneCraft";
    public static final String VERSION = "0.1";

    boolean isServer = true;
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if(!isServer) {
            Kana k = new Kana();
            k.setLine(event.message);
            k.convert();

            ChatComponentText text = new ChatComponentText("<" + event.username + "> " + event.message + " (" + k.getLine() + ")");
            FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().sendChatMsg(text);

            event.setCanceled(true);
        }
    }

    @NetworkCheckHandler
    public boolean netCheckHandler(Map<String, String> mods, Side side) {
        isServer = side.isServer();
        return true;
    }
}
