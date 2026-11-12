#!/usr/bin/env python3
"""Transcrit docs/lore/Quetes_Aeloria_v1.md en src/main/resources/data/mmorpg/lore/lore_quests.json (donnees chargees par LoreQuests)."""
import json, re, sys, os, unicodedata
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, 'docs/lore/Quetes_Aeloria_v1.md')
OUT = os.path.join(ROOT, 'src/main/resources/data/mmorpg/lore/lore_quests.json')

def norm(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s) if unicodedata.category(c) != 'Mn').lower()

secs, cur = {}, None
for l in open(SRC, encoding='utf-8').read().split('\n'):
    if l.startswith('## '):
        cur = l[3:].split(' (')[0]; secs[cur] = []
    elif cur and l.startswith('|'):
        secs[cur].append([c.strip() for c in l.strip().strip('|').split(' | ')])
tab = {k: [dict(zip(v[0], r)) for r in v[2:]] for k, v in secs.items() if v}

def qid(code): return code.lower().replace('-', '_')
CLASSES = {'Mage': 'mage', 'Archer': 'archer', 'Guerrier': 'guerrier', 'Tank': 'tank'}
JOBS = [('minage', 'mineur'), ('herboristerie', 'fermier'), ('agriculture', 'fermier'), ('peche', 'pecheur'), ('pecher', 'pecheur'), ('poisson', 'pecheur')]

def rewards(text):
    r = {'xp': 0, 'gold': 0, 'xpAdapt': False, 'evolution': 0, 'extras': []}
    for p in [x.strip() for x in text.split(' · ') if x.strip()]:
        m = re.fullmatch(r'(\d+) XP', p)
        if m: r['xp'] = int(m.group(1)); continue
        m = re.fullmatch(r'(\d+) or', p)
        if m: r['gold'] = int(m.group(1)); continue
        if p == 'XP adaptée au niveau': r['xpAdapt'] = True; continue
        m = re.search(r'palier (\d+)', p)
        if 'évolution' in p and m: r['evolution'] = {25: 1, 50: 2, 75: 3, 100: 4}[int(m.group(1))]; continue
        r['extras'].append(p)
    return r

def objective(typ, text, qty, level, evo=False):
    """Objectifs verifiables par le serveur ; sinon [] (etape narrative : parler au donneur)."""
    t = norm(typ); n = int(qty) if str(qty).isdigit() else 0
    text_n = norm(text)
    m = re.search(r'(?:vaincre|tuer|eliminer|ramasser|liberer|repousser)\s+(\d+)', text_n)
    if t in ('chasse', 'defi', 'enquete', 'action', 'condition horaire', 'entrainement de classe', 'collecte (butin)') and m and 'recolter' not in text_n.split(m.group(0))[0]:
        if t in ('chasse', 'defi', 'condition horaire', 'entrainement de classe', 'collecte (butin)') or 'vaincre' in text_n:
            return [['kill', '*', int(m.group(1))]]
    if t in ('chasse',) and n: return [['kill', '*', n]]
    if t == 'gain de niveau' and n: return [['level', 'niveau', n]]
    if t in ('cible nommee', 'boss'): return [['kill', 'boss', 1]] if level >= 18 else [['kill', '*', 3]]
    if t == 'donjon' and level >= 18: return [['kill', 'boss', 1]]
    if t == 'donjon': return [['kill', '*', 5]]
    if t in ('niveau metier', 'recolte metier'):
        job = next((j for k, j in JOBS if k in text_n), None)
        mm = re.search(r'(?:recolter|pecher)\s+(\d+)', text_n)
        cnt = int(mm.group(1)) if mm else (n if t == 'recolte metier' and n else 5)
        if job: return [['job', job, max(1, cnt)]]
    if t == 'evolution de classe': return [['level', 'niveau', n], ['kill', 'boss', 1]]
    return []

def donor_key(d):
    d = norm(d)
    for k in ('ysmeralde', 'thalgrim', 'brokkar', 'orvald', 'selian', 'halvine', 'ilda', 'ombrelle', 'bondimonde', 'maitre de classe', 'maitre des metiers', 'tableau des primes', 'annonce serveur', 'cadet egare', 'inscription ancelienne', 'rumeur'):
        if k in d: return k.replace(' ', '_')
    return re.sub(r'\W+', '_', d).strip('_')

DAYS = {'janvier':1,'février':2,'mars':3,'avril':4,'mai':5,'juin':6,'juillet':7,'août':8,'septembre':9,'octobre':10,'novembre':11,'décembre':12}
def condition(text):
    """Conditions de disponibilite : {slot:{m,d,from,to}} (heure serveur) ou {when:'night|dawn|storm|sunday'} ou {limit:minutes}."""
    c = {}
    m = re.search(r'Le (\d+) (\w+)(?: \([^)]*\))? (?:entre|de) (\d+)h(\d+) (?:et|à) (\d+)h(\d+)', text)
    if m: c['slot'] = [DAYS[m.group(2)], int(m.group(1)), int(m.group(3)) * 60 + int(m.group(4)), int(m.group(5)) * 60 + int(m.group(6))]
    m = re.search(r'Chaque jour entre (\d+)h(\d+) et (\d+)h(\d+)', text)
    if m: c['slot'] = [0, 0, int(m.group(1)) * 60 + int(m.group(2)), int(m.group(3)) * 60 + int(m.group(4))]
    m = re.search(r'Lancement entre (\d+)h(\d+) et (\d+)h(\d+)', text)
    if m: c['slot'] = [0, 0, int(m.group(1)) * 60 + int(m.group(2)), int(m.group(3)) * 60 + int(m.group(4))]
    tn = norm(text)
    if 'nuit' in tn: c['when'] = 'night'
    if "l'aube" in tn: c['when'] = 'dawn'
    if 'orage' in tn: c['when'] = 'storm'
    if 'dimanche' in tn: c['when'] = 'sunday'
    m = re.search(r'Durée limite : (\d+) min', text)
    if m: c['limit'] = int(m.group(1))
    return c

def date(s):
    d, m, y = s.split('/'); return f'{y}-{m}-{d}'

out = []
for r in tab['Quêtes principales']:
    rw = rewards(r['Récompenses']); lvl = int(r['Niveau'])
    maitre = r['Donneur'] == 'Maître de classe'
    out.append({'id': qid(r['ID']), 'code': r['ID'], 'kind': 'principale', 'name': r['Titre'], 'donor': r['Donneur'], 'level': lvl,
        'requires': [qid(r['Prérequis'])] if r['Prérequis'] not in ('—', '') else [],
        'type': r["Type d'objectif"], 'obj': r['Objectif'], 'objs': objective(r["Type d'objectif"], r['Objectif'], r['Quantité'], lvl),
        'act': r['Acte'], 'chapter': r['Chapitre'], 'region': r['Région'], 'gameplay': r['Gameplay introduit'], 'companion': r['Compagnon'],
        'desc': r['Description / lore'], 'rewards': rw})
    q = out[-1]
    if r["Type d'objectif"] == 'Évolution de classe':
        # les classes et evolutions restent celles du mod : l'Eveil du lore devient une etape de la trame (Flammeche grandit)
        stage = re.search(r'stade (\d)', r['Compagnon'])
        q['obj'] = f"Atteindre le niveau {lvl} et vaincre un boss : Flammèche s'éveille avec vous."
        q['desc'] = f"Éveil du palier {lvl}. Flammèche revient, plus grand" + (f" (stade {stage.group(1)})." if stage else ".")
        q['rewards']['evolution'] = 0
        q['rewards']['extras'] = [x for x in q['rewards']['extras'] if 'classe' not in x]
for r in tab['Quêtes secondaires']:
    if r['Catégorie'] == 'Classe': continue        # classes du mod conservees : pas de quetes de classe du lore
    rw = rewards(r['Récompenses']); lvl = int(r['Niveau min']); rep = r['Répétable']
    repeat = 'weekly' if 'hebdo' in rep else ('daily' if rep.startswith('Oui') else '')
    cl = CLASSES.get(r['Classe'])
    out.append({'id': qid(r['ID']), 'code': r['ID'], 'kind': 'secondaire', 'name': r['Titre'], 'donor': r['Donneur'], 'level': lvl, 'requires': [],
        'type': r["Type d'objectif"], 'obj': r['Objectif'], 'objs': objective(r["Type d'objectif"], r['Objectif'], '', lvl),
        'category': r['Catégorie'], 'region': r['Région'], 'repeat': repeat, 'condText': r['Conditions spéciales'], 'cond': condition(r['Conditions spéciales']),
        'classes': [], 'desc': r['Description'], 'rewards': rw})
ev_prev = {}
for r in tab['Événements']:
    rw = rewards(r['Récompenses']); lvl = int(r['Niveau min'])
    cond = condition(r['Conditions spéciales'])
    out.append({'id': qid(r['ID']), 'code': r['ID'], 'kind': 'evenement', 'name': r['Titre'], 'donor': r['Donneur'], 'level': lvl,
        'requires': [qid(r['Prérequis'])] if r['Prérequis'] not in ('—', '') else [],
        'type': r["Type d'objectif"], 'obj': r['Objectif'], 'objs': objective(r["Type d'objectif"], r['Objectif'], '', lvl),
        'event': r['Événement'], 'from': date(r['Débloquée le']), 'to': date(r["Fin de l'événement"]), 'condText': r['Conditions spéciales'], 'cond': cond,
        'repeat': 'daily' if r['Répétable'].startswith('Oui') else '', 'desc': r["Place dans l'événement"], 'rewards': rw})
needed = {r for q in out for r in q['requires']}
for q in out:                     # une quete exigee par une autre doit pouvoir etre terminee pour de bon : pas de repetition
    if q['id'] in needed: q['repeat'] = ''
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(out, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
import collections
print(len(out), 'quetes', os.path.getsize(OUT) // 1024, 'Ko')
print(collections.Counter((q['kind'], 'verifie' if q['objs'] else 'narratif') for q in out))
