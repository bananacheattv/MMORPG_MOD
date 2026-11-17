"""Usage: python generate.py --out <separate-output-directory>."""
import argparse,time
from pathlib import Path
from world import World
from architecture import build_all
from nature import polish
from export_map import export, validate

def main():
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--no-export',action='store_true');args=p.parse_args()
    start=time.time();w=World(args.out)
    w.plan_roads();w.fill_terrain();w.finish_roads();build_all(w);polish(w);w.save_arrays()
    if not args.no_export:export(w);validate(args.out)
    print(f'Terminé en {time.time()-start:.1f} secondes : {args.out}',flush=True)

if __name__=='__main__':main()
