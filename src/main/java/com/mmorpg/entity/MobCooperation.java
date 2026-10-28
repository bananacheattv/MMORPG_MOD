package com.mmorpg.entity;
import com.mmorpg.MMORPG;
import com.mmorpg.combat.RpgCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Server-owned squads: healers sustain allies, scouts rally existing packs and engineers raise temporary obstacles. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class MobCooperation {
    public enum Role {HEALER,SCOUT,ENGINEER,SOLDIER}
    public static Role role(String key){String k=key.toLowerCase(Locale.ROOT);if(k.equals("veilleur_champignon")||k.equals("sorcier_roseaux")||k.equals("spectre_enchaine")||k.contains("chaman")||k.contains("soigneur")||k.contains("pretresse")||k.contains("druide"))return Role.HEALER;if(k.equals("archer_sylvestre")||k.equals("rodeur_vide")||k.contains("eclaireur")||k.contains("guetteur")||k.contains("sentinelle")||k.contains("bandit"))return Role.SCOUT;if(k.contains("gobelin"))return Role.ENGINEER;return Role.SOLDIER;}
    public static void tick(Mob mob,ServerLevel level){
        if(!(mob instanceof RpgMob rpg)||mob instanceof com.mmorpg.entity.boss.RpgBoss||mob.getTarget()==null||!RpgCombat.isEnemy(mob,mob.getTarget())||mob.tickCount%20!=0)return;
        long now=level.getGameTime();var persistent=mob.getPersistentData();if(persistent.getLongOr("squad_next",0)>now)return;
        var target=mob.getTarget();var allies=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(14),a->a!=mob&&a instanceof RpgMob&&!(a instanceof com.mmorpg.entity.boss.RpgBoss)&&a.isAlive()&&mob.hasLineOfSight(a)&&!RpgCombat.isEnemy(mob,a));
        switch(role(rpg.mobKey())){
            case HEALER->{var ally=allies.stream().filter(a->{var d=a.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);return d!=null&&d.hp<d.maxHp*.8;}).min(Comparator.comparingDouble(a->{var d=a.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);return d.hp/d.maxHp;})).orElse(null);
                if(ally==null)return;var d=ally.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);d.hp=Math.min(d.maxHp,d.hp+d.maxHp*.12f);ally.setData(com.mmorpg.registry.ModAttachments.MOB,d);ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,60,0));
                level.addFreshEntity(SkillFxEntity.create(level,FxKind.BEAM,mob.position(),.25,0x70FF90,18).points(List.of(mob.getEyePosition(),ally.getEyePosition())));persistent.putLong("squad_next",now+160);
            }
            case SCOUT->{int called=0;for(var ally:allies)if(called<4&&ally.getTarget()==null&&ally.hasLineOfSight(target)){ally.setTarget(target);ally.getNavigation().moveTo(target,1.2);called++;}if(called==0)return;level.addFreshEntity(SkillFxEntity.create(level,FxKind.SHOCKWAVE,mob.position(),4,0xFFD060,24));persistent.putLong("squad_next",now+240);}
            case ENGINEER->{
                // Barricades are effects, not permanent world edits; only generated dungeon floors can receive one.
                if(com.mmorpg.dungeon.InstanceManager.slotAt(level.getServer(),level,mob.getX(),mob.getZ())==null)return;
                var delta=target.position().subtract(mob.position()).normalize();var base=BlockPos.containing(mob.position().add(delta.scale(2)));
                if(mob.distanceToSqr(target)<16)return;var positions=new ArrayList<BlockPos>();int placed=0;for(int i=-1;i<=1;i++){var at=base.offset(Math.abs(delta.x)>Math.abs(delta.z)?0:i,0,Math.abs(delta.x)>Math.abs(delta.z)?i:0);if(i==1)continue;
                    if(level.isEmptyBlock(at)&&level.getBlockState(at.below()).isSolidRender()&&level.getBlockEntity(at)==null){positions.add(at.immutable());placed++;}}
                if(placed>0){WorldBarricades.place(level,positions);persistent.putLong("squad_next",now+300);}
            }
            default->{}
        }
    }
}
