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
  const seated=()=>{p.seated=true;p.hips=[0,.53,-.27];both((a,sign)=>{p.feet[a]=[sign*.14,.075,.21];p.knees[a]=[sign*.14,.8,.35];p.hands[a]=[sign*.29,.69,-.15];p.elbows[a]=[sign*.54,.77,-.35];});};
  const quadruped=()=>{p.hips=[0,.49,-.27];p.rotation[0]=Math.PI/2;p.head=-.2;p.palms=true;both((a,sign)=>{p.hands[a]=[sign*.20,.055,.34];p.feet[a]=[sign*.14,.075,-.73];p.knees[a]=[sign*.14,-.4,-.25];p.elbows[a]=[sign*.55,.25,.05];});};
  switch(id){
    case 'shoulder_roll':{
      const phase=t*Math.PI/2;p.shoulderRoll=.12*Math.sin(phase);p.spine=-.025*Math.cos(phase);
      both((a,sign)=>{p.hands[a]=[sign*.29,1+.035*(1-Math.cos(phase)),.03+.04*Math.sin(phase)];});break;}
    case 'deep_breath':case 'box_breath':{
      const breath=(1-Math.cos(t*Math.PI/4))/2;p.spine=-.022*breath;
      both((a,sign)=>p.hands[a]=[sign*(.29+.045*breath),1+.055*breath,.03+.035*breath]);break;}
    case 'neck_side':p.headRoll=.13*u;p.hands.L=[.30,1.03,.02];break;
    case 'chin_tuck':p.head=.09*u;p.spine=-.015*u;break;
    case 'wrist_mobility':{
      both((a,sign)=>{p.hands[a]=[sign*.27,1.16,.16];p.elbows[a]=[sign*.55,1.08,-.15];});
      p.wrists={L:[.24*Math.sin(t*Math.PI/2),0,.20*Math.cos(t*Math.PI/2)],R:[.24*Math.sin(t*Math.PI/2),0,-.20*Math.cos(t*Math.PI/2)]};break;}
    case 'side_reach':{
      p.spineRoll=-.14*u;p.hips[0]=-.025*u;
      p.hands.L=[.38,1.66+.10*u,.02];p.elbows.L=[.54,1.45,-.18];
      p.hands.R=[-.27,.95,.04];break;}
    case 'chest_wall_stretch':{
      p.spineYaw=.14*u;p.hands.L=[.49,1.30,.06];p.elbows.L=[.62,1.14,-.15];
      p.hands.R=[-.29,1.02,.02];break;}
    case 'calf_wall':case 'hip_flexor_chair':{
      p.hips=[0,.87,-.04];p.rotation[0]=.14;p.feet.L=[.17,.075,.16];p.feet.R=[-.17,.075,-.27];
      p.knees.L=[.17,.45,.60];p.knees.R=[-.17,.43,.37];
      both((a,sign)=>p.hands[a]=[sign*.22,1.23,.39]);break;}
    case 'hamstring_chair':{
      seated();p.feet.L=[.14,.075,.45];p.knees.L=[.14,.95,.40];
      p.rotation[0]=.22*u;p.hands.L=[.25,.79,.12+.12*u];p.hands.R=[-.27,.64,-.12];break;}
    case 'figure_four_chair':{
      seated();p.feet.R=[.12,.50,.13];p.knees.R=[-.11,.85,.30];
      p.hands.L=[.28,.75,.05];p.hands.R=[-.18,.68,.08];break;}
    case 'pillow_squeeze':{
      seated();both((a,sign)=>{p.feet[a][0]=sign*(.14-.025*u);p.knees[a][0]=sign*(.14-.075*u);});break;}
    case 'seated_twist':{
      seated();p.spineYaw=.22*u;p.hands.L=[.27,.85,.05];p.hands.R=[-.23,.85,.08];break;}
    case 'thoracic_open':{
      seated();p.spineYaw=.13*u;p.spine=-.04*u;
      both((a,sign)=>{p.hands[a]=[sign*(.29+.19*u),.90+.10*u,-.08];p.elbows[a]=[sign*.65,1.08,-.24];});break;}
    case 'hip_circle':{
      p.hips=[.055*Math.sin(t*Math.PI/2),.91,.055*(1-Math.cos(t*Math.PI/2))];
      p.rotation[2]=.055*Math.sin(t*Math.PI/2);p.rotation[0]=.05*Math.cos(t*Math.PI/2);
      both((a,sign)=>p.hands[a]=[sign*.17+p.hips[0],.97,p.hips[2]+.08]);break;}
    case 'toe_raise':p.ankles={L:[-.28*u,0,0],R:[-.28*u,0,0]};p.hips[2]=-.02*u;break;
    case 'side_step':case 'band_side_step':case 'step_touch':{
      const sign=side==='L'?1:-1,other=side==='L'?'R':'L',spread=id==='band_side_step'?.20:.27;
      p.feet[side][0]+=sign*spread*u;p.feet[side][1]+=.055*Math.sin(Math.PI*u);
      p.feet[other][0]+=sign*.055*u;p.hips[0]+=sign*.11*u;p.hips[1]-=.025*u;
      p.hands.L=[.28+p.hips[0],1,.04+.12*u];p.hands.R=[-.28+p.hips[0],1,.04-.12*u];p.rotation[2]=-sign*.045*u;break;}
    case 'low_step':{
      const sign=side==='L'?1:-1;p.feet[side]=[sign*.14,.075+.21*u,.23*u];
      p.hips[1]+=.015*u;p.hips[2]+=.055*u;p.hips[0]-=sign*.025*u;
      p.hands.L[2]+=.10*u;p.hands.R[2]-=.10*u;break;}
    case 'single_leg_support':{
      p.hips[0]=-.045*u;p.feet.L=[.14,.075+.14*u,.14*u];p.hands.L=[.29,1.10,.02];
      p.hands.R=[-.29,1.12,.18];break;}
    case 'tandem_stance':p.feet.L=[.09,.075,.19];p.feet.R=[-.09,.075,-.19];p.hips[0]=.018*s;p.hands.L=[.29,1.08,.05];p.hands.R=[-.29,1.08,.05];break;
    case 'clock_reach':{
      const a=t*Math.PI/2;p.hips[0]=-.03;p.feet.L=[.14+.11*Math.sin(a),.075+.025*(1-Math.cos(a)),.07+.11*(1-Math.cos(a))];
      p.hands.L=[.29,1.15,.15];break;}
    case 'mini_lunge':case 'back_step':{
      p.feet.L=[.16,.075,.13];p.feet.R=[-.16,.075,-.16-.19*u];
      p.hips=[0,.91-.15*u,-.06*u];p.rotation[0]=.11*u;
      p.hands.L=[.29,1.08,.10];p.hands.R=[-.29,1.08,-.08];break;}
    case 'knee_push':case 'knee_plank':{
      quadruped();const a=id==='knee_plank'?.25:u;
      p.hips=[0,.41-.075*a,-.37+.065*a];p.rotation[0]=1.50+.06*a;
      both((b,sign)=>{p.hands[b]=[sign*.21,.055,.39];p.feet[b]=[sign*.14,.075,-.82];p.elbows[b]=[sign*.48,.22-.1*a,.28];});
      break;}
    case 'clamshell':{
      p.hips=[0,.17,0];p.rotation[2]=Math.PI/2;p.head=.12;
      p.feet.L=[-.43,.14,.24];p.feet.R=[-.43,.075,.24];
      p.knees.L=[-.28,.41+.17*u,.32];p.knees.R=[-.28,.055,.32];
      p.hands.L=[-.75,.17,-.16];p.hands.R=[-.55,.08,-.24];
      p.elbows.L=[-.75,.33,-.35];p.elbows.R=[-.55,.15,-.4];break;}
    case 'arm_swing':{
      both((a,sign)=>{p.hands[a]=[sign*.29,1.06+.04*Math.abs(s),.08+sign*.23*s];p.elbows[a]=[sign*.6,1.12,-.22];});
      p.spine=-.03*u;break;}
    case 'towel_row':case 'scap_squeeze':{
      both((a,sign)=>{p.hands[a]=[sign*.25,1.18,.37-.28*u];p.elbows[a]=[sign*.55,1.16,-.2-.14*u];});p.spine=-.04*u;break;}
    case 'wall_angels':case 'wall_slide':{
      both((a,sign)=>{p.hands[a]=[sign*(.40+.12*u),1.43+.24*u,-.16];p.elbows[a]=[sign*.66,1.38,-.34];});
      p.head=.03*u;break;}
    case 'ankle_circle':{
      const local=((t%4)+4)%4,lift=smooth(Math.min(1,local/.5,(4-local)/.5));
      const sign=side==='L'?1:-1;p.hips[0]=-.045*sign*lift;
      p.feet[side][1]+=.18*lift;p.feet[side][2]+=.12*lift;
      // Raise the foot, then articulate the ankle itself in two planes.
      // Both directions finish at neutral before changing legs.
      p.ankles={[side]:[.28*Math.sin(Math.PI*local)*lift,0,.22*(1-Math.cos(Math.PI*local))*lift*(local<2?1:-1)]};
      both(a=>p.hands[a][0]+=p.hips[0]);break;}
    case 'front_raise':case 'lateral_raise':case 'band_row':case 'biceps_band':{
      both((a,sign)=>{
        if(id==='front_raise')p.hands[a]=[sign*.23,1+.42*u,.03+.43*u];
        if(id==='lateral_raise')p.hands[a]=[sign*(.29+.39*u),1+.43*u,.03];
        if(id==='band_row')p.hands[a]=[sign*.24,1.25,.46-.39*u];
        if(id==='biceps_band')p.hands[a]=[sign*.28,1+.36*u,.03+.15*u];
        p.elbows[a]=[sign*.6,1.1,['band_row','lateral_raise'].includes(id)?-.3:0];
      });p.spine=-.035*u;p.head=.02*u;break;}
    case 'seated_march':case 'chair_knee_lift':case 'seated_leg_extend':case 'chair_row':case 'chair_punch':{
      seated();
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
      quadruped();
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
  if(pose.spineYaw)bones.get('spine03').rotateY(pose.spineYaw);
  if(pose.spineRoll)bones.get('spine03').rotateZ(pose.spineRoll);
  if(pose.headRoll)bones.get('head').rotateZ(pose.headRoll);
  if(pose.shoulderRoll)for(const side of ['L','R'])bones.get(key('clavicle.'+side)).rotateZ((side==='L'?1:-1)*pose.shoulderRoll);
  pivot.updateMatrixWorld(true);
  const errors=[];
  for(const side of ['L','R']){
    errors.push(solveLimb(bones,'upperleg01.'+side,'lowerleg01.'+side,'foot.'+side,pose.feet[side],pose.knees[side]));
    const foot=bones.get(key('foot.'+side));
    const angles=pose.ankles?.[side]||[pose.footPitch,0,0];
    const world=new THREE.Quaternion().setFromEuler(new THREE.Euler(...angles)).multiply(rest.get(key('foot.'+side)).world);
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
    if(pose.wrists?.[side]){const wrist=bones.get(key('wrist.'+side));
      const delta=new THREE.Quaternion().setFromEuler(new THREE.Euler(...pose.wrists[side]));
      wrist.quaternion.copy(wrist.parent.getWorldQuaternion(new THREE.Quaternion()).invert()).multiply(delta).multiply(rest.get(key('wrist.'+side)).world);wrist.updateMatrixWorld(true);}
  }return errors;
}
