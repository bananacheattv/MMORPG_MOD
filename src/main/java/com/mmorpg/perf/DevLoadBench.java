package com.mmorpg.perf;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.RpgPlayers;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Banc de charge serveur (dev uniquement, {@code -PloadBench=joueurs:monstres_par_joueur}) : de faux joueurs (connexion simulee, sans client)
 * sont entoures de monstres, puis la duree de chaque tick est mesuree. Ligne {@code [LOAD BENCH]} dans le journal.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevLoadBench {
    private static final String PROP = System.getProperty("mmorpg.loadBench", "");
    private static final int WARMUP = 300, MEASURE = 1200;
    private static long t0;
    private static long[] samples = new long[MEASURE];
    private static int n;
    private static boolean done;
    private static jdk.jfr.Recording jfr;
    private static int idle, setupAt = -1;
    private static final List<ServerPlayer> BOTS = new ArrayList<>();

    private DevLoadBench() {
    }

    @SubscribeEvent
    public static void pre(ServerTickEvent.Pre e) {
        t0 = System.nanoTime();
    }

    @SubscribeEvent
    public static void post(ServerTickEvent.Post e) {
        if (PROP.isEmpty() || done) return;
        long dt = System.nanoTime() - t0;
        var server = e.getServer();
        int tick = server.getTickCount();
        String[] a = PROP.split(":");
        int players = Integer.parseInt(a[0]), mobs = a.length > 1 ? Integer.parseInt(a[1]) : 30;
        int far = a.length > 2 ? Integer.parseInt(a[2]) : 0;
        if (setupAt < 0) {
            // la premiere fois, le monde construit ses mines et ses plaines (des minutes) : on attend la fin pour ne mesurer que le jeu
            idle = com.mmorpg.mine.MineManager.building() || com.mmorpg.plains.PlainsManager.building() ? 0 : idle + 1;
            if (idle >= 200) {
                setupAt = tick;
                setup(server, players, mobs, far);
            }
            return;
        }
        if (tick > setupAt + WARMUP && n < MEASURE) samples[n++] = dt;
        if (n == MEASURE) {
            done = true;
            long[] s = Arrays.copyOf(samples, n);
            Arrays.sort(s);
            double avg = Arrays.stream(s).average().orElse(0) / 1e6;
            MMORPG.LOGGER.info("[LOAD BENCH] joueurs={} monstres/joueur={} entites={} : moy {} ms, p50 {} ms, p95 {} ms, p99 {} ms, max {} ms",
                    BOTS.size(), mobs, count(server), f(avg), f(s[n / 2] / 1e6), f(s[(int) (n * 0.95)] / 1e6), f(s[(int) (n * 0.99)] / 1e6), f(s[n - 1] / 1e6));
            Perf.sections().forEach((k, v) -> MMORPG.LOGGER.info("[LOAD BENCH] section {} : moy {} ms, max {} ms, {} appels", k, f(v.avgMs), f(v.maxMs), v.calls.get()));
            if (jfr != null) {
                try {
                    jfr.stop();
                    jfr.dump(java.nio.file.Path.of(System.getProperty("mmorpg.loadBenchJfr")));
                } catch (Exception ex) {
                    MMORPG.LOGGER.warn("[LOAD BENCH] JFR non ecrit : {}", ex.toString());
                }
            }
            server.halt(false);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addPlayer(net.minecraft.server.MinecraftServer server, ServerPlayer bot) {
        try {
            var list = server.getPlayerList();
            for (var fld : net.minecraft.server.players.PlayerList.class.getDeclaredFields()) {
                if (fld.getType() == List.class && fld.getGenericType().getTypeName().contains("ServerPlayer")) {
                    fld.setAccessible(true);
                    ((List<ServerPlayer>) fld.get(list)).add(bot);
                    return;
                }
            }
            throw new IllegalStateException("liste des joueurs introuvable");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String f(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static int count(net.minecraft.server.MinecraftServer server) {
        int c = 0;
        for (var l : server.getAllLevels()) for (var ignored : l.getAllEntities()) c++;
        return c;
    }

    private static void setup(net.minecraft.server.MinecraftServer server, int players, int mobs, int far) {
        if (System.getProperty("mmorpg.loadBenchJfr") != null) {
            try {
                jfr = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
                jfr.start();
            } catch (Exception ex) {
                MMORPG.LOGGER.warn("[LOAD BENCH] JFR indisponible : {}", ex.toString());
            }
        }
        com.mmorpg.server.FeatureFlags.set(server, "apparition_mobs", false);       // effectif constant pendant la mesure
        var level = server.overworld();
        var random = new Random(42);
        var keys = new ArrayList<String>();
        ConfigManager.mobs().forEach((k, v) -> {
            if (!v.boss && v.spawn.enabled) keys.add(k);
        });
        MMORPG.LOGGER.info("[LOAD BENCH] preparation : {} bots, {} monstres chacun, {} types de monstres", players, mobs, keys.size());
        for (int i = 0; i < players; i++) {
            var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "Bot" + i);
            var bot = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
            var conn = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(conn);
            var cookie = CommonListenerCookie.createInitial(profile, false);
            // faux joueur : aucun paquet n'est envoye (pas de client), mais il est dans la liste des joueurs et dans le monde comme un vrai
            new net.minecraft.server.network.ServerGamePacketListenerImpl(server, conn, bot, cookie) {
                @Override
                public void send(net.minecraft.network.protocol.Packet<?> packet) {
                }
            };
            addPlayer(server, bot);
            bot.setPos(900.5 + (i % 5) * 200, 80, 900.5 + (i / 5) * 200);
            level.addNewPlayer(bot);
            bot.setGameMode(GameType.SURVIVAL);
            var d = RpgPlayers.get(bot);
            if (!d.playerClass.isPlayable()) d.playerClass = PlayerClass.GUERRIER;
            d.level = 40;
            BOTS.add(bot);
            int cx = 900 + (i % 5) * 200, cz = 900 + (i / 5) * 200;     // zones d'ecart : les monstres des bots ne se melangent pas
            level.getChunkAt(new BlockPos(cx, 64, cz));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            bot.teleportTo(level, cx + 0.5, y, cz + 0.5, java.util.Set.of(), 0, 0, true);
            bot.setHealth(bot.getMaxHealth());
            for (int m = 0; m < mobs + far && !keys.isEmpty(); m++) {
                double ang = random.nextDouble() * Math.PI * 2, dist = m < mobs ? 5 + random.nextDouble() * 14 : 45 + random.nextDouble() * 12;
                int x = (int) (cx + Math.cos(ang) * dist), z = (int) (cz + Math.sin(ang) * dist);
                level.getChunkAt(new BlockPos(x, 64, z));
                int yy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                MobSpawner.spawnForCommand(level, keys.get(random.nextInt(keys.size())), new BlockPos(x, yy, z), 20 + random.nextInt(20));
            }
        }
    }
}
