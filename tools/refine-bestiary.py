"""Refine existing cubic rigs without repainting or resampling their authored textures.

The baseline is the backed-up c92f062 revision. Added geometry reuses the original
material UVs, so the existing palettes, emissive masks and animation timings survive.
No raster image is generated or edited by this script.
"""
from pathlib import Path
import copy
import hashlib
import importlib
import json
import math
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
ART = ROOT / 'art/bestiary'
sys.path.insert(0, str(ROOT / 'tools'))
sys.path.insert(0, str(ROOT / 'art'))
sys.path.insert(0, str(ROOT / 'art/boss_blockbench'))


def baseline(path):
    return subprocess.check_output(['git', 'show', 'c92f062:' + path], cwd=ROOT)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def flat(node):
    yield node
    for child in node.get('children', []):
        yield from flat(child)


def uid(name):
    h = hashlib.sha256(name.encode()).hexdigest()[:32]
    return f'{h[:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:]}'


def normalize_tracks(animations):
    for animation in animations:
        corrected=[]
        for line in animation['tracks'].splitlines():
            bone,channel,frames=line.split('|');keys=frames.split(';');last=max(float(frame.split(',')[0]) for frame in keys)
            if last>animation['length']+.0001:
                adjusted=[]
                for frame in keys:
                    fields=frame.split(',');fields[0]=str(round(float(fields[0])*animation['length']/last,6));adjusted.append(','.join(fields))
                keys=adjusted
            corrected.append(bone+'|'+channel+'|'+';'.join(keys))
        animation['tracks']='\n'.join(corrected)


def blockbench(data, key, kind):
    elements = []
    names = {}

    def group(node):
        identifier = uid(key + '__' + node['uuid'])
        names[node['uuid']] = identifier
        if 'children' not in node:
            element = {**node, 'uuid': identifier, 'type': 'cube', 'name': node.get('name', node['uuid']), 'box_uv': False}
            elements.append(element)
            return identifier
        return dict(name=node.get('name', node['uuid']), uuid=identifier, origin=node.get('origin', [0, 0, 0]),
                    rotation=node.get('rotation', [0, 0, 0]), children=[group(c) for c in node['children']])

    outliner = [group(p) for p in data['parts']]
    animations = []
    for animation in data['animations']:
        animators = {}
        for line in animation['tracks'].splitlines():
            if not line:
                continue
            bone, channel, frames = line.split('|')
            target = animators.setdefault(names[bone], dict(name=bone, type='bone', keyframes=[]))
            for i, raw in enumerate(frames.split(';')):
                values = raw.split(',')
                target['keyframes'].append(dict(channel={'r': 'rotation', 'p': 'position', 's': 'scale'}[channel],
                    time=float(values[0]), data_points=[dict(zip('xyz', values[1:4]))],
                    interpolation='catmullrom' if values[-1] == 'c' else 'linear', uuid=uid(key + animation['name'] + bone + channel + str(i))))
        animations.append(dict(name=animation['name'], length=animation['length'], loop='loop' if animation['loop'] else 'once',
                               uuid=uid(key + animation['name']), animators=animators))
    return dict(meta=dict(format_version='4.10', model_format='free', box_uv=False), name=key,
                resolution=data['resolution'], outliner=outliner, elements=elements, animations=animations,
                textures=[dict(id='0', uuid=uid(key + 'texture'), name=key + '.png', relative_path=f'../textures/{kind}/{key}.png',
                               width=data['resolution']['width'], height=data['resolution']['height'])])


def mounts():
    import gen_mounts as original
    reports = []
    for factory in original.MOUNTS:
        model, animations = factory()  # Geometry only: intentionally never call Model.build(), which paints an atlas.
        key = model.key
        normalize_tracks(animations)
        old = json.loads(baseline(f'src/main/resources/assets/mmorpg/bbmodels/mounts/{key}.json'))
        cubes = {n['uuid']: n for p in old['parts'] for n in flat(p) if 'from' in n}
        original_count = len(model.boxes)
        by_material = {}
        for cube in model.boxes:
            cube['faces'] = copy.deepcopy(cubes[cube['uuid']]['faces'])
            by_material.setdefault(tuple(cube['mat']), cube)

        def material(*kinds):
            for kind in kinds:
                for mat in by_material:
                    if mat[0] == kind:
                        return mat
            return tuple(model.boxes[0]['mat'])

        hide = material('fur', 'rune', 'scale', 'hide', 'shell', 'wool', 'feather')
        trim, dark = material('gold', 'metal'), material('dark', 'leather')

        def cube(bone, name, center, size, mat=hide, rotation=None):
            a = [center[i] - size[i] / 2 for i in range(3)]
            b = [center[i] + size[i] / 2 for i in range(3)]
            model.box(bone, a, b, mat, rot=rotation, pivot=center if rotation else None)
            new = model.boxes[-1]
            new['name'] = name
            new['faces'] = copy.deepcopy(by_material[tuple(mat)]['faces'])

        body = [c for c in model.boxes if c in model.groups['body']['children'] and c['mat'][0] in ('fur', 'rune', 'scale', 'hide', 'shell', 'wool', 'feather')]
        torso = max(body or [model.boxes[0]], key=lambda c: math.prod(c['to'][i] - c['from'][i] for i in range(3)))
        a, b = torso['from'], torso['to']
        top, front, back, width = b[1], a[2], b[2], b[0] - a[0]
        # Raised saddle buckles, stitching and straps; keep the central seat and original passenger attachment clear.
        for side in (-1, 1):
            x = (a[0] - .25) if side < 0 else (b[0] + .25)
            for z in (-2.5, 3.5):
                cube('body', f'saddle_buckle_{side}_{z}', [x, top - 2, z], [.65, 2.5, 2.2], trim)
                cube('body', f'saddle_buckle_hole_{side}_{z}', [x + side * .37, top - 2, z], [.18, 1.25, 1.0], dark)
            for i in range(5):
                cube('body', f'hand_stitch_{side}_{i}', [x + side * .12, top - 3.2, -3.5 + i * 1.8], [.25, .45, .7], trim)

        shelled = key in ('tortue_cristal', 'scorpion_dunes', 'araignee_cavernes')
        scaled = key in ('lezard_lave', 'raptor_sables')
        if shelled:
            for side in (-1, 1):
                for i in range(5):
                    z = front + 2 + (back - front - 4) * i / 4
                    cube('body', f'segmented_shell_{side}_{i}', [side * width * .33, top + .35, z], [width * .3, .9, 2.8], hide)
                    cube('body', f'shell_rim_{side}_{i}', [side * width * .46, top + .2, z], [.5, 1.1, 2.4], trim)
        elif scaled:
            for i in range(8):
                z = front + 2 + (back - front - 3) * i / 7
                cube('body', f'dorsal_crest_{i}', [0, top + 1.1 + i % 2 * .5, z], [1.6, 2.2 + i % 2, 1.7], trim)
                for side in (-1, 1):
                    cube('body', f'overlapping_scale_{side}_{i}', [side * width * .43, top - 1.4, z], [.6, 2, 1.6], hide)
        else:
            for side in (-1, 1):
                for i in range(7):
                    cube('body', f'layered_mane_{side}_{i}', [side * (width * .35 + (i % 2) * .5), top - .8 - (i % 3) * .7, front + 1 + i * .9], [1.8, 2.5, 1.5], hide, [0, 0, side * 12])

        # Three-dimensional brows and nostrils retain each mount's original skull, eyes and muzzle.
        if 'head' in model.groups:
            skulls = [c for c in model.groups['head']['children'] if 'from' in c and c['mat'][0] not in ('eye', 'dark', 'glow')]
            if skulls:
                skull = max(skulls, key=lambda c: math.prod(c['to'][i] - c['from'][i] for i in range(3)))
                lo, hi = skull['from'], skull['to']
                for side in (-1, 1):
                    x = lo[0] - .15 if side < 0 else hi[0] + .15
                    cube('head', f'sculpted_brow_{side}', [x, hi[1] - 1.3, lo[2] + 2], [.6, 1.1, 2.4], hide)
                    cube('head', f'nostril_{side}', [side * (hi[0] - lo[0]) * .25, lo[1] + 1.3, lo[2] - .15], [.7, .6, .3], dark)

        for name, group in list(model.groups.items()):
            if name.startswith('leg_'):
                x, y, z = group['origin']
                cube(name, 'joint_cuff_' + name, [x, y - 2, z], [3.3, .8, 3.2], hide)
            if name in ('wing_l', 'wing_r'):
                side = 1 if name.endswith('l') else -1
                for i in range(12):
                    cube(name, f'flight_feather_{i}', [side * (12 + i * 1.5), 22.5, 8 + i * .5], [1.45, .8, 8 - i * .28], material('feather'), [0, -side * i * 1.6, side * 8])
        for c in model.boxes:
            c.pop('mat', None)
        data = dict(resolution=old['resolution'], parts=model.top, animations=animations, revision=3,
                    author='Aeloria', description='Original cubic mount rig with layered anatomy, material-matched sculpted details and articulated tack.')
        write(RES / 'bbmodels/mounts' / (key + '.json'), data)
        write(ART / 'mounts' / (key + '.bbmodel'), blockbench(data, key, 'mounts'))
        texture = ART / 'textures/mounts' / (key + '.png')
        texture.parent.mkdir(parents=True, exist_ok=True)
        texture.write_bytes(baseline(f'src/main/resources/assets/mmorpg/textures/imported/mounts/{key}.png'))
        reports.append(dict(id=key, before=original_count, after=len(model.boxes), bones=len(model.groups), animations=len(animations)))
    return reports


def bosses():
    import entity_kit as ek
    reports = []
    classes = dict(roi_gobelin='RoiGobelin', liche_ancienne='LicheAncienne', ignis='Ignis', titan_glace='TitanGlace', avatar_neant='AvatarNeant')
    for key, java_class in classes.items():
        module = importlib.import_module(key)
        model = module.build()
        module.animations(model)
        old = json.loads(baseline(f'art/boss_blockbench/{key}/{key}.bbmodel'))
        elements = {c['uuid']: c for c in old['elements']}
        by_bone_name = {}
        def old_group(group):
            for child in group.get('children', []):
                if isinstance(child, str):
                    element = elements[child]
                    by_bone_name[(group['name'], element['name'])] = element
                else:
                    old_group(child)
        for root in old['outliner']:
            old_group(root)
        original_count = len(model.cubes())
        original_cubes = list(model.cubes())
        for c in original_cubes:
            c.uv_offset = by_bone_name[(c.bone.name,c.name)]['uv_offset']

        def choose(*names):
            for name in names:
                if isinstance(getattr(module, name, None), ek.Mat):
                    return getattr(module, name)
            return original_cubes[0].mat

        def add(bone, name, frm, size, material):
            references = [c for c in original_cubes if c.mat is material]
            exact = next((c for c in references if list(c.size) == list(size)), None)
            offset = None
            if exact:
                offset = list(exact.uv_offset)
            else:
                required = (2 * (size[0] + size[2]), size[1] + size[2])
                for ref in sorted(references, key=lambda c: -math.prod(c.size)):
                    for uv in ek.box_uv(ref).values():
                        x0, y0, x1, y1 = uv
                        if abs(x1-x0) >= required[0] and abs(y1-y0) >= required[1]:
                            offset = [min(x0,x1), min(y0,y1)]
                            break
                    if offset is not None:
                        break
            if offset is None and max(size) > 2:
                axis = max(range(3), key=lambda a: size[a])
                first = list(size); second = list(size); first[axis] = size[axis]//2; second[axis] -= first[axis]
                moved = list(frm); moved[axis] += first[axis]
                add(bone, name+'_a', frm, first, material)
                add(bone, name+'_b', moved, second, material)
                return
            ref = max(references or original_cubes, key=lambda c: math.prod(c.size))
            c = model.cube(bone, 'refined_' + name, frm, size, material)
            c.uv_offset = offset if offset is not None else list(ref.uv_offset)
            return c

        trim = choose('GOLD', 'ICE_LIGHT', 'ICE', 'BONE', 'IVORY', 'IRON')
        glow = choose('RUNE', 'SOUL', 'CRYSTAL', 'LAVA', 'RUBY', 'GOLD')
        # Layered armor follows each original animated bone. Reuse its original material and UV region.
        for bone in model.order:
            if not any(word in bone.name for word in ('torse', 'epaule', 'avant_bras', 'poing', 'tibia')):
                continue
            candidates = [c for c in bone.cubes if min(c.size) >= 3]
            if not candidates:
                continue
            core = max(candidates, key=lambda c: math.prod(c.size))
            x, y, z = core.frm
            w, h, d = core.size
            for side in (0, 1):
                bx = x + (w - 2 if side else 0)
                add(bone, f'{bone.name}_edge_{side}', [bx, y + 1, z - 1], [2, max(1, h - 2), 1], core.mat)
                for step in (1, max(2, h - 3)):
                    add(bone, f'{bone.name}_rivet_{side}_{step}', [bx + .5, y + step, z - 1.5], [1, 1, 1], trim)
            for row in range(2, h - 2, max(3, h // 4)):
                add(bone, f'{bone.name}_raised_band_{row}', [x + 2, y + row, z - 1.1], [max(1, w - 4), 1, 1], trim)

        head = next((b for b in model.order if b.name in ('tete', 'masque')), model.order[1])
        skull=max(head.cubes,key=lambda c:math.prod(c.size)) if head.cubes else original_cubes[0]
        x=(skull.frm[0]+skull.to[0])/2;y=skull.frm[1]+skull.size[1]*.3;z=skull.frm[2]+3.5
        for side in (-1, 1):
            for i in range(3):
                add(head, f'cheek_guard_{side}_{i}', [x + side * (3 + i) - .5, y + 1 + i, z - 4.5], [1, 2, 1], trim)
        if key == 'roi_gobelin':
            for i in range(5):
                add('couronne', f'royal_gem_setting_{i}', [-5 + i * 2, 45, -9], [2, 2, 1], trim)
                add('couronne', f'royal_gem_{i}', [-4.5 + i * 2, 45.5, -9.6], [1, 1, 1], choose('RUBY'))
        elif key == 'liche_ancienne':
            for side in (-1, 1):
                for i in range(5):
                    add('torse', f'soul_chain_{side}_{i}', [side * (5 - i * .6), 29 - i, -6.8], [1, 1, 1], trim)
            for i in range(4):
                add('couronne', f'funerary_crown_{i}', [-5 + i * 3, 49, -2], [1, 4 + i % 2, 1], trim)
        elif key == 'ignis':
            for side, bone in ((1, 'epauliere_droite'), (-1, 'epauliere_gauche')):
                for i in range(4):
                    add(bone, f'volcanic_crest_{side}_{i}', [side * (16 + i) - 1, 51 + i, -4 + i * 2], [2, 3, 2], choose('OBSIDIAN', 'ROCK', 'IRON'))
        elif key == 'titan_glace':
            for side, bone in ((1, 'cristaux_epaule_droite'), (-1, 'cristaux_epaule_gauche')):
                for i in range(4):
                    add(bone, f'glacial_plate_{side}_{i}', [side * (28 + i * 2) - 1, 83 + i, -7 + i * 3], [3, 6 + i, 3], trim)
        else:
            for i in range(12):
                angle = i * math.pi / 6
                add('fragments', f'void_rune_{i}', [math.cos(angle) * 17 - 1, 34 + math.sin(angle) * 17, -7], [2, 2, 2], glow)

        # Byte-preserving wrapper: exporter embeds the existing PNG without generating a raster.
        class ExistingTexture:
            def __init__(self, raw):
                self.raw = raw
                self.width = int.from_bytes(raw[16:20], 'big')
                self.height = int.from_bytes(raw[20:24], 'big')
                self.size = self.width, self.height
            def save(self, stream, format=None):
                stream.write(self.raw)

        texture = ExistingTexture(baseline(f'art/boss_blockbench/{key}/{key}.png'))
        project = ek.bbmodel(model, texture, module.ENTITY_CLASS, module.COLORS, module.VISIBLE_BOX)
        write(ROOT / f'art/boss_blockbench/{key}/{key}.bbmodel', project)
        client, common = ek.java_export(model, texture, java_class, key, getattr(module, 'STRIKES', {}), 'tools/refine-bestiary.py')
        for relative, content in ((f'client/model/boss/{java_class}Model.java', client), (f'entity/boss/anim/{java_class}Anims.java', common)):
            (ROOT / 'src/main/java/com/mmorpg' / relative).write_text(content, encoding='utf-8')
        reports.append(dict(id=key, before=original_count, after=len(model.cubes()), bones=len(model.order), animations=len(model.animations)))
    return reports


def imported_mobs():
    reports=[]
    for project in sorted((ROOT/'art/imported_models/mobs').glob('*.bbmodel')):
        source=json.loads(baseline(project.relative_to(ROOT).as_posix()))
        key=project.stem[3:]
        data=json.loads(baseline(f'src/main/resources/assets/mmorpg/bbmodels/mobs/{key}.json'))
        labels={e['uuid']:e.get('name','') for e in source['elements']}
        leaves=[(parent,c) for root in data['parts'] for parent in flat(root) for c in parent.get('children',[]) if 'from' in c]
        candidates=[];glow=0
        for parent,c in leaves:
            name=labels.get(c['uuid'],'');c['name']=name
            if any(word in name for word in ('oeil','reflet','lumineux','lumiere','coeur_blanc','coeur_ambre','glande_venimeuse','rune')) and 'pupille' not in name:
                c['emissive']=True;glow+=1
            size=[c['to'][i]-c['from'][i] for i in range(3)]
            if min(size)>=1 and max(size)>=3:
                priority=2 if any(word in name for word in ('armure','carapace','plaque','ecorce','ecaille')) else 1
                candidates.append((priority,math.prod(size),parent,c))
        added=0
        for _,__,parent,c in sorted(candidates,key=lambda value:(-value[0],-value[1]))[:8]:
            x,y,z=c['from'];w,h,d=[c['to'][i]-c['from'][i] for i in range(3)]
            # Raised bevel strips follow the existing cube rotation and its original face materials.
            for side in (0,1):
                strip=copy.deepcopy(c);strip['uuid']=uid(key+c['uuid']+'relief'+str(side));strip['name']='sculpted_'+c.get('name','surface')
                sx=x+w*(.1 if side==0 else .82)
                strip['from']=[sx,y+h*.15,z-.2];strip['to']=[sx+max(.15,w*.08),y+h*.85,z-.025]
                strip['emissive']=False;parent['children'].append(strip);added+=1
        normalize_tracks(data['animations'])
        data['revision']=3;data['author']='Aeloria';data['description']='Preserved authored anatomy and animations, sculpted material relief and selective emissive eyes/cores.'
        write(RES/'bbmodels/mobs'/(key+'.json'),data)
        write(ART/'mobs'/(key+'.bbmodel'),blockbench(data,key,'mobs'))
        texture=ART/'textures/mobs'/(key+'.png');texture.parent.mkdir(parents=True,exist_ok=True)
        texture.write_bytes(baseline(f'src/main/resources/assets/mmorpg/textures/imported/mobs/{key}.png'))
        reports.append(dict(id=key,before=len(leaves),after=len(leaves)+added,emissive=glow))
    return reports


if __name__ == '__main__':
    report = dict(mounts=mounts(), bosses=bosses(), mobs=imported_mobs())
    write(ART / 'refinement-report.json', report)
    print(json.dumps(report, ensure_ascii=False))
