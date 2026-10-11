"""Auto-peche Minecraft - interface complete.

Lancer :  python autopeche.py      (F8 = demarrer / arreter)
"""
import queue
import sys
import time
import tkinter as tk
from collections import deque
from tkinter import filedialog, messagebox, ttk

if sys.platform == "win32":  # coordonnees ecran exactes avec la mise a l'echelle Windows
    try:
        import ctypes
        ctypes.windll.shcore.SetProcessDpiAwareness(2)
    except Exception:
        pass

from PIL import ImageTk

from bot import (APPROACH, CALIBRATING, PAUSED, REELING, SETTLING, STARTING, STOPPED, WAITING,
                 FishingBot)
from config import CONFIG_PATH, Config
from detector import rgb_to_hsv

BG, PANEL, FG, ACC, MUTED = "#15181e", "#1f242c", "#e6e6e6", "#4fc3f7", "#8a93a0"
STATE_COLORS = {STOPPED: "#8a93a0", STARTING: "#ffd54f", "LANCER": "#ffd54f", SETTLING: "#90caf9",
                WAITING: "#4fc3f7", CALIBRATING: "#ffd54f", APPROACH: "#ffa726", REELING: "#ef5350", PAUSED: "#ba68c8"}
SERIES = [("score", "#ffffff"), ("dip", "#66bb6a"), ("bubble", "#ffee58"), ("lost", "#ef5350")]


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Auto-Peche Minecraft")
        self.configure(bg=BG)
        self.geometry("1040x700")
        self.minsize(900, 620)
        self.cfg = Config.load()
        self.events = queue.Queue()
        self.bot = FishingBot(self.cfg, self.events)
        self.history = self.bot.history
        self._shown = None
        self.last_frame = None
        self.preview_img = None
        self.vars = {}
        self._style()
        self._build()
        self._hotkeys()
        self.bot.start()
        self.protocol("WM_DELETE_WINDOW", self._close)
        self.after(40, self._poll)
        self.after(1000, self._tick_stats)

    # --- style ----------------------------------------------------------
    def _style(self):
        st = ttk.Style(self)
        st.theme_use("clam")
        st.configure(".", background=BG, foreground=FG, fieldbackground=PANEL, bordercolor=PANEL)
        st.configure("TFrame", background=BG)
        st.configure("Panel.TFrame", background=PANEL)
        st.configure("TLabel", background=BG, foreground=FG)
        st.configure("Panel.TLabel", background=PANEL)
        st.configure("Muted.TLabel", background=PANEL, foreground=MUTED)
        st.configure("Big.TLabel", background=PANEL, font=("Segoe UI", 20, "bold"))
        st.configure("Stat.TLabel", background=PANEL, font=("Segoe UI", 13, "bold"))
        st.configure("TNotebook", background=BG, borderwidth=0)
        st.configure("TNotebook.Tab", background=PANEL, foreground=MUTED, padding=(12, 6))
        st.map("TNotebook.Tab", background=[("selected", BG)], foreground=[("selected", ACC)])
        st.configure("TButton", background=PANEL, foreground=FG, padding=6)
        st.map("TButton", background=[("active", "#2b323c")])
        st.configure("Go.TButton", background="#2e7d32", font=("Segoe UI", 12, "bold"), padding=10)
        st.map("Go.TButton", background=[("active", "#388e3c")])
        st.configure("Stop.TButton", background="#c62828", font=("Segoe UI", 12, "bold"), padding=10)
        st.map("Stop.TButton", background=[("active", "#d32f2f")])
        st.configure("TCheckbutton", background=BG, foreground=FG)
        st.configure("Horizontal.TScale", background=BG, troughcolor=PANEL)
        st.configure("TLabelframe", background=BG, foreground=ACC)
        st.configure("TLabelframe.Label", background=BG, foreground=ACC)

    # --- construction ---------------------------------------------------
    def _build(self):
        left = ttk.Frame(self, style="Panel.TFrame", padding=14)
        left.pack(side="left", fill="y")
        ttk.Label(left, text="AUTO-PECHE", style="Muted.TLabel").pack(anchor="w")
        self.state_lbl = ttk.Label(left, text=STOPPED, style="Big.TLabel", foreground=MUTED, width=14)
        self.state_lbl.pack(anchor="w", pady=(2, 10))
        self.go_btn = ttk.Button(left, text="DEMARRER  (F8)", style="Go.TButton", command=self.bot.toggle)
        self.go_btn.pack(fill="x")
        ttk.Button(left, text="Calibrer maintenant", command=self.bot.request_calibration).pack(fill="x", pady=(6, 0))
        self.hint = ttk.Label(left, text="", style="Muted.TLabel", wraplength=210)
        self.hint.pack(anchor="w", pady=(6, 12))

        self.stat_lbls = {}
        for key, label in [("prises", "Prises"), ("lancers", "Lancers"), ("timeouts", "Relances"),
                           ("taux", "Prises / heure"), ("attente", "Attente moyenne"),
                           ("duree", "Duree session"), ("fps", "Images / s")]:
            ttk.Label(left, text=label, style="Muted.TLabel").pack(anchor="w")
            lbl = ttk.Label(left, text="-", style="Stat.TLabel")
            lbl.pack(anchor="w", pady=(0, 6))
            self.stat_lbls[key] = lbl
        ttk.Button(left, text="Remettre a zero", command=self._reset_stats).pack(fill="x", pady=(8, 0))

        # Jauge du score en direct
        ttk.Label(left, text="Score de touche", style="Muted.TLabel").pack(anchor="w", pady=(14, 2))
        self.gauge = tk.Canvas(left, width=210, height=18, bg="#0e1014", highlightthickness=0)
        self.gauge.pack(anchor="w")

        nb = self.nb = ttk.Notebook(self)
        nb.pack(side="left", fill="both", expand=True, padx=8, pady=8)
        self._tab_preview(nb)
        self._tab_graph(nb)
        self._tab_detection(nb)
        self._tab_colors(nb)
        self._tab_bot(nb)
        self._tab_log(nb)

    def _tab_preview(self, nb):
        f = ttk.Frame(nb, padding=8)
        nb.add(f, text="Apercu")
        bar = ttk.Frame(f)
        bar.pack(fill="x")
        ttk.Button(bar, text="Choisir la zone a l'ecran", command=self._select_roi).pack(side="left")
        ttk.Button(bar, text="Zone centrale auto", command=self._auto_roi).pack(side="left", padx=6)
        self.prev_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(bar, text="Apercu actif", variable=self.prev_var,
                        command=lambda: setattr(self.bot, "preview", self.prev_var.get())).pack(side="left", padx=8)
        self.roi_lbl = ttk.Label(bar, text="", foreground=MUTED)
        self.roi_lbl.pack(side="right")
        self.canvas = tk.Canvas(f, bg="#0e1014", highlightthickness=0, cursor="crosshair")
        self.canvas.pack(fill="both", expand=True, pady=8)
        self.canvas.bind("<Button-1>", lambda e: self._pick(e, "bobber"))
        self.canvas.bind("<Button-3>", lambda e: self._pick(e, "bubble"))
        self.live_lbl = ttk.Label(f, text="", font=("Consolas", 10))
        self.live_lbl.pack(anchor="w")
        ttk.Label(f, foreground=MUTED, text=(
            "Pipette : clic gauche = couleur du bouchon, clic droit = couleur des bulles.   "
            "Vert = bouchon, cercle = zone de recherche (bleu attente / orange approche / rouge touche).")
        ).pack(anchor="w")
        self._update_roi_lbl()

    def _tab_graph(self, nb):
        f = ttk.Frame(nb, padding=8)
        nb.add(f, text="Signaux")
        self.graph = tk.Canvas(f, bg="#0e1014", highlightthickness=0)
        self.graph.pack(fill="both", expand=True)
        leg = ttk.Frame(f)
        leg.pack(fill="x", pady=4)
        for name, col in SERIES:
            tk.Label(leg, text="■ " + name, fg=col, bg=BG).pack(side="left", padx=8)
        tk.Label(leg, text="--- seuil", fg="#ef5350", bg=BG).pack(side="left", padx=8)

    def _scroll_frame(self, nb, title):
        outer = ttk.Frame(nb)
        nb.add(outer, text=title)
        cv = tk.Canvas(outer, bg=BG, highlightthickness=0)
        sb = ttk.Scrollbar(outer, orient="vertical", command=cv.yview)
        inner = ttk.Frame(cv, padding=10)
        inner.bind("<Configure>", lambda e: cv.configure(scrollregion=cv.bbox("all")))
        cv.create_window((0, 0), window=inner, anchor="nw")
        cv.configure(yscrollcommand=sb.set)
        cv.pack(side="left", fill="both", expand=True)
        sb.pack(side="right", fill="y")
        return inner

    def _slider(self, parent, key, label, lo, hi, res=0.01, tip=""):
        row = ttk.Frame(parent)
        row.pack(fill="x", pady=3)
        ttk.Label(row, text=label, width=30).pack(side="left")
        is_int = isinstance(getattr(self.cfg, key), int) and not isinstance(getattr(self.cfg, key), bool)
        var = tk.DoubleVar(value=getattr(self.cfg, key))
        val = ttk.Label(row, width=7, foreground=ACC)
        val.pack(side="right")

        def changed(*_):
            v = var.get()
            v = int(round(v)) if is_int else round(round(v / res) * res, 4)
            setattr(self.cfg, key, v)
            val.configure(text=str(v))
            if key.startswith(("bobber_hue", "bubble_hue")):
                self._update_swatches()
        var.trace_add("write", changed)
        ttk.Scale(row, from_=lo, to=hi, variable=var, orient="horizontal").pack(side="left", fill="x", expand=True)
        changed()
        self.vars[key] = var
        if tip:
            ttk.Label(parent, text=tip, foreground=MUTED, wraplength=640).pack(anchor="w", padx=(4, 0))

    def _check(self, parent, key, label):
        var = tk.BooleanVar(value=getattr(self.cfg, key))
        var.trace_add("write", lambda *_: setattr(self.cfg, key, var.get()))
        ttk.Checkbutton(parent, text=label, variable=var).pack(anchor="w", pady=3)
        self.vars[key] = var

    def _tab_detection(self, nb):
        f = self._scroll_frame(nb, "Detection")
        g = ttk.LabelFrame(f, text=" Automatique ", padding=8)
        g.pack(fill="x", pady=4)
        self._check(g, "auto_calibrate", "Calibrage automatique au demarrage (trouve le bouchon + sa couleur)")
        self._check(g, "bubble_motion", "Detecter les bulles aussi par mouvement (sans pack de textures)")
        self._slider(g, "motion_threshold", "Seuil de mouvement", 5, 100, 1)
        self._slider(g, "calib_wait", "Attente apres lancer (calibrage, s)", 1, 6, 0.1)
        g = ttk.LabelFrame(f, text=" Bouchon ", padding=8)
        g.pack(fill="x", pady=4)
        self._slider(g, "dip_threshold", "Enfoncement (tailles de bouchon)", 0.1, 2.0, 0.05,
                     "Distance verticale, relative a la taille du bouchon, qui donne un signal 'dip' plein.")
        self._slider(g, "lost_ratio", "Disparu si aire <", 0.05, 0.9, 0.05)
        self._slider(g, "lost_frames", "Images 'disparu' consecutives", 1, 10, 1)
        self._slider(g, "bobber_min_area", "Aire minimale (px)", 1, 80, 1)
        g = ttk.LabelFrame(f, text=" Bulles autour de la canne ", padding=8)
        g.pack(fill="x", pady=4)
        self._slider(g, "radius_factor", "Rayon (x taille bouchon)", 1, 20, 0.5)
        self._slider(g, "radius_px", "Rayon : marge fixe (px)", 0, 150, 1)
        self._slider(g, "bubble_threshold", "Pixels de bulles = signal plein", 1, 200, 1)
        self._slider(g, "approach_min", "Pixels mini pour 'approche'", 1, 50, 1,
                     "L'approche est detectee quand la trainee de bulles se rapproche du bouchon "
                     "image apres image (regression sur la distance moyenne).")
        g = ttk.LabelFrame(f, text=" Fusion des signaux ", padding=8)
        g.pack(fill="x", pady=4)
        self._slider(g, "w_dip", "Poids enfoncement", 0, 2, 0.05)
        self._slider(g, "w_bubble", "Poids bulles", 0, 2, 0.05)
        self._slider(g, "w_lost", "Poids disparition", 0, 2, 0.05)
        self._slider(g, "trigger_score", "Seuil de declenchement", 0.2, 3, 0.05)
        self._slider(g, "confirm_frames", "Images de confirmation", 1, 8, 1)
        self._slider(g, "stable_frames", "Images stables avant armement", 1, 40, 1)
        self._slider(g, "settle_time", "Ignorer apres le lancer (s)", 0.5, 6, 0.1)

    def _tab_colors(self, nb):
        f = self._scroll_frame(nb, "Couleurs")
        ttk.Label(f, foreground=MUTED, wraplength=660, text=(
            "Avec le pack de textures fourni : bouchon MAGENTA (300 deg), bulles JAUNES (55 deg). "
            "Utilisez la pipette dans l'onglet Apercu pour calibrer automatiquement.")).pack(anchor="w")
        self.swatches = {}
        for prefix, title in (("bobber", " Bouchon "), ("bubble", " Bulles / eclaboussures ")):
            g = ttk.LabelFrame(f, text=title, padding=8)
            g.pack(fill="x", pady=6)
            sw = tk.Canvas(g, width=60, height=24, highlightthickness=0)
            sw.pack(anchor="e")
            self.swatches[prefix] = sw
            self._slider(g, prefix + "_hue", "Teinte (deg)", 0, 359, 1)
            self._slider(g, prefix + "_hue_tol", "Tolerance teinte", 2, 60, 1)
            self._slider(g, prefix + "_smin", "Saturation mini", 0, 1, 0.01)
            self._slider(g, prefix + "_vmin", "Luminosite mini", 0, 1, 0.01)
        self._update_swatches()

    def _tab_bot(self, nb):
        f = self._scroll_frame(nb, "Bot")
        g = ttk.LabelFrame(f, text=" Rythme (comportement humain) ", padding=8)
        g.pack(fill="x", pady=4)
        self._slider(g, "react_min", "Reaction mini (s)", 0, 1, 0.01)
        self._slider(g, "react_max", "Reaction maxi (s)", 0, 1.5, 0.01)
        self._slider(g, "recast_min", "Relance mini (s)", 0.2, 3, 0.05)
        self._slider(g, "recast_max", "Relance maxi (s)", 0.2, 4, 0.05)
        self._slider(g, "fps", "Images par seconde", 5, 60, 1)
        g = ttk.LabelFrame(f, text=" Securite ", padding=8)
        g.pack(fill="x", pady=4)
        self._slider(g, "max_wait", "Relancer apres (s) sans touche", 10, 120, 1)
        self._slider(g, "no_bobber_timeout", "Relancer si bouchon perdu (s)", 1, 20, 0.5)
        self._slider(g, "start_delay", "Compte a rebours (s)", 0, 10, 0.5)
        self._slider(g, "stop_after_catches", "Arret apres N prises (0 = jamais)", 0, 1000, 1)
        self._slider(g, "stop_after_minutes", "Arret apres N minutes (0 = jamais)", 0, 600, 1)
        self._check(g, "cast_on_start", "Lancer la ligne au demarrage")
        self._check(g, "require_focus", "Pause si Minecraft n'est pas au premier plan (Windows)")
        g = ttk.LabelFrame(f, text=" Profil ", padding=8)
        g.pack(fill="x", pady=4)
        row = ttk.Frame(g)
        row.pack(fill="x")
        ttk.Button(row, text="Sauvegarder", command=self._save).pack(side="left")
        ttk.Button(row, text="Exporter...", command=self._export).pack(side="left", padx=6)
        ttk.Button(row, text="Importer...", command=self._import).pack(side="left")
        ttk.Button(row, text="Valeurs par defaut", command=self._defaults).pack(side="left", padx=6)
        ttk.Label(g, text="Fichier : " + CONFIG_PATH, foreground=MUTED).pack(anchor="w", pady=(6, 0))

    def _tab_log(self, nb):
        f = ttk.Frame(nb, padding=8)
        nb.add(f, text="Journal")
        self.log = tk.Text(f, bg="#0e1014", fg=FG, insertbackground=FG, font=("Consolas", 10),
                           relief="flat", state="disabled")
        self.log.pack(fill="both", expand=True)
        self.log.tag_configure("bite", foreground="#ef5350")
        self.log.tag_configure("warn", foreground="#ffa726")

    # --- actions --------------------------------------------------------
    def _hotkeys(self):
        try:
            from pynput import keyboard
            target = getattr(keyboard.Key, self.cfg.hotkey_toggle, keyboard.Key.f8)

            def on_press(key):
                if key == target:
                    self.after(0, self.bot.toggle)
            keyboard.Listener(on_press=on_press, daemon=True).start()
        except Exception as e:
            self._log("Raccourci clavier indisponible : %r" % e)

    def _select_roi(self):
        top = tk.Toplevel(self)
        top.attributes("-fullscreen", True)
        top.attributes("-alpha", 0.30)
        top.attributes("-topmost", True)
        top.configure(bg="black")
        cv = tk.Canvas(top, bg="black", highlightthickness=0, cursor="crosshair")
        cv.pack(fill="both", expand=True)
        cv.create_text(top.winfo_screenwidth() // 2, 40, fill="white", font=("Segoe UI", 18),
                       text="Glissez un rectangle autour de la zone de peche (Echap = annuler)")
        st = {}

        def down(e):
            st["x0"], st["y0"] = e.x_root, e.y_root
            st["r"] = cv.create_rectangle(e.x, e.y, e.x, e.y, outline="#4fc3f7", width=3)

        def move(e):
            if "r" in st:
                cv.coords(st["r"], st["x0"] - top.winfo_rootx(), st["y0"] - top.winfo_rooty(), e.x, e.y)

        def up(e):
            x0, y0 = min(st["x0"], e.x_root), min(st["y0"], e.y_root)
            w, h = abs(e.x_root - st["x0"]), abs(e.y_root - st["y0"])
            top.destroy()
            if w > 20 and h > 20:
                self.cfg.roi_x, self.cfg.roi_y, self.cfg.roi_w, self.cfg.roi_h = x0, y0, w, h
                self._update_roi_lbl()
                self._log("Zone : %dx%d en (%d, %d)" % (w, h, x0, y0))
        cv.bind("<Button-1>", down)
        cv.bind("<B1-Motion>", move)
        cv.bind("<ButtonRelease-1>", up)
        top.bind("<Escape>", lambda e: top.destroy())
        top.focus_force()

    def _auto_roi(self):
        self.cfg.roi_w = self.cfg.roi_h = 0
        self._update_roi_lbl()

    def _update_roi_lbl(self):
        c = self.cfg
        txt = "Zone : centrale %dx%d" % (c.auto_roi_w, c.auto_roi_h) if not c.roi_w else \
            "Zone : %dx%d @ %d,%d" % (c.roi_w, c.roi_h, c.roi_x, c.roi_y)
        self.roi_lbl.configure(text=txt)

    def _pick(self, e, which):
        if self.last_frame is None or self.preview_img is None:
            return
        frame = self.last_frame
        scale = frame.shape[1] / self.preview_img.width()
        ox = (self.canvas.winfo_width() - self.preview_img.width()) // 2
        oy = (self.canvas.winfo_height() - self.preview_img.height()) // 2
        x, y = int((e.x - ox) * scale), int((e.y - oy) * scale)
        if not (0 <= x < frame.shape[1] and 0 <= y < frame.shape[0]):
            return
        patch = frame[max(0, y - 1):y + 2, max(0, x - 1):x + 2].reshape(-1, 3)
        h, s, v = rgb_to_hsv(patch.reshape(1, -1, 3))
        i = int(s.argmax())  # pixel le plus sature du voisinage
        hue, sat, val = float(h[0, i]), float(s[0, i]), float(v[0, i])
        self.vars[which + "_hue"].set(round(hue))
        self.vars[which + "_smin"].set(round(max(0.2, sat * 0.6), 2))
        self.vars[which + "_vmin"].set(round(max(0.1, val * 0.45), 2))
        self._log("Pipette %s : teinte %.0f, sat %.2f, lum %.2f" % (
            "bouchon" if which == "bobber" else "bulles", hue, sat, val))

    def _update_swatches(self):
        import colorsys
        for prefix, sw in getattr(self, "swatches", {}).items():
            r, g, b = colorsys.hsv_to_rgb(getattr(self.cfg, prefix + "_hue") / 360.0, 1, 1)
            sw.configure(bg="#%02x%02x%02x" % (int(r * 255), int(g * 255), int(b * 255)))

    def _reset_stats(self):
        debut = self.bot.stats["debut"]
        self.bot.reset_stats()
        self.bot.stats["debut"] = debut if self.bot.active.is_set() else None

    def _save(self):
        self.cfg.save()
        self._log("Configuration sauvegardee.")

    def _export(self):
        p = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON", "*.json")])
        if p:
            self.cfg.save(p)

    def _import(self):
        p = filedialog.askopenfilename(filetypes=[("JSON", "*.json")])
        if p:
            self._apply(Config.load(p))

    def _defaults(self):
        if messagebox.askyesno("Defaut", "Revenir aux reglages par defaut ?"):
            self._apply(Config())

    def _apply(self, new):
        for k, v in vars(new).items():
            setattr(self.cfg, k, v)
            if k in self.vars:
                self.vars[k].set(v)
        self._update_roi_lbl()

    def _sync_vars(self):
        for k, var in self.vars.items():
            var.set(getattr(self.cfg, k))
        self._update_roi_lbl()
        self.cfg.save()

    def _log(self, msg):
        tag = "bite" if "TOUCHE" in msg else "warn" if ("Relance" in msg or "Erreur" in msg) else ""
        self.log.configure(state="normal")
        self.log.insert("end", msg + "\n", tag)
        self.log.see("end")
        self.log.configure(state="disabled")

    # --- boucle UI ------------------------------------------------------
    def _poll(self):
        last_frame = None
        try:
            while True:
                kind, data = self.events.get_nowait()
                if kind == "log":
                    self._log(data)
                elif kind == "calibrated":
                    self._sync_vars()
                elif kind == "state":
                    self._show_state(data)
                elif kind == "frame":
                    last_frame = data
        except queue.Empty:
            pass
        if last_frame is not None and self.nb.index("current") == 0:
            self._show_frame(*last_frame)
        r = self.bot.latest
        if r is not None and r is not self._shown:
            self._shown = r
            self._show_signal(r)
            if self.nb.index("current") == 1:
                self._draw_graph()
        self.after(60, self._poll)

    def _show_state(self, st):
        self.state_lbl.configure(text=st, foreground=STATE_COLORS.get(st, FG))
        running = st != STOPPED
        self.go_btn.configure(text="ARRETER  (F8)" if running else "DEMARRER  (F8)",
                              style="Stop.TButton" if running else "Go.TButton")
        self.hint.configure(text={
            STARTING: "Basculez sur Minecraft, canne a peche en main, visez l'eau.",
            CALIBRATING: "Calibrage : ne bougez pas, le bot lance et ramene la ligne.",
            SETTLING: "Le bouchon se stabilise...",
            WAITING: "En attente d'une touche.",
            APPROACH: "Un poisson arrive !",
            REELING: "Ferrage !",
            PAUSED: "Minecraft n'est pas au premier plan.",
        }.get(st, ""))

    def _show_frame(self, pil, raw, r):
        self.last_frame = raw
        cw, ch = max(self.canvas.winfo_width(), 50), max(self.canvas.winfo_height(), 50)
        k = min(cw / pil.width, ch / pil.height)
        if abs(k - 1) > 0.02:
            pil = pil.resize((max(1, int(pil.width * k)), max(1, int(pil.height * k))), 0)
        self.preview_img = ImageTk.PhotoImage(pil)
        self.canvas.delete("all")
        self.canvas.create_image(cw // 2, ch // 2, image=self.preview_img)

    def _show_signal(self, r):
        self.live_lbl.configure(text="bouchon:%s  dip:%.2f  bulles:%3d  disparu:%s  approche:%s  arme:%s  score:%.2f" % (
            "oui" if r.bobber else "non", r.dip, r.bubbles, "oui" if r.lost else "non",
            "oui" if r.approach else "non", "oui" if r.armed else "non", r.score))
        g = self.gauge
        g.delete("all")
        frac = min(r.score / max(self.cfg.trigger_score * 1.5, 0.01), 1.0)
        col = "#ef5350" if r.score >= self.cfg.trigger_score else "#ffa726" if r.approach else ACC
        g.create_rectangle(0, 0, 210 * frac, 18, fill=col, width=0)
        tx = 210 / 1.5
        g.create_line(tx, 0, tx, 18, fill="white")

    def _draw_graph(self):
        g = self.graph
        w, h = g.winfo_width(), g.winfo_height()
        hist = list(self.history)  # copie : le bot ecrit en parallele
        if w < 20 or not hist:
            return
        g.delete("all")
        top = max(self.cfg.trigger_score * 2.0, 0.5)
        y = lambda v: h - 10 - (h - 20) * min(v, top) / top
        for i in range(1, 4):
            g.create_line(0, y(top * i / 4), w, y(top * i / 4), fill="#222831")
        g.create_line(0, y(self.cfg.trigger_score), w, y(self.cfg.trigger_score), fill="#ef5350", dash=(6, 4))
        n = self.history.maxlen
        dx = w / (n - 1)
        x0 = w - dx * (len(hist) - 1)
        for idx, (_, col) in reversed(list(enumerate(SERIES))):
            pts = []
            for j, row in enumerate(hist):
                pts += [x0 + j * dx, y(row[idx])]
            if len(pts) >= 4:
                g.create_line(*pts, fill=col, width=2 if idx == 0 else 1)

    def _tick_stats(self):
        s = self.bot.stats
        el = time.time() - s["debut"] if s["debut"] else 0
        self.stat_lbls["prises"].configure(text=str(s["prises"]))
        self.stat_lbls["lancers"].configure(text=str(s["lancers"]))
        self.stat_lbls["timeouts"].configure(text=str(s["timeouts"]))
        self.stat_lbls["taux"].configure(text="%.0f" % (s["prises"] * 3600 / el) if el > 30 else "-")
        self.stat_lbls["attente"].configure(text="%.1f s" % s["attente_moy"] if s["prises"] else "-")
        self.stat_lbls["duree"].configure(text=time.strftime("%H:%M:%S", time.gmtime(el)))
        self.stat_lbls["fps"].configure(text=str(self.bot.frames))
        self.bot.frames = 0
        self.after(1000, self._tick_stats)

    def _close(self):
        self.cfg.save()
        self.bot.shutdown()
        self.destroy()


if __name__ == "__main__":
    App().mainloop()
