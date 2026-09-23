import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import modelBytes from '../../app/src/main/assets/fit50/models/fit50-coach.glb';
import {fullBodyPose, applyFullBody} from './full-body.mjs';
import {resolveExercise} from './resolve-exercise.mjs';
import libraryMotions from './library-motions.json';

const key = name => name.replace(/[^a-z0-9]/gi, '');
const seated = new Set(['seated_march','chair_knee_lift','seated_leg_extend','chair_row','chair_punch','pillow_squeeze','figure_four_chair','hamstring_chair','seated_twist','thoracic_open']);
const floor = new Set(['knee_push','knee_plank','bird_dog','cat_cow','child_pose','glute_bridge','clamshell','dead_bug','pelvic_tilt']);

// Angles are authored in the human's anatomical frame: +Z is forward,
// +Y is up. Neutral/rest transforms are retained for every bone.
function movement(id, seconds) {
  if(!id)return null;
  const cycle = seconds * Math.PI * 2 / (id.includes('breath') ? 8 : 4);
  const s = Math.sin(cycle), u = (1-Math.cos(cycle))/2;
  const left = Math.max(0,s), right = Math.max(0,-s);
  const p = {bones:{},height:.0,pitch:0,roll:0,yaw:0};
  const r=(name,x=0,y=0,z=0)=>{p.bones[name]=[x,y,z];};
  const arm=(side,x=0,y=0,z=0)=>r('upperarm01.'+side,x,y,z+(side==='L'?-.64:.64));
  const elbow=(side,x=0)=>r('lowerarm01.'+side,x+.35);
  const leg=(side,x=0,z=0)=>r('upperleg01.'+side,x,0,z);
  const knee=(side,x=0)=>r('lowerleg01.'+side,x);
  arm('L');arm('R');elbow('L');elbow('R');
  if(seated.has(id)){p.height=-.42;leg('L',-1.45);leg('R',-1.45);knee('L',1.45);knee('R',1.45);}
  switch(id){
    case 'shoulder_roll':
      for(const side of ['L','R']){
        const sign=side==='L'?1:-1;
        r('clavicle.'+side,.12*s,sign*.18*Math.cos(cycle),sign*.16*s);
        r('shoulder01.'+side,0,sign*.10*Math.cos(cycle),sign*.10*s);
        arm(side,.08*Math.cos(cycle),0,-sign*.05*s);elbow(side,-.04*u);
      }
      r('spine03',-.035*Math.cos(cycle));break;
    case 'march':case 'standing_knee_drive':leg('L',-.75*left);leg('R',-.75*right);knee('L',.8*left);knee('R',.8*right);arm('L',.28*s);arm('R',-.28*s);break;
    case 'seated_march':case 'chair_knee_lift':leg('L',-1.45-.3*left);leg('R',-1.45-.3*right);break;
    case 'seated_leg_extend':knee('L',1.45-1.35*left);knee('R',1.45-1.35*right);break;
    case 'chair_squat':case 'sit_to_stand':case 'wall_sit_short':{
      const a=id==='wall_sit_short'?.65:u;p.height=-.40*a;leg('L',-1.05*a);leg('R',-1.05*a);knee('L',1.65*a);knee('R',1.65*a);r('spine03',.25*a);arm('L',-.65*a);arm('R',-.65*a);break;}
    case 'wall_push':case 'counter_push':case 'triceps_wall':case 'wall_press_iso':case 'wall_plank':{
      const a=['wall_press_iso','wall_plank'].includes(id)?.35:u;p.pitch=.12+.10*a;arm('L',-1.18+.35*a);arm('R',-1.18+.35*a);elbow('L',-.18-.75*a);elbow('R',-.18-.75*a);break;}
    case 'band_row':case 'chair_row':arm('L',-1.1+1.35*u);arm('R',-1.1+1.35*u);elbow('L',-1.4*u);elbow('R',-1.4*u);break;
    case 'towel_row':arm('L',-.9,0,.3);arm('R',-.9,0,-.3);elbow('L',-.5);elbow('R',-.5);break;
    case 'biceps_band':elbow('L',-1.7*u);elbow('R',-1.7*u);break;
    case 'scap_squeeze':r('clavicle.L',0,-.12*u);r('clavicle.R',0,.12*u);arm('L',.1*u);arm('R',.1*u);break;
    case 'front_raise':arm('L',-1.35*u);arm('R',-1.35*u);break;
    case 'lateral_raise':arm('L',0,0,1.35*u);arm('R',0,0,-1.35*u);break;
    case 'arm_swing':arm('L',-1.2*(1-u),0,1.3*u);arm('R',-1.2*(1-u),0,-1.3*u);break;
    case 'wall_angels':case 'wall_slide':arm('L',0,0,1.3+.8*u);arm('R',0,0,-1.3-.8*u);elbow('L',-1.2+.6*u);elbow('R',-1.2+.6*u);break;
    case 'chair_punch':arm('L',-1.4);arm('R',-1.4);elbow('L',-1.3*(1-left));elbow('R',-1.3*(1-right));break;
    case 'heel_raise':case 'wall_calf_raise':p.height=.07*u;r('foot.L',.35*u);r('foot.R',.35*u);break;
    case 'toe_raise':r('foot.L',-.32*u);r('foot.R',-.32*u);break;
    case 'ankle_circle':leg('L',-.15);knee('L',.3);r('foot.L',.2*Math.cos(cycle),0,.18*s);break;
    case 'hip_circle':r('spine05',.09*s,0,.09*Math.cos(cycle));break;
    case 'hip_hinge':case 'good_morning':p.pitch=.4*u;leg('L',-.4*u);leg('R',-.4*u);knee('L',.1*u);knee('R',.1*u);break;
    case 'side_step':case 'band_side_step':case 'step_touch':leg('L',0,.28*left);leg('R',0,-.28*right);knee('L',.1);knee('R',.1);p.height=-.02;break;
    case 'low_step':leg('L',-.65*left);knee('L',.75*left);leg('R',-.65*right);knee('R',.75*right);p.height=.1*u;break;
    case 'back_step':case 'mini_lunge':leg('R',.4*u);leg('L',-.2*u);knee('L',.45*u);knee('R',.4*u);p.height=-.1*u;break;
    case 'line_walk':leg('L',-.3*s);leg('R',.3*s);knee('L',.25*left);knee('R',.25*right);break;
    case 'single_leg_support':leg('R',-.2);knee('R',.7);break;
    case 'tandem_stance':leg('L',-.13);leg('R',.13);break;
    case 'clock_reach':leg('R',.23*Math.cos(cycle),-.2-.12*s);break;
    case 'weight_shift':p.roll=.08*s;leg('L',0,-.08*s);leg('R',0,-.08*s);break;
    case 'knee_push':case 'knee_plank':p.pitch=1.12;p.height=-.64;leg('L',-.2);leg('R',-.2);knee('L',1.1);knee('R',1.1);arm('L',-1.12+.2*u);arm('R',-1.12+.2*u);elbow('L',id==='knee_plank'?-1.3:-.6*u);elbow('R',id==='knee_plank'?-1.3:-.6*u);break;
    case 'bird_dog':case 'cat_cow':case 'child_pose':p.pitch=Math.PI/2;p.height=-.52;leg('L',-1.5);leg('R',-1.5);knee('L',1.5);knee('R',1.5);arm('L',-1.5);arm('R',-1.5);
      if(id==='bird_dog'){const a=Math.floor(seconds/4)%2?'R':'L',b=a==='L'?'R':'L';arm(a,-1.5-1.4*u);leg(b,-1.5+1.5*u);knee(b,1.5*(1-u));}
      if(id==='cat_cow'){r('spine03',.12*s);r('neck01',-.12*s);}
      if(id==='child_pose'){p.height=-.71;arm('L',-2.7);arm('R',-2.7);knee('L',2.2);knee('R',2.2);}break;
    case 'glute_bridge':case 'pelvic_tilt':case 'dead_bug':p.pitch=-Math.PI/2;p.height=-.79+(id==='glute_bridge'?.18*u:.025*u);leg('L',-1);leg('R',-1);knee('L',1.7);knee('R',1.7);
      if(id==='dead_bug'){arm('L',-1.5-.8*left);arm('R',-1.5-.8*right);leg('L',-1.5+.8*right);leg('R',-1.5+.8*left);knee('L',1.5-.7*right);knee('R',1.5-.7*left);}break;
    case 'clamshell':p.roll=Math.PI/2;p.height=-.77;leg('L',-.55,0);leg('R',-.55,-.4*u);knee('L',1);knee('R',1);arm('L',0,0,2.6);break;
    case 'chest_wall_stretch':arm('L',0,-.2,1.5);elbow('L',-1.3);p.yaw=.14;break;
    case 'neck_side':r('neck01',0,0,.15);r('head',0,0,.08);break;
    case 'hamstring_chair':knee('L',.12);r('spine03',.18);break;
    case 'calf_wall':case 'hip_flexor_chair':leg('L',-.25);leg('R',.25);knee('L',.4);arm('L',-.9);arm('R',-.9);p.pitch=.12;break;
    case 'figure_four_chair':leg('R',-1.5,-.45);knee('R',1.4);r('upperleg02.R',0,-.45);break;
    case 'seated_twist':r('spine03',0,.3);r('spine01',0,.12);break;
    case 'thoracic_open':r('spine03',-.12);arm('L',0,0,1.1);arm('R',0,0,-1.1);break;
    case 'deep_breath':case 'box_breath':r('spine03',-.02*u);r('clavicle.L',0,0,.02*u);r('clavicle.R',0,0,-.02*u);break;
    case 'pillow_squeeze':leg('L',-1.45,-.06*u);leg('R',-1.45,.06*u);break;
    case 'chin_tuck':r('neck01',-.07*u);r('head',.07*u);break;
    case 'wrist_mobility':elbow('L',-1.2);elbow('R',-1.2);r('wrist.L',.25*s,0,.2*Math.cos(cycle));r('wrist.R',.25*s,0,-.2*Math.cos(cycle));break;
    case 'side_reach':r('spine03',0,0,-.15);arm('L',0,0,2.65);break;
    default:return null;
  }
  if(['bird_dog','cat_cow','child_pose','knee_push'].includes(id)){
    if(id==='bird_dog')r('wrist.'+(Math.floor(seconds/4)%2?'L':'R'),-1.1);
    else{r('wrist.R',-1.1);r('wrist.L',-1.1);}
  }
  return p;
}

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
    const coordinated=fullBodyPose(this.exercise,time);
    if(coordinated){this.contactErrors=applyFullBody(this,coordinated);return true;}
    const pose=movement(this.exercise,time);if(!pose)return false;
    this.model.position.set(0,-.9,0);this.pivot.position.set(0,.9,0);
    this.card.dataset.coachState='ready';
    for(const [name,bone]of this.bones)bone.quaternion.copy(this.rest.get(name).q);
    for(const [name,angles]of Object.entries(pose.bones)){
      const b=this.bones.get(key(name)),r=this.rest.get(key(name));if(!b)continue;
      const delta=new THREE.Quaternion().setFromEuler(new THREE.Euler(...angles,'XYZ'));
      b.quaternion.copy(r.parent).invert().multiply(delta).multiply(r.parent).multiply(r.q);
    }
    this.pivot.position.y=.9+pose.height;this.pivot.rotation.set(pose.pitch,pose.yaw,pose.roll);
    this.pivot.updateMatrixWorld(true);
    const supports=floor.has(this.exercise)?['foot.L','foot.R','wrist.L','wrist.R','head','lowerleg01.L','lowerleg01.R']:['foot.L','foot.R'];
    const lowest=Math.min(...supports.map(name=>this.bones.get(key(name)).getWorldPosition(new THREE.Vector3()).y));
    this.pivot.position.y+=(floor.has(this.exercise)?.045:.075)-lowest;
    this.pivot.updateMatrixWorld(true);
    return true;
  }
  renderAt(time){
    if(!this.model)return;
    const valid=this.poseAt(time);this.pivot.visible=valid;if(!valid){this.card.dataset.coachState='unavailable';return;}
    this.card.dataset.coachState='ready';
    const low=floor.has(this.exercise);
    const closeup=this.exercise==='shoulder_roll',ankle=this.exercise==='ankle_circle';
    this.stage.visible=!low&&!closeup;this.mat.visible=low;
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
