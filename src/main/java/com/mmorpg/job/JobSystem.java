package com.mmorpg.job;

import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;

/** Experience, niveaux et bonus passifs des metiers (cote serveur). */
public final class JobSystem {
    private JobSystem() {
    }

    public static int level(PlayerData d, Job job) {
        return Job.levelFor(d.jobXp.getOrDefault(job.id, 0L));
    }

    public static int level(ServerPlayer p, Job job) {
        return level(RpgPlayers.get(p), job);
    }

    public static void addXp(ServerPlayer p, Job job, long amount) {
        if (amount <= 0) return;
        PlayerData d = RpgPlayers.get(p);
        long max = Job.totalFor(Job.MAX_LEVEL);
        long before = d.jobXp.getOrDefault(job.id, 0L);
        if (before >= max) return;
        int oldLevel = Job.levelFor(before);
        long after = Math.min(max, before + amount);
        d.jobXp.put(job.id, after);
        d.dirty = true;
        int newLevel = Job.levelFor(after);
        p.sendOverlayMessage(Component.literal("+" + amount + " XP " + job.label).withColor(job.color & 0xFFFFFF));
        if (newLevel <= oldLevel) return;
        p.sendSystemMessage(Component.literal("✦ " + job.label + " niveau " + newLevel).withColor(job.color & 0xFFFFFF));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
        for (Job.Milestone m : job.milestones) {
            if (m.level() > oldLevel && m.level() <= newLevel) {
                p.sendSystemMessage(Component.literal("★ Palier " + m.level() + " — " + job.label + " : titre « " + m.title() + " », " + m.recipe() + ", " + m.cosmetic()).withColor(0xFFE2B85A));
                p.sendSystemMessage(Component.literal("   Bonus : " + m.bonus()).withColor(0xFFB9ABCF));
            }
        }
    }

    /** Vrai si la prise / recolte / extraction doit etre doublee. */
    public static boolean rollDouble(ServerPlayer p, Job job, RandomSource random) {
        return random.nextDouble() < Job.doubleChance(level(p, job));
    }

    public static void give(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!p.getInventory().add(stack)) p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), stack));
    }

    /** Plat du fermier : effet temporaire selon le palier, apres une recolte. */
    public static void farmerMeal(ServerPlayer p, RandomSource random) {
        int tier = Job.tier(level(p, Job.FERMIER));
        if (tier == 0 || random.nextInt(100) >= 15 + tier * 5) return;
        int secs = 20 + tier * 20;
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, secs * 20, tier - 1));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, secs * 20, tier >= 2 ? 1 : 0));
        if (tier >= 3) p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, secs * 20, 0));
        p.sendOverlayMessage(Component.literal("Plat du fermier : effets pendant " + secs + " s").withColor(0xFF7BCB4A));
    }
}
