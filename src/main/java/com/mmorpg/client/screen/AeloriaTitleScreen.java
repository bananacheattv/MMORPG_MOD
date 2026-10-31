package com.mmorpg.client.screen;

import com.mmorpg.client.AeloriaFont;
import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.cinematic.Ease;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Menu principal d'Aeloria : remplace celui de Minecraft (voir {@link com.mmorpg.client.TitleClient}). Panorama assombri, braises
 * dorees qui montent, logo, accroche et trois boutons : connexion au serveur, options et quitter.
 */
public class AeloriaTitleScreen extends Screen {
    private long openedAt;

    private final Component reason;

    public AeloriaTitleScreen() {
        this(null);
    }

    /** Menu affiche apres une deconnexion : la raison s'affiche au-dessus des boutons. */
    public AeloriaTitleScreen(Component reason) {
        super(Component.translatable("narrator.screen.title"));
        this.reason = reason;
        if (this.minecraft != null) this.minecraft.gameRenderer.panorama().startSpin();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        openedAt = System.nanoTime();
        int bw=Math.min(220,width-48),x=Math.max(24,width/12),y=Math.max(118,height/2+12);
        Button online=addRenderableWidget(new com.mmorpg.client.ui.GlassButton(x,y,bw,28,"ENTRER DANS AELORIA",0xFFE2B85A,b->join(serverAddress())));
        online.active=minecraft.allowsMultiplayer();
        if(!online.active)online.setTooltip(Tooltip.create(Component.translatable("title.multiplayer.disabled")));
        addRenderableWidget(new com.mmorpg.client.ui.GlassButton(x,y+35,bw,24,"OPTIONS",0xFFB191ED,b->minecraft.gui.setScreen(new OptionsScreen(this,minecraft.options))));
        addRenderableWidget(new com.mmorpg.client.ui.GlassButton(width-79,12,67,20,"QUITTER",0xFFB191ED,b->minecraft.stop()));
    }

    public static String serverAddress(){String server=System.getProperty("aeloria.server","").trim();if(server.isEmpty())server=ClientConfig.MAIN_MENU_SERVER.get().trim();return server.isEmpty()?"192.168.0.249:25565":server;}

    /** Connexion directe au serveur configure (config client : mainMenuServer). */
    private void join(String address) {
        net.minecraft.client.multiplayer.resolver.ServerAddress parsed = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(this, this.minecraft, parsed,
                new net.minecraft.client.multiplayer.ServerData("Aeloria", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
    }

    private float seconds() {
        return (System.nanoTime() - openedAt) / 1.0E9F;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        float t = seconds();
        float fade = Ease.OUT_CUBIC.apply(t / 1.4F);
        int iw=1664,ih=941;
        float scale=Math.max(width/(float)iw,height/(float)ih);
        int dw=(int)Math.ceil(iw*scale),dh=(int)Math.ceil(ih*scale);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,com.mmorpg.MMORPG.id("textures/gui/brand/citadel_twilight.png"),(width-dw)/2,(height-dh)/2,0,0,dw,dh,iw,ih,iw,ih);
        g.fillGradient(0,0,width,height,0x150B041B,0x650B041B);
        int x=Math.max(24,width/12),bw=Math.min(220,width-48),y=Math.max(118,height/2+12);
        int lw=Math.min(260,(int)((y-40)*com.mmorpg.client.ui.Logo.FULL_RATIO));
        com.mmorpg.client.ui.Logo.full(g,x+bw/2,y-35-(int)(lw/com.mmorpg.client.ui.Logo.FULL_RATIO),lw);
        g.text(font,AeloriaFont.of("ÉCRIVEZ VOTRE LÉGENDE"),x+4,y-21,0xFFEED59E,true);
        if(reason!=null){
            var lines=font.split(reason,Math.max(80,Math.min(width-48,320)));
            int n=Math.min(lines.size(),5),bx=Math.max(24,width-Math.max(80,Math.min(width-48,320))-24),by=height-40-n*10;
            com.mmorpg.client.ui.Ui.rounded(g,bx-6,by-6,Math.max(80,Math.min(width-48,320))+12,16+n*10+6,6,0xB0180F2B);
            g.text(font,"DÉCONNECTÉ",bx,by,0xFFE2B85A,false);
            for(int i=0;i<n;i++)g.text(font,lines.get(i),bx,by+12+i*10,0xFFF1E6FF,false);
        }
        embers(g,t,fade);
        com.mmorpg.client.ui.Ui.rounded(g,12,height-24,145,16,6,0x90180F2B);
        g.text(font,"AELORIA  ·  MMORPG",20,height-19,0xFFD9C9EE,false);
        super.extractRenderState(g,mx,my,a);
    }

    /** Braises dorees qui montent lentement, deterministes (pas d'etat). */
    private void embers(GuiGraphicsExtractor g, float t, float fade) {
        for (int i = 0; i < 46; i++) {
            float seed = i * 12.9898F;
            float speed = 6F + (i % 7) * 3F;
            float y = height - ((t * speed + Mth.frac(seed * 0.37F) * height) % height);
            float x = Mth.frac(seed * 0.618F) * width + Mth.sin(t * 0.7F + i) * 12F;
            float flick = 0.35F + 0.65F * Mth.sin(t * 2.4F + i * 1.7F) * Mth.sin(t * 2.4F + i * 1.7F);
            int sz = 1 + (i % 4 == 0 ? 1 : 0);
            g.fill(Math.round(x), Math.round(y), Math.round(x) + sz, Math.round(y) + sz, argb(0.6F * fade * flick, i % 3 == 0 ? 0xFFE6A0 : 0xE2B85A));
        }
    }

    private static int argb(float alpha, int rgb) {
        return (Mth.clamp((int) (alpha * 255F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
