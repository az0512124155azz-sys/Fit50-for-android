import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {resolveExercise} from './resolve-exercise.mjs';
import {fullBodyPose} from './full-body.mjs';
const catalog=readFileSync('app/src/main/java/com/fit50/app/WorkoutPlanEngine.kt','utf8');
const ids=[...catalog.matchAll(/Ex\("([^"]+)"/g)].map(m=>m[1]);
for(const [,id,name] of catalog.matchAll(/Ex\("([^"]+)","([^"]+)"/g)){
  assert.equal(resolveExercise({id,n:'translated name'}),id);
  assert.equal(resolveExercise({n:name}),id,'Name-only saved workout: '+name);
  assert.equal(resolveExercise({id:'old-id',n:'\u200f '+name+' '}),id);
}
assert.equal(resolveExercise({id:'unknown',n:'unknown'}),null);
const html=readFileSync('app/src/main/assets/fit50/workout.html','utf8');
const defaults=html.split('let WORKOUT = {')[1].split('/*')[0];
for(const [,name]of defaults.matchAll(/\{ n: '([^']+)'/g))assert.ok(resolveExercise({n:name}),'Default workout: '+name);
const glb=readFileSync('app/src/main/assets/fit50/models/fit50-coach.glb');
assert.equal(glb.toString('utf8',0,4),'glTF');
const gltf=JSON.parse(glb.toString('utf8',20,20+glb.readUInt32LE(12)));
assert.ok(gltf.skins.length>0,'Must be a skinned mesh, not an image');
assert.ok(!gltf.buffers.some(b=>b.uri),'GLB must be self-contained');
for(const id of ids){
  for(let t=0;t<=8;t+=.5){
    const pose=fullBodyPose(id,t);assert.ok(pose,`No movement for ${id}`);
    for(const [label,targets] of [['feet',pose.feet],['hands',pose.hands],['knees',pose.knees],['elbows',pose.elbows]]){
      for(const side of ['L','R'])assert.ok(targets[side].length===3&&targets[side].every(Number.isFinite),`Invalid ${label}.${side} target for ${id}`);
    }
    assert.ok(pose.hips.length===3&&pose.hips.every(Number.isFinite),`Invalid pelvis for ${id}`);
    assert.ok(pose.rotation.length===3&&pose.rotation.every(Number.isFinite),`Invalid rotation for ${id}`);
  }
}
assert.equal(fullBodyPose('unknown-exercise',0),null);
console.log(`${ids.length} exercises: finite full-body poses across 17 phases; exercise resolution and skinned GLB verified.`);
