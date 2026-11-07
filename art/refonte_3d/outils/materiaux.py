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
    'venin_vert': ramp('#123a12', '#1f6a1c', '#35a02c', '#62d043', '#a6f06a', '#e4ffc0'),
    'rouille': ramp('#1f110d', '#3d2015', '#5f3119', '#83482a', '#a96c44', '#c79a72'),
    # --- matieres communes (toute la collection)
    'cuir': ramp('#2a120b', '#4a1f10', '#6e3216', '#94491f', '#b8672d', '#d48c47'),
    'cuivre_vert': ramp('#0e1e20', '#16323a', '#214a50', '#2f6868', '#468a84', '#6eb0a0', '#a2d4bc'),
    'cuivre': ramp('#3a1e10', '#6a3a1c', '#a05c2a', '#cf8a45', '#eab676', '#fbe0b4'),
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
    # mobs
    'ambre': ramp('#5a2a08', '#8f4c0e', '#c97f18', '#f2b23a', '#ffe07a', '#fff6c8'),
    'champignon': ramp('#3f0a0c', '#6a1215', '#971c1f', '#c22f2a', '#de5140', '#f07f61'),
    'chair_pale': ramp('#5c5140', '#7f735a', '#a39678', '#c4b897', '#ded4b6', '#f0e9d2'),
    'lamelles': ramp('#6a5a3e', '#8c7b58', '#ad9c76', '#c9ba95', '#e0d3b0'),
    'crapaud': ramp('#0b2a26', '#123f38', '#1a5a4b', '#277a60', '#3c9c76', '#65bd8c', '#9bdca6'),
    'crapaud_ventre': ramp('#3f4f1f', '#5f7428', '#86a033', '#a9c446', '#c8dc68', '#e2ee96'),
    'limace': ramp('#4a5410', '#6f7e18', '#98aa22', '#bdd030', '#d8e856', '#f0f8a0'),
    'roseau': ramp('#232d10', '#384618', '#566a24', '#778c34', '#9dac4a', '#c6cd70'),
    'marais': ramp('#101a12', '#1a2a1b', '#273d26', '#365331', '#49693d', '#60824b'),
    'hydre': ramp('#07201b', '#0d332a', '#154a3c', '#1f6551', '#2e856c', '#49a68a', '#7dc9a8'),
    'hydre_ventre': ramp('#3a4a22', '#566b2e', '#76903c', '#98b04f', '#b8cb6a', '#d6e28e'),
    'os': ramp('#3f3829', '#615844', '#857b62', '#a99f83', '#c9c0a3', '#e2dbc1', '#f5f0dd'),
    'os_vieux': ramp('#2e2a20', '#494335', '#67604c', '#878068', '#a69f85', '#c2bca2'),
    'spectre': ramp('#0a1a28', '#10293c', '#183c54', '#22546f', '#31728e', '#4e97ae', '#8cc8d2'),
    'ame': ramp('#0a3a4a', '#0f6075', '#1c90a6', '#3ac6d4', '#86eef0', '#dcffff'),
    'chitine_bleue': ramp('#0b1428', '#142440', '#203a60', '#30557f', '#4676a3', '#6c9fc6'),
    'bandelette': ramp('#4d3f29', '#6f5e40', '#93825c', '#b3a27b', '#cfc09a', '#e6dabb'),
    'bandelette_sale': ramp('#3a3020', '#544631', '#716146', '#8f7e5e', '#ab9a78'),
    'cendre': ramp('#161214', '#262022', '#383033', '#4c4344', '#635858', '#7e7270'),
    'braise': ramp('#3c0f06', '#6e1f08', '#a8360c', '#e05a12', '#fb8f22', '#ffc65a'),
    'rose_chaos': ramp('#340a2a', '#621248', '#981d70', '#d0389c', '#ef7cc6', '#fcc8ea'),
    'gobelin': ramp('#1b2a0d', '#2c4215', '#42611f', '#5b802b', '#79a03c', '#9dbf58'),
    'orc': ramp('#27341a', '#394b24', '#506530', '#6a823e', '#88a052', '#aabd70'),
    'humain': ramp('#3f2618', '#62402a', '#875b3e', '#ab7a56', '#cb9c76', '#e6be9a'),
    'zombie': ramp('#172219', '#263628', '#3a4e3a', '#526a4e', '#6d8664', '#8ea384'),
    'tissu_vert': ramp('#10231a', '#183425', '#234c34', '#316747', '#44865c', '#60a675'),
    'tissu_brun': ramp('#1e140c', '#302115', '#463220', '#5f462d', '#7a5d3c'),
    'tissu_bleu': ramp('#0d1830', '#16264a', '#203a68', '#2e5288', '#436fa8', '#648fc4'),
    'nuit': ramp('#07060c', '#0f0d18', '#1a1726', '#272338', '#37324d', '#4b4566'),
    'corail': ramp('#4d1420', '#7c2230', '#b0383d', '#dc5a4c', '#f38c6c', '#fcc4a2'),
    'nuit_violet': ramp('#0d0b12', '#15121c', '#1e1a28', '#282334', '#6a2bc9', '#8f4cf0', '#e6d4ff'),
    'acier': ramp('#1c2028', '#30363f', '#4a525e', '#6c7683', '#98a2ae', '#cdd5dc', '#f2f6f8'),
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


def plan_masque(ctx, nom='fourrure_brune', base=2.5, ton=0.0, masque=None, fil=0, **_):
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
    if fil:
        # tranchant : les derniers texels pleins de chaque rangee sont plus clairs, le plat un peu plus sombre
        for y in range(H):
            for x in range(W):
                if alpha[y, x]:
                    j = x + fil
                    v[y, x] = base + ton + (1.8 if j >= W or not alpha[y, j] else -.5)
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
        ph = (sum(map(ord, ctx.face)) + ctx.proj.seed) % 7 / 7
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


# ------------------------------------------------------------------ mobs
def ecorce(ctx, nom='ecorce', base=2.6, ton=0.0, pas=5, **_):
    """Ecorce : cotes verticales larges et calmes, sillon sombre net, reflet a cote du sillon, rares fentes en travers. Le dessus d'une piece montre des cernes."""
    from aeloria3d import hash3
    if ctx.face in ('up', 'down'):
        c = (ctx.cube.frm + ctx.cube.to) / 2
        r = np.hypot(ctx.Plocal[..., 0] - c[0], ctx.Plocal[..., 2] - c[2])
        v = base + ton + .6 - 1.2 * (np.mod(np.floor(r), 3) == 0)
        return nom, v
    q = ctx.S + (ctx.noise((.04, .07, .04), 241) - .5) * 10
    r = np.mod(q, pas)
    k = np.floor(q / pas)
    v = base + ton + (hash3(k, 3, 7, 243 + ctx.proj.seed) - .5) * .7
    v = v - 1.25 * (r < 1) + .45 * (r >= pas - 1)
    f = hash3(k, np.floor(ctx.T / 7), 2, 247 + ctx.proj.seed)
    v = v - .9 * ((f > .86) & (np.mod(ctx.T, 7) < 1))
    v = v + (ctx.noise((.06, .06, .06), 245) - .5) * 1.0
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def chapeau(ctx, nom='champignon', base=3.0, ton=0.0, pas=9, r=2.1, tache='chair_pale', bas=False, **_):
    """Chapeau de champignon : degrade, cellules de taches claires nettes, le bord descend."""
    from aeloria3d import hash3
    g = _vgrad(ctx)
    v = base + ton + (g - .5) * 1.4 + .5 * (ctx.noise((.2, .2, .2), 251) - .5)
    v = _face_light(ctx, v, up=.5, down=-1.4)
    return nom, v


def verrues(ctx, nom='crapaud', base=2.8, ton=0.0, pas=6, **_):
    """Peau grumeleuse : pustules rondes de deux texels, cernees de sombre, reparties en quinconce irregulier ; beaucoup de peau calme entre elles."""
    idc, lx, ly = ctx.cells(pas, pas, .5, seed=31)
    cx, cy = lx - pas / 2 + (idc - .5) * 1.5, ly - pas / 2
    d = np.hypot(cx, cy)
    v = base + ton + .6 * (ctx.noise((.1, .1, .1), 257) - .5) * 2
    gros = idc > .42
    v = v + np.where(gros & (d < 1.25), 1.2, 0) + np.where(gros & (d >= 1.25) & (d < 2.1), -.8, 0)
    return nom, _face_light(ctx, v, up=.6, down=-1.1)


def gluant(ctx, nom='limace', base=3.0, ton=0.0, **_):
    """Chair humide : plages lisses, un reflet net en haut de chaque piece, coulures sombres rares vers le bas."""
    g = _vgrad(ctx)
    n = ctx.noise((.3, .08, .3), 263)
    v = base + ton + (g - .5) * 1.4 + (ctx.noise((.07, .1, .07), 261) - .5) * .9
    v = np.where((n > .68) & (g < .55), base + ton - 1.1, v)
    v = v + 1.3 * (ctx.Iv == 1) * (ctx.noise((.18, .4, .18), 265) > .46)
    return nom, _face_light(ctx, v, up=.7, down=-1.1)


def bandelette(ctx, nom='bandelette', base=3.0, ton=0.0, pas=5, pente=.45, **_):
    """Bandelettes de momie : bandes en biais qui font le tour de la piece, bord sombre, usure."""
    from aeloria3d import hash3
    q = ctx.T + ctx.S * pente
    r = np.mod(q, pas)
    idc = hash3(np.floor(q / pas), 5, 3, 271 + ctx.proj.seed)
    v = base + ton + (idc - .5) * .9 + .7 * (r >= pas - 1) - 1.6 * (r < 1)
    v = v + .35 * (ctx.noise((.5, .5, .5), 273) - .5)
    return nom, _face_light(ctx, v, up=.4, down=-.9)


def mailles(ctx, nom='fer', base=2.6, ton=0.0, **_):
    """Cotte de mailles : anneaux en quinconce, deux valeurs nettes."""
    x, y = ctx.Iu, ctx.Iv
    v = base + ton + np.where(np.mod(y, 2) == 0, np.where(np.mod(x + y // 2, 2) == 0, 1.1, -.9), np.where(np.mod(x + y // 2, 2) == 0, .2, -1.6))
    return nom, _face_light(ctx, v, up=.3, down=-.8)


def os_(ctx, nom='os', base=3.6, ton=0.0, **_):
    """Os : surface mate, stries longues, fissures fines et rares, bout clair."""
    n = ctx.noise((.5, .12, .5), 281)
    v = base + ton + (n - .5) * 1.4 + (_vgrad(ctx) - .5) * .6
    n2 = ctx.noise((.2, .2, .2), 283)
    v = np.where(np.abs(n2 - .5) < .012, base + ton - 2.0, v)
    return nom, _face_light(ctx, v, up=.5, down=-1.1)


def plaque(ctx, nom='fer_noir', base=3.0, ton=0.0, bord=1, rivets=0, liseret=None, **_):
    """Plaque d'armure : bord net, reflet doux en haut, patine large, rivets optionnels. liseret : rampe du filet colore."""
    v = base + ton + .8 * (ctx.noise((.1, .1, .1), 287) - .5) * 2 + .6 * (1 - ctx.Iv / max(1, ctx.H - 1))
    u, w, W, H, d = ctx.Iu, ctx.Iv, ctx.W, ctx.H, bord
    if W > 2 * d and H > 2 * d:
        v = np.where((u < d) | (w < d) | (u >= W - d) | (w >= H - d), base + ton - 1.8, v)
        v = v + .9 * (w == d) + .4 * (u == d)
    if rivets and W >= 6 and H >= 6:
        m = (w == d + 1) | (w == H - d - 2)
        v = np.where(m & (np.mod(u - d - 1, rivets) == 0) & (u > d) & (u < W - d - 1), base + ton + 2.2, v)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def voile(ctx, nom='spectre', base=2.6, ton=0.0, pli=7, alpha=None, **_):
    """Etoffe spectrale : plis verticaux, plus claire en haut, s'efface en sombre vers le bas. alpha : etoffe translucide (0 a 255), plus legere vers le bas."""
    g = _vgrad(ctx)
    v = base + ton + .8 * np.sin(ctx.Iu * 2 * np.pi / pli + 1.3 * (ctx.noise((.1, .05, .1), 291) - .5)) + (g - .4) * 1.8
    if alpha is not None:
        return nom, _face_light(ctx, v, up=.5, down=-.9), np.full((ctx.H, ctx.W), alpha, np.uint8)
    return nom, _face_light(ctx, v, up=.5, down=-.9)


def aile_insecte(ctx, nom='chair_pale', base=3.8, ton=0.0, masque=None, alpha=205, nervure=-2.4, n=5, **_):
    """Aile d'insecte en plan horizontal : translucide, nervures en eventail depuis l'attache (x bas), bord sombre. Le masque se lit vu du dessus, attache a gauche."""
    a, b = ctx.cube.frm, ctx.cube.to
    u = np.clip((ctx.Plocal[..., 0] - a[0]) / max(1e-6, b[0] - a[0]), 0, .999)
    if ctx.face in ('north', 'south'):                  # aile de face : le masque se lit de haut en bas
        w = np.clip((b[1] - ctx.Plocal[..., 1]) / max(1e-6, b[1] - a[1]), 0, .999)
        prof = b[1] - a[1]
    else:
        w = np.clip((ctx.Plocal[..., 2] - a[2]) / max(1e-6, b[2] - a[2]), 0, .999)
        prof = b[2] - a[2]
    rows = masque or ['#']
    A = np.zeros(u.shape, np.uint8)
    H, W = u.shape
    plein = np.zeros(u.shape, bool)
    for j in range(H):
        for i in range(W):
            r = rows[int(w[j, i] * len(rows))]
            plein[j, i] = r[int(u[j, i] * len(r))] == '#'
    bord = plein.copy()
    bord[1:-1, 1:-1] = plein[1:-1, 1:-1] & ~(plein[:-2, 1:-1] & plein[2:, 1:-1] & plein[1:-1, :-2] & plein[1:-1, 2:])
    A[plein] = alpha
    v = _full(ctx, base + ton) + (ctx.noise((.1, .1, .1), 293) - .5) * 1.0
    L = b[0] - a[0]
    for k in range(n):
        v0 = .5 + (k - (n - 1) / 2) / max(1, n - 1) * .95
        y0 = .5 + (v0 - .5) * u
        m = np.abs(w - y0) * prof < .55 * ctx.d ** -1
        v = np.where(m, base + ton + nervure, v)
    v = np.where(bord, base + ton + nervure, v)
    A[bord & plein] = 255
    return nom, v, A


def coquille(ctx, nom='turquoise', base=3.0, ton=0.0, autre='pourpre', tours=2.4, **_):
    """Coquille : spirale peinte sur les flancs (est, ouest), bandes transversales sur le dos."""
    a, b = ctx.cube.frm, ctx.cube.to
    if ctx.face in ('east', 'west'):
        cz, cy = (a[2] + b[2]) / 2, (a[1] + b[1]) / 2
        dz, dy = (ctx.Plocal[..., 2] - cz) / max(1e-6, (b[2] - a[2]) / 2), (ctx.Plocal[..., 1] - cy) / max(1e-6, (b[1] - a[1]) / 2)
        r = np.hypot(dz, dy)
        th = np.arctan2(dy, dz) / (2 * np.pi)
        q = r * tours + th
        f = np.mod(q, 1)
        v = base + ton + (f - .5) * 1.8 - 1.8 * (f < .14)
    else:
        q = ctx.Plocal[..., 1 if ctx.face in ('north', 'south') else 2] * ctx.d
        f = np.mod(q, 5)
        v = base + ton + (f / 5 - .5) * 1.4 - 1.6 * (f < 1)
    return nom, _face_light(ctx, v, up=.5, down=-1.0)


def lave(ctx, nom='basalte', base=1.4, ton=0.0, large=.05, **_):
    """Roche fendue de lave : fond sombre, fissures continues d'une face a l'autre, coeur clair au centre de la fissure, bord orange."""
    n1, n2 = ctx.noise((.13, .13, .13), 311), ctx.noise((.34, .34, .34), 313)
    d = np.abs(n1 - .5 + .22 * (n2 - .5))
    v = base + ton + (n2 - .5) * 1.2
    v = np.where(d < large, 4.0, v)
    v = np.where(d < large * .55, 5.0, v)
    v = np.where(d < large * .22, 6.0, v)
    return nom, v


def flamme_pleine(ctx, nom='flamme', base=3.0, ton=0.0, **_):
    """Volume de feu : clair en haut pres du corps, rouge vers le bas, langues verticales qui se decalent."""
    g = _vgrad(ctx)
    n = ctx.noise((.3, .08, .3), 317)
    v = base + ton + (g - .5) * 3.0 + (n - .5) * 2.4
    return nom, _face_light(ctx, v, up=.8, down=-.6)


def roche(ctx, nom='pierre_noire', base=3.0, ton=0.0, **_):
    """Roche fendue : grandes plages de valeur, fissures sombres continues d'une face a l'autre, aucune assise."""
    n1, n2 = ctx.noise((.08, .08, .08), 321), ctx.noise((.25, .25, .25), 323)
    v = base + ton + np.round((n1 - .5) * 5) / 2.5 + (n2 - .5) * .7
    v = np.where(np.abs(n1 - .5 + .2 * (n2 - .5)) < .012, base + ton - 1.9, v)
    return nom, _face_light(ctx, v, up=.6, down=-1.0)


PEINTRES = {'roche': roche, 'lave': lave, 'flamme_pleine': flamme_pleine, 'aile_insecte': aile_insecte, 'coquille': coquille, 'ecorce': ecorce, 'chapeau': chapeau, 'verrues': verrues, 'gluant': gluant, 'bandelette': bandelette, 'mailles': mailles, 'os': os_, 'plaque': plaque, 'voile': voile, 'caisse': caisse, 'peau_tissu': peau_tissu, 'marbre': marbre, 'flamme': flamme, 'feuilles': feuilles, 'braises': braises, 'tuyaux': tuyaux, 'cordage': cordage, 'pics': pics, 'scutes': scutes, 'cuirasse': cuirasse, 'crete_arriere': crete_arriere, 'membrane_pliee': membrane_pliee, 'segments': segments, 'rayures': rayures, 'aile_plumes': aile_plumes, 'aile_membrane': aile_membrane, 'nacre': nacre, 'griffe': griffe, 'creneau_pierre': creneau_pierre, 'plumes': plumes, 'ecailles': ecailles, 'plaques': plaques, 'peau_plissee': peau_plissee, 'poils_longs': poils_longs, 'laine': laine, 'basalte': basalte, 'etoile': etoile, 'glace': glace, 'uni': uni, 'corne': corne, 'fourrure': fourrure, 'peau': peau, 'soies': soies, 'crete': crete, 'plan': plan_masque, 'coeur_puits': coeur_puits, 'toit': toit, 'seau_bois': seau_bois, 'tambour_corde': tambour_corde, 'pignon': pignon, 'cristal': cristal, 'cuir': cuir, 'metal': metal, 'ivoire': ivoire, 'sabot': sabot, 'tissu': tissu,
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
