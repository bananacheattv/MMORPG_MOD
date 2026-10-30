package com.mmorpg.client.replay;

import com.mmorpg.MMORPG;
import com.mmorpg.client.Keys;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class ReplayControls {
    private static final KeyMapping MENU=new KeyMapping("key.mmorpg.replay",InputConstants.KEY_F8,Keys.CATEGORY);
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(MENU);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        while(MENU.consumeClick()){
            var mc=Minecraft.getInstance();
            if(mc.gui.screen() instanceof ReplayScreen)mc.gui.setScreen(null);
            else if(ReplayClient.authorized()&&mc.gui.screen()==null)ReplayClient.request("open");
        }
    }
    @SubscribeEvent public static void layers(RenderGuiLayerEvent.Pre e){
        if(ReplayClient.playing&&ReplayClient.clean)e.setCanceled(true);
    }
    @SubscribeEvent public static void overlay(RegisterGuiLayersEvent e){
        e.registerAboveAll(MMORPG.id("replay_status"),(g,delta)->{
            if(!ReplayClient.recording&&!ReplayClient.playing)return;
            var mc=Minecraft.getInstance();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
            if(ReplayClient.playing&&ReplayClient.framing!=0){
                int width=ReplayClient.framing==1?h*9/16:h;int margin=Math.max(0,(w-width)/2);
                g.fill(0,0,margin,h,0xFF000000);g.fill(w-margin,0,w,h,0xFF000000);
            }
            if(!ReplayClient.clean){
                g.fill(6,h-22,Math.min(w-6,320),h-4,0xDD1C132C);
                g.text(mc.font,ReplayClient.recording?"● REC — F8 : arrêter / atelier":"REPLAY — F8 : contrôles / quitter",12,h-17,ReplayClient.recording?0xFFFF7070:0xFFE3C66A,false);
            }
        });
    }
}
