"""Regenerate wiki catalogs from registered skills and weapons, without invented statistics."""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
out=root/'docs/wiki'
classes={'GUERRIER':'Guerrier','MAGE':'Mage','ARCHER':'Archer','TANK':'Paladin','G':'Guerrier','M':'Mage','A':'Archer','T':'Paladin'}
source=(root/'src/main/java/com/mmorpg/skill/Skills.java').read_text(encoding='utf-8')
skills=re.findall(r'Skill.builder\("([^"]+)",\s*"([^"]+)",\s*PlayerClass\.(\w+),\s*(\d+)\)(.*?)\.build\(\)',source,re.S)
assert len(skills)==44,len(skills)
page='# Catalogue des compétences\n\n[Accueil](README.md) · [Fonctionnement](competences.md)\n\nLes descriptions et valeurs au rang choisi sont affichées dans le menu en jeu. Chaque compétence se monte jusqu’au rang 5.\n\n| Classe | Compétence | Niveau | Type | Mana initial | Recharge initiale |\n|---|---|---:|---|---:|---|\n'
for identity,name,cls,level,body in skills:
    kind='Passive' if '.passive()' in body else 'Ultime' if '.ultimate()' in body else 'Active'
    cost=re.search(r'\.cost\((\d+),\s*(\d+)\)',body)
    mana,recharge=(cost.group(1),cost.group(2)+' s')if cost else ('—','—')
    page+=f'| {classes[cls]} | {name} | {level} | {kind} | {mana} | {recharge} |\n'
(out/'catalogue-competences.md').write_text(page,encoding='utf-8')
source=(root/'src/main/java/com/mmorpg/item/ItemDefs.java').read_text(encoding='utf-8')
weapons=re.findall(r'w\("([^"]+)",\s*(\w+),\s*WeaponKind\.(\w+),\s*(\d+),\s*(\w+),\s*[^,]+,\s*"([^"]*)"',source,re.S)
assert len(weapons)==44,len(weapons)
lang=__import__('json').loads((root/'src/main/resources/assets/mmorpg/lang/fr_fr.json').read_text(encoding='utf-8'))
page='# Catalogue des armes PvE\n\n[Accueil](README.md) · [Classes](classes.md) · [Équipement PvP](recompenses-pvp.md)\n\nLes statistiques exactes dépendent du modèle et des améliorations ; elles se consultent sur l’objet. Les vingt armes PvP, séparées, sont vendues au marchand des arènes.\n\n| Classe | Arme | Niveau | Famille | Description |\n|---|---|---:|---|---|\n'
types={'SWORD':'Épée','AXE':'Hache','STAFF':'Bâton','BOW':'Arc','HAMMER':'Marteau'}
for identity,cls,kind,level,rarity,lore in weapons:
    name=lang.get('item.mmorpg.'+identity,identity)
    page+=f'| {classes[cls]} | {name} | {level} | {types[kind]} | {lore} |\n'
(out/'catalogue-armes.md').write_text(page,encoding='utf-8')
for file in out.glob('*.md'):
    for link in re.findall(r'\]\(([^)]+)\)',file.read_text(encoding='utf-8')):
        if '://' not in link and not (file.parent/link.split('#')[0]).exists():raise ValueError(f'Broken link: {file}: {link}')
print(f'Wiki: {len(list(out.glob("*.md")))} pages, {len(skills)} skills, {len(weapons)} PvE weapons, all relative links valid')
