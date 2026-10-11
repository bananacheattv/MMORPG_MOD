"""Boucle du bot : capture -> detection -> machine a etats -> clics."""
import queue
import random
import sys
import threading
import time

import numpy as np

import calibrate
from detector import Detector, annotate

STOPPED, STARTING, CASTING, SETTLING, WAITING, APPROACH, REELING, PAUSED, CALIBRATING = (
    "ARRETE", "DEMARRAGE", "LANCER", "STABILISATION", "ATTENTE", "APPROCHE", "FERRAGE", "PAUSE",
    "CALIBRAGE")


def foreground_title():
    if sys.platform != "win32":
        return None
    try:
        import ctypes
        user32 = ctypes.windll.user32
        hwnd = user32.GetForegroundWindow()
        n = user32.GetWindowTextLengthW(hwnd)
        buf = ctypes.create_unicode_buffer(n + 1)
        user32.GetWindowTextW(hwnd, buf, n + 1)
        return buf.value
    except Exception:
        return None


def game_focused(cfg):
    title = foreground_title()
    if title is None:
        return True
    t = title.lower()
    return any(k.strip() and k.strip().lower() in t for k in cfg.focus_titles.split(","))


class FishingBot(threading.Thread):
    def __init__(self, cfg, events: queue.Queue):
        super().__init__(daemon=True)
        self.cfg = cfg
        self.events = events
        self.detector = Detector(cfg)
        self.state = STOPPED
        self.active = threading.Event()
        self.quit = threading.Event()
        self.preview = True
        self.want_calib = False
        self.frames = 0
        self.last_preview = 0.0
        self.latest = None
        from collections import deque
        self.history = deque(maxlen=300)
        self.reset_stats()
        from pynput.mouse import Button, Controller
        self._mouse, self._btn = Controller(), Button.right

    # --- controle -------------------------------------------------------
    def reset_stats(self):
        self.stats = {"lancers": 0, "prises": 0, "timeouts": 0, "attente_moy": 0.0,
                      "debut": None, "derniere_prise": None}

    def start_fishing(self):
        if not self.active.is_set():
            self.stats["debut"] = time.time()
            self._set(STARTING)
            self.t_state = time.time()
            self.active.set()
            self.log("Demarrage dans %.0f s : placez-vous dans Minecraft, canne en main." % self.cfg.start_delay)

    def request_calibration(self):
        self.want_calib = True
        if not self.active.is_set():
            self.start_fishing()

    def stop_fishing(self):
        if self.active.is_set():
            self.active.clear()
            self._set(STOPPED)
            self.log("Arret.")

    def toggle(self):
        self.stop_fishing() if self.active.is_set() else self.start_fishing()

    def shutdown(self):
        self.active.clear()
        self.quit.set()

    # --- utilitaires ----------------------------------------------------
    def log(self, msg):
        self.events.put(("log", time.strftime("%H:%M:%S ") + msg))

    def _set(self, st):
        self.state = st
        self.t_state = time.time()
        self.events.put(("state", st))

    def _click(self):
        self._mouse.click(self._btn)

    def _cast(self):
        self._click()
        self._line_out()

    def _line_out(self):
        self.stats["lancers"] += 1
        self._set(SETTLING)
        self.t_cast = time.time()
        self.detector.reset(self.t_cast)

    def _sleep(self, a, b):
        end = time.time() + random.uniform(a, b)
        while time.time() < end and self.active.is_set():
            time.sleep(0.01)

    # --- boucle principale ----------------------------------------------
    def run(self):
        import mss
        with mss.mss() as sct:
            mon = sct.monitors[1]
            while not self.quit.is_set():
                if not self.active.is_set():
                    # Apercu meme a l'arret pour le reglage des couleurs
                    self._frame(sct, mon, live=False)
                    time.sleep(0.15)
                    continue
                t0 = time.time()
                try:
                    self._step(sct, mon)
                except Exception as e:  # ne jamais tuer le thread
                    self.log("Erreur : %r" % e)
                    time.sleep(0.5)
                rest = 1.0 / max(self.cfg.fps, 1) - (time.time() - t0)
                if rest > 0.002:
                    time.sleep(rest)

    def _grab(self, sct, mon):
        x, y, w, h = self.cfg.roi(mon["width"], mon["height"])
        shot = sct.grab({"left": mon["left"] + x, "top": mon["top"] + y, "width": w, "height": h})
        return np.asarray(shot)[:, :, 2::-1].copy()  # BGRA -> RGB

    def _frame(self, sct, mon, live=True):
        frame = self._grab(sct, mon)
        now = time.time()
        r = self.detector.process(frame, now)
        self.frames += 1
        # Le rendu de l'apercu est limite a ~8 i/s pour ne pas ralentir la detection
        if self.preview and now - self.last_preview > 0.12 and self.events.qsize() < 20:
            self.last_preview = now
            r.bobber_mask, r.bubble_mask = r.bobber_mask.copy(), r.bubble_mask.copy()
            self.events.put(("frame", (annotate(frame, r), frame, r)))
        self.latest = r
        sg = r.signals
        c = self.cfg
        self.history.append((r.score, sg.get("dip", 0) * c.w_dip, sg.get("bubble", 0) * c.w_bubble,
                             sg.get("lost", 0) * c.w_lost))
        return r

    def _calibrate(self, sct, mon):
        self._set(CALIBRATING)
        for essai in range(1, 4):
            try:
                ok = calibrate.run(self, sct, mon)
            except InterruptedError:
                return
            if ok:
                self.detector.bg = None
                self._line_out()
                return
            self.log("Nouvel essai de calibrage (%d/3)..." % essai)
            self._sleep(1.0, 1.5)
            if not self.active.is_set():
                return
        self.log("Calibrage impossible : visez l'eau, canne en main, et activez le pack de textures.")
        self.stop_fishing()

    def _step(self, sct, mon):
        c, now = self.cfg, time.time()
        s = self.stats

        if c.stop_after_catches and s["prises"] >= c.stop_after_catches:
            self.log("Objectif de %d prises atteint." % c.stop_after_catches)
            return self.stop_fishing()
        if c.stop_after_minutes and s["debut"] and now - s["debut"] >= c.stop_after_minutes * 60:
            self.log("Duree maximale atteinte.")
            return self.stop_fishing()

        if self.state == STARTING:
            self._frame(sct, mon)
            if now - self.t_state >= c.start_delay:
                if c.auto_calibrate or self.want_calib:
                    self.want_calib = False
                    self._calibrate(sct, mon)
                elif c.cast_on_start:
                    self.log("Lancer.")
                    self._cast()
                else:
                    self._set(SETTLING)
                    self.t_cast = now
                    self.detector.reset(now)
            return

        if c.require_focus and not game_focused(c):
            if self.state != PAUSED:
                self._prev_state = self.state
                self._set(PAUSED)
                self.log("Fenetre du jeu pas au premier plan (fenetre active : \"%s\") : pause. "
                         "Ajoutez ce nom dans Bot > Fenetres du jeu, ou decochez la pause." % foreground_title())
            time.sleep(0.2)
            return
        if self.state == PAUSED:
            self.log("Reprise.")
            self._set(self._prev_state)
            self.detector.reset(time.time())

        if self.want_calib:
            self.want_calib = False
            return self._calibrate(sct, mon)

        r = self._frame(sct, mon)
        waited = now - self.t_cast

        if self.state == SETTLING and r.armed:
            self._set(WAITING)
        if self.state in (WAITING, APPROACH):
            if r.approach and self.state == WAITING:
                self._set(APPROACH)
                self.log("Poisson en approche (trainee de bulles).")
            elif not r.approach and self.state == APPROACH and r.bubbles == 0:
                self._set(WAITING)

        if r.bite and self.state in (WAITING, APPROACH):
            self._set(REELING)
            self.log("TOUCHE ! score=%.2f (dip %.2f, bulles %d%s)" % (
                r.score, r.dip, r.bubbles, ", disparu" if r.lost else ""))
            self._sleep(c.react_min, c.react_max)
            self._click()
            n = s["prises"]
            s["attente_moy"] = (s["attente_moy"] * n + waited) / (n + 1)
            s["prises"] += 1
            s["derniere_prise"] = time.time()
            self.events.put(("stats", dict(s)))
            self._sleep(c.recast_min, c.recast_max)
            if self.active.is_set():
                self._cast()
            return

        if waited > c.max_wait or (waited > c.settle_time
                                   and self.detector.seconds_without_bobber(now) > c.no_bobber_timeout):
            why = "delai depasse" if waited > c.max_wait else "bouchon introuvable"
            self.log("Relance (%s)." % why)
            s["timeouts"] += 1
            self.events.put(("stats", dict(s)))
            if c.auto_calibrate and why == "bouchon introuvable":
                return self._calibrate(sct, mon)
            if self.detector.base_y is not None:  # canne lancee : on la ramene d'abord
                self._click()
                self._sleep(c.recast_min, c.recast_max)
            self._cast()
