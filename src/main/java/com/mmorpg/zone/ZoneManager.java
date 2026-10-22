package com.mmorpg.zone;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Application des zones : regles (pas de construction, de PvP ni de monstres hostiles), annonce d'entree et de sortie,
 * et selection de la baguette de zone (deux coins par joueur, contour affiche en particules).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ZoneManager {
    /** Selection en cours d'un operateur : deux coins et la dimension. */
    public static final class Selection {
        public ResourceKey<Level> dimension;
        public @Nullable BlockPos a;
        public @Nullable BlockPos b;
        boolean nextIsB;

        public boolean complete() {
            return a != null && b != null;
        }
    }

    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
    private static final Map<UUID, String> CURRENT = new HashMap<>();

    private ZoneManager() {
    }

    // ================================================================== selection

    public static Selection selection(Player p) {
        return SELECTIONS.computeIfAbsent(p.getUUID(), k -> new Selection());
    }

    public static void pick(ServerPlayer p, BlockPos pos) {
        Selection s = selection(p);
        if (!p.level().dimension().equals(s.dimension)) {
            s.a = null;
            s.b = null;
            s.nextIsB = false;
        }
        s.dimension = p.level().dimension();
        if (s.nextIsB) s.b = pos.immutable();
        else s.a = pos.immutable();
        s.nextIsB = !s.nextIsB;
        String placed = s.nextIsB ? "A" : "B";
        p.sendOverlayMessage(Component.literal("Coin " + placed + " : " + pos.getX() + ", " + pos.getZ()
                + (s.complete() ? "  -  /zone creer <nom> pour valider" : "  -  choisissez le coin " + (s.nextIsB ? "B" : "A"))).withColor(0xA0E0FF));
    }

    public static void clearSelection(Player p) {
        SELECTIONS.remove(p.getUUID());
    }

    // ================================================================== regles

    public static ZoneData.@Nullable Zone zoneAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return null;
        return ZoneData.get(sl.getServer()).at(sl.dimension(), pos);
    }

    private static boolean builder(Entity e) {
        return e instanceof ServerPlayer p && p.isCreative() && p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if (builder(p)) return;
        ZoneData.Zone z = zoneAt(p.level(), event.getPos());
        if (z == null || !z.noBuild()) return;
        event.setCanceled(true);
        warn(p, z);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity e = event.getEntity();
        if (builder(e) || !(event.getLevel() instanceof Level level)) return;
        ZoneData.Zone z = zoneAt(level, event.getPos());
        if (z == null || !z.noBuild()) return;
        event.setCanceled(true);
        if (e instanceof Player p) warn(p, z);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity src = event.getSource().getEntity();
        if (src instanceof Projectile pr && pr.getOwner() != null) src = pr.getOwner();
        if (!(src instanceof Player attacker) || attacker == victim) return;
        ZoneData.Zone z = zoneAt(victim.level(), victim.blockPosition());
        if (z != null && z.noPvp()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (event.getSpawnType() == EntitySpawnReason.COMMAND || event.getSpawnType() == EntitySpawnReason.SPAWN_ITEM_USE) return;
        if (!(mob instanceof Enemy) && !(mob instanceof com.mmorpg.entity.RpgMob)) return;
        ZoneData.Zone z = zoneAt(mob.level(), mob.blockPosition());
        if (z != null && z.noMobs()) event.setSpawnCancelled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            ZoneData.Zone z = zoneAt(event.getLevel(), pos);
            return z != null && z.noBuild();
        });
    }

    private static void warn(Player p, ZoneData.Zone z) {
        if (p instanceof ServerPlayer sp && sp.tickCount % 10 == 0) {
            sp.sendOverlayMessage(Component.literal(z.name() + " est protégée : on ne peut pas y construire ni y casser de blocs.").withColor(0xFFC060));
        }
    }

    // ================================================================== annonces et contour

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        ServerLevel level = p.level();
        ZoneData.Zone z = ZoneData.get(level.getServer()).at(level.dimension(), p.blockPosition());
        String now = z == null ? "" : z.id();
        String before = CURRENT.getOrDefault(p.getUUID(), "");
        if (!now.equals(before)) {
            CURRENT.put(p.getUUID(), now);
            if (z != null) announce(p, z);
        }
        if (p.isHolding(com.mmorpg.registry.ModItems.BAGUETTE_ZONE.get())) outline(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        CURRENT.remove(id);
        SELECTIONS.remove(id);
    }

    public static String describe(ZoneData.Zone z) {
        StringBuilder sb = new StringBuilder();
        if (z.noPvp()) sb.append("PvP interdit");
        if (z.noBuild()) sb.append(sb.length() > 0 ? " · " : "").append("Construction interdite");
        if (z.noMobs()) sb.append(sb.length() > 0 ? " · " : "").append("Sans monstres hostiles");
        return sb.length() == 0 ? "Zone libre" : sb.toString();
    }

    /** Animation d'entree cote client (filet dore ou salle de boss), avec un son et quelques particules. */
    private static void announce(ServerPlayer p, ZoneData.Zone z) {
        String subtitle = z.subtitle().isEmpty() ? describe(z) : z.subtitle();
        String lore = "";
        boolean boss = z.boss();
        boolean dungeon = z.dungeon();
        if (dungeon) {                                       // « texte d'ambiance|ligne de niveau »
            int bar = subtitle.indexOf('|');
            lore = bar < 0 ? "" : subtitle.substring(0, bar);
            subtitle = bar < 0 ? subtitle : subtitle.substring(bar + 1);
        }
        int style = boss ? com.mmorpg.network.Payloads.AreaEntry.BOSS_ROOM : dungeon ? com.mmorpg.network.Payloads.AreaEntry.DUNGEON : com.mmorpg.network.Payloads.AreaEntry.ZONE;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                new com.mmorpg.network.Payloads.AreaEntry(z.name(), subtitle, style, boss ? 0xFF5A3C : dungeon ? 0xFFC868 : 0xFFD878, lore));
        ServerLevel level = p.level();
        if (dungeon) {
            level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.BELL.get(), net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.0F);
            level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.RISE.get(), net.minecraft.sounds.SoundSource.AMBIENT, 0.5F, 1.0F);
            level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1.5, p.getZ(), 40, 4, 1.5, 4, 0.01);
        } else if (boss) {
            level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.GROWL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.0F);
            level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.HEARTBEAT.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 1.0F);
            level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, 4, 1.2, 4, 0.01);
        } else {
            level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.CHIME.get(), net.minecraft.sounds.SoundSource.AMBIENT, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 14, 1.2, 0.8, 1.2, 0.02);
        }
    }

    /** Contour de la selection (et des zones proches) en particules visibles de l'operateur qui tient la baguette. */
    private static void outline(ServerPlayer p) {
        ServerLevel level = p.level();
        Selection s = SELECTIONS.get(p.getUUID());
        if (s != null && s.a != null && level.dimension().equals(s.dimension)) {
            BlockPos b = s.b == null ? s.a : s.b;
            box(p, level, Math.min(s.a.getX(), b.getX()), Math.min(s.a.getZ(), b.getZ()),
                    Math.max(s.a.getX(), b.getX()) + 1, Math.max(s.a.getZ(), b.getZ()) + 1, true);
        }
        for (ZoneData.Zone z : ZoneData.get(level.getServer()).all()) {
            if (!z.dimension().equals(level.dimension())) continue;
            if (p.getX() < z.minX() - 40 || p.getX() > z.maxX() + 40 || p.getZ() < z.minZ() - 40 || p.getZ() > z.maxZ() + 40) continue;
            box(p, level, z.minX(), z.minZ(), z.maxX() + 1, z.maxZ() + 1, false);
        }
    }

    private static void box(ServerPlayer p, ServerLevel level, int x0, int z0, int x1, int z1, boolean selection) {
        var particle = selection ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER;
        double y = p.getY() + 0.6;
        int step = Math.max(1, Math.max(x1 - x0, z1 - z0) / 48);
        for (int x = x0; x <= x1; x += step) {
            edge(p, level, particle, x, y, z0);
            edge(p, level, particle, x, y, z1);
        }
        for (int z = z0; z <= z1; z += step) {
            edge(p, level, particle, x0, y, z);
            edge(p, level, particle, x1, y, z);
        }
        if (selection) {
            for (int[] c : new int[][]{{x0, z0}, {x0, z1}, {x1, z0}, {x1, z1}}) {
                for (int dy = -2; dy <= 3; dy++) edge(p, level, particle, c[0], y + dy, c[1]);
            }
        }
    }

    private static void edge(ServerPlayer p, ServerLevel level, net.minecraft.core.particles.SimpleParticleType particle, double x, double y, double z) {
        level.sendParticles(p, particle, false, false, x, y, z, 1, 0, 0, 0, 0);
    }
}
