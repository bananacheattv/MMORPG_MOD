#!/usr/bin/env python3
"""Genere data/mmorpg/lore/world_npcs.json : PNJ donneurs de quetes du spawn (30) et du monde ouvert (200), un par quete.
Deterministe : relancer apres une modification des quetes (lore_quests.json). Les ancres de regions sont modifiables dans le JSON."""
import json, math, hashlib, unicodedata, pathlib
root = pathlib.Path(__file__).resolve().parents[2]
res = root / "src/main/resources/data/mmorpg/lore"
quests = json.load(open(res / "lore_quests.json", encoding="utf-8"))
def norm(s): return unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode().lower()
KEYS = ["ysmeralde","thalgrim","brokkar","orvald","selian","halvine","ilda","ombrelle","bondimonde","maitre de classe","maitre des metiers","tableau des primes","annonce serveur","cadet egare","inscription ancelienne","rumeur"]
def h(s): return int(hashlib.sha1(s.encode()).hexdigest(), 16)
FIRST = ["Aldric","Béatrice","Corvin","Dorian","Elwen","Fenna","Gareth","Hadrien","Isaure","Jorun","Kélia","Lorcan","Mirelle","Nolwenn","Osric","Perrin","Quentin","Rosalie","Sigrun","Tobias","Ulrich","Valène","Wendel","Xanthe","Yvain","Zélie","Anselme","Bastien","Cassiane","Damien","Éloi","Faustine","Gaspard","Héloïse","Ignace","Jeanne","Lazare","Maëlle","Nestor","Odile","Pacôme","Rémi","Solène","Thibault","Urbain","Viviane","Alaric","Brunehilde","Clovis","Dagmar","Edmée","Florian","Garance","Hugolin","Iseult","Jodelle","Kerrian","Lucien","Morgane","Nicodème","Ophélie","Philémon","Roxane","Sévrin","Tiphaine","Ambroise","Blanche","Célestin","Delphine","Eustache","Fabrice","Gwenaëlle","Honorine","Ivo","Jolan","Klervi","Léandre","Mélisande","Norbert","Océane","Prosper","Romane","Silvain","Tanguy","Ursule","Vladimir","Wilfrid","Yseult","Zacharie"]
FIRST = [f for f in FIRST if not any(k in norm(f) for k in KEYS)]
TITLES = {"Exploration":"Éclaireur","Collecte":"Collecteur","Chasse":"Chasseur","Cible nommée":"Traqueur","Libération":"Libérateur","Escorte":"Guide","Énigme":"Érudit","Livraison":"Messager","Fabrication":"Artisan","Donjon":"Gardien","Enquête":"Enquêteur","Dialogue":"Conteur","Choix":"Arbitre","Défi chronométré":"Maître d’épreuve","Récolte métier":"Récolteur","Niveau métier":"Contremaître","Événement":"Organisateur","Défi":"Champion","Action":"Veilleur","Gain de niveau":"Instructeur","Boss":"Chasseur de colosses","Collecte (butin)":"Pilleur repenti"}
SKINS = [0,1,2,3,4,5,6,7,8,14,15,16,17,18]
used = set()
def make(q, zone, idx):
    s = h(q["id"])
    while True:
        name = FIRST[s % len(FIRST)]; s += 7
        t = TITLES.get(q["type"], "Veilleur")
        full = f"{name} · {t}"
        if full not in used and name not in {u.split(" · ")[0] for u in used if u.endswith(t)}: break
    used.add(full)
    return {"id": f"{zone}_{idx:03d}", "name": full, "skin": SKINS[h(q['id'] + 's') % len(SKINS)], "quest": q["id"], "dialogue": (q.get("desc", "") + " " + q.get("ask", "")).strip()}
cands = [q for q in quests if q["kind"] == "secondaire"]
spawn_q = sorted([q for q in cands if q.get("category") == "PNJ de la citadelle"], key=lambda q: (q["level"], q["id"]))[:30]
taken = {q["id"] for q in spawn_q}
def pick(cat, n):
    return [q for q in cands if q.get("category") == cat and q["id"] not in taken][:n]
world_q = pick("Régionale", 140) + pick("Exploration", 20) + pick("Embranchements / fins multiples", 30) + pick("Chronométrée", 10)
assert len(spawn_q) == 30 and len(world_q) == 200, (len(spawn_q), len(world_q))
# regions : ancres en spirale autour de l'arrivee du monde ouvert (0,0), par niveau croissant
regions = {}
for q in world_q: regions.setdefault(q["region"] or "Valdhéris et les Contreforts", []).append(q)
order = sorted(regions, key=lambda r: min(q["level"] for q in regions[r]))
anchors = {}
for i, r in enumerate(order):
    ang = math.radians(i * 137.5); rad = 120 + 55 * i
    anchors[r] = [round(math.cos(ang) * rad), round(math.sin(ang) * rad)]
npcs = []
for i, q in enumerate(spawn_q):
    # anneaux autour du centre de la cite (dx, dz en blocs) : 3 anneaux de 10
    ring, k = divmod(i, 10); rad = 30 + ring * 24; ang = math.radians(k * 36 + ring * 18)
    n = make(q, "spawn", i); n.update(dim="spawn", dx=round(math.cos(ang) * rad), dz=round(math.sin(ang) * rad), region="Cité d’Aeloria"); npcs.append(n)
for r in order:
    for j, q in enumerate(sorted(regions[r], key=lambda q: (q["level"], q["id"]))):
        ang = math.radians(j * 137.5); rad = 8 + 5 * math.sqrt(j) * 3
        n = make(q, "world", len(npcs) - 30); n.update(dim="openworld", region=r, dx=round(math.cos(ang) * rad), dz=round(math.sin(ang) * rad)); npcs.append(n)
json.dump({"regions": anchors, "npcs": npcs}, open(res / "world_npcs.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(npcs), {r: len(v) for r, v in regions.items()})
