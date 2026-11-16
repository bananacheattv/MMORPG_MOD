#!/usr/bin/env python3
"""Modèle d'équilibrage donjons solo (formules recopiées du code Java)."""
import sys, json, math

# ------------------------------------------------------------------ joueur
CLS = {
 'guerrier': dict(base=[8,5,2,6,3,6,3,4], gr=[2.0,1.0,0.2,1.2,0.3,1.6,0.4,0.6], hp=110, hpl=13, mana=2, main=(0,5)),
 'mage':     dict(base=[2,4,10,4,7,1,3,2], gr=[0.2,0.6,2.4,0.8,1.4,0.1,0.4,0.4], hp=75, hpl=7, mana=6, main=(2,4)),
 'archer':   dict(base=[4,10,3,4,3,3,8,3], gr=[0.8,2.2,0.4,1.0,0.6,0.5,1.8,0.5], hp=85, hpl=9, mana=3, main=(1,6)),
 'tank':     dict(base=[6,2,2,10,4,3,2,9], gr=[1.2,0.3,0.2,2.5,0.8,0.5,0.3,2.0], hp=150, hpl=19, mana=2, main=(3,7)),
}
# index: FOR AGI INT VIT ESP FER DEX END
# sets (niveau, totals dict, bonus2 dict, bonus4pct dict) -- totals incl. 2nd attribute rule
def S(**k): return k
SETS = {
 'guerrier': [
  (1, S(hp=80,de=12,atk=5,FOR=4), S(atk=5), S(atk=.05)),
  (25, S(hp=300,de=40,atk=20,crit=2,FOR=10), S(atk=15), S(atk=.08,cd=.10)),
  (50, S(hp=700,de=90,atk=50,crit=4,FOR=20), S(atk=30), S(atk=.12,hp=.05)),
  (85, S(hp=1500,de=180,atk=100,crit=6,FOR=35), S(atk=50), S(atk=.15,cd=.20,hp=.08)),
  (100,S(hp=2100,de=250,atk=150,crit=8,FOR=45), S(atk=70), S(atk=.18,cd=.25,hp=.10))],
 'mage': [
  (1, S(hp=50,mana=60,mag=8,de=5,INT=4), S(mag=6), S(mag=.05)),
  (25, S(hp=180,mana=200,mag=30,de=18,mr=1.5,INT=10), S(mag=18), S(mag=.08,mana=.10)),
  (50, S(hp=400,mana=450,mag=70,de=40,mr=3,cdr=6,INT=20), S(mag=35), S(mag=.12,mr=.20)),
  (85, S(hp=900,mana=900,mag=140,de=80,mr=5,cdr=10,INT=35), S(mag=60), S(mag=.15,mana=.15,hp=.08)),
  (100,S(hp=1300,mana=1300,mag=210,de=110,mr=7,cdr=12,INT=45), S(mag=85), S(mag=.20,mana=.20,hp=.10))],
 'archer': [
  (1, S(hp=60,de=7,atk=5,crit=2,esq=1,spd=2,AGI=4), S(crit=2), S(atk=.05)),
  (25, S(hp=220,de=25,atk=18,crit=5,esq=3,spd=4,AGI=10), S(crit=3), S(atk=.08,esq=.10)),
  (50, S(hp=500,de=60,atk=45,crit=8,esq=5,spd=6,AGI=20), S(crit=4), S(atk=.12,cd=.15)),
  (85, S(hp=1100,de=120,atk=90,crit=12,esq=8,spd=10,AGI=35), S(crit=5), S(atk=.15,cd=.25)),
  (100,S(hp=1600,de=170,atk=140,crit=14,esq=10,spd=12,AGI=45), S(crit=7), S(atk=.18,cd=.30))],
 'tank': [
  (1, S(hp=120,de=18,atk=2,hr=.5,VIT=4), S(de=8), S(de=.08)),
  (25, S(hp=450,de=70,atk=8,hr=2,VIT=10), S(de=25), S(de=.12,hp=.05)),
  (50, S(hp=1000,de=160,atk=20,hr=4,VIT=20), S(de=50), S(de=.16,hp=.08)),
  (85, S(hp=2200,de=320,atk=40,hr=8,VIT=35), S(de=90), S(de=.20,hp=.12)),
  (100,S(hp=3100,de=460,atk=60,hr=12,VIT=45), S(de=125), S(de=.25,hp=.15))],
}
def interp_set(cls, lvl):
    L = SETS[cls]
    best = L[0]
    for i, s in enumerate(L):
        if s[0] <= lvl: best = s
    # interpolation entre deux voisins (panoplies intermédiaires)
    lo = max([s for s in L if s[0] <= lvl], key=lambda s: s[0], default=L[0])
    hi = min([s for s in L if s[0] > lvl], key=lambda s: s[0], default=lo)
    # pour les niveaux entre deux sets, une panoplie intermédiaire existe vers lvl 15/35/65/75 : on interpole à t=0.5
    if lo is hi: return lo
    t = 0.0 if lvl - lo[0] < (hi[0]-lo[0])*0.25 else 0.5
    def mix(a, b):
        k = set(a)|set(b); return {x: a.get(x,0)+(b.get(x,0)-a.get(x,0))*t for x in k}
    return (lo[0], mix(lo[1],hi[1]), mix(lo[2],hi[2]), mix(lo[3],hi[3]))

WEAPONS = {  # (niveau, dict)
 'guerrier': [(1,dict(atk=8)),(5,dict(atk=14,FOR=1)),(10,dict(atk=22,crit=2,FOR=3)),(18,dict(atk=36,crit=3,FOR=4)),(25,dict(atk=55,cd=15,hp=50,FOR=6)),(35,dict(atk=80,hp=100,ls=2,FOR=8)),(45,dict(atk=110,hp=150,ls=3,FOR=10)),(58,dict(atk=150,crit=5,cd=20,FOR=13)),(70,dict(atk=190,crit=6,ls=6,FOR=16)),(82,dict(atk=250,crit=8,ls=5,hp=250,FOR=20)),(95,dict(atk=320,crit=10,cd=40,hp=400,FOR=25))],
 'mage': [(1,dict(mag=10,mana=20)),(5,dict(mag=16,mana=35)),(10,dict(mag=26,mana=50,INT=3)),(18,dict(mag=42,mana=80,mr=1,INT=4)),(25,dict(mag=65,crit=3,mana=100,INT=6)),(35,dict(mag=95,mana=150,crit=4,INT=8)),(45,dict(mag=130,mana=200,mr=2,cdr=5,INT=10)),(58,dict(mag=175,mana=250,cdr=7,INT=13)),(70,dict(mag=220,crit=5,mana=300,INT=16)),(82,dict(mag=290,mana=400,crit=7,mr=4,INT=20)),(95,dict(mag=360,mana=500,cdr=10,mr=5,INT=25))],
 'archer': [(1,dict(atk=7,crit=2)),(5,dict(atk=12,crit=3)),(10,dict(atk=20,crit=4,spd=3,AGI=3)),(18,dict(atk=34,crit=5,spd=3,AGI=4)),(25,dict(atk=50,crit=6,esq=2,AGI=6)),(35,dict(atk=75,crit=7,esq=3,AGI=8)),(45,dict(atk=100,crit=8,cd=20,AGI=10)),(58,dict(atk=140,crit=9,cd=25,AGI=13)),(70,dict(atk=175,crit=10,esq=4,AGI=16)),(82,dict(atk=235,crit=12,cd=35,esq=5,AGI=20)),(95,dict(atk=300,crit=14,cd=50,AGI=25))],
 'tank': [(1,dict(atk=6,de=6,hp=30)),(5,dict(atk=10,de=10,hp=55)),(10,dict(atk=16,de=15,hp=80,VIT=3)),(18,dict(atk=28,de=25,hp=140,VIT=4)),(25,dict(atk=40,de=35,hp=200,VIT=6)),(35,dict(atk=60,de=52,hp=300,hr=1,VIT=8)),(45,dict(atk=80,de=70,hp=400,hr=2,VIT=10)),(58,dict(atk=110,de=95,hp=550,hr=3,VIT=13)),(70,dict(atk=140,de=120,hp=700,VIT=16)),(82,dict(atk=190,de=160,hp=950,VIT=20)),(95,dict(atk=240,de=200,hp=1200,VIT=25))],
}
PRIM = ['FOR','AGI','INT','VIT','ESP','FER','DEX','END']
SECOND = {'guerrier':('FOR','FER'),'mage':('INT','ESP'),'archer':('AGI','DEX'),'tank':('VIT','END')}

def gear(cls, lvl):
    ws = [w for w in WEAPONS[cls] if w[0] <= lvl]
    w = dict(ws[-1][1]) if ws else {}
    s = interp_set(cls, lvl)
    tot = dict(s[1]); b2 = s[2]; b4 = s[3]
    for d in (w, tot):
        a, b = SECOND[cls]
        if d.get(a,0) > 0 and d.get(b,0) == 0: d[b] = max(1, round(d[a]*0.6))
    flat = {}
    for d in (w, tot, b2):
        for k,v in d.items(): flat[k] = flat.get(k,0)+v
    return flat, b4

def stats(cls, lvl, alloc_split=(0.6,0.4)):
    c = CLS[cls]
    flat, b4 = gear(cls, lvl)
    pts = (lvl-1)*3
    alloc = [0]*8
    alloc[c['main'][0]] += pts*alloc_split[0]; alloc[c['main'][1]] += pts*alloc_split[1]
    prim = [c['base'][i] + c['gr'][i]*(lvl-1) + alloc[i] + flat.get(PRIM[i],0) for i in range(8)]
    FOR,AGI,INT,VIT,ESP,FER,DEX,END = prim
    evo = 1 + 0.05*min(4, lvl//25)
    ar = cls == 'archer'
    atk = (5 + lvl*.5 + FOR*(0.6 if ar else 1.4) + AGI*(1.3 if ar else .6) + FER*.5 + DEX*.4 + flat.get('atk',0))*evo*(1+b4.get('atk',0))
    mag = (2 + lvl*.5 + INT*2 + ESP*.5 + flat.get('mag',0))*evo*(1+b4.get('mag',0))
    hp = (c['hp'] + c['hpl']*(lvl-1) + VIT*12 + FOR*2 + END*5 + flat.get('hp',0))*evo*(1+b4.get('hp',0))
    mana = (50 + c['mana']*(lvl-1) + INT*6 + ESP*4 + flat.get('mana',0))*(1+b4.get('mana',0))
    de = (lvl*.5 + VIT*.8 + FOR*.3 + END*1.0 + flat.get('de',0))*evo*(1+b4.get('de',0))
    crit = min(75, 5 + AGI*.06 + DEX*.08 + flat.get('crit',0))
    cd = 150 + FOR*.05 + FER*.5 + flat.get('cd',0) + 100*b4.get('cd',0)
    esq = min(45, AGI*.07 + flat.get('esq',0))*(1+b4.get('esq',0))
    mr = (1 + ESP*.12 + INT*.03 + flat.get('mr',0))*(1+b4.get('mr',0))
    cdr = min(40, ESP*.03 + flat.get('cdr',0))
    ls = FER*.02 + flat.get('ls',0)
    return dict(atk=atk, mag=mag, hp=hp, mana=mana, de=de, crit=crit, cd=cd, esq=esq, mr=mr, cdr=cdr, ls=ls)

def rank(lvl): return 1 + 4*(lvl-1)/99.0
def rm(r): return 1 + 0.15*(r-1)

# ------------------------------------------------------------------ monstres
MOBS = {  # niveau min,max, hp, hpL, atk, atkL, defL
 'gobelin': (8,2.4,60,22,1.0), 'loup_sombre': (10,2.8,80,25,.8),
}
def mob_stats(base_hp, hpl, base_atk, atkl, defl, lvl):
    return dict(hp=base_hp+hpl*lvl, atk=base_atk+atkl*lvl, de=defl*lvl)

MOBCFG = {  # (baseHp, hpL, baseAtk, atkL, defL)
 'gobelin': (60,22,8,2.4,1.0), 'loup_sombre': (80,25,10,2.8,.8),
 'squelette_maudit': (120,28,12,3.0,1.2), 'orc_guerrier': (300,45,20,4.5,2.0),
 'elementaire_feu': (400,55,25,5.0,2.0), 'spectre_givre': (450,60,28,5.5,2.2),
 'golem_cristal': (900,90,35,6.0,3.5), 'chevalier_neant': (1000,100,40,7.0,3.5),
}
DUNGEONS = [
 ('crypte_gobeline', 15, ['gobelin','loup_sombre'], 'roi_gobelin', 20),
 ('catacombes', 35, ['squelette_maudit','orc_guerrier'], 'liche_ancienne', 45),
 ('forteresse_infernale', 55, ['elementaire_feu','orc_guerrier'], 'seigneur_ignis', 65),
 ('sanctuaire_glace', 70, ['spectre_givre','golem_cristal'], 'titan_glace', 80),
 ('citadelle_neant', 90, ['chevalier_neant','golem_cristal'], 'avatar_neant', 100),
]
BOSSCFG = {  # (hp, hpL, atk, atkL, defL, lvl)
 'roi_gobelin': (1500,250,15,3.5,2.5,20), 'liche_ancienne': (3000,400,20,5.0,3.0,45),
 'seigneur_ignis': (4000,600,30,6.0,3.5,65), 'titan_glace': (6000,800,40,7.0,4.0,80),
 'avatar_neant': (10000,1000,50,8.0,4.5,100),
}

# ------------------------------------------------------------------ combat
TUN = dict(            # leviers d'équilibrage (modifiés pour "après")
    wave_base=6, wave_per=3, wave_diff=3, wave_solo_cut=0,
    mob_dmg=1.0,                         # mult dégâts des monstres de donjon sur le joueur
    boss_dmg=1.5,                        # baseDamage du boss (dmg mult)
    boss_tough=1.8, boss_hp=1.0,
    cls_taken={'guerrier':1.0,'mage':1.0,'archer':1.0,'tank':1.0},   # facteur de dégâts subis par classe en donjon
    cls_dealt={'guerrier':1.0,'mage':1.0,'archer':1.0,'tank':1.0},   # facteur de dégâts infligés par classe en donjon
    mana_regen_mult={'guerrier':1.0,'mage':1.0,'archer':1.0,'tank':1.0},
    mob_hit_every=1.3,
)
CAP = {'guerrier':6,'tank':8,'mage':4,'archer':4}      # monstres au contact en même temps
AVOID = {'guerrier':0.10,'tank':0.05,'mage':0.25,'archer':0.30}   # fraction des coups évitée par le mouvement
BOSS_HIT = {'guerrier':0.55,'tank':0.65,'mage':0.45,'archer':0.40} # part des attaques du boss qui touchent
UPTIME = {'guerrier':0.75,'tank':0.7,'mage':0.8,'archer':0.8}

def red(de, plvl): return min(0.8, de/(de+200+10*plvl))
def critf(s): return 1 + s['crit']/100*(s['cd']/100-1)

def dps_profile(cls, lvl, target_def, group=1):
    """(dps sur une cible, dps total sur groupe (AOE), mana/s consommé, mana/s régénéré) -- avant armure monstre"""
    s = stats(cls, lvl); r = rank(lvl); k = rm(r); cf = critf(s)
    atk, mag = s['atk'], s['mag']
    cdr = 1 - s['cdr']/100
    single = 0.0; aoe = 0.0; mana_use = 0.0
    sk = []   # (dmg_single, dmg_total_aoe, cooldown, mana)
    if cls == 'guerrier':
        basic = atk*1.6*UPTIME[cls]
        sk.append((1.8*k*atk, 1.8*k*atk*min(3,group), 4*cdr, 10))
        if lvl >= 25: sk.append((1.5*k*atk, 1.5*k*atk*min(4,group), 8*cdr, 25))
        if lvl >= 50: sk.append((1.2*k*atk, 1.2*k*atk*min(3,group), 10*cdr, 20))
        if lvl >= 100: sk.append((4.0*k*atk, 4.0*k*atk*min(5,group), 60*cdr, 60))
    elif cls == 'tank':
        basic = atk*1.0*UPTIME[cls]
        sk.append(((1.2*k*atk+0.5*k*s['de']), (1.2*k*atk+0.5*k*s['de'])*min(2,group), 5*cdr, 10))
    elif cls == 'mage':
        basic = mag*0.8/0.6*UPTIME[cls]; mana_use = 3/0.6*UPTIME[cls]
        sk.append((1.6*k*mag, 1.6*k*mag, 6*cdr, 15)); sk.append((1.2*k*mag, 1.2*k*mag, 6*cdr, 25))
        if lvl >= 25: sk.append((1.4*k*mag, 1.4*k*mag*min(3,group)*0.75, 7*cdr, 30))
        if lvl >= 100: sk.append((5*k*mag, 5*k*mag*min(6,group), 60*cdr, 80))
    elif cls == 'archer':
        basic = atk*1.15/0.7*0.8*UPTIME[cls]/0.8
        n = 10+2*r
        sk.append((n*0.5*k*atk*0.5, n*0.5*k*atk*0.5*min(3,group), 12*cdr, 25))
        if lvl >= 50: sk.append((2.2*k*atk, 2.2*k*atk*min(3,group), 8*cdr, 25))
        if lvl >= 75: sk.append(((6+r)*0.9*k*atk*0.6, (6+r)*0.9*k*atk*0.6*min(3,group)*0.8, 10*cdr, 30))
        if lvl >= 100: sk.append((1.2*k*atk*6, 1.2*k*atk*6*min(6,group), 60*cdr, 70))
        sk.append((0.9*k*atk*20/1.2, 0.9*k*atk*20/1.2, 45*cdr, 30))   # chimère
    # mana limite l'usage des compétences
    mana_regen = s['mr']*TUN['mana_regen_mult'][cls]
    total_skill_mana = sum(m/c for _,_,c,m in sk)
    # mage : bolts consomment aussi ; usage compétences en priorité
    avail = mana_regen + (s['mana']/60.0)        # réserve amortie sur 60 s
    ratio = min(1.0, (avail)/(total_skill_mana + mana_use + 1e-9))
    single = basic*ratio_basic(cls, avail, total_skill_mana, mana_use) + sum(d/c for d,_,c,_ in sk)*min(1.0, avail/(total_skill_mana+1e-9))
    aoe = sum((t-d)/c for d,t,c,_ in sk)*min(1.0, avail/(total_skill_mana+1e-9))
    return single*cf, aoe*cf, s

def ratio_basic(cls, avail, skill_mana, bolt_mana):
    if cls != 'mage': return 1.0
    left = max(0.0, avail - skill_mana)
    return min(1.0, left/bolt_mana) if bolt_mana else 1.0

def wave_counts(diff, solo=True):
    w = 5 + (1 if diff >= 2 else 0)
    return [min(30, TUN['wave_base'] + n*TUN['wave_per'] + diff*TUN['wave_diff']) for n in range(1, w+1)]

def fight_wave(cls, lvl, mob, mlvl, n, finale):
    hp0, hpl, a0, al, dl = MOBCFG[mob]
    mhp = hp0+hpl*mlvl; matk = a0+al*mlvl; mde = dl*mlvl
    single, aoe, s = dps_profile(cls, lvl, mde, group=n)
    dealt = (single + aoe)*(1-red(mde,lvl))*TUN['cls_dealt'][cls]
    rk = dealt/mhp/1.2      # monstres tués / s (20 % de gaspillage)
    cap = CAP[cls]
    e_int = (cap*(n-cap) + cap*cap/2.0) if n > cap else n*n/2.0
    a_eff = matk*TUN['mob_dmg']*(1-red(s['de'], mlvl))/TUN['mob_hit_every']*(1-AVOID[cls])*(1-s['esq']/100)*TUN['cls_taken'][cls]
    lost = a_eff*e_int/max(rk,1e-9)
    heal = s['ls']/100*dealt*(e_int/max(rk,1e-9)/max(1,n))*0   # vol de vie négligé
    return lost, lost/s['hp']

def fight_boss(cls, lvl, boss):
    hp0,hpl,a0,al,dl,blvl = BOSSCFG[boss]
    bhp = (hp0+hpl*blvl)*TUN['boss_hp']; batk = a0+al*blvl; bde = dl*blvl
    single, aoe, s = dps_profile(cls, lvl, bde, group=1)
    dealt = single*(1-red(bde,lvl))/TUN['boss_tough']*TUN['cls_dealt'][cls]*0.85   # 15 % de temps hors portée
    T = bhp/dealt
    phase_avg = 1.15
    # une attaque toutes les 3.3 s en moyenne ; multiplicateur moyen d'une attaque 1.3 x ATK ; touche selon BOSS_HIT
    per_move = batk*1.3*TUN['boss_dmg']*phase_avg*(1-red(s['de'], blvl))*TUN['cls_taken'][cls]
    incoming = per_move*BOSS_HIT[cls]*(1-s['esq']/100)/3.3
    enrage = 1.0
    if T > 180: enrage = 1 + (T-180)/180*0.5
    lost = incoming*T*enrage
    return T, lost, lost/s['hp'], s

def report():
    print(f"{'donjon':22} {'diff':4} {'cls':9} {'nivJ':4} {'PV':>6} {'DEF':>5} {'dpsBoss':>8} {'TTK s':>6} {'boss PV%':>9} {'vagues PV% (1..n)':>40}")
    out = {}
    for name, tl, mobs, boss, bl in DUNGEONS:
        for diff in (0,1,2):
            lvl = tl + 5*diff
            for cls in CLS:
                T, lost, frac, s = fight_boss(cls, lvl, boss)
                fr = []
                counts = wave_counts(diff)
                for i, n in enumerate(counts):
                    mk = [mobs[j % len(mobs)] for j in range(n)]
                    # approx : mob dominant = moyenne des deux types
                    tot = 0
                    for mob in set(mk):
                        cnt = mk.count(mob)
                        l, f = fight_wave(cls, lvl, mob, tl+diff+(3 if i==len(counts)-1 else 0), cnt, i==len(counts)-1)
                        tot += f
                    fr.append(tot)
                out[(name,diff,cls)] = dict(ttk=T, boss=frac, waves=fr)
                print(f"{name:22} {diff:4} {cls:9} {lvl:4} {s['hp']:6.0f} {s['de']:5.0f} {boss_dps(cls,lvl,boss):8.1f} {T:6.0f} {frac*100:8.0f}% {' '.join(f'{x*100:4.0f}' for x in fr)}")
    return out

def boss_dps(cls, lvl, boss):
    hp0,hpl,a0,al,dl,blvl = BOSSCFG[boss]
    single,_,s = dps_profile(cls, lvl, dl*blvl)
    return single*(1-red(dl*blvl,lvl))/TUN['boss_tough']

if __name__ == '__main__':
    report()
