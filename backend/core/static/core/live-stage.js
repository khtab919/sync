'use strict';
if(typeof LivekitClient==='undefined'||typeof RelaxiaMedia==='undefined')throw Error('live_dependencies_unavailable');
const config=JSON.parse(document.getElementById('config').textContent),$=id=>document.getElementById(id);
const {Room,RoomEvent,Track,ConnectionState}=LivekitClient;
let room,closed=false,muted=false,speakerMuted=false,cameraOff=false,facing='user',commentsVisible=true,lastComments='',leaving=false,retrying=false,polling=false;
let activeSpeakerIds=new Set(),audioSeatSignature='',reconnectSince=0,controlFailures=0;
function updateRoomViewport(){
 const height=window.visualViewport?.height||window.innerHeight;
 // Preserve truly tiny viewports so Android's native stage-height recovery can detect and repair them.
 document.documentElement.style.setProperty('--room-viewport-height',Math.max(1,Math.round(height))+'px');
}
updateRoomViewport();
window.visualViewport?.addEventListener('resize',updateRoomViewport,{passive:true});
window.addEventListener('resize',updateRoomViewport,{passive:true});
let state={},speaking=false,activating=false,latestGift=0,haveGiftBaseline=false,giftQueue=[],effectBusy=false,giftKey=null,giftPending=null,quality=720,serverOffset=0;
let preparedTracks=[],mediaFault=false,mediaReady=false;
const path='/api/live/'+config.id+'/control';
document.body.classList.toggle('audio-mode',config.mode==='audio');
$('name').textContent=config.ownerName;$('title').textContent='ID: '+(config.ownerPublicId||'—');document.title='Relaxia · '+config.title;
if(config.ownerAvatar&&/^https:\/\//.test(config.ownerAvatar)){$('ownerAvatar').onload=()=>$('ownerAvatar').hidden=false;$('ownerAvatar').src=config.ownerAvatar}
$('profile').onclick=()=>{if(!config.owner&&window.LayaliLive?.canOpenProfile()){window.LayaliLive.openProfile(config.ownerId);return;}const box=sheet('ملف صاحب الغرفة');note(box,config.ownerName);note(box,'ID: '+(config.ownerPublicId||'—'));note(box,config.title)};
async function api(action){const abort=new AbortController(),timer=setTimeout(()=>abort.abort(),8000);try{const r=await fetch(path,{method:action?'POST':'GET',headers:{'Content-Type':'application/json','X-Live-Control':config.control},body:action?JSON.stringify(action):undefined,signal:abort.signal,cache:'no-store'});const value=await r.json();if(!r.ok)throw Error(value.error||'network');return value}finally{clearTimeout(timer)}}
function diagnostic(stage,outcome,reason=''){api({action:'media_diagnostic',stage,outcome,reason,build:'media-1'}).catch(()=>{})}
function mediaMessage(e){
 mediaFault=true;mediaReady=false;$('connection').classList.add('media-fault');const labels={camera_capture:'تعذر تشغيل الكاميرا',camera_preview:'الكاميرا لا تعرض صورة',microphone_capture:'تعذر تشغيل الميكروفون',media_publish:'تعذر نشر الصوت أو الصورة',room_video:'نُشر الفيديو لكن لم تظهر صورته داخل الغرفة'};
 const reasons={NotAllowedError:'تحقق من أذونات التطبيق والكاميرا في Android.',NotReadableError:'قد تكون الكاميرا أو الميكروفون مستخدمة في تطبيق آخر.',NotFoundError:'لم يعثر الجهاز على الكاميرا أو الميكروفون.'};
 $('status').textContent=(labels[e.stage]||'تعذر تجهيز البث')+' · '+(reasons[e.reason]||'أعد المحاولة أو ارجع وافتح البث مجددًا.');$('retryMedia').hidden=false;diagnostic(e.stage,'failed',e.reason);
}
function message(e){if(e.name==='MediaFailure'){mediaMessage(e);return}$('status').textContent=({'insufficient_balance':'رصيدك لا يكفي. اشحن من المحفظة قبل إرسال الهدية.','broadcast_ended':'انتهى البث','stage_full':'اكتملت مقاعد الضيوف','pending_gift_retry':'أعد محاولة الهدية السابقة أولًا لتجنب خصم غير واضح.','media_permission_required':'لم تمنح إذن الميكروفون أو الكاميرا. يمكنك الاستمرار بالمشاهدة.','stage_unavailable':'طلبات الانضمام غير متاحة في هذا البث','price_confirmation_required':'تغير السعر؛ افتح الهدايا مجددًا','invalid_pk_teams':'اختر متحدثين متصلين في فريقين مختلفين','slow_down':'انتظر قليلًا قبل إعادة الطلب','pk_already_active':'هناك منافسة جارية بالفعل'})[e.message]||'تعذّر تنفيذ الطلب. تحقق من الاتصال.'}
function releasePrepared(){for(const track of preparedTracks){track.detach().forEach(el=>el.closest('.tile')?.remove());track.stop()}preparedTracks=[]}
function stop(label){releasePrepared();if(closed)return;closed=true;room?.removeAllListeners();room?.disconnect();document.querySelectorAll('audio[data-live-remote]').forEach(el=>el.remove());$('ended').hidden=false;$('endedTitle').textContent=label;document.querySelectorAll('footer button,footer input').forEach(el=>el.disabled=true);$('video').replaceChildren();if($('sheet').open)$('sheet').close()}
function leave(){releasePrepared();document.querySelectorAll('audio[data-live-remote]').forEach(el=>el.remove());if(window.LayaliLive){leaving=true;closed=true;room?.disconnect();window.LayaliLive.closeRoom()}else stop('غادرت البث')}
$('closeEnded').onclick=()=>window.LayaliLive?window.LayaliLive.closeRoom():history.back();
function updateSpeakerControl(){
 const button=$('speaker');if(!button)return;
 button.textContent=speakerMuted?'🔇':'🔊';
 button.setAttribute('aria-label',speakerMuted?'تشغيل صوت الغرفة':'كتم صوت الغرفة');
 button.setAttribute('aria-pressed',String(speakerMuted));
 button.title=speakerMuted?'تشغيل صوت الغرفة':'كتم صوت الغرفة';
}
function renderTrack(track,participant,local=false){
 const el=track.attach();el.controls=false;el.autoplay=true;el.playsInline=true;el.setAttribute('playsinline','');el.setAttribute('webkit-playsinline','');el.addEventListener('contextmenu',e=>e.preventDefault());
 if(track.kind===Track.Kind.Audio){
  if(local){track.detach(el);return}
  el.dataset.liveRemote='1';el.muted=speakerMuted;document.body.appendChild(el);
  el.play().catch(()=>{
    if(!closed&&!speakerMuted){
      speakerMuted=true;el.muted=true;updateSpeakerControl();
      $('status').textContent='الصوت يحتاج إذنك. اضغط زر 🔇 لتشغيل صوت الغرفة.';
      el.play().catch(()=>{});
    }
  });
  return el;
 }
 if(track.kind!==Track.Kind.Video)return;
 const key=participant.identity;document.querySelectorAll('.tile').forEach(t=>{if(t.dataset.identity===key)t.remove()});
 const tile=document.createElement('div');tile.className='tile';tile.dataset.identity=key;el.muted=local;if(local)el.setAttribute('muted','');
 if(local&&facing==='user')el.style.transform='scaleX(-1)';const label=document.createElement('small');label.textContent=state.speakers?.find(s=>s.identity===key)?.name||(local?config.userName:participant.identity==='host-'+config.ownerId?config.ownerName:'ضيف');tile.append(el,label);$('video').appendChild(tile);layout();el.play().catch(()=>{});return el;
}
function layout(){const n=document.querySelectorAll('.tile').length;$('video').classList.toggle('group',n>1);$('video').classList.toggle('many',n>4);$('video').hidden=config.mode==='audio';$('audioStage').hidden=config.mode!=='audio';}
async function configurePublishing(){
 const allowed=!!(config.owner||state.can_publish),sources=state.publish_sources||(config.mode==='audio'?['microphone']:['microphone','camera']);
 $('publisherControls').hidden=!speaking;$('camera').hidden=!sources.includes('camera');$('switch').hidden=!sources.includes('camera');
 if(!allowed&&speaking){await room.localParticipant.setMicrophoneEnabled(false);await room.localParticipant.setCameraEnabled(false);speaking=false;$('publisherControls').hidden=true;updateQuickMic()}
}
async function prepareTracks(){
 releasePrepared();mediaFault=false;mediaReady=false;$('connection').classList.remove('media-fault');$('retryMedia').hidden=true;$('status').textContent='جارٍ تجهيز الكاميرا والميكروفون…';
 const sources=state.publish_sources||(config.mode==='audio'?['microphone']:['microphone','camera']);
 preparedTracks=await RelaxiaMedia.prepare({sdk:LivekitClient,video:sources.includes('camera')&&!cameraOff,facing,isClosed:()=>closed||leaving,render:track=>renderTrack(track,{identity:'local-preview'},true),report:(stage,outcome)=>diagnostic(stage,outcome)});
}
async function enableTracks(){
 try{
  let roomVideo=null;
  for(const track of preparedTracks){
   const publication=await room.localParticipant.publishTrack(track,{source:track.kind===Track.Kind.Video?Track.Source.Camera:Track.Source.Microphone});
   if(!publication||!publication.track)throw Error('publication_missing');
   if(track.kind===Track.Kind.Video){
    track.detach().forEach(el=>el.closest('.tile')?.remove());
    roomVideo=renderTrack(track,room.localParticipant,true);
   }
   if(track.kind===Track.Kind.Audio&&muted)await track.mute();
  }
  // Verify the actual room renderer, not only the initial camera preview.
  if(roomVideo){
   $('status').textContent='جارٍ التحقق من صورة البث…';
   try{await RelaxiaMedia.frameReady(roomVideo,10000)}
   catch(e){throw new RelaxiaMedia.MediaFailure('room_video',e)}
  }
  mediaReady=true;speaking=true;diagnostic('media_publish','ready');await configurePublishing();updateQuickMic();
 }catch(e){room?.removeAllListeners();await room?.disconnect();releasePrepared();throw e.name==='MediaFailure'?e:new RelaxiaMedia.MediaFailure('media_publish',e)}
}
// Time out a stalled LiveKit handshake rather than locking the room forever.
async function connectWithTimeout(client,url,token){
 let timer;
 try{return await Promise.race([client.connect(url,token),new Promise((_,reject)=>{timer=setTimeout(()=>reject(Error('signaling_timeout')),18000)})])}
 finally{clearTimeout(timer)}
}
async function connect(token=config.token){
 try{
 if(config.owner||speaking){await prepareTracks();const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;if(!fresh.can_publish){releasePrepared();throw Error('stage_unavailable')}token=fresh.token;}
 if(closed||leaving){releasePrepared();return}
 room=new Room({adaptiveStream:true,dynacast:true,audioCaptureDefaults:{echoCancellation:true,noiseSuppression:true,autoGainControl:true},videoCaptureDefaults:{resolution:{width:1280,height:720,frameRate:30},facingMode:facing},publishDefaults:{simulcast:false,videoEncoding:{maxBitrate:1500000,maxFramerate:30}}});
 room.on(RoomEvent.TrackSubscribed,(track,pub,participant)=>renderTrack(track,participant));
 room.on(RoomEvent.TrackUnsubscribed,track=>{track.detach().forEach(el=>{const tile=el.closest('.tile');el.remove();tile?.remove()});layout()});
 room.on(RoomEvent.ConnectionStateChanged,s=>{$('status').textContent=s===ConnectionState.Connected?((config.owner||speaking)&&!mediaReady?'جارٍ نشر الصوت والصورة…':'متصل'):s===ConnectionState.Reconnecting?'إعادة الاتصال…':'جارٍ الاتصال…'});
 room.on(RoomEvent.ConnectionQualityChanged,(q,p)=>{if(p===room.localParticipant&&['poor','lost'].includes(String(q).toLowerCase()))$('status').textContent='الشبكة ضعيفة · يجري تكييف الجودة'});
 room.on(RoomEvent.ActiveSpeakersChanged,participants=>{activeSpeakerIds=new Set(participants.map(p=>p.identity));document.querySelectorAll('.audio-person').forEach(el=>el.classList.toggle('speaking',activeSpeakerIds.has(el.dataset.identity)))});
 room.on(RoomEvent.Disconnected,()=>{if(!closed&&!leaving)reconnect()});
 await connectWithTimeout(room,config.url,token);$('reconnectAction').hidden=true;for(const participant of room.remoteParticipants.values()){for(const publication of participant.trackPublications.values()){if(publication.isSubscribed&&publication.track)renderTrack(publication.track,participant);}}if(closed||leaving){room.removeAllListeners();await room.disconnect();releasePrepared();return}if(config.owner||speaking)await enableTracks();$('status').textContent='مباشر';
 $('follow').hidden=config.owner;$('gift').hidden=true;$('speaker').hidden=true;updateSpeakerControl();updateQuickMic();
 layout();if(!polling){polling=true;poll()}
 }catch(e){room?.removeAllListeners();try{await room?.disconnect()}finally{releasePrepared();mediaReady=false}throw e}
}
async function reconnect(){if(retrying||closed||leaving)return;retrying=true;try{for(let attempt=0;attempt<6&&!closed&&!leaving;attempt++){$('status').textContent='إعادة الاتصال…';await new Promise(r=>setTimeout(r,Math.min(5000,1000*(attempt+1))));try{const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;if(closed||leaving)return;room?.removeAllListeners();await room?.disconnect();document.querySelectorAll('audio').forEach(el=>el.remove());$('video').replaceChildren();if(state.can_publish===false&&!config.owner)speaking=false;await connect(fresh.token);return}catch(e){if(e.name==='MediaFailure'){mediaMessage(e);return}if(['broadcast_forbidden','broadcast_ended','live_authorization_required','admission_refunded','feature_restricted','broadcast_permission_required'].includes(e.message)){stop('انتهت صلاحية البث');return}}}if(!closed&&!leaving){$('status').textContent='تعذّر الاتصال. جرّب إعادة الاتصال دون شراء دخول جديد.';$('reconnectAction').hidden=false}}finally{retrying=false}}
function sheet(title){$('sheetTitle').textContent=title;$('sheetBody').replaceChildren();if(!$('sheet').open)$('sheet').showModal();return $('sheetBody')}
function button(box,label,fn,cls=''){const b=document.createElement('button');b.textContent=label;b.className=cls;b.onclick=async()=>{if(b.disabled)return;b.disabled=true;try{await fn()}catch(e){message(e)}finally{b.disabled=false}};box.appendChild(b);return b}
function note(box,text){const p=document.createElement('p');p.textContent=text;box.appendChild(p)}
$('closeSheet').onclick=()=>$('sheet').close();$('sheet').addEventListener('click',e=>{if(e.target===$('sheet'))$('sheet').close()});
async function consent(mode){
 if(!window.LayaliLive?.requestPublishPermissions)return true;
 return new Promise(resolve=>{const timer=setTimeout(()=>{window.removeEventListener('publishpermissions',done);resolve(false)},60000);function done(e){clearTimeout(timer);window.removeEventListener('publishpermissions',done);resolve(e.detail===true)}window.addEventListener('publishpermissions',done);window.LayaliLive.requestPublishPermissions(mode)})
}
async function activateSeat(){if(activating)return;activating=true;try{const mode=(state.publish_sources||[]).includes('camera')?'video':'audio';if(!await consent(mode))throw Error('media_permission_required');const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;if(!fresh.can_publish)throw Error('stage_unavailable');room.removeAllListeners();await room.disconnect();$('video').replaceChildren();document.querySelectorAll('audio').forEach(el=>el.remove());speaking=true;await connect(fresh.token);$('sheet').close()}catch(e){speaking=false;message(e)}finally{activating=false}}
function guestSheet(){
 const box=sheet('الضيوف · '+(state.speakers?.length||1)+' / '+config.maxSpeakers);
 for(const s of state.speakers||[]){note(box,s.name+' · '+(s.mode==='audio'?'صوت':'فيديو'));if(state.manage_room&&!s.owner)button(box,'إزالة '+s.name,()=>api({action:'seat_decision',user_id:s.user_id,approve:false}).then(refresh))}
 if(!config.owner){
  if(state.seat_status==='approved')button(box,speaking?'مغادرة مقعد الضيف':(config.mode==='audio'?'تشغيل الميكروفون بإذني':'تشغيل الميكروفون والكاميرا بإذني'),()=>speaking?api({action:'seat_leave'}).then(refresh):activateSeat(),'primary');
  else if(state.seat_status==='pending')note(box,'طلبك بانتظار موافقة صاحب البث.');
  else if(config.maxSpeakers>1){button(box,'طلب الانضمام صوتيًا',()=>api({action:'seat_request',mode:'audio'}).then(refresh));if(config.mode!=='audio')button(box,'طلب الانضمام بالفيديو',()=>api({action:'seat_request',mode:'video'}).then(refresh))}
 }
 if(state.manage_room){for(const p of state.seat_requests||[]){note(box,'طلب: '+p.name+' · '+(p.mode==='audio'?'صوت':'فيديو'));button(box,'قبول '+p.name,()=>api({action:'seat_decision',user_id:p.user_id,approve:true}).then(refresh));button(box,'رفض '+p.name,()=>api({action:'seat_decision',user_id:p.user_id,approve:false}).then(refresh))}}
 if(config.owner&&state.speakers?.length>=2)button(box,'إعداد منافسة PK',pkSheet);
}
function pkSheet(){const box=sheet('منافسة PK');note(box,'تحدٍّ بالنقاط من الهدايا المسددة فقط. لا توجد مراهنات أو جوائز مالية تلقائية.');if(state.pk?.active){button(box,'إنهاء المنافسة',()=>api({action:'pk_cancel'}).then(refresh));return}
 const members=[];for(const s of state.speakers||[]){note(box,s.name);const select=document.createElement('select');for(const [v,t] of [['0','الفريق الأول'],['1','الفريق الثاني'],['','دون مشاركة']]){const o=new Option(t,v);select.add(o)}select.value=s.owner?'0':'1';box.append(select);members.push([s.user_id,select])}
 const duration=document.createElement('select');for(const n of [60,180,300])duration.add(new Option((n/60)+' دقيقة',String(n)));box.append(duration);
 button(box,'بدء المنافسة',async()=>{const teams=[[],[]];for(const [id,el] of members)if(el.value!=='')teams[Number(el.value)].push(id);await api({action:'pk_start',teams,duration:Number(duration.value)});$('sheet').close();await refresh()},'primary')}
function giftSheet(){
 const box=sheet('متجر الهدايا');note(box,'اختر المستلم والهدية. يظهر السعر قبل التأكيد؛ يحصل المستلم على 80٪.');
 const select=document.createElement('select');select.setAttribute('aria-label','مستلم الهدية');
 for(const speaker of state.speakers||[])if(speaker.user_id!==config.userId)select.add(new Option(speaker.name,String(speaker.user_id)));
 box.append(select);
 if(!select.options.length)note(box,'لا يوجد مستلم آخر على الميكروفون حاليًا.');
 const tabs=document.createElement('div');tabs.className='gift-tabs';box.append(tabs);
 const grid=document.createElement('div');grid.className='gift-grid';box.append(grid);
 function render(category){
  grid.replaceChildren();for(const tab of tabs.children)tab.setAttribute('aria-pressed',String(tab.dataset.category===category));
  const catalog=(state.gift_catalog||[]).filter(g=>category==='all'||g.effect===category);
  for(const g of catalog){
   const b=button(grid,'',async()=>{
    if(!select.value)return;
    if(!confirm('إرسال '+g.name+' إلى '+select.selectedOptions[0].text+' مقابل '+g.price_cents+' كوين؟'))return;
    const fingerprint=g.code+':'+select.value+':'+g.price_cents;
    if(giftPending&&giftPending!==fingerprint)throw Error('pending_gift_retry');
    giftPending=fingerprint;giftKey=giftKey||crypto.randomUUID();
    try{const result=await api({action:'gift',gift_code:g.code,recipient_id:Number(select.value),price_cents:g.price_cents,confirmed:true,request_key:giftKey});giftKey=null;giftPending=null;drawState(result);$('sheet').close()}
    catch(e){if(['insufficient_balance','price_confirmation_required','gift_unavailable'].includes(e.message)){giftKey=null;giftPending=null}throw e}
   });
   b.disabled=!select.options.length;b.className='gift-card '+g.effect;b.setAttribute('aria-label',g.name+' · '+g.price_cents+' كوين');
   const icon=document.createElement('span');icon.className='gift-art';icon.textContent=g.symbol;
   const medallion=document.createElement('span');medallion.className='gift-medallion';medallion.append(icon);
   b.classList.add('gift-'+String(g.code).replace(/[^a-z0-9_-]/g,''));
   const name=document.createElement('strong');name.textContent=g.name;
   const price=document.createElement('small');price.textContent=g.price_cents.toLocaleString('ar')+' كوين';
   b.append(medallion,name,price);
  }
  if(!catalog.length)note(grid,'لا توجد هدايا متاحة في هذا القسم.');
 }
 for(const [key,label] of [['all','الكل'],['simple','بسيطة'],['animated','متحركة'],['depth','فاخرة']]){
  const tab=button(tabs,label,()=>render(key));tab.dataset.category=key;
 }
 render('all');note(box,'أكبر الداعمين');for(const supporter of state.top_supporters||[])note(box,supporter.name+' · '+supporter.total+' كوين');
}
function drawAudioSeats(result){
 const box=$('audioStage'),capacity=Math.max(1,Math.min(15,result.max_speakers||config.maxSpeakers||1));
 const speakers=result.speakers||[];
 const signature=JSON.stringify([capacity,speakers.map(s=>[s.identity,s.name,s.owner,s.avatar_url])]);
 if(signature===audioSeatSignature&&box.querySelectorAll('.audio-seat').length===capacity){
  box.querySelectorAll('.audio-seat[data-identity]').forEach(item=>item.classList.toggle('speaking',activeSpeakerIds.has(item.dataset.identity)));
  return;
 }
 audioSeatSignature=signature;
 box.classList.add('seat-grid');box.classList.toggle('solo-room',capacity===1);
 const nodes=document.createDocumentFragment();
 for(let i=0;i<capacity;i++){
  const speaker=speakers[i],item=document.createElement('button');item.type='button';item.className='audio-person audio-seat';item.onclick=guestSheet;
  if(speaker){
   item.dataset.identity=speaker.identity;item.classList.toggle('speaking',activeSpeakerIds.has(speaker.identity));
   item.setAttribute('aria-label',speaker.name+(speaker.owner?' · صاحب الغرفة':''));
   const waves=document.createElement('div');waves.className='audio-waves';waves.setAttribute('aria-hidden','true');
   for(let n=0;n<3;n++)waves.append(document.createElement('span'));item.append(waves);
   const placeholder=document.createElement('div');placeholder.className='audio-placeholder';placeholder.textContent=(speaker.name||'م').slice(0,1);item.append(placeholder);
   if(/^https:\/\//.test(speaker.avatar_url)){
    const img=document.createElement('img');img.src=speaker.avatar_url;img.alt='';
    img.onload=()=>placeholder.style.visibility='hidden';img.onerror=()=>img.remove();item.append(img)
   }
   const label=document.createElement('span');label.className='audio-name';label.textContent=speaker.name;item.append(label);
   const role=document.createElement('small');role.textContent=speaker.owner?'المضيف':String(i+1);item.append(role);
  }else{
   item.classList.add('empty-seat');item.setAttribute('aria-label','مقعد '+(i+1)+' · طلب الميكروفون');
   const circle=document.createElement('span');circle.className='seat-plus';circle.textContent='+';
   const label=document.createElement('small');label.textContent=String(i+1);item.append(circle,label);
  }
  nodes.append(item);
 }
 box.replaceChildren(nodes);
}
function updateQuickMic(){
 const button=$('micQuick');if(!button)return;
 // Viewers already have the guest seat button. Only speakers need a quick mic.
 button.hidden=!speaking;
 button.textContent=speaking?(muted?'🔇':'🎙'):'🎙';
 button.setAttribute('aria-pressed',String(speaking&&!muted));
 button.setAttribute('aria-label',speaking?(muted?'تشغيل الميكروفون':'كتم الميكروفون'):'المقاعد وطلب التحدث');
}

function peopleSheet(){
 const box=sheet('المشاهدون والمشرفون');
 for(const p of state.participants||[]){
  note(box,p.name);
  if(state.manage_room)button(box,'حظر '+p.name,async()=>{
   if(confirm('حظر هذا المستخدم من البث؟')){await api({action:'ban',user_id:p.user_id});await refresh()}
  },'danger');
  if(config.owner&&state.manage_room){
   const isMod=state.moderator_ids?.includes(p.user_id);
   button(box,(isMod?'إزالة إشراف ':'تعيين مشرف ')+p.name,()=>api({action:'set_moderator',user_id:p.user_id,enabled:!isMod}).then(refresh))
  }
 }
}
function moreSheet(){const box=sheet('أدوات الغرفة');
 if(window.innerWidth<=380)button(box,'الرموز التعبيرية',()=>{$('sheet').close();$('emojiBtn').click()});
 if(!config.owner)button(box,state.following?'إلغاء متابعة المضيف':'متابعة المضيف',()=>api({action:'follow'}).then(refresh));
 if(config.mode==='audio')button(box,speakerMuted?'تشغيل صوت الغرفة':'كتم صوت الغرفة',()=>{$('sheet').close();$('speaker').click()});
 button(box,'مشاركة الغرفة',()=>{$('sheet').close();$('share').click()});
 if(speaking&&(state.publish_sources||[]).includes('camera')){
  button(box,cameraOff?'تشغيل الكاميرا':'إيقاف الكاميرا',()=>{$('sheet').close();$('camera').click()});
  button(box,'تبديل الكاميرا',()=>{$('sheet').close();$('switch').click()});
 }
 button(box,state.liked?'أعجبك هذا البث':'إعجاب بالبث',()=>api({action:'like'}).then(refresh));button(box,commentsVisible?'إخفاء التعليقات':'إظهار التعليقات',()=>{commentsVisible=!commentsVisible;$('messages').hidden=!commentsVisible;$('pinned').hidden=!commentsVisible||!state.pinned_comment;$('sheet').close()});if(state.manage_room)button(box,'المشاهدون والإشراف',peopleSheet);if(config.owner&&state.speakers?.length>=2)button(box,'منافسة PK',pkSheet);
 if(speaking){button(box,'جودة الفيديو · '+quality+'p',qualitySheet)}
 button(box,'إحصائيات البث',()=>{const b=sheet('إحصائيات البث');note(b,'المشاهدون المتصلون الآن: '+(state.viewers||0));note(b,'الإعجابات: '+(state.likes||0));note(b,'قيمة الهدايا: '+(state.gift_total_cents||0));if(config.owner)note(b,'حصة مستلمي الهدايا المستحقة: '+(state.earned_cents||0));for(const p of state.top_supporters||[])note(b,p.name+' · '+p.total)});
 button(box,'الإبلاغ عن البث',async()=>{const reason=prompt('سبب الإبلاغ (3 أحرف على الأقل)');if(reason&&reason.trim().length>=3){await api({action:'report',reason});$('status').textContent='وصل البلاغ إلى الإدارة';$('sheet').close()}});
 if(config.owner)button(box,'إنهاء البث للجميع',async()=>{if(confirm('إنهاء البث للجميع؟')){await api({action:'end'});stop('انتهى البث')}},'danger')}
function qualitySheet(){const box=sheet('جودة الفيديو');note(box,'تتكيّف جودة النقل تلقائيًا. 1080p تعتمد على الكاميرا والجهاز والشبكة.');for(const q of [360,720,1080])button(box,q+'p',async()=>{const track=room.localParticipant.getTrackPublication(Track.Source.Camera)?.track;if(!track)throw Error('camera_unavailable');await track.restartTrack({resolution:{width:Math.round(q*16/9),height:q,frameRate:q===360?20:30},facingMode:facing});quality=q;$('sheet').close()})}
function playEffects(){
 if(effectBusy||!giftQueue.length)return;effectBusy=true;
 const g=giftQueue.shift(),box=$('giftEffect');box.replaceChildren();
 const effect=['simple','animated','depth'].includes(g.effect)?g.effect:'simple';
 box.className='gift-show '+effect;
 if(g.code==='whale'||g.code==='lion')box.classList.add('show-'+g.code);
 const icon=document.createElement('span');icon.className='gift-hero';icon.textContent=g.symbol;
 const label=document.createElement('strong');label.textContent=g.sender+' أرسل '+g.name;
 const recipient=document.createElement('small');recipient.textContent='إلى '+g.recipient;
 if(effect!=='simple'){const particles=document.createElement('div');particles.className='gift-particles';particles.setAttribute('aria-hidden','true');for(let i=0;i<8;i++){const star=document.createElement('i');star.textContent=g.code==='whale'?'○':'✦';particles.append(star)}box.append(particles)}
 box.append(icon,label,recipient);box.hidden=false;
 const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
 setTimeout(()=>{box.hidden=true;effectBusy=false;playEffects()},reduced?1600:effect==='simple'?1600:effect==='depth'?3800:2600);
}
function drawState(result){state=result;updateQuickMic();document.querySelectorAll('.tile').forEach(t=>{const speaker=result.speakers?.find(s=>s.identity===t.dataset.identity);if(speaker)t.querySelector('small').textContent=speaker.name});serverOffset=Date.parse(result.server_time)-Date.now();const vCount=result.viewers||0;if($('viewerCount'))$('viewerCount').textContent=vCount>=1000?(vCount/1000).toFixed(1)+'K':vCount;else $('viewers').textContent=vCount+' مشاهد';$('viewers').title=vCount+' مشاهد';$('follow').textContent=result.following?'تتابعه':'متابعة';const host=result.speakers?.[0];$('badge').textContent=host&&Number.isFinite(Number(host.level))?('المستوى '+Math.max(0,Number(host.level))+(host.vip?' · VIP':'')):'';
 const signature=JSON.stringify(result.comments);if(lastComments!==signature){lastComments=signature;const box=$('messages');const nearBottom=box.scrollHeight-box.clientHeight-box.scrollTop<38,previousScroll=box.scrollTop;box.replaceChildren();for(const c of result.comments||[]){const p=document.createElement('div');p.className='comment-bubble';const av=document.createElement('div');av.className='comment-avatar';av.textContent=(c.name||'U').slice(0,1);const body=document.createElement('div');body.className='comment-body';const name=document.createElement('span');name.className='comment-author';name.textContent=c.name+': ';body.append(name,document.createTextNode(c.text));p.append(av,body);if(result.manage_room){const pin=document.createElement('button');pin.textContent='تثبيت';pin.onclick=()=>api({action:'pin_comment',comment_id:c.id}).then(refresh).catch(message);const del=document.createElement('button');del.textContent='حذف';del.onclick=()=>api({action:'delete_comment',comment_id:c.id}).then(refresh).catch(message);p.append(pin,del)}box.append(p)}box.scrollTop=nearBottom?box.scrollHeight:Math.min(previousScroll,Math.max(0,box.scrollHeight-box.clientHeight))}
 $('pinned').hidden=!result.pinned_comment||!commentsVisible;if(result.pinned_comment)$('pinned').textContent='مثبّت · '+result.pinned_comment.name+': '+result.pinned_comment.text;
 $('goal').hidden=!result.gift_goal_cents;if(result.gift_goal_cents){if($('goalText'))$('goalText').textContent=(result.gift_total_cents||0)+' / '+result.gift_goal_cents;if($('goalProgress'))$('goalProgress').style.width=Math.min(100,Math.round(((result.gift_total_cents||0)/result.gift_goal_cents)*100))+'%';else $('goal').textContent='هدف الهدايا '+(result.gift_total_cents||0)+' / '+result.gift_goal_cents;}
 $('pk').hidden=!result.pk;document.body.classList.toggle('has-pk',!!result.pk);if(result.pk){const pk=result.pk;const teams=pk.teams.map(team=>team.map(id=>result.speakers?.find(s=>s.user_id===id)?.name||'مشارك').join(' + '));$('pkTeams').replaceChildren();for(let i=0;i<2;i++){const span=document.createElement('span');span.title=teams[i];span.textContent=(i===0?'الفريق الأول':'الفريق الثاني')+' · '+pk.scores[i];$('pkTeams').append(span)}const total=pk.scores[0]+pk.scores[1];$('pkProgress').value=total?pk.scores[0]/total*100:50;pkClock()}
 if(config.mode==='audio')drawAudioSeats(result);
 for(const g of result.gift_events||[]){if(haveGiftBaseline&&g.id>latestGift)giftQueue.push(g);latestGift=Math.max(latestGift,g.id)}haveGiftBaseline=true;giftQueue=giftQueue.slice(-4);playEffects();configurePublishing().catch(message);
}
function pkClock(){if(!state.pk)return;const pk=state.pk;const seconds=Math.max(0,Math.ceil((Date.parse(pk.ends_at)-Date.now()-serverOffset)/1000));$('pkTimer').textContent=pk.cancelled?'أُلغيت المنافسة':seconds>0?Math.floor(seconds/60)+':'+String(seconds%60).padStart(2,'0'):'انتهت المنافسة · '+(pk.scores[0]===pk.scores[1]?'تعادل':pk.scores[0]>pk.scores[1]?'الفريق الأول يتقدم':'الفريق الثاني يتقدم')}
setInterval(pkClock,1000);
async function refresh(){
 const result=await api();controlFailures=0;
 if(!['starting','active','reconnecting'].includes(result.status)){stop('انتهى البث');return}
 drawState(result);
 if(result.status==='reconnecting'||result.status==='starting'){
  if(!reconnectSince)reconnectSince=Date.now();
  const late=Date.now()-reconnectSince>=12000;
  $('status').textContent=result.status==='starting'?(late?'تأخر بدء البث. يمكنك إعادة الاتصال.':'الغرفة تبدأ الآن…'):(late?'صاحب البث يعيد الاتصال. أعد المحاولة.':'صاحب البث يعيد الاتصال…');
  $('reconnectAction').hidden=!late;
 }else{
  reconnectSince=0;$('reconnectAction').hidden=true;
  if(!mediaFault&&room?.state===ConnectionState.Connected&&(!(config.owner||speaking)||mediaReady))$('status').textContent='مباشر';
 }
 return result;
}
async function poll(){while(!closed){
 try{await refresh()}
 catch(e){
  if(e.name==='MediaFailure'){mediaMessage(e);return}
  if(['broadcast_forbidden','broadcast_ended','live_authorization_required','admission_refunded'].includes(e.message)){stop('انتهت صلاحية البث');break}
  controlFailures++;
  $('status').textContent='تعذّر تحديث الغرفة. نعيد المحاولة…';
  $('reconnectAction').hidden=controlFailures<3;
 }
 await new Promise(r=>setTimeout(r,3000))
}}
$('composer').onsubmit=async e=>{e.preventDefault();const input=$('text'),text=input.value.trim();if(!text)return;const b=e.submitter||$('composer').querySelector('button');b.disabled=true;try{await api({action:'comment',text});input.value='';await refresh()}catch(e){message(e)}finally{b.disabled=false}};
$('mute').onclick=async()=>{try{await room.localParticipant.setMicrophoneEnabled(muted);muted=!muted;$('mute').textContent=muted?'تشغيل الميكروفون':'كتم الميكروفون';updateQuickMic()}catch(e){message(e)}};
$('camera').onclick=async()=>{try{await room.localParticipant.setCameraEnabled(cameraOff);cameraOff=!cameraOff;$('camera').textContent=cameraOff?'تشغيل الفيديو':'إيقاف الفيديو';if(!cameraOff){const track=room.localParticipant.getTrackPublication(Track.Source.Camera)?.track;if(track)renderTrack(track,room.localParticipant,true)}else document.querySelectorAll('.tile').forEach(t=>{if(t.dataset.identity===room.localParticipant.identity)t.remove()});layout()}catch(e){message(e)}};
$('switch').onclick=async()=>{try{const next=facing==='user'?'environment':'user';await room.localParticipant.getTrackPublication(Track.Source.Camera)?.track?.restartTrack({facingMode:next});facing=next;document.querySelectorAll('.tile').forEach(t=>{if(t.dataset.identity===room.localParticipant.identity)t.querySelector('video').style.transform=facing==='user'?'scaleX(-1)':''})}catch(e){$('status').textContent='الكاميرا الأخرى غير متاحة'}};
$('exit').onclick=()=>{if(config.owner){if(confirm('مغادرتك تنهي البث. هل تريد الإنهاء؟'))api({action:'end'}).then(leave).catch(message)}else leave()};
$('speaker').onclick=async()=>{
 speakerMuted=!speakerMuted;updateSpeakerControl();
 for(const el of document.querySelectorAll('audio[data-live-remote]')){
  el.muted=speakerMuted;
  if(!speakerMuted)try{await el.play()}catch(e){speakerMuted=true;el.muted=true;updateSpeakerControl();$('status').textContent='تعذر تشغيل الصوت. تحقق من صوت الجهاز وأذونات الوسائط.';break}
 }
};
$('follow').onclick=()=>api({action:'follow'}).then(refresh).catch(message);
$('gift').onclick=giftSheet;$('guests').onclick=guestSheet;
$('micQuick').onclick=()=>{if(speaking)$('mute').click();else guestSheet()};
$('more').onclick=moreSheet;
$('share').onclick=async()=>{const text='انضم إلى بث '+config.ownerName+' «'+config.title+'» على Relaxia. افتح قسم البث المباشر وابحث عن عنوان البث. الدخول الخاص يتطلب تصريحًا.';try{if(window.LayaliLive?.shareLive){window.LayaliLive.shareLive(text);$('status').textContent='نُسخت دعوة البث؛ يمكنك إرسالها إلى أصدقائك.';}else if(navigator.share)await navigator.share({title:'Relaxia · مباشر',text});else if(navigator.clipboard){await navigator.clipboard.writeText(text);$('status').textContent='نُسخت دعوة البث'}else prompt('انسخ دعوة البث',text)}catch(e){if(e.name!=='AbortError')message(e)}};
$('reconnectAction').onclick=()=>{$('reconnectAction').hidden=true;reconnectSince=Date.now();reconnect()};
$('retryMedia').onclick=async()=>{if(activating)return;activating=true;try{if(!await consent(config.mode))throw new RelaxiaMedia.MediaFailure('camera_capture',{name:'NotAllowedError'});room?.removeAllListeners();await room?.disconnect();const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;await connect(fresh.token)}catch(e){message(e)}finally{activating=false}};

const heartEmojis=['💜','💖','❤️','💕','✨'];
function spawnHeart(emoji){
  const layer=$('floatingHearts');
  if(!layer)return;
  const h=document.createElement('div');
  h.className='floating-heart';
  h.textContent=emoji||heartEmojis[Math.floor(Math.random()*heartEmojis.length)];
  h.style.right=(8+Math.random()*24)+'px';
  layer.appendChild(h);
  setTimeout(()=>h.remove(),2500);
}
$('stage')?.addEventListener('click',e=>{
  if(e.target.closest('button,input,form,dialog,#sheet,header,footer'))return;
  spawnHeart();
});
if($('bigGiftBtn')){if(!config.gifts)$('bigGiftBtn').hidden=true;$('bigGiftBtn').onclick=()=>{if(config.gifts)giftSheet();};}
if($('emojiBtn'))$('emojiBtn').onclick=()=>{
 const box=sheet('الرموز التعبيرية');
 for(const emoji of ['😀','😂','🥰','😍','👏','❤️','🌹','🔥','🎉','💙','👍','✨']){
  button(box,emoji,()=>{const input=$('text');if(!input)return;input.value=(input.value+emoji).slice(0,300);$('sheet').close();input.focus()});
 }
};
layout();updateQuickMic();window.relaxiaStageLoaded=true;
// These seats use room metadata; they are placeholders, not fabricated online users.
if(config.mode==='audio')drawAudioSeats({
 max_speakers:config.maxSpeakers||15,
 speakers:[{identity:'host-'+config.ownerId,name:config.ownerName,owner:true,avatar_url:config.ownerAvatar||''}]
});
// Load seats, audience and chat before LiveKit connects, including recovery states.
if(!polling){polling=true;poll()}
window.addEventListener('pagehide',()=>{leaving=true;releasePrepared();room?.disconnect()});connect().catch(e=>{message(e);if(e.name!=='MediaFailure')reconnect()});

// Native lifecycle: keep audio connected, release camera while the app is hidden.
let nativeCameraWasOn=false;
window.setNativeBackground=async hidden=>{
 try {
  if(hidden){nativeCameraWasOn=!!room?.localParticipant?.isCameraEnabled;if(nativeCameraWasOn)await room.localParticipant.setCameraEnabled(false)}
  else if(nativeCameraWasOn){nativeCameraWasOn=false;await room.localParticipant.setCameraEnabled(true)}
 }catch(error){message(error)}
};
