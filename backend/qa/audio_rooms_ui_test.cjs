'use strict';
const {chromium}=require('playwright');
const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..');
const html=execFileSync(process.env.TEST_PYTHON||'python',['-c',`import django;django.setup()
from django.template.loader import render_to_string
config={'id':'room-test','owner':False,'ownerName':'مضيف تجريبي','ownerId':1,'ownerPublicId':'58321','userId':2,'userName':'زائر تجريبي','mode':'audio','maxSpeakers':15,'title':'غرفة الأصدقاء','gifts':True,'private':False}
print(render_to_string('core/live_room.html',{'config':config,'nonce':'qa','theme':'dark'}))`],{cwd:root,env:{...process.env,DJANGO_SETTINGS_MODULE:'config.settings',DJANGO_SECRET_KEY:'test-only',POSTGRES_PASSWORD:'test-only',LAYALI_TESTING:'1'},encoding:'utf8'}).replace(/<script src="[^\"]+"><\/script>/g,'').replace(/<link rel="stylesheet"[^>]+>/,'');
const css=fs.readFileSync(path.join(root,'core/static/core/live-stage.css'),'utf8');
const js=fs.readFileSync(path.join(root,'core/static/core/live-stage.js'),'utf8').replace('connect().catch(e=>{message(e);if(e.name!==\'MediaFailure\')reconnect()});','');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CAPTURE_CHROMIUM?{executablePath:process.env.CAPTURE_CHROMIUM}:{}),args:['--no-sandbox']});
 try{
  for(const [width,height] of [[320,640],[360,820],[430,820]]){
   const page=await browser.newPage({viewport:{width,height}});
   await page.addInitScript(()=>window.relaxiaStageLoaded=true);
   await page.route('https://room.test/',route=>route.fulfill({contentType:'text/html',body:html}));
   await page.goto('https://room.test/');await page.addStyleTag({content:css});
   await page.evaluate(()=>{window.LivekitClient={Room:{},RoomEvent:{},Track:{},ConnectionState:{Connected:'connected'}};window.RelaxiaMedia={}});
   await page.addScriptTag({content:js});
   assert.equal(await page.locator('.audio-seat').count(),15,'placeholder seats render before network response');
   await page.evaluate(()=>{
    window.sent=[];room={state:'connected',localParticipant:{}};
    const result={status:'active',mode:'audio',max_speakers:15,server_time:new Date().toISOString(),speakers:[{identity:'host-1',user_id:1,name:'مضيف تجريبي',owner:true,mode:'audio',avatar_url:''}],comments:[],gift_events:[],gift_catalog:[{code:'flower',name:'زهرة',symbol:'🌷',price_cents:50,effect:'simple'},{code:'whale',name:'الحوت الأزرق',symbol:'🐋',price_cents:10000,effect:'depth'},{code:'lion',name:'الأسد الملكي',symbol:'🦁',price_cents:15000,effect:'depth'}]};
    window.qaRoom=result;api=async action=>{if(action)window.sent.push(action);return {...window.qaRoom,seat_status:action?.action==='seat_request'?'pending':''}};drawState(result);
   });
   assert.equal(await page.locator('.audio-seat').count(),15);
   assert.equal(await page.locator('.empty-seat').count(),14);
   assert.equal(await page.locator('.empty-seat small').count(),0,'empty places have accessible numbers, not visually crowded labels');
   assert.equal(await page.locator('.empty-seat').first().getAttribute('aria-label'),'مقعد 2 · طلب الميكروفون');
   assert.equal(await page.locator('#title').innerText(),'ID: 58321');
   assert.ok(!(await page.locator('#badge').innerText()).includes('undefined'),'level must not expose undefined placeholders');
   await page.evaluate(async()=>{qaRoom.status='starting';reconnectSince=Date.now()-13000;await refresh()});
   assert.equal(await page.locator('#reconnectAction').isVisible(),true,'stalled startup offers reconnect');
   await page.evaluate(async()=>{qaRoom.status='active';await refresh()});
   assert.equal(await page.locator('#reconnectAction').isVisible(),false,'reconnect hides after startup');
   assert.equal(await page.locator('#speaker').isVisible(),false);
   assert.equal(await page.locator('#share').isVisible(),false);
   assert.equal(await page.locator('#gift').isVisible(),false);
   assert.equal(await page.locator('#micQuick').isVisible(),false,'no duplicate quick microphone for audience');
   assert.equal(await page.locator('#bigGiftBtn').isVisible(),true);
   const actionRow=await page.locator('footer .quick-actions').boundingBox();
   const composerRow=await page.locator('footer #composer').boundingBox();
   assert.ok(actionRow && composerRow && composerRow.y>=actionRow.y+actionRow.height-2,'comment input is on a dedicated row below controls');
   assert.ok(composerRow.width >= width-30,'comment composer spans the mobile width');

   assert.equal(await page.locator('#viewers').isVisible(),true);
   assert.equal(await page.locator('#exit svg').count(),1,'use the consistent vector close icon');
   assert.equal(await page.locator('#more svg').count(),1,'use the consistent vector overflow icon');
   assert.equal(await page.locator('#guests svg').count(),1,'use the consistent vector guest icon');
   // Light stage text must not bleed into the dark controls, including host mic states.
   for(const theme of ['light','dark']){
    await page.evaluate(theme=>{document.documentElement.dataset.theme=theme;speaking=true;muted=false;updateQuickMic()},theme);
    for(const id of ['more','guests','micQuick']){
     const control=page.locator('#'+id);
     assert.equal(await control.locator('svg').count(),1,'visible controls use vectors: '+id);
     assert.equal(await control.evaluate(el=>getComputedStyle(el).color),'rgb(255, 255, 255)','readable control foreground in '+theme);
     const icon=await control.locator('svg').boundingBox();assert.ok(icon.width>=20&&icon.height>=20,'icon has explicit bounds');
    }
    await page.evaluate(()=>{muted=true;updateQuickMic()});
    assert.equal(await page.locator('#micQuick').getAttribute('aria-label'),'تشغيل الميكروفون');
    assert.equal(await page.locator('#micQuick').getAttribute('aria-pressed'),'false');
    if(process.env.LIVE_PREVIEW_DIR){fs.mkdirSync(process.env.LIVE_PREVIEW_DIR,{recursive:true});await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`controls-${theme}-${width}.png`)})}
   }
   await page.evaluate(()=>{speaking=false;muted=false;updateQuickMic();document.documentElement.dataset.theme='dark'});
   await page.evaluate(()=>{
     const audio=document.createElement('audio');audio.dataset.liveRemote='1';audio.play=()=>Promise.resolve();document.body.append(audio);
   });
   // Reproduce an Android WebView reporting a collapsed CSS viewport.
   await page.locator('#bigGiftBtn').click();
   await page.evaluate(()=>document.documentElement.style.setProperty('--room-viewport-height','1px'));
   assert.equal(await page.locator('#sheet').evaluate(el=>el.open),true,'gift modal opens');
   assert.ok(await page.getByRole('heading',{name:'متجر الهدايا'}).isVisible(),'gift modal heading visible');
   const giftBounds=await page.locator('#sheet').boundingBox();
   assert.ok(giftBounds && giftBounds.height>=Math.min(320,height*.5),'gift menu must not collapse below its header');
   assert.ok(giftBounds.y>=0 && giftBounds.y+giftBounds.height<=height+2,'gift menu remains within visible screen');
   assert.ok(await page.locator('#sheetBody .gift-card').count()>0,'gift grid is reachable');
   await page.locator('#sheet').evaluate(el=>{el.scrollTop=el.scrollHeight});
   await page.locator('#closeSheet').click();
   await page.evaluate(()=>document.documentElement.style.removeProperty('--room-viewport-height'));
   await page.locator('#more').click();
   if(width<=380){await page.getByRole('button',{name:'الرموز التعبيرية',exact:true}).click();await page.locator('#closeSheet').click();await page.locator('#more').click();}
   await page.getByRole('button',{name:'كتم صوت الغرفة',exact:true}).click();
   assert.equal(await page.locator('audio[data-live-remote]').evaluate(el=>el.muted),true);
   assert.equal(await page.locator('#speaker').getAttribute('aria-label'),'تشغيل صوت الغرفة');
   await page.locator('#more').click();
   await page.getByRole('button',{name:'تشغيل صوت الغرفة',exact:true}).click();
   assert.equal(await page.locator('audio[data-live-remote]').evaluate(el=>el.muted),false);
   if(width<=380){await page.locator('#more').click();await page.getByRole('button',{name:'الرموز التعبيرية',exact:true}).click();}else await page.locator('#emojiBtn').click();
   await page.getByRole('button',{name:'🎉',exact:true}).click();
   assert.equal(await page.locator('#text').inputValue(),'🎉');
   await page.locator('.composer-send').click();
   assert.equal(await page.evaluate(()=>sent.find(a=>a.action==='comment')?.text),'🎉');
   assert.equal(await page.locator('#text').inputValue(),'','sent text is cleared');
   assert.equal(await page.locator('#commentNotice').innerText(),'تم إرسال التعليق','chat confirms send');
   // Simulate Gboard reducing the viewport before the native keyboard test.
   await page.setViewportSize({width,height:440});
   await page.locator('#text').focus();
   assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('chat-focused')),true,'keyboard focus adjusts the room layout');
   assert.equal(await page.locator('#audioStage').isVisible(),false,'keyboard prioritizes comments over seats on short screens');
   assert.equal(await page.locator('.quick-actions').isVisible(),true,'room action menus stay tappable while typing');
   await page.locator('#more').click();
   assert.equal(await page.locator('#sheet').evaluate(el=>el.open),true,'more menu opens with keyboard focused');
   assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('chat-focused')),false,'menu dismisses keyboard before opening');
   await page.locator('#closeSheet').click();
   await page.locator('#text').focus();
   const keyboardComposer=await page.locator('#composer').boundingBox();
   assert.ok(keyboardComposer.y>=0 && keyboardComposer.y+keyboardComposer.height<=442,'the full comment input stays above the simulated keyboard');
   await page.locator('#text').fill('تعليق من لوحة المفاتيح');
   await page.locator('#text').press('Enter');
   assert.equal(await page.evaluate(()=>sent.find(a=>a.action==='comment'&&a.text==='تعليق من لوحة المفاتيح')?.text),'تعليق من لوحة المفاتيح','IME send posts comment');
   assert.equal(await page.locator('#text').inputValue(),'','successful IME send clears draft');
   await page.locator('#text').evaluate(el=>el.blur());
   await page.setViewportSize({width,height});
   assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('chat-focused')),false,'keyboard closing restores room layout');
   assert.equal(await page.locator('.quick-actions').isVisible(),true,'actions remain available after keyboard closes');
   for(const id of ['more','guests','bigGiftBtn']){
    await page.locator('#'+id).click();
    assert.equal(await page.locator('#sheet').evaluate(el=>el.open),true,id+' menu opens');
    const menu=await page.locator('#sheet').boundingBox();
    assert.ok(menu && menu.y>=0 && menu.y+menu.height<=height+2,id+' menu is not cut off');
    await page.locator('#closeSheet').click();
   }
   const composerBounds=await page.locator('#composer').boundingBox();
   assert.ok(composerBounds.width>=width-30,'composer spans the available viewport');
   await page.evaluate(()=>{api=async action=>{
    if(action?.action==='comment')throw Error('network');
    if(action)window.sent.push(action);
    return window.qaRoom;
   }});
   await page.locator('#text').fill('تعليق غير مُرسل');
   await page.locator('#sendComment').click();
   assert.equal(await page.locator('#text').inputValue(),'تعليق غير مُرسل','failed comment draft is retained');
   assert.ok(await page.locator('#commentNotice').getAttribute('class')==='error','failed comment has visible error feedback');
   await page.evaluate(()=>{api=async action=>{
    if(action)window.sent.push(action);
    return window.qaRoom;
   }});
   await page.locator('#text').fill('');
   await page.locator('#more').click();
   await page.getByRole('button',{name:'إعجاب بالبث',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='like').length),1);
   await page.locator('#closeSheet').click();
   // Even when triggered by a scripted preview, viewer-facing panels must never
   // offer destructive administrative controls without manage_room permission.
   await page.evaluate(()=>{state.participants=[{name:'عضو',user_id:9}];state.manage_room=false;peopleSheet()});
   assert.equal(await page.locator('#sheetBody').getByRole('button',{name:'حظر عضو'}).count(),0);
   await page.locator('#closeSheet').click();
   await page.locator('#more').click();
   await page.getByRole('button',{name:'الإبلاغ عن البث',exact:true}).click();
   assert.equal(await page.locator('#reportReason').count(),1,'report form stays inside the live room');
   await page.locator('#reportReason').fill('تجربة الإبلاغ عن مخالفة');
   await page.getByRole('button',{name:'إرسال البلاغ',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='report').length),1,'report reaches the real action');
   assert.equal(await page.locator('#sheet').evaluate(el=>el.open),false);
   assert.equal(await page.locator('.empty-seat').count(),14);
   const seats=await page.locator('#audioStage').boundingBox(),footer=await page.locator('footer').boundingBox();
   assert.ok(seats.y+seats.height<footer.y,'seat grid must not overlap controls');
   const comments=await page.locator('#messages').boundingBox();
   assert.ok(seats.y+seats.height<=comments.y+2,'seats must not cover comments');
   assert.ok(comments.y+comments.height<=footer.y+2,'comments must not hide under controls');
   const layout=await page.evaluate(()=>({
    overflowX:document.documentElement.scrollWidth-innerWidth,
    overflowY:document.getElementById('stage').scrollHeight-document.getElementById('stage').clientHeight,
    seatsScrollable:document.getElementById('audioStage').scrollHeight-document.getElementById('audioStage').clientHeight,
    actions:[...document.querySelectorAll('.quick-actions > button')].filter(x=>!x.hidden&&getComputedStyle(x).display!=='none').length
   }));
   assert.ok(layout.overflowX<=2,'room must fit phone width');
   assert.ok(layout.overflowY<=2,'room must not slide vertically');
   assert.ok(layout.seatsScrollable<=2,'audio seats must not scroll independently');
   assert.equal(layout.actions,3,'only three distinct primary viewer actions');
   assert.equal(await page.locator('.comment-empty').count(),1,'comments have an honest empty state');
   await page.locator('#more').click();
   assert.ok(await page.getByText('التعليقات والتفاعل',{exact:true}).isVisible(),'settings are grouped by purpose');
   await page.getByRole('button',{name:'إخفاء التعليقات',exact:true}).click();
   assert.equal(await page.locator('#messages').isVisible(),false,'comments can be hidden');
   assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('comments-hidden')),true,'hidden chat frees room space');
   await page.locator('#more').click();
   await page.getByRole('button',{name:'إظهار التعليقات',exact:true}).click();
   assert.equal(await page.locator('#messages').isVisible(),true,'comments can be restored');
   assert.equal(await page.locator('body').evaluate(el=>el.classList.contains('comments-hidden')),false,'chat area restores after re-enabling comments');
   await page.evaluate(()=>{
    qaRoom={...qaRoom,manage_room:true,comments:[{id:451,name:'عضو',text:'رسالة للاختبار'}]};
    drawState(qaRoom);
   });
   assert.equal(await page.locator('.comment-actions').count(),1,'moderation actions are not repeated inline');
   // Permissions may change without changing comment text; stale controls must disappear.
   await page.evaluate(()=>{qaRoom={...qaRoom,manage_room:false};drawState(qaRoom)});
   assert.equal(await page.locator('.comment-actions').count(),0,'moderation controls follow server permissions');
   await page.evaluate(()=>{qaRoom={...qaRoom,manage_room:true};drawState(qaRoom)});
   assert.equal(await page.locator('.comment-actions').count(),1,'moderation controls return only with permission');
   await page.locator('.comment-actions').click();
   await page.getByRole('button',{name:'تثبيت التعليق',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='pin_comment').length),1);
   await page.evaluate(()=>{qaRoom={...qaRoom,manage_room:false,comments:[]};drawState(qaRoom)});
   await page.evaluate(()=>{const before=document.querySelector('.audio-seat');window.seatBefore=before;drawAudioSeats({max_speakers:15,speakers:[{identity:'host-1',name:'مضيف تجريبي',owner:true,avatar_url:''}]})});
   assert.equal(await page.evaluate(()=>seatBefore===document.querySelector('.audio-seat')),true,'poll must not rebuild unchanged seats');
   await page.locator('.empty-seat').first().click();
   await page.getByRole('button',{name:'طلب الانضمام صوتيًا',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='seat_request').length),1);
   await page.locator('#closeSheet').click();await page.locator('#bigGiftBtn').click();
   await page.getByRole('button',{name:'فاخرة',exact:true}).click();
   assert.equal(await page.locator('.gift-card').count(),2);
   assert.equal(await page.locator('.gift-medallion').count(),2,'real gifts use dimensional artwork');
   assert.equal(await page.locator('.gift-card').first().getAttribute('aria-label'),'الحوت الأزرق · 10000 كوين');
   page.once('dialog',d=>d.dismiss());await page.locator('.gift-card').first().click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='gift').length),0);
   page.once('dialog',d=>d.accept());await page.locator('.gift-card').first().click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='gift').length),1);
   const gift=await page.evaluate(()=>sent.find(a=>a.action==='gift'));assert.equal(gift.price_cents,10000);assert.equal(gift.recipient_id,1);assert.ok(gift.request_key);
   if(process.env.LIVE_PREVIEW_DIR){fs.mkdirSync(process.env.LIVE_PREVIEW_DIR,{recursive:true});await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`audio-room-${width}.png`)});await page.locator('#bigGiftBtn').click();await page.getByRole('button',{name:'فاخرة',exact:true}).click();await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`audio-gifts-${width}.png`)});await page.locator('#closeSheet').click();}
   await page.evaluate(()=>{giftQueue=[{code:'whale',name:'الحوت الأزرق',symbol:'🐋',effect:'depth',sender:'داعم تجريبي',recipient:'مضيف تجريبي'}];playEffects()});
   assert.equal(await page.locator('#giftEffect').getAttribute('class'),'gift-show depth show-whale');
   if(process.env.LIVE_PREVIEW_DIR)await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`whale-effect-${width}.png`)});
   await page.close();
  }
  console.log('PASS: 15 seats at 360/430px, five-digit ID, speaker mute/unmute, emoji picker, comment and like, guest request, gift confirmation/cancellation, whale effect. Synthetic UI only.');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
