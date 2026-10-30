package com.mmorpg.dungeon;
import com.mmorpg.registry.*;
import com.mmorpg.store.*;
import com.mmorpg.server.*;
import com.mmorpg.network.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.EntitySpawnReason;
import java.util.*;

/** Real server regression suite. Runs only inside the isolated network-check world. */
public final class DevExpansionChecks {
 private static void check(boolean value,String message){if(!value)throw new AssertionError("expansion: "+message);}
 private static void rejects(Runnable action,String message){try{action.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
 public static void run(ServerPlayer source)throws Exception{
  if(!Boolean.getBoolean("mmorpg.checkNetwork"))throw new IllegalStateException("Development checks only");
  var server=source.level().getServer();var original=NetworkConfig.current;NetworkConfig.current=new NetworkConfig();
  var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(source.level(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ExpansionChecks"));var d=RpgPlayers.get(p);d.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;RpgPlayers.recompute(p);
  try{
   var cfg=StoreConfig.load(server);StoreConfig.validate(cfg);check(cfg.ranks.size()==5&&cfg.ranks.getFirst().id.equals("aelorien"),"five public grades with free Aelorien");
   check(cfg.products.stream().noneMatch(x->x.id.contains("fondateur")||x.id.contains("administrateur")||x.id.contains("moderateur")||x.id.contains("builder")),"staff cannot be purchased");
   check(!cfg.paymentBridgeEnabled&&cfg.websiteUrl.isEmpty(),"money payments disabled before site launch");
   var id=p.getUUID();String external="test:"+UUID.randomUUID();StoreLedger.credit(server,id,10000,external);StoreLedger.credit(server,id,10000,external);
   check(StoreLedger.access(server,s->StoreLedger.account(s,id).credits)==10000,"external payment idempotent");rejects(()->StoreLedger.credit(server,id,10001,external),"conflicting payment must fail");
   rejects(()->StoreLedger.credit(server,id,-10,"negative"),"negative credit must fail");
   var buy=UUID.randomUUID();StoreLedger.buy(server,id,"lucky_celeste",buy);StoreLedger.buy(server,id,"lucky_celeste",buy);
   check(StoreLedger.access(server,s->StoreLedger.account(s,id).credits)==9900&&StoreLedger.access(server,s->StoreLedger.account(s,id).pending.size())==1,"purchase retry cannot debit or deliver twice");
   for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));PremiumStore.deliver(p);
   check(StoreLedger.access(server,s->StoreLedger.account(s,id).pending.size())==1,"paid delivery retained for full inventory");
   p.getInventory().setItem(0,ItemStack.EMPTY);PremiumStore.deliver(p);check(p.getInventory().countItem(ModItems.LUCKY_BLOCKS.getFirst().get())==1,"paid item delivered into free slot");PremiumStore.deliver(p);check(p.getInventory().countItem(ModItems.LUCKY_BLOCKS.getFirst().get())==1,"delivery retry cannot duplicate");
   StoreLedger.buy(server,id,"grade_eclaireur",UUID.randomUUID());StoreLedger.buy(server,id,"grade_gardien",UUID.randomUUID());
   check(StoreLedger.access(server,s->StoreLedger.account(s,id).credits)==8700,"grade upgrades charge price difference");
   rejects(()->StoreLedger.buy(server,id,"grade_eclaireur",UUID.randomUUID()),"owned/lower grade refused");
   rejects(()->StoreLedger.buy(server,id,"lucky_neant",UUID.randomUUID(),1),"stale or forged display price refused");
   PremiumStore.refresh(p);check(d.rankTier==2&&d.auctionSlots==20&&d.rankLabel.equals("Gardien")&&d.cosmetics.contains("halo_dore"),"rank prefix, auction benefit and cosmetic apply");
   var saved=d.save();var copy=new com.mmorpg.rpg.PlayerData();copy.load(saved);check(copy.rankLabel.equals(d.rankLabel)&&copy.guildReceipts.equals(d.guildReceipts),"grade and delivery receipts survive profile roundtrip");
   String secret="aeloria-test-secret-000000000000000000000";byte[] body="{payment:one}".getBytes(java.nio.charset.StandardCharsets.UTF_8);var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));String signature=HexFormat.of().formatHex(mac.doFinal(body));
   check(CreditBridge.validSignature(body,signature,secret)&&!CreditBridge.validSignature("changed".getBytes(),signature,secret)&&!CreditBridge.validSignature(body,signature,"short"),"signed bridge authenticates exact bytes and rejects tampering");
   for(var tier:com.mmorpg.block.LuckyTier.values())check(!com.mmorpg.config.ConfigManager.loot().table(tier.id).isEmpty(),"configured lucky table "+tier.id);
   p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(ModItems.LUCKY_BLOCK.get(),3));LuckyMigration.inventory(p.getInventory());check(p.getInventory().countItem(ModItems.LUCKY_BLOCKS.getFirst().get())==3&&p.getInventory().countItem(ModItems.LUCKY_BLOCK.get())==0,"legacy lucky blocks migrate without loss");
   var origin=p.blockPosition().offset(2,0,0);p.setPos(origin.getX(),origin.getY(),origin.getZ());
   for(var tier:com.mmorpg.block.LuckyTier.values()){p.level().setBlock(origin,ModBlocks.LUCKY_BLOCKS.get(tier.ordinal()).get().defaultBlockState(),2);check(LuckyBlockManager.roll(p,origin),"tier opens "+tier.id);check(!LuckyBlockManager.roll(p,origin),"tier cannot roll twice "+tier.id);}
   com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] CREDIT_STORE PASS: signed bridge, idempotent payment/debit/delivery, full inventory, grade upgrades, staff isolation, persistence and five lucky tiers");
   // Same advertised seed for a party, a fresh seed after release, and no global reroll of a resumed expedition.
   String pool=InstanceManager.pool(com.mmorpg.world.DungeonBuilder.DungeonType.CRYPTE_GOBELINE,0),owner=UUID.randomUUID().toString();long first=DungeonVariants.preview(server,pool,owner);
   check(first==DungeonVariants.preview(server,pool,owner),"preview stable across requests");DungeonVariants.finish(server,pool,owner);check(first!=DungeonVariants.preview(server,pool,owner),"new expedition rerolls");
   var level=server.getLevel(InstanceManager.DIM);var slot=new InstanceData.Slot(pool,9999,8000,101,10000,owner,server.getTickCount(),true);slot.variantSeed=first;InstanceData.get(server).slots.add(slot);var o=new BlockPos(slot.x,slot.y,slot.z);
   try{
    for(int x=-35;x<=35;x++)for(int z=120;z<=165;z++)level.setBlock(o.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
    DungeonVariants.decorate(level,o,first);for(int z=Layout.C2.z0()+4;z<=Layout.C2.z1()-4;z++){int passage=0;for(int x=-3;x<=3;x++)if(level.isEmptyBlock(o.offset(x,0,z)))passage++;check(passage>=2,"variant passage cannot seal progression");}
    var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RuneChecks"));RpgPlayers.get(player).playerClass=com.mmorpg.rpg.PlayerClass.MAGE;
    for(int i=0;i<3;i++)for(int turn=0;turn<3;turn++){var at=DungeonSecrets.rune(o,i);player.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.UP,at,false);DungeonSecrets.interact(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,at,hit));}
    check(level.isEmptyBlock(o.offset(17,0,Layout.R2.z()))&&RpgPlayers.get(player).cosmetics.contains("aura_arcane"),"real rune clicks open secret and unlock cosmetic");
    check(!level.isEmptyBlock(o.offset(27,0,Layout.R2.z())),"multi-class seal remains locked for solo player");
    var healer=ModEntities.IMPORTED.get("veilleur_champignon").get().create(level,EntitySpawnReason.COMMAND);var colossus=ModEntities.IMPORTED.get("colosse_racine").get().create(level,EntitySpawnReason.COMMAND);var scout=ModEntities.IMPORTED.get("archer_sylvestre").get().create(level,EntitySpawnReason.COMMAND);
    try{
        healer.snapTo(o.getX(),o.getY(),o.getZ()+122,0,0);colossus.snapTo(o.getX()+3,o.getY(),o.getZ()+122,0,0);scout.snapTo(o.getX(),o.getY(),o.getZ()+120,0,0);healer.setNoAi(true);colossus.setNoAi(true);scout.setNoAi(true);level.addFreshEntity(healer);level.addFreshEntity(colossus);level.addFreshEntity(scout);
        player.setPos(o.getX(),o.getY(),o.getZ()+130);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPermanentlyInvulnerable(false);healer.setTarget(player);scout.setTarget(player);healer.tickCount=20;scout.tickCount=20;
        var hp=colossus.getData(ModAttachments.MOB);hp.hp=hp.maxHp*.2f;colossus.setData(ModAttachments.MOB,hp);float before=hp.hp;
        com.mmorpg.entity.MobCooperation.tick(healer,level);check(colossus.getData(ModAttachments.MOB).hp>before,"healer actually restores wounded colossus");float healed=colossus.getData(ModAttachments.MOB).hp;com.mmorpg.entity.MobCooperation.tick(healer,level);check(colossus.getData(ModAttachments.MOB).hp==healed,"healer cooldown prevents spam");
        check(colossus.getTarget()==null,"colossus begins without target");com.mmorpg.entity.MobCooperation.tick(scout,level);check(colossus.getTarget()==player,"scout rallies existing ally onto its target");
    }finally{healer.discard();colossus.discard();scout.discard();}
    var barrier=o.offset(2,0,150);level.setBlock(barrier,Blocks.AIR.defaultBlockState(),2);com.mmorpg.entity.WorldBarricades.place(level,java.util.List.of(barrier));check(level.getBlockState(barrier).is(Blocks.OAK_FENCE),"engineer obstacle exists physically");
    com.mmorpg.network.SharedLedger.access(server,"mob-barricades",com.mmorpg.entity.WorldBarricades.State.class,com.mmorpg.entity.WorldBarricades.State::new,state->{state.cells.forEach(c->c.expires=0);return null;});
    var cleanup=com.mmorpg.entity.WorldBarricades.class.getDeclaredMethod("clean",net.minecraft.server.MinecraftServer.class,boolean.class);cleanup.setAccessible(true);cleanup.invoke(null,server,false);check(level.isEmptyBlock(barrier),"barricades expire and restore walkable space");
    // dungeon session loot: a reconnection on the spot or a backpack must not shelter it, items owned before entering stay
    var looter=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SessionChecks"));looter.setPos(o.getX()+.5,o.getY(),o.getZ()+130);looter.getInventory().clearContent();
    var bag=Backpacks.inventory(looter);bag.setItem(2,new ItemStack(Items.GOLD_INGOT,2));looter.tickCount=7;SessionLoot.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(looter));
    looter.getInventory().add(new ItemStack(Items.DIAMOND,5));bag.setItem(3,new ItemStack(Items.EMERALD,3));bag.setChanged();SessionLoot.logout(looter);looter.tickCount=27;SessionLoot.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(looter));
    String lostLoot=SessionLoot.onDeath(looter);level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,looter.getBoundingBox().inflate(3)).forEach(net.minecraft.world.entity.Entity::discard);
    check(lostLoot.contains("minecraft:diamond*5")&&lostLoot.contains("minecraft:emerald*3")&&looter.getInventory().countItem(Items.DIAMOND)==0&&Backpacks.inventory(looter).countItem(Items.EMERALD)==0,"session loot survives neither relog nor backpack: "+lostLoot);
    check(Backpacks.inventory(looter).countItem(Items.GOLD_INGOT)==2&&!RpgPlayers.get(looter).backpacks.toString().contains("emerald"),"items carried in before entry are kept and the backpack is saved");
    var center=o.offset(0,-1,Layout.ARENA.z());for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++)for(int y=0;y>=-4;y--)level.setBlock(center.offset(x,y,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
    Class<?> pending=Arrays.stream(BossTerrain.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("Pending")).findFirst().orElseThrow();var constructor=pending.getDeclaredConstructors()[0];constructor.setAccessible(true);var change=BossTerrain.class.getDeclaredMethod("change",pending,UUID.class);change.setAccessible(true);UUID battle=UUID.randomUUID();
    change.invoke(null,constructor.newInstance(level,center,0L,2,0),battle);check(level.isEmptyBlock(center.offset(8,0,4))&&!level.isEmptyBlock(center),"boss destroys bridge while preserving safe center");
    BossTerrain.restore(server,battle.toString());check(level.getBlockState(center.offset(8,0,4)).is(Blocks.DEEPSLATE_TILES),"terrain journal restores after battle");
    change.invoke(null,constructor.newInstance(level,center,0L,2,1),battle);check(level.getBlockState(center.offset(10,0,5)).is(Blocks.WATER)&&!level.getBlockState(center).is(Blocks.WATER),"flood leaves safe central island");BossTerrain.restore(server,battle.toString());
   }finally{InstanceData.get(server).slots.remove(slot);InstanceData.get(server).dirty();DungeonVariants.finish(server,pool,owner);}
   check(com.mmorpg.quest.SpawnDiscovery.definitions().size()==5&&com.mmorpg.quest.SpawnDiscovery.definitions().values().stream().allMatch(q->q.giver.startsWith("spawn_decouverte_")),"five spawn-specific introductory quests");
   var hall=new com.mmorpg.guild.GuildStore.Guild();hall.hallIndex=1;hall.hallLevel=3;hall.banner="EMERAUDE";hall.bosses=3;var job=com.mmorpg.guild.GuildHall.build(hall);check(job.blocks.size()>20000&&job.blocks.stream().filter(c->c.state().is(Blocks.SKELETON_SKULL)).count()==2,"upgraded hall includes exactly earned trophies");
   check(com.mmorpg.entity.MobCooperation.role("veilleur_champignon")==com.mmorpg.entity.MobCooperation.Role.HEALER&&com.mmorpg.entity.MobCooperation.role("archer_sylvestre")==com.mmorpg.entity.MobCooperation.Role.SCOUT&&com.mmorpg.entity.MobCooperation.role("gobelin")==com.mmorpg.entity.MobCooperation.Role.ENGINEER,"existing bestiary provides all squad roles");
   com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] EXPEDITIONS PASS: stable previews/rerolls, walkable route variants, real runic puzzle/cosmetic, class seal, reversible bridge/flood, spawn quests, hall upgrades, earned trophies and session loot (relog, backpack)");
  }finally{NetworkConfig.current=original;}
 }
}
