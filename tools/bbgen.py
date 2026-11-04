"""Aide : calcule les cubes d'une lame effilee (corps + arete + pointe en tranches fines) a coller dans create_cubes.
Usage : python3 tools/bbgen.py blade y0 y1 halfwidth tipLen [zhalf=.25] [n=10] [cx=8] [prefix=""]"""
import json, sys
def blade(y0, y1, hw, tip, zh=.25, n=10, cx=8, pre=''):
    c = [{'name': f'mt:{pre}lame', 'from': [cx - hw, y0, 8 - zh], 'to': [cx + hw, y1, 8 + zh]},
         {'name': f'hl:{pre}arete', 'from': [cx - hw * .16, y0, 8 - zh - .15], 'to': [cx + hw * .16, y1, 8 + zh + .15]}]
    dy = tip / n
    for i in range(n):
        t = (i + .5) / n
        w = hw * (1 - t) ** .85
        z = zh * (1 - .45 * t)
        a = y1 + i * dy
        c.append({'name': f'{"hl" if i == n - 1 else "mt"}:{pre}t{i}', 'from': [round(cx - w, 3), round(a, 3), round(8 - z, 3)], 'to': [round(cx + w, 3), round(a + dy, 3), round(8 + z, 3)]})
        if i < n * .5:
            r = hw * .16 * (1 - t)
            zr = zh + .15 * (1 - t)
            c.append({'name': f'hl:{pre}r{i}', 'from': [round(cx - r, 3), round(a, 3), round(8 - zr, 3)], 'to': [round(cx + r, 3), round(a + dy, 3), round(8 + zr, 3)]})
    return c
if __name__ == '__main__':
    a = sys.argv[2:]
    print(json.dumps(blade(*[float(x) if '.' in x else int(x) for x in a[:5]], *[(float(x) if '.' in x else int(x)) for x in a[5:8]]), separators=(',', ':')))
