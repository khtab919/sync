/* Actual SFU media, ephemeral synthetic users. Does not save screenshots or media. */
const {chromium}=require('playwright');const fs=require('fs');
(async()=>{
 const fixture=JSON.parse(fs.readFileSync(process.env.LIVE_SMOKE_CONFIG,'utf8'));
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage','--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 const contexts=[],pages=[];const base='http://127.0.0.1:8880';
 try{
 for(let i=0;i<14;i++)contexts.push(await browser.newContext({permissions:['microphone','camera']}));
 async function api(i,path,data){const response=await contexts[i].request[ data===undefined?'get':'post'](base+path,{data,headers:{Authorization:'Bearer '+fixture.tokens[i]}});const raw=await response.text();let value;try{value=JSON.parse(raw)}catch(e){throw Error('Fixture API '+path+' returned '+response.status()+': '+raw.slice(0,180))}return{status:response.status(),value}}
 async function open(i,broadcast){const admission=await api(i,'/api/live/'+broadcast.id+'/admission',{confirmed:true,price_cents:broadcast.price_cents});if(admission.status!==200)throw Error('Admission '+admission.status+' '+JSON.stringify(admission.value));const page=await contexts[i].newPage();page.on('pageerror',e=>process.stderr.write('live-pageerror: '+e.message+'\n'));await page.goto(base+'/health/');await page.evaluate(({url,ticket})=>{const form=document.createElement('form');form.method='POST';form.action=url;const input=document.createElement('input');input.name='ticket';input.value=ticket;form.appendChild(input);document.body.appendChild(form);form.submit()},admission.value);await page.waitForURL('**/room');await page.waitForFunction(()=>typeof room!=='undefined'&&room?.state==='connected',null,{timeout:40000});pages.push(page);return page}
 const created=await api(0,'/api/live',{title:'Synthetic paid live test',private:true,price_cents:100,max_viewers:12,minimum_seconds:30});if(created.status!==201)throw Error('Create '+JSON.stringify(created));const broadcast=created.value;
 const host=await open(0,broadcast);
 let active=false;for(let attempt=0;attempt<20;attempt++){const s=await api(0,'/api/live/'+broadcast.id);if(s.value.status==='active'){active=true;break}await new Promise(r=>setTimeout(r,500))}if(!active)throw Error('Host never transmitted');
 if((await api(13,'/api/live/'+broadcast.id+'/events')).status!==403)throw Error('Unpaid access allowed');
 const unauthorized=await contexts[13].request.get(base+'/livekit/rtc/validate?access_token=forged');if(unauthorized.status()!==403)throw Error('Gateway failed to reject forged token');
 for(let i=1;i<=12;i++)await open(i,broadcast);
 await new Promise(r=>setTimeout(r,4000));
 const stats=[];for(const page of pages.slice(1)){await page.waitForFunction(async()=>{for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track?.kind==='video'){const s=await pub.track.getRTCStatsReport();if([...s.values()].some(x=>x.type==='inbound-rtp'&&x.bytesReceived>0))return true}return false},null,{timeout:30000});stats.push(await page.evaluate(async()=>{let received=0;for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track){const s=await pub.track.getRTCStatsReport();for(const x of s.values())if(x.type==='inbound-rtp')received+=x.bytesReceived||0}return{state:room.state,bytesReceived:received}}))}
 const status=await api(0,'/api/live/'+broadcast.id);if(status.value.viewers!==12)throw Error('Incorrect real viewer count '+status.value.viewers);
 await pages[1].evaluate(()=>room.disconnect());await pages[1].waitForFunction(()=>room.state==='connected'&&!retrying,null,{timeout:45000});
 const before=await api(1,'/api/wallet');await api(1,'/api/live/'+broadcast.id+'/admission',{confirmed:true,price_cents:100});const after=await api(1,'/api/wallet');if(after.value.available_cents!==before.value.available_cents)throw Error('Reentry charged twice');
 await api(1,'/api/live/'+broadcast.id+'/events',{action:'comment',text:'Synthetic live comment'});const comments=await api(0,'/api/live/'+broadcast.id+'/events');if(!comments.value.comments.some(x=>x.text==='Synthetic live comment'))throw Error('Live comment missing');
 await api(0,'/api/live/'+broadcast.id,{action:'end'});for(const page of pages)await page.waitForFunction(()=>closed===true||room.state==='disconnected',null,{timeout:10000});
 const history=await api(1,'/api/financial-history');if(history.value.items.filter(x=>x.kind==='live_entry').length!==1)throw Error('Duplicate entry ledger');
 if((await api(1,'/api/live')).value.items.length)throw Error('Ended broadcast remains listed');
 console.log(JSON.stringify({actualSfuViewers:12,hostPublishing:true,allReceivedRtp:stats.every(x=>x.bytesReceived>0),viewerStats:stats,unpaidRejected:true,forgedLinkRejected:true,freshTokenReconnect:true,reentryNoDuplicateCharge:true,comments:true,endedDisconnectsAll:true,recordingServices:0,devices:'synthetic browser media; not a production capacity benchmark'}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
