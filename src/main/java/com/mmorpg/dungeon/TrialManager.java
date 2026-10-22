package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.TrialData.Trial;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.DecorBlocks;
import com.mmorpg.registry.ModItems;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Deroulement des epreuves de donjon : vagues de monstres, parcours de saut, enigme de sequence. Les herses sont de vrais blocs
 * (posees a la construction) : on les retire quand l'epreuve est reussie, et l'etat « reussi » est enregistre avec le monde.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class TrialManager {
    private static final class Wave {
        int wave;
        int total;
        final List<UUID> mobs = new ArrayList<>();
        long nextAt = -1;
        boolean spawned;
    }

    private static final class Simon {
        final List<Integer> seq = new ArrayList<>();
        int progress;
        boolean input;
        int generation;
    }

    private static final Map<String, Wave> WAVES = new HashMap<>();
    private static final Map<String, Simon> SIMON = new HashMap<>();
    private static final Map<String, Integer> ORDER = new HashMap<>();
    private static final Map<String, Set<Integer>> HITS = new HashMap<>();
    private static final Set<String> SEEN = new HashSet<>();

    private TrialManager() {
    }

    public static BlockState state(String id) {
        if (id.contains(":")) {
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
            return block.defaultBlockState();
        }
        return DecorBlocks.state(id);
    }

    // ================================================================== boucle serveur

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 5 != 0) return;
        TrialData data = TrialData.get(server);
        for (Trial t : data.all()) {
            ServerLevel level = server.getLevel(t.dim());
            if (level == null || level.players().isEmpty()) continue;
            switch (t.kind()) {
                case TrialData.WAVES -> tickWaves(level, data, t);
                case TrialData.JUMP -> tickJump(level, data, t);
                case TrialData.SIMON, TrialData.ORDER, TrialData.TARGETS -> announceTo(level, t);
                default -> {
                }
            }
        }
    }

    private static List<ServerPlayer> inside(ServerLevel level, Trial t) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && t.inBox(p.getX(), p.getY(), p.getZ())) out.add(p);
        }
        return out;
    }

    /** Annonce du titre et de la consigne a la premiere entree de chaque joueur. */
    private static void announceTo(ServerLevel level, Trial t) {
        for (ServerPlayer p : inside(level, t)) announce(p, t);
    }

    private static void announce(ServerPlayer p, Trial t) {
        if (t.title().isEmpty() || !SEEN.add(p.getUUID() + t.id())) return;
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.INFO, t.title(), t.done() ? "Épreuve déjà réussie" : t.hint(), t.done() ? 0x60FF80 : 0xFFC060));
    }

    // ================================================================== vagues

    private static void tickWaves(ServerLevel level, TrialData data, Trial t) {
        List<ServerPlayer> in = inside(level, t);
        for (ServerPlayer p : in) announce(p, t);
        if (t.done()) {
            WAVES.remove(t.id());
            return;
        }
        Wave w = WAVES.get(t.id());
        if (w == null) {
            if (in.isEmpty()) return;
            w = new Wave();
            w.total = 3 + (t.diff() >= 2 ? 1 : 0);
            WAVES.put(t.id(), w);
            spawnWave(level, t, w, 1);
            return;
        }
        // personne a proximite : les chunks peuvent etre decharges, on ne juge pas les monstres absents
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        if (level.getNearestPlayer(cx + 0.5, cy, cz + 0.5, 48, false) == null) return;
        w.mobs.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (!w.spawned || !w.mobs.isEmpty()) return;
        long now = level.getGameTime();
        if (w.wave >= w.total) {
            finish(level, data, t);
            WAVES.remove(t.id());
        } else if (w.nextAt < 0) {
            w.nextAt = now + 70;
            for (ServerPlayer p : in) p.sendOverlayMessage(Component.literal("Vague " + w.wave + " vaincue : reprenez votre souffle…").withColor(0xA0E0A0));
        } else if (now >= w.nextAt) {
            spawnWave(level, t, w, w.wave + 1);
        }
    }

    private static void spawnWave(ServerLevel level, Trial t, Wave w, int number) {
        w.wave = number;
        w.nextAt = -1;
        w.spawned = true;
        int extra = Math.max(0, inside(level, t).size() - 1) * 2;          // le solo reste la reference : on n'ajoute des ennemis que pour les groupes
        int count = Math.min(16, 3 + number * 2 + t.diff() * 2 + extra);
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        var random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 5 + random.nextDouble() * 5;
            BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(a) * r), cy, (int) Math.round(cz + Math.sin(a) * r));
            String key = t.mobs().get(i % t.mobs().size());
            int lvl = t.level() + (number == w.total ? 3 : 0) + t.diff();
            Entity e = MobSpawner.spawnForCommand(level, key, p, lvl);
            if (e != null) {
                w.mobs.add(e.getUUID());
                level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 14, 0.3, 0.6, 0.3, 0.4);
            }
        }
        level.playSound(null, new BlockPos(cx, cy, cz), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.6F);
        for (ServerPlayer p : inside(level, t)) {
            p.sendOverlayMessage(Component.literal("Vague " + number + " / " + w.total + " : " + count + " ennemis !").withColor(number == w.total ? 0xFF6050 : 0xFFC060));
        }
    }

    // ================================================================== parcours de saut

    private static void tickJump(ServerLevel level, TrialData data, Trial t) {
        int x0 = t.box().get(0), z0 = t.box().get(2), x1 = t.box().get(3), z1 = t.box().get(5);
        double fallY = t.box().get(1);
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.getAbilities().flying) continue;
            if (p.getX() >= x0 && p.getX() < x1 + 1 && p.getZ() >= z0 && p.getZ() < z1 + 1) {
                if (p.getY() >= fallY && p.getY() < t.box().get(4) + 1) announce(p, t);
                if (p.getY() < fallY && p.getY() > fallY - 22) {
                    p.teleportTo(level, t.pos().get(0) + 0.5, t.pos().get(1), t.pos().get(2) + 0.5, Set.of(), 0F, 5F, true);
                    p.resetFallDistance();
                    p.sendOverlayMessage(Component.literal("Vous avez chuté : retour au départ du parcours.").withColor(0xFFA060));
                    level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.4F);
                }
            }
            if (!t.done() && t.inGate(p.getX(), p.getY(), p.getZ())) complete(level, data, t, p, false);
        }
    }

    // ================================================================== enigme de sequence

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        handleClick(event.getLevel(), event.getPos(), event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        handleClick(event.getLevel(), event.getPos(), event.getEntity(), event);
    }

    private static void handleClick(net.minecraft.world.level.Level lv, BlockPos pos, net.minecraft.world.entity.player.Player player, net.neoforged.bus.api.Event ev) {
        if (!(lv instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return;
        TrialData data = TrialData.get(level.getServer());
        for (Trial t : data.all()) {
            boolean order = t.kind().equals(TrialData.ORDER) || t.kind().equals(TrialData.TARGETS);
            if ((!t.kind().equals(TrialData.SIMON) && !order) || !t.dim().equals(level.dimension())) continue;
            if (!order && t.pos().get(0) == pos.getX() && t.pos().get(1) == pos.getY() && t.pos().get(2) == pos.getZ()) {
                cancel(ev);
                if (t.done()) sp.sendOverlayMessage(Component.literal("Cette énigme est déjà résolue.").withColor(0xA0E0A0));
                else startSimon(level, t, sp);
                return;
            }
            for (int k = 0; k * 3 + 2 < t.pads().size(); k++) {
                if (t.pads().get(k * 3) == pos.getX() && t.pads().get(k * 3 + 1) == pos.getY() && t.pads().get(k * 3 + 2) == pos.getZ()) {
                    cancel(ev);
                    padPressed(level, data, t, sp, k);
                    return;
                }
            }
        }
    }

    private static void cancel(net.neoforged.bus.api.Event ev) {
        if (ev instanceof net.neoforged.bus.api.ICancellableEvent c) c.setCanceled(true);
    }

    private static int padCount(Trial t) {
        return t.pads().size() / 3;
    }

    private static BlockPos padPos(Trial t, int k) {
        return new BlockPos(t.pads().get(k * 3), t.pads().get(k * 3 + 1), t.pads().get(k * 3 + 2));
    }

    private static void light(ServerLevel level, Trial t, int k, boolean on) {
        level.setBlock(padPos(t, k), state(t.states().get(k * 2 + (on ? 1 : 0))), 3);
    }

    private static void note(ServerLevel level, Trial t, int k, float volume) {
        int[] semis = {0, 4, 7, 12, 16, 19};
        float pitch = (float) Math.pow(2.0, semis[k % semis.length] / 12.0) * 0.7F;
        level.playSound(null, padPos(t, k), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, volume, pitch);
    }

    private static void startSimon(ServerLevel level, Trial t, ServerPlayer p) {
        Simon s = SIMON.computeIfAbsent(t.id(), k -> new Simon());
        s.generation++;
        s.seq.clear();
        s.progress = 0;
        s.input = false;
        int len = 4 + t.diff();
        var random = level.getRandom();
        int pads = padCount(t);
        for (int i = 0; i < len; i++) s.seq.add(random.nextInt(pads));
        final int gen = s.generation;
        p.sendOverlayMessage(Component.literal("Observez la séquence…").withColor(0xFFE080));
        for (int k = 0; k < pads; k++) light(level, t, k, false);
        for (int i = 0; i < len; i++) {
            final int pad = s.seq.get(i);
            Scheduler.later(level, 30 + i * 16, () -> {
                if (s.generation != gen) return;
                light(level, t, pad, true);
                note(level, t, pad, 1.2F);
                level.sendParticles(ParticleTypes.END_ROD, padPos(t, pad).getX() + 0.5, padPos(t, pad).getY() + 1.4, padPos(t, pad).getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
            });
            Scheduler.later(level, 30 + i * 16 + 9, () -> {
                if (s.generation == gen) light(level, t, pad, false);
            });
        }
        Scheduler.later(level, 30 + len * 16, () -> {
            if (s.generation != gen) return;
            s.input = true;
            s.progress = 0;
            for (ServerPlayer q : level.players()) {
                if (q.distanceToSqr(p) < 40 * 40) q.sendOverlayMessage(Component.literal("À vous ! Touchez les dalles dans le même ordre (" + len + ").").withColor(0x80E0FF));
            }
        });
    }

    private static void targetHit(ServerLevel level, TrialData data, Trial t, ServerPlayer p, int k) {
        if (t.done()) return;
        Set<Integer> hit = HITS.computeIfAbsent(t.id(), x -> new HashSet<>());
        if (!hit.add(k)) return;
        light(level, t, k, true);
        note(level, t, k, 1.0F);
        level.sendParticles(ParticleTypes.CRIT, padPos(t, k).getX() + 0.5, padPos(t, k).getY() + 0.5, padPos(t, k).getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.2);
        if (p != null) p.sendOverlayMessage(Component.literal("Cible touchée : " + hit.size() + " / " + padCount(t)).withColor(0xFFE080));
        if (hit.size() >= padCount(t)) {
            HITS.remove(t.id());
            complete(level, data, t, p, true);
        }
    }

    /** Une fleche, un sort ou tout autre projectile qui touche une cible compte comme un coup. */
    @SubscribeEvent
    public static void onProjectile(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.BlockHitResult hit)) return;
        if (!(event.getProjectile().level() instanceof ServerLevel level)) return;
        TrialData data = TrialData.get(level.getServer());
        BlockPos pos = hit.getBlockPos();
        for (Trial t : data.all()) {
            if (!t.kind().equals(TrialData.TARGETS) || !t.dim().equals(level.dimension())) continue;
            for (int k = 0; k * 3 + 2 < t.pads().size(); k++) {
                if (t.pads().get(k * 3) == pos.getX() && t.pads().get(k * 3 + 1) == pos.getY() && t.pads().get(k * 3 + 2) == pos.getZ()) {
                    ServerPlayer shooter = event.getProjectile() instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() instanceof ServerPlayer sp ? sp : null;
                    targetHit(level, data, t, shooter, k);
                    return;
                }
            }
        }
    }

    /** Retire les monstres d'une vague en cours (remise a zero du donjon). */
    static void discardWaveMobs(ServerLevel level, Trial t) {
        Wave w = WAVES.get(t.id());
        if (w == null) return;
        for (UUID id : w.mobs) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
    }

    /** Oublie l'etat en cours d'une epreuve (remise a zero du donjon). */
    static void forget(String id) {
        WAVES.remove(id);
        SIMON.remove(id);
        ORDER.remove(id);
        HITS.remove(id);
    }

    /** Enigme d'ordre : la sequence est gravee dans le couloir d'entree, les dalles gardent leur lumiere tant que l'ordre est bon. */
    private static void orderPressed(ServerLevel level, TrialData data, Trial t, ServerPlayer p, int k) {
        if (t.done()) {
            p.sendOverlayMessage(Component.literal("Cette énigme est déjà résolue.").withColor(0xA0E0A0));
            return;
        }
        int progress = ORDER.getOrDefault(t.id(), 0);
        int expected = Integer.parseInt(t.mobs().get(progress));
        if (expected == k) {
            progress++;
            ORDER.put(t.id(), progress);
            note(level, t, k, 1.0F);
            light(level, t, k, true);
            if (progress >= t.mobs().size()) {
                ORDER.remove(t.id());
                complete(level, data, t, p, true);
            }
        } else {
            ORDER.put(t.id(), 0);
            for (int i = 0; i < padCount(t); i++) light(level, t, i, false);
            level.playSound(null, padPos(t, k), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.4F, 0.5F);
            p.sendOverlayMessage(Component.literal("Mauvais ordre ! Relisez les couleurs gravées dans le couloir d'entrée.").withColor(0xFF6050));
        }
    }

    private static void padPressed(ServerLevel level, TrialData data, Trial t, ServerPlayer p, int k) {
        if (t.kind().equals(TrialData.ORDER)) {
            orderPressed(level, data, t, p, k);
            return;
        }
        if (t.kind().equals(TrialData.TARGETS)) {
            targetHit(level, data, t, p, k);
            return;
        }
        Simon s = SIMON.get(t.id());
        if (t.done()) {
            p.sendOverlayMessage(Component.literal("Cette énigme est déjà résolue.").withColor(0xA0E0A0));
            return;
        }
        if (s == null || !s.input) {
            p.sendOverlayMessage(Component.literal("Activez d'abord le lutrin pour voir la séquence.").withColor(0xFFC060));
            return;
        }
        if (s.seq.get(s.progress) == k) {
            s.progress++;
            note(level, t, k, 1.0F);
            light(level, t, k, true);
            Scheduler.later(level, 8, () -> light(level, t, k, false));
            if (s.progress >= s.seq.size()) {
                s.input = false;
                complete(level, data, t, p, true);
            }
        } else {
            s.input = false;
            level.playSound(null, padPos(t, k), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.4F, 0.5F);
            level.sendParticles(ParticleTypes.SMOKE, padPos(t, k).getX() + 0.5, padPos(t, k).getY() + 1.2, padPos(t, k).getZ() + 0.5, 16, 0.3, 0.2, 0.3, 0.02);
            p.sendOverlayMessage(Component.literal("Mauvaise dalle ! Reprenez le lutrin pour revoir la séquence.").withColor(0xFF6050));
        }
    }

    /** Test automatique : la sequence en cours (indices de dalles), ou vide. */
    public static List<Integer> debugSequence(String id) {
        Simon s = SIMON.get(id);
        return s == null ? List.of() : List.copyOf(s.seq);
    }

    public static boolean debugInputReady(String id) {
        Simon s = SIMON.get(id);
        return s != null && s.input;
    }

    public static void debugStartSimon(ServerLevel level, String id, ServerPlayer p) {
        Trial t = TrialData.get(level.getServer()).byId(id);
        if (t != null) startSimon(level, t, p);
    }

    public static void debugPress(ServerLevel level, String id, ServerPlayer p, int pad) {
        TrialData data = TrialData.get(level.getServer());
        Trial t = data.byId(id);
        if (t != null) padPressed(level, data, t, p, pad);
    }

    // ================================================================== reussite

    private static void finish(ServerLevel level, TrialData data, Trial t) {
        ServerPlayer any = null;
        for (ServerPlayer p : inside(level, t)) any = p;
        complete(level, data, t, any, true);
    }

    private static void complete(ServerLevel level, TrialData data, Trial t, ServerPlayer p, boolean gate) {
        data.markDone(t.id());
        Trial done = data.byId(t.id());
        if (gate && done != null) openGate(level, done);
        BlockPos at = BlockPos.containing(t.pos().get(0), t.pos().get(1), t.pos().get(2));
        level.playSound(null, at, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 40, 1.5, 1.0, 1.5, 0.3);
        if (p != null) {
            for (ServerPlayer q : level.players()) {
                if (q.distanceToSqr(p) < 40 * 40) {
                    Net.toPlayer(q, new Payloads.Notify(Payloads.Notify.QUEST, "ÉPREUVE RÉUSSIE", t.title().isEmpty() ? "Bravo !" : t.title(), 0x60FF80));
                }
            }
        }
        var group = data.inGroup(TrialData.group(t.id()));
        long doneCount = group.stream().filter(Trial::done).count();
        if (p != null && !group.isEmpty()) {
            p.sendSystemMessage(Component.literal("Épreuves du donjon : " + doneCount + " / " + group.size()
                    + (doneCount == group.size() ? " — l'antre du boss est ouverte !" : "")).withColor(doneCount == group.size() ? 0x60FF80 : 0xFFE080));
        }
        if (t.kind().equals(TrialData.WAVES)) {
            ItemEntity coins = new ItemEntity(level, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, new ItemStack(ModItems.PIECE_OR.get(), 8 + t.level() / 4));
            level.addFreshEntity(coins);
        }
    }

    private static void openGate(ServerLevel level, Trial t) {
        if (t.gate().size() != 6 || t.gateBlock().isEmpty()) return;
        var block = state(t.gateBlock()).getBlock();
        int opened = 0;
        for (int x = t.gate().get(0); x <= t.gate().get(3); x++) {
            for (int y = t.gate().get(1); y <= t.gate().get(4); y++) {
                for (int z = t.gate().get(2); z <= t.gate().get(5); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(block)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        if (opened++ % 6 == 0) level.sendParticles(ParticleTypes.CLOUD, x + 0.5, y + 0.5, z + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
        }
        BlockPos mid = new BlockPos((t.gate().get(0) + t.gate().get(3)) / 2, t.gate().get(1) + 1, (t.gate().get(2) + t.gate().get(5)) / 2);
        level.playSound(null, mid, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.4F, 0.7F);
    }

    @SuppressWarnings("unused")
    private static SoundEvent unused() {
        return SoundEvents.ANVIL_BREAK;
    }
}
