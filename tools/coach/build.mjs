import {build} from 'esbuild';
import {writeFileSync} from 'node:fs';
import {syncLibrary} from '../../exercise-library/tools/catalog.mjs';
syncLibrary();
const result=await build({entryPoints:['tools/coach/coach.js'],bundle:true,minify:true,format:'iife',target:'chrome80',loader:{'.glb':'binary'},write:false,legalComments:'eof'});
writeFileSync('app/src/main/assets/fit50/exercise-coach-3d.js',result.outputFiles[0].text.replace(/[\t ]+$/gm,''));
