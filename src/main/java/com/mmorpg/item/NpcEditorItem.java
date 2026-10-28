package com.mmorpg.item;

import com.mmorpg.entity.NpcEntity;
import com.mmorpg.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Operator tool: select the nearest visible NPC under the crosshair. */
public final class NpcEditorItem extends Item {
    public NpcEditorItem(Properties properties){super(properties.stacksTo(1));}
    public static boolean edit(ServerPlayer p,NpcEntity npc){
        if(!QuestEditor.allowed(p)){p.sendSystemMessage(Component.literal("Outil réservé aux opérateurs."));return false;}
        if(npc.level()!=p.level()||npc.distanceToSqr(p)>64||!p.hasLineOfSight(npc))return false;
        if(p.isShiftKeyDown()){NpcPlacement.select(p,npc);return true;}
        if(npc.role()==NpcEntity.Role.MARCHAND)MerchantEditor.open(p,npc);else QuestEditor.open(p,npc.getUUID().toString());
        return true;
    }
    private static void aimed(ServerPlayer p){
        if(!QuestEditor.allowed(p)){p.sendSystemMessage(Component.literal("Outil réservé aux opérateurs."));return;}
        var start=p.getEyePosition();var end=p.pick(8,0,false).getLocation();
        var target=p.level().getEntitiesOfClass(NpcEntity.class,new net.minecraft.world.phys.AABB(start,end).inflate(1)).stream()
            .filter(n->n.getBoundingBox().inflate(.15).clip(start,end).isPresent())
            .min(java.util.Comparator.comparingDouble(n->n.getBoundingBox().inflate(.15).clip(start,end).orElse(end).distanceToSqr(start))).orElse(null);
        if(target==null||!edit(p,target))p.sendSystemMessage(Component.literal("Vise un PNJ visible à huit blocs maximum."));
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p)aimed(p);return InteractionResult.SUCCESS;}
    @Override public InteractionResult useOn(UseOnContext context){if(context.getPlayer() instanceof ServerPlayer p){if(!p.isShiftKeyDown()&&NpcPlacement.placeSelected(p,context.getClickedPos().relative(context.getClickedFace())))return InteractionResult.SUCCESS;aimed(p);}return InteractionResult.SUCCESS;}
}
