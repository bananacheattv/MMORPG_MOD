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
        int maximum;
        final List<UUID> mobs = new ArrayList<>();
        long nextAt = -1;
        boolean spawned;
        /** Apres une apparition, on ne juge pas les monstres absents tout de suite : un troncon qui vient de se charger ne les retrouve pas encore. */
        long graceUntil;
    }

    private static final class Simon {
        final List<Integer> seq = new ArrayList<>();
        int progress;
        boolean input;
        int generation;
    }
    private static final Map<String,net.minecraft.server.level.ServerBossEvent> BARS=new HashMap<>();

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
                case TrialData.ELITE -> tickElite(level, data, t);
                case TrialData.AMBUSH -> tickAmbush(level, data, t);
                case TrialData.JUMP -> tickJump(level, data, t);
                case TrialData.SIMON, TrialData.ORDER, TrialData.TARGETS -> announceTo(level, t);
                default -> {
                }
            }
        }
        updateCounters(server,data);
    }
    private static void updateCounters(MinecraftServer server,TrialData data){
        Set<String> active=new HashSet<>();
        for(Trial trial:data.all()){
            Wave wave=WAVES.get(trial.id());ServerLevel level=server.getLevel(trial.dim());
            if(wave==null||trial.done()||level==null||!wave.spawned)continue;
            active.add(trial.id());wave.maximum=Math.max(wave.maximum,wave.mobs.size());
            var bar=BARS.computeIfAbsent(trial.id(),id->new net.minecraft.server.level.ServerBossEvent(java.util.UUID.randomUUID(),Component.empty(),net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS));
            int alive=wave.mobs.size();String label=trial.kind().equals(TrialData.AMBUSH)?"Embuscade":trial.kind().equals(TrialData.ELITE)?"Gardiens":"Vague "+wave.wave+" / "+wave.total;
            bar.setName(Component.literal(label+" · Ennemis restants : "+alive+(wave.nextAt>=0?" · Prochaine vague…":"")));
            bar.setProgress(wave.maximum==0?0:(float)alive/wave.maximum);
            var players=inside(level,trial);for(var previous:new ArrayList<>(bar.getPlayers()))if(!players.contains(previous))bar.removePlayer(previous);
            for(var player:players)bar.addPlayer(player);
        }
        BARS.entrySet().removeIf(entry->{if(active.contains(entry.getKey()))return false;entry.getValue().removeAllPlayers();return true;});
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){BARS.values().forEach(net.minecraft.server.level.ServerBossEvent::removeAllPlayers);BARS.clear();WAVES.clear();SIMON.clear();ORDER.clear();HITS.clear();SEEN.clear();JUMP_PROGRESS.clear();JUMP_LAST.clear();LAST_CLICK.clear();}

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
        if (t.title().isEmpty() || t.kind().equals(TrialData.WAVES) || !SEEN.add(p.getUUID() + t.id())) return;      // les vagues s'annoncent elles-memes
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
            w.total = waveCount(t);
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
        if (level.getGameTime() < w.graceUntil || !w.spawned || !w.mobs.isEmpty()) return;
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

    static int waveCount(Trial t) {
        return waveCount(t.diff());
    }

    /** Cinq vagues, six aux difficultes elevees. */
    static int waveCount(int diff) {
        return 5 + (diff >= 2 ? 1 : 0);
    }

    // ================================================================== gardien d'elite et embuscades

    private static void tickElite(ServerLevel level, TrialData data, Trial t) {
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
            w.total = 1;
            WAVES.put(t.id(), w);
            spawnElite(level, t, w, in.size());
            return;
        }
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        if (level.getNearestPlayer(cx + 0.5, cy, cz + 0.5, 48, false) == null || level.getGameTime() < w.graceUntil) return;
        w.mobs.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (w.spawned && w.mobs.isEmpty()) {
            finish(level, data, t);
            WAVES.remove(t.id());
        }
    }

    private static void spawnElite(ServerLevel level, Trial t, Wave w, int players) {
        w.spawned = true;
        w.graceUntil = level.getGameTime() + 40;
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        var random = level.getRandom();
        Entity boss = MobSpawner.spawnForCommand(level, t.mobs().get(0), new BlockPos(cx, cy, cz + 5), t.level() + 6 + t.diff() * 2);
        String name = "GARDIEN";
        if (boss != null) {
            w.mobs.add(boss.getUUID());
            name = boss.getName().getString().toUpperCase(java.util.Locale.ROOT);
            level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY() + 2, boss.getZ(), 60, 0.8, 1.6, 0.8, 0.6);
        }
        int escorts = Math.max(0,Math.min(18, 6 + t.diff() * 3 + Math.max(0, players - 1) * 3)-1);
        for (int i = 0; i < escorts; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 8 + random.nextDouble() * 4;
            BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(a) * r), cy, (int) Math.round(cz + Math.sin(a) * r));
            String key = t.mobs().get(1 + i % Math.max(1, t.mobs().size() - 1));
            Entity e = MobSpawner.spawnForCommand(level, key, p, t.level() + t.diff());
            if (e != null) {
                w.mobs.add(e.getUUID());
                level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 14, 0.3, 0.6, 0.3, 0.4);
            }
        }
        level.playSound(null, new BlockPos(cx, cy, cz), com.mmorpg.registry.ModSounds.GROWL.get(), SoundSource.HOSTILE, 1.6F, 0.8F);
        level.playSound(null, new BlockPos(cx, cy, cz), com.mmorpg.registry.ModSounds.HORN.get(), SoundSource.HOSTILE, 1.4F, 0.9F);
        for (ServerPlayer p : inside(level, t)) {
            Net.toPlayer(p, new Payloads.AreaEntry(name, "Gardien de l'étage — terrassez-le pour passer", Payloads.AreaEntry.BOSS_ROOM, 0xFF8A30, ""));
        }
    }

    private static void tickAmbush(ServerLevel level, TrialData data, Trial t) {
        if (t.done()) {
            WAVES.remove(t.id());
            return;
        }
        List<ServerPlayer> in = inside(level, t);
        Wave w = WAVES.get(t.id());
        if (w == null) {
            if (in.isEmpty()) return;
            w = new Wave();
            w.total = 1;
            WAVES.put(t.id(), w);
            spawnAmbush(level, t, w, in);
            return;
        }
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        if (level.getNearestPlayer(cx + 0.5, cy, cz + 0.5, 64, false) == null || level.getGameTime() < w.graceUntil) return;
        w.mobs.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (w.spawned && w.mobs.isEmpty()) {
            data.markDone(t.id());
            WAVES.remove(t.id());
            for (ServerPlayer p : in) p.sendOverlayMessage(Component.literal("Embuscade repoussée.").withColor(0xA0E0A0));
        }
    }

    /** Les monstres surgissent plus loin dans le couloir, sur le chemin du joueur. */
    private static void spawnAmbush(ServerLevel level, Trial t, Wave w, List<ServerPlayer> in) {
        w.spawned = true;
        w.graceUntil = level.getGameTime() + 40;
        int zMax = t.box().get(5) - 2;
        int ahead = (int) Math.min(zMax - 4, in.stream().mapToDouble(ServerPlayer::getZ).max().orElse(t.box().get(2)) + 9);
        int cy = t.pos().get(1);
        int cx = t.pos().get(0);
        var random = level.getRandom();
        int count = Math.min(10, 3 + t.diff() * 2 + Math.max(0, in.size() - 1) * 2);
        for (int i = 0; i < count; i++) {
            int z = Math.min(zMax, ahead + random.nextInt(Math.max(1, zMax - ahead + 1)));
            BlockPos p = new BlockPos(cx + random.nextInt(7) - 3, cy, z);
            Entity e = MobSpawner.spawnForCommand(level, t.mobs().get(i % t.mobs().size()), p, t.level() + t.diff());
            if (e != null) {
                w.mobs.add(e.getUUID());
                level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.6, 0.3, 0.4);
            }
        }
        level.playSound(null, new BlockPos(cx, cy, ahead), com.mmorpg.registry.ModSounds.GROWL.get(), SoundSource.HOSTILE, 1.0F, 1.1F);
        for (ServerPlayer p : in) {
            Net.toPlayer(p, new Payloads.AreaEntry("EMBUSCADE", count + " ennemis surgissent !", Payloads.AreaEntry.WAVE, 0xFF9030, ""));
        }
    }

    private static void spawnWave(ServerLevel level, Trial t, Wave w, int number) {
        w.wave = number;
        w.nextAt = -1;
        w.spawned = true;
        w.graceUntil = level.getGameTime() + 40;
        int extra = Math.max(0, inside(level, t).size() - 1) * 3;          // le solo reste la reference : on n'ajoute des ennemis que pour les groupes
        int count = Math.min(30, 6 + number * 3 + t.diff() * 3 + extra);
        int cx = t.pos().get(0), cy = t.pos().get(1), cz = t.pos().get(2);
        var random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 5 + random.nextDouble() * 7;
            BlockPos p = new BlockPos((int) Math.round(cx + Math.cos(a) * r), cy, (int) Math.round(cz + Math.sin(a) * r));
            String key = t.mobs().get(i % t.mobs().size());
            int lvl = t.level() + (number == w.total ? 3 : 0) + t.diff();
            Entity e = MobSpawner.spawnForCommand(level, key, p, lvl);
            if (e != null) {
                w.mobs.add(e.getUUID());
                level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 14, 0.3, 0.6, 0.3, 0.4);
            }
        }
        level.playSound(null, new BlockPos(cx, cy, cz), com.mmorpg.registry.ModSounds.HORN.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        level.playSound(null, new BlockPos(cx, cy, cz), com.mmorpg.registry.ModSounds.BOOM.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        boolean finale = number == w.total;
        for (ServerPlayer p : inside(level, t)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new Payloads.AreaEntry(finale ? "VAGUE FINALE" : "VAGUE " + number,
                    number == 1 ? count + " ennemis — survivez à " + w.total + " vagues" : count + " ennemis surgissent !", Payloads.AreaEntry.WAVE, finale ? 0xFF4A30 : 0xFFB040, number + "/" + w.total));
        }
    }

    // ================================================================== parcours de saut

    /** Longueur d'un jalon du parcours de saut (blocs). */
    private static final int JUMP_SPAN = 6;
    /** Jalon atteint par chaque joueur sur chaque parcours de saut : on ne peut pas franchir l'arrivee sans avoir parcouru le trajet. */
    private static final Map<String, Map<UUID, Integer>> JUMP_PROGRESS = new HashMap<>();

    private static void backToStart(ServerLevel level, Trial t, ServerPlayer p, String message) {
        p.teleportTo(level, t.pos().get(0) + 0.5, t.pos().get(1), t.pos().get(2) + 0.5, Set.of(), 0F, 5F, true);
        p.resetFallDistance();
        p.sendOverlayMessage(Component.literal(message).withColor(0xFFA060));
        level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.TELEPORT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        JUMP_PROGRESS.computeIfAbsent(t.id(), k -> new HashMap<>()).put(p.getUUID(), 0);
    }

    /** Derniere position (x, z) vue dans chaque parcours pour chaque joueur, pour detecter les deplacements instantanes. */
    private static final Map<String, Map<UUID, double[]>> JUMP_LAST = new HashMap<>();

    /** Parcours de saut non termine qui contient ce point (null sinon). */
    private static Trial jumpAt(ServerLevel level, double x, double y, double z) {
        for (Trial t : TrialData.get(level.getServer()).all()) {
            if (!t.kind().equals(TrialData.JUMP) || t.done() || !t.dim().equals(level.dimension())) continue;
            var b = t.box();
            if (x >= b.get(0) && x < b.get(3) + 1 && z >= b.get(2) && z < b.get(5) + 1 && y >= b.get(1) - 22 && y < b.get(4) + 1) return t;
        }
        return null;
    }

    /** Pas de perle ni de fruit de chorus dans (ou vers) un parcours de saut. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTeleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent event) {
        if (!(event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl)
                && !(event instanceof net.neoforged.neoforge.event.entity.EntityTeleportEvent.ItemConsumption)) return;
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (jumpAt(level, p.getX(), p.getY(), p.getZ()) == null && jumpAt(level, event.getTargetX(), event.getTargetY(), event.getTargetZ()) == null) return;
        event.setCanceled(true);
        p.sendOverlayMessage(Component.literal("Impossible de se téléporter ici : traversez à pied.").withColor(0xFFA060));
    }

    /** Pas de pont de fortune : on ne pose pas de bloc dans un parcours de saut. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlaceInJump(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.isCreative() && p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) return;
        BlockPos pos = event.getPos();
        if (jumpAt(level, pos.getX(), pos.getY(), pos.getZ()) == null) return;
        event.setCanceled(true);
        p.sendOverlayMessage(Component.literal("On ne construit pas dans cette épreuve.").withColor(0xFFA060));
    }

    private static void tickJump(ServerLevel level, TrialData data, Trial t) {
        int x0 = t.box().get(0), z0 = t.box().get(2), x1 = t.box().get(3), z1 = t.box().get(5);
        double fallY = t.box().get(1);
        int last = (z1 - z0) / JUMP_SPAN;
        Map<UUID, Integer> progress = JUMP_PROGRESS.computeIfAbsent(t.id(), k -> new HashMap<>());
        Map<UUID, double[]> seen = JUMP_LAST.computeIfAbsent(t.id(), k -> new HashMap<>());
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.getAbilities().flying) continue;
            int reached = progress.getOrDefault(p.getUUID(), 0);
            boolean inBox = p.getX() >= x0 && p.getX() < x1 + 1 && p.getZ() >= z0 && p.getZ() < z1 + 1;
            double[] before = inBox ? seen.put(p.getUUID(), new double[]{p.getX(), p.getZ()}) : seen.remove(p.getUUID());
            // deplacement impossible a pied en 0,25 s (perle, fruit de chorus, commande...) : on ne gagne pas de terrain ainsi
            if (!t.done() && inBox && before != null && Math.hypot(p.getX() - before[0], p.getZ() - before[1]) > 7) {
                backToStart(level, t, p, "Le parcours doit être traversé à pied : retour au départ.");
                continue;
            }
            if (p.getX() >= x0 && p.getX() < x1 + 1 && p.getZ() >= z0 && p.getZ() < z1 + 1) {
                if (p.getY() >= fallY && p.getY() < t.box().get(4) + 1) {
                    announce(p, t);
                    int idx = (int) ((p.getZ() - z0) / JUMP_SPAN);
                    if (!t.done() && idx > reached + 1) {                    // teleportation, bond demesure... : on ne saute pas une partie du parcours
                        backToStart(level, t, p, "Le parcours doit être traversé en entier : retour au départ.");
                        continue;
                    }
                    if (idx > reached) progress.put(p.getUUID(), idx);
                }
                if (p.getY() < fallY) {
                    backToStart(level, t, p, "Vous avez chuté : retour au départ du parcours.");
                    continue;
                }
            } else if (p.getZ() < z0 - 1) {
                progress.remove(p.getUUID());                                  // revenu en arriere : on repart de zero
            }
            if (!t.done() && t.inGate(p.getX(), p.getY(), p.getZ())) {
                if (progress.getOrDefault(p.getUUID(), 0) >= last - 2) {
                    progress.remove(p.getUUID());
                    complete(level, data, t, p, false);
                } else {
                    backToStart(level, t, p, "Le parcours doit être traversé en entier : retour au départ.");
                }
            }
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

    /** Dernier clic pris en compte pour chaque joueur (temps de jeu). */
    private static final Map<UUID, long[]> LAST_CLICK = new HashMap<>();
    /** Un clic droit arrive deux fois au serveur (main puis main secondaire), et un clic gauche peut s'y ajouter : on ignore tout clic qui suit le precedent de moins de 0,3 s. */
    private static final int CLICK_COOLDOWN_TICKS = 6;

    private static boolean freshClick(ServerLevel level, ServerPlayer sp, BlockPos pos) {
        long now = level.getGameTime();
        long key = pos.asLong();
        long[] last = LAST_CLICK.get(sp.getUUID());
        // seul un second clic sur le MEME bloc est ignore : deux dalles touchees coup sur coup comptent toutes les deux
        if (last != null && last[1] == key && now - last[0] < CLICK_COOLDOWN_TICKS && now >= last[0]) return false;
        LAST_CLICK.put(sp.getUUID(), new long[]{now, key});
        return true;
    }

    private static void handleClick(net.minecraft.world.level.Level lv, BlockPos pos, net.minecraft.world.entity.player.Player player, net.neoforged.bus.api.Event ev) {
        if (!(lv instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return;
        TrialData data = TrialData.get(level.getServer());
        for (Trial t : data.all()) {
            boolean order = t.kind().equals(TrialData.ORDER) || t.kind().equals(TrialData.TARGETS);
            if ((!t.kind().equals(TrialData.SIMON) && !order) || !t.dim().equals(level.dimension())) continue;
            if (!order && t.pos().get(0) == pos.getX() && t.pos().get(1) == pos.getY() && t.pos().get(2) == pos.getZ()) {
                cancel(ev);
                if (!freshClick(level, sp, pos)) return;
                if (t.done()) sp.sendOverlayMessage(Component.literal("Cette énigme est déjà résolue.").withColor(0xA0E0A0));
                else startSimon(level, t, sp);
                return;
            }
            for (int k = 0; k * 3 + 2 < t.pads().size(); k++) {
                if (t.pads().get(k * 3) == pos.getX() && t.pads().get(k * 3 + 1) == pos.getY() && t.pads().get(k * 3 + 2) == pos.getZ()) {
                    cancel(ev);
                    if (!freshClick(level, sp, pos)) return;
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
        // une dalle allumee eclaire la salle (bloc de lumiere au-dessus)
        BlockPos above = padPos(t, k).above();
        if (on) level.setBlock(above, Blocks.LIGHT.defaultBlockState(), 3);
        else if (level.getBlockState(above).is(Blocks.LIGHT)) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void note(ServerLevel level, Trial t, int k, float volume) {
        level.playSound(null, padPos(t, k), com.mmorpg.registry.ModSounds.NOTES[k % com.mmorpg.registry.ModSounds.NOTES.length].get(), SoundSource.BLOCKS, volume, 1.0F);
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
        JUMP_PROGRESS.remove(id);
        JUMP_LAST.remove(id);
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
            level.playSound(null, padPos(t, k), com.mmorpg.registry.ModSounds.ERROR.get(), SoundSource.BLOCKS, 1.4F, 0.5F);
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
            level.playSound(null, padPos(t, k), com.mmorpg.registry.ModSounds.ERROR.get(), SoundSource.BLOCKS, 1.4F, 0.5F);
            level.sendParticles(ParticleTypes.SMOKE, padPos(t, k).getX() + 0.5, padPos(t, k).getY() + 1.2, padPos(t, k).getZ() + 0.5, 16, 0.3, 0.2, 0.3, 0.02);
            p.sendOverlayMessage(Component.literal("Mauvaise dalle ! Reprenez le lutrin pour revoir la séquence.").withColor(0xFF6050));
        }
    }

    /** Test automatique : valide une epreuve comme si elle etait reussie. */
    public static void debugComplete(ServerLevel level, String id, ServerPlayer p) {
        TrialData data = TrialData.get(level.getServer());
        Trial t = data.byId(id);
        if (t != null) complete(level, data, t, p, true);
    }

    /** Test automatique : la sequence en cours (indices de dalles), ou vide. */
    public static List<Integer> debugSequence(String id) {
        Simon s = SIMON.get(id);
        return s == null ? List.of() : List.copyOf(s.seq);
    }

    public static String debugSimon(String id) {
        Simon s = SIMON.get(id);
        return s == null ? "aucune" : "progression " + s.progress + "/" + s.seq.size() + " saisie " + s.input + " generation " + s.generation;
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
        level.playSound(null, at, com.mmorpg.registry.ModSounds.SUCCESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 40, 1.5, 1.0, 1.5, 0.3);
        if (p != null) {
            for (ServerPlayer q : level.players()) {
                if (q.distanceToSqr(p) < 40 * 40) {
                    Net.toPlayer(q, new Payloads.Notify(Payloads.Notify.QUEST, "ÉPREUVE RÉUSSIE", t.title().isEmpty() ? "Bravo !" : t.title(), 0x60FF80));
                }
            }
        }
        var group = data.inGroup(TrialData.group(t.id())).stream().filter(x -> !x.kind().equals(TrialData.AMBUSH)).toList();
        long doneCount = group.stream().filter(Trial::done).count();
        if (!group.isEmpty() && doneCount == group.size()) ArenaEvents.dropKey(level, TrialData.group(t.id()), t.pos().get(1));
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
        level.playSound(null, mid, com.mmorpg.registry.ModSounds.DOOR.get(), SoundSource.BLOCKS, 1.4F, 0.7F);
    }
}
