# Exercise motion

`full-body.mjs` authors coordinated trajectories for 32 exercises, including
marching, squats, hinges, supported pushes, seated movements, arm raises,
bird dog, cat/cow, bridge and dead bug. These are authored animations, not
motion capture. Remaining exercises use the existing joint poses.

The pelvis moves independently of the camera. World-space two-bone IK keeps
support targets stationary and preserves limb lengths, including the rig's
intermediate twist bones. Quintic repetition timing provides a controlled
lowering, pause, return and rest. Alternating movements blend elbow/knee poles
with the movement to avoid snapping when sides switch. Stretches and holds
retain their gentle range.

The camera fits a sampled eight-second cycle on exercise or aspect change;
it does not follow the pelvis or change zoom during repetitions. `poseAt` and
`renderAt` allow deterministic inspection without starting extra animation loops.

From the repository root:

```
node tools/coach/test.mjs
node tools/coach/test-motion.mjs
node tools/coach/build.mjs
```

Motion tests load the actual GLB, sample all 32 cycles at 60 Hz, verify reachable
targets (under 5 mm), finite transforms, loop closure, joint continuity and
planted feet during squat/hinge movements. Android WebView QA also sampled all
66 exercises at 64 phases with no joint clipping and a stationary camera.
Android compilation, resource processing, asset merging and lint were checked
without assembling an APK.

The generated `exercise-catalog.json` maps every native exercise ID and Hebrew
name. `resolve-exercise.mjs` handles name-only saved workouts and rejects unknown
exercises instead of substituting a different demonstration. Ankle circles lift
one foot, rotate its ankle in two planes in both directions, then change legs.
The preview frames the lower legs for ankle circles and the upper body for
shoulder rolls; other exercises retain full-body framing.
