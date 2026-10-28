package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.job.Job;
import com.mmorpg.job.JobSystem;
import com.mmorpg.mine.MineOreBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Metiers : peche (XP, doubles prises, poissons rares) et vitesse de minage. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class JobEvents {
    private JobEvents() {
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var random = p.getRandom();
        int lvl = JobSystem.level(p, Job.PECHEUR);
        int tier = Job.tier(lvl);
        int caught = Math.max(1, event.getDrops().size());
        JobSystem.addXp(p, Job.PECHEUR, 6L + lvl / 4 + caught * 2L);
        if (JobSystem.rollDouble(p, Job.PECHEUR, random)) {
            for (ItemStack s : event.getDrops()) JobSystem.give(p, s.copy());
            p.sendOverlayMessage(Component.literal("Prise doublée !").withColor(0xFF4FB3E8));
        }
        // poissons rares et ingredients marins (a partir du palier 10)
        if (tier >= 1 && random.nextDouble() < 0.04 + lvl * 0.004) {
            ItemStack rare = switch (tier) {
                case 1 -> new ItemStack(random.nextBoolean() ? Items.PUFFERFISH : Items.TROPICAL_FISH);
                case 2 -> new ItemStack(random.nextBoolean() ? Items.NAUTILUS_SHELL : Items.PRISMARINE_CRYSTALS, 1 + random.nextInt(2));
                default -> new ItemStack(random.nextInt(4) == 0 ? Items.HEART_OF_THE_SEA : Items.NAUTILUS_SHELL);
            };
            JobSystem.give(p, rare);
            p.sendOverlayMessage(Component.literal("Prise rare : " + rare.getHoverName().getString()).withColor(0xFFE2B85A));
        }
        if (tier >= 1 && random.nextDouble() < 0.15 + tier * 0.05) {
            JobSystem.give(p, new ItemStack(random.nextBoolean() ? Items.KELP : Items.INK_SAC, 1 + random.nextInt(tier + 1)));
        }
    }

    @SubscribeEvent
    public static void miningSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getState().getBlock() instanceof MineOreBlock)) return;
        event.setNewSpeed(event.getNewSpeed() * Job.miningSpeed(JobSystem.level(p, Job.MINEUR)));
    }
}
