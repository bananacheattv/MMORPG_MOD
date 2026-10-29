package com.mmorpg.job;

import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Experience, niveaux, recompenses de paliers et bonus passifs des metiers (cote serveur). */
public final class JobSystem {
    private JobSystem() {
    }

    public static int level(PlayerData d, Job job) {
        return Job.levelOf(d.jobXp, job);
    }

    public static int level(ServerPlayer p, Job job) {
        return level(RpgPlayers.get(p), job);
    }

    public static void addXp(ServerPlayer p, Job job, long amount) {
        PlayerData d = RpgPlayers.get(p);
        addXp(p, d, job, amount);
    }

    /** Ajoute de l'experience de metier, gere les niveaux et octroie les recompenses des paliers franchis. */
    public static void addXp(ServerPlayer p, PlayerData d, Job job, long amount) {
        if (amount <= 0) return;
        long max = Job.totalFor(Job.MAX_LEVEL);
        long before = d.jobXp.getOrDefault(job.id, 0L);
        if (before >= max) return;
        int oldLevel = Job.levelFor(before);
        long after = Math.min(max, before + amount);
        d.jobXp.put(job.id, after);
        d.dirty = true;
        int newLevel = Job.levelFor(after);
        if (p != null) p.sendOverlayMessage(Component.literal("+" + amount + " XP " + job.label).withColor(job.color & 0xFFFFFF));
        if (newLevel <= oldLevel || p == null) return;
        p.sendSystemMessage(Component.literal("✦ " + job.label + " niveau " + newLevel).withColor(job.color & 0xFFFFFF));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
        syncRewards(p, d);
    }

    // ------------------------------------------------------------------ recompenses de paliers

    public static String claimKey(Job job, Job.Reward r) {
        return job.id + ":" + r.level();
    }

    /** Octroie toutes les recompenses des paliers atteints et pas encore recues (appele a la connexion et a chaque niveau). */
    public static int syncRewards(ServerPlayer p, PlayerData d) {
        int granted = 0;
        for (Job job : Job.values()) {
            int lvl = level(d, job);
            for (Job.Reward r : job.rewards) {
                if (lvl >= r.level() && d.jobClaimed.add(claimKey(job, r))) {
                    grant(p, d, job, r);
                    granted++;
                }
            }
        }
        if (granted > 0) d.dirty = true;
        return granted;
    }

    private static void grant(ServerPlayer p, PlayerData d, Job job, Job.Reward r) {
        if (r.cosmeticId() != null) d.cosmetics.add(r.cosmeticId());
        if (r.gold() > 0) d.gold += r.gold();
        if (p == null) return;
        if (r.itemId() != null) give(p, new ItemStack(ModItems.get(r.itemId()), r.itemCount()));
        p.sendSystemMessage(Component.literal("★ Palier " + r.level() + " — " + job.label + " : titre « " + r.title() + " » débloqué").withColor(0xFFE2B85A));
        if (r.recipeLabel() != null) p.sendSystemMessage(Component.literal("   Recette de forge : " + r.recipeLabel()).withColor(0xFFB9ABCF));
        if (r.cosmeticId() != null) {
            Cosmetic c = Cosmetic.byId(r.cosmeticId());
            p.sendSystemMessage(Component.literal("   Cosmétique : " + (c == null ? r.cosmeticId() : c.label)).withColor(0xFFB9ABCF));
        }
        if (r.itemId() != null) p.sendSystemMessage(Component.literal("   Objet : " + r.itemCount() + " × " + new ItemStack(ModItems.get(r.itemId())).getHoverName().getString()).withColor(0xFFB9ABCF));
        if (r.gold() > 0) p.sendSystemMessage(Component.literal("   " + r.gold() + " pièces d'or").withColor(0xFFB9ABCF));
    }

    /** Equipe (ou retire) le titre d'un palier de metier. Action envoyee par l'ecran Metiers. */
    public static void action(ServerPlayer p, CompoundTag t) {
        PlayerData d = RpgPlayers.get(p);
        if (!t.getStringOr("action", "").equals("title")) return;
        Job job = Job.byId(t.getStringOr("job", ""));
        int lvl = t.getIntOr("level", 0);
        if (job == null || lvl == 0) {
            d.pvpTitle = "";
        } else {
            Job.Reward r = job.rewards.stream().filter(x -> x.level() == lvl).findFirst().orElse(null);
            if (r == null || level(d, job) < r.level()) return;
            d.pvpTitle = r.title();
        }
        d.dirty = true;
        RpgPlayers.updatePublic(p);
    }

    // ------------------------------------------------------------------ bonus passifs

    /** Vrai si la prise / recolte / extraction doit etre doublee. */
    public static boolean rollDouble(ServerPlayer p, Job job, RandomSource random) {
        return random.nextDouble() < Job.doubleChance(level(p, job));
    }

    public static void give(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!p.getInventory().add(stack)) p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), stack));
    }

    /** Repas du fermier : effets temporaires apres une recolte, selon le palier. */
    public static void farmerMeal(ServerPlayer p, RandomSource random) {
        int lvl = level(p, Job.FERMIER);
        if (random.nextDouble() >= Job.mealChance(lvl)) return;
        int tier = Job.tier(lvl), secs = Job.mealSeconds(lvl);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, secs * 20, Math.min(1, tier / 3)));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, secs * 20, tier >= 3 ? 1 : 0));
        if (tier >= 5) p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, secs * 20, 0));
        p.sendOverlayMessage(Component.literal("Repas du fermier : effets pendant " + secs + " s").withColor(0xFF7BCB4A));
    }

    // ------------------------------------------------------------------ plats

    private static void fx(ServerPlayer p, Holder<MobEffect> effect, int seconds, int amp) {
        p.addEffect(new MobEffectInstance(effect, seconds * 20, amp));
    }

    /** Effets d'un plat de metier : soin et effets de potion selon la recette. Renvoie faux si l'objet n'est pas un plat. */
    public static boolean eatDish(ServerPlayer p, String id) {
        double heal;
        switch (id) {
            case "tonique_du_mineur" -> { heal = 800; fx(p, MobEffects.HASTE, 180, 1); fx(p, MobEffects.NIGHT_VISION, 180, 0); }
            case "bouillon_de_roche" -> { heal = 2000; fx(p, MobEffects.HASTE, 300, 2); fx(p, MobEffects.RESISTANCE, 300, 0); }
            case "elixir_du_seigneur" -> { heal = 4500; fx(p, MobEffects.HASTE, 600, 3); fx(p, MobEffects.RESISTANCE, 600, 1); fx(p, MobEffects.REGENERATION, 60, 1); fx(p, MobEffects.NIGHT_VISION, 600, 0); }
            case "soupe_de_racine" -> { heal = 800; fx(p, MobEffects.SPEED, 180, 0); fx(p, MobEffects.REGENERATION, 30, 0); }
            case "tourte_aux_baies" -> { heal = 2000; fx(p, MobEffects.SPEED, 300, 1); fx(p, MobEffects.REGENERATION, 45, 1); fx(p, MobEffects.ABSORPTION, 300, 0); }
            case "banquet_des_moissons" -> { heal = 4500; fx(p, MobEffects.SPEED, 600, 1); fx(p, MobEffects.REGENERATION, 60, 1); fx(p, MobEffects.STRENGTH, 600, 0); fx(p, MobEffects.ABSORPTION, 600, 1); }
            case "filet_grille" -> { heal = 800; fx(p, MobEffects.WATER_BREATHING, 180, 0); fx(p, MobEffects.DOLPHINS_GRACE, 120, 0); }
            case "ragout_des_profondeurs" -> { heal = 2000; fx(p, MobEffects.WATER_BREATHING, 300, 0); fx(p, MobEffects.DOLPHINS_GRACE, 300, 0); fx(p, MobEffects.LUCK, 300, 0); }
            case "festin_du_leviathan" -> { heal = 4500; fx(p, MobEffects.WATER_BREATHING, 600, 0); fx(p, MobEffects.CONDUIT_POWER, 600, 0); fx(p, MobEffects.LUCK, 600, 1); fx(p, MobEffects.REGENERATION, 60, 1); }
            default -> { return false; }
        }
        RpgPlayers.heal(p, heal, true);
        return true;
    }
}
