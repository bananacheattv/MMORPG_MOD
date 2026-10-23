# -*- coding: utf-8 -*-
"""Musique de la Cite d'Aeloria (le grand Spawn) : boucle de ~2 min en sol mixolydien, a 92 pulsations/min, d'allure medievale et vivante.

Instruments synthetises (reprennent ceux du hub, gen_hub_music) : luth (cordes pincees a partiels additifs), flute (souffle et vibrato),
bourdon grave, tambour et grelots legers pour le pas du marche, nappe chaude ; reverberation de salle pour l'espace.
Structure : intro de bourdon et de nappes, luth, tambour et grelots, melodie de flute, retour au calme.

Dependances : numpy, soundfile. Sortie : assets/mmorpg/sounds/music/aeloria_cite.ogg (declare dans sounds.json : « cite.ambiance »).
Pour utiliser une autre musique : remplacer ce fichier OGG par le votre (meme nom), rien d'autre a changer.
"""
import os

import numpy as np
import soundfile as sf

import gen_music as gm
import gen_hub_music as hm

SR = gm.SR
BPM = 92.0
BEAT = 60.0 / BPM
BAR = 4 * BEAT
STEP = 2 * BAR
STEPS = 22
TOTAL = STEPS * STEP
N = int(TOTAL * SR) + SR * 5
hm.N = N
hm.RNG = np.random.default_rng(27182)
RNG = hm.RNG

# sol mixolydien : G - F - C - D ; (basse, nappe, arpege, melodie)
CHORDS = [
    ('G2', ['D3', 'G3', 'B3', 'D4'], ['G3', 'D4', 'B3', 'G4', 'D4', 'B3'], ['B4', 'D5', 'G5', 'A5', 'E5']),
    ('F2', ['C3', 'F3', 'A3', 'C4'], ['F3', 'C4', 'A3', 'F4', 'C4', 'A3'], ['A4', 'C5', 'F5', 'G5', 'D5']),
    ('C2', ['G2', 'C3', 'E3', 'G3'], ['C3', 'G3', 'E4', 'C4', 'G3', 'E3'], ['G4', 'C5', 'E5', 'G5', 'D5']),
    ('D2', ['A2', 'D3', 'F#3', 'A3'], ['D3', 'A3', 'F#4', 'D4', 'A3', 'F#3'], ['F#4', 'A4', 'D5', 'E5', 'C5']),
]
PROG = [0, 1, 2, 3] * 6


def chord_at(step):
    return CHORDS[PROG[step % len(PROG)]]


def lute(note, vel=1.0):
    f = gm.hz(note)
    dur = 2.6
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for h in range(1, 9):
        fh = f * h * (1 + 0.0006 * h * h)
        out += np.sin(2 * np.pi * fh * t) / h ** 1.1 * np.exp(-t * (2.2 + 0.9 * h))
    click = RNG.normal(0, 1, len(t)) * np.exp(-t * 120)
    out += gm.lowpass(click, 2600) * 0.05
    return out * 0.22 * vel


def flute(note, dur, vel=1.0):
    f = gm.hz(note)
    t = gm.tt(dur)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.4 * t) * np.minimum(1, t * 2)
    out = np.sin(2 * np.pi * f * t * vib) + 0.18 * np.sin(2 * np.pi * 2 * f * t * vib) + 0.06 * np.sin(2 * np.pi * 3 * f * t * vib)
    breath = gm.lowpass(RNG.normal(0, 1, len(t)), 5200) * 0.05 * (0.4 + np.minimum(1, t * 8))
    return (out + breath) * gm.adsr(len(t), 0.08, 0.1, 0.8, 0.35) * 0.13 * vel


def drone(note, dur):
    f = gm.hz(note)
    t = gm.tt(dur)
    out = np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * 2 * f * t) + 0.25 * np.sin(2 * np.pi * 3 * f * t)
    out = gm.lowpass(out, 700)
    return out * gm.adsr(len(t), 1.8, 0.5, 0.85, 2.2) * 0.1


def drum(vel=1.0):
    t = gm.tt(0.5)
    f = 110 * np.exp(-t * 9) + 52
    out = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)
    out += gm.lowpass(RNG.normal(0, 1, len(t)), 900) * np.exp(-t * 28) * 0.4
    return out * 0.26 * vel


def shaker(vel=1.0):
    t = gm.tt(0.18)
    n = RNG.normal(0, 1, len(t))
    n = n - gm.lowpass(n, 4000)
    return n * np.exp(-t * 26) * 0.07 * vel


def render(path):
    L, R = hm.stereo()
    lutL, lutR = hm.stereo()
    fluL, fluR = hm.stereo()
    for s in range(STEPS):
        t0 = s * STEP
        bass, pads, arp, mel = chord_at(s)
        last = s >= STEPS - 3
        lvl = 0.6 if last else 1.0
        hm.put(L, R, t0, drone(bass, STEP + 2.2), 0.0, lvl)
        for i, nname in enumerate(pads):
            hm.put(L, R, t0 + i * 0.1, hm.pad(nname, STEP + 2.6, 0.9 * lvl), -0.6 + 1.2 * i / max(1, len(pads) - 1))
        if 2 <= s < STEPS - 2:
            for i in range(16):                                   # luth : arpege en croches, un temps sur quatre mis en valeur
                if RNG.random() > (1.0 if s >= 6 else 0.75):
                    continue
                note = arp[i % len(arp)] if i < 8 else arp[(len(arp) - 1 - (i % len(arp)))]
                hm.put(lutL, lutR, t0 + i * BEAT * 0.5, lute(note, 0.5 + 0.5 * RNG.random() + (0.25 if i % 4 == 0 else 0)), RNG.uniform(-0.5, 0.5))
        if 6 <= s < STEPS - 3:
            for b in range(8):                                    # tambour sur les temps 1 et 3, grelots a contretemps
                if b % 2 == 0:
                    hm.put(L, R, t0 + b * BEAT, drum(0.9 if b % 4 == 0 else 0.6), 0.0)
                hm.put(L, R, t0 + b * BEAT + BEAT * 0.5, shaker(0.6 + 0.4 * RNG.random()), RNG.uniform(-0.3, 0.3))
        if 8 <= s < STEPS - 3:
            t = t0
            prev = int(RNG.integers(len(mel)))
            while t < t0 + STEP - BEAT:
                prev = int(np.clip(prev + RNG.integers(-1, 2), 0, len(mel) - 1))
                dur = float(RNG.choice([1.0, 1.5, 2.0])) * BEAT
                if RNG.random() < 0.8:
                    hm.put(fluL, fluR, t, flute(mel[prev], dur * 0.95, 0.7 + 0.3 * RNG.random()), RNG.uniform(-0.3, 0.3))
                t += dur
    lutL = hm.echo(lutL, BEAT * 0.75, 0.3, 0.7)
    lutR = hm.echo(lutR, BEAT * 0.75 * 1.04, 0.3, 0.7)
    fluL = hm.echo(fluL, BEAT * 1.5, 0.35, 0.8)
    fluR = hm.echo(fluR, BEAT * 1.5 * 1.03, 0.35, 0.8)
    L += lutL + fluL
    R += lutR + fluR
    L, R = hm.reverb(L, R, 0.26)
    peak = max(np.max(np.abs(L)), np.max(np.abs(R)))
    L, R = L / peak * 0.82, R / peak * 0.82
    fade_in = np.minimum(1, np.arange(N) / (2.5 * SR))
    tail = np.minimum(1, (N - np.arange(N)) / (3.5 * SR))
    L, R = L * fade_in * tail, R * fade_in * tail
    data = np.stack([L, R], axis=1).astype(np.float32)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=2, format='OGG', subtype='VORBIS') as f:
        for i in range(0, len(data), 1 << 16):
            f.write(data[i:i + (1 << 16)])
    return N / SR


def generate(assets_dir):
    path = os.path.join(assets_dir, 'sounds', 'music', 'aeloria_cite.ogg')
    secs = render(path)
    print('musique de la cite generee : %.1f s' % secs)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    generate(os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'mmorpg'))
