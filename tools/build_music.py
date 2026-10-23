# -*- coding: utf-8 -*-
"""Musiques d'ambiance des donjons a partir de morceaux libres de droits (CC0, OpenGameArt) : une par donjon et une pour l'antre du boss.

Les sources sont recuperees par fetch_sfx_sources.py. Chaque morceau est ramene a un niveau d'ecoute commun, rendu bouclable (fondu enchaine
de la fin sur le debut) et ecrit en OGG stereo dans assets/mmorpg/sounds/music/ ; sounds.json recoit les evenements `mmorpg:dungeon.<nom>`.
Dependances : numpy, scipy, soundfile.

usage : python tools/build_music.py [dossier_du_cache]
"""
import glob
import json
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import resample_poly

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
OUT = os.path.join(ROOT, 'sounds', 'music')
CACHE = os.path.join(HERE, '_sfx_cache')

# nom -> (motif du fichier source, debut en s, fin en s ou None, fondu de bouclage en s)
TRACKS = {
    'crypte_gobeline': ('dungeon-ambience/dungeon002.ogg', 0, None, 4.0),
    'catacombes': ('dark-cavern-ambient/dark_cavern_ambient_002.ogg', 0, None, 1.0),
    'forteresse_infernale': ('hells-god-part-2/dark_forest_combatv2asparag_loop.ogg', 0, None, 1.0),
    'sanctuaire_glace': ('beyond-the-frozen-veil/beyond_the_frozen_veil_preview.ogg', 0, None, 4.0),
    'citadelle_neant': ('node_159643/dark_place.ogg', 0, None, 3.0),
    'boss': ('node_55649/*Seamlessly Looping*.wav', 0, None, 0.0),
}
TARGET_RMS = 0.075


def load(pattern, t0, t1):
    hits = sorted(glob.glob(os.path.join(CACHE, pattern)))
    if not hits:
        raise FileNotFoundError(f'source introuvable : {pattern} (lancez fetch_sfx_sources.py)')
    x, sr = sf.read(hits[0], always_2d=True)
    if sr != SR:
        g = np.gcd(sr, SR)
        x = resample_poly(x, SR // g, sr // g, axis=0)
    if x.shape[1] == 1:
        x = np.repeat(x, 2, axis=1)
    x = x[:, :2]
    return x[int(t0 * SR): len(x) if t1 is None else int(t1 * SR)]


def make_loop(x, fade):
    """Fond enchaine de la fin sur le debut (puissance constante) : la boucle ne claque plus."""
    n = int(fade * SR)
    if n <= 0 or len(x) < 4 * n:
        return x
    head, tail = x[:n], x[-n:]
    t = np.linspace(0, np.pi / 2, n)[:, None]
    mixed = head * np.sin(t) + tail * np.cos(t)
    return np.concatenate([mixed, x[n:-n]])


def normalise(x):
    rms = np.sqrt((x ** 2).mean())
    y = x * (TARGET_RMS / (rms + 1e-9))
    peak = np.abs(y).max()
    if peak > 0.95:
        y = np.tanh(y / peak * 1.2) / np.tanh(1.2) * 0.95
    return y


def generate():
    os.makedirs(OUT, exist_ok=True)
    data = json.load(open(os.path.join(ROOT, 'sounds.json'), encoding='utf-8'))
    for name, (pattern, t0, t1, fade) in TRACKS.items():
        x = normalise(make_loop(load(pattern, t0, t1), fade))
        fn = f'donjon_{name}.ogg'
        # ecriture par blocs : l'encodeur Vorbis de libsndfile deborde de sa pile sur un long morceau ecrit d'un coup (Windows)
        with sf.SoundFile(os.path.join(OUT, fn), 'w', SR, 2, format='OGG', subtype='VORBIS') as out:
            for i in range(0, len(x), 8192):
                out.write(x[i:i + 8192].astype(np.float32))
        data[f'dungeon.{name}'] = {'category': 'music', 'sounds': [{'name': f'mmorpg:music/donjon_{name}', 'stream': True}]}
        print(f'{fn:34s} {len(x) / SR:6.1f} s  {os.path.getsize(os.path.join(OUT, fn)) // 1024} Ko')
    with open(os.path.join(ROOT, 'sounds.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)


if __name__ == '__main__':
    if len(sys.argv) > 1:
        CACHE = sys.argv[1]
    generate()
