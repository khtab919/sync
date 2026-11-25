/* Relaxia room moderation: Material-bottom-sheet actions on real participants.
   The Django/LiveKit server is the sole permission authority; DOM buttons are hints.
   Loaded after live-stage.js; no external SDK or direct Firestore client writes. */
(function(){
 'use strict';
 const ignoredKey='relaxia-local-ignore-v2';
 let ignored=new Set();
 try{ignored=new Set(JSON.parse(localStorage.getItem(ignoredKey)||'[]').filter(Number.isInteger))}catch(_){}
 let giftOff=false,clean=false;
 const originalDraw=window.drawState;
 if(typeof originalDraw==='function'){
   window.drawState=function(result){
     const input=result&&Array.isArray(result.comments)?result.comments:[];
     const filtered={...result,comments:input.filter(c=>!ignored.has(Number(c.user_id)))};
     originalDraw(filtered);
     const card=document.querySelector('#sheet[open] .member-identity');
     if(card){
       const person=[...(filtered.speakers||[]),...filtered.comments,...(filtered.participants_visible||[])]
         .find(p=>String(p.user_id)===card.dataset.userId);
       if(person)updateMember(card,person);
     }
     const allow=filtered.room_settings?.chat_enabled!==false||!!filtered.manage_room;
     if($('text')){$('text').disabled=!allow;$('text').placeholder=allow?'اكتب تعليقًا…':'الدردشة مغلقة بقرار المضيف'}
     if($('sendComment'))$('sendComment').disabled=!allow;
     const settings=filtered.room_settings||{};
     for(const el of document.querySelectorAll('#audioStage .audio-seat')){
       const num=Number(el.dataset.seatNumber);
       if(num>0)el.classList.toggle('seat-locked',(settings.locked_seat_numbers||[]).includes(num));
     }
   };
 }
 const originalEffects=window.playEffects;
 if(typeof originalEffects==='function'){
   window.playEffects=function(){
     if(giftOff){giftQueue.length=0;return}
     return originalEffects();
   };
 }
 const originalPeople=window.peopleSheet;
 if(typeof originalPeople==='function'){
   window.peopleSheet=function(){
     if(window.RelaxiaAudience){window.RelaxiaAudience.open();return}
     const box=sheet('المشاهدون والمشاركون');
     for(const p of state.participants_visible||[]){
       if(Number(p.user_id)!==config.userId)
         button(box,p.name||'مستخدم',()=>showUser(p), 'room-user-entry');
     }
     if(!(state.participants_visible||[]).length)note(box,'لا يوجد مشاركون متصلون حاليًا.');
   };
 }
 const originalMore=window.moreSheet;
 if(typeof originalMore==='function'){
   window.moreSheet=function(){
     originalMore();
     const body=$('sheetBody');
     sectionHeading(body,'الخصوصية وإدارة الغرفة');
     button(body,clean?'إظهار واجهة البث':'وضع الشاشة النظيفة',()=>{
       clean=!clean;document.body.classList.toggle('relaxia-clean',clean);$('sheet').close();
     });
     button(body,giftOff?'تشغيل الحركات البصرية':'إيقاف حركات الهدايا',()=>{
       giftOff=!giftOff;
       if(giftOff){window.RelaxiaGiftCinema?.stop();if($('giftEffect'))$('giftEffect').hidden=true}
       $('sheet').close();
     });
     button(body,'قائمة المشاركين وإجراءات المستخدمين',()=>window.peopleSheet());
     if(['host','super_admin'].includes(state.current_role)){
       button(body,'إعدادات المضيف والمقاعد والدردشة',roomSettings,'primary');
     }
   };
 }
 function reportUser(user){
   const box=sheet('إبلاغ عن مستخدم');
   note(box,'سيصل بلاغك إلى إدارة Relaxia، مع رقم المستخدم والغرفة ووقت الإبلاغ.');
   const field=document.createElement('textarea');
   field.placeholder='اكتب سبب البلاغ';field.maxLength=500;field.rows=4;
   field.setAttribute('aria-label','سبب الإبلاغ');box.append(field);
   button(box,'إرسال البلاغ',async()=>{
     const reason=field.value.trim();
     if(reason.length<3){field.setCustomValidity('اكتب سببًا لا يقل عن ثلاثة أحرف');field.reportValidity();return}
     await api({action:'report_user',user_id:Number(user.user_id),reason});
     $('sheet').close();commentNotice('وصل بلاغك إلى الإدارة');
   },'primary');
 }
 function confirmed(title,detail,action){
   confirmActionSheet(title,detail,async()=>{await action();await refresh()});
 }
 function updateLocalBlock(id){
   if(ignored.has(id))ignored.delete(id);else ignored.add(id);
   try{localStorage.setItem(ignoredKey,JSON.stringify([...ignored]))}catch(_){}
   lastComments='';window.drawState(state);$('sheet').close();
 }
 function updateMember(card,user){
   renderPortrait(card.firstElementChild,user);
   card.lastElementChild.textContent=identityLabel(user);
 }
 function showUser(user,message=null){
   const id=Number(user?.user_id);
   if(!Number.isInteger(id)||id<1)return;
   const box=sheet('ملف العضو');
   const card=document.createElement('strong');
   card.className='member-identity';
   card.dataset.userId=String(id);
   card.append(document.createElement('span'),document.createElement('span'));
   updateMember(card,user);
   box.append(card);
   if(message)note(box,message.text);
   if(id===config.userId){note(box,'هذه صفحتك الشخصية.');return}
   sectionHeading(box,'الخيارات الشخصية');
   if(config.gifts&&(user.connected||[...(state.speakers||[]),...(state.participants_visible||[])].some(s=>s.user_id===id)))button(box,'إرسال هدية لهذا المستخدم',()=>giftSheet(id,user));
   let following=user.following===true||(state.followed_ids||[]).includes(id);
   const follow=button(box,following?'إلغاء المتابعة':'متابعة +',async()=>{
     const updated=await api({action:'follow_member',user_id:id,following:!following});
     following=!following;
     follow.textContent=following?'إلغاء المتابعة':'متابعة +';
     window.drawState(updated);
   });
   if(window.LayaliLive?.messageMember&&config.mode==='audio'){
     button(box,'مراسلة',()=>{$('sheet').close();window.LayaliLive.messageMember(id,user.name||'')});
   }
   button(box,ignored.has(id)?'إلغاء التجاهل المحلي':'إخفاء تعليقات المستخدم لدي فقط',()=>updateLocalBlock(id));
   button(box,'الإبلاغ عن المستخدم',()=>reportUser(user));
   if(!state.manage_room||id===config.ownerId||user.protected)return;
   sectionHeading(box,'إدارة الغرفة');
   const onSeat=(state.speakers||[]).some(s=>s.user_id===id&&!s.owner);
   button(box,onSeat?'إنزال من المقعد':'دعوة إلى المايك',()=>{
     confirmed('إدارة المقعد','يُمنح إذن التحدث، لكن لا يُفتح ميكروفون جهاز المستخدم إلا بموافقته.',
       ()=>api({action:onSeat?'remove_from_seat':'invite_to_seat',user_id:id}));
   });
   if(onSeat){
     const isMuted=!!(state.speakers||[]).find(s=>s.user_id===id)?.forced_mic_mute;
     button(box,isMuted?'رفع منع المايك':'كتم صوت المتحدث من الغرفة',()=>{
       confirmed('التحكم بالمايك','لفرض الكتم على الخادم قد يُعاد اتصال المتحدث. لا يتم تشغيل المايك قسريًا.',
         ()=>api({action:isMuted?'release_mic_mute':'force_mute_mic',user_id:id}));
     });
   }
   const duration=document.createElement('select');
   duration.className='room-moderation-select';
   duration.setAttribute('aria-label','مدة كتم الدردشة');
   for(const [v,label] of [['10m','10 دقائق'],['1h','ساعة'],['stream','حتى نهاية البث']])duration.add(new Option(label,v));
   box.append(duration);
   button(box,'كتم دردشة المستخدم',async()=>{
     await api({action:'mute_chat',user_id:id,duration:duration.value});
     $('sheet').close();await refresh();
   },'danger');
   button(box,'إلغاء كتم الدردشة',async()=>{
     await api({action:'unmute_chat',user_id:id});$('sheet').close();await refresh();
   });
   if(message?.id)button(box,'حذف هذا التعليق للجميع',()=>{
     confirmed('حذف التعليق','سيحذف التعليق في هذه الغرفة لجميع المشاهدين.',
       ()=>api({action:'delete_comment',comment_id:message.id}));
   },'danger');
   button(box,'طرد من هذا البث',()=>confirmed('طرد المستخدم',
     'سيُفصل المستخدم من الغرفة فورًا، ولن يستطيع العودة إلى الجلسة الحالية.',
     ()=>api({action:'kick_user',user_id:id,reason:'مخالفة قواعد البث'})),'danger');
   if(['host','super_admin'].includes(state.current_role)){
     const mod=(state.moderator_ids||[]).includes(id);
     button(box,mod?'إزالة صفة المشرف':'تعيين مشرف غرفة',async()=>{
       await api({action:'set_room_moderator',user_id:id,enabled:!mod});$('sheet').close();await refresh()
     });
     button(box,'حظر دائم من بثوث المضيف',()=>confirmed('حظر بثوث المضيف',
       'سيُمنع هذا المستخدم من دخول أي بث جديد للمضيف نفسه.',
       ()=>api({action:'ban_from_host_rooms',user_id:id,reason:'مخالفة قواعد المضيف'})),'danger');
     button(box,'رفع الحظر من بثوث المضيف',async()=>{
       await api({action:'remove_host_ban',user_id:id});$('sheet').close();await refresh()
     });
   }
   if(state.current_role==='super_admin'){
     sectionHeading(box,'الإدارة العليا');
     note(box,'تعليق الحساب الكامل أو حظره يتم من لوحة إدارة المسؤول بسجل تدقيق رسمي.');
     button(box,'فتح ملف الحساب',()=>{$('sheet').close();window.LayaliLive?.openProfile?.(id)});
   }
 }
 function roomSettings(){
   if(!['host','super_admin'].includes(state.current_role))return;
   const options=state.room_settings||{},box=sheet('إعدادات الغرفة');
   note(box,'كل إعداد يُحفظ على الخادم ويُفرض على الجميع.');
   for(const [key,label] of [['chat_enabled','إتاحة الدردشة'],['block_external_links','منع الروابط']]){
     button(box,(options[key]?'✓ ':'')+label,async()=>{
       await api({action:'room_settings',settings:{[key]:!options[key]}});
       await refresh();roomSettings();
     });
   }
   for(const [key,label,choices] of [
     ['chat_slow_seconds','فاصل التعليقات',[[0,'بدون انتظار'],[5,'5 ثوانٍ'],[10,'10 ثوانٍ'],[30,'30 ثانية']]],
     ['seat_join_mode','الانضمام للمقاعد',[['request','بطلب موافقة'],['free','مفتوح']]]
   ]){
     const field=document.createElement('label');field.textContent=label;
     const select=document.createElement('select');
     for(const [value,title] of choices)select.add(new Option(title,value));
     select.value=String(options[key]);field.append(select);box.append(field);
     select.onchange=async()=>{select.disabled=true;try{await api({action:'room_settings',settings:{[key]:key==='chat_slow_seconds'?Number(select.value):select.value}});await refresh()}catch(e){select.value=String(options[key]);message(e)}finally{select.disabled=false}};
   }
   if(config.mode==='audio'){
     for(const [key,title,choices,current] of [
       ['capacity','توزيع المقاعد',[[5,'جلسة خاصة · 5'],[9,'غرفة اجتماعية · 9'],[13,'غرفة كبيرة · 13']],state.max_speakers],
       ['room_theme','خلفية الغرفة',[['royal','الأزرق الملكي'],['diamond','الماسي الأسود'],['emerald','الزمرد'],['amethyst','الجمشت']],options.room_theme]
     ]){
       sectionHeading(box,title);
       const select=document.createElement('select');select.setAttribute('aria-label',title);
       for(const [value,label] of choices)select.add(new Option(label,String(value)));
       select.value=String(current);box.append(select);
       select.onchange=async()=>{select.disabled=true;try{
         await api({action:'room_settings',settings:{[key]:key==='capacity'?Number(select.value):select.value}});
         await refresh();roomSettings();
       }catch(e){select.value=String(current);note(box,e.message==='occupied_seat_outside_layout'?'أحد المقاعد المطلوب إخفاؤها مشغول.':'تعذر حفظ الإعداد.')}finally{select.disabled=false}};
     }
   }
   sectionHeading(box,'قفل المقاعد');
   for(let number=1;number<(state.max_speakers||config.maxSpeakers);number++){
     const locked=(options.locked_seat_numbers||[]).includes(number);
     button(box,'المقعد '+number+' — '+(locked?'🔒 مغلق':'مفتوح'),async()=>{
       await api({action:'room_settings',settings:{lock_seat:{number,locked:!locked}}});
       await refresh();roomSettings()
     });
   }
   sectionHeading(box,'خصوصية الدخول');
   const password=document.createElement('input');password.type='password';password.minLength=4;password.maxLength=40;
   password.placeholder='كلمة مرور جديدة';password.setAttribute('aria-label','كلمة مرور الغرفة');box.append(password);
   button(box,'تعيين كلمة مرور',async()=>{
     if(password.value.length<4){password.reportValidity();return}
     await api({action:'room_settings',settings:{password:password.value}});
     password.value='';$('sheet').close();await refresh()
   });
   if(options.password_protected)button(box,'إلغاء كلمة المرور',async()=>{
     await api({action:'room_settings',settings:{clear_password:true}});
     $('sheet').close();await refresh()
   });
 }
 // Delegate at capture phase: one hook for dynamically re-rendered avatars/comments.
 function openActionFromEvent(e){
   if(e.target.closest?.('.comment-actions'))return;
   if(config.mode==='audio' && e.target.closest?.('#profile')){
     const host=state.speakers?.find(s=>s.owner);
     if(host){e.preventDefault();e.stopPropagation();chosenRoomMember=host.user_id;showUser(host);return}
   }
   const bubble=e.target.closest?.('#messages .comment-bubble');
   if(bubble){
     const id=Number(bubble.dataset.userId);
     if(Number.isInteger(id)&&id>0){
       const message=(state.comments||[]).find(c=>String(c.id)===bubble.dataset.commentId);
       e.preventDefault();e.stopPropagation();showUser(message,message);
     }
     return;
   }
   const seat=e.target.closest?.('#audioStage .audio-seat[data-user-id]');
   if(seat){
     const id=Number(seat.dataset.userId);
     if(id>0&&id!==config.userId){
       const speaker=(state.speakers||[]).find(s=>Number(s.user_id)===id);
       chosenRoomMember=id;e.preventDefault();e.stopPropagation();showUser(speaker)
     }
   }
 }
 $('stage')?.addEventListener('click',openActionFromEvent,true);
 // Keep IDs attached to regenerated content without relying on their visual labels.
 const messages=$('messages');
 if(messages){
   const track=new MutationObserver(()=>{
     for(const bubble of messages.querySelectorAll('.comment-bubble')){
       if(bubble.dataset.userId)continue;
       const author=bubble.querySelector('.comment-author')?.textContent?.replace(/:\\s*$/,'')?.trim();
       const candidates=(state.comments||[]).filter(c=>c.name===author&&!ignored.has(Number(c.user_id)));
       if(candidates.length===1)bubble.dataset.userId=String(candidates[0].user_id);
     }
   });
   track.observe(messages,{childList:true,subtree:true});
 }
 const audio=$('audioStage');
 if(audio){
   const track=new MutationObserver(()=>{
     for(const seat of audio.querySelectorAll('.audio-seat[data-identity]')){
       const speaker=(state.speakers||[]).find(s=>s.identity===seat.dataset.identity);
       if(speaker)seat.dataset.userId=String(speaker.user_id)
     }
   });
   track.observe(audio,{childList:true,subtree:true});
 }
 window.RelaxiaRoomModeration={showUser,roomSettings,reportUser,getState:()=>state,openSheet:sheet,renderPortrait,loadAudience:after=>api({action:"audience_page",after})};
})();
