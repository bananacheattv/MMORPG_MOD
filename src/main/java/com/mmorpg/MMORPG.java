package com.mmorpg;

import com.mmorpg.network.Net;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModCreativeTabs;
import com.mmorpg.registry.ModDataComponents;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.registry.ModItems;
import com.mmorpg.skill.Skills;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Eldoria MMORPG — point d'entree du mod.
 * Classes evolutives, competences, statistiques, equipement, monstres et boss configurables,
 * familiers, teleporteurs, donjons et cosmetiques animes.
 */
@Mod(MMORPG.MODID)
public final class MMORPG {
    public static final String MODID = "mmorpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MMORPG(IEventBus modBus, ModContainer container) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(Net::register);
        Skills.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
