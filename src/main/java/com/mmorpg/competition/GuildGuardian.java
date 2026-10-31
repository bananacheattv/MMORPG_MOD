package com.mmorpg.competition;

import com.mmorpg.guild.Yggdrasil;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class GuildGuardian extends com.mmorpg.entity.boss.RpgBoss {
    public boolean testing;public final int tier;public UUID guild;private int windup;private net.minecraft.world.phys.Vec3 aim;
    private boolean attackQueued;private int casts;
    public GuildGuardian(EntityType<? extends Monster> type,Level level,int tier){super(type,level,"yggdrasil_"+Yggdrasil.ALL[tier].id());this.tier=tier;setCustomName(net.minecraft.network.chat.Component.literal(Yggdrasil.ALL[tier].name()));}
    @Override protected void registerGoals(){super.registerGoals();}
    @Override protected double baseDamage(){return 1.15;}
    @Override protected double baseToughness(){return 1.25;}
    public void initialize(){setHomeTo(blockPosition(),34);var md=new com.mmorpg.rpg.MobData();md.key=mobKey();md.level=Math.min(100,(tier+1)*10);md.boss=true;md.maxHp=14000*(tier+1)*(tier+1);md.hp=md.maxHp;md.atk=(float)(65*Math.pow(tier+1,1.55));md.def=10*(tier+1);md.xpMultiplier=0;setData(com.mmorpg.registry.ModAttachments.MOB,md);setHealth(getMaxHealth());}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(!(source.getEntity() instanceof ServerPlayer p)||!GuildRaids.allowed(this,p.getUUID()))return false;return super.hurtServer(level,source,amount);}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(source.getEntity() instanceof ServerPlayer p)GuildRaids.killed(this,p);super.die(source);}
    @Override public void handleEntityEvent(byte event){if(event==73)attackStarted=tickCount;else if(event==74)castStarted=tickCount;else super.handleEntityEvent(event);}
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){if(attackQueued||windup>0)return false;attackQueued=true;level.broadcastEntityEvent(this,(byte)73);com.mmorpg.server.Scheduler.later(level,10,()->{attackQueued=false;if(isAlive()&&target instanceof ServerPlayer p&&p.isAlive()&&GuildRaids.allowed(this,p.getUUID())&&distanceTo(p)<getBbWidth()/2+p.getBbWidth()/2+3)super.doHurtTarget(level,target);});return true;}
    @Override protected int leashRadius(){return 34;}
    @Override protected boolean busy(){return super.busy()||windup>0||attackQueued;}
    private boolean danger(net.minecraft.world.phys.Vec3 point){double distance=Math.hypot(point.x-aim.x,point.z-aim.z);return switch((tier+phase-1+casts)%5){case 0->distance<7;case 1->distance>5&&distance<13;case 2->distance<17&&(Math.abs(point.x-aim.x)<3||Math.abs(point.z-aim.z)<3);case 3->distance<15&&Math.abs(Math.sin(Math.atan2(point.z-aim.z,point.x-aim.x)*3))<.55;default->distance<11;};}
    @Override protected void bossTick(ServerLevel level){
        if(guild==null){discard();return;}if(getTarget()==null||attackQueued)return;
        if(windup>0){getNavigation().stop();windup--;if(windup%4==0){for(int x=-15;x<=15;x+=3)for(int z=-15;z<=15;z+=3){var point=new net.minecraft.world.phys.Vec3(aim.x+x,aim.y+.15,aim.z+z);if(danger(point))level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,point.x,point.y,point.z,1,0,0,0,0);}}
            if(windup==0){for(var p:level.players())if(GuildRaids.allowed(this,p.getUUID())&&danger(p.position()))com.mmorpg.combat.RpgCombat.mobHit(this,p,1.4+phase*.25+tier*.04);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,aim.x,aim.y+1,aim.z,20,5,1,5,.03);}return;}
        if(ready("yggdrasil_special",145-tier*5)){casts++;aim=(casts%2==0?getTarget().position():position());windup=phase>=3?30:phase==2?40:50;level.addFreshEntity(com.mmorpg.entity.SkillFxEntity.create(level,com.mmorpg.entity.FxKind.CIRCLE,aim,7,Yggdrasil.ALL[tier].color(),windup));level.broadcastEntityEvent(this,(byte)74);for(var p:level.players())if(GuildRaids.allowed(this,p.getUUID()))p.sendSystemMessage(net.minecraft.network.chat.Component.literal("⚠ "+Yggdrasil.ALL[tier].attack()+" — quittez les marques lumineuses !").withColor(Yggdrasil.ALL[tier].color()));}
    }
}
