(function () {
  'use strict';

  const V = (x, y, z) => ({ x, y, z });
  const copy = p => V(p.x, p.y, p.z);
  const clamp = (n, min, max) => Math.max(min, Math.min(max, n));

  const GROUPS = {
    march: new Set(['march', 'seated_march', 'standing_knee_drive', 'chair_knee_lift']),
    squat: new Set(['sit_to_stand', 'chair_squat', 'wall_sit_short', 'pillow_squeeze']),
    push: new Set(['wall_push', 'counter_push', 'knee_push', 'wall_press_iso', 'triceps_wall', 'wall_plank']),
    row: new Set(['band_row', 'towel_row', 'chair_row', 'scap_squeeze', 'biceps_band']),
    arms: new Set(['arm_swing', 'wall_angels', 'wall_slide', 'front_raise', 'lateral_raise', 'chair_punch', 'thoracic_open']),
    steps: new Set(['side_step', 'band_side_step', 'step_touch', 'low_step', 'line_walk', 'back_step', 'mini_lunge']),
    balance: new Set(['single_leg_support', 'tandem_stance', 'clock_reach', 'weight_shift']),
    floor: new Set(['knee_plank', 'bird_dog', 'cat_cow', 'child_pose']),
    supine: new Set(['glute_bridge', 'clamshell', 'dead_bug', 'pelvic_tilt']),
    stretch: new Set(['chest_wall_stretch', 'neck_side', 'hamstring_chair', 'calf_wall', 'hip_flexor_chair', 'figure_four_chair', 'seated_twist', 'side_reach']),
    ankle: new Set(['ankle_circle', 'heel_raise', 'toe_raise', 'wall_calf_raise']),
    hinge: new Set(['hip_circle', 'hip_hinge', 'good_morning']),
    breath: new Set(['deep_breath', 'box_breath'])
  };

  function standingPose() {
    return {
      pelvis: V(0, 1.52, 0), chest: V(0, 2.38, 0), neck: V(0, 2.82, 0), head: V(0, 3.17, 0),
      shoulderL: V(-0.55, 2.55, 0), elbowL: V(-0.67, 1.92, 0.02), wristL: V(-0.64, 1.32, 0),
      shoulderR: V(0.55, 2.55, 0), elbowR: V(0.67, 1.92, 0.02), wristR: V(0.64, 1.32, 0),
      hipL: V(-0.25, 1.48, 0), kneeL: V(-0.27, 0.76, 0), ankleL: V(-0.28, 0.08, 0),
      hipR: V(0.25, 1.48, 0), kneeR: V(0.27, 0.76, 0), ankleR: V(0.28, 0.08, 0)
    };
  }

  function shift(points, names, dx, dy, dz) {
    names.forEach(name => {
      points[name].x += dx;
      points[name].y += dy;
      points[name].z += dz;
    });
  }

  function poseFor(id, t) {
    const p = standingPose();
    const wave = Math.sin(t * Math.PI * 2);
    const pulse = (1 - Math.cos(t * Math.PI * 2)) / 2;
    const alternate = Math.sin(t * Math.PI * 2);

    if (id === 'shoulder_roll') {
      p.shoulderL.y += 0.13 * Math.sin(t * Math.PI * 2);
      p.shoulderR.y += 0.13 * Math.sin(t * Math.PI * 2 + Math.PI);
      p.shoulderL.z += 0.16 * Math.cos(t * Math.PI * 2);
      p.shoulderR.z += 0.16 * Math.cos(t * Math.PI * 2 + Math.PI);
    } else if (id === 'seated_leg_extend') {
      shift(p, ['pelvis', 'chest', 'neck', 'head', 'shoulderL', 'shoulderR', 'elbowL', 'elbowR', 'wristL', 'wristR', 'hipL', 'hipR'], 0, -0.35, 0);
      const left = Math.max(0, alternate);
      const right = Math.max(0, -alternate);
      p.ankleL.y += 0.55 * left; p.ankleL.z -= 0.62 * left;
      p.ankleR.y += 0.55 * right; p.ankleR.z -= 0.62 * right;
    } else if (GROUPS.march.has(id)) {
      const left = Math.max(0, alternate);
      const right = Math.max(0, -alternate);
      p.kneeL.y += 0.68 * left; p.ankleL.y += 0.34 * left; p.ankleL.z -= 0.35 * left;
      p.kneeR.y += 0.68 * right; p.ankleR.y += 0.34 * right; p.ankleR.z -= 0.35 * right;
      p.elbowL.z += 0.35 * alternate; p.wristL.z += 0.55 * alternate;
      p.elbowR.z -= 0.35 * alternate; p.wristR.z -= 0.55 * alternate;
      if (id === 'seated_march' || id === 'chair_knee_lift') shift(p, Object.keys(p), 0, -0.35, 0);
    } else if (GROUPS.squat.has(id)) {
      const down = id === 'wall_sit_short' ? 0.78 : 0.68 * pulse;
      shift(p, ['pelvis', 'chest', 'neck', 'head', 'shoulderL', 'shoulderR', 'elbowL', 'elbowR', 'wristL', 'wristR', 'hipL', 'hipR'], 0, -down, 0.15 * down);
      p.kneeL.x -= 0.15 * down; p.kneeR.x += 0.15 * down;
      p.kneeL.z -= 0.45 * down; p.kneeR.z -= 0.45 * down;
      p.wristL = V(-0.48, 2.2 - down, -0.62); p.wristR = V(0.48, 2.2 - down, -0.62);
    } else if (GROUPS.push.has(id)) {
      const press = id === 'wall_press_iso' || id === 'wall_plank' ? 0.35 : pulse;
      p.chest.z = -0.25 - 0.27 * press; p.pelvis.z = 0.12;
      p.shoulderL.z = p.shoulderR.z = -0.3;
      p.elbowL = V(-0.64, 2.3, -0.72 + 0.14 * press); p.elbowR = V(0.64, 2.3, -0.72 + 0.14 * press);
      p.wristL = V(-0.5, 2.34, -1.18); p.wristR = V(0.5, 2.34, -1.18);
    } else if (GROUPS.row.has(id)) {
      const pull = pulse;
      p.elbowL = V(-0.72, 2.28, 0.18 + 0.52 * pull); p.elbowR = V(0.72, 2.28, 0.18 + 0.52 * pull);
      p.wristL = V(-0.42, 2.22, -0.42 + 0.55 * pull); p.wristR = V(0.42, 2.22, -0.42 + 0.55 * pull);
    } else if (GROUPS.arms.has(id)) {
      const lift = pulse;
      if (id === 'front_raise' || id === 'chair_punch') {
        p.elbowL = V(-0.46, 2.43, -0.52 * lift); p.elbowR = V(0.46, 2.43, -0.52 * lift);
        p.wristL = V(-0.42, 2.32, -1.08 * lift); p.wristR = V(0.42, 2.32, -1.08 * lift);
      } else {
        p.elbowL = V(-0.72 - 0.38 * lift, 2.05 + 0.75 * lift, 0); p.elbowR = V(0.72 + 0.38 * lift, 2.05 + 0.75 * lift, 0);
        p.wristL = V(-0.68 - 0.44 * lift, 1.42 + 1.65 * lift, 0); p.wristR = V(0.68 + 0.44 * lift, 1.42 + 1.65 * lift, 0);
      }
    } else if (GROUPS.steps.has(id)) {
      const side = 0.42 * alternate;
      shift(p, ['pelvis', 'chest', 'neck', 'head', 'shoulderL', 'shoulderR'], side, 0, 0);
      p.ankleL.x -= 0.25 * Math.max(0, -alternate); p.ankleR.x += 0.25 * Math.max(0, alternate);
      p.kneeL.y += 0.16 * Math.max(0, alternate); p.kneeR.y += 0.16 * Math.max(0, -alternate);
      p.elbowL.z += 0.28 * alternate; p.elbowR.z -= 0.28 * alternate;
    } else if (GROUPS.balance.has(id)) {
      const sway = 0.08 * wave;
      shift(p, ['pelvis', 'chest', 'neck', 'head', 'shoulderL', 'shoulderR'], sway, 0, 0);
      if (id === 'single_leg_support' || id === 'clock_reach') {
        p.kneeR = V(0.48 + 0.22 * wave, 1.02, -0.15); p.ankleR = V(0.6 + 0.3 * wave, 0.62, -0.28);
        p.wristR = V(0.9 + 0.25 * wave, 2.12, -0.2);
      } else {
        p.ankleL.z = -0.38; p.ankleR.z = 0.38;
      }
    } else if (GROUPS.floor.has(id)) {
      return floorPose(id, t);
    } else if (GROUPS.supine.has(id)) {
      return supinePose(id, t);
    } else if (GROUPS.stretch.has(id)) {
      if (id === 'side_reach') {
        const lean = 0.26 * Math.sin(t * Math.PI);
        shift(p, ['chest', 'neck', 'head', 'shoulderL', 'shoulderR'], lean, 0, 0);
        p.elbowL = V(-0.45, 2.92, 0); p.wristL = V(-0.18, 3.42, 0);
      } else if (id === 'neck_side') {
        p.head.x = 0.12 * wave; p.head.y -= 0.04 * Math.abs(wave);
      } else if (id === 'seated_twist' || id === 'thoracic_open') {
        p.shoulderL.z = 0.32 * wave; p.shoulderR.z = -0.32 * wave;
        p.wristL = V(-0.2, 2.08, 0.46 * wave); p.wristR = V(0.2, 2.08, -0.46 * wave);
      } else {
        p.wristL = V(-1.0, 2.52, -0.18); p.elbowL = V(-0.75, 2.52, -0.05);
        p.chest.z = -0.12 * pulse;
      }
    } else if (GROUPS.ankle.has(id)) {
      const rise = id.includes('raise') ? 0.11 * pulse : 0;
      shift(p, Object.keys(p), 0, rise, 0);
      if (id === 'ankle_circle') {
        p.ankleR.x += 0.12 * Math.cos(t * Math.PI * 2); p.ankleR.y += 0.18 + 0.12 * Math.sin(t * Math.PI * 2);
      }
    } else if (GROUPS.hinge.has(id)) {
      p.chest.z -= 0.48 * pulse; p.neck.z -= 0.58 * pulse; p.head.z -= 0.64 * pulse;
      p.pelvis.z += 0.28 * pulse;
    } else if (GROUPS.breath.has(id)) {
      const open = 0.5 + 0.5 * Math.sin(t * Math.PI * 2 - Math.PI / 2);
      p.elbowL = V(-0.78 - 0.18 * open, 2.15 + 0.42 * open, 0); p.elbowR = V(0.78 + 0.18 * open, 2.15 + 0.42 * open, 0);
      p.wristL = V(-0.82 - 0.18 * open, 1.65 + 0.8 * open, 0); p.wristR = V(0.82 + 0.18 * open, 1.65 + 0.8 * open, 0);
      p.chest.y += 0.04 * open;
    } else if (id === 'wrist_mobility') {
      p.elbowL = V(-0.46, 2.24, -0.25); p.elbowR = V(0.46, 2.24, -0.25);
      p.wristL = V(-0.25 + 0.08 * wave, 2.1, -0.55); p.wristR = V(0.25 - 0.08 * wave, 2.1, -0.55);
    } else if (id === 'chin_tuck') {
      p.head.z = -0.1 * pulse;
    }
    return p;
  }

  function floorPose(id, t) {
    const pulse = (1 - Math.cos(t * Math.PI * 2)) / 2;
    const p = {
      head: V(1.32, 1.12, -0.05), neck: V(1.05, 1.03, 0), chest: V(0.62, 0.92, 0), pelvis: V(-0.2, 0.82, 0),
      shoulderL: V(0.72, 0.91, -0.34), elbowL: V(0.72, 0.48, -0.35), wristL: V(0.72, 0.08, -0.36),
      shoulderR: V(0.72, 0.91, 0.34), elbowR: V(0.72, 0.48, 0.35), wristR: V(0.72, 0.08, 0.36),
      hipL: V(-0.28, 0.79, -0.23), kneeL: V(-0.85, 0.34, -0.24), ankleL: V(-1.34, 0.08, -0.25),
      hipR: V(-0.28, 0.79, 0.23), kneeR: V(-0.85, 0.34, 0.24), ankleR: V(-1.34, 0.08, 0.25)
    };
    if (id === 'bird_dog') {
      p.wristL = V(1.58, 0.9 + 0.12 * pulse, -0.28); p.elbowL = V(1.15, 0.92, -0.3);
      p.kneeR = V(-0.88, 0.78, 0.22); p.ankleR = V(-1.55, 0.82 + 0.12 * pulse, 0.22);
    } else if (id === 'cat_cow') {
      p.chest.y += 0.18 * pulse; p.pelvis.y += 0.08 * pulse; p.head.y -= 0.08 * pulse;
    } else if (id === 'child_pose') {
      p.chest = V(0.1, 0.42, 0); p.neck = V(0.48, 0.38, 0); p.head = V(0.77, 0.35, 0);
      p.elbowL = V(0.82, 0.2, -0.32); p.wristL = V(1.25, 0.08, -0.32);
      p.elbowR = V(0.82, 0.2, 0.32); p.wristR = V(1.25, 0.08, 0.32);
    }
    return p;
  }

  function supinePose(id, t) {
    const pulse = (1 - Math.cos(t * Math.PI * 2)) / 2;
    const p = {
      head: V(-1.35, 0.32, 0), neck: V(-1.08, 0.27, 0), chest: V(-0.64, 0.24, 0), pelvis: V(0.05, 0.2, 0),
      shoulderL: V(-0.72, 0.22, -0.35), elbowL: V(-0.25, 0.13, -0.5), wristL: V(0.2, 0.08, -0.58),
      shoulderR: V(-0.72, 0.22, 0.35), elbowR: V(-0.25, 0.13, 0.5), wristR: V(0.2, 0.08, 0.58),
      hipL: V(0.06, 0.2, -0.24), kneeL: V(0.72, 0.75, -0.24), ankleL: V(1.25, 0.08, -0.24),
      hipR: V(0.06, 0.2, 0.24), kneeR: V(0.72, 0.75, 0.24), ankleR: V(1.25, 0.08, 0.24)
    };
    if (id === 'glute_bridge' || id === 'pelvic_tilt') {
      p.pelvis.y += (id === 'glute_bridge' ? 0.62 : 0.2) * pulse;
      p.hipL.y = p.hipR.y = p.pelvis.y;
      p.chest.y += 0.2 * pulse;
    } else if (id === 'dead_bug') {
      p.wristL = V(-0.35, 1.18 - 0.35 * pulse, -0.28); p.elbowL = V(-0.55, 0.75, -0.3);
      p.kneeR = V(0.45, 1.16, 0.25); p.ankleR = V(0.92, 0.78 - 0.45 * pulse, 0.25);
    } else if (id === 'clamshell') {
      p.kneeR.z += 0.55 * pulse; p.hipR.z += 0.12 * pulse;
    }
    return p;
  }

  class ExerciseCoach3D {
    constructor(canvas) {
      this.canvas = canvas;
      this.ctx = canvas.getContext('2d');
      this.exercise = 'shoulder_roll';
      this.start = performance.now();
      this.visible = true;
      this.resize = this.resize.bind(this);
      this.frame = this.frame.bind(this);
      if ('ResizeObserver' in window) {
        this.observer = new ResizeObserver(this.resize);
        this.observer.observe(canvas);
      } else {
        window.addEventListener('resize', this.resize);
      }
      document.addEventListener('visibilitychange', () => { this.visible = !document.hidden; });
      this.resize();
      requestAnimationFrame(this.frame);
    }

    setExercise(id) {
      this.exercise = id || 'deep_breath';
      this.start = performance.now();
      this.canvas.setAttribute('aria-label', 'Live 3D demonstration: ' + this.exercise.replace(/_/g, ' '));
    }

    resize() {
      const rect = this.canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const width = Math.max(1, Math.round(rect.width * dpr));
      const height = Math.max(1, Math.round(rect.height * dpr));
      if (this.canvas.width !== width || this.canvas.height !== height) {
        this.canvas.width = width; this.canvas.height = height;
      }
      this.dpr = dpr;
    }

    project(point, width, height) {
      const yaw = -0.48;
      const cos = Math.cos(yaw), sin = Math.sin(yaw);
      const x = point.x * cos + point.z * sin;
      const z = -point.x * sin + point.z * cos;
      const camera = 6.8;
      const f = width * 1.82;
      const scale = f / (camera - z);
      return { x: width / 2 + x * scale, y: height * 0.92 - point.y * scale, z, scale };
    }

    limb(a, b, width, color, projected) {
      const ctx = this.ctx;
      const pa = projected[a], pb = projected[b];
      const lineWidth = clamp(width * (pa.scale + pb.scale) / 50, 2.5, 12);
      ctx.lineCap = 'round';
      ctx.strokeStyle = 'rgba(8,36,27,.7)'; ctx.lineWidth = lineWidth + 2.2;
      ctx.beginPath(); ctx.moveTo(pa.x, pa.y); ctx.lineTo(pb.x, pb.y); ctx.stroke();
      const grad = ctx.createLinearGradient(pa.x, pa.y, pb.x, pb.y);
      grad.addColorStop(0, color); grad.addColorStop(1, this.shade(color));
      ctx.strokeStyle = grad; ctx.lineWidth = lineWidth;
      ctx.beginPath(); ctx.moveTo(pa.x, pa.y); ctx.lineTo(pb.x, pb.y); ctx.stroke();
    }

    shade(color) {
      return color === '#f0b08a' ? '#d98968' : color === '#d4e85c' ? '#8fae36' : '#1f5b48';
    }

    frame(now) {
      if (this.visible) this.draw(now);
      requestAnimationFrame(this.frame);
    }

    draw(now) {
      const ctx = this.ctx;
      const width = this.canvas.width / this.dpr;
      const height = this.canvas.height / this.dpr;
      ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
      ctx.clearRect(0, 0, width, height);
      const speed = GROUPS.breath.has(this.exercise) ? 0.12 : 0.32;
      const t = ((now - this.start) / 1000 * speed) % 1;
      const points = poseFor(this.exercise, t);
      const projected = {};
      Object.keys(points).forEach(key => { projected[key] = this.project(points[key], width, height); });

      const shadow = ctx.createRadialGradient(width / 2, height * 0.91, 2, width / 2, height * 0.91, width * 0.38);
      shadow.addColorStop(0, 'rgba(3,20,15,.42)'); shadow.addColorStop(1, 'rgba(3,20,15,0)');
      ctx.fillStyle = shadow; ctx.beginPath(); ctx.ellipse(width / 2, height * 0.91, width * 0.38, height * 0.055, 0, 0, Math.PI * 2); ctx.fill();

      const limbs = [
        ['hipL','kneeL',12,'#174b3a'], ['kneeL','ankleL',11,'#174b3a'], ['hipR','kneeR',12,'#205c47'], ['kneeR','ankleR',11,'#205c47'],
        ['shoulderL','elbowL',10,'#f0b08a'], ['elbowL','wristL',9,'#f0b08a'], ['shoulderR','elbowR',10,'#f0b08a'], ['elbowR','wristR',9,'#f0b08a']
      ].sort((a, b) => ((projected[a[0]].z + projected[a[1]].z) - (projected[b[0]].z + projected[b[1]].z)));
      limbs.forEach(l => this.limb(l[0], l[1], l[2], l[3], projected));

      this.limb('pelvis', 'chest', 22, '#d4e85c', projected);
      this.limb('chest', 'neck', 13, '#d4e85c', projected);
      this.limb('hipL', 'hipR', 15, '#174b3a', projected);
      this.limb('shoulderL', 'shoulderR', 18, '#d4e85c', projected);

      const head = projected.head;
      const radius = clamp(head.scale * 0.25, 5.5, 12);
      const headGrad = ctx.createRadialGradient(head.x - radius * .35, head.y - radius * .4, 1, head.x, head.y, radius);
      headGrad.addColorStop(0, '#ffd1b2'); headGrad.addColorStop(1, '#d98968');
      ctx.fillStyle = 'rgba(8,36,27,.7)'; ctx.beginPath(); ctx.arc(head.x, head.y, radius + 1.2, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = headGrad; ctx.beginPath(); ctx.arc(head.x, head.y, radius, 0, Math.PI * 2); ctx.fill();

      ['wristL','wristR'].forEach(name => {
        const q = projected[name]; ctx.fillStyle = '#f0b08a'; ctx.beginPath(); ctx.arc(q.x, q.y, 3.2, 0, Math.PI * 2); ctx.fill();
      });
      ['ankleL','ankleR'].forEach(name => {
        const q = projected[name]; ctx.strokeStyle = '#0a2e23'; ctx.lineWidth = 4; ctx.lineCap = 'round';
        ctx.beginPath(); ctx.moveTo(q.x - 3, q.y); ctx.lineTo(q.x + 5, q.y + 1); ctx.stroke();
      });
    }
  }

  window.Fit50ExerciseCoach3D = ExerciseCoach3D;
})();
