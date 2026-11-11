package com.mmorpg.competition;

import com.mmorpg.guild.Yggdrasil;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import java.util.UUID;
import java.util.List;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.mob.ImportedSpells;
import net.minecraft.world.phys.Vec3;

public final class GuildGuardian extends com.mmorpg.entity.boss.RpgBoss {
    public boolean testing;public final int tier;public UUID guild;private int windup;private Vec3 aim;
    private boolean attackQueued;private int casts;
    public GuildGuardian(EntityType<? extends Monster> type,Level level,int tier){super(type,level,"yggdrasil_"+Yggdrasil.ALL[tier].id());this.tier=tier;setCustomName(net.minecraft.network.chat.Component.literal(Yggdrasil.ALL[tier].name()));}
    @Override protected void registerGoals(){super.registerGoals();}
    @Override protected double baseDamage(){return 1.15;}
    @Override protected double baseToughness(){return 1.25;}
    public void initialize(){setHomeTo(blockPosition(),34);var md=new com.mmorpg.rpg.MobData();md.key=mobKey();md.level=Math.min(100,(tier+1)*10);md.boss=true;md.maxHp=14000*(tier+1)*(tier+1);md.hp=md.maxHp;md.atk=(float)(65*Math.pow(tier+1,1.55));md.def=10*(tier+1);md.xpMultiplier=0;setData(com.mmorpg.registry.ModAttachments.MOB,md);setHealth(getMaxHealth());}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(!(source.getEntity() instanceof ServerPlayer p)||!GuildRaids.allowed(this,p.getUUID()))return false;return super.hurtServer(level,source,amount);}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){if(source.getEntity() instanceof ServerPlayer p)GuildRaids.killed(this,p);super.die(source);}
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){if(attackQueued||busy())return false;attackQueued=true;playClip(ImportedSpells.ATTACK);com.mmorpg.server.Scheduler.later(level,ImportedSpells.CLIP_WINDUP[ImportedSpells.ATTACK],()->{attackQueued=false;if(isAlive()&&target instanceof ServerPlayer p&&p.isAlive()&&GuildRaids.allowed(this,p.getUUID())&&distanceTo(p)<getBbWidth()/2+p.getBbWidth()/2+3)super.doHurtTarget(level,target);});return true;}
    @Override protected int leashRadius(){return 34;}
    @Override protected boolean busy(){return super.busy()||windup>0||attackQueued;}
    private boolean danger(net.minecraft.world.phys.Vec3 point){double distance=Math.hypot(point.x-aim.x,point.z-aim.z);return switch((tier+phase-1+casts)%5){case 0->distance<7;case 1->distance>5&&distance<13;case 2->distance<17&&(Math.abs(point.x-aim.x)<3||Math.abs(point.z-aim.z)<3);case 3->distance<15&&Math.abs(Math.sin(Math.atan2(point.z-aim.z,point.x-aim.x)*3))<.55;default->distance<11;};}
    @Override protected boolean canHit(LivingEntity e){return e instanceof ServerPlayer p&&p.isAlive()&&GuildRaids.allowed(this,p.getUUID());}
    private int color(){return Yggdrasil.ALL[tier].color();}
    /** Joue un clip, immobilise le gardien le temps du geste, puis declenche l'effet a l'impact. */
    private void act(ServerLevel level,int clip,int windup,int recover,Runnable impact){playClip(clip);immobile(windup+recover);com.mmorpg.server.Scheduler.later(level,windup,()->{if(isAlive()&&!isRemoved())impact.run();});}
    @Override protected void bossTick(ServerLevel level){
        if(guild==null){discard();return;}if(getTarget()==null)return;
        if(windup>0){getNavigation().stop();windup--;if(windup%4==0){for(int x=-15;x<=15;x+=3)for(int z=-15;z<=15;z+=3){var point=new Vec3(aim.x+x,aim.y+.15,aim.z+z);if(danger(point))level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,point.x,point.y,point.z,1,0,0,0,0);}}
            if(windup==0){for(var p:level.players())if(canHit(p)&&danger(p.position()))com.mmorpg.combat.RpgCombat.mobHit(this,p,1.4+phase*.25+tier*.04);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,aim.x,aim.y+1,aim.z,20,5,1,5,.03);}}
    }
    /** Six attaques, une par clip : coup (melee, clip attack), double taille (attack2), ecrasement (special), marques lumineuses et salve (cast), ondes successives (ultime). */
    @Override protected void defineMoves(List<Move> out){
        move(out,"taille",ImportedSpells.ATTACK2,110,1,0,8,level->{int w=ImportedSpells.CLIP_WINDUP[ImportedSpells.ATTACK2]+9;telegraphCone(level,8,60,w,net.minecraft.core.particles.ParticleTypes.END_ROD);
            act(level,ImportedSpells.ATTACK2,w,14,()->{fx(level,FxKind.SLASH,position().add(0,1.5,0),8,color(),10);hitCone(facing(),8,60,1.2+phase*.1,e->knock(e,.9,.35));
                com.mmorpg.server.Scheduler.later(level,8,()->{if(!isAlive())return;fx(level,FxKind.SLASH,position().add(0,1.5,0),8,color(),10);hitCone(facing(),8,60,.9+phase*.1,null);});});});
        move(out,"ecrasement",ImportedSpells.SPECIAL,170,1,0,9,level->{int w=ImportedSpells.CLIP_WINDUP[ImportedSpells.SPECIAL];telegraph(level,position(),8,color(),w);
            act(level,ImportedSpells.SPECIAL,w,16,()->{fx(level,FxKind.SHOCKWAVE,position(),8,color(),14);fx(level,FxKind.CRACKS,position(),8,color(),60);level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,getX(),getY()+.5,getZ(),6,3,.2,3,0);hitCircle(position(),8,1.6+phase*.15,e->knock(e,1.1,.55));});});
        move(out,"marques",ImportedSpells.CAST,145-tier*5,1,0,40,level->{casts++;aim=(casts%2==0?aim():position());windup=phase>=3?30:phase==2?40:50;immobile(windup+6);telegraph(level,aim,7,color(),windup);playClip(ImportedSpells.CAST);
            warn(level,"⚠ "+Yggdrasil.ALL[tier].attack()+" — quittez les marques lumineuses !");});
        move(out,"salve",ImportedSpells.CAST,130,1,7,44,level->act(level,ImportedSpells.CAST,ImportedSpells.CLIP_WINDUP[ImportedSpells.CAST]+6,12,()->{for(var p:foes(level,44))for(int i=0;i<Math.min(3,phase);i++)com.mmorpg.entity.MobAbilities.bolt(this,p,com.mmorpg.item.ItemDefs.Element.ARCANE,.8+tier*.02,.9f+i*.2f,1.5);
            fx(level,FxKind.BURST,position().add(0,getBbHeight()*.6,0),2,color(),14);}));
        move(out,"ondes",ImportedSpells.ULTIME,260,2,0,40,level->{int w=ImportedSpells.CLIP_WINDUP[ImportedSpells.ULTIME];telegraph(level,position(),18,color(),w);warn(level,"⚠ Ondes de l'Arbre : sautez entre les vagues !");
            act(level,ImportedSpells.ULTIME,w,30,()->{Vec3 c=position();for(int i=0;i<3;i++){final int wave=i;com.mmorpg.server.Scheduler.later(level,i*10,()->{if(!isAlive())return;fx(level,FxKind.RING,c,(wave+1)*6,color(),12);
                for(int k=0;k<40;k++){double a=Math.PI*2*k/40;level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,c.x+Math.cos(a)*(wave*6+3),c.y+.3,c.z+Math.sin(a)*(wave*6+3),1,.5,.1,.5,.01);}
                hitRing(c,wave*6,(wave+1)*6,1.5+phase*.2+tier*.04,e->knock(e,.6,.4));});}});});
    }
}
