import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import modelBytes from '../../app/src/main/assets/fit50/models/fit50-coach.glb';
import {fullBodyPose, applyFullBody} from './full-body.mjs';
import {resolveExercise} from './resolve-exercise.mjs';
import libraryMotions from './library-motions.json';

const key = name => name.replace(/[^a-z0-9]/gi, '');
const floor = new Set(['knee_push','knee_plank','bird_dog','cat_cow','child_pose','glute_bridge','clamshell','dead_bug','pelvic_tilt']);

class ExerciseCoach3D {
  static resolveExercise(ex){return resolveExercise(ex);}
  static motionId(id){return libraryMotions[id]?.id||id;}
  constructor(canvas){
    this.canvas=canvas;this.card=canvas.parentElement;this.exercise=null;this.started=performance.now();this.bones=new Map();this.rest=new Map();this.failed=false;
    // The demonstration is essential exercise content and starts immediately.
    // Keep an explicit pause control; reduced-motion still disables decorative CSS.
    this.paused=false;
    try{
      this.renderer=new THREE.WebGLRenderer({canvas,alpha:true,antialias:true,powerPreference:'low-power'});
      this.renderer.setPixelRatio(Math.min(devicePixelRatio,2));this.renderer.setClearColor(0,0);
      this.renderer.outputColorSpace=THREE.SRGBColorSpace;this.renderer.toneMapping=THREE.ACESFilmicToneMapping;this.renderer.toneMappingExposure=1.3;
      this.scene=new THREE.Scene();this.camera=new THREE.PerspectiveCamera(32,1,.01,30);
      this.scene.add(new THREE.HemisphereLight(0xfff7e9,0x506858,2));
      const lamp=new THREE.DirectionalLight(0xffe7d3,3);lamp.position.set(-2,3,4);this.scene.add(lamp);
      const rim=new THREE.DirectionalLight(0xc8e4ff,2);rim.position.set(2,2,-2);this.scene.add(rim);
      this.pivot=new THREE.Group();this.scene.add(this.pivot);
      const stage=new THREE.Mesh(new THREE.CylinderGeometry(.6,.62,.015,48),new THREE.MeshStandardMaterial({color:0x42614e,roughness:1}));stage.position.y=-.015;this.scene.add(stage);this.stage=stage;
      this.mat=new THREE.Mesh(new THREE.BoxGeometry(1.2,.025,2.5),new THREE.MeshStandardMaterial({color:0x395c50,roughness:1}));this.mat.position.y=-.025;this.mat.visible=false;this.scene.add(this.mat);
      this.step=new THREE.Mesh(new THREE.BoxGeometry(.56,.21,.38),new THREE.MeshStandardMaterial({color:0x566456,roughness:.9}));
      this.step.position.set(0,.105,.3);this.step.visible=false;this.scene.add(this.step);
      this.chair=new THREE.Group();this.scene.add(this.chair);this.chair.visible=false;
      const chairMaterial=new THREE.MeshStandardMaterial({color:0x5b685c,roughness:.86});
      const chairPart=(size,position)=>{const mesh=new THREE.Mesh(new THREE.BoxGeometry(...size),chairMaterial);mesh.position.set(...position);this.chair.add(mesh);};
      chairPart([.56,.055,.49],[0,.47,-.37]);chairPart([.56,.38,.045],[0,.70,-.615]);
      for(const x of [-.235,.235])for(const z of [-.57,-.17])chairPart([.045,.46,.045],[x,.23,z]);
      this.resize=()=>{const b=canvas.getBoundingClientRect();this.renderer.setSize(Math.max(1,b.width),Math.max(1,b.height),false);this.camera.aspect=b.width/Math.max(1,b.height);this.camera.updateProjectionMatrix();};
      this.observer=new ResizeObserver(this.resize);this.observer.observe(canvas);
      this.card.removeAttribute('aria-hidden');this.card.setAttribute('role','button');this.card.tabIndex=0;this.card.setAttribute('aria-label','הגדלת הדגמת התרגיל');
      this.card.addEventListener('click',()=>this.expand());this.card.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();this.expand();}});
      canvas.addEventListener('webglcontextlost',e=>{e.preventDefault();this.failed=true;this.card.dataset.coachState='unavailable';});
      canvas.addEventListener('webglcontextrestored',()=>{this.failed=false;this.card.dataset.coachState='ready';});
      new GLTFLoader().parse(modelBytes.buffer.slice(modelBytes.byteOffset,modelBytes.byteOffset+modelBytes.byteLength),'',gltf=>{
        this.model=gltf.scene;this.pivot.add(this.model);this.model.updateMatrixWorld(true);
        this.model.traverse(o=>{if(o.isBone){this.bones.set(key(o.name),o);this.rest.set(key(o.name),{q:o.quaternion.clone(),world:o.getWorldQuaternion(new THREE.Quaternion()),parent:o.parent.getWorldQuaternion(new THREE.Quaternion())});}if(o.isMesh)o.frustumCulled=false;});
        this.hipOrigin=this.bones.get('upperleg01L').getWorldPosition(new THREE.Vector3()).add(this.bones.get('upperleg01R').getWorldPosition(new THREE.Vector3())).multiplyScalar(.5);
        this.model.position.y=-.9;this.pivot.position.y=.9;this.card.dataset.coachState='ready';
      },()=>{this.failed=true;this.card.dataset.coachState='unavailable';});
      this.frame=this.frame.bind(this);requestAnimationFrame(this.frame);
    }catch(e){this.failed=true;this.card.dataset.coachState='unavailable';console.warn('3D coach unavailable',e);}
  }
  setExercise(id){if(id!==this.card.dataset.exercise)this.paused=false;this.exercise=ExerciseCoach3D.motionId(id);this.playbackRate=libraryMotions[id]?.speed||1;this.started=performance.now();this.time=0;this.card.dataset.exercise=id||'';if(this.dialog){this.dialog.querySelector('.coach-title').textContent=document.getElementById('exName').textContent;this.dialog.querySelector('.coach-pause').textContent=this.paused?'▶':'Ⅱ';}}
  expand(){
    if(this.dialog||this.failed)return;
    const dialog=document.createElement('dialog');dialog.className='coach-dialog';
    const close=document.createElement('button');close.textContent='×';close.className='coach-close';close.setAttribute('aria-label','סגירה');
    const pause=document.createElement('button');pause.textContent=this.paused?'▶':'Ⅱ';pause.className='coach-pause';pause.setAttribute('aria-label','השהיה או הפעלה של ההדגמה');
    const title=document.createElement('div');title.className='coach-title';title.textContent=document.getElementById('exName').textContent;
    dialog.append(close,title,this.canvas,pause);document.body.append(dialog);this.dialog=dialog;
    close.onclick=()=>dialog.close();pause.onclick=()=>{this.paused=!this.paused;pause.textContent=this.paused?'▶':'Ⅱ';};
    dialog.addEventListener('close',()=>{this.card.append(this.canvas);dialog.remove();this.dialog=null;this.resize();this.card.focus();});dialog.showModal();this.resize();
  }
  frame(now){
    requestAnimationFrame(this.frame);
    if(document.hidden||this.failed||!this.model||now-(this.lastFrame||0)<33)return;
    const dt=Math.min(.1,(now-(this.lastFrame||now))/1000);
    this.lastFrame=now;if(!this.paused)this.time=(this.time||0)+dt;
    this.renderAt((this.time||0)*(this.playbackRate||1));
  }
  poseAt(time){
    const pose=fullBodyPose(this.exercise,time);
    if(!pose)return false;
    this.contactErrors=applyFullBody(this,pose);
    return true;
  }
  renderAt(time){
    if(!this.model)return;
    const valid=this.poseAt(time);this.pivot.visible=valid;if(!valid){this.card.dataset.coachState='unavailable';return;}
    this.card.dataset.coachState='ready';
    const low=floor.has(this.exercise);
    const closeup=this.exercise==='shoulder_roll',ankle=this.exercise==='ankle_circle';
    const seated=!!fullBodyPose(this.exercise,time)?.seated;
    const stepping=this.exercise==='low_step';
    this.stage.visible=!low&&!closeup&&!seated&&!stepping;this.mat.visible=low;this.chair.visible=seated;this.step.visible=stepping;
    // Fit the entire cycle once, never follow the hips or zoom with each rep.
    const framingKey=this.exercise+':'+this.camera.aspect;
    if(this.framingKey!==framingKey){
    const points=[];
    for(let t=0;t<8;t+=.25){this.poseAt(t);for(const b of this.bones.values()){const point=b.getWorldPosition(new THREE.Vector3());if((!closeup||point.y>1.02)&&(!ankle||point.y<.85))points.push(point);}}
    const box=new THREE.Box3().setFromPoints(points).expandByScalar(.13);
    const center=box.getCenter(new THREE.Vector3());
    const view=new THREE.Vector3(low?3.2:1.1,low?1.25:.4,low?1.8:3).normalize();
    const right=new THREE.Vector3().crossVectors(new THREE.Vector3(0,1,0),view).normalize();
    const up=new THREE.Vector3().crossVectors(view,right);
    const tan=Math.tan(THREE.MathUtils.degToRad(this.camera.fov/2));
    let distance=0;
    for(const point of points){const d=point.clone().sub(center);distance=Math.max(distance,Math.abs(d.dot(up))/tan+d.dot(view),Math.abs(d.dot(right))/(tan*this.camera.aspect)+d.dot(view));}
    distance=(distance+(closeup||ankle?.30:.35))*1.06;
    this.camera.position.copy(center).addScaledVector(view,distance);this.camera.lookAt(center);
    this.framingKey=framingKey;this.poseAt(time);
    }
    this.renderer.render(this.scene,this.camera);
  }
}
window.Fit50ExerciseCoach3D=ExerciseCoach3D;
