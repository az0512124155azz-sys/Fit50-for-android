import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import path from 'node:path';
import {state,validate,nativeCatalog,root} from './catalog.mjs';
import {resolveExercise} from '../../tools/coach/resolve-exercise.mjs';
const current=state(),bases=nativeCatalog();
assert.equal(current.all.filter(e=>e.kind==='base').length,bases.length);
assert.ok(current.all.filter(e=>e.kind==='tempo-variant').length>=24);
for(const e of current.all.filter(e=>e.kind==='tempo-variant'))validate(current.all,[e.id]);
assert.throws(()=>validate(current.all,['unknown']));
assert.throws(()=>validate(current.all,['march']));
assert.throws(()=>validate([...current.all,current.all[0]],[]));
assert.throws(()=>validate(current.all.map((e,i)=>i===0?{...e,motion:{id:'fake',speed:1}}:e),[]));
const motions=JSON.parse(readFileSync(path.join(root,'tools/coach/library-motions.json'),'utf8'));
for(const e of current.all){assert.equal(motions[e.id].id,e.motion.id);assert.equal(motions[e.id].speed,e.motion.speed);assert.equal(resolveExercise({id:e.id}),e.id);assert.equal(resolveExercise({n:e.name}),e.id);}
const server=process.argv[2];
if(server){
  const initial=await (await fetch(server+'/api/library')).json();
  const post=(body,headers={})=>fetch(server+'/api/activate',{method:'POST',headers:{'Content-Type':'application/json',Origin:server,'X-Library-Token':initial.token,...headers},body:JSON.stringify(body)});
  assert.equal((await post({id:'march_slow',enabled:true},{'X-Library-Token':'wrong'})).status,403);
  assert.equal((await post({id:'unknown',enabled:true})).status,400);
  assert.equal((await post({id:'march',enabled:false})).status,400);
  assert.equal((await fetch(server+'/.git/config')).status,404);
  const id=initial.all.find(e=>e.kind==='tempo-variant'&&!initial.active.includes(e.id)).id;
  try{
    assert.equal((await post({id,enabled:true})).status,200);
    assert.equal((await post({id,enabled:true})).status,200);
    const after=await(await fetch(server+'/api/library')).json();assert.equal(after.active.filter(x=>x===id).length,1);
    assert.ok(readFileSync(path.join(root,'app/src/main/java/com/fit50/app/LibraryExercises.kt'),'utf8').includes(`id = "${id}"`));
    const catalog=JSON.parse(readFileSync(path.join(root,'tools/coach/exercise-catalog.json'),'utf8'));assert.ok(catalog.some(e=>e.id===id));
  }finally{assert.equal((await post({id,enabled:false})).status,200);}
  assert.deepEqual((await(await fetch(server+'/api/library')).json()).active,initial.active);
}
console.log(`${current.all.length} library entries validated; ${server?'HTTP activation, idempotency, removal and write protection passed':'motion references and invalid entries checked'}.`);
