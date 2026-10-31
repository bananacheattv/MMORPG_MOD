package eldoria.launcher;

import java.io.DataInputStream;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Presence Discord du launcher : meme application que le jeu (« Joue a Aeloria », temps ecoule), detail « Dans le launcher ».
 * Elle se coupe au lancement du jeu (le mod prend le relais avec classe et zone, sans doublon) et revient a sa fermeture.
 * Sans dependance : dialogue direct avec le socket IPC de Discord, sur un fil daemon ; silencieuse si Discord est absent.
 */
final class Presence {
    static final String DEFAULT_APP_ID = "1565874848167755776";
    private static final long RETRY_MS = 10_000L, POLL_MS = 1_000L;

    private final String clientId;
    private final long start = System.currentTimeMillis() / 1000L;
    private volatile boolean active = true;
    private volatile boolean stopped;

    private final Consumer<String> log;
    private String lastLog = "";

    Presence(String clientId, Consumer<String> log) {
        this.log = log == null ? x -> { } : log;
        this.clientId = clientId == null || clientId.isBlank() ? DEFAULT_APP_ID : clientId.trim();
    }

    private void say(String m) {
        if (m.equals(lastLog)) return; // pas de repetition toutes les 30 s
        lastLog = m;
        log.accept("[Discord] " + m);
    }

    void start() {
        Thread t = new Thread(this::run, "Aeloria-LauncherRPC");
        t.setDaemon(true);
        t.start();
    }

    /** false = le jeu tourne : on retire notre statut pour laisser celui du mod. */
    void setActive(boolean on) {
        active = on;
    }

    void stop() {
        stopped = true;
    }

    private void run() {
        try {
            say("Presence demarree (application " + clientId + ", systeme " + System.getProperty("os.name") + ", Java " + System.getProperty("java.version") + ")");
            loop();
        } catch (Throwable t) {
            say("Presence arretee par une erreur : " + t);
        }
    }

    private void loop() {
        AutoCloseable conn = null;
        OutputStream out = null;
        InputStream in = null;
        while (!stopped) {
            try {
                if (!active) {
                    close(conn);
                    conn = null;
                    Thread.sleep(POLL_MS);
                    continue;
                }
                if (conn == null) {
                    Object[] c = connect();
                    if (c == null) {
                        say("Discord introuvable (aucun discord-ipc-0..9). Discord doit etre lance avant le launcher ; nouvel essai toutes les 10 s.");
                        Thread.sleep(RETRY_MS);
                        continue;
                    }
                    conn = (AutoCloseable) c[0];
                    in = (InputStream) c[1];
                    out = (OutputStream) c[2];
                    say("Connecte a Discord (" + c[3] + "), application " + clientId);
                    say("Handshake envoye, attente de la reponse de Discord...");
                    write(out, 0, "{\"v\":1,\"client_id\":\"" + clientId + "\"}");
                    say("Handshake : " + read(in));
                    say("Envoi du statut, attente de la reponse...");
                    write(out, 1, activity());
                    say("Statut envoye : " + read(in));
                }
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                say("Erreur Discord : " + e);
                close(conn);
                conn = null;
                try {
                    Thread.sleep(RETRY_MS);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        close(conn);
    }

    String activity() {
        return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                + ",\"activity\":{\"details\":\"Dans le launcher\",\"timestamps\":{\"start\":" + start + "}}},\"nonce\":\""
                + UUID.randomUUID() + "\"}";
    }

    private static void close(AutoCloseable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    private static Object[] connect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        for (int i = 0; i < 10; i++) {
            try {
                if (windows) {
                    String name = "\\\\.\\pipe\\discord-ipc-" + i;
                    RandomAccessFile f; // pas de exists() : peu fiable sur les pipes nommes Windows
                    try {
                        f = new RandomAccessFile(name, "rw");
                    } catch (IOException e) {
                        continue;
                    }
                    return new Object[]{f, Channels.newInputStream(f.getChannel()), Channels.newOutputStream(f.getChannel()), name};
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
                    return new Object[]{ch, Channels.newInputStream(ch), Channels.newOutputStream(ch), p.toString()};
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static List<String> unixDirs() {
        List<String> bases = new ArrayList<>();
        for (String v : new String[]{"XDG_RUNTIME_DIR", "TMPDIR"}) {
            String s = System.getenv(v);
            if (s != null && !s.isEmpty()) bases.add(s);
        }
        bases.add("/tmp");
        List<String> dirs = new ArrayList<>();
        for (String b : bases) {
            dirs.add(b);
            dirs.add(b + "/snap.discord");
            dirs.add(b + "/app/com.discordapp.Discord");
        }
        return dirs;
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
