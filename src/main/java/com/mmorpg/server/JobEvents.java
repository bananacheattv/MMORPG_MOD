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

    /** Experience d'une prise : poisson commun, poisson rare, tresor ou dechet. */
    public static int catchXp(ItemStack s) {
        if (s.is(Items.COD) || s.is(Items.SALMON)) return Job.FISH_COMMON_XP;
        if (s.is(Items.TROPICAL_FISH) || s.is(Items.PUFFERFISH)) return Job.FISH_RARE_XP;
        if (s.is(Items.BOWL) || s.is(Items.LEATHER) || s.is(Items.LEATHER_BOOTS) || s.is(Items.ROTTEN_FLESH) || s.is(Items.STICK)
                || s.is(Items.STRING) || s.is(Items.BONE) || s.is(Items.INK_SAC) || s.is(Items.TRIPWIRE_HOOK) || s.is(Items.BAMBOO)
               ) return Job.FISH_JUNK_XP;
        return Job.FISH_TREASURE_XP;
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var random = p.getRandom();
        int lvl = JobSystem.level(p, Job.PECHEUR);
        int tier = Job.tier(lvl);
        int xp = 0;
        for (ItemStack s : event.getDrops()) xp += catchXp(s);
        JobSystem.addXp(p, Job.PECHEUR, Math.max(Job.FISH_JUNK_XP, xp));
        if (JobSystem.rollDouble(p, Job.PECHEUR, random)) {
            for (ItemStack s : event.getDrops()) JobSystem.give(p, s.copy());
            p.sendOverlayMessage(Component.literal("Prise doublée !").withColor(0xFF4FB3E8));
        }
        // poissons rares et ingredients marins (a partir du palier 10)
        if (random.nextDouble() < Job.rareCatchChance(lvl)) {
            ItemStack rare = switch (tier) {
                case 1 -> new ItemStack(random.nextBoolean() ? Items.PUFFERFISH : Items.TROPICAL_FISH);
                case 2, 3 -> new ItemStack(random.nextBoolean() ? Items.NAUTILUS_SHELL : Items.PRISMARINE_CRYSTALS, 1 + random.nextInt(2));
                default -> new ItemStack(random.nextInt(4) == 0 ? Items.HEART_OF_THE_SEA : Items.NAUTILUS_SHELL);
            };
            JobSystem.give(p, rare);
            p.sendOverlayMessage(Component.literal("Prise rare : " + rare.getHoverName().getString()).withColor(0xFFE2B85A));
        }
        if (random.nextDouble() < Job.ingredientChance(lvl)) {
            JobSystem.give(p, new ItemStack(random.nextBoolean() ? Items.KELP : Items.PRISMARINE_SHARD, 1 + random.nextInt(tier + 1)));
        }
    }

    @SubscribeEvent
    public static void miningSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getState().getBlock() instanceof MineOreBlock)) return;
        event.setNewSpeed(event.getNewSpeed() * Job.miningSpeed(JobSystem.level(p, Job.MINEUR)));
    }
}
