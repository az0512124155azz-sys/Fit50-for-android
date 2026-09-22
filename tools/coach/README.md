# Fit50 human coach

The coach is a real skinned GLB human mesh, rendered with WebGL/Three.js.
MakeHuman anatomical geometry replaces the previous projected Canvas stick figure.
The generated model has a 163-bone rig, face, hands, separate clothing shells,
eyes and trainers. Exercise poses are authored in `coach.js`.

Rebuild from the repository root:

```
python tools/coach/fetch_source.py ../human-source
blender --factory-startup --background --python tools/coach/build_model.py -- ../human-source
npm ci --prefix tools/coach
node tools/coach/build.mjs
node tools/coach/test.mjs
```

`fit50-coach.glb` is embedded in the generated classic-script bundle so Android
file-origin restrictions cannot block model loading. No CDN or runtime fetch is
used. The GLB is retained as an editable/reusable source asset. The runtime limits
rendering to 30 fps and pixel ratio 2, and skips hidden documents. Users can enlarge
and pause the demonstration. An unavailable WebGL renderer hides the demonstration
without blocking the workout. Unknown exercises never display an unrelated pose.

These are authored illustrative exercise loops, not captured human video or
motion-capture data. A visual check of representative movements does not establish
biomechanical accuracy of every exercise.

See the model directory's `NOTICE.md` for upstream provenance and licensing.
