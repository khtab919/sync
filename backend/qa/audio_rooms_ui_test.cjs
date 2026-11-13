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
   assert.equal(await page.locator('#title').innerText(),'ID: 58321');
   await page.evaluate(async()=>{qaRoom.status='starting';reconnectSince=Date.now()-13000;await refresh()});
   assert.equal(await page.locator('#reconnectAction').isVisible(),true,'stalled startup offers reconnect');
   await page.evaluate(async()=>{qaRoom.status='active';await refresh()});
   assert.equal(await page.locator('#reconnectAction').isVisible(),false,'reconnect hides after startup');
   assert.equal(await page.locator('#speaker').isVisible(),false);
   assert.equal(await page.locator('#share').isVisible(),false);
   assert.equal(await page.locator('#gift').isVisible(),false);
   assert.equal(await page.locator('#micQuick').isVisible(),false,'no duplicate quick microphone for audience');
   assert.equal(await page.locator('#bigGiftBtn').isVisible(),true);
   assert.equal(await page.locator('#viewers').isVisible(),true);
   await page.evaluate(()=>{
     const audio=document.createElement('audio');audio.dataset.liveRemote='1';audio.play=()=>Promise.resolve();document.body.append(audio);
   });
   await page.locator('#more').click();
   if(width<=380){await page.getByRole('button',{name:'الرموز التعبيرية',exact:true}).click();await page.locator('#closeSheet').click();await page.locator('#more').click();}
   await page.getByRole('button',{name:'كتم صوت الغرفة',exact:true}).click();
   assert.equal(await page.locator('audio[data-live-remote]').evaluate(el=>el.muted),true);
   assert.equal(await page.locator('#speaker').getAttribute('aria-label'),'تشغيل صوت الغرفة');
   await page.locator('#more').click();
   await page.getByRole('button',{name:'تشغيل صوت الغرفة',exact:true}).click();
   assert.equal(await page.locator('audio[data-live-remote]').evaluate(el=>el.muted),false);
   await page.locator('#emojiBtn').click();
   await page.getByRole('button',{name:'🎉',exact:true}).click();
   assert.equal(await page.locator('#text').inputValue(),'🎉');
   await page.locator('.composer-send').click();
   assert.equal(await page.evaluate(()=>sent.find(a=>a.action==='comment')?.text),'🎉');
   await page.locator('#more').click();
   await page.getByRole('button',{name:'إعجاب بالبث',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='like').length),1);
   await page.locator('#closeSheet').click();
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
    actions:[...document.querySelectorAll('.bottom > button')].filter(x=>!x.hidden&&getComputedStyle(x).display!=='none').length
   }));
   assert.ok(layout.overflowX<=2,'room must fit phone width');
   assert.ok(layout.overflowY<=2,'room must not slide vertically');
   assert.ok(layout.seatsScrollable<=2,'audio seats must not scroll independently');
   assert.equal(layout.actions,3,'only three distinct primary viewer actions');
   await page.evaluate(()=>{const before=document.querySelector('.audio-seat');window.seatBefore=before;drawAudioSeats({max_speakers:15,speakers:[{identity:'host-1',name:'مضيف تجريبي',owner:true,avatar_url:''}]})});
   assert.equal(await page.evaluate(()=>seatBefore===document.querySelector('.audio-seat')),true,'poll must not rebuild unchanged seats');
   await page.locator('.empty-seat').first().click();
   await page.getByRole('button',{name:'طلب الانضمام صوتيًا',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='seat_request').length),1);
   await page.locator('#closeSheet').click();await page.locator('#bigGiftBtn').click();
   await page.getByRole('button',{name:'فاخرة',exact:true}).click();
   assert.equal(await page.locator('.gift-card').count(),2);
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
