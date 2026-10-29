package eldoria.launcher;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Coeur du launcher Aeloria (mis a jour automatiquement a chaque push) : synchronise les mods,
 * installe Minecraft + NeoForge, connecte le compte (Microsoft ou hors ligne) et lance le jeu directement.
 * Interface plate (barre laterale, pages Accueil / News / Changelog / Compte / Reglages) aux couleurs du jeu : voir {@link Theme}.
 */
public final class Launcher {
    private static final String WIKI_URL = "https://github.com/bananacheattv/MMORPG_MOD";

    private final Path home;
    private final Path gameDir;
    private final Path mods;
    private final Path state;
    private final int bootVersion;

    private JFrame frame;
    private JLabel status;
    private JLabel accountLabel;
    private JLabel sideName;
    private Theme.FlatButton loginBtn;
    private Theme.FlatButton sideLogin;
    private JTextField pseudo;
    private JComboBox<String> ram;
    private Theme.PlayButton play;
    private JTextArea log;
    private JComponent homeStats;
    private JPanel pages;
    private JPanel newsBox;
    private JPanel changeBox;
    private final List<Theme.NavItem> nav = new ArrayList<>();
    private volatile BufferedImage head;
    private JComponent avatarView;

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

    /**
     * Apercu de l'interface sans rien telecharger ni lancer : {@code java -cp launcher-core.jar eldoria.launcher.Launcher <dossier> <page>}
     * ecrit une capture PNG de chaque page (accueil, news, changelog, compte, reglages) puis quitte.
     */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "apercu");
        Files.createDirectories(out);
        Launcher l = new Launcher(out.resolve("home"), 1);
        SwingUtilities.invokeAndWait(() -> {
            l.buildUi();
            l.manifest = new LinkedHashMap<>();
            l.manifest.put("version", "1.8.2");
            l.manifest.put("date", "2026-10-23");
            l.manifest.put("notes", List.of("Donjons de 20 minutes : 6 epreuves, 2 parcours de saut et 4 embuscades",
                    "Le boss se reveille tout seul a l'entree de son arene", "Nouvelle interface plate dans tout le jeu", "Bestiaire verrouille jusqu'a la premiere victoire"));
            l.manifest.put("mods", List.of(Map.of("file", "aeloria-0.0.0.jar"), Map.of("file", "worldedit-neoforge.jar"), Map.of("file", "geckolib.jar")));
            l.fillNews();
            l.refreshHomeStats();
            l.frame.setLocation(40, 40);
        });
        String[] names = {"home", "news", "changelog", "account", "settings"};
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            SwingUtilities.invokeAndWait(() -> {
                l.show(names[k]);
                if (k == 0) l.play.loading("CHARGEMENT... 25%", 250);
                if (k == 1) l.play.ready();
            });
            Thread.sleep(400);
            BufferedImage img = new BufferedImage(l.frame.getWidth(), l.frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(() -> l.frame.paint(img.getGraphics()));
            javax.imageio.ImageIO.write(img, "png", out.resolve("launcher_" + names[i] + ".png").toFile());
        }
        System.exit(0);
    }

    // ---------------------------------------------------------------- UI

    private static JLabel text(String s, float size, int style, Color c) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(style, size));
        l.setForeground(c);
        return l;
    }

    private void buildUi() {
        frame = new JFrame("Aeloria MMORPG - Launcher");
        frame.setUndecorated(true);
        if (Theme.EMBLEM_IMG != null) frame.setIconImage(Theme.scaled(Theme.EMBLEM_IMG, "icon", 64, 64));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMaximizedBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        root.add(topBar(), BorderLayout.NORTH);
        root.add(sideBar(), BorderLayout.WEST);

        pages = new JPanel(new CardLayout());
        pages.setOpaque(false);
        pages.add(homePage(), "home");
        pages.add(listPage("ACTUALITES", newsBox = new JPanel()), "news");
        pages.add(listPage("CHANGELOG", changeBox = new JPanel()), "changelog");
        pages.add(accountPage(), "account");
        pages.add(settingsPage(), "settings");
        JPanel center = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                g.setPaint(new GradientPaint(0, 0, new Color(0x0C0A13), 0, getHeight(), new Color(0x08060D)));
                g.fillRect(0, 0, getWidth(), getHeight());
                // filigrane : grand emblème tres pale
                Graphics2D w = (Graphics2D) g.create();
                w.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.07f));
                Theme.emblem(w, getWidth() * 0.80, getHeight() * 0.98, getHeight() * 0.95);
                w.dispose();
                g.dispose();
            }
        };
        center.add(pages, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setSize(1100, 690);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        show("home");
        play.loading("VERIFICATION DES MISES A JOUR...", -1);
    }

    private void show(String page) {
        ((CardLayout) pages.getLayout()).show(pages, page);
        String[] order = {"home", "news", "changelog", "account"};
        for (int i = 0; i < nav.size() && i < order.length; i++) nav.get(i).setActive(order[i].equals(page));
        pages.repaint();
    }

    /** Barre du haut : nom du jeu, reglages, reduire / agrandir / fermer ; elle sert aussi a deplacer la fenetre. */
    private JComponent topBar() {
        JPanel bar = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                g.setPaint(Theme.BAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(Theme.LINE);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
                Theme.emblem(g, 22, 16, 26);
                Theme.wordmark(g, 40, 22, 15f);
                g.setFont(Theme.font(Font.PLAIN, 11f));
                g.setPaint(Theme.MUTED);
                g.drawString("LAUNCHER", 40 + Theme.wordmarkWidth(g, 15f) + 10, 21);
                g.dispose();
            }

            @Override
            public void doLayout() {
                int w = getWidth();
                int x = w - 46;
                for (int i = getComponentCount() - 1; i >= 0; i--) {
                    getComponent(i).setBounds(x, 0, 46, 32);
                    x -= 46;
                }
            }
        };
        bar.setPreferredSize(new Dimension(100, 33));
        int[] drag = new int[2];
        MouseAdapter move = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { drag[0] = e.getXOnScreen() - frame.getX(); drag[1] = e.getYOnScreen() - frame.getY(); }
            @Override public void mouseDragged(MouseEvent e) {
                if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) frame.setLocation(e.getXOnScreen() - drag[0], e.getYOnScreen() - drag[1]);
            }
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) toggleMaximize(); }
        };
        bar.addMouseListener(move);
        bar.addMouseMotionListener(move);
        // de droite a gauche : fermer, agrandir, reduire, reglages
        bar.add(windowButton(0, () -> System.exit(0)));
        bar.add(windowButton(1, this::toggleMaximize));
        bar.add(windowButton(2, () -> frame.setState(JFrame.ICONIFIED)));
        bar.add(windowButton(3, () -> show("settings")));
        return bar;
    }

    private void toggleMaximize() {
        boolean max = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        frame.setExtendedState(max ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
    }

    private JComponent windowButton(int kind, Runnable action) {
        return new JComponent() {
            private boolean hover;

            {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { action.run(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                int w = getWidth(), h = getHeight();
                if (hover) {
                    g.setPaint(kind == 0 ? new Color(0xC42B1C) : Theme.alpha(Color.WHITE, 22));
                    g.fillRect(0, 0, w, h);
                }
                g.setPaint(hover && kind == 0 ? Color.WHITE : Theme.TEXT);
                g.setStroke(new BasicStroke(1.3f));
                int cx = w / 2, cy = h / 2;
                switch (kind) {
                    case 0 -> { g.draw(new Line2D.Double(cx - 5, cy - 5, cx + 5, cy + 5)); g.draw(new Line2D.Double(cx - 5, cy + 5, cx + 5, cy - 5)); }
                    case 1 -> g.drawRect(cx - 5, cy - 5, 10, 10);
                    case 2 -> g.draw(new Line2D.Double(cx - 5, cy + 4, cx + 5, cy + 4));
                    default -> Theme.icon(g, Theme.Icon.GEAR, cx - 8, cy - 8, 16, hover ? Theme.GOLD_LIGHT : Theme.MUTED);
                }
                g.dispose();
            }
        };
    }

    private JComponent sideBar() {
        JPanel side = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                g.setPaint(Theme.SIDE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(Theme.LINE);
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
                g.dispose();
            }
        };
        side.setPreferredSize(new Dimension(170, 100));
        JPanel items = new JPanel();
        items.setOpaque(false);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        nav.clear();
        addNav(items, "Accueil", Theme.Icon.HOME, "home");
        addNav(items, "News", Theme.Icon.NEWS, "news");
        addNav(items, "Changelog", Theme.Icon.CHANGELOG, "changelog");
        addNav(items, "Compte", Theme.Icon.ACCOUNT, "account");
        items.add(new Theme.NavItem("Wiki", Theme.Icon.WIKI, true, () -> open(WIKI_URL)));
        side.add(items, BorderLayout.NORTH);

        // pied : deconnexion / connexion et joueur
        JPanel foot = new JPanel();
        foot.setOpaque(false);
        foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
        foot.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        sideLogin = new Theme.FlatButton("SE CONNECTER").onClick(this::toggleLogin);
        sideLogin.setMaximumSize(new Dimension(142, 30));
        sideLogin.setPreferredSize(new Dimension(142, 30));
        sideLogin.setAlignmentX(0f);
        foot.add(sideLogin);
        foot.add(Box.createVerticalStrut(14));
        JPanel who = new JPanel(new BorderLayout(8, 0));
        who.setOpaque(false);
        avatarView = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                Theme.avatar(g, head, 1, 1, 38);
                g.dispose();
            }
        };
        avatarView.setPreferredSize(new Dimension(40, 40));
        who.add(avatarView, BorderLayout.EAST);
        JPanel names = new JPanel(new GridBagLayout());
        names.setOpaque(false);
        sideName = text("Hors ligne", 13f, Font.BOLD, Theme.TEXT);
        JLabel sub = text("Pas de compte Microsoft", 10.5f, Font.PLAIN, Theme.MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        names.add(sideName, c);
        c.gridy = 1;
        names.add(sub, c);
        who.add(names, BorderLayout.CENTER);
        who.setAlignmentX(0f);
        who.setMaximumSize(new Dimension(142, 42));
        foot.add(who);
        side.add(foot, BorderLayout.SOUTH);
        return side;
    }

    private void addNav(JPanel items, String label, Theme.Icon icon, String page) {
        Theme.NavItem item = new Theme.NavItem(label, icon, false, () -> show(page));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        nav.add(item);
        items.add(item);
    }

    private JComponent homePage() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);

        JComponent emblem = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                Theme.logo(g, getWidth() / 2.0, 20, 430);
                g.dispose();
            }
        };
        emblem.setPreferredSize(new Dimension(450, 250));
        p.add(emblem, c);

        c.gridy++;
        homeStats = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                String v = manifest == null ? "-" : Json.str(manifest.get("gameVersion"), "0.0");
                int mods = manifest == null ? 0 : Json.arr(manifest.get("mods")).size();
                stat(g, 70, "Version du jeu", v, Theme.GREEN);
                stat(g, 250, "Mods installes", String.valueOf(mods), Theme.GOLD);
                g.dispose();
            }

            private void stat(Graphics2D g, int x, String label, String value, Color dot) {
                g.setPaint(dot);
                g.fillOval(x, 8, 8, 8);
                g.setFont(Theme.font(Font.PLAIN, 12f));
                g.setPaint(Theme.MUTED);
                g.drawString(label, x + 14, 16);
                g.setFont(Theme.font(Font.PLAIN, 22f));
                g.setPaint(Theme.TEXT);
                g.drawString(value, x, 44);
            }
        };
        homeStats.setPreferredSize(new Dimension(420, 56));
        p.add(homeStats, c);

        c.gridy++;
        c.insets = new Insets(14, 0, 8, 0);
        play = new Theme.PlayButton();
        play.onClick(() -> new Thread(this::play, "eldoria-play").start());
        p.add(play, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        status = text("Recherche de mises a jour...", 11.5f, Font.PLAIN, Theme.MUTED);
        p.add(status, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel social = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        social.setOpaque(false);
        social.add(new Theme.RoundIcon(Theme.Icon.WIKI, "Wiki et code source", () -> open(WIKI_URL)));
        social.add(new Theme.RoundIcon(Theme.Icon.DISCORD, "Discord", () -> {
            String url = manifest == null ? "" : Json.str(manifest.get("discordUrl"), "");
            if (!url.isBlank()) open(url);
        }));
        social.add(new Theme.RoundIcon(Theme.Icon.FOLDER, "Ouvrir le dossier du jeu", () -> open(gameDir.toUri().toString())));
        p.add(social, c);
        return p;
    }

    private JComponent listPage(String title, JPanel box) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(26, 34, 20, 34));
        JLabel t = text(title, 24f, Font.BOLD, Theme.TEXT);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        page.add(t, BorderLayout.NORTH);
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(box);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        page.add(sp, BorderLayout.CENTER);
        return page;
    }

    private JComponent card(String head, String sub, String body) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                g.setPaint(Theme.CARD);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g.setPaint(Theme.LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g.setPaint(Theme.GOLD);
                g.fillRect(0, 10, 3, getHeight() - 20);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        card.add(text(head, 15f, Font.BOLD, Theme.TEXT), c);
        if (sub != null) {
            c.gridy = 1;
            card.add(text(sub, 11f, Font.PLAIN, Theme.GOLD), c);
        }
        if (body != null) {
            c.gridy = 2;
            c.insets = new Insets(4, 0, 0, 0);
            JLabel b = text("<html><body style='width:560px'>" + body.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</body></html>", 12.5f, Font.PLAIN, Theme.MUTED);
            card.add(b, c);
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        card.setAlignmentX(0f);
        return card;
    }

    /** Remplit les pages News et Changelog a partir du manifeste (version, date, notes de publication et liste des mods). */
    private void fillNews() {
        newsBox.removeAll();
        changeBox.removeAll();
        String version = Json.str(manifest.get("gameVersion"), "0.0");
        String date = Json.str(manifest.get("date"), "");
        List<Object> notes = Json.arr(manifest.get("notes"));
        if (notes.isEmpty()) {
            newsBox.add(card("Aucune actualite pour le moment", null, "Revenez bientot : les nouveautes d'Aeloria apparaissent ici."));
        }
        for (Object n : notes) {
            newsBox.add(card(String.valueOf(n), "Version " + version, null));
            newsBox.add(Box.createVerticalStrut(10));
        }
        StringBuilder sb = new StringBuilder();
        for (Object n : notes) sb.append("• ").append(n).append("\n");
        changeBox.add(card("Version " + version, date, sb.length() == 0 ? "Pas de notes pour cette version." : sb.toString().stripTrailing()));
        changeBox.add(Box.createVerticalStrut(10));
        StringBuilder modList = new StringBuilder();
        for (Object o : Json.arr(manifest.get("mods"))) modList.append("• ").append(Json.str(Json.obj(o).get("file"), "?")).append("\n");
        changeBox.add(card("Mods installes", Json.arr(manifest.get("mods")).size() + " fichiers", modList.toString().stripTrailing()));
        newsBox.revalidate();
        newsBox.repaint();
        changeBox.revalidate();
        changeBox.repaint();
    }

    private void refreshHomeStats() {
        if (homeStats != null) homeStats.repaint();
    }

    private JComponent accountPage() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 12, 0);
        c.anchor = GridBagConstraints.CENTER;
        JComponent big = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.smooth(g0);
                Theme.avatar(g, head, 8, 8, 96);
                g.dispose();
            }
        };
        big.setPreferredSize(new Dimension(112, 112));
        page.add(big, c);
        c.gridy++;
        accountLabel = text("Compte : hors ligne", 18f, Font.BOLD, Theme.TEXT);
        page.add(accountLabel, c);
        c.gridy++;
        loginBtn = new Theme.FlatButton("Connexion Microsoft").primary().icon(Theme.Icon.ACCOUNT).onClick(this::toggleLogin);
        loginBtn.setEnabled(false);
        loginBtn.setPreferredSize(new Dimension(240, 38));
        page.add(loginBtn, c);
        c.gridy++;
        c.insets = new Insets(18, 0, 6, 0);
        page.add(text("Pseudo hors ligne (solo, serveurs hors ligne)", 12f, Font.PLAIN, Theme.MUTED), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        pseudo = new JTextField(Json.str(settings.get("pseudo"), ""), 16);
        style(pseudo);
        pseudo.setToolTipText("Pseudo utilise sans compte Microsoft (solo / serveur hors ligne)");
        pseudo.setPreferredSize(new Dimension(240, 34));
        page.add(pseudo, c);
        return page;
    }

    private JComponent settingsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(26, 34, 20, 34));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        top.setOpaque(false);
        top.add(text("REGLAGES", 24f, Font.BOLD, Theme.TEXT));
        page.add(top, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.add(text("Memoire allouee au jeu (Go)", 13f, Font.PLAIN, Theme.MUTED));
        ram = new JComboBox<>(new String[]{"2", "3", "4", "6", "8", "12", "16"});
        ram.setSelectedItem(Json.str(settings.get("ramGo"), "4"));
        ram.setBackground(Theme.CARD);
        ram.setForeground(Theme.TEXT);
        row.add(ram);
        row.add(new Theme.FlatButton("Ouvrir le dossier du jeu").icon(Theme.Icon.FOLDER).onClick(() -> open(gameDir.toUri().toString())));
        body.add(row, BorderLayout.NORTH);
        log = new JTextArea();
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBackground(new Color(0x0B0910));
        log.setForeground(new Color(0xB9B0CC));
        log.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane sp = new JScrollPane(log);
        sp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        body.add(sp, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private static void style(JTextField f) {
        f.setBackground(new Color(0x0B0910));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.GOLD);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        f.setFont(Theme.font(Font.PLAIN, 13f));
    }

    private void open(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            log("Impossible d'ouvrir " + url + " : " + e.getMessage());
        }
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

    /** Message d'etat sous le bouton ; tant que le jeu n'est pas pret, le bouton JOUER se transforme en jauge. */
    private void status(String s, int permille) {
        if (status == null) return;
        SwingUtilities.invokeLater(() -> {
            status.setText(s);
            if (!ready) play.loading(permille < 0 ? "MISE A JOUR EN COURS..." : "CHARGEMENT... " + permille / 10 + "%", permille);
        });
    }

    private void ui(Runnable r) { if (frame != null) SwingUtilities.invokeLater(r); }

    private void error(String what, Exception e) {
        e.printStackTrace();
        log("ERREUR : " + e.getMessage());
        status(what + " - voir le journal", 0);
        ui(() -> JOptionPane.showMessageDialog(frame, what + " :\n" + e.getMessage(), "Aeloria", JOptionPane.ERROR_MESSAGE));
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
            ui(() -> {
                fillNews();
                refreshHomeStats();
            });
            for (Object n : Json.arr(manifest.get("notes"))) log("  - " + n);
            int minBoot = (int) Double.parseDouble(Json.str(manifest.get("bootstrapVersion"), "1"));
            if (minBoot > bootVersion)
                log("! Nouvelle version du launcher disponible : " + Json.str(manifest.get("releaseUrl"), ""));

            long t0 = System.nanoTime();
            checkDisk();
            setupAuth();
            long t1 = System.nanoTime();
            syncMods();
            long t2 = System.nanoTime();
            game = new Game(home.resolve("minecraft"), gameDir, this::log, this::status);
            game.prepare(Json.str(manifest.get("minecraft"), "26.3"), Json.str(manifest.get("neoforge"), ""));
            long t3 = System.nanoTime();
            cleanup();
            log(String.format("Temps : compte %.1f s, mods %.1f s, Minecraft/Java/NeoForge %.1f s, total %.1f s",
                    (t1 - t0) / 1e9, (t2 - t1) / 1e9, (t3 - t2) / 1e9, (System.nanoTime() - t0) / 1e9));
            ready = true;
            status("Pret ! Clique sur JOUER.", 1000);
            log("Pret.");
            ui(() -> play.ready());
        } catch (Exception e) {
            error("Erreur pendant la mise a jour", e);
        }
    }

    /** Premiere installation : ~2 Go ; mise a jour : bien moins. On previent avant de commencer si le disque est trop plein. */
    private void checkDisk() throws Exception {
        boolean fresh = !Files.isDirectory(home.resolve("minecraft").resolve("assets"));
        long need = fresh ? 3L << 30 : 500L << 20;
        long free = Files.getFileStore(Files.createDirectories(home)).getUsableSpace();
        log("Espace disque libre : " + (free >> 20) + " Mo");
        if (free < need) {
            String msg = "Espace disque insuffisant : " + (free >> 20) + " Mo libres, environ " + (need >> 20)
                    + " Mo necessaires. Libere de la place sur le disque puis relance le launcher.";
            if (free < need / 4) throw new IllegalStateException(msg);
            log("! " + msg);
        }
    }

    /** Supprime les telechargements incomplets et installeurs restants. */
    private void cleanup() {
        long freed = 0;
        for (Path root : new Path[]{mods, state, home.resolve("launcher"), home.resolve("minecraft")}) {
            if (!Files.isDirectory(root)) continue;
            try (var w = Files.walk(root, root.equals(home.resolve("minecraft")) ? 1 : 2)) {
                for (Path p : (Iterable<Path>) w.filter(Files::isRegularFile)::iterator) {
                    String n = p.getFileName().toString();
                    if (n.endsWith(".part") || n.endsWith("-installer.jar") || n.endsWith("-installer.jar.log")) {
                        freed += Files.size(p);
                        Files.deleteIfExists(p);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (freed > 0) log("Fichiers temporaires supprimes : " + (freed >> 20) + " Mo liberes");
    }

    private void syncMods() throws Exception {
        List<Object> list = Json.arr(manifest.get("mods"));
        Set<String> wanted = new LinkedHashSet<>();
        Path cachePath = state.resolve("hash-cache.json");
        Map<String, Object> cache = new java.util.concurrent.ConcurrentHashMap<>();
        try {
            if (Files.exists(cachePath)) cache.putAll(Json.obj(Json.parse(Files.readString(cachePath))));
        } catch (Exception ignored) {}
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        List<java.util.concurrent.Future<?>> jobs = new ArrayList<>();
        int total = Math.max(1, list.size());
        try {
            for (Object o : list) {
                Map<String, Object> mod = Json.obj(o);
                String file = Json.str(mod.get("file"), null);
                String url = Json.str(mod.get("url"), null);
                if (file == null || url == null || file.contains("/") || file.contains("\\")) continue;
                String algo = mod.containsKey("sha512") ? "SHA-512" : mod.containsKey("sha1") ? "SHA-1" : "SHA-256";
                String sha = Json.str(mod.containsKey("sha512") ? mod.get("sha512") : mod.containsKey("sha1") ? mod.get("sha1") : mod.get("sha256"), "");
                wanted.add(file);
                Path target = mods.resolve(file);
                jobs.add(pool.submit(() -> {
                    try {
                        if (Net.validCached(target, algo, sha, cache)) {
                            log("A jour : " + file);
                        } else {
                            log("Telechargement : " + file);
                            long start = System.nanoTime();
                            Net.download(url, target, algo, sha, d -> {
                                double sec = Math.max(0.2, (System.nanoTime() - start) / 1e9);
                                status("Telechargement de " + file + " : " + (d >> 20) + " Mo, " + String.format("%.1f", d / sec / 1048576) + " Mo/s", -1);
                            });
                            Net.remember(target, sha, cache);
                            long sz = Files.size(target);
                            log(String.format("  %s : %d Mo en %.1f s (%.1f Mo/s)", file, sz >> 20, (System.nanoTime() - start) / 1e9,
                                    sz / Math.max(0.1, (System.nanoTime() - start) / 1e9) / 1048576));
                        }
                        int d = done.incrementAndGet();
                        status("Mods (" + d + "/" + total + ")", d * 1000 / total);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            for (java.util.concurrent.Future<?> f : jobs) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable c = e.getCause();
                    throw c instanceof RuntimeException && c.getCause() instanceof Exception ex ? ex : e;
                }
            }
        } finally {
            pool.shutdownNow();
            try {
                Files.writeString(cachePath, Json.write(new LinkedHashMap<>(cache)));
            } catch (Exception ignored) {}
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
        ui(() -> {
            loginBtn.setEnabled(true);
            sideLogin.setEnabled(true);
        });
        msAccount = auth.refresh();
        refreshAccountUi();
        if (msAccount != null) log("Connecte en tant que " + msAccount.name());
    }

    private void refreshAccountUi() {
        MsAuth.Account acc = msAccount;
        ui(() -> {
            accountLabel.setText(acc != null ? acc.name() : "Hors ligne");
            sideName.setText(acc != null ? acc.name() : "Hors ligne");
            loginBtn.setText(acc != null ? "Deconnexion" : "Connexion Microsoft");
            sideLogin.setText(acc != null ? "SE DECONNECTER" : "SE CONNECTER");
            if (acc == null) head = null;
            avatarView.repaint();
            pages.repaint();
        });
        if (acc != null) loadHead(acc.name());
    }

    /** Telecharge la tete du joueur (en arriere-plan) ; sans reseau, la silhouette reste affichee. */
    private void loadHead(String name) {
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = javax.imageio.ImageIO.read(URI.create("https://mc-heads.net/avatar/" + name + "/64").toURL());
                if (img != null) {
                    head = img;
                    ui(() -> {
                        avatarView.repaint();
                        pages.repaint();
                    });
                }
            } catch (Exception ignored) {}
        }, "eldoria-head");
        t.setDaemon(true);
        t.start();
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
        sideLogin.setEnabled(false);
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
                    sideLogin.setEnabled(true);
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
                        "Aeloria", JOptionPane.WARNING_MESSAGE));
                return;
            }
            acc = MsAuth.Account.offline(name);
            settings.put("pseudo", name);
        }
        String go = ram != null ? (String) ram.getSelectedItem() : "4";
        settings.put("ramGo", go);
        saveSettings();
        ui(() -> play.launched("MINECRAFT EST LANCE"));
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
            ui(() -> play.ready());
        }
    }
}
