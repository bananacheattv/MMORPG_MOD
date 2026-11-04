package com.mmorpg.client.wiki;

import com.mmorpg.MMORPG;
import com.mmorpg.client.ui.Ui;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import java.util.*;

/** Diorama 3D isolé : vrais modèles/textures, profondeur commune et animation, sans toucher au monde. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class WikiScene {
    public record Piece(ItemStackRenderState item, double x, double y, double z, float size, float yaw, int glow) {}
    public record State(List<Piece> pieces, EntityRenderState actor, float actorX, float actorZ, float actorScale,
                        float yaw, float pitch, int x0, int y0, int x1, int y1, float scale) implements PictureInPictureRenderState {
        @Override public ScreenRectangle scissorArea() { return new ScreenRectangle(x0,y0,x1-x0,y1-y0); }
        @Override public ScreenRectangle bounds() { return scissorArea(); }
    }
    private final Map<String, ItemStackRenderState> models = new HashMap<>();
    private net.minecraft.client.multiplayer.ClientLevel world;
    private com.mmorpg.entity.NpcEntity actor;
    private ItemStackRenderState model(String id, boolean block) {
        String key = id + block;
        return models.computeIfAbsent(key, ignored -> {
            var item = com.mmorpg.crafting.ForgeRecipes.resolveItem(id);
            if (item == net.minecraft.world.item.Items.AIR) item = net.minecraft.world.item.Items.AMETHYST_SHARD;
            var state = new ItemStackRenderState(); var mc = Minecraft.getInstance();
            mc.getItemModelResolver().updateForTopItem(state, new ItemStack(item), block ? ItemDisplayContext.NONE : ItemDisplayContext.FIXED, mc.level, mc.player, 0);
            return state;
        });
    }
    /** Libere les modeles et l'acteur du diorama : tout est reconstruit a la demande a la prochaine page illustree. */
    public void release() { models.clear(); actor = null; world = null; }
    public void draw(GuiGraphicsExtractor g, WikiStories.Story story, int step, float progress, float yaw, float pitch, float zoom, int x, int y, int w, int h) {
        var mc = Minecraft.getInstance(); if (mc.level == null || mc.player == null) return;
        boolean mascot = story.theme().equals("mascot");
        if (world != mc.level || actor == null || mascot != (actor instanceof com.mmorpg.entity.MascotEntity)) {
            world = mc.level; models.clear();
            if (mascot) { var m = new com.mmorpg.entity.MascotEntity(com.mmorpg.registry.ModEntities.MASCOT.get(), world); m.setOwnerId(mc.player.getId()); actor = m; }
            else actor = new com.mmorpg.entity.NpcEntity(com.mmorpg.registry.ModEntities.NPC.get(), world);
            actor.setId(-160001); actor.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, "Guide", 4, "");
        }
        var pieces = new ArrayList<Piece>();
        String theme = story.theme();
        String floor = switch(theme) { case "mine" -> "minecraft:deepslate_tiles"; case "arena" -> "minecraft:polished_blackstone_bricks"; case "water" -> "minecraft:prismarine_bricks"; case "guild" -> "minecraft:moss_block"; default -> "minecraft:polished_deepslate"; };
        for (int bx=-3;bx<=3;bx++) for (int bz=-2;bz<=2;bz++) {
            pieces.add(new Piece(model((bx+bz)%2==0?floor:"minecraft:stone_bricks",true),bx,-.5,bz,1,0,0));
            if (Math.abs(bx)==3 && Math.abs(bz)==2) pieces.add(new Piece(model("minecraft:chiseled_stone_bricks",true),bx,.15,bz,.65f,0,0));
        }
        String prop = switch(theme) { case "forge" -> "mmorpg:forge_arcanique"; case "market" -> "minecraft:chest"; case "guild" -> "minecraft:oak_log"; case "mine" -> "minecraft:diamond_ore"; case "arena" -> "minecraft:amethyst_cluster"; case "water" -> "minecraft:sea_lantern"; case "mount" -> "minecraft:hay_block"; default -> "minecraft:lectern"; };
        pieces.add(new Piece(model(prop,true),.8,.5,.3,1.2f,0,0));
        if (theme.equals("portal")) {
            for(int a=-1;a<=1;a++) for(int b=0;b<=3;b++) if(Math.abs(a)==1 || b==3)
                pieces.add(new Piece(model("minecraft:crying_obsidian",true),a+1,b+.5,1,1,0,0));
            for(int b=0;b<3;b++) pieces.add(new Piece(model("minecraft:purple_stained_glass",true),1,b+.5,1,.95f,0,0));
        }
        if (theme.equals("mine")) for(int a=0;a<3;a++) pieces.add(new Piece(model("minecraft:deepslate",true),2,a+.5,1,1,0,0));
        if (theme.equals("guild")) { pieces.add(new Piece(model("minecraft:oak_log",true),.8,1.5,.3,1,0,0)); pieces.add(new Piece(model("minecraft:oak_leaves",true),.8,2.5,.3,1.7f,0,0)); }
        // Le matériau avance vers l'atelier, puis le résultat se soulève sur son socle.
        double px = step==0 ? -1.5 + progress * 1.2 : step==1 ? .5 : 1.6;
        double py = step==1 ? 1.5 + Math.abs(Math.sin(progress*Math.PI*4))*.35 : 1.25 + Math.sin(progress*Math.PI*2)*.12;
        pieces.add(new Piece(model(story.items()[step], false), px, py, -.5, .9f, progress * 45, 0));
        for (int k=0;k<4;k++) {
            double t=progress*Math.PI*2+k*Math.PI/2;
            pieces.add(new Piece(model("minecraft:amethyst_shard",false),px+Math.cos(t)*.6,py+.4+Math.sin(t)*.15,-.5+Math.sin(t)*.6,.1f,(float)Math.toDegrees(t),0));
        }
        actor.tickCount = (int)(progress*80); actor.yBodyRot=actor.yBodyRotO=155; actor.yHeadRot=actor.yHeadRotO=155; actor.setYRot(155);
        var actorState = mc.getEntityRenderDispatcher().getRenderer(actor).createRenderState(actor,1);
        actorState.shadowPieces.clear(); actorState.lightCoords=0xF000F0; actorState.nameTag=null;
        float ui=Ui.renderScale;
        g.submitPictureInPictureRenderState(new State(List.copyOf(pieces),actorState,-2f+(theme.equals("portal")?progress*(step+1)*.55f:0),-.8f,mascot?.5f:1f,
                yaw+35,pitch+25,Math.round(x*ui),Math.round(y*ui),Math.round((x+w)*ui),Math.round((y+h)*ui),Math.min(w/10f,h/5.2f)*zoom*ui));
    }
    @SubscribeEvent public static void register(RegisterPictureInPictureRenderersEvent e) { e.register(State.class, Renderer::new); }
    public static final class Renderer extends PictureInPictureRenderer<State> {
        @Override public Class<State> getRenderStateClass() { return State.class; }
        @Override protected String getTextureLabel() { return "aeloria_wiki"; }
        @Override protected float getTranslateY(int h,int scale) { return h*.65f; }
        @Override protected void renderToTexture(State s, PoseStack p, SubmitNodeCollector out) {
            var mc=Minecraft.getInstance(); mc.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
            p.rotateDegrees(Axis.ZP,180); p.rotateDegrees(Axis.XP,-s.pitch()); p.rotateDegrees(Axis.YP,s.yaw());
            for(var part:s.pieces()) { p.pushPose(); p.translate(part.x(),part.y(),part.z()); p.rotateDegrees(Axis.YP,part.yaw()); p.scale(part.size(),part.size(),part.size()); part.item().submit(p,out,0xF000F0,OverlayTexture.NO_OVERLAY,part.glow()); p.popPose(); }
            p.pushPose(); p.translate(s.actorX(),0,s.actorZ()); p.scale(s.actorScale(),s.actorScale(),s.actorScale());
            mc.getEntityRenderDispatcher().submit(s.actor(),new CameraRenderState(),0,0,0,p,out); p.popPose();
        }
    }
}
