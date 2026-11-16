import donjons_modele as M, sys
def run(cfg, verbose=False):
    M.TUN.update(cfg)
    res = {}
    worst = {c:[] for c in M.CLS}
    for name, tl, mobs, boss, bl in M.DUNGEONS:
        for diff in (0,1,2):
            lvl = tl+5*diff
            for cls in M.CLS:
                T, lost, frac, s = M.fight_boss(cls, lvl, boss)
                counts = M.wave_counts(diff)
                wf=[]
                for i,n in enumerate(counts):
                    mk=[mobs[j%len(mobs)] for j in range(n)]
                    tot=sum(M.fight_wave(cls,lvl,mob,tl+diff+(3 if i==len(counts)-1 else 0),mk.count(mob),False)[1] for mob in set(mk))
                    wf.append(tot)
                res[(name,diff,cls)] = (T, frac, max(wf), wf)
    return res
def summary(res):
    for cls in M.CLS:
        T=[v[0] for k,v in res.items() if k[2]==cls]; B=[v[1] for k,v in res.items() if k[2]==cls]; W=[v[2] for k,v in res.items() if k[2]==cls]
        print(f"{cls:9} TTK moy {sum(T)/len(T):5.0f} max {max(T):5.0f} | boss PV% moy {100*sum(B)/len(B):4.0f} max {100*max(B):4.0f} | vague max PV% moy {100*sum(W)/len(W):4.0f} max {100*max(W):4.0f}")
if __name__=='__main__':
    print('AVANT'); summary(run({}))
