# -*- coding: utf-8 -*-
"""Musiques d'ambiance des mines et des plaines d'Aeloria (deux boucles d'environ 2 min), distinctes de celles du hub, de la Cite et des donjons.

Mines : re phrygien a 54 pulsations/min, un bourdon tres grave, des nappes sombres, des gouttes d'eau (cloches aigues en cascade d'echos), des
coups de pioche lointains (metal court) et un battement sourd de temps en temps ; reverberation de caverne.
Plaines : do lydien a 96 pulsations/min, luth en arpeges legers, flute pastorale, nappes claires, souffle de vent et chants d'oiseaux ; pas de tambour.

Reprend les instruments de gen_hub_music et gen_cite_music. Dependances : numpy, soundfile.
Sorties : assets/mmorpg/sounds/music/aeloria_mines.ogg et aeloria_plaines.ogg (declares dans sounds.json : « mines.ambiance », « plaines.ambiance »).
Pour utiliser une autre musique : remplacer le fichier OGG par le votre (meme nom), rien d'autre a changer.
"""
import os

import numpy as np
import soundfile as sf

import gen_music as gm
import gen_hub_music as hm
import gen_cite_music as cm

SR = gm.SR


def setup(total, seed):
    n = int(total * SR) + SR * 5
    hm.N = n
    return n, np.random.default_rng(seed)


def finish(path, L, R, n, fade_in, fade_out):
    peak = max(np.max(np.abs(L)), np.max(np.abs(R)))
    L, R = L / peak * 0.82, R / peak * 0.82
    a = np.minimum(1, np.arange(n) / (fade_in * SR))
    b = np.minimum(1, (n - np.arange(n)) / (fade_out * SR))
    data = np.stack([L * a * b, R * a * b], axis=1).astype(np.float32)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=2, format='OGG', subtype='VORBIS') as f:
        for i in range(0, len(data), 1 << 16):
            f.write(data[i:i + (1 << 16)])
    return n / SR


# ------------------------------------------------------------------ mines

def dark_pad(note, dur, level=1.0):
    f = gm.hz(note)
    t = gm.tt(dur)
    out = np.zeros_like(t)
    for det in (-9, 0, 8):
        ff = f * 2 ** (det / 1200.0)
        for h in range(1, 6):
            out += np.sin(2 * np.pi * ff * h * t + cm.RNG.uniform(0, 6.28)) / h ** 1.4
    out *= 1 + 0.12 * np.sin(2 * np.pi * 0.17 * t + cm.RNG.uniform(0, 6.28))
    out = gm.lowpass(out, 650)
    return out * gm.adsr(len(t), 3.0, 1.0, 0.8, 3.2) * 0.05 * level


def drip(note, vel=1.0):
    """Goutte d'eau dans une caverne : sinus aigu a attaque tres breve et chute de hauteur."""
    f = gm.hz(note)
    t = gm.tt(1.6)
    fall = f * (1 + 0.12 * np.exp(-t * 40))
    out = np.sin(2 * np.pi * np.cumsum(fall) / SR) * np.exp(-t * 7.5)
    out += 0.3 * np.sin(2 * np.pi * 2.01 * np.cumsum(fall) / SR) * np.exp(-t * 14)
    return out * np.minimum(1, t * 900) * 0.16 * vel


def pick(vel=1.0):
    """Coup de pioche lointain : tic metallique court et resonance de roche."""
    t = gm.tt(0.9)
    out = np.zeros_like(t)
    for ratio, amp, dec in ((1.0, 1.0, 14), (2.76, 0.6, 22), (5.4, 0.35, 34), (8.9, 0.2, 48)):
        out += amp * np.sin(2 * np.pi * 1180 * ratio * t) * np.exp(-t * dec)
    out += gm.lowpass(cm.RNG.normal(0, 1, len(t)), 3000) * np.exp(-t * 90) * 0.5
    return gm.lowpass(out, 5200) * np.minimum(1, t * 1500) * 0.07 * vel


def thump(vel=1.0):
    t = gm.tt(0.8)
    f = 70 * np.exp(-t * 6) + 34
    out = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5.5)
    return out * 0.22 * vel


def mines(path):
    bpm = 54.0
    beat = 60.0 / bpm
    step = 4 * beat * 2
    steps = 16
    n, rng = setup(steps * step, 1618)
    # re phrygien : Dm, Eb, Cm, Dm... (basse, nappe, notes de gouttes)
    chords = [
        ('D1', ['A2', 'D3', 'F3', 'A3'], ['D6', 'F6', 'A5', 'C6', 'G6']),
        ('Eb1', ['Bb2', 'Eb3', 'G3', 'Bb3'], ['Eb6', 'G6', 'Bb5', 'D6', 'F6']),
        ('C1', ['G2', 'C3', 'Eb3', 'G3'], ['C6', 'Eb6', 'G5', 'Bb5', 'F6']),
        ('D1', ['A2', 'D3', 'E3', 'A3'], ['D6', 'E6', 'A5', 'F6', 'C6']),
    ]
    prog = [0, 0, 1, 0, 2, 2, 1, 3] * 2
    L, R = hm.stereo()
    dL, dR = hm.stereo()
    for s in range(steps):
        t0 = s * step
        bass, pads, drops, = chords[prog[s % len(prog)]]
        lvl = 0.6 if s >= steps - 2 else 1.0
        hm.put(L, R, t0, hm.sub(bass, step + 3.0), 0.0, 0.9 * lvl)
        for i, nname in enumerate(pads):
            hm.put(L, R, t0 + i * 0.2, dark_pad(nname, step + 4.0, lvl), -0.7 + 1.4 * i / max(1, len(pads) - 1))
        if s % 4 == 1:
            hm.put(L, R, t0 + 1.0, hm.air(step * 1.6, 0.9), 0.0)
        if s >= 1:                                                     # gouttes : au hasard, plus ou moins serrees
            t = t0 + rng.uniform(0.2, 1.8)
            while t < t0 + step - 1.0:
                hm.put(dL, dR, t, drip(drops[int(rng.integers(len(drops)))], 0.45 + 0.55 * rng.random()), rng.uniform(-0.8, 0.8))
                t += float(rng.choice([0.9, 1.7, 2.6, 3.8]))
        if s >= 3 and s < steps - 2 and s % 2 == 1:                    # pioches au loin, en petite salve de deux ou trois coups irreguliers
            t = t0 + rng.uniform(1.0, 4.0)
            for _ in range(int(rng.integers(2, 4))):
                hm.put(dL, dR, t, pick(0.5 + 0.5 * rng.random()), rng.uniform(-0.9, 0.9))
                t += float(rng.choice([0.55, 0.8, 1.1]))
        if s >= 5 and s < steps - 2 and s % 4 == 3:                    # battement sourd
            hm.put(L, R, t0 + beat, thump(0.8), 0.0)
            hm.put(L, R, t0 + beat * 1.7, thump(0.5), 0.0)
    dL = hm.echo(dL, beat * 1.4, 0.55, 0.9)
    dR = hm.echo(dR, beat * 1.4 * 1.05, 0.55, 0.9)
    L += dL
    R += dR
    L, R = hm.reverb(L, R, 0.46)
    return finish(path, L, R, n, 3.5, 4.0)


# ------------------------------------------------------------------ plaines

def chirp(vel=1.0, base=3200.0):
    """Chant d'oiseau : deux ou trois notes aigues glissees, courtes."""
    notes = int(cm.RNG.integers(2, 4))
    out = np.zeros(int(0.7 * SR))
    pos = 0
    for k in range(notes):
        d = int(cm.RNG.uniform(0.07, 0.13) * SR)
        t = np.arange(d) / SR
        f = base * cm.RNG.uniform(0.9, 1.35) * (1 + cm.RNG.uniform(-0.35, 0.35) * t / (d / SR))
        seg = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * np.arange(d) / d) ** 1.5
        out[pos:pos + d] += seg
        pos += d + int(cm.RNG.uniform(0.02, 0.06) * SR)
        if pos + d >= len(out):
            break
    return out * 0.03 * vel


def wind(dur, level=1.0):
    t = gm.tt(dur)
    n = cm.RNG.normal(0, 1, len(t))
    n = gm.lowpass(n, 1600)
    gust = 0.35 + 0.65 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.09 * t + 0.7)) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.047 * t + 2.0))
    return n * gust * gm.adsr(len(t), 5, 1, 0.9, 5) * 0.045 * level


def plaines(path):
    bpm = 96.0
    beat = 60.0 / bpm
    step = 8 * beat / 2 * 2                                              # deux mesures par accord
    steps = 24
    n, rng = setup(steps * step, 2718)
    # do lydien : C - D - Em - G (basse, nappe, arpege du luth, melodie de flute)
    chords = [
        ('C2', ['G3', 'C4', 'E4', 'G4'], ['C3', 'G3', 'E4', 'C4', 'G3', 'E3'], ['E5', 'G5', 'C6', 'B5', 'D5']),
        ('D2', ['A3', 'D4', 'F#4', 'A4'], ['D3', 'A3', 'F#4', 'D4', 'A3', 'F#3'], ['F#5', 'A5', 'D6', 'C#6', 'E5']),
        ('E2', ['B3', 'E4', 'G4', 'B4'], ['E3', 'B3', 'G4', 'E4', 'B3', 'G3'], ['G5', 'B5', 'E6', 'D6', 'F#5']),
        ('G2', ['D4', 'G4', 'B4', 'D5'], ['G3', 'D4', 'B4', 'G4', 'D4', 'B3'], ['B5', 'D6', 'G5', 'A5', 'E5']),
    ]
    prog = [0, 1, 0, 3, 2, 1, 0, 3] * 3
    L, R = hm.stereo()
    lutL, lutR = hm.stereo()
    fluL, fluR = hm.stereo()
    birdL, birdR = hm.stereo()
    for s in range(steps):
        t0 = s * step
        bass, pads, arp, mel = chords[prog[s % len(prog)]]
        lvl = 0.6 if s >= steps - 3 else 1.0
        hm.put(L, R, t0, cm.drone(bass, step + 2.0), 0.0, 0.7 * lvl)
        for i, nname in enumerate(pads):
            hm.put(L, R, t0 + i * 0.1, hm.pad(nname, step + 2.6, 0.8 * lvl), -0.6 + 1.2 * i / max(1, len(pads) - 1))
        if s % 3 == 0:
            hm.put(L, R, t0, wind(step * 3, 1.0), 0.0)
        if 1 <= s < steps - 2:                                           # luth : arpeges doux en croches
            for i in range(8):
                if rng.random() > (0.95 if s >= 4 else 0.6):
                    continue
                note = arp[i % len(arp)] if i < 4 else arp[(len(arp) - 1 - (i % len(arp)))]
                hm.put(lutL, lutR, t0 + i * beat, cm.lute(note, 0.45 + 0.4 * rng.random() + (0.2 if i % 4 == 0 else 0)), rng.uniform(-0.5, 0.5))
        if 4 <= s < steps - 3:                                           # flute : marche aleatoire douce sur les notes de l'accord
            t = t0 + (beat if s % 2 else 0)
            prev = int(rng.integers(len(mel)))
            while t < t0 + step - beat:
                prev = int(np.clip(prev + rng.integers(-1, 2), 0, len(mel) - 1))
                dur = float(rng.choice([1.0, 1.5, 2.0, 3.0])) * beat
                if rng.random() < 0.7:
                    hm.put(fluL, fluR, t, cm.flute(mel[prev], dur * 0.95, 0.65 + 0.35 * rng.random()), rng.uniform(-0.3, 0.3))
                t += dur
        if s >= 2 and s < steps - 2:                                     # oiseaux : un ou deux chants par accord, au hasard
            for _ in range(int(rng.integers(1, 3))):
                hm.put(birdL, birdR, t0 + rng.uniform(0.3, step - 1.0), chirp(0.5 + 0.5 * rng.random(), float(rng.uniform(2600, 4200))), rng.uniform(-0.9, 0.9))
    lutL = hm.echo(lutL, beat * 0.75, 0.28, 0.7)
    lutR = hm.echo(lutR, beat * 0.75 * 1.04, 0.28, 0.7)
    fluL = hm.echo(fluL, beat * 1.5, 0.3, 0.8)
    fluR = hm.echo(fluR, beat * 1.5 * 1.03, 0.3, 0.8)
    L += lutL + fluL + birdL
    R += lutR + fluR + birdR
    L, R = hm.reverb(L, R, 0.2)
    return finish(path, L, R, n, 2.5, 3.5)


def generate(assets_dir):
    out = os.path.join(assets_dir, 'sounds', 'music')
    print('musique des mines generee : %.1f s' % mines(os.path.join(out, 'aeloria_mines.ogg')))
    print('musique des plaines generee : %.1f s' % plaines(os.path.join(out, 'aeloria_plaines.ogg')))


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    generate(os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'mmorpg'))
