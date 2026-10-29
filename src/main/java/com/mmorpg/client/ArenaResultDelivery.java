package com.mmorpg.client;

import com.mmorpg.client.screen.ArenaResultsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Wait until the destination world is visible, so loading screens cannot swallow the report. */
public final class ArenaResultDelivery {
    private static CompoundTag pending;
    private static int readyTicks;
    public static void receive(CompoundTag report){pending=report.copy();readyTicks=0;}
    public static void tick(Minecraft mc){
        if(pending==null)return;
        if(mc.player==null||mc.level==null||mc.gui.screen()!=null||mc.player.tickCount<40){readyTicks=0;return;}
        if(++readyTicks<20)return;
        var report=pending;pending=null;mc.gui.setScreen(new ArenaResultsScreen(report));
    }
}
