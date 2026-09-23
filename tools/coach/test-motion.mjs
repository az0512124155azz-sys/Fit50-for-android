import * as THREE from 'three';
import {GLTFLoader} from 'three/examples/jsm/loaders/GLTFLoader.js';
import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {fullBodyPose,applyFullBody} from './full-body.mjs';
const bytes=readFileSync('app/src/main/assets/fit50/models/fit50-coach.glb');
const gltf=await new GLTFLoader().parseAsync(bytes.buffer.slice(bytes.byteOffset,bytes.byteOffset+bytes.byteLength),'');
const pivot=new THREE.Group(),model=gltf.scene,bones=new Map(),rest=new Map();pivot.add(model);pivot.updateMatrixWorld(true);
model.traverse(o=>{if(o.isBone){const name=o.name.replace(/[^a-z0-9]/gi,'');bones.set(name,o);rest.set(name,{q:o.quaternion.clone(),world:o.getWorldQuaternion(new THREE.Quaternion())});}});
const hipOrigin=bones.get('upperleg01L').getWorldPosition(new THREE.Vector3()).add(bones.get('upperleg01R').getWorldPosition(new THREE.Vector3())).multiplyScalar(.5);
const rig={pivot,model,bones,rest,hipOrigin};
const ids=[...readFileSync('app/src/main/java/com/fit50/app/WorkoutPlanEngine.kt','utf8').matchAll(/Ex\("([^"]+)"/g)].map(m=>m[1]);
for(const id of ids)assert.ok(fullBodyPose(id,0),`Missing coordinated movement for ${id}`);
for(const id of ids){let max=0,previous=null,worst=null;for(let t=0;t<=8;t+=1/60){const errors=applyFullBody(rig,fullBodyPose(id,t));if(Math.max(...errors)>max){max=Math.max(...errors);worst={t,errors};}const positions=[];for(const b of bones.values()){assert.ok(b.quaternion.toArray().every(Number.isFinite));positions.push(b.getWorldPosition(new THREE.Vector3()));}
 if(previous)positions.forEach((p,i)=>assert.ok(p.distanceTo(previous[i])<.05,id+' abrupt joint movement'));
 previous=positions;}
 if(fullBodyPose(id,0).palms){applyFullBody(rig,fullBodyPose(id,0));for(const side of ['L','R']){
   const pos=n=>bones.get(n+side).getWorldPosition(new THREE.Vector3());
   const along=pos('finger31').sub(pos('wrist')).normalize();
   const across=pos('finger21').sub(pos('finger51')).normalize();
   assert.ok(Math.abs(along.y)<.001&&along.z>.99,id+' fingers forward');
   assert.ok(Math.abs(across.y)<.001,id+' palm horizontal');
 }}
 if(max>.005)console.error(id,'unreachable target',worst);
 assert.ok(max<.005,id+' limb target must be reachable');
 const rounded=p=>JSON.stringify(p,(k,v)=>typeof v==='number'?Math.round(v*1e6)/1e6:v);
 assert.equal(rounded(fullBodyPose(id,0)),rounded(fullBodyPose(id,8)),id+' loop');
}
for(const id of ['chair_squat','sit_to_stand','hip_hinge']){for(let t=0;t<4;t+=.05){const p=fullBodyPose(id,t);applyFullBody(rig,p);for(const side of ['L','R'])assert.ok(bones.get('foot'+side).getWorldPosition(new THREE.Vector3()).distanceTo(new THREE.Vector3(...p.feet[side]))<.01,id+' planted foot');}}
assert.ok(fullBodyPose('chair_squat',2).hips[1]<fullBodyPose('chair_squat',0).hips[1]-.25);
for(const [side,offset]of [['L',0],['R',4]]){
  const quaternions=[];
  for(const t of [.5,1,1.5,2.5,3]){
    const p=fullBodyPose('ankle_circle',offset+t);applyFullBody(rig,p);
    const moving=bones.get('foot'+side),support=bones.get('foot'+(side==='L'?'R':'L'));
    assert.ok(moving.getWorldPosition(new THREE.Vector3()).y>.20,'Rotating foot must be lifted');
    assert.ok(Math.abs(support.getWorldPosition(new THREE.Vector3()).y-.075)<.005,'Support foot planted');
    quaternions.push(moving.getWorldQuaternion(new THREE.Quaternion()));
  }
  assert.ok(quaternions[0].angleTo(quaternions[2])>.4,'Ankle must rotate, not just lift');
}
console.log(ids.length+' coordinated cycles: finite skeletons, loop continuity, planted feet and squat depth verified.');
