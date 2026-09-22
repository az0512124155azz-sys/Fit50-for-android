import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source=readFileSync('tools/coach/coach.js','utf8').replace(/^import .*;\n/gm,'').split('class ExerciseCoach3D')[0];
const context=vm.createContext({THREE:{MathUtils:{clamp:(x,a,b)=>Math.min(b,Math.max(a,x))}}});
vm.runInContext(source+';this.pose=movement;',context);
const catalog=readFileSync('app/src/main/java/com/fit50/app/WorkoutPlanEngine.kt','utf8');
const ids=[...catalog.matchAll(/Ex\("([^"]+)"/g)].map(m=>m[1]);
const glb=readFileSync('app/src/main/assets/fit50/models/fit50-coach.glb');
assert.equal(glb.toString('utf8',0,4),'glTF');
const gltf=JSON.parse(glb.toString('utf8',20,20+glb.readUInt32LE(12)));
const bones=new Set(gltf.nodes.map(n=>n.name));
for(const id of ids){
  for(let t=0;t<=8;t+=.5){
    const pose=context.pose(id,t);assert.ok(pose,`No movement for ${id}`);
    for(const [name,angles]of Object.entries(pose.bones)){
      assert.ok(bones.has(name),`Missing rig joint ${name} for ${id}`);
      assert.ok(angles.every(Number.isFinite),`Invalid angles for ${id}`);
    }
  }
}
assert.equal(context.pose('unknown-exercise',0),null);
assert.ok(gltf.skins.length>0,'Must be a skinned mesh, not an image');
assert.ok(!gltf.buffers.some(b=>b.uri),'GLB must be self-contained');
console.log(`${ids.length} exercises: valid poses across 17 phases; all joint names exist; standalone skinned GLB verified.`);
