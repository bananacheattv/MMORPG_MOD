import donjons_modele as M, tune, sys
AFTER = dict(wave_base=2, wave_per=2, wave_diff=2, mob_dmg=1.0, boss_dmg=1.25, boss_tough=1.5)
TAKEN = {'guerrier':0.55,'mage':0.22,'archer':0.45,'tank':0.55}
DEALT = {'guerrier':1.0,'mage':1.0,'archer':1.0,'tank':2.5}
def before():
    M.TUN.update(dict(wave_base=6, wave_per=3, wave_diff=3, mob_dmg=1.0, boss_dmg=1.5, boss_tough=1.8,
        cls_taken={c:1.0 for c in M.CLS}, cls_dealt={c:1.0 for c in M.CLS}))
    return tune.run({})
def after(taken=TAKEN, dealt=DEALT, **kw):
    cfg = dict(AFTER, **kw); cfg['cls_taken']=taken; cfg['cls_dealt']=dealt
    return tune.run(cfg)
if __name__=='__main__':
    b=before(); print('AVANT'); tune.summary(b)
    a=after(); print('APRES'); tune.summary(a)

    print('\nTABLE (donjon, classe) : avant -> apres  [TTK boss s | PV perdus boss % | pire vague %] (normal)')
    for (name,diff,cls),v in sorted(b.items()):
        if diff!=0: continue
        w=a[(name,diff,cls)]
        print(f"{name:22} {cls:9} {v[0]:4.0f}s {v[1]*100:4.0f}% {v[2]*100:5.0f}%  ->  {w[0]:4.0f}s {w[1]*100:4.0f}% {w[2]*100:5.0f}%")
