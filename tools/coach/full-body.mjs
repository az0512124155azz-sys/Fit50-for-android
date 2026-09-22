import * as THREE from 'three';
const key = name => name.replace(/[^a-z0-9]/gi, '');
const v = a => new THREE.Vector3(...a);
const smooth = x => x*x*x*(x*(x*6-15)+10);
// Rest at both ends, including zero acceleration: no snapping when sides swap.
const repetition = t => {const p=((t%4)+4)%4/4;return p<.4?smooth(p/.4):p<.5?1:p<.9?1-smooth((p-.5)/.4):0;};
export function fullBodyPose(id,t){
  const u=repetition(t),s=Math.sin(t*Math.PI/2), side=Math.floor(t/4)%2?'R':'L';
  const p={hips:[0,.91,0],rotation:[0,0,0],feet:{L:[.14,.075,0],R:[-.14,.075,0]},hands:{L:[.29,1,.03],R:[-.29,1,.03]},knees:{L:[.14,.45,.7],R:[-.14,.45,.7]},elbows:{L:[.65,1.2,-.2],R:[-.65,1.2,-.2]},spine:0,head:0,footPitch:0};
  const both=(fn)=>['L','R'].forEach(a=>fn(a,a==='L'?1:-1));
  switch(id){
    case 'front_raise':case 'lateral_raise':case 'band_row':case 'biceps_band':{
      both((a,sign)=>{
        if(id==='front_raise')p.hands[a]=[sign*.23,1+.42*u,.03+.43*u];
        if(id==='lateral_raise')p.hands[a]=[sign*(.29+.39*u),1+.43*u,.03];
        if(id==='band_row')p.hands[a]=[sign*.24,1.25,.46-.39*u];
        if(id==='biceps_band')p.hands[a]=[sign*.28,1+.36*u,.03+.15*u];
        p.elbows[a]=[sign*.6,1.1,['band_row','lateral_raise'].includes(id)?-.3:0];
      });p.spine=-.035*u;p.head=.02*u;break;}
    case 'seated_march':case 'chair_knee_lift':case 'seated_leg_extend':case 'chair_row':case 'chair_punch':{
      p.hips=[0,.53,-.27];
      both((a,sign)=>{p.feet[a]=[sign*.14,.075,.21];p.hands[a]=[sign*.29,.69,-.15];p.knees[a]=[sign*.14,.45,.65];
        const phase=((t/4+(a==='L'?0:.5))%1+1)%1, lift=phase<.5?Math.sin(phase*2*Math.PI)**2:0;
        if(id==='seated_march'||id==='chair_knee_lift'){p.feet[a][1]+=.17*lift;p.hands[a][2]-=.04*lift;}
        if(id==='seated_leg_extend'){p.feet[a][1]+=.40*lift;p.feet[a][2]+=.32*lift;p.knees[a][1]=1;}
        if(id==='chair_row')p.hands[a]=[sign*.24,.90,.20-.38*u];
        if(id==='chair_punch')p.hands[a]=[sign*.22,.94,-.08+.33*lift];
      });p.rotation[1]=id==='chair_punch'?.08*s:0;break;}
    case 'march':case 'standing_knee_drive':case 'line_walk':{
      const high=id==='standing_knee_drive'?.3:.15;
      both((a,sign)=>{const phase=((t/4+(a==='L'?0:.5))%1+1)%1;const lift=phase<.5?Math.sin(phase*2*Math.PI)**2:0;
        p.feet[a]=[sign*(id==='line_walk'?.065:.14),.075+high*lift,.16*lift];
        p.hands[a]=[sign*.29,1+.045*Math.abs(s),.02+sign*.20*s];
        p.hips[0]-=sign*.04*lift;
      });p.hips[1]+=.012*(1-Math.cos(t*Math.PI));p.rotation[1]=.055*s;p.spine=-.025*s;break;}
    case 'chair_squat':case 'sit_to_stand':case 'wall_sit_short':{
      const a=id==='wall_sit_short'?.8:u;p.hips=[0,.91-.31*a,-.20*a];p.rotation[0]=.30*a;p.head=-.19*a;
      both((b,sign)=>{p.feet[b][0]=sign*.18;p.hands[b]=[sign*.23,1+.22*a,.03+.42*a];});break;}
    case 'hip_hinge':case 'good_morning':p.hips=[0,.91-.09*u,-.23*u];p.rotation[0]=.65*u;p.head=-.2*u;both((a,sign)=>p.hands[a]=[sign*.29,1-.18*u,.03+.22*u]);break;
    case 'weight_shift':p.hips[0]=.075*s;p.hips[1]-=.02*Math.abs(s);both((a,sign)=>p.hands[a][0]+=p.hips[0]);break;
    case 'heel_raise':case 'wall_calf_raise':p.hips[1]+=.065*u;p.footPitch=.30*u;both(a=>{p.feet[a][1]+=.065*u;p.feet[a][2]+=.015*u;p.hands[a][1]+=.065*u;});break;
    case 'wall_push':case 'counter_push':case 'triceps_wall':case 'wall_press_iso':case 'wall_plank':{
      const a=['wall_press_iso','wall_plank'].includes(id)?.35:u,low=id==='counter_push';
      p.hips=[0,low?.79:.85,(low?-.12:0)+.13*a];p.rotation[0]=(low?.48:.16)+.12*a;p.head=-.12;
      both((b,sign)=>{p.feet[b][2]=low?-.45:-.27;p.hands[b]=[sign*(id==='triceps_wall'?.16:.23),low?1.02:1.32,.47];p.elbows[b]=[sign*.6,1,.2];});break;}
    case 'bird_dog':case 'cat_cow':case 'child_pose':{
      p.hips=[0,.49,-.27];p.rotation[0]=Math.PI/2;p.head=-.2;p.palms=true;
      both((a,sign)=>{p.hands[a]=[sign*.20,.055,.34];p.feet[a]=[sign*.14,.075,-.73];p.knees[a]=[sign*.14,-.4,-.25];p.elbows[a]=[sign*.55,.25,.05];});
      if(id==='bird_dog'){const other=side==='L'?'R':'L',sign=side==='L'?1:-1;
        p.hips[0]=-.025*sign*u;p.hands[side]=[sign*.20,.055+.435*u,.34+.36*u];p.feet[other]=[-sign*.14,.075+.415*u,-.73-.36*u];
        p.elbows[side]=[sign*(.55-.05*u),.25+.35*u,.05+.50*u];p.knees[other]=[-sign*.14,-.4+.45*u,-.25-.45*u];}
      if(id==='cat_cow'){p.spine=.15*s;p.head=-.2-.25*s;p.hips[1]+=.015*s;}
      if(id==='child_pose'){p.hips=[0,.28,-.48];p.rotation[0]=1.2;both((a,sign)=>p.hands[a]=[sign*.20,.10,.30]);}break;}
    case 'glute_bridge':case 'pelvic_tilt':{
      const a=id==='glute_bridge'?u:.13*u;p.hips=[0,.17+.19*a,0];p.rotation[0]=-Math.PI/2-.36*a;p.head=.12;
      both((b,sign)=>{p.feet[b]=[sign*.15,.075,.57];p.knees[b]=[sign*.15,.9,.3];p.hands[b]=[sign*.32,.10,-.3];p.elbows[b]=[sign*.55,.1,-.35];});break;}
    case 'dead_bug':{
      p.hips=[0,.17,0];p.rotation[0]=-Math.PI/2;p.head=.1;
      both((a,sign)=>{p.hands[a]=[sign*.2,.72,-.42];p.feet[a]=[sign*.15,.52,.36];p.knees[a]=[sign*.15,1,.1];p.elbows[a]=[sign*.4,.4,-.55];});
      const other=side==='L'?'R':'L';p.hands[side][1]-=.50*u;p.hands[side][2]-=.43*u;p.feet[other][1]-=.35*u;p.feet[other][2]+=.40*u;break;}
    default:return null;
  }return p;
}

// Two-bone inverse kinematics. The intermediate twist bones retain their rest
// transforms. Solve in world space, then convert rotations back to parent space.
export function solveLimb(bones,upper,middle,end,target,pole){
  const a=bones.get(key(upper)),b=bones.get(key(middle)),c=bones.get(key(end));
  const pos=o=>o.getWorldPosition(new THREE.Vector3());
  const A=pos(a),B=pos(b),C=pos(c),T=v(target),axis=T.clone().sub(A),raw=axis.length();
  const l1=A.distanceTo(B),l2=B.distanceTo(C),d=THREE.MathUtils.clamp(raw,Math.abs(l1-l2)+.0001,l1+l2-.0001);axis.normalize();
  const bend=v(pole).sub(A);bend.addScaledVector(axis,-bend.dot(axis));
  if(bend.lengthSq()<1e-8)bend.set(0,0,1).addScaledVector(axis,-axis.z);bend.normalize();
  const along=(l1*l1-l2*l2+d*d)/(2*d),height=Math.sqrt(Math.max(0,l1*l1-along*along));
  const desired=A.clone().addScaledVector(axis,along).addScaledVector(bend,height);
  const rotate=(bone,from,to)=>{const world=bone.getWorldQuaternion(new THREE.Quaternion());
    const delta=new THREE.Quaternion().setFromUnitVectors(from.normalize(),to.normalize());
    bone.quaternion.copy(bone.parent.getWorldQuaternion(new THREE.Quaternion()).invert()).multiply(delta).multiply(world);bone.updateMatrixWorld(true);};
  rotate(a,B.clone().sub(A),desired.clone().sub(A));
  const mid=pos(b);rotate(b,pos(c).sub(mid),A.clone().addScaledVector(axis,d).sub(mid));
  return pos(c).distanceTo(T);
}

export function applyFullBody(rig,pose){
  const {bones,rest,pivot,model}=rig;
  for(const [name,bone]of bones)bone.quaternion.copy(rest.get(name).q);
  pivot.rotation.set(...pose.rotation);pivot.position.fromArray(pose.hips);
  model.position.copy(rig.hipOrigin).multiplyScalar(-1);pivot.updateMatrixWorld(true);
  for(const [name,angle]of [['spine03',pose.spine],['head',pose.head]]){
    const bone=bones.get(name);bone.rotateX(angle);bone.updateMatrixWorld(true);
  }
  const errors=[];
  for(const side of ['L','R']){
    errors.push(solveLimb(bones,'upperleg01.'+side,'lowerleg01.'+side,'foot.'+side,pose.feet[side],pose.knees[side]));
    const foot=bones.get(key('foot.'+side));
    const world=new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1,0,0),pose.footPitch).multiply(rest.get(key('foot.'+side)).world);
    foot.quaternion.copy(foot.parent.getWorldQuaternion(new THREE.Quaternion()).invert()).multiply(world);foot.updateMatrixWorld(true);
    errors.push(solveLimb(bones,'upperarm01.'+side,'lowerarm01.'+side,'wrist.'+side,pose.hands[side],pose.elbows[side]));
    if(pose.palms){const wrist=bones.get(key('wrist.'+side)),pos=name=>bones.get(key(name+'.'+side)).getWorldPosition(new THREE.Vector3());
      // Derive the palm plane from the actual finger rig, not assumed bone axes.
      const forward=pos('finger3-1').sub(pos('wrist')).normalize();
      const across=pos('finger2-1').sub(pos('finger5-1'));across.addScaledVector(forward,-across.dot(forward)).normalize();
      const normal=new THREE.Vector3().crossVectors(across,forward).normalize();
      const source=new THREE.Matrix4().makeBasis(across,forward,normal);
      const targetAcross=new THREE.Vector3(side==='L'?-1:1,0,0),targetForward=new THREE.Vector3(0,0,1);
      const target=new THREE.Matrix4().makeBasis(targetAcross,targetForward,new THREE.Vector3().crossVectors(targetAcross,targetForward));
      const delta=new THREE.Quaternion().setFromRotationMatrix(target.multiply(source.invert()));
      const world=delta.multiply(wrist.getWorldQuaternion(new THREE.Quaternion()));
      wrist.quaternion.copy(wrist.parent.getWorldQuaternion(new THREE.Quaternion()).invert()).multiply(world);wrist.updateMatrixWorld(true);}
  }return errors;
}
