#!/usr/bin/env python3
"""Bibliotheque de matieres Aeloria (direction artistique) : palettes limitees et peintres pixel art.

Principe : chaque peintre renvoie un indice de teinte par texel, arrondi sur une rampe de 4 a 6 couleurs (ombres froides, lumieres chaudes).
Beaucoup d'aplats propres, peu de marques mais placees : pas de bruit continu, pas de degrade lisse. Les ombres de contact sont peintes par
Project.paint (occlusion), donc une pose de repos deja lisible sans shader.
"""
import numpy as np

from aeloria3d import hex2rgb


def ramp(*colors):
    return np.array([hex2rgb(c) for c in colors], dtype=np.uint8)


RAMPS = {
    # --- matieres communes (toute la collection)
    'cuir': ramp('#2a120b', '#4a1f10', '#6e3216', '#94491f', '#b8672d', '#d48c47'),
    'cuivre_vert': ramp('#0e1e20', '#16323a', '#214a50', '#2f6868', '#468a84', '#6eb0a0', '#a2d4bc'),
    'marbre': ramp('#5f5c55', '#827f76', '#a3a097', '#c2c0b6', '#dbd9d0', '#efede6', '#fbfaf5'),
    'marbre_sombre': ramp('#16141a', '#25222a', '#37333d', '#4c4752', '#655f6b', '#837c88'),
    'flamme': ramp('#5c1206', '#a22408', '#e0490c', '#f97c14', '#fdb02b', '#fee56f', '#fff9d8'),
    'cire': ramp('#7d6c4d', '#a4916b', '#c7b58f', '#e3d4b1', '#f3e9d0', '#fdf8ea'),
    'ardoise': ramp('#191c24', '#272b35', '#393f4c', '#505766', '#6c7485', '#8d95a6'),
    'amethyste': ramp('#1d0b33', '#34145a', '#532286', '#7a39b5', '#a765dd', '#d7a8f7', '#f7e8ff'),
    'bois_sombre': ramp('#120b07', '#20140c', '#321f12', '#47301c', '#5f4228', '#7a5736'),
    'fer_noir': ramp('#0c0d11', '#17191f', '#262932', '#3a3e49', '#555a67', '#7c8290'),
    'pourpre': ramp('#1c0a2c', '#2f1149', '#471b6c', '#632a91', '#8341b8', '#a968da'),
    'rouge': ramp('#2d0808', '#4f0f0f', '#7a1616', '#a82222', '#d03a32', '#ed6a55'),
    'pierre_noire': ramp('#101114', '#1b1d22', '#2a2d34', '#3d4149', '#555a64', '#737883'),
    'terre': ramp('#1d130b', '#2f2013', '#46301c', '#5f4326', '#7a5833'),
    'feuillage_sombre': ramp('#0a1c10', '#123019', '#1c4824', '#2b6432', '#3f8243', '#5ea356'),
    'fer': ramp('#191b22', '#2c303b', '#474e5c', '#6e7888', '#a0abb9', '#dbe3ec'),
    'or': ramp('#4a2b0c', '#7a4c13', '#ad781d', '#dca534', '#f6d566', '#fff3b6'),
    'ivoire': ramp('#5f4d31', '#8f7a4f', '#bda871', '#e2d5a4', '#f7f0cf', '#fffbe8'),
    'sabot': ramp('#120e0c', '#231a15', '#372a21', '#4f3d2e', '#6a5441'),
    'corde': ramp('#4a3a22', '#6e5832', '#927848', '#b79c64', '#d6bf88'),
    # --- rarete (couleurs de Rarity.java) : tapis et bannieres
    'rar_commun': ramp('#2c2c30', '#47474d', '#6b6b72', '#94949b', '#c0c0c6', '#e6e6ea'),
    'rar_peu_commun': ramp('#0e2a18', '#164526', '#21683a', '#2f9150', '#4bbd6a', '#86e58e'),
    'rar_rare': ramp('#0d1f42', '#16386f', '#2058a8', '#357fd8', '#58a6f5', '#97cbff'),
    'rar_epique': ramp('#1f0f3a', '#351a62', '#522d8c', '#7549bb', '#9f72e6', '#cba6ff'),
    'rar_legendaire': ramp('#43190a', '#74300e', '#ad5315', '#e27f22', '#ffaa3c', '#ffd28a'),
    'rar_mythique': ramp('#3a0a1e', '#651234', '#991b4c', '#d42f68', '#ff5f8e', '#ffa3bd'),
    # --- betes
    'fourrure_brune': ramp('#261814', '#3e2719', '#5a3a22', '#77502e', '#94693b', '#b0864f', '#cba76c'),
    'museau_gris': ramp('#141010', '#231c1a', '#352c28', '#4b403a', '#65574e', '#837268'),
    'soie_noire': ramp('#0d0908', '#191210', '#281c17', '#3a2a20', '#54392a', '#74492f'),
    'museau_rose': ramp('#2f1c1a', '#4e302b', '#71493e', '#936352', '#b28169', '#cfa285'),
    'fourrure_givre': ramp('#2a3448', '#3f4f68', '#5f7690', '#8aa3b8', '#b7cad8', '#dfe9f0', '#f7fbff'),
    'glace': ramp('#14406a', '#1f6a9a', '#38a0cc', '#6fd0e8', '#b4eef7', '#effdff'),
    'fauve': ramp('#2a1a0e', '#452b14', '#6a441d', '#8f6128', '#b38237', '#d3a44f', '#ecc873'),
    'creme': ramp('#4b3a26', '#6f593c', '#957c56', '#b9a077', '#d7c29b', '#efe0c2', '#fbf4e2'),
    'daim': ramp('#2c1c12', '#49301c', '#6c4a2a', '#8e643a', '#ad7f4e', '#c99d6a', '#e0bb8c'),
    'ecorce': ramp('#1f150d', '#322216', '#4a3320', '#64472d', '#7f5e3c'),
    'feuillage': ramp('#16300f', '#24491a', '#356a24', '#4c8f30', '#6fb53e', '#9ad552'),
    'toison': ramp('#6d7690', '#8f97b0', '#b1b9cf', '#d0d6e6', '#e8ecf5', '#fbfcff'),
    'or_corne': ramp('#5a3a0e', '#8a5f17', '#bb8a24', '#e3b53c', '#f8da70', '#fff4b8'),
    'basalte': ramp('#121014', '#1e1a1e', '#2d2729', '#413836', '#ff5a1f', '#ffb02e', '#ffe98a'),
    'sable': ramp('#4f3a22', '#745733', '#9a7a4a', '#bd9c66', '#d9bc86', '#efd9ab', '#fbf0d2'),
    'sable_sombre': ramp('#2e2014', '#4a3420', '#6b4d2e', '#8c6a3f', '#ad8a55'),
    'turquoise': ramp('#0b2e33', '#114a50', '#1a6c70', '#2a9591', '#4fc0b4', '#92e6d4'),
    'ours': ramp('#1a120e', '#2c1e15', '#44301f', '#5d432a', '#7a5a38', '#9a774b', '#bc9a68'),
    'rune': ramp('#0d3f43', '#12686c', '#1fa3a3', '#3ddbcf', '#8ff5e6', '#e4fff9'),
    'chitine': ramp('#2d2013', '#4b361f', '#6e5230', '#927045', '#b6935f', '#d6b880', '#f0dcae'),
    'vide': ramp('#141024', '#201838', '#2f2352', '#443272', '#5d4899', '#8f6ce0', '#ffffff'),
    'chitine_sombre': ramp('#120e18', '#211a2b', '#32283f', '#463857', '#5f4d72', '#7f6c94'),
    'venin': ramp('#3a0d0d', '#6a1414', '#a01e1e', '#d6322a', '#ff6a4a', '#ffb08a'),
    'plume_blanche': ramp('#5a5c68', '#7f818d', '#a3a5af', '#c5c7ce', '#e2e3e8', '#f9f9fb'),
    'plume_fauve': ramp('#3a2413', '#5a3a1e', '#7d552c', '#a0733e', '#c29658', '#dfbd80'),
    'bec': ramp('#5c3a0c', '#8f5f12', '#c28a1c', '#eab535', '#fbd86a', '#fff0a8'),
    'cuivre_sombre': ramp('#1c0e12', '#311a1e', '#4a2628', '#673733', '#874b3e', '#a9664e'),
    'lion_criniere': ramp('#2c1208', '#4a1f0c', '#6e3210', '#964a16', '#bd6a20', '#dd8f33', '#f0b257'),
    'taureau': ramp('#1c1210', '#2f1f19', '#472e24', '#614032', '#7e5643', '#9d7058', '#bd8d72'),
    'lion_or': ramp('#3a2208', '#5f3a0e', '#8a5916', '#b47f22', '#d8a535', '#efc756', '#fbe38e'),
    'museau_bovin': ramp('#2c1f1f', '#463232', '#634848', '#7f6060', '#9c7c78', '#ba9a92', '#d6baae'),
    'gris_peau': ramp('#2a2a2e', '#3f3f45', '#57585f', '#71737a', '#8d8f96', '#a9abb1', '#c6c8cc'),
    'bison': ramp('#1a100a', '#2b1b10', '#402917', '#583a20', '#72502d', '#8e6a3f'),
    'laine_mammouth': ramp('#1b0f0a', '#2e1a10', '#472917', '#633b1f', '#82522a', '#a26e3a', '#c28f52'),
    'elephant': ramp('#262833', '#393c48', '#50535f', '#6a6d79', '#868995', '#a4a7b1'),
    'carapace': ramp('#0f2a22', '#174034', '#22594a', '#2f7663', '#43967f', '#66b89d'),
    'peau_tortue': ramp('#1f2a14', '#30401e', '#465a2b', '#5f763a', '#7b934c', '#9cb165'),
    'dragon_pourpre': ramp('#14081f', '#220f33', '#35184d', '#4c226b', '#673089', '#8646ab', '#a968c9'),
    'dragon_ventre': ramp('#4a2e1a', '#6e4a2a', '#94693e', '#b98c58', '#d6ad78', '#ecd09c'),
    'dragon_or': ramp('#3b1d08', '#6a3a0e', '#a06214', '#d18f22', '#efbb43', '#fde07f'),
    'membrane': ramp('#2a0d2e', '#431648', '#5f2266', '#7f3086', '#a144a6', '#c663c7'),
    # --- blocs
    'pierre': ramp('#2b2c33', '#41434b', '#5b5e67', '#797c85', '#9a9da4', '#bcbec3'),
    'pierre_chaude': ramp('#2f2a2a', '#474040', '#625a57', '#817873', '#a2978f', '#c4b9ad'),
    'mousse': ramp('#1a2a14', '#2a4019', '#3f5a22', '#5a7a2f', '#7c9c3d'),
    'bois': ramp('#20120b', '#351e12', '#4f2e1a', '#6d4325', '#8c5b32', '#ab7743'),
    'bois_clair': ramp('#3a2415', '#573720', '#77502d', '#99693c', '#bc8650', '#d7a56c'),
    'tuile_violette': ramp('#160b28', '#27133f', '#3b1d5c', '#54297e', '#7039a3', '#9354c8', '#b983e6'),
    'eau': ramp('#071a2c', '#0c2c47', '#124465', '#1b6386', '#2d88a8', '#5cb6c9', '#a5e0e4'),
    'cristal': ramp('#2a1250', '#4a1f8a', '#7a35c8', '#a95bf0', '#d6a0ff', '#f4e2ff'),
}


# ------------------------------------------------------------------ aides
def _edges(ctx, tone, top=0.0, bottom=0.0, side=0.0):
    """Biseau peint : ligne du haut plus claire, ligne du bas plus sombre, cotes legerement assombris."""
    if ctx.H >= 2:
        tone[0, :] += top
        tone[-1, :] += bottom
    if ctx.W >= 3 and side:
        tone[:, 0] += side
        tone[:, -1] += side
    return tone


def _face_light(ctx, tone, up=.45, down=-1.1, side=0.0):
    """Teinte par orientation peinte legerement : le jeu ajoute deja son eclairage, on reste sous 1 cran."""
    n = ctx.N
    return tone + up * max(0, n[1]) + down * max(0, -n[1]) + side * (1 - abs(n[1]))


def _vgrad(ctx, y0=None, y1=None):
    a, b = ctx.cube.frm[1], ctx.cube.to[1]
    y0 = a if y0 is None else y0
    y1 = b if y1 is None else y1
    return np.clip((ctx.P[..., 1] - y0) / max(1e-6, y1 - y0), 0, 1)


def _full(ctx, v):
    return np.full((ctx.H, ctx.W), float(v))


# ------------------------------------------------------------------ fourrure et peau
def fourrure(ctx, nom='fourrure_brune', base=3.0, ton=0.0, bas=-1.2, haut=1.0, y=None, taches=.8, traits=.8, dorsale=0.0, dessus=.1, **_):
    """Pelage : valeur verticale du corps (ventre sombre, dos clair), grandes plaques, quelques traits de poils."""
    yr = y or ctx.proj.meta.get('y_corps', (0, 24))
    g = np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]), 0, 1)
    v = base + ton + bas + (haut - bas) * g
    v = v + taches * (ctx.noise((.13, .17, .13), 3) - .5) * 2
    v = v + .55 * (ctx.noise((.42, .28, .42), 7) - .5) * 2
    idc, lx, ly = ctx.cells(3, 5, .5, seed=11)
    trait = (lx < 1) & (ly < 3)
    v = v + traits * ((idc > .80) & trait) - traits * ((idc < .20) & trait)
    if dorsale:
        v = v - dorsale * np.clip(1.15 - np.abs(ctx.P[..., 0]) / 3.2, 0, 1) * np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]) * 2.2 - .9, 0, 1)
    v = _face_light(ctx, v, up=dessus, down=-.8)
    return nom, v


def peau(ctx, nom='museau_rose', base=2.6, ton=0.0, **_):
    v = _full(ctx, base + ton) + .8 * (ctx.noise((.3, .3, .3), 5) - .5) * 2
    v = _face_light(ctx, v, up=.6, down=-1.0)
    return nom, v


def soies(ctx, nom='soie_noire', base=2.0, ton=0.0, **_):
    """Touffes : stries longues, pointes rousses."""
    g = _vgrad(ctx)
    idc, lx, ly = ctx.cells(2, 6, .5, seed=23)
    v = base + ton + (idc - .5) * 2.2 + g * 1.2
    return nom, _face_light(ctx, v, up=.5, down=-.6)


def crete(ctx, nom='soie_noire', base=1.2, ton=0.0, pas=6, dent=5, sens=1, bas=False, ecart=3.0, **_):
    """Crete ou touffe : un seul plan fin dont un bord est decoupe en dents (alpha). Silhouette riche sans multiplier les cubes."""
    from aeloria3d import hash3
    H, W = ctx.H, ctx.W
    z = np.round(ctx.Plocal[..., 2] * ctx.d - .5).astype(int)
    row = H - 1 - ctx.Iv if bas else ctx.Iv
    tooth = np.floor_divide(z, pas)
    phase = np.mod(z, pas) / max(1, pas - 1)
    if sens > 0:
        phase = 1 - phase
    h = hash3(tooth, 2, 9, 71 + ctx.proj.seed)
    limit = (phase * dent) + h * 2.2                    # lignes retirees depuis le bord
    alpha = np.where(row >= limit, 255, 0).astype(np.uint8)
    g = 1 - np.clip((row - limit) / max(1, H * .55), 0, 1)
    idc = hash3(z, np.floor_divide(row, 3), 3, 73 + ctx.proj.seed)
    v = base + ton + g * ecart + (idc - .5) * 1.3
    return nom, v, alpha


def plan_masque(ctx, nom='fourrure_brune', base=2.5, ton=0.0, masque=None, **_):
    """Plan fin decoupe selon un masque ASCII (oreille, etrier, feuille...). '#' plein, '.' vide. Le masque se lit vu depuis le nord, l'est ou le dessus, et les deux faces du plan decoupent la meme forme."""
    H, W = ctx.H, ctx.W
    rows = masque or ['#']
    a, b = ctx.cube.frm, ctx.cube.to
    X, Y, Z = ctx.Plocal[..., 0], ctx.Plocal[..., 1], ctx.Plocal[..., 2]
    if ctx.face in ('north', 'south'):
        tc = (b[0] - X) / max(1e-6, b[0] - a[0])
        tr = (b[1] - Y) / max(1e-6, b[1] - a[1])
    elif ctx.face in ('east', 'west'):
        tc = (b[2] - Z) / max(1e-6, b[2] - a[2])
        tr = (b[1] - Y) / max(1e-6, b[1] - a[1])
    else:
        tc = (X - a[0]) / max(1e-6, b[0] - a[0])
        tr = (Z - a[2]) / max(1e-6, b[2] - a[2])
    alpha = np.zeros((H, W), np.uint8)
    for y in range(H):
        for x in range(W):
            r = rows[min(int(tr[y, x] * len(rows)), len(rows) - 1)]
            if r[min(int(tc[y, x] * len(r)), len(r) - 1)] == '#':
                alpha[y, x] = 255
    v = _full(ctx, base + ton) + .6 * (ctx.noise((.5, .5, .5), 29) - .5) * 2
    return nom, v, alpha


# ------------------------------------------------------------------ harnachement
def cuir(ctx, nom='cuir', base=2.6, ton=0.0, couture=True, **_):
    v = _full(ctx, base + ton) + .6 * (ctx.noise((.5, .5, .5), 9) - .5) * 2
    v = _face_light(ctx, v, up=.7, down=-.8)
    v = _edges(ctx, v, top=.6, bottom=-.7)
    if couture and ctx.W >= 6 and ctx.H >= 5 and ctx.is_side:
        m = np.zeros_like(v, bool)
        m[1, 1:-1] = (np.arange(1, ctx.W - 1) % 3) != 2
        m[-2, 1:-1] = (np.arange(1, ctx.W - 1) % 3) != 2
        v = np.where(m, base + ton + 1.3, v)
    return nom, v


def metal(ctx, nom='fer', base=2.6, ton=0.0, reflet=True, **_):
    """Metal : arete haute brillante, arete basse sombre, reflet diagonal net, peu de bruit."""
    v = _full(ctx, base + ton) + .18 * (ctx.noise((.6, .6, .6), 13) - .5) * 2
    if reflet and ctx.W >= 4 and ctx.H >= 3:
        band = np.mod(ctx.Iu + ctx.Iv, 13)
        v = v + .9 * (band < 2)
    v = _face_light(ctx, v, up=.9, down=-1.2)
    v = _edges(ctx, v, top=1.2, bottom=-1.1, side=-.3)
    return nom, v


def ivoire(ctx, nom='ivoire', base=3.0, ton=0.0, **_):
    g = _vgrad(ctx)
    v = base + ton - .9 + g * 1.8
    rings = np.mod(np.floor(ctx.P[..., 1] * ctx.d), 5) == 0
    v = v - .8 * rings
    return nom, _face_light(ctx, v, up=.4, down=-.8)


def sabot(ctx, nom='sabot', base=2.4, ton=0.0, **_):
    g = _vgrad(ctx)
    v = base + ton - .8 + g * 1.5 + .4 * (ctx.noise((.8, .3, .8), 17) - .5) * 2
    v = _edges(ctx, v, top=.6, bottom=-.6)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def tissu(ctx, nom='rar_rare', base=3.0, ton=0.0, bord=2, liseret='or', motif=True, franges=False, **_):
    """Etoffe : tissage discret, motif en losanges tres leger, liseret d'or (dessine par decalque a la fin)."""
    v = _full(ctx, base + ton)
    if motif:
        d = (np.mod(ctx.Iu + ctx.Iv, 8) == 0) | (np.mod(ctx.Iu - ctx.Iv, 8) == 0)
        v = v - .7 * d
    v = v + .25 * ((ctx.Iu + ctx.Iv) % 2 == 0) - .12
    v = _face_light(ctx, v, up=.5, down=-1.0)
    if bord and liseret:
        def _trim(c, face):
            H, W = face.shape[:2]
            from materiaux import RAMPS as R
            o = R[liseret]
            for t in range(min(bord, H // 2, W // 2)):
                shade = o[4] if t == 0 else o[2]
                face[t, t:W - t, :3] = shade
                face[H - 1 - t, t:W - t, :3] = o[2] if t == 0 else o[1]
                face[t:H - t, t, :3] = shade
                face[t:H - t, W - 1 - t, :3] = shade
            if franges and H > 3:
                face[-1, ::2, 3] = 0
        ctx.proj.meta.setdefault('_post', []).append((ctx.cube.name, ctx.face, _trim))
    return nom, v


# ------------------------------------------------------------------ blocs
def maconnerie(ctx, nom='pierre', base=3.0, ton=0.0, assise=8, long=16, joint=True, mousse=None, varie=False, **_):
    """Pierres appareillees : assises continues d'un cube a l'autre, longueurs variees par rang, teinte propre a chaque pierre, biseau et joints sombres."""
    from aeloria3d import hash3
    row = np.floor(ctx.T / assise)
    tid = np.where(ctx.is_side, 1, 0)
    if varie:
        lr = long + np.floor((hash3(row, 7, tid, 37 + ctx.proj.seed) - .5) * long * .7)
    else:
        lr = np.full_like(row, long, dtype=float)
    off = hash3(row, 3, tid, 39 + ctx.proj.seed) * lr
    sx = ctx.S + off
    col = np.floor(sx / lr)
    h = hash3(col, row, tid, 41 + ctx.proj.seed)
    v = base + ton + np.select([h < .2, h > .78], [-.9, .85], 0.0)
    lx, ly = sx - col * lr, ctx.T - row * assise
    v = v + .4 * (ctx.noise((.35, .35, .35), 19) - .5) * 2
    speck = hash3(np.floor(ctx.S / 2), np.floor(ctx.T / 2), col * 7 + tid, 43 + ctx.proj.seed)
    v = v - .8 * (speck > .94)
    if joint:
        v = np.where(ly < 1, v + .9, v)                         # arete haute eclairee
        v = np.where(lx < 1, v + .4, v)                         # arete gauche
        v = np.where((ly >= assise - 2) & (ly < assise - 1), v - .6, v)   # bas de la pierre
        v = np.where((lx >= lr - 2) & (lx < lr - 1), v - .4, v)           # droite de la pierre
        jm = (ly >= assise - 1) | (lx >= lr - 1)
        v = np.where(jm, base + ton - 2.3, v)
    v = _face_light(ctx, v, up=.5, down=-1.4)
    return nom, v


def dalle(ctx, nom='pierre', base=3.2, ton=0.0, pas=12, **_):
    """Margelle / dessus : grandes dalles, joints fins."""
    return maconnerie(ctx, nom, base, ton, assise=pas, long=pas * 2)


def bois(ctx, nom='bois', base=2.6, ton=0.0, planche=6, sens='y', **_):
    """Bois : fil dans le sens de la poutre, planches, noeuds rares, extremites en cernes."""
    P = ctx.P
    from aeloria3d import hash3
    long_axis = {'x': 0, 'y': 1, 'z': 2}[sens]
    n_axis = np.argmax(np.abs(ctx.N))
    d = ctx.d
    if n_axis == long_axis:                             # bout de poutre : cernes
        c = (ctx.cube.frm + ctx.cube.to) / 2
        ax = [k for k in (0, 1, 2) if k != long_axis]
        r = np.maximum(np.abs(P[..., ax[0]] - c[ax[0]]), np.abs(P[..., ax[1]] - c[ax[1]]))
        v = base + ton + 1.0 - 1.2 * (np.mod(np.floor(r * d), 3) == 0)
        return nom, _edges(ctx, v, side=-.4, top=-.2, bottom=-.5)
    # coordonnee en travers du fil
    across = [k for k in (0, 1, 2) if k != long_axis and k != n_axis][0]
    q = np.floor(P[..., across] * d)
    along = P[..., long_axis] * d
    plank = np.floor(q / planche)
    seed = 53 + ctx.proj.seed
    tone_plank = (hash3(plank, n_axis, 3, seed) - .5) * 1.4
    streak = hash3(q, np.floor((along + hash3(q, 1, 1, seed) * 40) / 9), n_axis, seed + 1)
    v = base + ton + tone_plank + .9 * (streak > .78) - .9 * (streak < .22)
    joint = np.mod(q, planche) == planche - 1
    v = np.where(joint, base + ton - 1.8, v)
    v = _face_light(ctx, v, up=.5, down=-1.1)
    v = _edges(ctx, v, top=.5, bottom=-.6)
    return nom, v


def tuiles(ctx, nom='tuile_violette', base=3.2, ton=0.0, l=8, h=6, **_):
    """Ardoises en ecailles : rangs decales, chaque ardoise arrondie en bas, lumiere en haut."""
    from aeloria3d import hash3
    row = np.floor(ctx.T / h)
    sx = ctx.S + np.mod(row, 2) * l / 2
    col = np.floor(sx / l)
    tid = hash3(col, row, 5, 61 + ctx.proj.seed)
    lx, ly = sx - col * l, ctx.T - row * h
    v = base + ton + (tid - .5) * 1.8
    v = np.where(ly < 1, v + 1.0, v)
    edge = (ly >= h - 1) | (lx < .99)
    v = np.where(edge, base + ton - 1.9, v)
    corner = (ly >= h - 2) & ((lx < 1.5) | (lx >= l - 1))
    v = np.where(corner, base + ton - 1.6, v)
    return nom, v


def corde(ctx, nom='corde', base=2.2, ton=0.0, **_):
    v = base + ton + 1.4 * (np.mod(ctx.Iu + ctx.Iv, 3) == 0) - .6 * (np.mod(ctx.Iu + ctx.Iv, 3) == 2)
    return nom, v


def eau(ctx, nom='eau', base=2.4, **_):
    """Surface d'eau opaque (pas de tri de transparence) : ondes concentriques nettes, reflets rares."""
    c = (ctx.cube.frm + ctx.cube.to) / 2
    r = np.hypot(ctx.P[..., 0] - c[0], ctx.P[..., 2] - c[2])
    v = base + 1.2 * (np.mod(np.floor(r * ctx.d * .5 + 1.5 * ctx.noise((.2, .2, .2), 31)), 5) == 0)
    v = v + .9 * (ctx.noise((.25, .25, .25), 37) - .5) * 2
    sp = (ctx.noise((.55, .55, .55), 41) > .82)
    return nom, np.where(sp, base + 3.2, v)



def coeur_puits(ctx, **_):
    """Masse du puits : cotes en pierre, dessus = surface de l'eau."""
    if ctx.face == 'up':
        return eau(ctx)
    return maconnerie(ctx, 'pierre')


def toit(ctx, **_):
    """Pan de toit : ardoises dessus, voliges dessous, chant en bois sombre."""
    if ctx.face == 'up':
        return tuiles(ctx)
    if ctx.face == 'down':
        return bois(ctx, 'bois', ton=-.3, planche=5, sens='z')
    return bois(ctx, 'bois', ton=-.8, planche=3, sens='x')


def seau_bois(ctx, **_):
    """Douelles verticales ; dessus : eau sombre."""
    if ctx.face == 'up':
        return eau(ctx, base=1.2)
    return bois(ctx, 'bois_clair', ton=-.6, planche=3, sens='y')


def tambour_corde(ctx, **_):
    """Tambour : spires de corde sur les quatre cotes visibles."""
    if ctx.face in ('east', 'west'):
        return bois(ctx, 'bois', ton=-.4, sens='x')
    v = 2.0 + 1.5 * (np.mod(ctx.Iu, 2) == 0) + .8 * (np.mod(ctx.Iu + ctx.Iv * 2, 4) == 0) - .3
    return 'corde', v


def pignon(ctx, **_):
    """Pignon ferme : planches decoupees en triangle exactement sous les pans du toit (faite, demi-largeur et pente lues dans proj.meta)."""
    y = ctx.Plocal[..., 1]
    z = np.abs(ctx.Plocal[..., 2])
    fy, tang = ctx.proj.meta['faite_y'], ctx.proj.meta['pente_tan']
    limit = (fy - 1.5 - y) / tang
    alpha = np.where(z <= limit, 255, 0).astype(np.uint8)
    name, v = bois(ctx, 'bois', ton=-.2, planche=5, sens='y')
    return name, v, alpha


def cristal(ctx, nom='cristal', base=1.6, ton=0.0, **_):
    """Cristal d'Aeloria : facettes nettes, arete lumineuse. Grandes faces : plusieurs facettes triangulaires, chacune sa valeur."""
    g = _vgrad(ctx)
    u = (ctx.Iu + .5) / max(1, ctx.W)
    if ctx.W >= 8 and ctx.H >= 8 and ctx.is_side:
        t = (ctx.Iv + .5) / ctx.H
        ph = (hash(ctx.face) + ctx.proj.seed) % 7 / 7
        s1, s2, s3 = t > .25 + .4 * u + ph * .2, t > .95 - .7 * u, u > .5 + .2 * t - ph * .2
        fid = s1.astype(int) + 2 * s2.astype(int) + 4 * s3.astype(int)
        val = np.array([0, .9, -.5, .5, 1.5, -.2, .7, .2])[fid]
        v = base + ton + val + g * .8
        lim = np.zeros_like(fid, dtype=bool)
        lim[1:, :] |= fid[1:, :] != fid[:-1, :]
        lim[:, 1:] |= fid[:, 1:] != fid[:, :-1]
        v = np.where(lim, base + ton + 2.6, v)
        return nom, _face_light(ctx, v, up=.8, down=-1)
    v = base + ton + 1.2 * (u < .5) + g * 1.0
    v = _face_light(ctx, v, up=.8, down=-1)
    if ctx.W >= 3 and ctx.is_side:
        v[:, ctx.W // 2] += 1.4
    return nom, v


def mousse_sur(hauteur, seed=0):
    """Decalque de mousse : plaques nettes sur le bas d'une face, plus denses vers le sol."""
    from aeloria3d import hash3

    def fn(ctx, face):
        y = ctx.P[..., 1]
        n = ctx.noise((.16, .22, .16), 83 + seed)
        n2 = ctx.noise((.5, .6, .5), 89 + seed)
        limite = np.clip((hauteur - y) / hauteur, 0, 1)
        m = (n * .75 + n2 * .25) < (limite * .62)
        r = RAMPS['mousse']
        tone = np.clip(np.rint(1.4 + (n2 - .3) * 4).astype(int), 0, len(r) - 1)
        face[m, :3] = r[tone[m]]
    return fn



def plumes(ctx, nom='plume_blanche', base=3.0, ton=0.0, l=6, h=4, **_):
    """Plumes en rangs decales : extremite en U sombre, jamais de joint vertical complet (pas d'effet de briques)."""
    from aeloria3d import hash3
    row = np.floor(ctx.T / h)
    sx = ctx.S + np.mod(row, 2) * l / 2
    col = np.floor(sx / l)
    tid = hash3(col, row, 9, 83 + ctx.proj.seed)
    lx, ly = sx - col * l, ctx.T - row * h
    v = base + ton + (tid - .5) * .9 + .6 * ((ly < 1) & (np.abs(lx - l / 2) < l / 2 - 1))
    u = ((ly >= h - 1) & (lx >= 1) & (lx < l - 1)) | ((ly >= h - 2) & ((lx < 1) | (lx >= l - 1)))
    v = np.where(u, base + ton - 1.5, v)
    return nom, _face_light(ctx, v, up=.4, down=-.8)


def ecailles(ctx, nom='dragon_pourpre', base=3.0, ton=0.0, l=4, h=4, bas=-.8, haut=.8, y=None, **_):
    """Ecailles en losanges : pointe haute claire, base sombre, aucun alignement horizontal ni vertical (ni paves ni planches). Plus sombres vers le bas si un y de corps est donne."""
    from aeloria3d import hash3
    u, w = ctx.S / l + ctx.T / h, ctx.S / l - ctx.T / h
    iu, iw = np.floor(u), np.floor(w)
    fu, fw = u - iu, w - iw
    t = (fu + 1 - fw) / 2                                 # 0 en haut de l'ecaille, 1 en bas
    tid = hash3(iu, iw, 11, 87 + ctx.proj.seed)
    yr = y or ctx.proj.meta.get('y_corps', (0, 24))
    g = np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]), 0, 1)
    b = base + ton + bas + (haut - bas) * g
    v = b + (tid - .5) * .6 + .5 * (t < .34) - .85 * (t > .74)
    return nom, _face_light(ctx, v, up=.4, down=-.7)


def scutes(ctx, nom='carapace', base=3.0, ton=0.0, l=18, h=14, joint=1.6, anneaux=4, **_):
    """Ecailles de carapace : grands hexagones, joint sombre, biseau, stries de croissance concentriques, centre plus clair."""
    from aeloria3d import hash3
    S, T = ctx.S, ctx.T
    j0 = np.floor(T / h)
    b1 = np.full(S.shape, 1e9)
    b2 = b1.copy()
    idn = np.zeros(S.shape)
    for dj in (-1, 0, 1):
        j = j0 + dj
        off = np.mod(j, 2) * l / 2
        i0 = np.floor((S - off) / l)
        for di in (-1, 0, 1):
            i = i0 + di
            d = np.hypot(S - (i * l + off + l / 2), (T - (j * h + h / 2)) * (l / h) * .87)
            new = d < b1
            b2 = np.where(new, b1, np.minimum(b2, d))
            idn = np.where(new, hash3(i, j, 23, 131 + ctx.proj.seed), idn)
            b1 = np.where(new, d, b1)
    e = b2 - b1
    v = base + ton + (idn - .5) * .8 + .9 * (1 - np.clip(b1 / (l * .55), 0, 1)) - .5 * (np.mod(b1, anneaux) < 1)
    v = v + .5 * ((e >= joint) & (e < joint + 1.2))
    v = np.where(e < joint, base + ton - 2.0, v)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def plaques(ctx, nom='carapace', base=3.0, ton=0.0, l=14, h=10, joint=1.5, **_):
    """Grandes plaques cuirassees : joints sombres, biseau clair, teinte tres peu variable d'une plaque a l'autre."""
    from aeloria3d import hash3
    row = np.floor(ctx.T / h)
    sx = ctx.S + np.mod(row, 2) * l / 2
    col = np.floor(sx / l)
    tid = hash3(col, row, 13, 91 + ctx.proj.seed)
    lx, ly = sx - col * l, ctx.T - row * h
    v = base + ton + (tid - .5) * .6
    v = v + .6 * (ly < 1) + .25 * (lx < 1)
    v = np.where((ly >= h - 1) | (lx >= l - 1), base + ton - joint, v)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def peau_plissee(ctx, nom='elephant', base=2.6, ton=0.0, bas=-.7, haut=.8, y=None, **_):
    """Peau epaisse : grandes plaques de valeur et plis horizontaux nets."""
    yr = y or ctx.proj.meta.get('y_corps', (0, 24))
    g = np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]), 0, 1)
    v = base + ton + bas + (haut - bas) * g + .8 * (ctx.noise((.12, .16, .12), 21) - .5) * 2
    pli = (ctx.noise((.22, .9, .22), 23) > .66)
    v = np.where(pli, v - .9, v)
    tache = (ctx.noise((.8, .8, .8), 25) > .78)
    v = v + .6 * tache
    return nom, _face_light(ctx, v, up=.3, down=-.8)


def poils_longs(ctx, nom='laine_mammouth', base=3.0, ton=0.0, l=2, h=14, bas=-.8, haut=.8, y=None, **_):
    """Meches pendantes : fines colonnes qui gardent leur teinte sur toute la longueur, pointes decalees d'une meche a l'autre, jamais de rectangles (pas d'effet de briques)."""
    from aeloria3d import hash3
    col = np.floor(ctx.S / l)
    tcol = hash3(col, 1, 15, 95 + ctx.proj.seed)
    dec = hash3(col, 2, 16, 96 + ctx.proj.seed) * h
    ly = np.mod(ctx.T + dec, h)
    yr = y or ctx.proj.meta.get('y_corps', (0, 24))
    g = np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]), 0, 1)
    v = base + ton + bas + (haut - bas) * g + (tcol - .5) * 1.5
    if ctx.is_side:
        v = v + .55 * (1 - ly / h) * 2 - .55
        v = v - 1.1 * (ly >= h - 1)
    v = v - .45 * (np.mod(ctx.S, l) >= l - 1)
    return nom, _face_light(ctx, v, up=.25, down=-.8)


def laine(ctx, nom='toison', base=3.0, ton=0.0, **_):
    """Toison bouclee : petites boucles rondes, ombre en bas de chaque boucle."""
    from aeloria3d import hash3
    row = np.floor(ctx.T / 3)
    sx = ctx.S + np.mod(row, 2) * 2
    col = np.floor(sx / 4)
    tid = hash3(col, row, 19, 101 + ctx.proj.seed)
    lx, ly = sx - col * 4, ctx.T - row * 3
    v = base + ton + (tid - .5) * 1.1 + .7 * ((ly < 1) & (lx >= 1) & (lx < 3)) - 1.0 * ((ly >= 2) & ((lx < 1) | (lx >= 3)))
    return nom, _face_light(ctx, v, up=.4, down=-1.0)


def basalte(ctx, nom='basalte', base=1.4, ton=0.0, **_):
    """Basalte fendu de lave : roche sombre, fissures fines et vives, jamais en plaques."""
    n = ctx.noise((.2, .28, .2), 29)
    n2 = ctx.noise((.6, .6, .6), 31)
    v = base + ton + .9 * (n2 - .5) * 2 + .5 * (ctx.P[..., 1] > ctx.proj.meta.get('y_corps', (0, 24))[1] * .6) + .6 * (ctx.N[1] > .5)
    f = np.abs(n - .5) < .02
    v = np.where(f, 4.0 + 1.0 * (n2 > .6), np.minimum(v, 3.2))
    return nom, v


def etoile(ctx, nom='vide', base=2.4, ton=0.0, densite=.972, **_):
    """Pelage de nuit : violet profond, nebuleuses, rares etoiles blanches."""
    from aeloria3d import hash3
    n = ctx.noise((.14, .16, .14), 33)
    v = base + ton + 2.4 * (n - .45) + .5 * (ctx.P[..., 1] > ctx.proj.meta.get('y_corps', (0, 24))[1] * .7)
    h = hash3(np.floor(ctx.S), np.floor(ctx.T), {'north': 1, 'south': 2, 'east': 3, 'west': 4, 'up': 5, 'down': 6}[ctx.face], 103 + ctx.proj.seed)
    v = np.minimum(v, 4.4)
    return nom, np.where(h > densite, 6.0, np.where(h > densite - .03, 5.0, v))


def glace(ctx, nom='glace', base=2.0, ton=0.0, **_):
    """Eclat de glace : facettes franches, arete blanche."""
    g = _vgrad(ctx)
    u = ctx.Iu / max(1, ctx.W - 1)
    v = base + ton + 1.2 * (u < .5) + g * 1.3
    v = _face_light(ctx, v, up=.9, down=-.8)
    if ctx.W >= 2 and ctx.is_side:
        v[:, ctx.W // 2] += 1.2
    return nom, v


def uni(ctx, nom='peau_tortue', base=2.6, ton=0.0, **_):
    """Matiere lisse a peine marbree (peau, corne, chair)."""
    v = _full(ctx, base + ton) + .7 * (ctx.noise((.3, .3, .3), 35) - .5) * 2
    return nom, _face_light(ctx, v, up=.5, down=-.9)


def corne(ctx, nom='ivoire', base=2.4, ton=0.0, anneaux=4, **_):
    """Corne : anneaux de croissance, plus claire vers la pointe."""
    g = _vgrad(ctx)
    v = base + ton + g * 1.8 - .8 * (np.mod(np.floor(ctx.Plocal[..., 1] * ctx.d), anneaux) == 0)
    return nom, _face_light(ctx, v, up=.4, down=-.8)



def rayures(ctx, nom='sable', base=3.2, ton=0.0, pas=9, large=3, sombre=-2.0, y0=None, **_):
    """Robe rayee : aplats clairs, peu de bruit, bandes sombres transversales sur le dessus du corps uniquement."""
    yr = ctx.proj.meta.get('y_corps', (0, 24))
    g = np.clip((ctx.P[..., 1] - yr[0]) / (yr[1] - yr[0]), 0, 1)
    v = base + ton - .7 + 1.2 * g + .5 * (ctx.noise((.2, .3, .2), 39) - .5) * 2
    idc, lx, ly = ctx.cells(4, 3, .5, seed=17)
    v = v + .7 * ((idc > .84) & (lx < 2) & (ly < 1)) - .7 * ((idc < .14) & (lx < 2) & (ly < 1))
    z = ctx.P[..., 2] * ctx.d
    dep = 3 * (ctx.noise((.3, .3, .3), 37) - .5)
    band = np.mod(np.floor(z + dep), pas) < large
    lim = y0 if y0 is not None else yr[0] + 6
    v = np.where(band & (ctx.P[..., 1] > lim), v + sombre, v)
    return nom, _face_light(ctx, v, up=.3, down=-.7)


def aile_plumes(ctx, nom='plume_blanche', base=3.0, ton=0.0, pas=8, dent=8, **_):
    """Aile d'oiseau : plan fin (faces dessus et dessous), bord de fuite decoupe en grandes plumes, rangs de couvertures."""
    from aeloria3d import hash3
    H, W = ctx.H, ctx.W
    xx = np.round(np.abs(ctx.Plocal[..., 0]) * ctx.d - .5).astype(int)
    zz = (ctx.Plocal[..., 2] - ctx.cube.frm[2]) * ctx.d
    t = np.floor_divide(xx, pas)
    ph = np.mod(xx, pas) / max(1, pas - 1)
    lim = H - (dent * (1 - np.abs(2 * ph - 1)) + hash3(t, 1, 3, 111 + ctx.proj.seed) * 3)
    alpha = np.where(zz <= lim, 255, 0).astype(np.uint8)
    rows = np.floor_divide(np.floor(zz).astype(int), 6)
    tid = hash3(t, rows, 5, 113 + ctx.proj.seed)
    v = base + ton + (tid - .5) * 1.2 + .8 * (np.mod(zz, 6) < 1) - 1.2 * (np.mod(xx, pas) == 0)
    v = v - .8 * (zz > H * .55)
    return nom, v, alpha


def aile_membrane(ctx, nom='membrane', base=3.0, ton=0.0, doigts=4, os_nom='dragon_pourpre', **_):
    """Aile de dragon : membrane en plan fin, bord de fuite festonne entre les doigts, doigts sombres, veines."""
    H, W = ctx.H, ctx.W
    u = (np.abs(ctx.Plocal[..., 0]) - abs(ctx.cube.frm[0] if abs(ctx.cube.frm[0]) < abs(ctx.cube.to[0]) else ctx.cube.to[0])) / max(1e-6, abs(ctx.cube.to[0] - ctx.cube.frm[0]))
    zz = (ctx.Plocal[..., 2] - ctx.cube.frm[2]) / max(1e-6, ctx.cube.to[2] - ctx.cube.frm[2])
    f = u * doigts
    frac = f - np.floor(f)
    bord = .52 + .46 * (2 * np.abs(frac - .5)) ** 1.5                  # feston : creux entre deux doigts
    bord = bord * (1 - .25 * u)
    alpha = np.where(zz <= bord, 255, 0).astype(np.uint8)
    v = base + ton + 1.3 * (1 - zz) - .6 * (np.mod(ctx.Iu + ctx.Iv // 3, 11) == 0)
    doigt = (np.abs(frac - .5) > .46) | (zz < .05)
    v = np.where(doigt, base + ton - 2.2, v)
    return nom, v, alpha


def nacre(ctx, nom='dragon_ventre', base=3.2, ton=0.0, h=5, axe='z', joint=1.0, **_):
    """Plaques de ventre de reptile : bandes transversales claires a joints discrets. axe : direction le long de laquelle les bandes se suivent."""
    q = ctx.Plocal[..., {'x': 0, 'y': 1, 'z': 2}[axe]] * ctx.d
    row = np.floor(q / h)
    v = base + ton + .35 * (np.mod(row, 2) == 0)
    v = v + .4 * (np.mod(q, h) >= h - 1)
    v = np.where(np.mod(q, h) < 1, base + ton - joint, v)
    return nom, _face_light(ctx, v, up=.3, down=-.2)


def griffe(ctx, nom='ivoire', base=1.8, ton=0.0, **_):
    g = _vgrad(ctx)
    return nom, base + ton + g * 2.2


def creneau_pierre(ctx, nom='pierre', base=3.2, ton=0.0, **_):
    return maconnerie(ctx, nom, base, ton, assise=4, long=8)



def segments(ctx, nom='chitine', base=3.0, ton=0.0, pas=8, axe='z', **_):
    """Anneaux de chitine ou de cuirasse : bandes transversales nettes, arete claire, joint sombre (ni briques ni bruit)."""
    i = {'x': 0, 'y': 1, 'z': 2}[axe]
    q = ctx.Plocal[..., i] * ctx.d
    r = np.mod(q, pas)
    from aeloria3d import hash3
    tid = hash3(np.floor(q / pas), 2, 21, 121 + ctx.proj.seed)
    v = base + ton + (tid - .5) * .8 + .8 * (r >= pas - 1.0) - 1.9 * (r < 1.0)
    v = v + .35 * (ctx.noise((.4, .4, .4), 43) - .5) * 2
    return nom, _face_light(ctx, v, up=.6, down=-.9)



def cuirasse(ctx, nom='gris_peau', base=3.2, ton=0.0, bord=1, **_):
    """Plaque d'un seul tenant : contour sombre sur toute la face, biseau clair en haut et a gauche, patine large. La forme vient du cube, pas d'un quadrillage."""
    v = base + ton + .9 * (ctx.noise((.09, .09, .09), 37) - .5) * 2 + .35 * (ctx.noise((.4, .4, .4), 39) - .5) * 2
    u, w, W, H, d = ctx.Iu, ctx.Iv, ctx.W, ctx.H, bord
    v = v + .9 * (w < d + 1) + .4 * (u < d + 1)
    v = np.where((u < d) | (w < d) | (u >= W - d) | (w >= H - d), base + ton - 2.0, v)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def crete_arriere(ctx, nom='dragon_pourpre', base=1.2, ton=0.0, pas=6, dent=5, ecart=3.0, devant=False, **_):
    """Crete pour un plan qui suit un cou ou une queue : bord arriere (z max local) decoupe en dents couchees, meme decoupe sur les deux faces."""
    from aeloria3d import hash3
    y = np.round(ctx.Plocal[..., 1] * ctx.d - .5).astype(int)
    z0, z1 = ctx.cube.frm[2], ctx.cube.to[2]
    col = (ctx.Plocal[..., 2] - z0) * ctx.d if devant else (z1 - ctx.Plocal[..., 2]) * ctx.d
    tooth = np.floor_divide(y, pas)
    phase = np.mod(y, pas) / max(1, pas - 1)
    limit = phase * dent + hash3(tooth, 2, 9, 71 + ctx.proj.seed) * 1.5
    alpha = np.where(col >= limit, 255, 0).astype(np.uint8)
    largeur = max(1.0, (z1 - z0) * ctx.d)
    v = base + ton + (1 - np.clip((col - limit) / (largeur * .7), 0, 1)) * ecart
    return nom, v, alpha


def membrane_pliee(ctx, nom='membrane', base=3.0, ton=0.0, doigts=4, **_):
    """Aile de dragon repliee le long du flanc : plan vertical, doigts en eventail depuis le poignet (haut avant), bord festonne entre les doigts."""
    z0, z1 = ctx.cube.frm[2], ctx.cube.to[2]
    y0, y1 = ctx.cube.frm[1], ctx.cube.to[1]
    ux = (ctx.Plocal[..., 2] - z0) / max(1e-6, z1 - z0)
    uy = (y1 - ctx.Plocal[..., 1]) / max(1e-6, y1 - y0)
    r = np.hypot(ux, uy)
    f = np.arctan2(uy, ux) / (np.pi / 2) * doigts
    frac = f - np.floor(f)
    bord = .66 + .34 * (2 * np.abs(frac - .5)) ** 1.5
    alpha = np.where(r <= bord, 255, 0).astype(np.uint8)
    v = base + ton + 1.2 * (1 - r) - .5 * (np.mod(ctx.Iu + ctx.Iv // 3, 11) == 0)
    v = np.where((np.abs(frac - .5) > .46) | (r < .06), base + ton - 2.0, v)
    return nom, v, alpha


def pics(ctx, nom='dragon_or', base=1.5, ton=0.0, pas=8, dent=6, ecart=3.0, axe='z', pointe=None, **_):
    """Pics nets d'une crete de dragon : triangles reguliers couches, base sombre, pointe claire. axe z : dents vers le haut le long d'une queue ; axe y : dents vers l'arriere le long d'un cou."""
    a, b = ctx.cube.frm, ctx.cube.to
    if axe == 'z':
        q = ctx.Plocal[..., 2] * ctx.d
        prof = (b[1] - ctx.Plocal[..., 1]) * ctx.d
        tip = .68 if pointe is None else pointe
    else:
        q = ctx.Plocal[..., 1] * ctx.d
        prof = (b[2] - ctx.Plocal[..., 2]) * ctx.d
        tip = .32 if pointe is None else pointe
    ph = np.mod(q, pas) / pas
    tri = np.where(ph < tip, ph / tip, (1 - ph) / (1 - tip))
    limit = dent * (1 - tri)
    alpha = np.where(prof >= limit, 255, 0).astype(np.uint8)
    v = base + ton + ecart * (1 - np.clip(prof / max(1, dent), 0, 1))
    return nom, v, alpha


def marbre(ctx, nom='marbre', base=3.4, ton=0.0, **_):
    """Marbre clair de cathedrale : grandes plages douces, veines fines et rares, biseau haut."""
    n1, n2 = ctx.noise((.05, .08, .05), 51), ctx.noise((.17, .17, .17), 53)
    v = _full(ctx, base + ton) + (n1 - .5) * .55 + (n2 - .5) * .3
    v = np.where(np.abs(n1 - .5 + .32 * (n2 - .5)) < .012, v - .9, v)
    v = _face_light(ctx, v, up=.4, down=-1.0)
    return nom, _edges(ctx, v, top=.4, bottom=-.5)


def flamme(ctx, nom='flamme', base=1.4, ton=0.0, **_):
    """Flamme en plan fin : ventre en bas, pointe en haut, coeur clair, bord rouge. Meme decoupe sur les deux faces."""
    a, b = ctx.cube.frm, ctx.cube.to
    if ctx.face in ('north', 'south'):
        u = ((ctx.Plocal[..., 0] - a[0]) / max(1e-6, b[0] - a[0])) * 2 - 1
    else:
        u = ((ctx.Plocal[..., 2] - a[2]) / max(1e-6, b[2] - a[2])) * 2 - 1
    t = (ctx.Plocal[..., 1] - a[1]) / max(1e-6, b[1] - a[1])
    n = ctx.noise((.3, .18, .3), 61)
    large = (1 - t) ** .75 * (.62 + .38 * np.sin(np.pi * np.minimum(1, t * 2.4)))
    large = large + (n - .5) * .3 * (t > .12)
    alpha = (np.abs(u) <= large).astype(np.uint8) * 255
    coeur = np.clip(1 - np.abs(u) / np.maximum(.02, large), 0, 1)
    v = base + ton + 3.4 * coeur * (1 - .35 * t) + 1.0 * (1 - t)
    return nom, v, alpha


def feuilles(ctx, nom='feuillage', base=2.8, ton=0.0, **_):
    """Feuillage : touffes rondes, trous sombres, dessus eclaire, jamais de quadrillage."""
    n1, n2 = ctx.noise((.2, .2, .2), 71), ctx.noise((.5, .5, .5), 73)
    v = _full(ctx, base + ton) + (n1 - .5) * 2.2 + (n2 - .5) * 1.4
    v = np.where(n2 + .4 * n1 < .42, base + ton - 1.8, v)
    v = np.where(n2 + .3 * n1 > 1.0, base + ton + 1.6, v)
    return nom, _face_light(ctx, v, up=.7, down=-1.1)


def braises(ctx, nom='basalte', base=4.4, ton=0.0, **_):
    """Lit de braises : charbon sombre, fissures incandescentes."""
    n1, n2 = ctx.noise((.3, .3, .3), 81), ctx.noise((.7, .7, .7), 83)
    v = np.where(n1 + .5 * n2 > .82, base + ton + (n2 - .3) * 3, 1.6 + (n2 - .5))
    return nom, v


def tuyaux(ctx, nom='fer', base=3.0, ton=0.0, pas=6, **_):
    """Tuyaux d'orgue peints sur une face : colonnes rondes, reflet central, bouche sombre en bas."""
    q = np.mod(ctx.Iu, pas)
    v = base + ton + 1.2 * (q == pas // 2) + .5 * (q == pas // 2 - 1) - 1.8 * (q == 0)
    v = v - 1.5 * ((ctx.H - ctx.Iv) % 30 < 2)
    return nom, _face_light(ctx, v, up=.4, down=-.8)


def cordage(ctx, nom='corde', base=2.2, ton=0.0, pas=4, **_):
    v = base + ton + 1.2 * (np.mod(ctx.Iu + ctx.Iv, pas) < pas / 2) - .5
    return nom, v


def caisse(ctx, nom='bois_clair', base=3.0, ton=0.0, cadre=4, **_):
    """Caisse de bois : cadre de planches sombres sur chaque face, panneau de planches, diagonale sur les cotes."""
    u, w, W, H = ctx.Iu, ctx.Iv, ctx.W, ctx.H
    q = np.mod(u, 6)
    from aeloria3d import hash3
    v = base + ton + (hash3(np.floor(u / 6), 2, 3, 201 + ctx.proj.seed) - .5) * 1.0 - 1.4 * (q == 5)
    v = v + .4 * (ctx.noise((.4, .05, .4), 203) - .5) * 2
    bord = (u < cadre) | (u >= W - cadre) | (w < cadre) | (w >= H - cadre)
    if ctx.is_side and W > 2 * cadre + 4 and H > 2 * cadre + 4:
        diag = np.abs((u - cadre) * (H - 2 * cadre) - (w - cadre) * (W - 2 * cadre)) < max(W, H) * 1.6
        v = np.where(diag, base + ton - .3, v)
    v = np.where(bord, base + ton - 1.0 + .6 * ((u < 1) | (w < 1)) - .8 * ((u >= W - 1) | (w >= H - 1)), v)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def peau_tissu(ctx, nom='rouge', base=3.0, ton=0.0, pli=8, **_):
    """Etoffe pendante : plis verticaux doux, sans motif (robes, rideaux, draps)."""
    v = base + ton + .9 * np.sin(ctx.Iu * 2 * np.pi / pli) + .4 * (ctx.noise((.15, .05, .15), 211) - .5) * 2
    return nom, _face_light(ctx, v, up=.5, down=-.9)


PEINTRES = {'caisse': caisse, 'peau_tissu': peau_tissu, 'marbre': marbre, 'flamme': flamme, 'feuilles': feuilles, 'braises': braises, 'tuyaux': tuyaux, 'cordage': cordage, 'pics': pics, 'scutes': scutes, 'cuirasse': cuirasse, 'crete_arriere': crete_arriere, 'membrane_pliee': membrane_pliee, 'segments': segments, 'rayures': rayures, 'aile_plumes': aile_plumes, 'aile_membrane': aile_membrane, 'nacre': nacre, 'griffe': griffe, 'creneau_pierre': creneau_pierre, 'plumes': plumes, 'ecailles': ecailles, 'plaques': plaques, 'peau_plissee': peau_plissee, 'poils_longs': poils_longs, 'laine': laine, 'basalte': basalte, 'etoile': etoile, 'glace': glace, 'uni': uni, 'corne': corne, 'fourrure': fourrure, 'peau': peau, 'soies': soies, 'crete': crete, 'plan': plan_masque, 'coeur_puits': coeur_puits, 'toit': toit, 'seau_bois': seau_bois, 'tambour_corde': tambour_corde, 'pignon': pignon, 'cristal': cristal, 'cuir': cuir, 'metal': metal, 'ivoire': ivoire, 'sabot': sabot, 'tissu': tissu,
            'maconnerie': maconnerie, 'dalle': dalle, 'bois': bois, 'tuiles': tuiles, 'corde': corde, 'eau': eau}


def post_process(proj):
    """Applique les liserets differes (dessines apres ombres de contact, pour rester nets)."""
    img = proj.texture
    cubes = {c.name: c for c in proj.cubes}
    for name, face, fn in proj.meta.pop('_post', []):
        c = cubes[name]
        u0, v0, u1, v1 = c.faces[face]
        fn(c, img[v0:v1, u0:u1])


def sprite(rows, palette):
    """Decalque ASCII : '.' transparent, sinon couleur de la palette. Renvoie (hauteur, largeur, liste)."""
    pts = []
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.':
                pts.append((x, y, hex2rgb(palette[ch])))
    return len(rows), max(len(r) for r in rows), pts


def stamp(rows, palette, along, y, flip_on=('west',), center=False):
    """Decalque place par coordonnees de surface. Sur les faces de flip_on, le motif est retourne (symetrie gauche/droite)."""
    h, w, pts = sprite(rows, palette)

    def fn(ctx, face):
        cx, cy = ctx.xy(along, y)
        x0, y0 = int(round(cx)), int(round(cy))
        if center:
            x0 -= w // 2
            y0 -= h // 2
        flip = ctx.face in flip_on
        for px, py, col in pts:
            xx = x0 + (w - 1 - px if flip else px)
            yy = y0 + py
            if 0 <= yy < face.shape[0] and 0 <= xx < face.shape[1] and face[yy, xx, 3] > 0:
                face[yy, xx, :3] = col
    return fn
