"""Background Blender previews of the exported voxel geometry, with simplified block materials.

Run: blender --background --factory-startup --python render_blender.py -- OUTPUT [scene]
These are geometry checks, not screenshots from the Minecraft client.
"""
import bpy,sys,json,math,gc
from pathlib import Path
import numpy as np
from mathutils import Vector

out=Path(sys.argv[sys.argv.index('--')+1]);wanted=sys.argv[sys.argv.index('--')+2:] or ['all']
meta=json.loads((out/'map.json').read_text(encoding='utf-8'))
palette=meta['palette'];colors=json.loads((out/'colors.json').read_text())
blocks=np.load(out/'blocks.npy',mmap_mode='r')

scene=bpy.context.scene
for ob in list(scene.objects):bpy.data.objects.remove(ob,do_unlink=True)
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True
scene.render.resolution_x=1600;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.render.threads_mode='FIXED';scene.render.threads=4
scene.world.use_nodes=True;scene.world.node_tree.nodes.get('Background').inputs['Color'].default_value=(.35,.46,.65,1)
scene.world.node_tree.nodes.get('Background').inputs['Strength'].default_value=.65
scene.view_settings.view_transform='AgX'

materials=[]
for name,col in zip(palette,colors):
    mat=bpy.data.materials.new(name);mat.use_nodes=True
    bs=mat.node_tree.nodes.get('Principled BSDF')
    rgba=tuple((v/255)**2.2 for v in col)+(1,)
    mat.diffuse_color=rgba;bs.inputs['Base Color'].default_value=rgba;bs.inputs['Roughness'].default_value=.9
    if any(s in name for s in ['sea_lantern','glowstone','shroomlight','lava','lantern']):
        bs.inputs['Emission Color'].default_value=rgba;bs.inputs['Emission Strength'].default_value=.6
    materials.append(mat)

sun_data=bpy.data.lights.new('Soleil','SUN');sun_data.energy=3.2;sun_data.angle=math.radians(12)
sun=bpy.data.objects.new('Soleil',sun_data);scene.collection.objects.link(sun);sun.rotation_euler=(math.radians(24),math.radians(-32),math.radians(-25))
cam_data=bpy.data.cameras.new('Vue');cam=bpy.data.objects.new('Vue',cam_data);scene.collection.objects.link(cam);scene.camera=cam
cam_data.type='ORTHO';cam_data.clip_start=.1;cam_data.clip_end=3000

def mesh_crop(x,z,r,name):
    x1=max(-400,x-r);x2=min(400,x+r);z1=max(-400,z-r);z2=min(400,z+r)
    a=np.array(blocks[108:,z1+400:z2+400,x1+400:x2+400]);offset=np.array([x1,-z1,44],np.float32)
    # Keep geometry for structural blocks; fine vegetation is omitted in external previews.
    skip=np.array([i==0 or any(v in b for v in ['short_grass','fern','dead_bush','dandelion','poppy','azure_bluet','oxeye_daisy','wheat','carrots','beetroots','sugar_cane']) for i,b in enumerate(palette)])
    solid=~skip[a];p=np.pad(solid,1)
    dims=a.shape
    verts=[];mats=[]
    # Each face has its own vertices. Coordinates map MC (x,y,z) to Blender (x,-z,y).
    sides=[
        ((1,0,0),[(0,0,1),(1,0,1),(1,-1,1),(0,-1,1)]),
        ((-1,0,0),[(0,-1,0),(1,-1,0),(1,0,0),(0,0,0)]),
        ((0,0,-1),[(0,0,0),(0,0,1),(0,-1,1),(0,-1,0)]),
        ((0,0,1),[(1,-1,0),(1,-1,1),(1,0,1),(1,0,0)]),
        ((0,-1,0),[(1,0,0),(1,0,1),(0,0,1),(0,0,0)]),
        ((0,1,0),[(0,-1,0),(0,-1,1),(1,-1,1),(1,-1,0)])]
    for (dy,dz,dx),corners in sides:
        neighbor=p[1+dy:1+dy+dims[0],1+dz:1+dz+dims[1],1+dx:1+dx+dims[2]]
        iy,iz,ix=np.nonzero(solid&~neighbor)
        xyz=np.stack((ix,-iz,iy),axis=-1).astype(np.float32)+offset
        verts.append((xyz[:,None,:]+np.array(corners,np.float32)).reshape(-1,3))
        mats.append(a[iy,iz,ix].astype(np.int32))
    vv=np.concatenate(verts);mi=np.concatenate(mats);n=len(mi)
    mesh=bpy.data.meshes.new(name)
    mesh.vertices.add(len(vv));mesh.vertices.foreach_set('co',vv.ravel())
    mesh.loops.add(n*4);mesh.loops.foreach_set('vertex_index',np.arange(n*4,dtype=np.int32))
    mesh.polygons.add(n);mesh.polygons.foreach_set('loop_start',np.arange(0,n*4,4,dtype=np.int32));mesh.polygons.foreach_set('loop_total',np.full(n,4,np.int32))
    for m in materials:mesh.materials.append(m)
    mesh.polygons.foreach_set('material_index',mi);mesh.update()
    ob=bpy.data.objects.new(name,mesh);scene.collection.objects.link(ob)
    print(f'{name}: {n} faces visibles',flush=True)
    return ob

scenes=[
    ('01-sylveronce',-285,20,105,(-455,-180,235),(-285,20,105),215),
    ('02-abbaye',55,302,95,(-90,440,220),(55,312,105),190),
    ('03-borealis',-226,-274,112,(-370,-80,300),(-226,-266,155),240),
    ('04-ignis',300,-55,93,(460,115,250),(295,-60,122),190),
    ('05-astreon',280,264,104,(445,450,285),(278,270,143),210),
    ('06-place-horizons',0,0,92,(122,155,195),(0,0,88),155),
    ('07-brumevert',-245,220,84,(-370,370,168),(-245,220,70),165),
]
folder=out/'vues-3d';folder.mkdir(exist_ok=True)
for name,x,z,r,eye,target,scale in scenes:
    if wanted!=['all'] and not any(s in name for s in wanted):continue
    ob=mesh_crop(x,z,r,name)
    cam.location=(eye[0],-eye[1],eye[2]);aim=Vector((target[0],-target[1],target[2]));cam.rotation_euler=(aim-cam.location).to_track_quat('-Z','Y').to_euler();cam_data.ortho_scale=scale
    scene.render.filepath=str(folder/(name+'.png'))
    bpy.ops.render.render(write_still=True)
    mesh=ob.data;bpy.data.objects.remove(ob,do_unlink=True);bpy.data.meshes.remove(mesh);gc.collect()
print('Rendus géométriques terminés.',flush=True)
