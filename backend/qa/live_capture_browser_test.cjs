/* Real SDK capture/rendering without a reachable SFU. Synthetic camera, not Android. */
'use strict';
const {chromium}=require('playwright');const http=require('http');const fs=require('fs');const path=require('path');const assert=require('node:assert/strict');
(async()=>{
 const files=new Set(['live-media.js','livekit-client.umd.min.js']);
 const server=http.createServer((req,res)=>{const name=req.url.slice(1);if(files.has(name)){res.setHeader('Content-Type','application/javascript');res.end(fs.readFileSync(path.join(__dirname,'../core/static/core',name)))}else if(req.url==='/'){res.setHeader('Content-Type','text/html');res.end('<!doctype html><html><body></body></html>')}else{res.statusCode=404;res.end()}});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,...(process.env.CAPTURE_CHROMIUM?{executablePath:process.env.CAPTURE_CHROMIUM}:{}),args:['--no-sandbox','--disable-dev-shm-usage','--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream']});
  const context=await browser.newContext({permissions:['camera','microphone']});
  async function page(){const p=await context.newPage();await p.goto('http://127.0.0.1:'+server.address().port);await p.addScriptTag({url:'/livekit-client.umd.min.js'});await p.addScriptTag({url:'/live-media.js'});return p}
  const p=await page();const success=await p.evaluate(async()=>{
   const stages=[];let video;
   const tracks=await RelaxiaMedia.prepare({sdk:LivekitClient,video:true,render(track){video=track.attach();video.muted=true;video.autoplay=true;video.playsInline=true;document.body.append(video);return video},report(stage){stages.push(stage)}});
   const result={width:video.videoWidth,height:video.videoHeight,stages,kinds:tracks.map(t=>t.kind),roomCreated:false};tracks.forEach(t=>{t.detach();t.stop()});result.stopped=tracks.every(t=>t.mediaStreamTrack.readyState==='ended');return result;
  });assert.ok(success.width>0&&success.height>0);assert.deepEqual(success.kinds,['video','audio']);assert.equal(success.stopped,true);
  const denied=await page();const failure=await denied.evaluate(async()=>{
   const original=navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);let captured;
   navigator.mediaDevices.getUserMedia=constraints=>constraints.audio?Promise.reject(new DOMException('synthetic microphone denial','NotAllowedError')):original(constraints);
   try{await RelaxiaMedia.prepare({sdk:LivekitClient,video:true,render(track){captured=track;const v=track.attach();v.muted=true;document.body.append(v);return v}});return null}catch(e){return{stage:e.stage,reason:e.reason,cameraStopped:captured.mediaStreamTrack.readyState==='ended'}}
  });assert.deepEqual(failure,{stage:'microphone_capture',reason:'NotAllowedError',cameraStopped:true});
  console.log(JSON.stringify({sdkCaptureWithoutSfu:success,microphoneDenial:failure,device:'synthetic Chromium, not physical Android'}));
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
