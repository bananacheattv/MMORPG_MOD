package com.mmorpg.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** La Mascotte : pet dragon du spawn qui guide le tutoriel. PNJ de quetes (groupe « tutoriel »), immobile, qui flotte sur place. */
public final class MascotEntity extends NpcEntity {
    public static final UUID ID = com.mmorpg.quest.QuestNpcPlan.MASCOT_ID;
    public static final String NAME = "Mascotte";

    public MascotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void init() {
        setUUID(ID);
        setup(Role.QUETES, NAME, 0, com.mmorpg.server.MascotGuide.GROUP);
    }
}
