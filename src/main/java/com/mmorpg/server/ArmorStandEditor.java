package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class ArmorStandEditor {
    public static final EquipmentSlot[] SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    public static void aim(ServerPlayer p){
        if(!QuestEditor.allowed(p))return;var start=p.getEyePosition();var end=start.add(p.getLookAngle().scale(8));
        var stand=p.level().getEntitiesOfClass(ArmorStand.class,new net.minecraft.world.phys.AABB(start,end).inflate(1)).stream()
                .filter(s->s.getBoundingBox().inflate(.2).clip(start,end).isPresent()&&p.hasLineOfSight(s)).min(java.util.Comparator.comparingDouble(s->s.distanceToSqr(p))).orElse(null);
        if(stand==null){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Vise un porte-armure à huit blocs maximum."));return;}
        open(p,stand);
    }
    public static void open(ServerPlayer p,ArmorStand stand){if(!QuestEditor.allowed(p)||stand.level()!=p.level()||stand.distanceToSqr(p)>64)return;var t=new CompoundTag();t.putInt("entity",stand.getId());Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARMOR_STAND_EDITOR,t));}
    public static Rotations pose(ArmorStand s,int part){return switch(part){case 0->s.getHeadPose();case 1->s.getBodyPose();case 2->s.getLeftArmPose();case 3->s.getRightArmPose();case 4->s.getLeftLegPose();default->s.getRightLegPose();};}
    private static void pose(ArmorStand s,int part,Rotations r){switch(part){case 0->s.setHeadPose(r);case 1->s.setBodyPose(r);case 2->s.setLeftArmPose(r);case 3->s.setRightArmPose(r);case 4->s.setLeftLegPose(r);case 5->s.setRightLegPose(r);}}
    public static void action(ServerPlayer p,CompoundTag t){
        if(!QuestEditor.allowed(p)||!(p.level().getEntity(t.getIntOr("entity",-1)) instanceof ArmorStand s)||s.distanceToSqr(p)>100||!p.hasLineOfSight(s))return;
        s.setShowArms(true);s.setNoGravity(true);
        switch(t.getStringOr("action","")){
            case "equip"->{int slot=t.getIntOr("slot",-1),inventory=t.getIntOr("inventory",-1);if(slot<0||slot>=SLOTS.length)return;if(inventory<0)s.setItemSlot(SLOTS[slot],ItemStack.EMPTY);else if(inventory<p.getInventory().getContainerSize()){var copy=p.getInventory().getItem(inventory).copy();copy.setCount(1);s.setItemSlot(SLOTS[slot],copy);}}
            case "pose"->{int part=t.getIntOr("part",-1),axis=t.getIntOr("axis",-1),delta=t.getIntOr("delta",0);if(part<0||part>5||axis<0||axis>2||Math.abs(delta)>15)return;var r=pose(s,part);float[] v={r.x(),r.y(),r.z()};v[axis]+=delta;pose(s,part,new Rotations(v[0],v[1],v[2]));}
            case "preset"->{int preset=t.getIntOr("preset",0);for(int part=0;part<6;part++)pose(s,part,new Rotations(0,0,0));if(preset==1){s.setRightArmPose(new Rotations(-105,0,15));s.setLeftArmPose(new Rotations(-65,20,-15));s.setBodyPose(new Rotations(10,-15,0));}else if(preset==2){s.setRightArmPose(new Rotations(-90,-40,0));s.setLeftArmPose(new Rotations(-90,35,0));s.setHeadPose(new Rotations(0,-25,0));}else if(preset==3){s.setRightArmPose(new Rotations(45,0,0));s.setLeftArmPose(new Rotations(-45,0,0));s.setRightLegPose(new Rotations(-35,0,0));s.setLeftLegPose(new Rotations(35,0,0));s.setBodyPose(new Rotations(12,0,0));}}
            case "move"->{int axis=t.getIntOr("axis",-1),delta=t.getIntOr("delta",0);if(axis<0||axis>3||Math.abs(delta)>1)return;if(axis==3){s.setYRot(s.getYRot()+delta*15);s.yBodyRot=s.getYRot();}else{s.teleportTo(s.getX()+(axis==0?delta*.1:0),s.getY()+(axis==1?delta*.1:0),s.getZ()+(axis==2?delta*.1:0));}}
        }
    }
    @SubscribeEvent public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof ServerPlayer p&&event.getTarget() instanceof ArmorStand s&&p.getMainHandItem().is(com.mmorpg.registry.ModItems.ARMOR_STAND_EDITOR.get())){event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);open(p,s);}
    }
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event){event.getDispatcher().register(net.minecraft.commands.Commands.literal("outilarmure").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)).executes(c->{RpgPlayers.give(c.getSource().getPlayerOrException(),com.mmorpg.registry.ModItems.ARMOR_STAND_EDITOR.get(),1);return 1;}));}
}
