import {build} from 'esbuild';
import {readFileSync,writeFileSync} from 'node:fs';
const catalog=[...readFileSync('app/src/main/java/com/fit50/app/WorkoutPlanEngine.kt','utf8').matchAll(/Ex\("([^"]+)","([^"]+)"/g)].map(m=>({id:m[1],name:m[2]}));
if(!catalog.length)throw Error('Exercise catalog is empty');
writeFileSync('tools/coach/exercise-catalog.json',JSON.stringify(catalog,null,2)+'\n');
const result=await build({entryPoints:['tools/coach/coach.js'],bundle:true,minify:true,format:'iife',target:'chrome80',loader:{'.glb':'binary'},write:false,legalComments:'eof'});
writeFileSync('app/src/main/assets/fit50/exercise-coach-3d.js',result.outputFiles[0].text.replace(/[\t ]+$/gm,''));
