package com.mmorpg.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Display;
import com.mmorpg.client.ui.Ui;

/** A private, genuinely three-dimensional horizontal arrow. The client display never exists on the server. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class GuidanceArrow {
    private static CompoundTag target=new CompoundTag();private static long received,lastFrame;
    private static Display.ItemDisplay arrow;private static boolean firstPerson;private static int nextId=-1900000000;private static float heading;
    public static void receive(CompoundTag data){target=data.copy();received=System.nanoTime();}
    private static void remove(){if(arrow!=null){if(arrow.level() instanceof net.minecraft.client.multiplayer.ClientLevel world)world.removeEntity(arrow.getId(),net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);arrow=null;}lastFrame=0;}
    public static void clear(){remove();target=new CompoundTag();received=0;}
    private static boolean visible(Minecraft mc){return mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&mc.level!=null&&!mc.gui.hud.isHidden()&&mc.gui.screen()==null&&target.getBooleanOr("active",false)&&System.nanoTime()-received<=10_000_000_000L&&!com.mmorpg.client.LoadingClient.active()&&!com.mmorpg.client.cinematic.CinematicClient.active();}
    private static boolean located(Minecraft mc){return target.getBooleanOr("located",false)&&target.getStringOr("dimension","").equals(mc.level.dimension().identifier().toString())&&(target.getStringOr("backend","").isEmpty()||com.mmorpg.client.ClientData.backend.isEmpty()||target.getStringOr("backend","").equals(com.mmorpg.client.ClientData.backend));}
    @net.neoforged.bus.api.SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event){update(Minecraft.getInstance());}
    public static void update(Minecraft mc){
        if(!visible(mc)||!located(mc)){remove();return;}
        boolean first=mc.options.getCameraType().isFirstPerson();
        if(arrow!=null&&(arrow.level()!=mc.level||arrow.isRemoved()||first!=firstPerson))remove();
        var dest=new Vec3(target.getDoubleOr("x",0),target.getDoubleOr("y",0),target.getDoubleOr("z",0));var direction=dest.subtract(mc.player.position());
        // The JSON nose points north; ItemDisplayRenderer adds its half-turn after minus entity yaw.
        float desired=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        if(arrow==null){
            firstPerson=first;arrow=net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY.create(mc.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            var data=com.mmorpg.network.GuidanceDisplayData.create(first?.18f:.45f);
            arrow.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,mc.level.registryAccess(),data));arrow.setId(nextId--);
            var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD);stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,com.mmorpg.MMORPG.id("guidance_arrow"));arrow.getSlot(0).set(stack);arrow.setNoGravity(true);arrow.setSilent(true);heading=desired;mc.level.addEntity(arrow);
        }
        long now=System.nanoTime();float dt=lastFrame==0?1:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;
        if(direction.horizontalDistanceSqr()>.01)heading+=net.minecraft.util.Mth.wrapDegrees(desired-heading)*(1-(float)Math.exp(-dt*12));
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);Vec3 position;
        if(first){var camera=mc.gameRenderer.mainCamera();var f=camera.forwardVector();position=camera.position().add(f.x()*1.15,f.y()*1.15-.3,f.z()*1.15);position=new Vec3(position.x,Math.max(position.y,mc.player.getY()+.25),position.z);}
        else position=mc.player.getPosition(partial).add(0,mc.player.getBbHeight()+.6,0);
        arrow.snapTo(position.x,position.y,position.z,heading,0);
    }
    public static void render(GuiGraphicsExtractor g,Minecraft mc){
        if(!visible(mc))return;int color=0xFFC696FF;float x=g.guiWidth()/2f,y=g.guiHeight()-65;double distance=0;
        if(located(mc)){distance=new Vec3(target.getDoubleOr("x",0),target.getDoubleOr("y",0),target.getDoubleOr("z",0)).distanceTo(mc.player.position());
            if(arrow!=null){var projected=mc.gameRenderer.projectPointToScreen(arrow.position());if(Double.isFinite(projected.x)&&Double.isFinite(projected.y)&&Math.abs(projected.x)<1&&Math.abs(projected.y)<1){x=(float)((projected.x+1)*.5*g.guiWidth());y=(float)((1-projected.y)*.5*g.guiHeight())+9;}}
        }
        x=Math.clamp(x,65,g.guiWidth()-65);y=Math.clamp(y,24,g.guiHeight()-45);
        String title=mc.font.plainSubstrByWidth(target.getStringOr("title",""),150),label=mc.font.plainSubstrByWidth(target.getStringOr("label",""),170);String range=located(mc)?" · "+Math.round(distance)+" m":"";
        Ui.scaledText(g,title,x,y+5,.7f,color,true);Ui.scaledText(g,label+range,x,y+13,.7f,Ui.TEXT,true);
    }
}
