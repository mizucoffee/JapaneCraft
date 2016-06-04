package com.kawakawaplanning.japanecraft;

import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

@Mod(name = JapaneCraftMod.MODID,modid = JapaneCraftMod.MODID, version = JapaneCraftMod.VERSION)
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