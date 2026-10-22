# -*- coding: utf-8 -*-
"""Musique d'ambiance du hub d'Eldoria (« Citadelle celeste ») : boucle de ~2 min 20 en re lydien, a 72 mesures/min.

Instruments synthetises : nappe chaude (saw desaccordees filtrees), harpe (partiels additifs a decroissance propre), celesta, chœur
« ah » (formants), basse profonde, souffle d'air ; un echo en cascade et une reverberation de salle (reponse impulsionnelle
bruitee, 3 s) donnent l'espace. Structure : intro de nappes, theme de harpe, melodie de celesta avec chœur, retour au calme.

Dependances : numpy, soundfile. Sortie : assets/mmorpg/sounds/music/eldoria_hub.ogg (declare dans sounds.json par gen_music.py).
Pour utiliser une autre musique : remplacer ce fichier OGG par le votre (meme nom), rien d'autre a changer.
"""
import os

import numpy as np
import soundfile as sf

import gen_music as gm

SR = gm.SR
BPM = 72.0
BEAT = 60.0 / BPM
BAR = 4 * BEAT
STEP = 2 * BAR                                  # un accord toutes les deux mesures
STEPS = 21
TOTAL = STEPS * STEP
N = int(TOTAL * SR) + SR * 5
RNG = np.random.default_rng(31415)

# progression en re lydien : accord = (basse, notes de nappe, notes de l'arpege, notes de melodie permises)
CHORDS = [
    ('D2', ['A3', 'D4', 'F#4', 'A4', 'E5'], ['D4', 'A4', 'F#4', 'E5', 'A4', 'F#4'], ['F#5', 'A5', 'E5', 'D5', 'B5']),
    ('B1', ['F#3', 'B3', 'D4', 'A4'], ['B3', 'F#4', 'D5', 'A4', 'F#4', 'D4'], ['D5', 'F#5', 'A5', 'B5', 'E5']),
    ('G1', ['D3', 'B3', 'F#4', 'C#5'], ['G3', 'D4', 'B4', 'F#4', 'D4', 'B3'], ['B5', 'F#5', 'C#6', 'D5', 'A5']),
    ('A1', ['E3', 'A3', 'B3', 'E4'], ['A3', 'E4', 'B4', 'E5', 'B4', 'E4'], ['E5', 'A5', 'B5', 'C#6', 'F#5']),
]
PROG = [0, 1, 2, 3] * 6


def chord_at(step):
    return CHORDS[PROG[step % len(PROG)]]


def stereo():
    return np.zeros(N), np.zeros(N)


def put(L, R, start, sig, pan=0.0, gain=1.0):
    i = int(start * SR)
    if i >= N:
        return
    n = min(len(sig), N - i)
    lg = gain * np.cos((pan + 1) * np.pi / 4)
    rg = gain * np.sin((pan + 1) * np.pi / 4)
    L[i:i + n] += sig[:n] * lg
    R[i:i + n] += sig[:n] * rg


# ------------------------------------------------------------------ instruments

def pad(note, dur, level=1.0):
    f = gm.hz(note)
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for det in (-7, 0, 6):
        ff = f * 2 ** (det / 1200.0)
        for h in range(1, 9):
            out += np.sin(2 * np.pi * ff * h * t + RNG.uniform(0, 6.28)) / h ** 1.15
    out *= 1 + 0.08 * np.sin(2 * np.pi * 0.23 * t + RNG.uniform(0, 6.28))
    out = gm.lowpass(out, 1500)
    return out * gm.adsr(len(t), 2.2, 0.8, 0.8, 2.6) * 0.045 * level


def sub(note, dur):
    f = gm.hz(note)
    t = gm.tt(dur)
    out = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t)
    return out * gm.adsr(len(t), 1.6, 0.5, 0.85, 2.2) * 0.12


def harp(note, vel=1.0):
    f = gm.hz(note)
    dur = 4.2
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for h in range(1, 12):
        fh = f * h * (1 + 0.0004 * h * h)
        if fh > 9000:
            break
        out += np.sin(2 * np.pi * fh * t) / h ** 1.25 * np.exp(-t * (0.9 + 0.55 * h))
    click = RNG.normal(0, 1, len(t)) * np.exp(-t * 90)
    out += gm.lowpass(click, 3200) * 0.06
    return out * 0.2 * vel


def bell(note, vel=1.0):
    f = gm.hz(note)
    dur = 5.0
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1, 1.0, 0.9), (2.0, 0.5, 1.3), (3.0, 0.22, 1.8), (4.16, 0.14, 2.4), (5.43, 0.08, 3.0)):
        out += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t * dec)
    attack = np.minimum(1, t * 600)
    return out * attack * 0.17 * vel


def choir(note, dur, level=1.0):
    f = gm.hz(note)
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for det in (-9, 0, 8):
        ff = f * 2 ** (det / 1200.0)
        out += np.sin(2 * np.pi * ff * t + 0.4 * np.sin(2 * np.pi * 5.1 * t)) + 0.5 * np.sin(2 * np.pi * 2 * ff * t) + 0.3 * np.sin(2 * np.pi * 3 * ff * t)
    out = gm.formant(out, [(800, 1.0), (1150, 0.55), (2900, 0.18)])
    return out * gm.adsr(len(t), 1.4, 0.4, 0.8, 1.8) * 0.02 * level


def air(dur, level=1.0):
    t = gm.tt(dur)
    n = RNG.normal(0, 1, len(t))
    n = gm.lowpass(n, 1100)
    slow = 0.5 + 0.5 * np.sin(2 * np.pi * 0.07 * t + 1.0) * np.sin(2 * np.pi * 0.031 * t)
    return n * slow * gm.adsr(len(t), 6, 1, 0.9, 6) * 0.05 * level


# ------------------------------------------------------------------ espace

def reverb(L, R, wet=0.3):
    n = int(3.2 * SR)
    t = np.arange(n) / SR
    out = []
    for ch in (L, R):
        ir = RNG.normal(0, 1, n) * np.exp(-t * 1.5)
        ir = gm.lowpass(ir, 6500) * (0.4 + 0.6 * np.exp(-t * 4))
        ir[0] = 0
        size = 1
        while size < len(ch) + n:
            size *= 2
        conv = np.fft.irfft(np.fft.rfft(ch, size) * np.fft.rfft(ir, size), size)[:len(ch)]
        out.append(conv / (np.max(np.abs(conv)) + 1e-9) * np.max(np.abs(ch)) * wet)
    return L * (1 - wet * 0.4) + out[0], R * (1 - wet * 0.4) + out[1]


def echo(sig, delay, fb, mix):
    d = int(delay * SR)
    out = sig.copy()
    gain = fb
    for k in range(1, 6):
        i = d * k
        if i >= len(sig):
            break
        out[i:] += sig[:-i] * gain * mix
        gain *= fb
    return out


# ------------------------------------------------------------------ composition

def render(path):
    L, R = stereo()
    harpL, harpR = stereo()
    bellL, bellR = stereo()
    for s in range(STEPS):
        t0 = s * STEP
        bass, pads, arp, mel = chord_at(s)
        last = s >= STEPS - 3
        # nappes et basse : toujours presentes, plus legeres a la fin
        lvl = 0.7 if last else 1.0
        put(L, R, t0, sub(bass, STEP + 2.5), 0.0, lvl)
        for i, nname in enumerate(pads):
            put(L, R, t0 + i * 0.12, pad(nname, STEP + 3.0, lvl), -0.7 + 1.4 * i / max(1, len(pads) - 1))
        if s % 6 == 0 and s > 0:
            put(L, R, t0 + 0.5, air(STEP * 2), 0.0, 1.0)
        # arpeges de harpe : a partir du 5e accord, dessinent l'accord en croches
        if 4 <= s < STEPS - 3:
            density = 1.0 if s >= 8 else 0.7
            for i in range(12):
                if RNG.random() > density:
                    continue
                note = arp[i % len(arp)]
                if i >= 6:
                    note = arp[(len(arp) - 1 - (i % len(arp)))]
                put(harpL, harpR, t0 + i * BEAT * 0.66, harp(note, 0.55 + 0.45 * RNG.random()), RNG.uniform(-0.6, 0.6))
        # melodie de celesta : marche aleatoire sur les notes permises, rythmes a deux ou trois temps
        if 10 <= s < STEPS - 3:
            t = t0
            prev = RNG.integers(len(mel))
            while t < t0 + STEP - BEAT:
                prev = int(np.clip(prev + RNG.integers(-1, 2), 0, len(mel) - 1))
                dur = float(RNG.choice([1.0, 1.5, 2.0])) * BEAT
                if RNG.random() < 0.75:
                    put(bellL, bellR, t, bell(mel[prev], 0.6 + 0.4 * RNG.random()), RNG.uniform(-0.4, 0.4))
                t += dur
            if s >= 12:
                put(L, R, t0 + 0.3, choir(pads[-2], STEP + 1.5, 1.0), 0.0)
                put(L, R, t0 + 0.3, choir(pads[1], STEP + 1.5, 0.8), 0.3)
    harpL = echo(harpL, BEAT * 0.75, 0.42, 0.9)
    harpR = echo(harpR, BEAT * 0.75 * 1.04, 0.42, 0.9)
    bellL = echo(bellL, BEAT * 1.5, 0.4, 0.9)
    bellR = echo(bellR, BEAT * 1.5 * 1.03, 0.4, 0.9)
    L += harpL + bellL
    R += harpR + bellR
    L, R = reverb(L, R, 0.34)
    peak = max(np.max(np.abs(L)), np.max(np.abs(R)))
    L, R = L / peak * 0.82, R / peak * 0.82
    fade_in = np.minimum(1, np.arange(N) / (3.0 * SR))
    tail = np.minimum(1, (N - np.arange(N)) / (3.5 * SR))
    L, R = L * fade_in * tail, R * fade_in * tail
    stereo_data = np.stack([L, R], axis=1).astype(np.float32)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=2, format='OGG', subtype='VORBIS') as f:
        for i in range(0, len(stereo_data), 1 << 16):
            f.write(stereo_data[i:i + (1 << 16)])
    return N / SR


def generate(assets_dir):
    path = os.path.join(assets_dir, 'sounds', 'music', 'eldoria_hub.ogg')
    secs = render(path)
    print('musique du hub generee : %.1f s' % secs)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    generate(os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'mmorpg'))
