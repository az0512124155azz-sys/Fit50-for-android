import {readFileSync} from 'node:fs';
for(const name of ['login','register','questionnaire','home','workout','settings','legal']){
  const html=readFileSync(`app/src/main/assets/fit50/${name}.html`,'utf8');
  let scripts=0;
  for(const match of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)){
    if(!match[1].trim())continue;
    new Function(match[1]);
    scripts++;
  }
  if(!scripts)throw Error(`${name} has no inline scripts`);
  console.log(`${name}: ${scripts} scripts parse`);
}
