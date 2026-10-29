package com.mmorpg.competition;

import com.mmorpg.MMORPG;
import com.mmorpg.guild.GuildService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class CompetitionEvents {
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){ArenaManager.tick(e.getServer());GuildRaids.tick(e.getServer());if(e.getServer().getTickCount()%100==0){GuildService.flush(e.getServer());GuildService.presence(e.getServer());for(var p:e.getServer().getPlayerList().getPlayers())GuildService.refresh(p);}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),20,()->{try{GuildService.reconcile(p);GuildService.refresh(p);if(!com.mmorpg.network.AdminTravel.observing(p)&&CompetitionTravel.inside(p)&&ArenaManager.session(p.getUUID())==null&&GuildRaids.of(p.getUUID())==null)CompetitionTravel.home(p);}catch(Exception ex){MMORPG.LOGGER.error("Guild login reconciliation",ex);}});}
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){GuildService.flush(e.getServer());ArenaManager.shutdown(e.getServer());GuildRaids.shutdown(e.getServer());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingIncomingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p){if(e.getSource().getEntity() instanceof ServerPlayer a&&(GuildRaids.of(p.getUUID())!=null||GuildRaids.of(a.getUUID())!=null)){e.setCanceled(true);return;}if(ArenaManager.protectedPlayer(p)){e.setCanceled(true);return;}if(e.getSource().getEntity() instanceof ServerPlayer attacker&&(ArenaManager.session(p.getUUID())!=null||ArenaManager.session(attacker.getUUID())!=null)&&(!ArenaManager.enemies(p,attacker)||ArenaManager.protectedPlayer(attacker)))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){if(ArenaManager.lethal(p,e.getSource().getEntity())||GuildRaids.testDeath(p))e.setCanceled(true);else GuildRaids.died(p);}}
    @SubscribeEvent public static void naturalSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent e){if(e.getLevel().getLevel().dimension().equals(CompetitionWorld.DIM)&&e.getSpawnType()!=net.minecraft.world.entity.EntitySpawnReason.COMMAND&&e.getSpawnType()!=net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE)e.setSpawnCancelled(true);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("pvp").executes(c->{PvpProgression.open(c.getSource().getPlayerOrException(),"");return 1;}));
        var victory=Commands.literal("victoire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));for(String color:new String[]{"rouge","bleu","bleue"}){int chosen=color.equals("rouge")?0:1;victory.then(Commands.literal(color).executes(c->{var p=c.getSource().getPlayerOrException();ArenaManager.forceWin(p,p,chosen);return 1;}).then(Commands.argument("joueur",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->{ArenaManager.forceWin(c.getSource().getPlayerOrException(),net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"joueur"),chosen);return 1;})));}e.getDispatcher().register(Commands.literal("arene").then(victory).then(Commands.literal("bilan").executes(c->{var p=c.getSource().getPlayerOrException();if(!PvpProgression.showReport(p,true))p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Aucun bilan d’arène enregistré."));return 1;})));

        e.getDispatcher().register(Commands.literal("guilde").executes(c->{GuildService.open(c.getSource().getPlayerOrException());return 1;}).then(Commands.literal("sortir").executes(c->{GuildRaids.leave(c.getSource().getPlayerOrException());return 1;})));
        e.getDispatcher().register(Commands.literal("arene").executes(c->{ArenaManager.open(c.getSource().getPlayerOrException(),"");return 1;}).then(Commands.literal("quitter").executes(c->{ArenaManager.leave(c.getSource().getPlayerOrException());return 1;})));
    }
    @SubscribeEvent public static void breakBlock(net.neoforged.neoforge.event.level.block.BreakBlockEvent e){if(e.getPlayer().level().dimension().equals(CompetitionWorld.DIM)&&!e.getPlayer().isCreative())e.setCanceled(true);}
    @SubscribeEvent public static void place(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof net.minecraft.server.level.ServerLevel l&&l.dimension().equals(CompetitionWorld.DIM))e.setCanceled(true);}
    @SubscribeEvent public static void explosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate e){if(e.getLevel().dimension().equals(CompetitionWorld.DIM))e.getAffectedBlocks().clear();}
}
