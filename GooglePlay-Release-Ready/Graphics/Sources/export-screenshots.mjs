// Export real captures only. TV pixels remain unaltered; phone framing never crops/scales UI.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {png24,rgbPixels} from './png-format.mjs';
const sources=path.dirname(fileURLToPath(import.meta.url));
const graphics=path.dirname(sources);
const manifestPath=process.argv[2];
if(!manifestPath)throw new Error('Usage: node export-screenshots.mjs <reviewed-captures.json>');
const checkOnly=process.argv.includes('--check-only');
const inputs=JSON.parse(fs.readFileSync(manifestPath,'utf8')).captures;
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const profile=fs.mkdtempSync(path.join(os.tmpdir(),'YourIPTV-capture-export-'));
const browser=spawn('C:/Program Files/Google/Chrome/Application/chrome.exe',[
  '--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check',
  '--disable-background-networking','--disable-component-update','--disable-sync',
  '--disable-breakpad','--remote-debugging-port=0','--remote-debugging-address=127.0.0.1',
  '--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE localhost','--user-data-dir='+profile,'about:blank'],
  {windowsHide:true,stdio:'ignore'});
let ws,seq=0;const pending=new Map();
const command=(method,params={},sessionId)=>new Promise((resolve,reject)=>{
  const id=++seq;pending.set(id,{resolve,reject});
  ws.send(JSON.stringify({id,method,params,...(sessionId?{sessionId}:{})}));
});
try{
  const portFile=path.join(profile,'DevToolsActivePort');
  for(let i=0;!fs.existsSync(portFile);i++){if(i>200)throw new Error('Chrome startup timeout');await sleep(100);}
  const [port,endpoint]=fs.readFileSync(portFile,'utf8').trim().split(/\r?\n/);
  ws=new WebSocket(`ws://127.0.0.1:${port}${endpoint}`);
  await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});
  ws.addEventListener('message',event=>{const msg=JSON.parse(event.data);if(!msg.id)return;const item=pending.get(msg.id);if(!item)return;pending.delete(msg.id);msg.error?item.reject(new Error(msg.error.message)):item.resolve(msg.result);});
  const {targetId}=await command('Target.createTarget',{url:'about:blank'});
  const {sessionId}=await command('Target.attachToTarget',{targetId,flatten:true});
  const report={method:'Actual app screenshots only; no generated/reconstructed UI, text overlays, UI crop or stretch.',captures:[]};
  for(const item of inputs){
    if(!['phone','tv'].includes(item.device))throw new Error('Unsupported capture device');
    if(path.basename(item.filename)!==item.filename||!item.filename.endsWith('.png'))throw new Error('Unsafe capture filename');
    if(typeof item.alt!=='string'||!item.alt.trim()||item.alt.length>140)throw new Error('Capture alt text must be1-140characters');
    const raw=fs.readFileSync(item.source),source=rgbPixels(raw);
    const rawDir=path.join(sources,'Raw-Captures',item.device);
    const rawTarget=path.join(rawDir,item.filename);
    if(!checkOnly){fs.mkdirSync(rawDir,{recursive:true});fs.copyFileSync(item.source,rawTarget);}
    let width=source.width,height=source.height,x=0,y=0,encoded=raw;
    if(item.device==='tv'){
      if(width*9!==height*16)throw new Error('TV capture must be original high-resolution16:9');
      if(width<1920||height<1080)throw new Error('TV capture resolution too small');
    }else if(Math.max(width,height)>2*Math.min(width,height)){
      // Exact16:9/9:16 presentation meets the promotional recommendation too.
      const landscape=width>height;
      const unit=Math.ceil(Math.max((landscape?width:height)/16,(landscape?height:width)/9)/10)*10;
      width=(landscape?16:9)*unit;height=(landscape?9:16)*unit;
      x=Math.floor((width-source.width)/2);y=Math.floor((height-source.height)/2);
      const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}"><rect width="100%" height="100%" fill="#0e1116"/><image href="data:image/png;base64,${raw.toString('base64')}" x="${x}" y="${y}" width="${source.width}" height="${source.height}" image-rendering="pixelated"/></svg>`;
      const expression=`(async()=>{const im=new Image();im.src=${JSON.stringify('data:image/svg+xml;base64,'+Buffer.from(svg).toString('base64'))};await im.decode();const c=document.createElement('canvas');c.width=${width};c.height=${height};c.getContext('2d',{alpha:false}).drawImage(im,0,0);return c.toDataURL('image/png').split(',')[1];})()`;
      const rendered=await command('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true},sessionId);
      if(rendered.exceptionDetails||!rendered.result.value)throw new Error('Capture export failed');
      encoded=Buffer.from(rendered.result.value,'base64');
    }
    const normalized=encoded[25]===2?encoded:png24(encoded);
    const final=rgbPixels(normalized),region=Buffer.alloc(source.rgb.length);
    for(let row=0;row<source.height;row++)final.rgb.copy(region,row*source.width*3,((row+y)*width+x)*3,((row+y)*width+x+source.width)*3);
    if(!region.equals(source.rgb))throw new Error('Export modified actual UI pixels');
    if(Math.max(width,height)>3840||Math.min(width,height)<320||Math.max(width,height)>2*Math.min(width,height))throw new Error('Store screenshot dimensions invalid');
    const relative=(item.device==='phone'?'Phone-Screenshots':'Android-TV-Screenshots')+'/'+item.filename;
    const out=path.join(graphics,relative);
    if(!checkOnly){fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,normalized);}
    report.captures.push({device:item.device,role:item.role,alt:item.alt,output:relative,width,height,bitDepth:normalized[24],colorType:normalized[25],bytes:normalized.length,sha256:sha(normalized),
      source:{path:path.relative(graphics,rawTarget).replaceAll('\\','/'),width:source.width,height:source.height,sha256:sha(raw),rgbPixelsSha256:sha(source.rgb)},
      formatting:{addedCanvasPixelsOnly:x!==0||y!==0,offsetX:x,offsetY:y,uiPixelScale:1,sourceRgbPixelsExactlyPreserved:true},captureProvenance:item.provenance});
  }
  if(!checkOnly){
    fs.writeFileSync(path.join(graphics,'Screenshots-Provenance.json'),JSON.stringify(report,null,2)+'\n');
    const alt=['App-Icon.png: YourIPTV silver and gold wordmark on a dark background.',
      'Feature-Graphic.png: YourIPTV authorized playlist player for phones, tablets and Android TV.',
      'Android-TV-Banner.png: YourIPTV wordmark and Your authorized playlists tagline on a dark gold background.',
      ...report.captures.map(item=>item.output+': '+item.alt)];
    fs.writeFileSync(path.join(graphics,'Alt-Text.txt'),alt.join('\n')+'\n','utf8');
  }
  console.log(JSON.stringify({checkOnly,captures:report.captures.length,uiPixelsExactlyPreserved:true,files:report.captures.map(item=>({file:item.output,width:item.width,height:item.height,colorType:item.colorType}))},null,2));
  await command('Browser.close');
}finally{if(ws)ws.close();browser.kill();}
