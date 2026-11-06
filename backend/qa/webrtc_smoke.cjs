/* Uses synthetic media and an isolated loopback-only test server, never live accounts. */
const {chromium}=require('playwright');
const fs=require('fs');
(async()=>{
 const fixture=JSON.parse(fs.readFileSync(process.env.CALL_SMOKE_CONFIG,'utf8'));
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage','--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 try {
 const contexts=[],pages=[];
 for(let i=0;i<2;i++)contexts.push(await browser.newContext({permissions:['microphone','camera']}));
 const base='http://127.0.0.1:8765';
 async function request(i,path,data){const response=await contexts[i].request.post(base+path,{data,headers:{Authorization:'Bearer '+fixture.tokens[i]}});if(!response.ok())throw Error('API '+path+' status '+response.status());return response.json()}
 const call=await request(0,'/api/calls',{peer_id:fixture.ids[1],mode:'video'});
 await request(1,'/api/calls/'+call.id,{action:'accept'});
 for(let i=0;i<2;i++){
  const room=await request(i,'/api/calls/'+call.id+'/ticket',{});
  const page=await contexts[i].newPage();
  page.on('pageerror',e=>process.stderr.write('pageerror: '+e.message+'\n'));
  await page.goto(base+'/health/');
  await page.evaluate(({id,ticket})=>{const form=document.createElement('form');form.method='POST';form.action='/api/calls/'+id+'/room';const input=document.createElement('input');input.name='ticket';input.value=ticket;form.appendChild(input);document.body.appendChild(form);form.submit();},{id:call.id,ticket:room.ticket});
  await page.waitForURL('**/room');pages.push(page);
 }
 for(const page of pages)await page.waitForFunction(()=>typeof pc!=='undefined'&&pc&&pc.connectionState==='connected',null,{timeout:40000});
 await new Promise(r=>setTimeout(r,2500));
 const stats=[];
 for(const page of pages)stats.push(await page.evaluate(async()=>{const s=await pc.getStats();const pair=[...s.values()].find(x=>x.type==='candidate-pair'&&x.state==='succeeded'&&x.nominated);return {state:pc.connectionState,remoteTracks:remote.srcObject.getTracks().length,localCandidate:pair?s.get(pair.localCandidateId)?.candidateType:null,remoteCandidate:pair?s.get(pair.remoteCandidateId)?.candidateType:null,outbound:[...s.values()].filter(x=>x.type==='outbound-rtp').map(x=>({kind:x.kind,bytesSent:x.bytesSent,packetsSent:x.packetsSent}))}}));
 for(const s of stats){
  if(s.localCandidate!=='relay'||s.remoteCandidate!=='relay')throw Error('TURN relay was not used');
  if(!s.outbound.some(x=>x.kind==='video'&&x.bytesSent>0)||!s.outbound.some(x=>x.kind==='audio'&&x.bytesSent>0)||s.remoteTracks!==2)throw Error('Missing actual audio/video RTP');
 }
 await pages[0].locator('#mute').click();if(await pages[0].evaluate(()=>stream.getAudioTracks()[0].enabled))throw Error('Mute failed');
 await pages[0].locator('#camera').click();if(await pages[0].evaluate(()=>stream.getVideoTracks()[0].enabled))throw Error('Camera disable failed');
 await request(0,'/api/calls/'+call.id,{action:'end'});
 for(const page of pages)await page.waitForFunction(()=>stream.getTracks().every(t=>t.readyState==='ended'),null,{timeout:6000});
 console.log(JSON.stringify({twoPeers:stats,mute:true,cameraToggle:true,hangupStopsBothStreams:true,devices:'synthetic browser devices',transport:'forced TURN relay'}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
