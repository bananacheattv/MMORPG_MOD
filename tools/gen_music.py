# -*- coding: utf-8 -*-
"""Musique de la cinematique d'arrivee du hub (« Eldoria ») : piece originale synthetisee (OGG Vorbis stereo, ~70 s), en re majeur,
a 82 mesures/min, calee sur la ligne de temps de la cinematique : survol de la citadelle (mesures 0-6), puis une section par classe
(Guerrier : bois graves et tambours ; Mage : celesta et chœur ; Paladin : cors et timbales ; Archer : cordes pincees et flute), puis
le retour au joueur (mesures 19-23) qui s'acheve sur l'accord de re majeur.

Dependances : numpy, soundfile. Sortie : assets/mmorpg/sounds/music/eldoria_theme.ogg et assets/mmorpg/sounds.json
Appele par tools/gen_hub.py ; lancer ce fichier seul regenere la piece.
"""
import json
import math
import os

import numpy as np
import soundfile as sf

SR = 44100
BPM = 82.3
BEAT = 60.0 / BPM
BAR = 4 * BEAT
BARS = 24
TOTAL = BARS * BAR
RNG = np.random.default_rng(2026)
N = int(TOTAL * SR) + SR * 4

NOTES = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}


def hz(name):
    letter = name[0]
    acc = 1 if '#' in name else (-1 if 'b' in name else 0)
    octave = int(name[-1])
    semis = NOTES[letter] + acc + (octave - 4) * 12 - 9
    return 440.0 * 2 ** (semis / 12.0)


def tt(dur):
    return np.arange(int(dur * SR)) / SR


def adsr(n, a, d, s, r):
    a, d, r = max(int(a * SR), 1), max(int(d * SR), 1), max(int(r * SR), 1)
    sus = max(0, n - a - d - r)
    e = np.concatenate([np.linspace(0, 1, a), np.linspace(1, s, d), np.full(sus, s), np.linspace(s, 0, r)])
    return e[:n] if len(e) >= n else np.pad(e, (0, n - len(e)))


def lowpass(x, cutoff):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X *= 1 / (1 + (f / cutoff) ** 4)
    return np.fft.irfft(X, len(x))


def formant(x, centers, width=0.18):
    """Filtre a formants (voyelle) : pics gaussiens autour des frequences donnees."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    g = np.zeros_like(f)
    for c, amp in centers:
        g += amp * np.exp(-0.5 * ((f - c) / (c * width)) ** 2)
    return np.fft.irfft(X * (g + 0.02), len(x))


def saw(freq, dur, detune=(0.0,), vib=0.0):
    t = tt(dur)
    out = np.zeros_like(t)
    for d in detune:
        f = freq * 2 ** (d / 1200.0)
        ph = 2 * np.pi * f * t + (vib * np.sin(2 * np.pi * 5.2 * t) if vib else 0)
        for h in range(1, int(min(16, SR / 2 / f))):
            out += np.sin(ph * h + h) / h
    return out / (len(detune) * 2.2)


# ------------------------------------------------------------------ pistes (stereo) et envoi de reverberation
L = np.zeros(N)
R = np.zeros(N)
SEND = np.zeros(N)


def put(sig, t0, gain=1.0, pan=0.0, send=0.3):
    i = int(t0 * SR)
    if i < 0 or i >= N:
        return
    sig = sig[:N - i]
    gl = math.cos((pan + 1) * math.pi / 4) * gain
    gr = math.sin((pan + 1) * math.pi / 4) * gain
    L[i:i + len(sig)] += sig * gl
    R[i:i + len(sig)] += sig * gr
    SEND[i:i + len(sig)] += sig * gain * send


# ------------------------------------------------------------------ instruments
def strings(note, dur, gain=0.5, pan=0.0):
    f = hz(note)
    s = saw(f, dur + 0.5, detune=(-8, 0, 7), vib=0.0035)
    s = lowpass(s, 3800)
    n = len(s)
    return s * adsr(n, 1.1, 0.4, 0.85, 1.4)


def add_strings(note, t0, dur, gain=0.5, pan=0.0):
    put(strings(note, dur, gain), t0, gain, pan, 0.45)


def add_choir(note, t0, dur, gain=0.4, pan=0.0):
    f = hz(note)
    s = saw(f, dur + 0.6, detune=(-14, -5, 5, 14), vib=0.006)
    s = formant(s, [(650, 1.0), (1080, 0.7), (2650, 0.25)])
    s = s / (np.max(np.abs(s)) + 1e-6)
    put(s * adsr(len(s), 1.4, 0.5, 0.8, 1.6), t0, gain, pan, 0.6)


def add_brass(note, t0, dur, gain=0.5, pan=0.0):
    f = hz(note)
    s = saw(f, dur + 0.3, detune=(-4, 3))
    s = lowpass(s, 1500)
    s = s / (np.max(np.abs(s)) + 1e-6)
    put(s * adsr(len(s), 0.12, 0.2, 0.8, 0.35), t0, gain, pan, 0.3)


def add_pluck(note, t0, gain=0.5, pan=0.0, dur=1.6, bright=1.0):
    f = hz(note)
    t = tt(dur)
    s = np.zeros_like(t)
    for h in range(1, 9):
        s += np.sin(2 * np.pi * f * h * t) / h * np.exp(-t * (3.5 + h * 1.4 / bright))
    s *= np.minimum(1, t / 0.004)
    put(s / 2.4, t0, gain, pan, 0.5)


def add_bell(note, t0, gain=0.35, pan=0.0, dur=3.5):
    f = hz(note)
    t = tt(dur)
    s = np.zeros_like(t)
    for ratio, amp, dec in ((1, 1.0, 1.4), (2.76, 0.5, 2.2), (5.4, 0.28, 3.4), (8.9, 0.15, 5.0)):
        s += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t * dec)
    s *= np.minimum(1, t / 0.002)
    put(s / 1.9, t0, gain, pan, 0.8)


def add_flute(note, t0, dur, gain=0.35, pan=0.0):
    f = hz(note)
    t = tt(dur + 0.2)
    ph = 2 * np.pi * f * t + 0.012 * np.sin(2 * np.pi * 5 * t)
    s = np.sin(ph) + 0.25 * np.sin(2 * ph) + 0.08 * np.sin(3 * ph)
    breath = lowpass(RNG.standard_normal(len(t)), 5000) * 0.05
    put((s + breath) * adsr(len(t), 0.08, 0.1, 0.8, 0.2), t0, gain, pan, 0.5)


def add_timpani(t0, note='D2', gain=0.8):
    f = hz(note)
    t = tt(1.4)
    pitch = f * (1 + 0.8 * np.exp(-t * 18))
    s = np.sin(2 * np.pi * np.cumsum(pitch) / SR) * np.exp(-t * 3.2)
    s += lowpass(RNG.standard_normal(len(t)), 900) * np.exp(-t * 28) * 0.5
    put(s, t0, gain * 0.5, -0.15, 0.35)


def add_taiko(t0, gain=0.9, note='D2'):
    f = hz(note) * 0.8
    t = tt(1.0)
    s = np.sin(2 * np.pi * np.cumsum(f * (1 + 1.4 * np.exp(-t * 24))) / SR) * np.exp(-t * 4.0)
    s += lowpass(RNG.standard_normal(len(t)), 2500) * np.exp(-t * 40) * 0.35
    put(s, t0, gain * 0.5, 0.1, 0.25)


def add_swell(t0, dur, gain=0.3):
    t = tt(dur)
    n = lowpass(RNG.standard_normal(len(t)), 3500)
    n = n * (np.linspace(0, 1, len(t)) ** 2.2)
    put(n * 0.5, t0, gain, 0, 0.5)


# ------------------------------------------------------------------ partition
CHORDS = {
    'Dsus2': ['D3', 'A3', 'E4', 'A4'], 'D': ['D3', 'A3', 'D4', 'F#4'], 'Bm': ['B2', 'F#3', 'B3', 'D4'],
    'G': ['G2', 'D3', 'G3', 'B3'], 'A': ['A2', 'E3', 'A3', 'C#4'], 'Em': ['E3', 'B3', 'E4', 'G4'],
    'Gmaj7': ['G2', 'D3', 'B3', 'F#4'], 'A/C#': ['C#3', 'E3', 'A3', 'C#4'],
}
PROG = (['Dsus2', 'D', 'Bm', 'G', 'D', 'A', 'A'] +                 # 0-6 : survol de la citadelle
        ['Bm', 'G', 'A'] +                                          # 7-9 : Guerrier
        ['Gmaj7', 'Em', 'Bm'] +                                     # 10-12 : Mage
        ['D', 'A/C#', 'G'] +                                        # 13-15 : Paladin
        ['Em', 'G', 'A'] +                                          # 16-18 : Archer
        ['G', 'A', 'Bm', 'G'] +                                     # 19-22 : retour
        ['D'])                                                      # 23 : resolution

MELODY = [   # (mesure, temps, note, duree en temps)
    (4, 0, 'A4', 2), (4, 2, 'D5', 1), (4, 3, 'E5', 1), (5, 0, 'F#5', 3), (5, 3, 'E5', 1), (6, 0, 'A5', 4),
    (7, 0, 'B4', 2), (7, 2, 'D5', 2), (8, 0, 'D5', 3), (8, 3, 'B4', 1), (9, 0, 'C#5', 4),
    (10, 0, 'D5', 2), (10, 2, 'F#5', 2), (11, 0, 'E5', 3), (11, 3, 'B4', 1), (12, 0, 'D5', 4),
    (13, 0, 'F#4', 1), (13, 1, 'A4', 1), (13, 2, 'D5', 2), (14, 0, 'C#5', 2), (14, 2, 'E5', 2), (15, 0, 'D5', 4),
    (16, 0, 'G4', 2), (16, 2, 'B4', 2), (17, 0, 'D5', 2), (17, 2, 'B4', 2), (18, 0, 'C#5', 4),
    (19, 0, 'B4', 2), (19, 2, 'D5', 2), (20, 0, 'E5', 2), (20, 2, 'C#5', 2), (21, 0, 'D5', 3), (21, 3, 'F#5', 1), (22, 0, 'A5', 4),
    (23, 0, 'D5', 4),
]


def bar_t(bar, beat=0.0):
    return (bar + beat / 4.0) * BAR


def compose():
    for b, name in enumerate(PROG):
        ch = CHORDS[name]
        t0 = bar_t(b)
        quiet = 0.55 if b < 2 else 1.0
        for i, n in enumerate(ch):
            add_strings(n, t0, BAR + 0.4, 0.2 * quiet, pan=-0.5 + i * 0.33)
        # harpe : arpege sur la mesure (accord + octave des deux notes medianes)
        up = lambda n: n[:-1] + str(int(n[-1]) + 1)
        notes = ch + [up(ch[1]), up(ch[2])]
        for k in range(8):
            if b >= 2 or k % 2 == 0:
                add_pluck(notes[k % len(notes)], t0 + k * BEAT / 2, 0.16 * quiet, pan=-0.3 + 0.1 * (k % 5), dur=1.8)
    # choeur et melodie
    for (b, beat, note, dur) in MELODY:
        t0 = bar_t(b, beat)
        d = dur * BEAT
        if b < 7:
            add_choir(note, t0, d, 0.28, pan=0.1)
        elif b < 10:
            add_brass(note, t0, d, 0.34, pan=-0.2)
            add_brass(note[:-1] + str(int(note[-1]) - 1), t0, d, 0.18, pan=-0.35)
        elif b < 13:
            add_bell(note, t0, 0.3, pan=0.25)
            add_choir(note, t0, d, 0.22, pan=0.2)
        elif b < 16:
            add_brass(note[:-1] + str(int(note[-1]) - 1), t0, d, 0.36, pan=-0.1)
            add_choir(note, t0, d, 0.2, pan=0.1)
        elif b < 19:
            add_flute(note, t0, d, 0.32, pan=0.3)
            add_pluck(note[:-1] + str(int(note[-1]) + 1), t0, 0.12, pan=0.35, dur=1.2)
        else:
            add_choir(note, t0, d, 0.3, pan=0.0)
            add_strings(note, t0, d, 0.16, pan=0.2)
            add_bell(note[:-1] + str(int(note[-1]) + 1), t0, 0.16, pan=0.3)
    # percussions
    for b in range(0, 24):
        t0 = bar_t(b)
        if b in (4, 5):
            for k in range(4):
                add_timpani(t0 + k * BEAT, 'D2', 0.45 + 0.08 * (b - 4) + 0.04 * k)
        if b == 6:
            for k in range(8):
                add_timpani(t0 + k * BEAT / 2, 'A1', 0.4 + 0.07 * k)
            add_swell(t0, BAR, 0.5)
        if 7 <= b < 10:                                   # Guerrier : tambours
            add_taiko(t0, 0.85)
            add_taiko(t0 + 1.5 * BEAT, 0.5)
            add_taiko(t0 + 2 * BEAT, 0.7)
            add_taiko(t0 + 3.5 * BEAT, 0.45)
        if 13 <= b < 16:                                  # Paladin : timbales
            add_timpani(t0, 'D2', 0.8)
            add_timpani(t0 + 2 * BEAT, 'A1', 0.6)
            add_timpani(t0 + 3 * BEAT, 'D2', 0.5)
        if 19 <= b < 22:
            add_timpani(t0, 'D2', 0.55 + 0.1 * (b - 19))
            if b >= 20:
                add_taiko(t0 + 2 * BEAT, 0.5)
        if b == 22:
            add_swell(t0, BAR * 1.2, 0.5)
    # cloches d'ouverture et accord final
    for k, n in enumerate(['D5', 'A5', 'D6', 'F#6']):
        add_bell(n, 0.4 + k * 0.55, 0.28, pan=-0.3 + 0.2 * k, dur=5.0)
    final = bar_t(23)
    for n in ['D2', 'D3', 'A3', 'D4', 'F#4', 'A4', 'D5']:
        add_strings(n, final, 3.0, 0.22)
    for k, n in enumerate(['D5', 'F#5', 'A5', 'D6']):
        add_bell(n, final + 0.1 + k * 0.18, 0.3, pan=-0.2 + 0.15 * k, dur=6.0)


def reverb(x, seconds=3.2):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    ir = RNG.standard_normal(n) * np.exp(-t * 2.2)
    ir = lowpass(ir, 5200)
    ir[: int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))
    size = 1
    while size < len(x) + n:
        size *= 2
    y = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(x)]
    return y / (np.max(np.abs(ir)) * 30)


def render(path):
    compose()
    wetL = reverb(SEND)
    wetR = reverb(SEND[::-1])[::-1]
    outL = L + wetL * 0.9
    outR = R + wetR * 0.9
    out = np.stack([outL, outR], axis=1)[:int(TOTAL * SR) + int(1.0 * SR)]
    out = out - np.mean(out, axis=0)
    out = np.tanh(out * 1.0 / (np.percentile(np.abs(out), 99) + 1e-9) * 1.1) / np.tanh(1.1)   # compression douce : le niveau moyen monte sans ecreter
    out = out / np.max(np.abs(out)) * 0.7
    n = len(out)
    fade_in = int(0.8 * SR)
    fade_out = int(3.0 * SR)
    out[:fade_in] *= np.linspace(0, 1, fade_in)[:, None]
    out[-fade_out:] *= (np.linspace(1, 0, fade_out) ** 1.5)[:, None]
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = out.astype(np.float32)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=2, format='OGG', subtype='VORBIS') as f:   # par blocs : un seul gros bloc fait planter libvorbis sous Windows
        for i in range(0, len(data), 16384):
            f.write(data[i:i + 16384])
    return n / SR


def generate(assets_dir):
    path = os.path.join(assets_dir, 'sounds', 'music', 'eldoria_theme.ogg')
    secs = render(path)
    with open(os.path.join(assets_dir, 'sounds.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'cinematique.theme': {'category': 'music', 'sounds': [{'name': 'mmorpg:music/eldoria_theme', 'stream': True}]},
                   'hub.ambiance': {'category': 'music', 'sounds': [{'name': 'mmorpg:music/eldoria_hub', 'stream': True}]}}, f, indent=2)
        f.write('\n')
    print('musique generee : %.1f s' % secs)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    generate(os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'mmorpg'))
