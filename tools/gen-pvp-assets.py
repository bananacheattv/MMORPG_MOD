"""Original volumetric arena equipment icons and runtime equipment layers. Reproducible."""
from pathlib import Path
import json,runpy
ROOT=Path(__file__).resolve().parents[1]
art=runpy.run_path(str(ROOT/'tools/sculpt-rpg-assets.py'))
B,model,write,RES,MODELS=(art[k]for k in ('B','model','write','RES','MODELS'))
labels={}
for cls,label,body in [('guerrier','Guerrier',10),('mage','Mage',13),('archer','Archer',6),('tank','Paladin',3)]:
 for tier in range(5):
  key=f'pvp_{cls}_{tier}';metal=3 if tier<2 else 1;gem=8
  write(RES/f'equipment/{key}.json',{'layers':{k:[{'texture':'mmorpg:avatar_arcanique'}]for k in ('humanoid','humanoid_leggings')}})
  for piece in ('casque','plastron','jambieres','bottes'):
   E=[]
   if piece=='casque':
    E=[B([8,12,8],[9,2,9],body),B([8,8,12],[9,8,1.5],body),B([4,8,8],[1.5,8,8],body),B([12,8,8],[1.5,8,8],body),B([8,11,3.8],[9,1,1],metal),B([8,9,3.4],[1.3,1.8,.8],gem,'Arena crest','z',45)]
    if cls in ('guerrier','tank'):
     E.extend([B([8,5,3.8],[8,2,1],body),B([8,7,3.5],[1,5,1],metal)])
    for side in [-1,1]:
     for j in range(tier//2+1):E.append(B([8+side*(4+j*.8),13+j,8],[1.3,3,2],metal,'Crown crest','z',side*22.5))
   elif piece=='plastron':
    E=[B([8,8,8],[8,10,5],body),B([8,8,5.2],[4.5,6,.7],8,'Arena tabard'),B([8,6,4.6],[1,3,.6],metal,'Sword crest','z',45),B([8,6,4.6],[1,3,.6],metal,'Sword crest','z',-45),B([8,3,8],[9,1,6],metal)]
    for side in [-1,1]:E.extend([B([8+side*5.7,10,8],[3,4,5.6],metal),B([8+side*5.7,6,8],[3,4,4.6],body)])
   elif piece=='jambieres':
    E=[B([8,12,8],[9,3,5],metal)]
    for side in [-1,1]:E.extend([B([8+side*2.4,7,8],[4.2,9,4.5],body),B([8+side*2.4,6,5.5],[3.3,3,.8],8)])
   else:
    for side in [-1,1]:E.extend([B([8+side*2.9,7,8],[4.8,7,5],body),B([8+side*2.9,4.8,6.5],[5,3,7],metal),B([8+side*2.9,10,8],[5.4,1,5.4],metal)])
   name=f'{key}_{piece}';write(MODELS/f'item/{name}.json',model(E));write(RES/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'mmorpg:item/{name}'}})
   labels[f'item.mmorpg.{name}']=f'{dict(casque="Heaume",plastron="Armure",jambieres="Jambières",bottes="Bottes")[piece]} de Gladiateur {label} {tier+1}'
  labels[f'item.mmorpg.{key}_arme']=f'Arme de Gladiateur {label} {tier+1}'
# PvP seal is only obtainable from an operator; the merchant unlocks the mount permanently.
E=[B([8,7,8],[7,9,2],3,'Arena seal'),B([8,7,6.7],[4,5,.8],8),B([8,7,6],[.8,4,.6],1,'Crossed blades','z',45),B([8,7,6],[.8,4,.6],1,'Crossed blades','z',-45)]
write(MODELS/'item/sceau_chimere_arene.json',model(E));write(RES/'items/sceau_chimere_arene.json',{'model':{'type':'minecraft:model','model':'mmorpg:item/sceau_chimere_arene'}})
labels['item.mmorpg.sceau_chimere_arene']='Sceau de la Chimère des arènes';labels['entity.mmorpg.monture_chimere_arene']='Chimère des arènes'
for language in ('fr_fr','en_us'):
 p=RES/f'lang/{language}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update(labels);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('80 arena armour icons, 20 equipment layers and translations generated')

# Private presentation models reuse exactly the closed/open meshes rendered by actual crate blocks.
for key in ('rare','epique','legendaire'):
 for opened in (False,True):
  state='ouverte' if opened else 'fermee';suffix='_ouverte' if opened else ''
  write(RES/f'items/ui_caisse_{key}_{state}.json',{'model':{'type':'minecraft:model','model':f'mmorpg:block/caisse_{key}_bloc{suffix}'}})
