package com.mmorpg.client;

import com.google.gson.JsonObject;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Discord Rich Presence (statut « Joue à Aeloria », classe, niveau, zone), sans dependance : dialogue direct avec le
 * socket IPC local de Discord. Ne fait rien si l'ID d'application est vide, si l'option est coupee ou si Discord est absent.
 * Tout le reseau se fait sur un fil daemon ; le jeu ne l'attend jamais.
 */
public final class DiscordPresence {
    private static final long RETRY_MS = 60_000L;
    private static final long UPDATE_MS = 15_000L;
    private static final int OP_HANDSHAKE = 0, OP_FRAME = 1;

    private static volatile String details = "", state = "";
    private static volatile boolean inGame;
    private static volatile String clientId = "";
    private static volatile Thread worker;
    private static String zone = "";
    private static int tickCounter;
    private static final long START_SECONDS = System.currentTimeMillis() / 1000L;

    private DiscordPresence() {
    }

    /** Memorise la derniere zone annoncee par le serveur (appele a l'animation d'entree). */
    public static void onArea(Payloads.AreaEntry p) {
        if (p.style() == Payloads.AreaEntry.ZONE || p.style() == Payloads.AreaEntry.DUNGEON) {
            zone = p.title();
        }
    }

    public static void onLogout() {
        zone = "";
    }

    /** Appele a chaque tick client ; ne travaille qu'une fois par seconde. */
    public static void tick(Minecraft mc) {
        if (++tickCounter < 20) return;
        tickCounter = 0;
        String id;
        try {
            if (!ClientConfig.DISCORD_ENABLED.get()) return;
            id = ClientConfig.DISCORD_APP_ID.get().trim();
        } catch (RuntimeException e) {
            return;
        }
        if (id.isEmpty()) return;
        clientId = id;
        if (mc.level == null || mc.player == null) {
            inGame = false;
            details = "Dans les menus";
            state = "";
        } else {
            inGame = true;
            var d = ClientData.DATA;
            details = d.playerClass.label + " niv. " + d.level;
            state = zone.isEmpty() ? "Explore Aeloria" : zone;
        }
        if (worker == null) {
            Thread t = new Thread(DiscordPresence::run, "Aeloria-DiscordRPC");
            t.setDaemon(true);
            worker = t;
            t.start();
        }
    }

    private static void run() {
        String lastSent = null;
        String connectedId = null;
        InputStream in = null;
        OutputStream out = null;
        AutoCloseable conn = null;
        while (true) {
            try {
                if (conn == null) {
                    String id = clientId;
                    if (id.isEmpty()) {
                        Thread.sleep(2000);
                        continue;
                    }
                    Object[] c = connect();
                    if (c == null) {
                        Thread.sleep(RETRY_MS);
                        continue;
                    }
                    conn = (AutoCloseable) c[0];
                    in = (InputStream) c[1];
                    out = (OutputStream) c[2];
                    JsonObject hs = new JsonObject();
                    hs.addProperty("v", 1);
                    hs.addProperty("client_id", id);
                    write(out, OP_HANDSHAKE, hs.toString());
                    read(in);
                    connectedId = id;
                    lastSent = null;
                }
                if (!connectedId.equals(clientId)) throw new IOException("ID change");
                String key = inGame + "|" + details + "|" + state;
                if (!key.equals(lastSent)) {
                    write(out, OP_FRAME, activity().toString());
                    read(in);
                    lastSent = key;
                }
                Thread.sleep(UPDATE_MS);
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception ignored) {
                    }
                }
                conn = null;
                try {
                    Thread.sleep(RETRY_MS);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    static JsonObject activity() {
        JsonObject act = new JsonObject();
        act.addProperty("details", details);
        if (!state.isEmpty()) act.addProperty("state", state);
        JsonObject ts = new JsonObject();
        ts.addProperty("start", START_SECONDS);
        act.add("timestamps", ts);
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", act);
        JsonObject msg = new JsonObject();
        msg.addProperty("cmd", "SET_ACTIVITY");
        msg.add("args", args);
        msg.addProperty("nonce", UUID.randomUUID().toString());
        return msg;
    }

    /** Retourne {connexion, entree, sortie} ou null si Discord n'est pas lance. */
    private static Object[] connect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
        for (int i = 0; i < 10; i++) {
            try {
                if (windows) {
                    RandomAccessFile f;
                    try {
                        f = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"); // pas de exists() : peu fiable sur les pipes Windows
                    } catch (IOException e) {
                        continue;
                    }
                    return new Object[]{f, Channels.newInputStream(f.getChannel()), Channels.newOutputStream(f.getChannel())};
                }
                for (String dir : unixDirs()) {
                    Path p = Path.of(dir, "discord-ipc-" + i);
                    if (!Files.exists(p)) continue;
                    SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                    try {
                        ch.connect(UnixDomainSocketAddress.of(p));
                    } catch (IOException e) {
                        ch.close();
                        continue;
                    }
                    return new Object[]{ch, Channels.newInputStream(ch), Channels.newOutputStream(ch)};
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static String[] unixDirs() {
        String xdg = System.getenv("XDG_RUNTIME_DIR");
        String tmp = System.getenv("TMPDIR");
        java.util.List<String> bases = new java.util.ArrayList<>();
        if (xdg != null && !xdg.isEmpty()) bases.add(xdg);
        if (tmp != null && !tmp.isEmpty()) bases.add(tmp);
        bases.add("/tmp");
        java.util.List<String> dirs = new java.util.ArrayList<>();
        for (String b : bases) {
            dirs.add(b);
            dirs.add(b + "/snap.discord");
            dirs.add(b + "/app/com.discordapp.Discord");
        }
        return dirs.toArray(new String[0]);
    }

    private static void write(OutputStream out, int op, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op).putInt(body.length).put(body);
        out.write(buf.array());
        out.flush();
    }

    private static String read(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        byte[] head = new byte[8];
        din.readFully(head);
        ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
        hb.getInt();
        int len = hb.getInt();
        if (len < 0 || len > 1 << 20) throw new IOException("trame invalide");
        byte[] body = new byte[len];
        din.readFully(body);
        return new String(body, StandardCharsets.UTF_8);
    }
}
