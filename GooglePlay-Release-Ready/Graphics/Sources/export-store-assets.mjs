// Faithful local exports of approved YourIPTV artwork; no remote assets or generated UI.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import crypto from 'node:crypto';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {png24,rgbPixels} from './png-format.mjs';

const sourceDir = path.dirname(fileURLToPath(import.meta.url));
const graphics = path.dirname(sourceDir);
const project = path.resolve(graphics, '..', '..');
const chrome = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const original = path.join(project, 'docs/branding/youriptv_icon_source.jpg');
const sha = data => crypto.createHash('sha256').update(data).digest('hex');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'YourIPTV-graphics-'));
const browser = spawn(chrome, ['--headless=new','--disable-gpu','--no-first-run',
    '--no-default-browser-check','--disable-background-networking','--disable-component-update',
    '--disable-sync','--disable-breakpad','--remote-debugging-port=0',
    '--remote-debugging-address=127.0.0.1','--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE localhost',
    '--user-data-dir='+profile,'about:blank'], {windowsHide:true, stdio:'ignore'});
const pending = new Map();
let seq=0;
let ws;
function command(method, params={}, sessionId) {
  return new Promise((resolve,reject) => {
    const id=++seq;
    pending.set(id,{resolve,reject});
    ws.send(JSON.stringify({id,method,params,...(sessionId?{sessionId}:{})}));
  });
}
function svg(width,height,content) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${content}</svg>`;
}
function logo(data,x,y,w,h) {
  return `<image href="data:image/png;base64,${data.toString('base64')}" x="${x}" y="${y}" width="${w}" height="${h}" preserveAspectRatio="xMidYMid meet"/>`;
}
async function render(markup,width,height,alpha,output,sessionId) {
  const expression = `(async()=>{
    const image = new Image();
    image.src=${JSON.stringify('data:image/svg+xml;base64,'+Buffer.from(markup).toString('base64'))};
    await image.decode();
    const canvas=document.createElement('canvas');canvas.width=${width};canvas.height=${height};
    const ctx=canvas.getContext('2d',{alpha:${alpha}});ctx.drawImage(image,0,0);
    return canvas.toDataURL('image/png').split(',')[1];
  })()`;
  const result=await command('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true},sessionId);
  if(result.exceptionDetails || !result.result.value) throw new Error('Native SVG export failed: '+(result.exceptionDetails?.exception?.description ?? result.exceptionDetails?.text ?? 'empty PNG'));
  const encoded=Buffer.from(result.result.value,'base64');
  const final=alpha?encoded:png24(encoded);
  if(!alpha&&sha(rgbPixels(encoded).rgb)!==sha(rgbPixels(final).rgb))throw new Error('PNG format conversion altered RGB pixels');
  fs.writeFileSync(output,final);
}
try {
  const portFile=path.join(profile,'DevToolsActivePort');
  for(let n=0;!fs.existsSync(portFile);n++) { if(n>200) throw new Error('Chrome startup timeout');await sleep(100); }
  const [port,endpoint]=fs.readFileSync(portFile,'utf8').trim().split(/\r?\n/);
  ws=new WebSocket(`ws://127.0.0.1:${port}${endpoint}`);
  await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});
  ws.addEventListener('message',event=>{
    const data=JSON.parse(event.data);if(!data.id)return;
    const item=pending.get(data.id);if(!item)return;pending.delete(data.id);
    data.error?item.reject(new Error(data.error.message)):item.resolve(data.result);
  });
  const {targetId}=await command('Target.createTarget',{url:'about:blank'});
  const {sessionId}=await command('Target.attachToTarget',{targetId,flatten:true});
  const data=fs.readFileSync(original);
  // Only this central logo rectangle is exported. The surrounding poster collage is excluded.
  const wordmark=svg(465,163,`<svg width="465" height="163" viewBox="153 305 465 163" overflow="hidden"><image href="data:image/jpeg;base64,${data.toString('base64')}" width="768" height="768"/></svg>`);
  const wordmarkPath=path.join(sourceDir,'Official-Wordmark.png');
  await render(wordmark,465,163,false,wordmarkPath,sessionId);
  const brand=fs.readFileSync(wordmarkPath);
  const backdrop=`<defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="#16191d"/><stop offset=".55" stop-color="#090b0e"/><stop offset="1" stop-color="#21130b"/></linearGradient><radialGradient id="glow"><stop stop-color="#da7a20" stop-opacity=".24"/><stop offset="1" stop-color="#d47b20" stop-opacity="0"/></radialGradient><linearGradient id="gold"><stop stop-color="#f4c76c"/><stop offset=".48" stop-color="#e69b31"/><stop offset="1" stop-color="#a54f1d"/></linearGradient></defs>`;
  // Owner correction: preserve the full existing icon, including its collage.
  // Only PNG serialization changes; every RGB pixel is verified below.
  const originalIcon=fs.readFileSync(path.join(sourceDir,'Original-App-Icon.png'));
  const icon=svg(512,512,logo(originalIcon,0,0,512,512));
  const feature=svg(1024,500,`${backdrop}<rect width="1024" height="500" fill="url(#bg)"/><ellipse cx="840" cy="230" rx="420" ry="310" fill="url(#glow)"/><path d="M628 0L1024 277M759 0L1024 185M542 500L1024 166" stroke="url(#gold)" stroke-width="1" opacity=".22"/>${logo(brand,52,27,360,126)}<text x="66" y="239" fill="#f2f0ed" font-family="Segoe UI,Arial,sans-serif" font-size="48" font-weight="600">Your playlists.</text><text x="66" y="300" fill="#e7af58" font-family="Segoe UI,Arial,sans-serif" font-size="48" font-weight="400">Your screen.</text><text x="68" y="377" fill="#c8c0b6" font-family="Segoe UI,Arial,sans-serif" font-size="25">Phone · Tablet · Android TV</text><rect x="645" y="147" width="307" height="184" rx="12" fill="#0a0c0f" stroke="url(#gold)" stroke-width="2"/><path d="M782 202L782 276L839 239Z" fill="url(#gold)"/><path d="M747 355H851M799 332V355" stroke="#b27b3e" stroke-width="2"/><path d="M66 435H348" stroke="url(#gold)" stroke-width="2"/></svg>`);
  const banner=svg(1280,720,`${backdrop}<rect width="1280" height="720" fill="url(#bg)"/><ellipse cx="850" cy="395" rx="650" ry="450" fill="url(#glow)"/><path d="M0 97L1280 574M0 132L1280 609" stroke="url(#gold)" stroke-width="1" opacity=".12"/>${logo(brand,257,211,766,269)}<path d="M390 520H890" stroke="url(#gold)" stroke-width="2"/><text x="640" y="577" text-anchor="middle" fill="#d8c8b1" font-family="Segoe UI,Arial,sans-serif" font-size="28">Your authorized playlists</text>`);
  const assets=[['App-Icon',512,512,true,icon],['Feature-Graphic',1024,500,false,feature],['Android-TV-Banner',1280,720,false,banner]];
  for(const [name,w,h,alpha,markup] of assets) {
    const validMarkup=markup.replace(/<\/svg><\/svg>$/, '</svg>');
    fs.writeFileSync(path.join(sourceDir,name+'.svg'),validMarkup,'utf8');
    await render(validMarkup,w,h,alpha,path.join(graphics,name+'.png'),sessionId);
    if(name==='App-Icon' && sha(rgbPixels(originalIcon).rgb)!==sha(rgbPixels(fs.readFileSync(path.join(graphics,name+'.png'))).rgb))throw new Error('Original icon pixels changed');
  }
  let appResources={};
  const provenancePath=path.join(graphics,'Artwork-Provenance.json');
  if(fs.existsSync(provenancePath))appResources=JSON.parse(fs.readFileSync(provenancePath,'utf8')).appResources??{};
  if(process.argv.includes('--app-resources')){
    for(const [density,size,foreground] of [['mdpi',48,108],['hdpi',72,162],['xhdpi',96,216],['xxhdpi',144,324],['xxxhdpi',192,432]]){
      const variants=[['ic_launcher.png',size],['ic_launcher_round.png',size],['ic_launcher_foreground.png',foreground]];
      for(const [name,dimension] of variants){
        const relative=`app/src/main/res/mipmap-${density}/${name}`;
        fs.copyFileSync(path.join(sourceDir,'Original-App-Icons',density,name),path.join(project,relative));
        const bytes=fs.readFileSync(path.join(project,relative));
        appResources[relative]={width:dimension,height:dimension,bitDepth:bytes[24],colorType:bytes[25],bytes:bytes.length,sha256:sha(bytes)};
      }
    }
    for(const [density,w,h] of [['xhdpi',320,180],['xxxhdpi',640,360]]){
      const relative=`app/src/main/res/drawable-${density}/banner.png`;
      await render(svg(w,h,`<g transform="scale(${w/1280})">${banner}</g>`),w,h,false,path.join(project,relative),sessionId);
      const bytes=fs.readFileSync(path.join(project,relative));
      appResources[relative]={width:w,height:h,bitDepth:bytes[24],colorType:bytes[25],bytes:bytes.length,sha256:sha(bytes)};
    }
  }
  let tvAdaptiveAliases={};
  if(fs.existsSync(provenancePath))tvAdaptiveAliases=JSON.parse(fs.readFileSync(provenancePath,'utf8')).tvAdaptiveAliases??{};
  if(process.argv.includes('--tv-icons')){
    for(const [density,size] of [['mdpi',48],['hdpi',72],['xhdpi',96],['xxhdpi',144],['xxxhdpi',192]]){
      const folder=`app/src/main/res/mipmap-television-${density}`;
      fs.mkdirSync(path.join(project,folder),{recursive:true});
      for(const name of ['ic_launcher.png','ic_launcher_round.png']){
        const relative=folder+'/'+name;
        fs.copyFileSync(path.join(sourceDir,'Original-App-Icons',density,name),path.join(project,relative));
        const bytes=fs.readFileSync(path.join(project,relative));
        appResources[relative]={width:size,height:size,bitDepth:bytes[24],colorType:bytes[25],bytes:bytes.length,sha256:sha(bytes)};
      }
    }
    const aliasFolder='app/src/main/res/mipmap-television-anydpi-v26';
    fs.mkdirSync(path.join(project,aliasFolder),{recursive:true});
    for(const name of ['ic_launcher.xml','ic_launcher_round.xml']){
      const from='app/src/main/res/mipmap-anydpi-v26/'+name;
      const relative=aliasFolder+'/'+name;
      fs.copyFileSync(path.join(project,from),path.join(project,relative));
      tvAdaptiveAliases[relative]={source:from,sha256:sha(fs.readFileSync(path.join(project,relative)))};
    }
  }
  const manifest={source:'docs/branding/youriptv_icon_source.jpg',sourceSha256:sha(data),wordmarkExtraction:{x:153,y:305,width:465,height:163,method:'Native SVG clip; no generated/retyped wordmark or poster imagery'},
    ownerIconCorrection:'Full original collage icon restored. Launcher PNGs copied byte for byte from independent baseline. Store icon preserves every original512x512 RGB pixel. Wordmark extraction applies only to supporting feature/banner graphics.',
    renderer:'Chrome headless SVG/canvas PNG export; local data URIs only',appResources,tvAdaptiveAliases,files:{}};
  for(const [name,w,h,alpha] of assets){const bytes=fs.readFileSync(path.join(graphics,name+'.png'));manifest.files[name+'.png']={width:w,height:h,bitDepth:bytes[24],colorType:bytes[25],bytes:bytes.length,sha256:sha(bytes)};}
  fs.writeFileSync(path.join(graphics,'Artwork-Provenance.json'),JSON.stringify(manifest,null,2)+'\n');
  console.log(JSON.stringify(manifest.files,null,2));
  await command('Browser.close');
} finally {
  if(ws)ws.close();
  browser.kill();
}
