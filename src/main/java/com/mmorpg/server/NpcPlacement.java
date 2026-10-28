package com.mmorpg.server;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.world.NpcDirectory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Operator-only placement. Identity, quests and shop are preserved. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class NpcPlacement {
 private static final Map<UUID,UUID> SELECTED=new HashMap<>();
 public static void select(ServerPlayer p,NpcEntity npc){if(!QuestEditor.allowed(p))return;SELECTED.put(p.getUUID(),npc.getUUID());p.sendSystemMessage(Component.literal("PNJ sélectionné : "+npc.displayName()+". Clic droit sur un sol avec l’outil pour le déplacer ; Maj + clic pour changer de PNJ."));}
 public static NpcEntity find(ServerPlayer p,String id){
  if(!QuestEditor.allowed(p))throw new IllegalArgumentException("Réservé aux opérateurs.");UUID uuid=UUID.fromString(id);var entity=p.level().getEntity(uuid);
  if(!(entity instanceof NpcEntity)){var entry=NpcDirectory.get(p.level().getServer()).entry(id);if(entry!=null&&entry.getStringOr("dimension","").equals(p.level().dimension().identifier().toString())){var at=BlockPos.of(entry.getLongOr("pos",0));p.level().getChunkAt(at);entity=p.level().getEntity(uuid);}}
  if(!(entity instanceof NpcEntity npc)||npc instanceof com.mmorpg.entity.hub.HubGuideEntity||npc.role()==NpcEntity.Role.SORTIE)throw new IllegalArgumentException("Rejoignez la zone de ce PNJ du spawn pour le déplacer.");return npc;
 }
 public static void move(ServerPlayer p,NpcEntity npc,BlockPos at,float yaw){
  if(!QuestEditor.allowed(p)||npc.level()!=p.level()||!p.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM))throw new IllegalArgumentException("Placement réservé aux PNJ du spawn.");
  if(!p.level().getWorldBorder().isWithinBounds(at)||at.getY()<p.level().getMinY()+1||at.getY()+2>=p.level().getMaxY()||p.level().getBlockState(at.below()).getCollisionShape(p.level(),at.below()).isEmpty())throw new IllegalArgumentException("Choisissez un sol solide.");
  var box=npc.getBoundingBox().move(at.getX()+.5-npc.getX(),at.getY()-npc.getY(),at.getZ()+.5-npc.getZ());
  if(p.level().getBlockCollisions(npc,box).iterator().hasNext())throw new IllegalArgumentException("Le PNJ manque de place ici.");
  npc.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,yaw,0);npc.setYHeadRot(yaw);npc.yBodyRot=yaw;npc.setPersistenceRequired();NpcDirectory.get(p.level().getServer()).record(npc);com.mmorpg.quest.SpawnDiscovery.refreshNavigation(p.level().getServer());
 }
 public static boolean placeSelected(ServerPlayer p,BlockPos at){var id=SELECTED.get(p.getUUID());if(id==null)return false;try{var npc=find(p,id.toString());move(p,npc,at,p.getYRot()+180);p.sendSystemMessage(Component.literal("PNJ déplacé : "+npc.displayName()));}catch(IllegalArgumentException e){p.sendSystemMessage(Component.literal(e.getMessage()));}return true;}
 @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SELECTED.remove(e.getEntity().getUUID());}
 @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SELECTED.clear();}
}
