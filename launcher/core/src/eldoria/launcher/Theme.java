package eldoria.launcher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Habillage du launcher Aeloria : palette (violet nuit, or), emblème dessiné en Java2D et composants plats (boutons, navigation,
 * bouton JOUER avec sa progression). Rien n'est chargé depuis le disque : tout est dessiné.
 */
final class Theme {
    static final Color BG = new Color(0x0A0810);
    static final Color BAR = new Color(0x0D0B14);
    static final Color SIDE = new Color(0x0C0A12);
    static final Color CARD = new Color(0x14111C);
    static final Color CARD_HOVER = new Color(0x1E1929);
    static final Color LINE = new Color(0x2B2538);
    static final Color GOLD = new Color(0xE2B85A);
    static final Color GOLD_LIGHT = new Color(0xFFE6A0);
    static final Color GOLD_DARK = new Color(0xB8862B);
    static final Color VIOLET = new Color(0x6E5A9C);
    static final Color VIOLET_DEEP = new Color(0x3A2F58);
    static final Color TEXT = new Color(0xEDE8F5);
    static final Color MUTED = new Color(0x8D84A3);
    static final Color GREEN = new Color(0x6AE06A);
    static final Color RED = new Color(0xFF6B6B);

    private Theme() {}

    static Font font(int style, float size) {
        return new Font("Segoe UI", style, 12).deriveFont(size);
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // ------------------------------------------------------------------------------------------------ emblème

    /** Images du logo (embarquees dans le jar du launcher, voir tools/build_logo.py) ; null si elles manquent : le dessin par code prend le relais. */
    static final BufferedImage EMBLEM_IMG = load("/eldoria/launcher/emblem.png");
    static final BufferedImage LOGO_IMG = load("/eldoria/launcher/logo.png");
    private static final java.util.Map<String, BufferedImage> SCALED = new java.util.HashMap<>();

    private static BufferedImage load(String name) {
        try (java.io.InputStream in = Theme.class.getResourceAsStream(name)) {
            return in == null ? null : javax.imageio.ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    /** Version reduite de bonne qualite (reduction par moities successives), mise en cache. */
    static BufferedImage scaled(BufferedImage src, String key, int w, int h) {
        String k = key + w + "x" + h;
        BufferedImage hit = SCALED.get(k);
        if (hit != null) return hit;
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = step(cur, cw, ch);
        }
        cur = step(cur, w, h);
        SCALED.put(k, cur);
        return cur;
    }

    private static BufferedImage step(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Embleme d'Aeloria centre en (cx, cy), {@code size} de haut. */
    static void emblem(Graphics2D g, double cx, double cy, double size) {
        if (EMBLEM_IMG == null) {
            emblemVector(g, cx, cy, size);
            return;
        }
        int s = Math.max(8, (int) Math.round(size));
        g.drawImage(scaled(EMBLEM_IMG, "e", s, s), (int) Math.round(cx - s / 2.0), (int) Math.round(cy - s / 2.0), null);
    }

    /** Logo complet (embleme et nom) centre horizontalement en cx, haut en y, {@code width} de large ; renvoie sa hauteur. */
    static int logo(Graphics2D g, double cx, double y, int width) {
        if (LOGO_IMG == null) {
            emblemVector(g, cx, y + 100, 190);
            int w = wordmarkWidth(g, 34f);
            wordmark(g, (int) (cx - w / 2.0), (int) (y + 250), 34f);
            return 280;
        }
        int h = (int) Math.round(LOGO_IMG.getHeight() * width / (double) LOGO_IMG.getWidth());
        g.drawImage(scaled(LOGO_IMG, "l", width, h), (int) Math.round(cx - width / 2.0), (int) Math.round(y), null);
        return h;
    }

    /** Emblème d'Aeloria dessiné par code (secours) : anneau d'or ouvert, cristal facetté violet et rune en « A ». */
    static void emblemVector(Graphics2D g, double cx, double cy, double size) {
        double r = size / 2;
        // halo
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (r * 1.25), new float[]{0f, 0.55f, 1f},
                new Color[]{alpha(VIOLET, 120), alpha(VIOLET_DEEP, 60), alpha(BG, 0)}));
        g.fill(new Ellipse2D.Double(cx - r * 1.25, cy - r * 1.25, r * 2.5, r * 2.5));
        // anneau ouvert
        double ring = r * 0.92;
        g.setStroke(new BasicStroke((float) (size * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint((float) (cx - r), (float) (cy - r), GOLD_LIGHT, (float) (cx + r), (float) (cy + r), GOLD_DARK));
        g.draw(new Arc2D.Double(cx - ring, cy - ring, ring * 2, ring * 2, 118, 304, Arc2D.OPEN));
        g.setStroke(new BasicStroke((float) (size * 0.012)));
        g.setPaint(alpha(GOLD, 120));
        double inner = r * 0.8;
        g.draw(new Arc2D.Double(cx - inner, cy - inner, inner * 2, inner * 2, 130, 280, Arc2D.OPEN));
        // petits losanges aux extrémités de l'anneau
        for (double deg : new double[]{118, 62}) {
            double a = Math.toRadians(deg);
            diamond(g, cx + Math.cos(a) * ring, cy - Math.sin(a) * ring, size * 0.045, GOLD_LIGHT);
        }
        // cristal central : six facettes
        double h = r * 0.95, w = r * 0.5;
        double[][] p = {{0, -h}, {w, -h * 0.3}, {w * 0.8, h * 0.55}, {0, h}, {-w * 0.8, h * 0.55}, {-w, -h * 0.3}};
        Path2D.Double crystal = new Path2D.Double();
        crystal.moveTo(cx + p[0][0], cy + p[0][1]);
        for (int i = 1; i < p.length; i++) crystal.lineTo(cx + p[i][0], cy + p[i][1]);
        crystal.closePath();
        g.setPaint(new GradientPaint((float) cx, (float) (cy - h), new Color(0x9A84D6), (float) cx, (float) (cy + h), VIOLET_DEEP));
        g.fill(crystal);
        // facettes
        Path2D.Double left = new Path2D.Double();
        left.moveTo(cx, cy - h);
        left.lineTo(cx - w, cy - h * 0.3);
        left.lineTo(cx - w * 0.8, cy + h * 0.55);
        left.lineTo(cx, cy + h * 0.1);
        left.closePath();
        g.setPaint(alpha(new Color(0x2A2145), 150));
        g.fill(left);
        Path2D.Double top = new Path2D.Double();
        top.moveTo(cx, cy - h);
        top.lineTo(cx + w, cy - h * 0.3);
        top.lineTo(cx, cy + h * 0.1);
        top.lineTo(cx - w, cy - h * 0.3);
        top.closePath();
        g.setPaint(alpha(new Color(0xD7C8FF), 90));
        g.fill(top);
        g.setStroke(new BasicStroke((float) (size * 0.014), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(alpha(GOLD_LIGHT, 200));
        g.draw(crystal);
        g.draw(new java.awt.geom.Line2D.Double(cx, cy - h, cx, cy + h * 0.1));
        // rune en « A » dorée devant le cristal
        double ah = r * 0.62, aw = r * 0.36;
        g.setStroke(new BasicStroke((float) (size * 0.034), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint((float) cx, (float) (cy - ah), GOLD_LIGHT, (float) cx, (float) (cy + ah), GOLD));
        Path2D.Double a = new Path2D.Double();
        a.moveTo(cx - aw, cy + ah * 0.7);
        a.lineTo(cx, cy - ah * 0.75);
        a.lineTo(cx + aw, cy + ah * 0.7);
        g.draw(a);
        g.draw(new java.awt.geom.Line2D.Double(cx - aw * 0.5, cy + ah * 0.18, cx + aw * 0.5, cy + ah * 0.18));
        diamond(g, cx, cy - ah * 0.98, size * 0.04, GOLD_LIGHT);
    }

    static void diamond(Graphics2D g, double x, double y, double r, Color c) {
        Path2D.Double d = new Path2D.Double();
        d.moveTo(x, y - r);
        d.lineTo(x + r, y);
        d.lineTo(x, y + r);
        d.lineTo(x - r, y);
        d.closePath();
        g.setPaint(c);
        g.fill(d);
    }

    /** Nom du jeu : « AEL » épais en or, « ORIA » fin en violet clair (comme le logo en jeu). */
    static void wordmark(Graphics2D g, int x, int baseline, float size) {
        Font bold = new Font("Segoe UI", Font.BOLD, 12).deriveFont(size);
        Font thin = new Font("Segoe UI Light", Font.PLAIN, 12).deriveFont(size);
        FontMetrics fb = g.getFontMetrics(bold);
        g.setFont(bold);
        g.setPaint(new GradientPaint(x, baseline - size, GOLD_LIGHT, x, baseline, GOLD_DARK));
        g.drawString("AEL", x, baseline);
        int w = fb.stringWidth("AEL");
        g.setFont(thin);
        g.setPaint(new Color(0xA896D8));
        g.drawString("ORIA", x + w, baseline);
    }

    static int wordmarkWidth(Graphics2D g, float size) {
        Font bold = new Font("Segoe UI", Font.BOLD, 12).deriveFont(size);
        Font thin = new Font("Segoe UI Light", Font.PLAIN, 12).deriveFont(size);
        return g.getFontMetrics(bold).stringWidth("AEL") + g.getFontMetrics(thin).stringWidth("ORIA");
    }

    // ------------------------------------------------------------------------------------------------ icônes de navigation

    enum Icon { HOME, NEWS, CHANGELOG, ACCOUNT, WIKI, DISCORD, GEAR, FOLDER }

    static void icon(Graphics2D g, Icon icon, double x, double y, double s, Color c) {
        g.setPaint(c);
        g.setStroke(new BasicStroke((float) (s * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case HOME -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x + s * 0.1, y + s * 0.5);
                p.lineTo(x + s * 0.5, y + s * 0.12);
                p.lineTo(x + s * 0.9, y + s * 0.5);
                g.draw(p);
                g.draw(new java.awt.geom.Rectangle2D.Double(x + s * 0.22, y + s * 0.5, s * 0.56, s * 0.38));
            }
            case NEWS -> {
                g.draw(new RoundRectangle2D.Double(x + s * 0.12, y + s * 0.16, s * 0.76, s * 0.68, 4, 4));
                for (int i = 0; i < 3; i++) g.draw(new java.awt.geom.Line2D.Double(x + s * 0.28, y + s * (0.34 + i * 0.16), x + s * 0.72, y + s * (0.34 + i * 0.16)));
            }
            case CHANGELOG -> {
                g.draw(new Arc2D.Double(x + s * 0.14, y + s * 0.14, s * 0.72, s * 0.72, 30, 250, Arc2D.OPEN));
                diamond(g, x + s * 0.8, y + s * 0.3, s * 0.1, c);
            }
            case ACCOUNT -> {
                g.draw(new Ellipse2D.Double(x + s * 0.32, y + s * 0.1, s * 0.36, s * 0.36));
                g.draw(new Arc2D.Double(x + s * 0.14, y + s * 0.52, s * 0.72, s * 0.7, 20, 140, Arc2D.OPEN));
            }
            case WIKI -> {
                g.draw(new RoundRectangle2D.Double(x + s * 0.2, y + s * 0.12, s * 0.6, s * 0.76, 4, 4));
                g.draw(new java.awt.geom.Line2D.Double(x + s * 0.34, y + s * 0.12, x + s * 0.34, y + s * 0.88));
                g.draw(new java.awt.geom.Line2D.Double(x + s * 0.46, y + s * 0.36, x + s * 0.7, y + s * 0.36));
            }
            case DISCORD -> {
                g.draw(new RoundRectangle2D.Double(x + s * 0.1, y + s * 0.18, s * 0.8, s * 0.56, 10, 10));
                g.fill(new Ellipse2D.Double(x + s * 0.3, y + s * 0.38, s * 0.1, s * 0.12));
                g.fill(new Ellipse2D.Double(x + s * 0.6, y + s * 0.38, s * 0.1, s * 0.12));
            }
            case GEAR -> {
                g.draw(new Ellipse2D.Double(x + s * 0.3, y + s * 0.3, s * 0.4, s * 0.4));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    g.draw(new java.awt.geom.Line2D.Double(x + s * 0.5 + Math.cos(a) * s * 0.28, y + s * 0.5 + Math.sin(a) * s * 0.28,
                            x + s * 0.5 + Math.cos(a) * s * 0.42, y + s * 0.5 + Math.sin(a) * s * 0.42));
                }
            }
            case FOLDER -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x + s * 0.1, y + s * 0.8);
                p.lineTo(x + s * 0.1, y + s * 0.22);
                p.lineTo(x + s * 0.4, y + s * 0.22);
                p.lineTo(x + s * 0.5, y + s * 0.34);
                p.lineTo(x + s * 0.9, y + s * 0.34);
                p.lineTo(x + s * 0.9, y + s * 0.8);
                p.closePath();
                g.draw(p);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ composants

    /** Bouton plat : fond de carte, bordure fine, accent doré au survol. */
    static class FlatButton extends JComponent {
        private String text;
        private boolean hover;
        private boolean primary;
        private Runnable action = () -> {};
        private Icon icon;

        FlatButton(String text) {
            this.text = text;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(180, 34));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (isEnabled() && e.getButton() == MouseEvent.BUTTON1) action.run(); }
            });
        }

        FlatButton primary() { primary = true; return this; }
        FlatButton icon(Icon i) { icon = i; return this; }
        FlatButton onClick(Runnable r) { action = r; return this; }
        void setText(String t) { text = t; repaint(); }
        String text() { return text; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            Color fill = !on ? new Color(0x0F0D15) : primary ? mix(CARD, GOLD, hover ? 0.3f : 0.16f) : hover ? CARD_HOVER : CARD;
            g.setPaint(fill);
            g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 6, 6));
            g.setPaint(!on ? new Color(0x1E1A28) : hover || primary ? alpha(GOLD, hover ? 255 : 150) : LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 2, h - 2, 6, 6));
            g.setFont(font(Font.BOLD, 12.5f));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int ix = icon == null ? 0 : 22;
            int tx = (w - tw - ix) / 2 + ix;
            if (icon != null) Theme.icon(g, icon, tx - 22, (h - 16) / 2.0, 16, on ? (hover ? GOLD_LIGHT : MUTED) : new Color(0x5C556E));
            g.setPaint(!on ? new Color(0x5C556E) : hover ? Color.WHITE : TEXT);
            g.drawString(text, tx, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Entrée de la barre latérale : icône + texte, trait doré à gauche quand elle est active. */
    static class NavItem extends JComponent {
        private final String text;
        private final Icon icon;
        private final boolean external;
        private boolean active;
        private boolean hover;

        NavItem(String text, Icon icon, boolean external, Runnable action) {
            this.text = text;
            this.icon = icon;
            this.external = external;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(158, 46));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) action.run(); }
            });
        }

        void setActive(boolean a) { active = a; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            if (active) {
                g.setPaint(new GradientPaint(0, 0, alpha(GOLD, 38), w, 0, alpha(GOLD, 0)));
                g.fillRect(0, 0, w, h);
                g.setPaint(GOLD);
                g.fillRect(0, 6, 3, h - 12);
            } else if (hover) {
                g.setPaint(alpha(Color.WHITE, 10));
                g.fillRect(0, 0, w, h);
            }
            Color c = active ? GOLD_LIGHT : hover ? TEXT : MUTED;
            icon(g, icon, 18, (h - 20) / 2.0, 20, c);
            g.setFont(font(active ? Font.BOLD : Font.PLAIN, 13.5f));
            FontMetrics fm = g.getFontMetrics();
            g.setPaint(active ? Color.WHITE : c);
            g.drawString(text, 50, (h + fm.getAscent() - fm.getDescent()) / 2);
            if (external) {
                g.setStroke(new BasicStroke(1.3f));
                g.setPaint(MUTED);
                int ex = w - 26, ey = h / 2 - 5;
                g.draw(new java.awt.geom.Line2D.Double(ex + 2, ey + 8, ex + 9, ey + 1));
                g.draw(new java.awt.geom.Line2D.Double(ex + 4, ey + 1, ex + 9, ey + 1));
                g.draw(new java.awt.geom.Line2D.Double(ex + 9, ey + 1, ex + 9, ey + 6));
            }
            g.dispose();
        }
    }

    /** Gros bouton JOUER : dégradé d'or ; pendant les chargements il se remplit comme une jauge. */
    static class PlayButton extends JComponent {
        private String text = "JOUER";
        private int permille = -1;
        private boolean hover;
        private boolean busy;
        private boolean working;
        private Runnable action = () -> {};
        private int spin;

        PlayButton() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(420, 54));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (isEnabled() && !busy && e.getButton() == MouseEvent.BUTTON1) action.run(); }
            });
            new javax.swing.Timer(60, e -> {
                if (working) {
                    spin = (spin + 30) % 360;
                    repaint();
                }
            }).start();
        }

        void onClick(Runnable r) { action = r; }

        /** Etat « prêt » : texte JOUER, cliquable. */
        void ready() { busy = false; working = false; permille = -1; text = "JOUER"; repaint(); }

        /** Etat « en cours » : jauge de progression ({@code permille} < 0 : indéterminé). */
        void loading(String label, int permille) { busy = true; working = true; this.permille = permille; text = label; repaint(); }

        void launched(String label) { busy = true; working = false; permille = -1; text = label; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled() || busy;
            Shape shape = new RoundRectangle2D.Double(0, 0, w, h, 8, 8);
            Color top = hover && !busy ? new Color(0xFFD46A) : GOLD;
            Color bottom = hover && !busy ? new Color(0xE0A53A) : GOLD_DARK;
            if (busy) {
                g.setPaint(new Color(0x2A2112));
                g.fill(shape);
                int fw = permille < 0 ? w : w * Math.max(0, Math.min(1000, permille)) / 1000;
                g.setClip(shape);
                g.setPaint(new GradientPaint(0, 0, alpha(top, 235), 0, h, alpha(bottom, 235)));
                g.fillRect(0, 0, fw, h);
                g.setClip(null);
                g.setPaint(alpha(GOLD, 120));
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 8, 8));
            } else {
                g.setPaint(on ? new GradientPaint(0, 0, top, 0, h, bottom) : new GradientPaint(0, 0, new Color(0x3A3446), 0, h, new Color(0x2A2535)));
                g.fill(shape);
                g.setPaint(alpha(Color.WHITE, 40));
                g.fill(new RoundRectangle2D.Double(1, 1, w - 2, h / 2.0 - 1, 8, 8));
            }
            g.setFont(font(Font.BOLD, 21f));
            FontMetrics fm = g.getFontMetrics();
            String label = text;
            int tw = fm.stringWidth(label);
            int extra = working ? 30 : busy ? 0 : 30;
            int tx = (w - tw - extra) / 2 + (working ? 30 : 0);
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            Color ink = busy ? Color.WHITE : on ? new Color(0x1A1206) : new Color(0x8D84A3);
            g.setPaint(busy ? alpha(Color.BLACK, 120) : alpha(Color.WHITE, 60));
            g.drawString(label, tx + 1, ty + 1);
            g.setPaint(ink);
            g.drawString(label, tx, ty);
            if (working) {
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setPaint(Color.WHITE);
                g.draw(new Arc2D.Double(tx - 30, h / 2.0 - 9, 18, 18, spin, 260, Arc2D.OPEN));
            } else if (!busy) {
                int cx = tx + tw + 14;
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setPaint(ink);
                for (int i = 0; i < 2; i++) {
                    Path2D.Double chevron = new Path2D.Double();
                    chevron.moveTo(cx + i * 9, h / 2.0 - 7);
                    chevron.lineTo(cx + i * 9 + 7, h / 2.0);
                    chevron.lineTo(cx + i * 9, h / 2.0 + 7);
                    g.draw(chevron);
                }
            }
            g.dispose();
        }
    }

    /** Pastille ronde (réseaux) : icône sur fond sombre cerclé d'or. */
    static class RoundIcon extends JComponent {
        private final Icon icon;
        private boolean hover;

        RoundIcon(Icon icon, String tooltip, Runnable action) {
            this.icon = icon;
            setToolTipText(tooltip);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(34, 34));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { action.run(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = getWidth(), h = getHeight();
            g.setPaint(hover ? new Color(0x2A2112) : CARD);
            g.fill(new Ellipse2D.Double(1, 1, w - 3, h - 3));
            g.setStroke(new BasicStroke(1.4f));
            g.setPaint(hover ? GOLD_LIGHT : alpha(GOLD, 170));
            g.draw(new Ellipse2D.Double(1, 1, w - 3, h - 3));
            icon(g, icon, w / 2.0 - 9, h / 2.0 - 9, 18, hover ? GOLD_LIGHT : GOLD);
            g.dispose();
        }
    }

    /** Silhouette de joueur (avatar de secours) ou tête Minecraft si elle a pu être téléchargée. */
    static void avatar(Graphics2D g, BufferedImage head, int x, int y, int size) {
        Shape circle = new Ellipse2D.Double(x, y, size, size);
        g.setPaint(new Color(0x1B1626));
        g.fill(circle);
        if (head != null) {
            g.setClip(circle);
            g.drawImage(head, x, y, size, size, null);
            g.setClip(null);
        } else {
            g.setClip(circle);
            g.setPaint(new Color(0xCFC6E6));
            g.fill(new Ellipse2D.Double(x + size * 0.32, y + size * 0.16, size * 0.36, size * 0.36));
            g.fill(new Ellipse2D.Double(x + size * 0.14, y + size * 0.58, size * 0.72, size * 0.7));
            g.setClip(null);
        }
        g.setStroke(new BasicStroke(2f));
        g.setPaint(GOLD);
        g.draw(circle);
    }
}
