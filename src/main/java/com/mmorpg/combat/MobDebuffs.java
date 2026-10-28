package com.mmorpg.combat;

import com.mmorpg.entity.*;
import com.mmorpg.rpg.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;

/** Short, visible debuffs; one application at most every six seconds, no indefinite crowd-control chain. */
public final class MobDebuffs {
    public static void onHit(LivingEntity source,ServerPlayer player){
        if(!(source instanceof RpgMob mob)||!(source.level() instanceof ServerLevel level))return;
        var data=RpgPlayers.get(player);long now=level.getGameTime();
        if(data.cooldowns.getOrDefault("mob_debuff",0L)>now)return;
        boolean boss=source instanceof com.mmorpg.entity.boss.RpgBoss;
        if(!boss&&source.getRandom().nextFloat()>.2f)return;
        data.cooldowns.put("mob_debuff",now+120);
        int type=Math.floorMod(mob.mobKey().hashCode(),3);
        BuffType buff=new BuffType[]{BuffType.ARMOR_FRACTURE,BuffType.ARCANE_EXHAUSTION,BuffType.MORTAL_WOUND}[type];
        RpgPlayers.addBuff(player,buff,boss?120:80,.25,0);
        level.addFreshEntity(SkillFxEntity.create(level,new FxKind[]{FxKind.SHIELD,FxKind.MARK,FxKind.AURA}[type],player.position(),1,buff.color&0xFFFFFF,60).follow(player));
    }
    public static double healingMultiplier(ServerPlayer player){return RpgPlayers.get(player).buffs.containsKey(BuffType.MORTAL_WOUND)?.6:1;}
}
