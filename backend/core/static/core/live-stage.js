'use strict';
if(typeof LivekitClient==='undefined'||typeof RelaxiaMedia==='undefined')throw Error('live_dependencies_unavailable');
const config=JSON.parse(document.getElementById('config').textContent),$=id=>document.getElementById(id);
const {Room,RoomEvent,Track,ConnectionState}=LivekitClient;
let room,closed=false,muted=false,speakerMuted=false,cameraOff=false,facing='user',commentsVisible=true,lastComments='',leaving=false,retrying=false,polling=false;
let activeSpeakerIds=new Set(),audioSeatSignature='',reconnectSince=0,controlFailures=0,connecting=false;
let stableViewportHeight=Math.max(window.innerHeight||0,document.documentElement.clientHeight||0);
function updateRoomViewport(){
 // Android IME and browser chrome both resize visualViewport. Keyboard focus
 // alone is NOT proof that the keyboard is showing: do not obscure seats.
 const visual=window.visualViewport;
 const layoutHeight=document.documentElement.clientHeight||window.innerHeight;
 const visible=visual?Math.min(layoutHeight,visual.height):layoutHeight;
 const height=Math.max(1,Math.round(visible));
 const focus=document.activeElement?.id==='text';
 if(!focus)stableViewportHeight=Math.max(stableViewportHeight,height);
 const keyboardVisible=focus&&(stableViewportHeight-height>=145);
 document.body.classList.toggle('ime-open',keyboardVisible);
 document.body.classList.remove('chat-focused');
 document.documentElement.style.setProperty('--room-viewport-height',height+'px');
}
updateRoomViewport();
window.visualViewport?.addEventListener('resize',updateRoomViewport,{passive:true});
window.addEventListener('resize',updateRoomViewport,{passive:true});
/* Footer height is device-dependent (font scale, keyboard, cutout, safe areas).
   Use its measured height to keep comments and seats above the working controls. */
function syncRoomFooterMetrics(){
 const footer=document.querySelector('#stage footer'),stage=$('stage');
 if(!footer||!stage)return;
 const height=Math.ceil(footer.getBoundingClientRect().height);
 if(height>0)stage.style.setProperty('--room-footer-height',Math.max(124,height+6)+'px');
}
const roomFooter=document.querySelector('#stage footer');
const footerSizeObserver=roomFooter&&typeof ResizeObserver!=='undefined'?new ResizeObserver(syncRoomFooterMetrics):null;
if(roomFooter&&footerSizeObserver)footerSizeObserver.observe(roomFooter);
window.visualViewport?.addEventListener('resize',syncRoomFooterMetrics,{passive:true});
window.addEventListener('resize',syncRoomFooterMetrics,{passive:true});
requestAnimationFrame(syncRoomFooterMetrics);
let state={},speaking=false,activating=false,latestGift=0,haveGiftBaseline=false,giftQueue=[],effectBusy=false,giftKey=null,giftPending=null,quality=720,serverOffset=0;
// Keep the current gift recipient list in sync with server-approved speakers.
let activeGiftRecipientSync=null;
let preparedTracks=[],mediaFault=false,mediaReady=false;
const path='/api/live/'+config.id+'/control';
document.body.classList.toggle('audio-mode',config.mode==='audio');
$('name').textContent=config.ownerName;$('title').textContent='ID: '+(config.ownerPublicId||'—');document.title='Relaxia · '+config.title;
$('profile').onclick=()=>{if(!config.owner&&window.LayaliLive?.canOpenProfile()){window.LayaliLive.openProfile(config.ownerId);return;}const box=sheet('ملف صاحب الغرفة');note(box,config.ownerName);note(box,'ID: '+(config.ownerPublicId||'—'));note(box,config.title)};
async function api(action){const abort=new AbortController(),timer=setTimeout(()=>abort.abort(),8000);try{const r=await fetch(path,{method:action?'POST':'GET',headers:{'Content-Type':'application/json','X-Live-Control':config.control},body:action?JSON.stringify(action):undefined,signal:abort.signal,cache:'no-store'});const value=await r.json();if(!r.ok)throw Error(value.error||'network');return value}finally{clearTimeout(timer)}}
function diagnostic(stage,outcome,reason=''){api({action:'media_diagnostic',stage,outcome,reason,build:'media-1'}).catch(()=>{})}
function mediaMessage(e){
 mediaFault=true;mediaReady=false;$('connection').classList.add('media-fault');const labels={camera_capture:'تعذر تشغيل الكاميرا',camera_preview:'الكاميرا لا تعرض صورة',microphone_capture:'تعذر تشغيل الميكروفون',media_publish:'تعذر نشر الصوت أو الصورة',room_video:'نُشر الفيديو لكن لم تظهر صورته داخل الغرفة'};
 const reasons={NotAllowedError:'تحقق من أذونات التطبيق والكاميرا في Android.',NotReadableError:'قد تكون الكاميرا أو الميكروفون مستخدمة في تطبيق آخر.',NotFoundError:'لم يعثر الجهاز على الكاميرا أو الميكروفون.'};
 $('status').textContent=(labels[e.stage]||'تعذر تجهيز البث')+' · '+(reasons[e.reason]||'أعد المحاولة أو ارجع وافتح البث مجددًا.');$('retryMedia').hidden=false;diagnostic(e.stage,'failed',e.reason);
}
function message(e){if(e.name==='MediaFailure'){mediaMessage(e);return}$('connection').classList.remove('is-ready');$('status').textContent=({'insufficient_balance':'رصيدك لا يكفي. اشحن من المحفظة قبل إرسال الهدية.','broadcast_ended':'انتهى البث','stage_full':'اكتملت مقاعد الضيوف','pending_gift_retry':'أعد محاولة الهدية السابقة أولًا لتجنب خصم غير واضح.','invalid_gift_combo':'عدد الهدايا أو الإجمالي غير مسموح.','payment_request_conflict':'طلب الهدية تغيّر. تحقق من العملية السابقة قبل إعادة الإرسال.','media_permission_required':'لم تمنح إذن الميكروفون أو الكاميرا. يمكنك الاستمرار بالمشاهدة.','stage_unavailable':'طلبات الانضمام غير متاحة في هذا البث','price_confirmation_required':'تغير السعر؛ افتح الهدايا مجددًا','invalid_pk_teams':'اختر متحدثين متصلين في فريقين مختلفين','slow_down':'انتظر قليلًا قبل إعادة الطلب','pk_already_active':'هناك منافسة جارية بالفعل'})[e.message]||'تعذّر تنفيذ الطلب. تحقق من الاتصال.'}
function releasePrepared(){for(const track of preparedTracks){track.detach().forEach(el=>el.closest('.tile')?.remove());track.stop()}preparedTracks=[]}
function stop(label){window.RelaxiaGiftCinema?.stop();releasePrepared();if(closed)return;closed=true;room?.removeAllListeners();room?.disconnect();document.querySelectorAll('audio[data-live-remote]').forEach(el=>el.remove());$('ended').hidden=false;$('endedTitle').textContent=label;document.querySelectorAll('footer button,footer input').forEach(el=>el.disabled=true);$('video').replaceChildren();if($('sheet').open)$('sheet').close()}
function leave(){window.RelaxiaGiftCinema?.stop();releasePrepared();document.querySelectorAll('audio[data-live-remote]').forEach(el=>el.remove());if(window.LayaliLive){leaving=true;closed=true;room?.disconnect();window.LayaliLive.closeRoom()}else stop('غادرت البث')}
$('closeEnded').onclick=()=>window.LayaliLive?window.LayaliLive.closeRoom():history.back();
// Fixed vector paths keep microphone/speaker states legible on Android WebView.
function setMediaControlIcon(button,kind,off){
 const paths=kind==='mic'
  ? '<rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10v2a7 7 0 0 0 14 0v-2M12 19v3M8 22h8"/>'
  : '<path d="M11 5 6 9H3v6h3l5 4zM15 8a6 6 0 0 1 0 8M18 5a10 10 0 0 1 0 14"/>';
 button.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+paths+(off?'<path d="m3 3 18 18"/>':'')+'</svg>';
}
function updateSpeakerControl(){
 const button=$('speaker');if(!button)return;
 setMediaControlIcon(button,'speaker',speakerMuted);
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
 if(connecting)throw Error('connection_already_pending');
 connecting=true;
 try{
 if(config.owner||speaking){await prepareTracks();const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;if(!fresh.can_publish){releasePrepared();throw Error('stage_unavailable')}token=fresh.token;}
 if(closed||leaving){releasePrepared();return}
 room=new Room({adaptiveStream:true,dynacast:true,audioCaptureDefaults:{echoCancellation:true,noiseSuppression:true,autoGainControl:true},videoCaptureDefaults:{resolution:{width:1280,height:720,frameRate:30},facingMode:facing},publishDefaults:{simulcast:false,videoEncoding:{maxBitrate:1500000,maxFramerate:30}}});
 let identityRefresh;
 room.on(RoomEvent.DataReceived,(_bytes,participant,_kind,topic)=>{
  if(topic!=='profile.changed'||participant||closed||leaving)return;
  clearTimeout(identityRefresh);
  identityRefresh=setTimeout(()=>refresh().catch(message),80);
 });
 room.on(RoomEvent.TrackSubscribed,(track,pub,participant)=>renderTrack(track,participant));
 room.on(RoomEvent.TrackUnsubscribed,track=>{track.detach().forEach(el=>{const tile=el.closest('.tile');el.remove();tile?.remove()});layout()});
 room.on(RoomEvent.ConnectionStateChanged,s=>{if(mediaFault)return;$('connection').classList.remove('is-ready');$('status').textContent=s===ConnectionState.Connected?((config.owner||speaking)&&!mediaReady?'جارٍ نشر الصوت والصورة…':'متصل'):s===ConnectionState.Reconnecting?'إعادة الاتصال…':'جارٍ الاتصال…'});
 room.on(RoomEvent.ConnectionQualityChanged,(q,p)=>{if(p===room.localParticipant&&['poor','lost'].includes(String(q).toLowerCase())){$('connection').classList.remove('is-ready');$('status').textContent='الشبكة ضعيفة · يجري تكييف الجودة'}});
 room.on(RoomEvent.ActiveSpeakersChanged,participants=>{activeSpeakerIds=new Set(participants.map(p=>p.identity));document.querySelectorAll('.audio-person').forEach(el=>el.classList.toggle('speaking',activeSpeakerIds.has(el.dataset.identity)))});
 room.on(RoomEvent.Disconnected,()=>{if(!closed&&!leaving)reconnect()});
 await connectWithTimeout(room,config.url,token);$('reconnectAction').hidden=true;for(const participant of room.remoteParticipants.values()){for(const publication of participant.trackPublications.values()){if(publication.isSubscribed&&publication.track)renderTrack(publication.track,participant);}}if(closed||leaving){room.removeAllListeners();await room.disconnect();releasePrepared();return}if(config.owner||speaking)await enableTracks();$('status').textContent='مباشر';
 $('follow').hidden=config.owner;$('gift').hidden=true;$('speaker').hidden=true;updateSpeakerControl();updateQuickMic();
 layout();if(!polling){polling=true;poll()}
 }catch(e){room?.removeAllListeners();try{await room?.disconnect()}finally{releasePrepared();mediaReady=false}throw e}
 finally{connecting=false}
}
async function reconnect(){if(retrying||connecting||closed||leaving)return;retrying=true;try{for(let attempt=0;attempt<6&&!closed&&!leaving;attempt++){$('connection').classList.remove('is-ready');$('status').textContent='إعادة الاتصال…';await new Promise(r=>setTimeout(r,Math.min(5000,1000*(attempt+1))));try{const fresh=await api({action:'token'});state.can_publish=fresh.can_publish;state.publish_sources=fresh.publish_sources;if(closed||leaving)return;room?.removeAllListeners();await room?.disconnect();document.querySelectorAll('audio').forEach(el=>el.remove());$('video').replaceChildren();if(state.can_publish===false&&!config.owner)speaking=false;await connect(fresh.token);return}catch(e){if(e.name==='MediaFailure'){mediaMessage(e);return}if(['broadcast_forbidden','broadcast_ended','live_authorization_required','admission_refunded','feature_restricted','broadcast_permission_required'].includes(e.message)){stop('انتهت صلاحية البث');return}}}if(!closed&&!leaving){$('status').textContent='تعذّر الاتصال. جرّب إعادة الاتصال دون شراء دخول جديد.';$('reconnectAction').hidden=false}}finally{retrying=false}}
// Android System WebView can calculate a tiny CSS dialog max-height when
// visualViewport collapses or the stage was resized by Gboard. Position the
// modal in real, measured CSS pixels instead of relying on bottom/inset/100dvh.
function positionRoomSheet(){
 const modal=$('sheet');
 if(!modal?.open)return;
 const layoutHeight=Math.max(document.documentElement.clientHeight||0,window.innerHeight||0);
 const visual=window.visualViewport;
 const usableVisual=visual && visual.height>=240 && visual.height<=layoutHeight+2;
 const height=usableVisual?Math.min(layoutHeight,visual.height):layoutHeight;
 if(height<160)return;
 const offsetTop=usableVisual?Math.max(0,visual.offsetTop||0):0;
 const inset=8;
 const sheetHeight=Math.max(180,Math.min(760,Math.floor(height*(modal.classList.contains('gift-store-open')?.96:.78))));
 const top=Math.max(inset,Math.floor(offsetTop+height-sheetHeight-inset));
 const bounds={
  position:'fixed',top:top+'px',bottom:'auto',
  left:'10px',right:'10px',margin:'0 auto',
  width:'calc(100% - 20px)',height:sheetHeight+'px',
  maxHeight:sheetHeight+'px',minHeight:'0px',
  overflowY:'auto',overscrollBehavior:'contain'
 };
 for(const [key,value] of Object.entries(bounds))modal.style.setProperty(key.replace(/[A-Z]/g,c=>'-'+c.toLowerCase()),value,'important');
 modal.scrollTop=Math.max(0,modal.scrollTop);
}
window.addEventListener('resize',positionRoomSheet,{passive:true});
window.visualViewport?.addEventListener('resize',positionRoomSheet,{passive:true});
window.visualViewport?.addEventListener('scroll',positionRoomSheet,{passive:true});
function sheet(title){
 if(title!=='متجر الهدايا'){
  activeGiftRecipientSync=null;
 }
 // Dismiss the IME before opening every room menu, not just gifts.
 if(document.activeElement?.id==='text')document.activeElement.blur();
 const modal=$('sheet');
 modal.classList.toggle('gift-store-open',title==='متجر الهدايا');
 $('sheetTitle').textContent=title;
 $('sheetBody').replaceChildren();
 if(!modal.open)modal.showModal();
 positionRoomSheet();
 modal.scrollTop=0;
 return $('sheetBody');
}
function button(box,label,fn,cls=''){const b=document.createElement('button');b.textContent=label;b.className=cls;b.onclick=async()=>{if(b.disabled)return;b.disabled=true;try{await fn()}catch(e){message(e)}finally{b.disabled=false}};box.appendChild(b);return b}
function note(box,text){const p=document.createElement('p');p.textContent=text;box.appendChild(p)}
function sectionHeading(box,title){const h=document.createElement('h3');h.className='room-setting-heading';h.textContent=title;box.append(h)}
function confirmActionSheet(title,explanation,action){
 const box=sheet(title);
 note(box,explanation);
 button(box,'إلغاء',()=>{$('sheet').close()});
 button(box,'تأكيد الإجراء',async()=>{await action();if($('sheet').open)$('sheet').close()},'danger');
}
function reportSheet(){
 const box=sheet('الإبلاغ عن البث');
 note(box,'وضح سبب البلاغ ليصل إلى فريق الإدارة. لن يُرسل شيء دون تأكيدك.');
 const input=document.createElement('textarea');
 input.id='reportReason';input.rows=4;input.maxLength=500;
 input.placeholder='اكتب سبب البلاغ (3 أحرف على الأقل)';
 input.setAttribute('aria-label','سبب البلاغ');
 input.addEventListener('input',()=>input.setCustomValidity(''));
 box.append(input);
 button(box,'إرسال البلاغ',async()=>{
  const reason=input.value.trim();
  if(reason.length<3){input.setCustomValidity('اكتب سببًا واضحًا لا يقل عن 3 أحرف');input.reportValidity();return}
  await api({action:'report',reason});
  $('sheet').close();$('status').textContent='وصل البلاغ إلى الإدارة';
 },'primary');
 button(box,'إلغاء',()=>{$('sheet').close()});
}
function commentNotice(text,isError=false){
 const el=$('commentNotice');if(!el)return;
 clearTimeout(commentNotice.timer);
 el.hidden=!text;el.textContent=text;el.classList.toggle('error',!!isError);
 if(text&&!isError&&text.startsWith('تم إرسال'))commentNotice.timer=setTimeout(()=>{el.hidden=true;el.textContent=''},2200);
}
function commentActionsSheet(comment){
 if(!state.manage_room)return;
 const box=sheet('إدارة التعليق');
 note(box,(comment.name||'مستخدم')+' · '+(comment.text||''));
 button(box,'تثبيت التعليق',async()=>{await api({action:'pin_comment',comment_id:comment.id});$('sheet').close();await refresh()});
 button(box,'حذف التعليق',()=>confirmActionSheet('حذف التعليق','سيُحذف التعليق من الغرفة بعد تأكيدك.',async()=>{await api({action:'delete_comment',comment_id:comment.id});await refresh()}),'danger');
}
$('closeSheet').onclick=()=>$('sheet').close();
$('sheet').addEventListener('close',()=>{
 activeGiftRecipientSync=null;
});$('sheet').addEventListener('click',e=>{if(e.target===$('sheet'))$('sheet').close()});
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
function giftSheet(preferredRecipientId=null){
 const selectedSeatId=Number.isInteger(preferredRecipientId)?preferredRecipientId:null;
 const box=sheet('متجر الهدايا');
 // A seat chooses the recipient; without a seat the gift goes to the host.
 // Clicking a seat NEVER sends money until the user explicitly confirms.
 const balance=document.createElement('div');balance.id='giftBalance';balance.className='gift-balance';
 balance.textContent='رصيدك: '+Number(state.available_coins||0).toLocaleString('ar')+' كوين';box.append(balance);
 const target=document.createElement('small');
 target.className='gift-target-summary';target.setAttribute('role','status');
 target.setAttribute('aria-live','polite');
 function recipientOf(snapshot){
  const speakers=snapshot.speakers||[];
  const host=speakers.find(s=>s.owner)||null;
  const chosen=selectedSeatId==null?host:speakers.find(s=>s.user_id===selectedSeatId);
  // Never silently redirect a seat gift to the host after its recipient leaves.
  // No payment can ever target the sender; this must remain server enforced too.
  return chosen&&chosen.user_id!==config.userId?chosen:null;
 }
 function updateRecipients(snapshot){
  if(!target.isConnected)return;
  const receiver=recipientOf(snapshot);
  if(receiver){
   const bySeat=selectedSeatId!=null&&receiver.user_id===selectedSeatId&&!receiver.owner;
   target.textContent='المستلم: '+receiver.name+(bySeat?' · المقعد المختار':' · المضيف');
   target.classList.remove('gift-target-unavailable');
  }else{
   target.textContent=selectedSeatId!=null?'غادر صاحب المقعد؛ أغلق المتجر واختر مستلمًا جديدًا.':'اختر مقعد متحدث آخر؛ لا يمكنك إرسال هدية لنفسك.';
   target.classList.add('gift-target-unavailable');
  }
  refreshChoice();
 }
 const preview=document.createElement('div');preview.className='gift-preview';box.append(preview);
 const previewArt=document.createElement('div');previewArt.className='gift-preview-art';preview.append(previewArt);
 const previewText=document.createElement('div');previewText.className='gift-preview-text';preview.append(previewText);
 const previewName=document.createElement('strong');previewText.append(previewName);
 const previewHint=document.createElement('small');previewHint.textContent='مجسّم حيّ متحرك · صوت مخصص لكل هدية';previewText.append(previewHint);
 const previewButton=button(previewText,'معاينة الصوت',()=>{
  if(selected&&window.RelaxiaGiftCinema){
   window.RelaxiaGiftCinema.setSound(true);
   window.RelaxiaGiftCinema.previewSound(selected.code);
  }
 },'gift-preview-sound');
 let lastPreview;
 const tabs=document.createElement('div');tabs.className='gift-tabs';box.append(tabs);
 const grid=document.createElement('div');grid.className='gift-grid';box.append(grid);
 // Show purchasable choices before any optional cinema preview: the 3D preview
 // can be unavailable on some Android GPUs without making the shop look empty.
 grid.after(preview);
 const catalog=(Array.isArray(state.gift_catalog)?state.gift_catalog:[])
  .filter(g=>/^gift_[a-z_]+$/.test(g.code))
  .sort((a,b)=>a.price_cents-b.price_cents);
 let selected=catalog[0]||null,comboCount=1,readyFingerprint=null;
 const picker=document.createElement('div');picker.className='gift-combo-picker';box.append(picker);
 const comboTitle=document.createElement('div');comboTitle.className='gift-combo-title';
 comboTitle.textContent='إرسال متتابع';picker.append(comboTitle);
 const counts=document.createElement('div');counts.className='gift-combo-counts';picker.append(counts);
 const checkout=document.createElement('div');checkout.className='gift-checkout';box.append(checkout);
 const summary=document.createElement('div');summary.className='gift-checkout-summary';checkout.append(summary,target);
 const confirmation=document.createElement('p');confirmation.className='gift-confirm-hint';confirmation.textContent='لم تُخصم أي كوينز.';checkout.append(confirmation);
 const send=button(checkout,'',async()=>{
  const recipient=recipientOf(state);
  if(!selected||!recipient)return;
  const total=selected.price_cents*comboCount;
  if(!Number.isSafeInteger(total)||total<=0||total>1000000)throw Error('invalid_gift_combo');
  const receiver=recipient.name;
  // Native two-step confirmation avoids Android's unstyled browser confirm
  // and ensures browsing, tapping a card or selecting a seat never spends.
  const fingerprint=selected.code+':'+recipient.user_id+':'+selected.price_cents+':'+comboCount;
  if(readyFingerprint!==fingerprint){
   readyFingerprint=fingerprint;
   refreshChoice();
   return;
  }
  // Each combination is one server-authorized financial operation.
  if(giftPending&&giftPending!==fingerprint)throw Error('pending_gift_retry');
  giftPending=fingerprint;giftKey=giftKey||crypto.randomUUID();
  try{
   const result=await api({action:'gift',gift_code:selected.code,recipient_id:recipient.user_id,
       price_cents:selected.price_cents,combo_count:comboCount,confirmed:true,request_key:giftKey});
   giftKey=null;giftPending=null;readyFingerprint=null;
   drawState(result);
   $('sheet').close();
  }catch(error){
   if(['insufficient_balance','price_confirmation_required','gift_unavailable','invalid_gift_combo','payment_request_conflict'].includes(error.message)){
    giftKey=null;giftPending=null;readyFingerprint=null;refreshChoice();
   }
   throw error;
  }
 },'primary gift-send');
 function refreshChoice(){
  const cost=selected?selected.price_cents*comboCount:0;
  const recipient=recipientOf(state),current=selected&&recipient
   ?selected.code+':'+recipient.user_id+':'+selected.price_cents+':'+comboCount:null;
  if(readyFingerprint!==current)readyFingerprint=null;
  summary.textContent=selected
    ?selected.name+' ×'+comboCount+' · '+cost.toLocaleString('ar')+' كوين'
    :'لا توجد هدايا متاحة';
  send.textContent=!recipient?'اختر مستلمًا':readyFingerprint===current?'إرسال الآن':'مراجعة الإرسال';
  send.disabled=!selected||!recipient||cost>1000000||(giftPending&&giftPending!==current);
  confirmation.textContent=current!==null&&readyFingerprint===current
   ?'تأكيد: '+selected.name+' إلى '+recipient.name+' بمبلغ '+cost.toLocaleString('ar')+' كوين.'
   :'راجع المستلم والسعر. لا يُخصم الرصيد إلا بعد الضغط على «إرسال الآن».';
  if(selected?.code!==lastPreview){
   previewName.textContent=selected?.name||'اختر هدية';
   previewButton.disabled=!selected;
   preview.hidden=!selected;
   previewArt.replaceChildren();
   if(selected){
    const icon=document.createElement('img');
    icon.alt=selected.name;
    icon.onerror=()=>previewArt.replaceChildren(document.createTextNode(selected.symbol));
    icon.src=selected.icon_url;
    previewArt.append(icon);
   }
   lastPreview=selected?.code||'';
  }
  for(const el of grid.children)el.setAttribute('aria-pressed',String(el.dataset.giftCode===selected?.code));
  for(const el of counts.children)el.setAttribute('aria-pressed',String(Number(el.dataset.comboCount)===comboCount));
 }
 for(const n of [1,10,66,99]){
  const b=button(counts,'×'+n,()=>{comboCount=n;refreshChoice()});
  b.dataset.comboCount=String(n);
 }
 function render(category){
  grid.replaceChildren();
  for(const tab of tabs.children)tab.setAttribute('aria-pressed',String(tab.dataset.category===category));
  const filtered=catalog.filter(g=>category==='all'||g.category===category);
  if(!filtered.some(g=>g.code===selected?.code))selected=filtered[0]||null;
  for(const g of filtered){
   const b=button(grid,'',()=>{selected=g;comboCount=1;readyFingerprint=null;refreshChoice()});
   // Browse safely even when no eligible target exists; checkout stays locked.
   b.className='gift-card '+g.effect+(g.price_cents>=1000?' gift-premium':'');
   b.dataset.giftCode=g.code;
   b.setAttribute('aria-label',g.name+' · '+g.price_cents+' كوين');
   const medallion=document.createElement('span');medallion.className='gift-medallion';
   const icon=document.createElement('img');
   icon.className='gift-webp';icon.alt='';icon.loading='lazy';icon.decoding='async';
   icon.onerror=()=>medallion.replaceChildren(document.createTextNode(g.symbol||g.name));
   icon.src=g.icon_url;medallion.append(icon);
   b.classList.add('gift-'+String(g.code).replace(/[^a-z0-9_-]/g,''));
   const name=document.createElement('strong');name.textContent=g.name;
   const price=document.createElement('small');price.className='gift-price';price.textContent=g.price_cents.toLocaleString('ar')+' كوين';
   const coin=document.createElement('span');coin.className='gift-price-coin';coin.setAttribute('aria-hidden','true');
   price.append(coin);
   b.append(medallion,name,price);
  }
  if(!filtered.length)note(grid,'لا توجد هدايا متاحة في هذا القسم.');
  refreshChoice();
 }
 for(const [key,label] of [['all','الكل'],['simple','سريعة'],['animated','متحركة'],['luxury','فاخرة']]){
  const tab=button(tabs,label,()=>render(key));
  tab.dataset.category=key;
 }
 render('all');
 activeGiftRecipientSync=updateRecipients;
 updateRecipients(state);
 // Supporter rankings belong in the room's information menu, not the checkout.
 // Keeping the gift store focused prevents accidental scrolling past pricing.
}
function drawAudioSeats(result){
 const box=$('audioStage'),speakers=result.speakers||[],capacity=8;
 // The host is fixed in the first RTL slot. Approved guest seats must retain
 // their assigned number even when another speaker leaves or a slot is locked.
 const positions=Array(capacity).fill(null);
 positions[0]=speakers.find(s=>s.owner)||null;
 for(const guest of speakers.filter(s=>!s.owner)){
  let slot=Number(guest.seat_number);
  if(!Number.isInteger(slot)||slot<1||slot>=capacity||positions[slot]){
   slot=positions.findIndex((value,i)=>i>0&&!value);
  }
  if(slot>0&&slot<capacity)positions[slot]=guest;
 }
 const signature=JSON.stringify([capacity,positions.map(s=>s&&[s.identity,s.name,s.owner,s.avatar_url,s.selected_frame,s.level,s.seat_number]),result.room_settings?.locked_seat_numbers||[]]);
 if(signature===audioSeatSignature&&box.querySelectorAll('.audio-seat').length===capacity){
  box.querySelectorAll('.audio-seat[data-identity]').forEach(item=>item.classList.toggle('speaking',activeSpeakerIds.has(item.dataset.identity)));
  return;
 }
 audioSeatSignature=signature;
 box.classList.add('seat-grid');box.classList.toggle('solo-room',capacity===1);
 const nodes=document.createDocumentFragment();
 for(let i=0;i<capacity;i++){
  const speaker=positions[i],item=document.createElement('button');item.type='button';item.className='audio-person audio-seat';
  item.dataset.seatNumber=String(i);
  item.classList.toggle('seat-locked',(result.room_settings?.locked_seat_numbers||[]).includes(i));
  // Occupied seat = choose gift recipient and open store; empty seat = request mic.
  item.onclick=speaker&&speaker.user_id!==config.userId
   ?()=>giftSheet(speaker.user_id)
   :guestSheet;
  if(speaker){
   item.dataset.userId=String(speaker.user_id);item.dataset.seatNumber=String(speaker.seat_number||i);item.dataset.identity=speaker.identity;item.classList.toggle('speaking',activeSpeakerIds.has(speaker.identity));item.classList.toggle('host-seat',!!speaker.owner);
   item.setAttribute('aria-label',speaker.name+(speaker.owner?' · صاحب الغرفة':'')+(speaker.user_id!==config.userId?' · إهداء':''));
   if(speaker.user_id!==config.userId)item.title='إرسال هدية إلى '+speaker.name;
   const waves=document.createElement('div');waves.className='audio-waves';waves.setAttribute('aria-hidden','true');
   for(let n=0;n<3;n++)waves.append(document.createElement('span'));item.append(waves);
   const portrait=document.createElement('span');
   portrait.className='room-portrait';
   renderPortrait(portrait,speaker);
   item.append(portrait);
   const label=document.createElement('span');label.className='audio-name';label.textContent=speaker.name;item.append(label);
   const role=document.createElement('small');role.textContent=(speaker.owner?'المضيف':'مقعد '+i)+(config.gifts&&speaker.user_id!==config.userId?' · إهداء':'');item.append(role);
  }else{
   item.disabled=item.classList.contains('seat-locked');
   item.classList.add('empty-seat');item.dataset.seatNumber=String(i);
   item.setAttribute('aria-label','مقعد '+i+(item.classList.contains('seat-locked')?' مقفل':' · طلب الميكروفون'));
   const circle=document.createElement('span');circle.className='seat-plus';circle.setAttribute('aria-hidden','true');circle.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 10v2a7 7 0 0 0 11.3 5.5M12 19v2M8.5 21h7M19 11v6M16 14h6"/></svg>';
   // Empty seats show one clear action; the seat number remains in aria-label.
   item.append(circle);
  }
  nodes.append(item);
 }
 box.replaceChildren(nodes);
}
function updateQuickMic(){
 const button=$('micQuick');if(!button)return;
 // Viewers already have the guest seat button. Only speakers need a quick mic.
 button.hidden=!speaking;
 setMediaControlIcon(button,'mic',muted);
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
function moreSheet(){
 const box=sheet('إعدادات وأدوات البث');
 sectionHeading(box,'الصوت والمقاعد');
 if(config.mode==='audio')button(box,speakerMuted?'تشغيل صوت الغرفة':'كتم صوت الغرفة',()=>{$('sheet').close();$('speaker').click()});
 if(speaking){
  button(box,muted?'تشغيل ميكروفوني':'كتم ميكروفوني',()=>{$('sheet').close();$('mute').click()});
  if((state.publish_sources||[]).includes('camera')){
   button(box,cameraOff?'تشغيل الكاميرا':'إيقاف الكاميرا',()=>{$('sheet').close();$('camera').click()});
   button(box,'تبديل الكاميرا',()=>{$('sheet').close();$('switch').click()});
   button(box,'جودة الفيديو · '+quality+'p',qualitySheet);
  }
 }
 button(box,'المقاعد وطلبات التحدث',guestSheet);
 sectionHeading(box,'المؤثرات والهدايا');
 if(window.RelaxiaGiftCinema){
  button(box,window.RelaxiaGiftCinema.soundEnabled()?'كتم أصوات الهدايا':'تشغيل أصوات الهدايا',()=>{
   const next=window.RelaxiaGiftCinema.setSound(!window.RelaxiaGiftCinema.soundEnabled());
   const message=document.createElement('p');
   message.textContent=next?'تم تشغيل أصوات الهدايا.':'تم كتم أصوات الهدايا.';
   box.append(message);
   $('sheet').close();
  });
 }
 sectionHeading(box,'التعليقات والتفاعل');
 button(box,commentsVisible?'إخفاء التعليقات':'إظهار التعليقات',()=>{
  commentsVisible=!commentsVisible;
  document.body.classList.toggle('comments-hidden',!commentsVisible);
  $('messages').hidden=!commentsVisible;
  $('pinned').hidden=!commentsVisible||!state.pinned_comment;$('sheet').close()
 });
 if(window.innerWidth<=380)button(box,'الرموز التعبيرية',()=>{$('sheet').close();$('emojiBtn').click()});
 button(box,state.liked?'أعجبك هذا البث':'إعجاب بالبث',()=>api({action:'like'}).then(refresh));
 if(!config.owner)button(box,state.following?'إلغاء متابعة المضيف':'متابعة المضيف',()=>api({action:'follow'}).then(refresh));
 button(box,'مشاركة الغرفة',()=>{$('sheet').close();$('share').click()});
 sectionHeading(box,'الإدارة والمعلومات');
 if(state.manage_room)button(box,'المشاهدون والمشرفون',peopleSheet);
 if(config.owner&&state.speakers?.length>=2)button(box,'منافسة PK',pkSheet);
 button(box,'إحصائيات البث',()=>{
  const b=sheet('إحصائيات البث');
  note(b,'المشاهدون المتصلون الآن: '+(state.viewers||0));
  note(b,'الإعجابات: '+(state.likes||0));
  note(b,'قيمة الهدايا: '+(state.gift_total_cents||0));
  if(config.owner)note(b,'حصة مستلمي الهدايا المستحقة: '+(state.earned_cents||0));
  for(const p of state.top_supporters||[])note(b,p.name+' · '+p.total);
 });
 sectionHeading(box,'الاتصال والجودة');
 button(box,'حالة الاتصال',()=>{
  const b=sheet('حالة الاتصال');
  const connected=room?.state===ConnectionState.Connected;
  note(b,'حالة الغرفة: '+(connected?'متصل':'غير متصل أو يعيد الاتصال'));
  if(config.owner||speaking)note(b,'نشر الصوت أو الفيديو: '+(mediaReady?'جاهز':'قيد التجهيز أو متوقف'));
  note(b,'إعدادات الصوت: '+(speakerMuted?'صوت الغرفة مكتوم':'صوت الغرفة يعمل'));
  if(!connected)button(b,'إعادة محاولة الاتصال',()=>{$('sheet').close();$('reconnectAction').click()},'primary');
 });
 sectionHeading(box,'السلامة');
 button(box,'الإبلاغ عن البث',reportSheet);
 if(config.owner)button(box,'إنهاء البث للجميع',()=>confirmActionSheet(
  'إنهاء البث','سيُغلق البث لجميع المشاهدين. لن يتم تسجيل البث أو حفظه.',
  async()=>{await api({action:'end'});stop('انتهى البث')}
 ),'danger');
}
function qualitySheet(){const box=sheet('جودة الفيديو');note(box,'تتكيّف جودة النقل تلقائيًا. 1080p تعتمد على الكاميرا والجهاز والشبكة.');for(const q of [360,720,1080])button(box,q+'p',async()=>{const track=room.localParticipant.getTrackPublication(Track.Source.Camera)?.track;if(!track)throw Error('camera_unavailable');await track.restartTrack({resolution:{width:Math.round(q*16/9),height:q,frameRate:q===360?20:30},facingMode:facing});quality=q;$('sheet').close()})}
function playEffects(){
 if(effectBusy||!giftQueue.length||closed||leaving)return;
 effectBusy=true;
 const g=giftQueue.shift(),box=$('giftEffect');
 box.replaceChildren();
 const effect=['simple','animated','depth'].includes(g.effect)?g.effect:'simple';
 box.className='gift-show gift-cinematic '+effect;
 if(effect==='depth')box.classList.add('gift-fullscreen');
 const activeSeat=(state.speakers||[]).find(s=>s.name===g.recipient);
 const targetSeat=activeSeat&&Array.from(document.querySelectorAll('#audioStage .audio-seat[data-identity]'))
  .find(el=>el.dataset.identity===activeSeat.identity);
 if(targetSeat)targetSeat.classList.add('gift-seat-glow');
 box.hidden=false;
 let dispose=null;
 try{
  if(window.RelaxiaGiftCinema){
   dispose=window.RelaxiaGiftCinema.play(box,g);
  }else{
   const fallback=document.createElement('strong');
   fallback.className='gift-cinema-fallback';
   fallback.textContent=g.sender+' أرسل '+g.name+' إلى '+g.recipient;
   box.append(fallback);
  }
 }catch(error){
  console.warn('Gift animation unavailable',error);
  const fallback=document.createElement('strong');
  fallback.textContent=g.sender+' أرسل '+g.name;
  box.append(fallback);
 }
 const reduced=window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
 const duration=reduced?1100:effect==='simple'?2300:effect==='depth'?4400:3400;
 setTimeout(()=>{
  dispose?.();box.replaceChildren();box.hidden=true;
  if(targetSeat)targetSeat.classList.remove('gift-seat-glow');
  effectBusy=false;
  playEffects();
 },duration);
}
function identityLabel(user){
 const parts=[user.name];
 if(Number.isInteger(user.level)&&user.level>0)parts.push('Lv '+user.level);
 if(user.vip_tier>0)parts.push('VIP '+user.vip_tier);
 if(user.identity_verified)parts.push('✓');
 return parts.join(' · ');
}

// Aperture centres and radii are measured in the Android frame atlas cells.
const frameOpenings=[[.5488,.4829,.3149],[.5341,.4888,.3054],[.4996,.518,.2887],
 [.4531,.496,.2789],[.4393,.5042,.292],[.5358,.4702,.276],[.5284,.4652,.269],
 [.4773,.4833,.2772],[.4539,.4785,.258],[.4342,.4766,.2601]];
function renderPortrait(node,user){
 const key=JSON.stringify([user.name,user.avatar_url,user.selected_frame]);
 if(node.dataset.portrait===key)return;
 node.dataset.portrait=key;
 node.classList.add('room-portrait');
 const face=document.createElement('span');
 face.className='portrait-face';
 face.textContent=Array.from(user.name||'')[0]||'';
 node.replaceChildren(face);
 if(/^https:\/\//.test(user.avatar_url)){
  const photo=document.createElement('img');
  photo.alt='';
  photo.onerror=()=>photo.remove();
  photo.src=user.avatar_url;
  face.append(photo);
 }
 const index=(user.selected_frame-110)/10;
 const opening=frameOpenings[index];
 if(!opening)return;
 const [x,y,radius]=opening;
 face.style.left=(x*100)+'%';
 face.style.top=(y*100)+'%';
 face.style.width=(radius*205)+'%';
 const ornament=document.createElement('span');
 ornament.className='portrait-frame';
 ornament.style.backgroundPosition=(index%5*25)+'% '+(Math.floor(index/5)*100)+'%';
 node.append(ornament);
}
function drawState(result){state=result;
 if($('sheet')?.open&&$('sheetTitle')?.textContent==='متجر الهدايا'&&activeGiftRecipientSync)activeGiftRecipientSync(result);
 const wallet=document.getElementById('giftBalance');if(wallet)wallet.textContent='رصيدك المتاح: '+Number(result.available_coins||0).toLocaleString('ar')+' كوين';updateQuickMic();document.querySelectorAll('.tile').forEach(t=>{const speaker=result.speakers?.find(s=>s.identity===t.dataset.identity);if(speaker)t.querySelector('small').textContent=speaker.name});serverOffset=Date.parse(result.server_time)-Date.now();const vCount=result.viewers||0;if($('viewerCount'))$('viewerCount').textContent=vCount>=1000?(vCount/1000).toFixed(1)+'K':vCount;else $('viewers').textContent=vCount+' مشاهد';$('viewers').title=vCount+' مشاهد';$('follow').textContent=result.following?'تتابعه':'متابعة';const host=result.speakers?.[0];$('badge').textContent=host&&Number(host.level)>0?('المستوى '+Math.floor(Number(host.level))+(host.vip?' · VIP':'')):(host?.vip?'VIP':'');
 if(host){
  $('name').textContent=host.name;
  renderPortrait($('profile').querySelector('.identity-avatar-ring'),host);
 }
 const headerLevel=$('hostLevel');
 if(headerLevel){
  const level=host&&Number(host.level)>=1?Math.floor(Number(host.level)):0;
  headerLevel.hidden=!level&&!host?.vip;
  headerLevel.textContent=level?'المستوى '+level+(host.vip?' · VIP':''):'VIP';
 }
 const requestMic=$('requestMic');
 if(requestMic){
  requestMic.hidden=!!speaking;
  requestMic.textContent=result.seat_status==='pending'?'طلب الانضمام قيد المراجعة':'طلب الانضمام';
 }
 const signature=JSON.stringify([!!result.manage_room,result.comments||[]]);if(lastComments!==signature){lastComments=signature;const box=$('messages');const nearBottom=box.scrollHeight-box.clientHeight-box.scrollTop<38,previousScroll=box.scrollTop;box.replaceChildren();for(const c of result.comments||[]){const p=document.createElement('div');p.className='comment-bubble';const av=document.createElement('div');av.className='comment-avatar';renderPortrait(av,c);const body=document.createElement('div');body.className='comment-body';const name=document.createElement('span');name.className='comment-author';name.textContent=c.name;
 if(Number.isInteger(c.level)&&c.level>0){
  const badge=document.createElement('small');badge.className='public-level';
  badge.textContent='Lv '+c.level;name.append(badge);
 }
 if(c.vip_tier>0)name.append(' VIP '+c.vip_tier);
 if(c.identity_verified)name.append(' ✓');
 name.append(' · ');body.append(name,document.createTextNode(c.text));p.dataset.userId=String(c.user_id);p.dataset.commentId=String(c.id);p.append(av,body);if(result.manage_room){const manage=document.createElement('button');manage.type='button';manage.className='comment-actions';manage.textContent='⋯';manage.setAttribute('aria-label','إدارة تعليق '+(c.name||''));manage.onclick=()=>commentActionsSheet(c);p.append(manage)}box.append(p)}if(!(result.comments||[]).length){const blank=document.createElement('p');blank.className='comment-empty';blank.textContent='لا توجد تعليقات بعد · ابدأ الحوار';box.append(blank)}box.scrollTop=nearBottom?box.scrollHeight:Math.min(previousScroll,Math.max(0,box.scrollHeight-box.clientHeight))}
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
  $('connection').classList.remove('is-ready');
  if(mediaFault){
   // Preserve the permission error; room heartbeat must not overwrite it.
   $('connection').classList.add('media-fault');
   $('retryMedia').hidden=false;
   $('reconnectAction').hidden=true;
  }else{
   if(!reconnectSince)reconnectSince=Date.now();
   const late=Date.now()-reconnectSince>=12000;
   $('status').textContent=result.status==='starting'?(late?'تأخر بدء البث. يمكنك إعادة الاتصال.':'الغرفة تبدأ الآن…'):(late?'صاحب البث يعيد الاتصال. أعد المحاولة.':'صاحب البث يعيد الاتصال…');
   $('reconnectAction').hidden=connecting||retrying||!late;
  }
 }else{
  reconnectSince=0;$('reconnectAction').hidden=true;
  if(!mediaFault&&room?.state===ConnectionState.Connected&&(!(config.owner||speaking)||mediaReady)){$('status').textContent='مباشر';$('connection').classList.add('is-ready')}else $('connection').classList.remove('is-ready');
 }
 return result;
}
async function poll(){while(!closed){
 try{await refresh()}
 catch(e){
  if(e.name==='MediaFailure'){mediaMessage(e);return}
  if(['broadcast_forbidden','broadcast_ended','live_authorization_required','admission_refunded'].includes(e.message)){stop('انتهت صلاحية البث');break}
  controlFailures++;
  if(!mediaFault){
   $('connection').classList.remove('is-ready');
   $('status').textContent='تعذّر تحديث الغرفة. نعيد المحاولة…';
   $('reconnectAction').hidden=controlFailures<3;
  }
 }
 await new Promise(r=>setTimeout(r,3000))
}}
let sendingComment=false;
$('composer').onsubmit=async e=>{
 e.preventDefault();
 const input=$('text'),content=input.value.trim(),send=$('sendComment');
 if(sendingComment||!content)return;
 sendingComment=true;send.disabled=true;$('composer').classList.add('is-sending');
 commentNotice('جارٍ إرسال التعليق…');
 try{
  const result=await api({action:'comment',text:content});
  // Successful POST already contains the saved comment and latest room state.
  // A second fetch is unnecessary and may fail after a successful write.
  // Do not erase a second message typed while the first POST was in flight.
  if(input.value.trim()===content)input.value='';
  drawState(result);
  commentNotice('تم إرسال التعليق');
 }catch(error){
  const reason=error?.message||'';
  const detail=reason==='slow_down'?'أرسلت تعليقات كثيرة. حاول بعد قليل.'
   :reason==='live_authorization_required'?'انتهت صلاحية الغرفة؛ أعد الدخول دون دفع جديد.'
   :error?.name==='AbortError'?'انقطع الاتصال. تحقق من ظهور التعليق قبل إعادة المحاولة.'
   :'تعذر إرسال التعليق. احتفظنا بنصك؛ حاول مجددًا.';
  commentNotice(detail,true);
 }finally{
  sendingComment=false;send.disabled=false;$('composer').classList.remove('is-sending');
 }
};
$('text').setAttribute('enterkeyhint','send');
$('text').addEventListener('keydown',event=>{
 if(event.key==='Enter'&&!event.isComposing&&event.keyCode!==229){
  event.preventDefault();
  if(!$('sendComment').disabled){
   const form=$('composer');
   if(typeof form.requestSubmit==='function')form.requestSubmit($('sendComment'));
   else $('sendComment').click(); // Compatibility for older Android WebView.
  }
 }
});
$('text').addEventListener('input',()=>commentNotice(''));
$('text').addEventListener('focus',()=>{
 requestAnimationFrame(()=>{updateRoomViewport();syncRoomFooterMetrics()});
});
$('text').addEventListener('blur',()=>{
 document.body.classList.remove('ime-open','chat-focused');
 requestAnimationFrame(()=>{updateRoomViewport();syncRoomFooterMetrics()});
});
// Keep microphone and camera independent, including during rapid taps.
let micToggleBusy=false;
$('mute').onclick=async()=>{
 if(micToggleBusy||!speaking||!room?.localParticipant)return;
 micToggleBusy=true;
 const button=$('mute'),quick=$('micQuick');
 button.disabled=true;if(quick)quick.disabled=true;
 try{
  const targetEnabled=muted;
  const enabled=await RelaxiaMedia.setMicrophoneOnly(room.localParticipant,LivekitClient,targetEnabled);
  muted=!enabled;
  button.textContent=muted?'تشغيل الميكروفون':'كتم الميكروفون';
  button.setAttribute('aria-label',button.textContent);
  button.setAttribute('aria-pressed',String(!muted));
  updateQuickMic();
  // This is only feedback: the camera and its preview are never modified here.
  commentNotice(muted?'تم كتم الميكروفون · الفيديو مستمر':'تم تشغيل الميكروفون');
 }catch(error){
  if(error?.stage==='microphone_toggle'){
   $('connection').classList.remove('is-ready');
   $('status').textContent='الميكروفون غير جاهز؛ الفيديو لم يتوقف. أعد الاتصال إن استمرت المشكلة.';
  }else message(error);
 }finally{
  micToggleBusy=false;button.disabled=false;if(quick)quick.disabled=false;
 }
};
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
if($('requestMic'))$('requestMic').onclick=()=>guestSheet();
$('more').onclick=moreSheet;
$('share').onclick=async()=>{const text='انضم إلى بث '+config.ownerName+' «'+config.title+'» على Relaxia. افتح قسم البث المباشر وابحث عن عنوان البث. الدخول الخاص يتطلب تصريحًا.';try{if(window.LayaliLive?.shareLive){window.LayaliLive.shareLive(text);$('status').textContent='نُسخت دعوة البث؛ يمكنك إرسالها إلى أصدقائك.';}else if(navigator.share)await navigator.share({title:'Relaxia · مباشر',text});else if(navigator.clipboard){await navigator.clipboard.writeText(text);$('status').textContent='نُسخت دعوة البث'}else prompt('انسخ دعوة البث',text)}catch(e){if(e.name!=='AbortError')message(e)}};
$('reconnectAction').onclick=()=>{if(connecting||retrying)return;$('reconnectAction').hidden=true;reconnectSince=Date.now();reconnect()};
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
 max_speakers:8,
 speakers:[{identity:'host-'+config.ownerId,name:config.ownerName,owner:true,avatar_url:config.ownerAvatar||''}]
});
// Load seats, audience and chat before LiveKit connects, including recovery states.
if(!polling){polling=true;poll()}
window.addEventListener('pagehide',()=>{leaving=true;releasePrepared();room?.disconnect()});connect().catch(e=>{message(e);if(e.name!=='MediaFailure')reconnect()});

// Android background privacy: release camera only when the app really stops,
// not when the user taps microphone controls. Restore and reattach on resume.
let nativeCameraWasOn=false,nativeBackgroundTransition=Promise.resolve();
window.setNativeBackground=hidden=>{
 nativeBackgroundTransition=nativeBackgroundTransition.catch(()=>{}).then(async()=>{
  const local=room?.localParticipant;
  if(!local||config.mode==='audio'||!speaking)return;
  if(hidden){
   if(cameraOff||nativeCameraWasOn)return;
   nativeCameraWasOn=!!local.isCameraEnabled;
   if(nativeCameraWasOn)await local.setCameraEnabled(false);
  }else if(nativeCameraWasOn){
   if(cameraOff){nativeCameraWasOn=false;return;}
   await local.setCameraEnabled(true);
   nativeCameraWasOn=false;
   // LiveKit may replace the video track on Android after backgrounding.
   const track=local.getTrackPublication(Track.Source.Camera)?.track;
   if(track&&!closed&&!leaving)renderTrack(track,local,true);
  }
 }).catch(error=>{message(error)});
 return nativeBackgroundTransition;
};
