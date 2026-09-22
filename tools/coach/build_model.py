"""Build the clothed Fit50 coach from CC0 MakeHuman geometry and rig data.
Run with Blender --background --python tools/coach/build_model.py -- SOURCE_DIR.
"""
import bpy, bmesh, json, sys, pathlib, math
from mathutils import Vector

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = pathlib.Path(sys.argv[sys.argv.index('--') + 1]).resolve()
verts, faces, group = [], [], ''
for line in (SRC / 'base.obj').read_text().splitlines():
    parts = line.split()
    if not parts: continue
    if parts[0] == 'v': verts.append(Vector(tuple(map(float, parts[1:4]))))
    elif parts[0] == 'g': group = parts[1]
    elif parts[0] == 'f' and group == 'body': faces.append([int(i.split('/')[0])-1 for i in parts[1:]])
for filename in ['caucasian-male-young.target','universal-male-young-averagemuscle-averageweight.target']:
    for line in (SRC / filename).read_text().splitlines():
        p = line.split()
        if p and not p[0].startswith('#'): verts[int(p[0])] += Vector(tuple(map(float,p[1:4])))
used = sorted({i for f in faces for i in f})
bottom = min(verts[i].y for i in used)
height = max(verts[i].y for i in used) - bottom
def convert(v): return Vector((v.x*1.78/height, -v.z*1.78/height, (v.y-bottom)*1.78/height))
points = [convert(v) for v in verts]
rig_data = json.loads((SRC/'rig.json').read_text())
weights = json.loads((SRC/'weights.json').read_text())['weights']
def joint(name):
    ids = rig_data['joints'][name]
    return sum((points[i] for i in ids),Vector()) / len(ids)

bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)
def material(name,color,roughness=.65):
    m=bpy.data.materials.new(name); m.diffuse_color=(*color,1); m.use_nodes=True
    bs=m.node_tree.nodes.get('Principled BSDF'); bs.inputs['Base Color'].default_value=(*color,1)
    bs.inputs['Roughness'].default_value=roughness
    return m
skin=material('Warm natural skin',(.55,.29,.17),.56)
shirt=material('Forest performance shirt',(.055,.19,.135),.8)
shorts=material('Charcoal training shorts',(.025,.035,.04),.88)
hair=material('Dark cropped hair',(.038,.023,.014),.95)
white=material('Eye sclera',(.8,.76,.69),.35)
iris=material('Brown iris',(.075,.035,.012),.3)
pupil=material('Pupil',(.006,.005,.004),.22)

mesh=bpy.data.meshes.new('Human anatomical surface')
index={old:new for new,old in enumerate(used)}
mesh.from_pydata([points[i] for i in used],[],[[index[i] for i in f] for f in faces]); mesh.update()
body=bpy.data.objects.new('Fit50 Human Coach',mesh); bpy.context.collection.objects.link(body)
for mat in [skin,shirt,shorts,hair]: mesh.materials.append(mat)
for poly in mesh.polygons:
    c=sum((mesh.vertices[i].co for i in poly.vertices),Vector())/len(poly.vertices)
    # Athletic clothing with bare anatomical feet for visible ankle movement.
    if .87 < c.z < 1.43 and abs(c.x)<(.25 if c.z>1.25 else .20): poly.material_index=1
    elif .59 < c.z <= .89: poly.material_index=2
    elif c.z > 1.70 or (c.z>1.60 and c.y>-.005): poly.material_index=3
    poly.use_smooth=True

arm=bpy.data.armatures.new('Coach anatomy rig'); rig=bpy.data.objects.new('CoachRig',arm)
bpy.context.collection.objects.link(rig); bpy.context.view_layer.objects.active=rig; rig.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
for name,data in rig_data['bones'].items():
    b=arm.edit_bones.new(name); b.head=joint(data['head']); b.tail=joint(data['tail'])
    if (b.tail-b.head).length<.0001: b.tail.z+=.001
for name,data in rig_data['bones'].items():
    if data['parent']: arm.edit_bones[name].parent=arm.edit_bones[data['parent']]
bpy.ops.object.mode_set(mode='OBJECT')
for name,entries in weights.items():
    vg=body.vertex_groups.new(name=name)
    for old,w in entries:
        if old in index: vg.add([index[old]],w,'REPLACE')
mod=body.modifiers.new('Weighted anatomical deformation','ARMATURE'); mod.object=rig
body.parent=rig

# Tailored shells with cut hems, sleeve openings and a neck opening. Unlike
# vertex colour clothing these have actual thickness and a separate silhouette.
def garment(name,mat,planes,inflate):
    o=body.copy();o.data=body.data.copy();o.name=name;bpy.context.collection.objects.link(o)
    o.data.materials.clear();o.data.materials.append(mat)
    bm=bmesh.new();bm.from_mesh(o.data)
    if name=='Training shorts':
        bmesh.ops.delete(bm,geom=[v for v in bm.verts if abs(v.co.x)>.23],context='VERTS')
    for point,normal in planes:
        bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.00001,plane_co=point,plane_no=normal,clear_outer=True)
    for face in bm.faces:face.material_index=0
    bmesh.ops.smooth_vert(bm,verts=list(bm.verts),factor=.4,use_axis_x=True,use_axis_y=True,use_axis_z=False)
    bm.normal_update()
    for v in bm.verts:v.co+=v.normal*inflate
    bm.to_mesh(o.data);bm.free()
    for poly in o.data.polygons:poly.use_smooth=True
    sub=o.modifiers.new('Soft fabric','SUBSURF');sub.levels=1
    solid=o.modifiers.new('Hem thickness','SOLIDIFY');solid.thickness=.003
    return o
garment('Forest training T shirt',shirt,[((0,0,.97),(0,0,-1)),((0,0,1.51),(0,0,1)),((.34,0,0),(1,0,0)),((-.34,0,0),(-1,0,0))],.012)
garment('Training shorts',shorts,[((0,0,.60),(0,0,-1)),((0,0,1.01),(0,0,1))],.018)
for poly in body.data.polygons:
    if poly.material_index not in [3]:poly.material_index=0
# Remove the skin under the outfit, avoiding intersections at flexed joints.
bm=bmesh.new();bm.from_mesh(body.data)
hidden=[]
for face in bm.faces:
    c=face.calc_center_median()
    if (.62<c.z<1.0 and abs(c.x)<.22) or (.985<c.z<1.49 and abs(c.x)<.315):hidden.append(face)
bmesh.ops.delete(bm,geom=hidden,context='FACES')
bm.to_mesh(body.data);bm.free()

def sphere(name,center,scale,mat,bone='head'):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24,ring_count=16,location=center)
    o=bpy.context.object; o.name=name; o.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(mat)
    for f in o.data.polygons:f.use_smooth=True
    vg=o.vertex_groups.new(name=bone); vg.add(list(range(len(o.data.vertices))),1,'REPLACE')
    modifier=o.modifiers.new('Follow skeleton','ARMATURE'); modifier.object=rig; o.parent=rig
    return o
for side in ['L','R']:
    c=joint(rig_data['bones']['eye.'+side]['head']); r=.0105
    sphere('Eyeball '+side,c,(r,r,r),white)
    sphere('Iris '+side,c+Vector((0,-r*.9,0)),(.0046,.002,.0046),iris)
    sphere('Pupil '+side,c+Vector((0,-r*1.05,0)),(.0022,.001,.0022),pupil)

out=ROOT/'app/src/main/assets/fit50/models'; out.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.export_scene.gltf(filepath=str(out/'fit50-coach.glb'),export_format='GLB',use_selection=True,export_animations=False,export_yup=True)
print('MODEL_READY',out/'fit50-coach.glb')
