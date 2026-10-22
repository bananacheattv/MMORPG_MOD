# -*- coding: utf-8 -*-
"""Effets sonores originaux d'Eldoria (cinematiques du hub, donjons, epreuves, boss) : synthetises, aucun son de Minecraft.

Chaque effet est un OGG Vorbis mono (positionnable dans le monde) ecrit dans assets/mmorpg/sounds/sfx/ ; sounds.json est mis a jour
par gen_music.py. Dependances : numpy, soundfile.
"""
import os

import numpy as np
import soundfile as sf

SR = 44100
RNG = np.random.default_rng(7)

NOTE = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}


def hz(name):
    acc = 1 if '#' in name else (-1 if 'b' in name else 0)
    octave = int(name[-1])
    return 440.0 * 2 ** ((NOTE[name[0]] + acc + (octave - 4) * 12 - 9) / 12.0)


def tt(d):
    return np.arange(int(d * SR)) / SR


def env(n, a, d, s, r):
    a, d, r = max(int(a * SR), 1), max(int(d * SR), 1), max(int(r * SR), 1)
    sus = max(0, n - a - d - r)
    e = np.concatenate([np.linspace(0, 1, a), np.linspace(1, s, d), np.full(sus, s), np.linspace(s, 0, r)])
    return e[:n] if len(e) >= n else np.pad(e, (0, n - len(e)))


def fft_filter(x, fn):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(X * fn(f), len(x))


def lowpass(x, c):
    return fft_filter(x, lambda f: 1 / (1 + (f / c) ** 4))


def highpass(x, c):
    return fft_filter(x, lambda f: 1 - 1 / (1 + (f / c) ** 4))


def bandpass(x, lo, hi):
    return fft_filter(x, lambda f: (1 / (1 + (f / hi) ** 4)) * (1 - 1 / (1 + (f / lo) ** 4)))


def formant(x, centers, width=0.16):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    g = np.zeros_like(f)
    for c, a in centers:
        g += a * np.exp(-0.5 * ((f - c) / (c * width)) ** 2)
    return np.fft.irfft(X * g, len(x))


def sweep_noise(dur, f0, f1, q=0.5):
    """Bruit filtre dont la bande centrale glisse de f0 a f1 (traitement par blocs)."""
    n = int(dur * SR)
    noise = RNG.normal(0, 1, n)
    out = np.zeros(n)
    block = 2048
    win = np.hanning(block * 2)
    for i in range(0, n - block, block):
        seg = noise[i:i + block * 2]
        if len(seg) < block * 2:
            break
        frac = i / n
        c = f0 * (f1 / f0) ** frac
        f = np.fft.rfftfreq(len(seg), 1 / SR)
        g = np.exp(-0.5 * ((np.log(f + 1) - np.log(c)) / q) ** 2)
        out[i:i + block * 2] += np.fft.irfft(np.fft.rfft(seg * win) * g, len(seg)) * win
    return out


def reverb(x, tail=1.6, wet=0.3, hf=5000):
    n = int(tail * SR)
    t = np.arange(n) / SR
    ir = RNG.normal(0, 1, n) * np.exp(-t * (4.5 / tail))
    ir = lowpass(ir, hf)
    ir[0] = 0
    size = 1
    while size < len(x) + n:
        size *= 2
    y = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(x) + n]
    y = y / (np.max(np.abs(y)) + 1e-9) * (np.max(np.abs(x)) + 1e-9)
    out = np.zeros(len(x) + n)
    out[:len(x)] += x * (1 - wet * 0.5)
    out += y * wet
    return out


def bell_tone(f, dur=3.0, bright=1.0, dec=1.0):
    t = tt(dur)
    out = np.zeros_like(t)
    for ratio, amp, d in ((1, 1.0, 0.9), (2.0, 0.55, 1.2), (2.76, 0.4, 1.5), (4.1, 0.2, 2.3), (5.4, 0.12, 3.0), (6.8, 0.06, 4.0)):
        out += amp * (bright if ratio > 2 else 1.0) * np.sin(2 * np.pi * f * ratio * t + RNG.uniform(0, 6.28)) * np.exp(-t * d * dec)
    return out * np.minimum(1, t * 800)


def saw(f, dur, harmonics=14):
    t = tt(dur)
    out = np.zeros_like(t)
    for h in range(1, harmonics + 1):
        if f * h > 12000:
            break
        out += np.sin(2 * np.pi * f * h * t + RNG.uniform(0, 0.4)) / h
    return out


def place(out, sig, at, gain=1.0):
    i = int(at * SR)
    if i >= len(out):
        return
    n = min(len(sig), len(out) - i)
    out[i:i + n] += sig[:n] * gain


def save(name, x, peak=0.9, fade=0.02):
    x = np.asarray(x, dtype=np.float64)
    x = x - np.mean(x)
    m = np.max(np.abs(x)) + 1e-9
    x = x / m * peak
    n = int(fade * SR)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    path = os.path.join(OUT, name + '.ogg')
    os.makedirs(OUT, exist_ok=True)
    with sf.SoundFile(path, 'w', samplerate=SR, channels=1, format='OGG', subtype='VORBIS') as f:
        for i in range(0, len(x), 1 << 15):
            f.write(x[i:i + (1 << 15)].astype(np.float32))
    return len(x) / SR


# ------------------------------------------------------------------ les effets

def rise():
    d = 2.8
    t = tt(d)
    f = 110 * (880 / 110) ** (t / d)
    phase = 2 * np.pi * np.cumsum(f) / SR
    tone = sum(np.sin(phase * h) / h for h in (1, 2, 3, 4, 6))
    shimmer = sum(np.sin(2 * np.pi * (1320 + 220 * k) * t * (1 + 0.02 * t)) for k in range(4)) * (t / d) ** 2 * 0.25
    air = highpass(RNG.normal(0, 1, len(t)), 2500) * (t / d) ** 2 * 0.5
    x = (tone * 0.5 + shimmer + air) * np.minimum(1, t / 0.6) * np.where(t > d - 0.25, (d - t) / 0.25, 1)
    return reverb(x, 1.8, 0.35)


def boom():
    d = 3.0
    t = tt(d)
    sub = np.sin(2 * np.pi * np.cumsum(38 + 90 * np.exp(-t * 9)) / SR) * np.exp(-t * 1.6)
    hit = lowpass(RNG.normal(0, 1, len(t)), 700) * np.exp(-t * 7) * 0.9
    crack = highpass(RNG.normal(0, 1, len(t)), 3000) * np.exp(-t * 40) * 0.5
    return reverb(sub + hit + crack, 2.4, 0.4, 3000)


def whoosh():
    d = 1.1
    t = tt(d)
    x = sweep_noise(d, 500, 4200, 0.7) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.6
    return x


def bell():
    d = 5.0
    x = bell_tone(hz('D3'), d, 1.0, 0.45) * 1.0 + 0.5 * bell_tone(hz('A3'), d, 0.8, 0.5)
    strike = lowpass(RNG.normal(0, 1, int(d * SR)), 2500) * np.exp(-tt(d) * 60) * 0.4
    return reverb(x + strike, 2.4, 0.35)


def chime():
    out = np.zeros(int(2.4 * SR))
    for i, n in enumerate(('D6', 'F#6', 'A6', 'D7', 'F#7')):
        place(out, bell_tone(hz(n), 1.8, 1.2, 1.4), 0.07 * i, 0.8 - 0.1 * i)
    return reverb(out, 1.4, 0.4, 7000)


def warrior():
    d = 1.8
    t = tt(d)
    clang = sum(np.sin(2 * np.pi * 330 * r * t + RNG.uniform(0, 6)) * a * np.exp(-t * dc) for r, a, dc in ((1, 1, 5), (2.76, 0.8, 7), (5.4, 0.6, 9), (8.93, 0.4, 12)))
    scrape = highpass(RNG.normal(0, 1, len(t)), 2200) * np.exp(-t * 18) * 0.7
    thud = np.sin(2 * np.pi * np.cumsum(70 + 60 * np.exp(-t * 14)) / SR) * np.exp(-t * 4)
    return reverb(clang * 0.5 + scrape + thud, 1.2, 0.25)


def arrow():
    d = 1.1
    t = tt(d)
    flight = sweep_noise(0.5, 2200, 6500, 0.6) * np.sin(np.pi * np.clip(t[:int(0.5 * SR)] / 0.5, 0, 1))
    out = np.zeros(len(t))
    place(out, flight, 0.0, 0.8)
    thud = (np.sin(2 * np.pi * 140 * t) * np.exp(-t * 28) + lowpass(RNG.normal(0, 1, len(t)), 900) * np.exp(-t * 40))
    place(out, thud[:int(0.4 * SR)], 0.48, 1.1)
    return out


def arcane():
    d = 2.4
    t = tt(d)
    carrier = np.sin(2 * np.pi * np.cumsum(500 + 900 * (t / d) ** 2) / SR + 3 * np.sin(2 * np.pi * 7 * t))
    ring = carrier * np.sin(2 * np.pi * np.cumsum(90 + 60 * np.sin(2 * np.pi * 0.8 * t)) / SR)
    swirl = sweep_noise(d, 800, 5000, 0.5) * (0.5 + 0.5 * np.sin(2 * np.pi * 3.2 * t))
    x = (ring * 0.5 + swirl * 0.5) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 0.8
    out = np.array(x)
    place(out, bell_tone(hz('A6'), 1.4, 1.3, 1.6), d - 1.0, 0.5)
    return reverb(out, 1.6, 0.4)


def shield():
    d = 1.4
    t = tt(d)
    thud = np.sin(2 * np.pi * np.cumsum(60 + 80 * np.exp(-t * 18)) / SR) * np.exp(-t * 6)
    ring = sum(np.sin(2 * np.pi * 520 * r * t) * a * np.exp(-t * dc) for r, a, dc in ((1, 1, 8), (2.4, 0.6, 12), (4.7, 0.3, 18)))
    scuff = lowpass(RNG.normal(0, 1, len(t)), 1800) * np.exp(-t * 30) * 0.7
    return reverb(thud * 1.1 + ring * 0.35 + scuff, 1.0, 0.25)


def brass(f, d, gain=1.0):
    t = tt(d)
    x = saw(f, d, 16) + saw(f * 1.005, d, 16)
    x = lowpass(x, 2600)
    x *= (1 + 0.04 * np.sin(2 * np.pi * 5.5 * t))
    return x * env(len(t), 0.07, 0.15, 0.85, 0.35) * gain


def fanfare():
    out = np.zeros(int(6.0 * SR))
    chords = [(0.0, 0.7, ('D3', 'A3', 'D4', 'F#4')), (0.75, 0.55, ('E3', 'B3', 'E4', 'G#4')), (1.35, 0.55, ('F#3', 'C#4', 'F#4', 'A4')),
              (1.95, 2.8, ('D3', 'A3', 'D4', 'F#4', 'A4', 'D5'))]
    for at, d, notes in chords:
        for n in notes:
            place(out, brass(hz(n), d, 0.35 if hz(n) < 300 else 0.28), at)
    for at in (0.0, 0.75, 1.95):
        t = tt(1.2)
        place(out, np.sin(2 * np.pi * np.cumsum(120 * np.exp(-t * 4) + 55) / SR) * np.exp(-t * 3.5), at, 1.4)
    for k, n in enumerate(('D6', 'F#6', 'A6', 'D7')):
        place(out, bell_tone(hz(n), 2.0, 1.3, 1.2), 2.0 + 0.12 * k, 0.45)
    return reverb(out, 2.4, 0.35)


def growl():
    d = 3.0
    t = tt(d)
    f = 62 - 14 * (t / d) + 3 * np.sin(2 * np.pi * 7 * t)
    phase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(phase * h) / h ** 0.8 for h in range(1, 18))
    x = formant(x, [(300, 1.0), (700, 0.8), (1800, 0.35)], 0.22)
    x = np.tanh(x * 3.0)
    breath = lowpass(RNG.normal(0, 1, len(t)), 900) * 0.35
    shape = np.minimum(1, t / 0.25) * np.exp(-np.maximum(0, t - 1.6) * 1.1)
    return reverb((x + breath) * shape, 2.0, 0.3, 3500)


def ritual():
    d = 3.2
    t = tt(d)
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * (4 + 7 * t / d) * t)
    drone = sum(np.sin(2 * np.pi * f * t * (1 + 0.0004 * k)) * a for k, (f, a) in enumerate(((55, 1.0), (110, 0.8), (164.8, 0.55), (220.5, 0.4))))
    choir = formant(sum(np.sin(2 * np.pi * (220 * (1 + 0.6 * t / d)) * h * t) / h for h in range(1, 9)), [(800, 1.0), (1150, 0.5)])
    x = (drone * trem + choir * (t / d) * 0.9) * (t / d) ** 0.7
    x *= np.where(t > d - 0.2, (d - t) / 0.2, 1)
    return reverb(x, 1.8, 0.3)


def heartbeat():
    d = 1.5
    out = np.zeros(int(d * SR))
    for at, g in ((0.0, 1.0), (0.3, 0.75)):
        t = tt(0.6)
        beat = np.sin(2 * np.pi * np.cumsum(70 + 30 * np.exp(-t * 20)) / SR) * np.exp(-t * 8) + lowpass(RNG.normal(0, 1, len(t)), 400) * np.exp(-t * 30) * 0.5
        place(out, beat, at, g)
    return out


def horn():
    d = 2.4
    out = np.zeros(int(d * SR))
    for at, n, dur in ((0.0, 'D3', 0.9), (0.85, 'A3', 1.5)):
        t = tt(dur)
        f = hz(n) * (1 + 0.02 * np.exp(-t * 6))
        x = sum(np.sin(2 * np.pi * np.cumsum(f) / SR * h) / h ** 0.9 for h in range(1, 12))
        x = formant(x, [(450, 1.0), (900, 0.7), (1800, 0.3)], 0.2) * (1 + 0.05 * np.sin(2 * np.pi * 5 * t))
        place(out, x * env(len(t), 0.12, 0.1, 0.9, 0.3), at, 1.0)
    return reverb(out, 1.6, 0.3, 4000)


def door():
    d = 2.4
    t = tt(d)
    scrape = lowpass(RNG.normal(0, 1, len(t)), 450) * (0.55 + 0.45 * np.sin(2 * np.pi * 7 * t)) * np.minimum(1, t / 0.2) * np.where(t > 1.8, np.maximum(0, (2.0 - t) / 0.2), 1)
    squeal = np.sin(2 * np.pi * np.cumsum(780 - 280 * t / d) / SR) * 0.07 * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t))
    out = scrape + squeal
    tail = np.sin(2 * np.pi * 52 * tt(0.7)) * np.exp(-tt(0.7) * 6) + lowpass(RNG.normal(0, 1, int(0.7 * SR)), 500) * np.exp(-tt(0.7) * 14)
    place(out, tail, 1.85, 1.4)
    return reverb(out, 1.4, 0.3, 3000)


def success():
    out = np.zeros(int(2.2 * SR))
    for k, n in enumerate(('D5', 'F#5', 'A5', 'D6')):
        place(out, bell_tone(hz(n), 1.6, 1.2, 1.5), 0.09 * k, 0.9 - 0.1 * k)
    return reverb(out, 1.4, 0.35, 7000)


def error():
    d = 0.9
    t = tt(d)
    x = (np.sin(2 * np.pi * np.cumsum(150 - 70 * t / d) / SR) + 0.8 * np.sin(2 * np.pi * np.cumsum(158 - 74 * t / d) / SR)) * np.exp(-t * 3.5)
    x = np.tanh(x * 2.5) * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 14 * t)))
    return lowpass(x, 1800)


def pad_note(name):
    def make():
        t = tt(1.6)
        f = hz(name)
        x = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 4) + 0.25 * np.sin(2 * np.pi * 4.1 * f * t) * np.exp(-t * 9)) * np.exp(-t * 2.6)
        return reverb(x * np.minimum(1, t * 600), 1.0, 0.25, 6000)
    return make


def firework():
    d = 1.6
    t = tt(d)
    whistle = np.sin(2 * np.pi * np.cumsum(1500 + 2500 * np.clip(t / 0.5, 0, 1)) / SR) * np.where(t < 0.5, 0.25, 0)
    out = whistle
    for k in range(60):
        at = 0.5 + RNG.exponential(0.12)
        if at > d - 0.1:
            continue
        click = highpass(RNG.normal(0, 1, int(0.03 * SR)), 2500) * np.exp(-tt(0.03) * 120)
        place(out, click, at, RNG.uniform(0.2, 0.8))
    boom_t = tt(0.5)
    place(out, lowpass(RNG.normal(0, 1, len(boom_t)), 900) * np.exp(-boom_t * 9), 0.5, 1.1)
    return reverb(out, 1.2, 0.3)


def teleport():
    d = 0.9
    t = tt(d)
    x = np.sin(2 * np.pi * np.cumsum(1300 - 1000 * t / d) / SR) * np.exp(-t * 3) + sweep_noise(d, 3500, 500, 0.7) * 0.8
    return x * np.minimum(1, t / 0.03)


EFFECTS = {
    'rise': rise, 'boom': boom, 'whoosh': whoosh, 'bell': bell, 'chime': chime, 'warrior': warrior, 'arrow': arrow, 'arcane': arcane,
    'shield': shield, 'fanfare': fanfare, 'growl': growl, 'ritual': ritual, 'heartbeat': heartbeat, 'horn': horn, 'door': door,
    'success': success, 'error': error, 'firework': firework, 'teleport': teleport,
    'note0': pad_note('D5'), 'note1': pad_note('F#5'), 'note2': pad_note('A5'), 'note3': pad_note('D6'),
}

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
OUT = os.path.join(ROOT, 'sounds', 'sfx')


def sounds_json_entries():
    return {f'sfx.{name}': {'category': 'master', 'sounds': [{'name': f'mmorpg:sfx/{name}'}]} for name in EFFECTS}


def generate(assets_dir=None):
    global OUT
    if assets_dir:
        OUT = os.path.join(assets_dir, 'sounds', 'sfx')
    total = 0.0
    for name, fn in EFFECTS.items():
        total += save(name, fn())
    print('effets sonores generes : %d fichiers, %.1f s au total' % (len(EFFECTS), total))


if __name__ == '__main__':
    generate()
