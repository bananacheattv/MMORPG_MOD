package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.*;
import com.mmorpg.item.ItemDefs;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Shared anti-kiting attacks: telegraphed casts, dodgeable bolts, then split salvos and marked eruptions in later phases. */
public final class BossPressure {
    public static final class State {long next;int windup,phase;UUID target;Vec3 mark;}
    private static ItemDefs.Element element(RpgBoss boss){String key=boss.mobKey();return key.contains("ignis")?ItemDefs.Element.FEU:key.contains("glace")?ItemDefs.Element.GIVRE:key.contains("neant")?ItemDefs.Element.NEANT:ItemDefs.Element.ARCANE;}
    private static void fx(ServerLevel level,FxKind kind,Vec3 pos,double radius,int color,int life){level.addFreshEntity(SkillFxEntity.create(level,kind,pos,radius,color,life));}
    public static boolean tick(RpgBoss boss,ServerLevel level,int phase,State state){
        if(state.windup>0){
            boss.getNavigation().stop();
            if(--state.windup==0){
                var entity=level.getEntity(state.target);
                if(entity instanceof LivingEntity target&&RpgCombat.isEnemy(boss,target)&&boss.distanceToSqr(target)<48*48){
                    ItemDefs.Element element=element(boss);
                    int bolts=state.phase==1?1:state.phase==2?3:state.phase==3?5:7;
                    Vec3 origin=boss.position().add(0,Math.min(5,boss.getBbHeight()*.65),0);
                    Vec3 direction=state.mark.add(0,1,0).subtract(origin).normalize();
                    for(int i=0;i<bolts;i++){
                        var bolt=MagicProjectile.create(level,boss,element,RpgCombat.mobAtk(boss)*(.8+.1*state.phase),0);
                        bolt.setPos(origin.x,origin.y,origin.z);Vec3 ray=direction.yRot((float)((i-(bolts-1)/2.0)*.16));
                        bolt.shoot(ray.x,ray.y,ray.z,1.05f,0);level.addFreshEntity(bolt);
                    }
                    fx(level,FxKind.BURST,origin,1.5,0xB080FF,14);
                    if(state.phase>=3){
                        fx(level,FxKind.NOVA,state.mark,4,0xEF6090,20);
                        for(var victim:level.players())if(RpgCombat.isEnemy(boss,victim)&&victim.distanceToSqr(state.mark)<16)RpgCombat.mobHit(boss,victim,1.4);
                    }
                }
            }
            return true;
        }
        if(state.next==0){state.next=level.getGameTime()+50;return false;}
        if(boss.busy()||boss.getTarget()==null||level.getGameTime()<state.next)return false;
        LivingEntity target=boss.getTarget();
        if(!RpgCombat.isEnemy(boss,target)||boss.distanceToSqr(target)>48*48||!boss.hasLineOfSight(target))return false;
        if(phase<3&&boss.distanceToSqr(target)<36)return false;
        state.phase=phase;state.target=target.getUUID();state.mark=target.position();state.windup=phase>=3?24:32;
        state.next=level.getGameTime()+(phase>=3?60:phase==2?90:120);
        boss.pressureAnimation();
        fx(level,FxKind.CIRCLE,boss.position(),Math.max(2,boss.getBbWidth()/2),0xB080FF,state.windup);
        if(phase>=3)fx(level,FxKind.CIRCLE,state.mark,4,0xEF5070,state.windup);
        if(target instanceof ServerPlayer player)player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(phase>=3?"⚠ Éruption : quittez le cercle !":"⚠ Salve : préparez-vous à esquiver !").withColor(0xFF8050));
        return true;
    }
}
