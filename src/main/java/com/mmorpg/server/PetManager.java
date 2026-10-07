package com.mmorpg.server;

import com.mmorpg.entity.PetEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/** Gestion des familiers actifs : invocation, maintien a cote du joueur, experience. */
public final class PetManager {
    private PetManager() {
    }

    public static void setActive(ServerPlayer player, String id) {
        PlayerData d = RpgPlayers.get(player);
        despawn(player);
        if (id == null || id.isEmpty()) {
            d.activePet = "";
        } else {
            PetType type = PetType.byId(id);
            if (type == null || !d.petXp.containsKey(id)) return;
            d.activePet = id;
            spawn(player, type);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1f, 1.2f);
        }
        RpgPlayers.recomputeAndSync(player);
    }

    private static void spawn(ServerPlayer player, PetType type) {
        ServerLevel level = player.level();
        PetEntity pet = new PetEntity(ModEntities.PET.get(), level);
        pet.setup(player, type);
        level.addFreshEntity(pet);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY(), pet.getZ(), 10, 0.3, 0.3, 0.3, 0);
        RpgPlayers.get(player).petEntity = pet.getUUID();
    }

    public static void despawn(ServerPlayer player) {
        PlayerData d = RpgPlayers.get(player);
        if (d.petEntity != null) {
            for (ServerLevel level : player.level().getServer().getAllLevels()) {
                Entity e = level.getEntity(d.petEntity);
                if (e != null) e.discard();
            }
            d.petEntity = null;
        }
    }

    /** Appele chaque seconde : recree le familier s'il a disparu (changement de dimension, dechargement...). */
    public static void maintain(ServerPlayer player) {
        PlayerData d = RpgPlayers.get(player);
        if (d.activePet.isEmpty() || !player.isAlive() || player.isSpectator()) return;
        PetType type = PetType.byId(d.activePet);
        if (type == null) return;
        if (player.level().getGameTime() % 1200 == 0 && d.happiness(type.id) > 0) {
            d.petHappiness.put(type.id, d.happiness(type.id) - 1);
            RpgPlayers.recomputeAndSync(player);
        }
        Entity existing = d.petEntity == null ? null : player.level().getEntity(d.petEntity);
        if (existing == null || !existing.isAlive()) {
            despawn(player);
            spawn(player, type);
        }
    }

    /** Food is consumed only when an owned, summoned companion needs it. */
    public static boolean feed(ServerPlayer player, int amount) {
        PlayerData d = RpgPlayers.get(player);
        if (d.activePet.isEmpty() || !d.petXp.containsKey(d.activePet) || d.happiness(d.activePet) >= 100) {
            player.sendOverlayMessage(Component.literal("Invoquez un familier qui a faim."));
            return false;
        }
        d.petHappiness.put(d.activePet, Math.min(100, d.happiness(d.activePet) + amount));
        RpgPlayers.recomputeAndSync(player);
        player.level().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 5, 0.5, 0.5, 0.5, 0);
        return true;
    }

    public static void addPetXp(ServerPlayer player, String id, int xp) {
        PlayerData d = RpgPlayers.get(player);
        if (!d.petXp.containsKey(id) || xp <= 0) return;
        int before = d.petLevel(id);
        d.petXp.merge(id, xp, Integer::sum);
        int after = d.petLevel(id);
        if (after > before) {
            PetType type = PetType.byId(id);
            String name = type != null ? type.label : id;
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.INFO, "FAMILIER NIVEAU " + after, name + " devient plus puissant !",
                    type != null ? type.color : 0xFFFFFF));
            player.sendSystemMessage(Component.literal("✦ " + name + " atteint le niveau " + after + " !").withColor(0xE8C060));
            RpgPlayers.recompute(player);
        }
        d.dirty = true;
    }
}
