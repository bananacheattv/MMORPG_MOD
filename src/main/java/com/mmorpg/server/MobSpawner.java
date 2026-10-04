package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.GeneralConfig;
import com.mmorpg.config.MobConfig;
import com.mmorpg.entity.RpgMob;
import com.mmorpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Apparition naturelle des monstres RPG autour des joueurs, entierement pilotee par mobs.json
 * (biomes, dimensions, obscurite, altitude, poids, taille des groupes).
 */
public final class MobSpawner {
    private MobSpawner() {
    }

    private record Candidate(String key, MobConfig cfg, EntityType<? extends Mob> type) {
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> typeOf(String key) {
        var holder = ModEntities.SPAWNABLE.get(key);
        return holder == null ? null : (EntityType<? extends Mob>) holder.get();
    }

    public static void tick(ServerLevel level) {
        GeneralConfig g = ConfigManager.general();
        if (!g.customSpawning || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (level.getGameTime() % Math.max(20, g.spawnIntervalTicks) != 0) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            trySpawnNear(level, player, g);
        }
    }

    private static void trySpawnNear(ServerLevel level, ServerPlayer player, GeneralConfig g) {
        AABB area = player.getBoundingBox().inflate(48, 24, 48);
        int nearby = level.getEntitiesOfClass(Mob.class, area, m -> m instanceof RpgMob).size();
        if (nearby >= g.maxCustomMobsNearPlayer) return;
        var random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 24 + random.nextDouble() * 20;
        int x = (int) (player.getX() + Math.cos(angle) * dist);
        int z = (int) (player.getZ() + Math.sin(angle) * dist);
        if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) return;
        BlockPos pos = findGround(level, x, player.getBlockY(), z);
        if (pos == null) return;
        Holder<Biome> biome = level.getBiome(pos);
        String dim = level.dimension().identifier().toString();
        boolean dark = level.getBrightness(LightLayer.BLOCK, pos) <= 7 && (level.getBrightness(LightLayer.SKY, pos) <= 7 || !level.isBrightOutside());

        List<Candidate> candidates = new ArrayList<>();
        int totalWeight = 0;
        for (Map.Entry<String, MobConfig> e : ConfigManager.mobs().entrySet()) {
            MobConfig cfg = e.getValue();
            MobConfig.Spawn s = cfg.spawn;
            if (cfg.boss || !s.enabled || s.weight <= 0) continue;
            if (!s.dimensions.isEmpty() && !s.dimensions.contains(dim)) continue;
            if (pos.getY() < s.minY || pos.getY() > s.maxY) continue;
            if (s.darkOnly && !dark) continue;
            if (!biomeMatches(biome, s.biomes)) continue;
            EntityType<? extends Mob> type = typeOf(e.getKey());
            if (type == null) continue;
            candidates.add(new Candidate(e.getKey(), cfg, type));
            totalWeight += s.weight;
        }
        if (candidates.isEmpty()) return;
        int roll = random.nextInt(totalWeight);
        Candidate chosen = candidates.get(0);
        for (Candidate c : candidates) {
            roll -= c.cfg.spawn.weight;
            if (roll < 0) {
                chosen = c;
                break;
            }
        }
        int group = chosen.cfg.spawn.minGroup + random.nextInt(Math.max(1, chosen.cfg.spawn.maxGroup - chosen.cfg.spawn.minGroup + 1));
        for (int i = 0; i < group; i++) {
            BlockPos p = i == 0 ? pos : findGround(level, pos.getX() + random.nextInt(5) - 2, pos.getY(), pos.getZ() + random.nextInt(5) - 2);
            if (p == null) continue;
            Mob mob = chosen.type.create(level, EntitySpawnReason.NATURAL);
            if (mob == null) continue;
            mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(mob)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.NATURAL, null);
            level.addFreshEntity(mob);
        }
    }

    private static BlockPos findGround(ServerLevel level, int x, int nearY, int z) {
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD || level.dimension() == net.minecraft.world.level.Level.END) {
            // en surface, ou dans une cavite proche de l'altitude du joueur
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(top - nearY) < 24) return valid(level, new BlockPos(x, top, z)) ? new BlockPos(x, top, z) : null;
        }
        for (int dy = 0; dy < 16; dy++) {
            for (int sign : new int[]{-1, 1}) {
                BlockPos p = new BlockPos(x, nearY + dy * sign, z);
                if (valid(level, p)) return p;
            }
        }
        return null;
    }

    private static boolean valid(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                && level.getFluidState(p).isEmpty();
    }

    public static boolean biomeMatches(Holder<Biome> biome, List<String> filters) {
        if (filters.isEmpty()) return true;
        for (String f : filters) {
            if (f.equals("*")) return true;
            if (f.startsWith("#")) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, Identifier.parse(f.substring(1)));
                if (biome.is(tag)) return true;
            } else if (biome.unwrapKey().map(k -> k.identifier().toString().equals(f)).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    public static Entity spawnForCommand(ServerLevel level, String key, BlockPos pos, int lvl) {
        EntityType<? extends Mob> type = typeOf(key);
        if (type == null) return null;
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) return null;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.COMMAND, null);
        if (lvl > 0) MobLevels.forceLevel(mob, key, lvl);
        level.addFreshEntity(mob);
        return mob;
    }
}
