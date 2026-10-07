package eldoria.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Coeur du launcher Eldoria (mis a jour automatiquement a chaque push) : synchronise les mods,
 * installe Minecraft + NeoForge, connecte le compte (Microsoft ou hors ligne) et lance le jeu directement.
 */
public final class Launcher {
    private final Path home;
    private final Path gameDir;
    private final Path mods;
    private final Path state;
    private final int bootVersion;

    private JFrame frame;
    private JLabel status;
    private JLabel accountLabel;
    private JButton loginBtn;
    private JTextField pseudo;
    private JComboBox<String> ram;
    private JProgressBar bar;
    private JTextArea log;
    private JButton play;

    private Map<String, Object> manifest;
    private Map<String, Object> settings = new LinkedHashMap<>();
    private Game game;
    private MsAuth auth;
    private volatile MsAuth.Account msAccount;
    private volatile boolean ready;

    private Launcher(Path home, int bootVersion) {
        this.home = home;
        this.gameDir = home.resolve("instance");
        this.mods = gameDir.resolve("mods");
        this.state = home.resolve("launcher");
        this.bootVersion = bootVersion;
    }

    /** Appele par l'amorce. {@code manifestJson} vaut null hors ligne. */
    public static void start(String manifestJson, Path home, int bootVersion) {
        Launcher l = new Launcher(home, bootVersion);
        l.loadSettings();
        if (java.awt.GraphicsEnvironment.isHeadless()) { // mode console (tests) : prepare puis lance hors ligne
            l.update(manifestJson);
            if (l.ready && Boolean.getBoolean("eldoria.launch")) l.play();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            l.buildUi();
            Thread t = new Thread(() -> l.update(manifestJson), "eldoria-update");
            t.setDaemon(true);
            t.start();
        });
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        frame = new JFrame("Eldoria MMORPG - Launcher");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.setBackground(new Color(0x1b1622));

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.setOpaque(false);
        JLabel title = new JLabel("ELDORIA MMORPG");
        title.setFont(new Font(Font.SERIF, Font.BOLD, 30));
        title.setForeground(new Color(0xf0c860));
        north.add(title, BorderLayout.NORTH);

        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acc.setOpaque(false);
        accountLabel = label("Compte : hors ligne");
        loginBtn = new JButton("Connexion Microsoft");
        loginBtn.setEnabled(false);
        loginBtn.addActionListener(e -> toggleLogin());
        pseudo = new JTextField(Json.str(settings.get("pseudo"), ""), 12);
        pseudo.setToolTipText("Pseudo utilise sans compte Microsoft (solo / serveur hors ligne)");
        ram = new JComboBox<>(new String[]{"2", "3", "4", "6", "8", "12", "16"});
        ram.setSelectedItem(Json.str(settings.get("ramGo"), "4"));
        acc.add(accountLabel);
        acc.add(loginBtn);
        acc.add(label("  Pseudo hors ligne :"));
        acc.add(pseudo);
        acc.add(label("  RAM (Go) :"));
        acc.add(ram);
        north.add(acc, BorderLayout.SOUTH);
        root.add(north, BorderLayout.NORTH);

        log = new JTextArea();
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBackground(new Color(0x120e18));
        log.setForeground(new Color(0xd8d0e0));
        JScrollPane sp = new JScrollPane(log);
        sp.setPreferredSize(new Dimension(760, 320));
        root.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        status = label("Recherche de mises a jour...");
        bar = new JProgressBar(0, 1000);
        bar.setIndeterminate(true);
        play = new JButton("JOUER");
        play.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        play.setEnabled(false);
        play.addActionListener(e -> new Thread(this::play, "eldoria-play").start());
        south.add(status, BorderLayout.NORTH);
        south.add(bar, BorderLayout.CENTER);
        south.add(play, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Color.WHITE);
        return l;
    }

    private void log(String s) {
        System.out.println(s);
        if (log == null) return;
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            if (log.getLineCount() > 3000) log.setText(log.getText().substring(log.getText().length() / 2));
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

    private void ui(Runnable r) { if (frame != null) SwingUtilities.invokeLater(r); }

    private void error(String what, Exception e) {
        e.printStackTrace();
        log("ERREUR : " + e.getMessage());
        status(what + " - voir le journal", 0);
        ui(() -> JOptionPane.showMessageDialog(frame, what + " :\n" + e.getMessage(), "Eldoria", JOptionPane.ERROR_MESSAGE));
    }

    // ---------------------------------------------------------------- reglages

    private void loadSettings() {
        try {
            Path f = state.resolve("settings.json");
            if (Files.exists(f)) settings = Json.obj(Json.parse(Files.readString(f)));
        } catch (Exception ignored) {}
    }

    private void saveSettings() {
        try {
            Files.createDirectories(state);
            Files.writeString(state.resolve("settings.json"), Json.write(settings));
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- mise a jour

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
                log("! Nouvelle version du launcher disponible : " + Json.str(manifest.get("releaseUrl"), ""));

            setupAuth();
            syncMods();
            game = new Game(home.resolve("minecraft"), gameDir, this::log, this::status);
            game.prepare(Json.str(manifest.get("minecraft"), "26.3"), Json.str(manifest.get("neoforge"), ""));
            ready = true;
            status("Pret ! Clique sur JOUER.", 1000);
            log("Pret.");
            ui(() -> play.setEnabled(true));
        } catch (Exception e) {
            error("Erreur pendant la mise a jour", e);
        }
    }

    private void syncMods() throws Exception {
        List<Object> list = Json.arr(manifest.get("mods"));
        Set<String> wanted = new LinkedHashSet<>();
        int k = 0;
        for (Object o : list) {
            Map<String, Object> mod = Json.obj(o);
            String file = Json.str(mod.get("file"), null);
            String url = Json.str(mod.get("url"), null);
            if (file == null || url == null || file.contains("/") || file.contains("\\")) continue;
            String algo = mod.containsKey("sha512") ? "SHA-512" : mod.containsKey("sha1") ? "SHA-1" : "SHA-256";
            String sha = Json.str(mod.containsKey("sha512") ? mod.get("sha512") : mod.containsKey("sha1") ? mod.get("sha1") : mod.get("sha256"), "");
            wanted.add(file);
            Path target = mods.resolve(file);
            status("Verification de " + file, k++ * 1000 / Math.max(1, list.size()));
            if (Net.valid(target, algo, sha, 0)) {
                log("A jour : " + file);
            } else {
                log("Telechargement : " + file);
                Net.download(url, target, algo, sha, done -> status("Telechargement de " + file + " (" + done / 1024 + " Ko)", -1));
            }
        }
        // supprime les anciens fichiers geres par le launcher (les mods ajoutes a la main sont conserves)
        Path managed = state.resolve("managed-mods.txt");
        if (Files.exists(managed)) {
            for (String old : Files.readAllLines(managed)) {
                if (!old.isBlank() && !wanted.contains(old) && !old.contains("/") && !old.contains("\\")
                        && Files.deleteIfExists(mods.resolve(old))) log("Supprime (obsolete) : " + old);
            }
        }
        Files.write(managed, wanted);
    }

    // ---------------------------------------------------------------- compte

    private void setupAuth() {
        String clientId = Json.str(manifest.get("msaClientId"), "");
        if (clientId.isBlank()) {
            log("Connexion Microsoft non configuree : mode hors ligne (pseudo).");
            return;
        }
        auth = new MsAuth(clientId, state.resolve("account.json"));
        ui(() -> loginBtn.setEnabled(true));
        msAccount = auth.refresh();
        refreshAccountUi();
        if (msAccount != null) log("Connecte en tant que " + msAccount.name());
    }

    private void refreshAccountUi() {
        ui(() -> {
            accountLabel.setText(msAccount != null ? "Compte : " + msAccount.name() + " (Microsoft)" : "Compte : hors ligne");
            loginBtn.setText(msAccount != null ? "Deconnexion" : "Connexion Microsoft");
        });
    }

    private void toggleLogin() {
        if (auth == null) return;
        if (msAccount != null) {
            try { auth.logout(); } catch (Exception ignored) {}
            msAccount = null;
            refreshAccountUi();
            return;
        }
        loginBtn.setEnabled(false);
        JDialog[] dlg = new JDialog[1];
        new Thread(() -> {
            try {
                msAccount = auth.login((url, code) -> ui(() -> {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
                    try { Desktop.getDesktop().browse(URI.create(url)); } catch (Exception ignored) {}
                    JOptionPane pane = new JOptionPane("Va sur " + url + "\net entre le code (copie dans le presse-papier) :\n\n" + code,
                            JOptionPane.INFORMATION_MESSAGE);
                    dlg[0] = pane.createDialog(frame, "Connexion Microsoft");
                    dlg[0].setModal(false);
                    dlg[0].setVisible(true);
                }), this::log);
                log("Connecte en tant que " + msAccount.name());
            } catch (Exception e) {
                error("Connexion impossible", e);
            } finally {
                ui(() -> {
                    if (dlg[0] != null) dlg[0].dispose();
                    loginBtn.setEnabled(true);
                });
                refreshAccountUi();
            }
        }, "eldoria-login").start();
    }

    // ---------------------------------------------------------------- jeu

    private void play() {
        if (!ready) return;
        MsAuth.Account acc = msAccount;
        if (acc == null) {
            String name = pseudo != null ? pseudo.getText().trim() : System.getProperty("eldoria.pseudo", "Joueur");
            if (!name.matches("[A-Za-z0-9_]{3,16}")) {
                ui(() -> JOptionPane.showMessageDialog(frame, "Connecte-toi avec Microsoft ou entre un pseudo (3-16 lettres, chiffres ou _).",
                        "Eldoria", JOptionPane.WARNING_MESSAGE));
                return;
            }
            acc = MsAuth.Account.offline(name);
            settings.put("pseudo", name);
        }
        String go = ram != null ? (String) ram.getSelectedItem() : "4";
        settings.put("ramGo", go);
        saveSettings();
        ui(() -> play.setEnabled(false));
        try {
            List<String> jvm = new ArrayList<>(List.of("-Xmx" + go + "G"));
            Process p = game.launch(acc, jvm);
            status("Minecraft est lance.", 1000);
            ui(() -> frame.setState(JFrame.ICONIFIED));
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(Files.newBufferedWriter(state.resolve("latest.log")))) {
                for (String line; (line = r.readLine()) != null; ) {
                    out.println(line);
                    log(line);
                }
            }
            int code = p.waitFor();
            log("Minecraft ferme (code " + code + ").");
            status(code == 0 ? "Pret ! Clique sur JOUER." : "Minecraft s'est arrete avec une erreur (code " + code + ")", 1000);
            ui(() -> frame.setState(JFrame.NORMAL));
        } catch (Exception e) {
            error("Lancement impossible", e);
        } finally {
            ui(() -> play.setEnabled(true));
        }
    }
}
