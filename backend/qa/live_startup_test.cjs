/* Failure-path tests with the shipped template and SDK. Synthetic devices only. */
'use strict';
const {chromium}=require('playwright');
const {execFileSync}=require('node:child_process');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..');
function html(mode){
 return execFileSync('python',['-c',`import django,json;django.setup()
from django.template.loader import render_to_string
config={'id':'test-room','owner':True,'ownerName':'Test host','ownerId':1,'userName':'Test host','mode':'${mode}','token':'test-only','control':'test-only','url':'ws://127.0.0.1:1','maxSpeakers':1,'title':'Test'}
print(render_to_string('core/live_room.html',{'config':config,'nonce':'test-only','theme':'dark'}))`],{cwd:root,env:{...process.env,DJANGO_SETTINGS_MODULE:'config.settings',DJANGO_SECRET_KEY:'test-only',POSTGRES_PASSWORD:'test-only',LAYALI_TESTING:'1'},encoding:'utf8'});
}
(async()=>{
 const pages={audio:html('audio'),video:html('video')};
 const server=http.createServer((req,res)=>{
  const url=new URL(req.url,'http://localhost');
  if(url.pathname.startsWith('/static/core/')){
   const name=path.basename(url.pathname);
   if(!['live-stage.js','live-media.js','live-stage.css','livekit-client.umd.min.js'].includes(name)){res.writeHead(404).end();return}
   res.setHeader('Content-Type',name.endsWith('.css')?'text/css':'application/javascript');
   res.end(fs.readFileSync(path.join(root,'core/static/core',name)));return;
  }
  if(url.pathname.startsWith('/api/')){res.writeHead(503,{'Content-Type':'application/json'}).end('{"error":"live_server_unavailable"}');return}
  // Match the actual Django live_room Content-Security-Policy, which earlier
  // browser tests omitted even though it can suppress bootstrap scripts.
  res.setHeader('Content-Security-Policy',"default-src 'none'; script-src 'self' 'nonce-test-only'; style-src 'self' 'unsafe-inline'; connect-src 'self' ws://127.0.0.1:1; media-src 'self' blob:; img-src 'self' data: https:; frame-ancestors 'none'; base-uri 'none'");
  res.setHeader('Content-Type','text/html');res.end(pages[url.pathname==='/audio'?'audio':'video']);
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,...(process.env.CAPTURE_CHROMIUM?{executablePath:process.env.CAPTURE_CHROMIUM}:{}),args:['--no-sandbox','--disable-dev-shm-usage','--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream']});
  const origin='http://127.0.0.1:'+server.address().port;
  // Both kinds of rooms must finish the JS bootstrap even when the SFU
  // itself is deliberately unreachable. Catch CSP and parser failures.
  for(const mode of ['audio','video']){
   const room=await browser.newPage();
   const errors=[];
   room.on('pageerror',e=>errors.push(e.message));
   await room.goto(origin+'/'+mode);
   try{await room.waitForFunction(()=>window.relaxiaStageLoaded===true,{timeout:7000});}
   catch(e){throw new Error(mode+' live bootstrap failed: '+errors.slice(0,3).join(' | '));}
   assert.equal(await room.locator('#stage').isVisible(),true);
   await room.close();
  }
  // Regression for Android D06: the live DOM and scripts exist, yet
  // a zero-height viewport makes #stage have no visible geometry.
  // Run the *same repair expression* shipped in LiveScreens.kt, not a copy.
  const kotlin=fs.readFileSync(path.resolve(root,'../app/src/main/java/com/example/ui/screens/LiveScreens.kt'),'utf8');
  const source=kotlin.match(/val probeScript="""([\s\S]*?)"""\.trimIndent\(\)/);
  assert.ok(source,'Android WebView stage repair expression must exist');
  const repair=source[1].replaceAll('$heightPx','900').replaceAll('$widthPx','390');
  for(const mode of ['audio','video']){
   const p=await browser.newPage({viewport:{width:390,height:1}});
   await p.goto(origin+'/'+mode);
   await p.waitForFunction(()=>window.relaxiaStageLoaded===true);
   const before=await p.evaluate(()=>document.getElementById('stage').getBoundingClientRect().height);
   assert.ok(before<2,mode+': must reproduce a zero-height live stage');
   const flags=await p.evaluate(repair);
   assert.equal(flags,63,mode+': Android native measured-height recovery must restore all readiness flags');
   const after=await p.evaluate(()=>document.getElementById('stage').getBoundingClientRect().height);
   assert.ok(after>300,mode+': live stage must become visible after native height fallback');
   await p.close();
  }
  const noJS=await browser.newPage({javaScriptEnabled:false});await noJS.goto(origin+'/audio');
  assert.equal(await noJS.locator('#audioStage').isVisible(),true);
  assert.equal(await noJS.locator('#video').isVisible(),false);
  assert.match(await noJS.locator('#audioStage').innerText(),/Test host/);
  const legacy=await browser.newPage({javaScriptEnabled:false});
  await legacy.route('**/live-stage.css*',async route=>{
   const response=await route.fetch();
   // Simulate a WebView CSS parser discarding unsupported dynamic viewport units.
   const css=(await response.text()).replace(/(?:max-)?height:[0-9.]+dvh;?/g,'');
   await route.fulfill({response,body:css});
  });
  await legacy.goto(origin+'/audio');
  const bounds=await legacy.locator('#stage').boundingBox();
  assert.ok(bounds&&bounds.height>=600,'stage must retain viewport height without dvh support');
  assert.equal(await legacy.locator('#audioStage').isVisible(),true);
  for(const asset of ['livekit-client.umd.min.js','live-media.js','live-stage.js']){
   const p=await browser.newPage();await p.route('**/'+asset+'*',route=>route.abort());await p.goto(origin+'/audio');
   await p.waitForFunction(()=>document.getElementById('status').textContent.includes('تعذر تشغيل واجهة'));
   assert.equal(await p.locator('#audioStage').isVisible(),true);await p.close();
  }
  const p=await browser.newPage({permissions:['camera','microphone']});
  await p.addInitScript(()=>{
   window.captured=[];const get=navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
   navigator.mediaDevices.getUserMedia=async c=>{const stream=await get(c);window.captured.push(...stream.getTracks());return stream};
  });
  await p.goto(origin+'/video');
  await p.waitForFunction(()=>window.captured.length>=2&&window.captured.every(t=>t.readyState==='ended'));
  assert.equal(await p.locator('.tile').count(),0);
  console.log('PASS: audio stage without JS; missing SDK/media/stage errors; token failure releases camera and microphone. Synthetic Chromium only.');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
