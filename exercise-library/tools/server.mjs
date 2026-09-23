import http from 'node:http';
import {randomBytes} from 'node:crypto';
import {readFileSync,writeFileSync} from 'node:fs';
import {spawn} from 'node:child_process';
import path from 'node:path';
import {root,library,state,validate} from './catalog.mjs';
const token=randomBytes(24).toString('hex');
let busy=false;
const generated=['exercise-library/active.json','app/src/main/java/com/fit50/app/LibraryExercises.kt','tools/coach/exercise-catalog.json','tools/coach/library-motions.json','app/src/main/assets/fit50/exercise-coach-3d.js'];
const build=()=>new Promise((resolve,reject)=>{const child=spawn(process.execPath,['tools/coach/build.mjs'],{cwd:root,windowsHide:true});let error='';child.stderr.on('data',s=>error+=s);child.on('error',reject);child.on('exit',code=>code===0?resolve():reject(Error(error||'Build failed')));});
await build();
const server=http.createServer(async(req,res)=>{
  const host=`127.0.0.1:${server.address().port}`,origin=`http://${host}`;
  const reply=(code,data)=>{res.writeHead(code,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify(data));};
  if(req.headers.host!==host){reply(403,{error:'Invalid host'});return;}
  const url=new URL(req.url,origin);
  try{
    if(req.method==='GET'&&url.pathname==='/api/library'){reply(200,{...state(),token});return;}
    if(req.method==='POST'&&url.pathname==='/api/activate'){
      if(req.headers.origin!==origin||req.headers['x-library-token']!==token||req.headers['content-type']!=='application/json'){reply(403,{error:'פתח את מנהל הספרייה מחדש'});return;}
      if(busy){reply(409,{error:'העדכון הקודם עדיין מתבצע'});return;}
      let body='';for await(const chunk of req){body+=chunk;if(body.length>4096){reply(413,{error:'Request too large'});return;}}
      if(busy){reply(409,{error:'העדכון הקודם עדיין מתבצע'});return;}
      const request=JSON.parse(body),current=state(),entry=current.all.find(e=>e.id===request.id);
      if(!entry||entry.kind==='base'||typeof request.enabled!=='boolean'){reply(400,{error:'תרגיל לא תקין או תרגיל בסיס'});return;}
      busy=true;
      const backups=new Map(generated.map(p=>[p,readFileSync(path.join(root,p))]));
      try{
        const active=request.enabled?[...new Set([...current.active,entry.id])]:current.active.filter(id=>id!==entry.id);
        validate(current.all,active);writeFileSync(path.join(library,'active.json'),JSON.stringify(active,null,2)+'\n');
        await build();reply(200,{...state(),token,message:'נשמר בפרויקט. השינוי ייכלל בגרסה הבאה שתיבנה מהקוד הזה.'});
      }catch(error){for(const [p,data]of backups)writeFileSync(path.join(root,p),data);throw error;}
      finally{busy=false;}return;
    }
    const files={'/':['exercise-library/index.html','text/html; charset=utf-8'],'/coach.js':['app/src/main/assets/fit50/exercise-coach-3d.js','text/javascript; charset=utf-8']};
    if(req.method==='GET'&&files[url.pathname]){const [p,type]=files[url.pathname];res.writeHead(200,{'Content-Type':type,'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(readFileSync(path.join(root,p)));return;}
    reply(404,{error:'Not found'});
  }catch(error){reply(500,{error:error.message});}
});
server.listen(Number(process.env.FIT50_LIBRARY_PORT||4174),'127.0.0.1',()=>console.log(`Fit50 exercise library: http://127.0.0.1:${server.address().port}`));
