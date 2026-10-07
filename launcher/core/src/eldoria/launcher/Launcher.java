package eldoria.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Coeur du launcher Eldoria (mis a jour automatiquement a chaque push) :
 * synchronise les mods de l'instance, installe NeoForge, cree le profil dans le launcher Minecraft officiel
 * puis l'ouvre (la connexion Microsoft reste geree par Mojang).
 */
public final class Launcher {
    private static final String PROFILE_ID = "eldoria-mmorpg";

    private final Path home;
    private final Path mods;
    private final Path state;
    private final Path mcDir;
    private final int bootVersion;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15)).build();

    private JFrame frame;
    private JLabel status;
    private JProgressBar bar;
    private JTextArea log;
    private JButton play;
    private Map<String, Object> manifest;

    private Launcher(Path home, int bootVersion) {
        this.home = home;
        this.mods = home.resolve("mods");
        this.state = home.resolve("launcher");
        this.mcDir = minecraftDir();
        this.bootVersion = bootVersion;
    }

    /** Appele par l'amorce. {@code manifestJson} vaut null hors ligne. */
    public static void start(String manifestJson, Path home, int bootVersion) {
        Launcher l = new Launcher(home, bootVersion);
        if (java.awt.GraphicsEnvironment.isHeadless()) { // mode console (serveur, tests)
            l.update(manifestJson);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            l.buildUi();
            Thread t = new Thread(() -> l.update(manifestJson), "eldoria-update");
            t.setDaemon(true);
            t.start();
        });
    }

    static Path minecraftDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String user = System.getProperty("user.home");
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            return Path.of(appdata != null ? appdata : user, ".minecraft");
        }
        if (os.contains("mac")) return Path.of(user, "Library", "Application Support", "minecraft");
        return Path.of(user, ".minecraft");
    }

    private void buildUi() {
        frame = new JFrame("Eldoria MMORPG - Launcher");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.setBackground(new Color(0x1b1622));

        JLabel title = new JLabel("ELDORIA MMORPG");
        title.setFont(new Font(Font.SERIF, Font.BOLD, 30));
        title.setForeground(new Color(0xf0c860));
        root.add(title, BorderLayout.NORTH);

        log = new JTextArea();
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBackground(new Color(0x120e18));
        log.setForeground(new Color(0xd8d0e0));
        JScrollPane sp = new JScrollPane(log);
        sp.setPreferredSize(new Dimension(620, 300));
        root.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        status = new JLabel("Recherche de mises a jour...");
        status.setForeground(Color.WHITE);
        bar = new JProgressBar(0, 1000);
        bar.setIndeterminate(true);
        play = new JButton("JOUER");
        play.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        play.setEnabled(false);
        play.addActionListener(e -> launch());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btns.setOpaque(false);
        btns.add(play);
        south.add(status, BorderLayout.NORTH);
        south.add(bar, BorderLayout.CENTER);
        south.add(btns, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void log(String s) {
        System.out.println(s);
        if (log == null) return;
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    private void status(String s, int permille) {
        if (status == null) return;
        SwingUtilities.invokeLater(() -> {
            status.setText(s);
            bar.setIndeterminate(permille < 0);
            if (permille >= 0) bar.setValue(permille);
        });
    }

    private void update(String manifestJson) {
        try {
            Files.createDirectories(mods);
            Files.createDirectories(state);
            if (manifestJson == null) {
                Path cached = state.resolve("manifest.json");
                if (!Files.exists(cached)) throw new IllegalStateException("Aucune connexion et aucun manifeste en cache.");
                log("Hors ligne : utilisation de la derniere version telechargee.");
                manifestJson = Files.readString(cached);
            }
            manifest = Json.obj(Json.parse(manifestJson));
            log("Version du modpack : " + Json.str(manifest.get("version"), "?") + "  (" + Json.str(manifest.get("date"), "") + ")");
            for (Object n : Json.arr(manifest.get("notes"))) log("  - " + n);

            int minBoot = (int) Double.parseDouble(Json.str(manifest.get("bootstrapVersion"), "1"));
            if (minBoot > bootVersion)
                log("! Une nouvelle version du launcher est disponible : " + Json.str(manifest.get("releaseUrl"), ""));

            syncMods();
            ensureNeoForge();
            writeProfiles();
            status("Pret ! Clique sur JOUER.", 1000);
            log("Pret. Le profil \"Eldoria MMORPG\" est installe dans le launcher Minecraft.");
            if (play != null) SwingUtilities.invokeLater(() -> play.setEnabled(true));
        } catch (Exception e) {
            e.printStackTrace();
            log("ERREUR : " + e);
            status("Erreur - voir le journal", 0);
            if (frame != null) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Erreur pendant la mise a jour :\n" + e,
                    "Eldoria", JOptionPane.ERROR_MESSAGE));
        }
    }

    // ---------------------------------------------------------------- mods

    private void syncMods() throws Exception {
        List<Object> list = Json.arr(manifest.get("mods"));
        Set<String> wanted = new LinkedHashSet<>();
        int k = 0;
        for (Object o : list) {
            Map<String, Object> mod = Json.obj(o);
            String file = Json.str(mod.get("file"), null);
            String url = Json.str(mod.get("url"), null);
            String sha = Json.str(mod.get("sha256"), "");
            if (file == null || url == null || file.contains("/") || file.contains("\\")) continue;
            wanted.add(file);
            Path target = mods.resolve(file);
            status("Verification de " + file, k * 1000 / Math.max(1, list.size()));
            if (Files.exists(target) && sha.equalsIgnoreCase(sha256(target))) {
                log("A jour : " + file);
            } else {
                log("Telechargement : " + file);
                download(url, target, sha);
            }
            k++;
        }
        // supprime les anciens fichiers geres par le launcher (les mods ajoutes a la main sont conserves)
        Path managed = state.resolve("managed-mods.txt");
        if (Files.exists(managed)) {
            for (String old : Files.readAllLines(managed)) {
                if (!old.isBlank() && !wanted.contains(old) && !old.contains("/") && !old.contains("\\")) {
                    if (Files.deleteIfExists(mods.resolve(old))) log("Supprime (obsolete) : " + old);
                }
            }
        }
        Files.write(managed, wanted);
    }

    private void download(String url, Path target, String sha) throws Exception {
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        HttpResponse<InputStream> r = http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode() + " pour " + url);
        long total = r.headers().firstValueAsLong("content-length").orElse(-1);
        try (InputStream in = r.body(); var out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1 << 16];
            long done = 0;
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                done += n;
                if (total > 0) status("Telechargement de " + target.getFileName() + " (" + done / 1024 + " Ko)", (int) (done * 1000 / total));
            }
        }
        if (!sha.isEmpty() && !sha.equalsIgnoreCase(sha256(tmp))) {
            Files.deleteIfExists(tmp);
            throw new IllegalStateException("Fichier corrompu (somme de controle) : " + target.getFileName());
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    static String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    // ---------------------------------------------------------------- NeoForge

    private String versionId() {
        return "neoforge-" + Json.str(manifest.get("neoforge"), "");
    }

    private void ensureNeoForge() throws Exception {
        String neo = Json.str(manifest.get("neoforge"), null);
        if (neo == null) throw new IllegalStateException("Version NeoForge absente du manifeste");
        Path vjson = mcDir.resolve("versions").resolve(versionId()).resolve(versionId() + ".json");
        if (Files.exists(vjson)) {
            log("NeoForge " + neo + " deja installe.");
            return;
        }
        Files.createDirectories(mcDir);
        Path profiles = mcDir.resolve("launcher_profiles.json");
        if (!Files.exists(profiles)) Files.writeString(profiles, "{\n  \"profiles\" : {}\n}");

        String url = Json.str(manifest.get("neoforgeInstaller"),
                "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + neo + "/neoforge-" + neo + "-installer.jar");
        Path inst = state.resolve("neoforge-" + neo + "-installer.jar");
        log("Telechargement de l'installeur NeoForge " + neo + "...");
        download(url, inst, "");
        status("Installation de NeoForge (peut prendre quelques minutes)...", -1);
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        Process p = new ProcessBuilder(java, "-jar", inst.toString(), "--install-client", mcDir.toString())
                .directory(state.toFile()).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) log("  [neoforge] " + line);
        }
        int code = p.waitFor();
        if (code != 0 || !Files.exists(vjson)) throw new IllegalStateException("Echec de l'installation de NeoForge (code " + code + ")");
        log("NeoForge installe.");
    }

    // ---------------------------------------------------------------- profil du launcher officiel

    private void writeProfiles() throws Exception {
        for (String name : new String[]{"launcher_profiles.json", "launcher_profiles_microsoft_store.json"}) {
            Path f = mcDir.resolve(name);
            if (!Files.exists(f)) continue;
            Map<String, Object> root;
            try {
                root = Json.obj(Json.parse(Files.readString(f)));
            } catch (Exception e) {
                log("Fichier " + name + " illisible, ignore : " + e.getMessage());
                continue;
            }
            Map<String, Object> profiles = Json.obj(root.get("profiles"));
            root.put("profiles", profiles);
            Map<String, Object> p = Json.obj(profiles.get(PROFILE_ID));
            if (p.isEmpty()) p = new LinkedHashMap<>();
            String now = Instant.now().toString();
            p.put("name", "Eldoria MMORPG");
            p.put("type", "custom");
            p.put("lastVersionId", versionId());
            p.put("gameDir", home.toAbsolutePath().toString());
            p.put("icon", "Enchanting_Table");
            p.putIfAbsent("created", now);
            p.put("lastUsed", now);
            p.putIfAbsent("javaArgs", Json.str(manifest.get("javaArgs"), "-Xmx4G -XX:+UseG1GC"));
            profiles.put(PROFILE_ID, p);
            Files.writeString(f, Json.write(root));
            log("Profil ajoute dans " + name);
        }
    }

    // ---------------------------------------------------------------- lancement

    private void launch() {
        play.setEnabled(false);
        List<List<String>> cmds = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            for (String env : new String[]{"ProgramFiles(x86)", "ProgramFiles", "LOCALAPPDATA"}) {
                String base = System.getenv(env);
                if (base == null) continue;
                Path exe = Path.of(base, "Minecraft Launcher", "MinecraftLauncher.exe");
                if (Files.exists(exe)) cmds.add(List.of(exe.toString()));
                Path exe2 = Path.of(base, "Programs", "Minecraft Launcher", "MinecraftLauncher.exe");
                if (Files.exists(exe2)) cmds.add(List.of(exe2.toString()));
            }
            Path xbox = Path.of("C:\\XboxGames\\Minecraft Launcher\\Content\\Minecraft.exe");
            if (Files.exists(xbox)) cmds.add(List.of(xbox.toString()));
            cmds.add(List.of("explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft"));
        } else if (os.contains("mac")) {
            cmds.add(List.of("open", "-a", "Minecraft"));
        } else {
            cmds.add(List.of("minecraft-launcher"));
        }
        for (List<String> c : cmds) {
            try {
                new ProcessBuilder(c).start();
                log("Ouverture du launcher Minecraft : selectionne le profil \"Eldoria MMORPG\" puis Jouer.");
                status("Launcher Minecraft ouvert.", 1000);
                new Thread(() -> {
                    try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                    System.exit(0);
                }).start();
                return;
            } catch (Exception e) {
                log("Echec : " + c.get(0) + " (" + e.getMessage() + ")");
            }
        }
        JOptionPane.showMessageDialog(frame, "Launcher Minecraft introuvable.\nOuvre-le manuellement et choisis le profil \"Eldoria MMORPG\".",
                "Eldoria", JOptionPane.INFORMATION_MESSAGE);
        play.setEnabled(true);
    }
}
