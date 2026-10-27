package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class SceneryPersistence {
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var entity=event.getEntity();
        if(entity instanceof com.mmorpg.entity.NpcEntity||entity instanceof com.mmorpg.entity.dummy.TrainingDummyEntity||entity instanceof com.mmorpg.entity.hub.ClassMasterEntity){
            var mob=(Mob)entity;mob.setPersistenceRequired();mob.setNoGravity(true);mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if(entity instanceof Display.TextDisplay&&!entity.entityTags().contains("aeloria_visibility_v1"))Scheduler.later(level,2,()->{
            if(entity.isRemoved())return;
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),"data merge entity "+entity.getUUID()+" {view_range:3.0f}");
            entity.addTag("aeloria_visibility_v1");
        });
    }
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event){
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("tuto").executes(c->{com.mmorpg.network.Net.toPlayer(c.getSource().getPlayerOrException(),new com.mmorpg.network.S2COpenScreen(com.mmorpg.network.S2COpenScreen.HUB_TUTORIAL,new net.minecraft.nbt.CompoundTag()));return 1;}));
    }
}
