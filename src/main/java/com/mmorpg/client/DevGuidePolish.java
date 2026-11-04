package com.mmorpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import com.mmorpg.client.screen.*;

/** Captures privÃ©es opt-in, aucune activation dans le launcher. */
public final class DevGuidePolish {
    private static int tick;
    private static void shot(Minecraft mc,String name) {
        Screenshot.grab(mc.gameDirectory, "showcase_guide_"+name+".png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
    }
    public static void tick(Minecraft mc) {
        if(mc.player==null||mc.level==null||LoadingClient.active())return;
        int t=++tick; mc.gui.toastManager().clear();
        if(t<80)com.mmorpg.client.cinematic.CinematicClient.skip();
        if(t==80){mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();}
        if(t==100)mc.gui.setScreen(new WikiScreen());
        if(t==150)shot(mc,"wiki");
        if(t==260)shot(mc,"wiki_etape2");
        if(t==270)mc.gui.setScreen(new WikiScreen("forge"));
        if(t==278)shot(mc,"wiki_forge");
        if(t==280){mc.gui.setScreen(null);MascotDialogClient.say("Mascotte","Bienvenue sur Aeloria ! Deviens un hÃ©ros de niveau 100. Je vais t'accompagner vers tes premiÃ¨res quÃªtes et t'apprendre Ã  fabriquer ton Ã©quipement.");}
        if(t==300)shot(mc,"dialogue");
        if(t==305){
            MascotDialogClient.say("PNJ quêtes", "Portrait du PNJ agrandi : la tête doit rester dans le cadre.");
            try { var npc=new com.mmorpg.entity.NpcEntity(com.mmorpg.registry.ModEntities.NPC.get(),mc.level);npc.setId(-160010);npc.setup(com.mmorpg.entity.NpcEntity.Role.QUETES,"PNJ quêtes",4,"");npc.setNpcScale(3);var f=MascotDialogClient.class.getDeclaredField("portrait");f.setAccessible(true);f.set(null,npc); }catch(Exception ex){throw new IllegalStateException(ex);}
        }
        if(t==315)shot(mc,"portrait_pnj_x3");
        if(t==320)mc.gui.setScreen(new HudEditorScreen(null));
        if(t==345)shot(mc,"editeur_u");
        if(t==350){com.mmorpg.client.hud.HudLayout.DIALOGUE.moveTo(10,120,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());com.mmorpg.client.hud.HudLayout.DIALOGUE.setScale(1.25f,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());}
        if(t==355)shot(mc,"dialogue_deplace");
        if(t==360){mc.gui.setScreen(null);mc.gui.hud.toggle();}
        if(t==365)shot(mc,"f1");
        if(t==375){mc.gui.hud.toggle();mc.stop();}
    }
}
