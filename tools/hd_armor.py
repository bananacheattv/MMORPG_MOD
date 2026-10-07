"""Icones HD (64 x 64) des pieces d'armure : casque, plastron, jambieres, bottes ; styles plate, robe et cuir."""
from hd_render import Layer, compose, hexrgb, lighten, darken, poly, ellipse, union, minus, thick_line, new_mask
from PIL import ImageChops


def _mat(style):
    return {'plate': 'metal', 'robe': 'cloth', 'leather': 'leather'}[style]


def _clip(a, b):
    return ImageChops.darker(a, b)


def helmet(style, base, trim, gem, seed):
    m = _mat(style)
    if style == 'plate':
        dome = union(ellipse((46, 30, 210, 190)), poly([(46, 110), (210, 110), (204, 200), (172, 222), (84, 222), (52, 200)]))
        slit, d = new_mask()
        d.rectangle((70, 118, 186, 134), fill=255)
        d.rectangle((120, 118, 136, 190), fill=255)
        crest = poly([(116, 14), (140, 14), (146, 110), (110, 110)])
        rim = _clip(thick_line([(50, 112), (206, 112)], 14), dome)
        return [Layer(minus(dome, slit), base, m, seed, bevel=14, spec=1),
                Layer(slit, '#0c0a10', 'organic', seed, bevel=2, spec=0),
                Layer(union(crest, rim), trim, 'gold', seed, bevel=6, spec=1),
                Layer(ellipse((114, 64, 142, 92)), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]
    hood = poly([(128, 18), (196, 70), (222, 200), (176, 230), (80, 230), (34, 200), (60, 70)], smooth=10)
    face = poly([(128, 92), (170, 130), (164, 196), (92, 196), (86, 130)], smooth=8)
    band = _clip(thick_line([(46, 196), (128, 230), (210, 196)], 16), hood)
    return [Layer(hood, base, m, seed, bevel=14, spec=.3), Layer(face, '#0c0a12', 'organic', seed, bevel=10, spec=0),
            Layer(band, trim, 'gold' if style == 'robe' else 'leather', seed, bevel=5, spec=.8),
            Layer(ellipse((116, 46, 140, 70)), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]


def chest(style, base, trim, gem, seed):
    m = _mat(style)
    if style == 'robe':
        body = poly([(86, 30), (170, 30), (214, 70), (196, 120), (180, 104), (196, 232), (60, 232), (76, 104), (60, 120), (42, 70)])
        sash = _clip(thick_line([(76, 120), (180, 150)], 16), body)
        hem = _clip(thick_line([(60, 224), (196, 224)], 14), body)
        collar = poly([(100, 30), (156, 30), (128, 76)])
        return [Layer(body, base, m, seed, bevel=14, spec=.3), Layer(union(sash, hem), trim, 'gold', seed, bevel=5, spec=.8),
                Layer(collar, darken(hexrgb(base), .5), 'cloth', seed, bevel=4, spec=0),
                Layer(ellipse((114, 124, 142, 152)), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]
    body = poly([(80, 40), (176, 40), (192, 110), (182, 214), (128, 230), (74, 214), (64, 110)])
    pads = union(ellipse((26, 38, 100, 106)), ellipse((156, 38, 230, 106)))
    neck = poly([(104, 40), (152, 40), (128, 70)])
    plate = _clip(union(thick_line([(78, 120), (178, 120)], 10), thick_line([(84, 170), (172, 170)], 10)), body)
    return [Layer(body, base, m, seed, bevel=16, spec=.9 if style == 'plate' else .35),
            Layer(pads, lighten(hexrgb(base), .1), m, seed + 1, bevel=14, spec=.9 if style == 'plate' else .35),
            Layer(plate, trim, 'gold' if style == 'plate' else 'leather', seed, bevel=4, spec=.8),
            Layer(neck, '#0c0a12', 'organic', seed, bevel=4, spec=0),
            Layer(poly([(128, 84), (148, 104), (128, 124), (108, 104)]), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]


def legs(style, base, trim, gem, seed):
    m = _mat(style)
    belt = poly([(62, 34), (194, 34), (194, 66), (62, 66)])
    if style == 'robe':
        skirt = poly([(66, 60), (190, 60), (214, 228), (42, 228)])
        slit = poly([(124, 120), (132, 120), (140, 228), (116, 228)])
        return [Layer(minus(skirt, slit), base, m, seed, bevel=14, spec=.3),
                Layer(belt, trim, 'gold', seed, bevel=6, spec=.8),
                Layer(ellipse((114, 36, 142, 64)), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]
    left = poly([(64, 60), (126, 60), (120, 230), (74, 230)])
    right = poly([(130, 60), (192, 60), (182, 230), (136, 230)])
    knees = union(ellipse((70, 120, 120, 164)), ellipse((136, 120, 186, 164)))
    return [Layer(union(left, right), base, m, seed, bevel=14, spec=.9 if style == 'plate' else .3),
            Layer(belt, trim, 'gold' if style == 'plate' else 'leather', seed, bevel=6, spec=.8),
            Layer(knees, lighten(hexrgb(trim), .1), 'gold' if style == 'plate' else m, seed, bevel=8, spec=.9),
            Layer(ellipse((116, 38, 140, 62)), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]


def boots(style, base, trim, gem, seed):
    m = _mat(style)
    left = poly([(42, 70), (98, 70), (102, 180), (128, 196), (124, 226), (32, 226), (36, 180)])
    right = poly([(140, 52), (196, 52), (200, 168), (226, 184), (222, 214), (130, 214), (134, 168)])
    cuffs = union(poly([(36, 62), (104, 62), (104, 92), (36, 92)]), poly([(134, 44), (202, 44), (202, 74), (134, 74)]))
    soles = union(poly([(30, 214), (126, 214), (126, 230), (30, 230)]), poly([(128, 202), (224, 202), (224, 218), (128, 218)]))
    return [Layer(union(left, right), base, m, seed, bevel=12, spec=.9 if style == 'plate' else .35),
            Layer(cuffs, trim, 'gold' if style != 'leather' else 'leather', seed, bevel=6, spec=.8),
            Layer(soles, '#2a2228', 'leather', seed, bevel=4, spec=.2),
            Layer(union(ellipse((58, 66, 82, 90)), ellipse((156, 48, 180, 72))), gem, 'gem', seed, bevel=4, spec=1, glow=gem)]


PIECES = {'casque': helmet, 'plastron': chest, 'jambieres': legs, 'bottes': boots}


def armor_icon(piece, style, base, trim, gem, seed=0, halo=None):
    return compose(PIECES[piece](style, base, trim, gem, seed), halo=halo)
