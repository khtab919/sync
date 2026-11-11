/* Actual SFU media, ephemeral synthetic users. Does not save screenshots or media. */
const {chromium}=require('playwright');const fs=require('fs');
(async()=>{
 const fixture=JSON.parse(fs.readFileSync(process.env.LIVE_SMOKE_CONFIG,'utf8'));
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage','--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 const contexts=[],pages=[];const previewDir=process.env.LIVE_PREVIEW_DIR;if(previewDir)fs.mkdirSync(previewDir,{recursive:true});
 async function preview(page,name){if(!previewDir)return;await page.setViewportSize({width:390,height:844});await page.bringToFront();await page.waitForFunction(()=>getComputedStyle(document.getElementById('stage')).position==='relative');await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await page.screenshot({path:previewDir+'/'+name+'.png'});if(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth))throw Error('Live UI overflow '+name)}
const base='http://127.0.0.1:8880';
 try{
 for(let i=0;i<14;i++)contexts.push(await browser.newContext({permissions:['microphone','camera']}));
 async function api(i,path,data){const response=await contexts[i].request[ data===undefined?'get':'post'](base+path,{data,headers:{Authorization:'Bearer '+fixture.tokens[i]}});const raw=await response.text();let value;try{value=JSON.parse(raw)}catch(e){throw Error('Fixture API '+path+' returned '+response.status()+': '+raw.slice(0,180))}return{status:response.status(),value}}
 async function open(i,broadcast,expectFailure=false){const admission=await api(i,'/api/live/'+broadcast.id+'/admission',{confirmed:true,price_cents:broadcast.price_cents});if(admission.status!==200)throw Error('Admission '+admission.status+' '+JSON.stringify(admission.value));const page=await contexts[i].newPage();page.on('pageerror',e=>process.stderr.write('live-pageerror: '+e.message+'\n'));await page.goto(base+'/health/');await page.evaluate(({url,ticket})=>{const form=document.createElement('form');form.method='POST';form.action=url;const input=document.createElement('input');input.name='ticket';input.value=ticket;form.appendChild(input);document.body.appendChild(form);form.submit()},admission.value);await page.waitForURL('**/room');if(expectFailure)await page.waitForFunction(()=>!document.getElementById('retryMedia').hidden,null,{timeout:20000});else await page.waitForFunction(()=>typeof room!=='undefined'&&room?.state==='connected',null,{timeout:40000});pages.push(page);return page}
 const created=await api(0,'/api/live',{title:'Synthetic paid live test',private:true,price_cents:100,max_viewers:12,minimum_seconds:30,max_speakers:12,gifts_enabled:true});if(created.status!==201)throw Error('Create '+JSON.stringify(created));const broadcast=created.value;
 const host=await open(0,broadcast);
 await host.waitForFunction(()=>mediaReady&&[...document.querySelectorAll('#video video')].some(v=>v.readyState>=2&&v.videoWidth>0&&v.videoHeight>0),null,{timeout:20000});
 let active=false;for(let attempt=0;attempt<20;attempt++){const s=await api(0,'/api/live/'+broadcast.id);if(s.value.status==='active'){active=true;break}await new Promise(r=>setTimeout(r,500))}if(!active)throw Error('Host never transmitted');
 if((await api(13,'/api/live/'+broadcast.id+'/events')).status!==403)throw Error('Unpaid access allowed');
 const unauthorized=await contexts[13].request.get(base+'/livekit/rtc/validate?access_token=forged');if(unauthorized.status()!==403)throw Error('Gateway failed to reject forged token');
 for(let i=1;i<=12;i++)await open(i,broadcast);
 await new Promise(r=>setTimeout(r,4000));
 const stats=[];for(const page of pages.slice(1)){await page.waitForFunction(async()=>{for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track?.kind==='video'){const s=await pub.track.getRTCStatsReport();if([...s.values()].some(x=>x.type==='inbound-rtp'&&x.bytesReceived>0))return true}return false},null,{timeout:30000});stats.push(await page.evaluate(async()=>{let received=0;for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track){const s=await pub.track.getRTCStatsReport();for(const x of s.values())if(x.type==='inbound-rtp')received+=x.bytesReceived||0}return{state:room.state,bytesReceived:received}}))}
 await preview(pages[1],'01-paid-live');
 const status=await api(0,'/api/live/'+broadcast.id);if(status.value.viewers!==12)throw Error('Incorrect real viewer count '+status.value.viewers);
 await pages[1].evaluate(()=>room.disconnect());await pages[1].waitForFunction(()=>room.state==='connected'&&!retrying,null,{timeout:45000});
 const before=await api(1,'/api/wallet');await api(1,'/api/live/'+broadcast.id+'/admission',{confirmed:true,price_cents:100});const after=await api(1,'/api/wallet');if(after.value.available_cents!==before.value.available_cents)throw Error('Reentry charged twice');
 await api(1,'/api/live/'+broadcast.id+'/events',{action:'comment',text:'Synthetic live comment'});const comments=await api(0,'/api/live/'+broadcast.id+'/events');if(!comments.value.comments.some(x=>x.text==='Synthetic live comment'))throw Error('Live comment missing');
 // Promote eleven audience members only after server approval and their own media action.
 for(let i=1;i<=11;i++){
  const requested=await api(i,'/api/live/'+broadcast.id+'/events',{action:'seat_request',mode:i===1?'video':'audio'});if(requested.status!==200)throw Error('Guest request '+JSON.stringify(requested));
  const approved=await api(0,'/api/live/'+broadcast.id+'/events',{action:'seat_decision',user_id:fixture.ids[i],approve:true});if(approved.status!==200)throw Error('Guest approval '+JSON.stringify(approved));
  await pages[i].evaluate(async()=>{await refresh();await activateSeat()});
  await pages[i].waitForFunction(()=>speaking&&room.localParticipant.audioTrackPublications.size>0,null,{timeout:20000});
 }
 await host.waitForFunction(async()=>{let audio=0;for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track?.kind==='audio'){const stats=await pub.track.getRTCStatsReport();if([...stats.values()].some(s=>s.type==='inbound-rtp'&&s.bytesReceived>0))audio++}return audio>=11},null,{timeout:40000});
 const stage=await api(0,'/api/live/'+broadcast.id+'/events');if(stage.value.speakers.length!==12)throw Error('Twelve speaker stage failed');
 const teams=[fixture.ids.slice(0,6),fixture.ids.slice(6,12)];const pk=await api(0,'/api/live/'+broadcast.id+'/events',{action:'pk_start',teams,duration:60});if(pk.status!==200)throw Error('PK '+JSON.stringify(pk));
 const gift={action:'gift',gift_code:'rose',price_cents:100,recipient_id:fixture.ids[1],confirmed:true,request_key:require('crypto').randomUUID()};
 await host.evaluate(()=>refresh());if(!await host.evaluate(()=>[...document.querySelectorAll('.tile small')].some(el=>el.textContent==='sfu_test_1')))throw Error('Guest tile shows incorrect name');await preview(host,'02-group-pk');await pages[12].evaluate(()=>{giftSheet()});await preview(pages[12],'03-gift-catalog');await pages[12].evaluate(()=>$('sheet').close());
 const balanceBefore=(await api(12,'/api/wallet')).value.available_cents;for(let i=0;i<2;i++){const r=await api(12,'/api/live/'+broadcast.id+'/events',gift);if(r.status!==200)throw Error('Gift '+JSON.stringify(r))}
 if((await api(12,'/api/wallet')).value.available_cents!==balanceBefore-100)throw Error('Gift duplicate debit');
 if((await api(0,'/api/live/'+broadcast.id+'/events')).value.pk.scores[0]!==100)throw Error('PK score mismatch');
 const priorBytes=await pages[2].evaluate(async()=>{let n=0;for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track){for(const r of (await pub.track.getRTCStatsReport()).values())if(r.type==='inbound-rtp')n+=r.bytesReceived||0}return n});
 await contexts[2].setOffline(true);await new Promise(r=>setTimeout(r,3500));await contexts[2].setOffline(false);await pages[2].waitForFunction(async prior=>{let n=0;for(const p of room.remoteParticipants.values())for(const pub of p.trackPublications.values())if(pub.track){for(const r of (await pub.track.getRTCStatsReport()).values())if(r.type==='inbound-rtp')n+=r.bytesReceived||0}return room.state==='connected'&&n>prior},priorBytes,{timeout:40000});
 // Removing a guest must stop publishing and only permit an audience reconnect.
 const revoked=await api(0,'/api/live/'+broadcast.id+'/events',{action:'seat_decision',user_id:fixture.ids[1],approve:false});if(revoked.status!==200)throw Error('Revoke failed');
 await pages[1].waitForFunction(()=>!speaking&&room.state==='connected',null,{timeout:40000});
 await api(0,'/api/live/'+broadcast.id,{action:'end'});for(const page of pages)await page.waitForFunction(()=>closed===true||room.state==='disconnected',null,{timeout:10000});
 const history=await api(1,'/api/financial-history');if(history.value.items.filter(x=>x.kind==='live_entry').length!==1)throw Error('Duplicate entry ledger');
 if((await api(1,'/api/live')).value.items.length)throw Error('Ended broadcast remains listed');
 // A separate audio-only stage never captures a camera track.
 for(const p of pages)await p.close();pages.length=0;
 const audioCreated=await api(0,'/api/live',{title:'Synthetic audio stage',mode:'audio',max_speakers:15,max_viewers:25,minimum_seconds:30});if(audioCreated.status!==201)throw Error('Audio create failed');
 const audioRoom=audioCreated.value,audioHost=await open(0,audioRoom);
 for(let i=0;i<20;i++){if((await api(0,'/api/live/'+audioRoom.id)).value.status==='active')break;await new Promise(r=>setTimeout(r,500))}
 const audioGuest=await open(1,audioRoom);
 await api(1,'/api/live/'+audioRoom.id+'/events',{action:'seat_request',mode:'audio'});await api(0,'/api/live/'+audioRoom.id+'/events',{action:'seat_decision',user_id:fixture.ids[1],approve:true});await audioGuest.evaluate(async()=>{await refresh();await activateSeat()});
 await audioHost.waitForFunction(()=>room.remoteParticipants.size>0&&[...room.remoteParticipants.values()].some(p=>p.audioTrackPublications.size>0),null,{timeout:20000});
 for(const p of [audioHost,audioGuest])if(await p.evaluate(()=>room.localParticipant.videoTrackPublications.size!==0))throw Error('Audio stage captured camera');
 await audioHost.evaluate(()=>refresh());if(await audioHost.locator('.audio-seat').count()!==15)throw Error('Audio room must show fifteen seats');await preview(audioHost,'04-audio-stage');await audioHost.evaluate(()=>document.documentElement.dataset.theme='light');await preview(audioHost,'05-audio-light');if(await audioHost.evaluate(()=>getComputedStyle(document.getElementById('stage')).backgroundColor)!=='rgb(244, 248, 250)')throw Error('Audio day theme background incorrect');
 await api(0,'/api/live/'+audioRoom.id,{action:'end'});
 // Explicit denial must produce a recoverable camera error, without joining/publishing.
 await contexts[0].addInitScript(()=>{navigator.mediaDevices.getUserMedia=async()=>{throw new DOMException('Denied by regression test','NotAllowedError')}});
 const deniedCreated=await api(0,'/api/live',{title:'Synthetic denied camera',max_viewers:1,minimum_seconds:30});if(deniedCreated.status!==201)throw Error('Denied fixture create failed');
 const denied=await open(0,deniedCreated.value,true);
 if(!await denied.evaluate(()=>mediaFault&&typeof room==='undefined'&&document.getElementById('status').textContent.includes('الكاميرا')))throw Error('Camera failure remained silent or attempted signaling');
 await api(0,'/api/live/'+deniedCreated.value.id,{action:'end'});
 console.log(JSON.stringify({cameraDenialExplicit:true,audioOnlyNoCamera:true,actualSpeakers:12,actualGuestAudioTracksAtHost:11,guestVideoPublished:true,pkFromSettledGift:true,giftDebitOnce:true,offlineRecovery:true,guestRevocation:true,actualSfuViewers:12,hostPublishing:true,hostDecodedPreview:true,allReceivedRtp:stats.every(x=>x.bytesReceived>0),viewerStats:stats,unpaidRejected:true,forgedLinkRejected:true,freshTokenReconnect:true,reentryNoDuplicateCharge:true,comments:true,endedDisconnectsAll:true,recordingServices:0,devices:'synthetic browser media; not a production capacity benchmark'}));
 }finally{await browser.close()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
