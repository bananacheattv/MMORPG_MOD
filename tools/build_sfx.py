# -*- coding: utf-8 -*-
"""Effets sonores d'Eldoria a partir d'enregistrements libres de droits (CC0, OpenGameArt), montes et mixes : couches, hauteur, reverberation.

Les sources sont recuperees par fetch_sfx_sources.py (dossier de cache tools/_sfx_cache par defaut, ou celui passe en argument).
Chaque effet est un OGG Vorbis mono (positionnable dans le monde) ecrit dans assets/mmorpg/sounds/sfx/ avec les memes noms qu'avant :
le code du mod n'a pas change. Dependances : numpy, scipy, soundfile.

usage : python tools/build_sfx.py [dossier_du_cache]
"""
import glob
import json
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve, resample_poly

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
OUT = os.path.join(ROOT, 'sounds', 'sfx')
CACHE = os.path.join(HERE, '_sfx_cache')
RNG = np.random.default_rng(11)


# ------------------------------------------------------------------ outils

def src(pattern):
    hits = sorted(glob.glob(os.path.join(CACHE, pattern), recursive=True))
    if not hits:
        raise FileNotFoundError(f'source introuvable : {pattern} (lancez fetch_sfx_sources.py)')
    return hits[0]


def load(pattern, t0=0.0, t1=None, gain=1.0):
    x, sr = sf.read(src(pattern), always_2d=True)
    x = x.mean(1)
    if sr != SR:
        g = np.gcd(sr, SR)
        x = resample_poly(x, SR // g, sr // g)
    a = int(t0 * SR)
    b = len(x) if t1 is None else int(t1 * SR)
    return x[a:b].astype(np.float64) * gain


def tt(d):
    return np.arange(int(d * SR)) / SR


def pitch(x, semis):
    """Change la hauteur (et la duree) par reechantillonnage."""
    f = 2 ** (semis / 12.0)
    n = int(len(x) / f)
    return np.interp(np.arange(n) * f, np.arange(len(x)), x)


def fade(x, a=0.005, r=0.05):
    x = x.copy()
    na, nr = min(int(a * SR), len(x) // 2), min(int(r * SR), len(x) // 2)
    if na:
        x[:na] *= np.sin(np.linspace(0, np.pi / 2, na)) ** 2
    if nr:
        x[-nr:] *= np.cos(np.linspace(0, np.pi / 2, nr)) ** 2
    return x


def fft_filter(x, fn):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(X * fn(f), len(x))


def lowpass(x, c):
    return fft_filter(x, lambda f: 1 / (1 + (f / c) ** 4))


def highpass(x, c):
    return fft_filter(x, lambda f: 1 - 1 / (1 + (f / c) ** 4))


def reverb(x, tail=1.4, wet=0.25, damp=4500, pre=0.012):
    """Reverberation par convolution avec une reponse impulsionnelle de bruit decroissant."""
    n = int(tail * SR)
    t = np.arange(n) / SR
    ir = RNG.standard_normal(n) * np.exp(-t * (6.9 / tail))
    ir = lowpass(ir, damp)
    ir[: int(pre * SR)] = 0
    ir /= np.sqrt((ir ** 2).sum()) + 1e-9
    y = fftconvolve(x, ir)[: len(x) + n]
    out = np.zeros(len(y))
    out[: len(x)] += x * (1 - wet * 0.5)
    out += y * wet * 1.6
    return out


def sub(f0, f1, d, decay=3.0, gain=1.0):
    """Coup grave : sinus qui descend de f0 a f1 (poids des impacts)."""
    t = tt(d)
    ph = 2 * np.pi * np.cumsum(f1 + (f0 - f1) * np.exp(-t * 9)) / SR
    return np.sin(ph) * np.exp(-t * decay) * gain


def noise_burst(d, lo, hi, decay=6.0, gain=1.0):
    n = RNG.standard_normal(int(d * SR))
    n = highpass(lowpass(n, hi), lo)
    return n * np.exp(-tt(d) * decay) * gain


def mix(*layers, length=None):
    """layers : (signal, decalage_en_s, gain)."""
    end = max(int(off * SR) + len(x) for x, off, _ in layers)
    out = np.zeros(max(end, length and int(length * SR) or 0))
    for x, off, g in layers:
        i = int(off * SR)
        out[i:i + len(x)] += x * g
    return out


def finish(x, peak=0.89, fade_in=0.004, fade_out=0.08, tail_cut=True):
    """Normalise avec un leger ecretage doux, fondus d'entree et de sortie, silence final retire."""
    x = np.tanh(x * 1.15) / np.tanh(1.15)
    if tail_cut:
        a = np.abs(x)
        idx = np.where(a > 0.004 * max(1e-9, a.max()))[0]
        if len(idx):
            x = x[: min(len(x), idx[-1] + int(0.05 * SR))]
    x = fade(x, fade_in, fade_out)
    return x / (np.abs(x).max() + 1e-9) * peak


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + '.ogg'), x.astype(np.float32), SR, format='OGG', subtype='VORBIS')
    return len(x) / SR


def dominant_hz(x, lo=150, hi=3000):
    X = np.abs(np.fft.rfft(x * np.hanning(len(x))))
    f = np.fft.rfftfreq(len(x), 1 / SR)
    m = (f > lo) & (f < hi)
    return f[m][np.argmax(X[m])]


# ------------------------------------------------------------------ effets

def e_boom():
    hit = load('100-cc0-sfx/**/explosion.ogg')
    slam = pitch(load('100-cc0-sfx/**/slam_03.ogg'), -7)
    x = mix((hit, 0, 1.0), (slam, 0.01, 0.8), (sub(95, 34, 1.8, 2.2, 1.6), 0, 1.0),
            (noise_burst(0.9, 80, 900, 4.5, 0.5), 0, 1.0))
    return finish(reverb(x, 1.8, 0.28, 3000), fade_out=0.35)


def e_whoosh():
    w = load('air-whoosh/**/whoosh2_0.wav', 0.0, 2.4)
    w = highpass(w, 200)
    r = pitch(load('swishes-sound-pack/**/swish-7.wav'), -5)
    return finish(mix((w, 0, 1.0), (r, 0.15, 0.6)), fade_in=0.18, fade_out=0.6)


def e_rise():
    f = load('flight-or-spell-sound/*.mp3')
    d = load('air-whoosh/**/whoosh2_0.wav', 0.0, 3.0)[::-1]
    shimmer = highpass(load('magic-spell-sfx/magical_3.ogg')[::-1], 1500)
    x = mix((f, 0, 0.9), (d, 0.2, 0.7), (shimmer, 1.0, 0.5))
    return finish(reverb(x, 1.2, 0.2), fade_in=0.2, fade_out=0.25)


def e_bell():
    g = load('100-cc0-sfx/**/gong_01.ogg')
    b = pitch(load('100-cc0-sfx/**/bell_03.ogg'), -5)
    x = mix((g, 0, 1.0), (b, 0.0, 0.7), (sub(70, 52, 2.4, 1.3, 0.5), 0, 1.0))
    return finish(reverb(x, 2.8, 0.32, 3800), fade_out=0.9)


def e_chime():
    a = load('80-cc0-rpg-sfx/**/item_gem_01.ogg')
    b = pitch(load('100-cc0-sfx/**/bell_02.ogg'), 7)
    x = mix((a, 0, 0.9), (b, 0.03, 0.55), (pitch(a, 12), 0.09, 0.4))
    return finish(reverb(x, 1.6, 0.3, 6000), fade_out=0.4)


def e_warrior():
    c = load('20-sword-sound-effects-attacks-and-clashes/**/sword_clash.4.ogg')
    u = load('rpg-sound-pack/**/sword-unsheathe3.wav')
    r = pitch(load('80-cc0-rpg-sfx/**/creature_roar_01.ogg'), -3)
    x = mix((u, 0, 0.8), (c, 0.35, 1.0), (r, 0.38, 0.55), (sub(80, 48, 0.9, 3.2, 0.9), 0.36, 1.0))
    return finish(reverb(x, 1.1, 0.2, 5000), fade_out=0.3)


def e_arrow():
    s = load('swishes-sound-pack/**/swish-4.wav')
    s2 = pitch(load('swishes-sound-pack/**/swish-9.wav'), 3)
    t = pitch(load('80-cc0-rpg-sfx/**/wood_02.ogg'), 5)
    x = mix((s, 0, 0.9), (s2, 0.08, 0.7), (t, 0.2, 0.9))
    return finish(reverb(x, 0.5, 0.12), fade_out=0.15)


def e_arcane():
    a = load('magic-spell-sfx/magical_1.ogg')
    b = load('magic-spell-sfx/magical_3.ogg')
    c = load('80-cc0-rpg-sfx/**/spell_02.ogg')
    x = mix((a, 0, 1.0), (b, 0.1, 0.8), (c, 0.25, 0.7), (pitch(load('80-cc0-rpg-sfx/**/item_gem_03.ogg'), 5), 0.5, 0.5))
    return finish(reverb(x, 1.8, 0.32, 5200), fade_out=0.5)


def e_shield():
    i = load('10-impactshield-blocks/**/impact.5.ogg')
    m = load('80-cc0-rpg-sfx/**/metal_02.ogg')
    z = load('magic-spell-sfx/magical_5.ogg') * 2.5
    x = mix((i, 0, 1.0), (m, 0.02, 0.7), (z, 0.04, 0.6), (sub(120, 70, 0.4, 7, 0.8), 0, 1.0))
    return finish(reverb(x, 0.9, 0.2, 5200), fade_out=0.25)


def e_fanfare():
    f = load('just-a-random-fanfare/fanfare.ogg', 0.0, 8.6)
    h = load('war-horns/war_horns.mp3', 22.0, 26.0) * 0.45
    x = mix((f, 0.0, 1.0), (h, 0.0, 0.5))
    return finish(reverb(x, 2.2, 0.22), fade_out=1.2)


def e_growl():
    r = load('cc0-deep-monster-roar/monster_roar.wav', 0.0, 4.2)
    r2 = pitch(load('80-cc0-rpg-sfx/**/creature_roar_03.ogg'), -6)
    x = mix((r, 0, 1.0), (r2, 0.1, 0.7))
    return finish(reverb(x, 1.4, 0.22, 3500), fade_out=1.0)


def e_ritual():
    d = load('horde-war-drums-loop/*.wav', 0.0, 7.0, 0.8)
    e = load('energy-drain/*.ogg')
    h = pitch(load('80-cc0-rpg-sfx/**/creature_monster_03.ogg'), -9)
    x = mix((d, 0, 0.9), (e, 0.3, 0.8), (h, 1.2, 0.8), (sub(60, 45, 3.0, 0.8, 0.5), 3.0, 1.0))
    return finish(reverb(x, 2.4, 0.28, 3200), fade_out=1.4)


def e_heartbeat():
    h = load('heartbeat-sounds/heartbeat_slow_reverb.wav', 0.0, 1.8)
    x = mix((h * 3.5, 0, 1.0), (sub(70, 45, 0.3, 11, 0.7), 0.31, 1.0), (sub(70, 45, 0.3, 11, 0.5), 0.58, 1.0))
    return finish(lowpass(x, 700), fade_out=0.3)


def e_horn():
    h = load('war-horns/war_horns.mp3', 34.6, 38.4)
    return finish(reverb(h, 1.8, 0.25, 4200), fade_in=0.12, fade_out=0.9)


def e_door():
    c = load('50-rpg-sound-effects/**/doorOpen_2.ogg')
    s = pitch(load('100-cc0-sfx/**/slam_02.ogg'), -6)
    x = mix((c, 0, 0.8), (s, 1.05, 1.0), (sub(70, 38, 1.2, 3.5, 1.0), 1.05, 1.0), (noise_burst(1.0, 40, 400, 2.0, 0.5), 0.1, 1.0))
    return finish(reverb(x, 1.3, 0.22, 3000), fade_out=0.4)


def e_success():
    a = load('cure-magic/**/Cure1.wav')
    g = load('80-cc0-rpg-sfx/**/item_gem_04.ogg')
    b = pitch(load('100-cc0-sfx/**/bell_02.ogg'), 12)
    x = mix((a, 0, 1.0), (g, 0.2, 0.6), (b, 0.45, 0.5))
    return finish(reverb(x, 1.5, 0.25, 6000), fade_out=0.5)


def e_error():
    m = pitch(load('100-cc0-sfx/**/metal_04.ogg'), -8)
    s = pitch(load('100-cc0-sfx/**/slam_04.ogg'), -9)
    x = mix((m, 0, 0.8), (s, 0.0, 1.0), (sub(110, 60, 0.5, 6, 0.9), 0, 1.0))
    return finish(reverb(x, 0.6, 0.12, 2500), fade_out=0.2)


def e_firework():
    w = highpass(load('air-whoosh/**/whoosh2_0.wav', 0.2, 1.1), 600)
    e = pitch(load('100-cc0-sfx/**/explosion.ogg'), 4)
    sp = load('80-cc0-rpg-sfx/**/spell_fire_06.ogg') * 3
    g = [pitch(load('80-cc0-rpg-sfx/**/item_gem_02.ogg'), s) for s in (7, 12, 16)]
    x = mix((w, 0, 0.7), (e, 0.85, 0.9), (sp, 0.9, 0.7), (g[0], 1.0, 0.5), (g[1], 1.12, 0.5), (g[2], 1.25, 0.4))
    return finish(reverb(x, 1.6, 0.3, 6000), fade_out=0.6)


def e_teleport():
    t = load('teleport-spell/teleport.wav')
    p = load('portal-sound/porta.ogg') * 3
    r = highpass(load('air-whoosh/**/whoosh2_0.wav', 0.0, 1.2)[::-1], 700)
    x = mix((r, 0, 0.6), (t, 0.1, 1.0), (p, 0.9, 0.8), (sub(220, 90, 0.5, 6, 0.4), 0.9, 1.0))
    return finish(reverb(x, 1.3, 0.25, 5500), fade_out=0.45)


def e_notes():
    v = load('pleasing-bell-sound-effect/pleasing-bell.ogg')
    v = v / (np.abs(v).max() + 1e-9)
    base = dominant_hz(v, 200, 2500)
    out = []
    for target in (523.25, 659.25, 783.99, 1046.5):          # do, mi, sol, do aigu
        semis = 12 * np.log2(target / base)
        y = pitch(v, semis)
        out.append(finish(reverb(y, 1.2, 0.25, 6000), fade_out=0.3))
    return out


def e_death():
    d = load('energy-drain/*.ogg')
    hb = load('heartbeat-sounds/heartbeat_slow_reverb.wav', 0.0, 1.8) * 3.5
    c = pitch(load('80-cc0-rpg-sfx/**/creature_die_01.ogg'), -6)
    x = mix((d, 0, 0.9), (lowpass(hb, 700), 0.2, 1.0), (c, 0.8, 0.6), (sub(55, 35, 2.5, 1.2, 0.9), 0.6, 1.0))
    return finish(reverb(x, 2.6, 0.3, 2800), fade_out=1.6)


def _vibe(semis_from_c5):
    v = load('pleasing-bell-sound-effect/pleasing-bell.ogg')
    v = v / (np.abs(v).max() + 1e-9)
    return pitch(v, 12 * np.log2(523.25 / dominant_hz(v, 200, 2500)) + semis_from_c5)


def e_levelup():
    notes = [_vibe(s) for s in (0, 4, 7, 12, 16)]
    x = mix(*[(n, 0.09 * i, 0.9) for i, n in enumerate(notes)], (load('cure-magic/**/Cure5.wav'), 0.25, 0.7),
            (pitch(load('80-cc0-rpg-sfx/**/item_gem_04.ogg'), 5), 0.45, 0.6))
    return finish(reverb(x, 1.8, 0.3, 6500), fade_out=0.7)


def e_quest_accept():
    b = load('80-cc0-rpg-sfx/**/book_02.ogg')
    x = mix((b, 0, 0.8), (_vibe(0), 0.12, 0.9), (_vibe(7), 0.26, 0.9), (pitch(load('80-cc0-rpg-sfx/**/item_gem_01.ogg'), 5), 0.4, 0.5))
    return finish(reverb(x, 1.1, 0.22, 6000), fade_out=0.35)


def e_coins():
    c = load('80-cc0-rpg-sfx/**/item_coins_03.ogg')
    g = pitch(load('80-cc0-rpg-sfx/**/item_gem_03.ogg'), 7)
    return finish(reverb(mix((c, 0, 1.0), (g, 0.15, 0.4)), 0.5, 0.12), fade_out=0.2)


EFFECTS = ['rise', 'boom', 'whoosh', 'bell', 'chime', 'warrior', 'arrow', 'arcane', 'shield', 'fanfare', 'growl', 'ritual', 'heartbeat', 'horn',
           'door', 'success', 'error', 'firework', 'teleport', 'note0', 'note1', 'note2', 'note3', 'death', 'levelup', 'quest_accept', 'coins']


def sounds_json_entries():
    return {f'sfx.{n}': {'category': 'master', 'sounds': [{'name': f'mmorpg:sfx/{n}'}]} for n in EFFECTS}


def generate():
    notes = e_notes()
    total = 0.0
    for name in EFFECTS:
        if name.startswith('note'):
            x = notes[int(name[4:])]
        else:
            x = globals()['e_' + name]()
        d = save(name, x)
        total += d
        print(f'{name:10s} {d:5.2f}s')
    print('effets sonores : %d fichiers, %.1f s' % (len(EFFECTS), total))
    path = os.path.join(ROOT, 'sounds.json')
    data = json.load(open(path, encoding='utf-8'))
    data.update(sounds_json_entries())
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)


if __name__ == '__main__':
    if len(sys.argv) > 1:
        CACHE = sys.argv[1]
    generate()
