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
  for(const width of [360,430]){
   const page=await browser.newPage({viewport:{width,height:820}});
   await page.addInitScript(()=>window.relaxiaStageLoaded=true);
   await page.route('https://room.test/',route=>route.fulfill({contentType:'text/html',body:html}));
   await page.goto('https://room.test/');await page.addStyleTag({content:css});
   await page.evaluate(()=>{window.LivekitClient={Room:{},RoomEvent:{},Track:{},ConnectionState:{Connected:'connected'}};window.RelaxiaMedia={}});
   await page.addScriptTag({content:js});
   await page.evaluate(()=>{
    window.sent=[];room={state:'connected',localParticipant:{}};
    const result={status:'active',mode:'audio',max_speakers:15,server_time:new Date().toISOString(),speakers:[{identity:'host-1',user_id:1,name:'مضيف تجريبي',owner:true,mode:'audio',avatar_url:''}],comments:[],gift_events:[],gift_catalog:[{code:'flower',name:'زهرة',symbol:'🌷',price_cents:50,effect:'simple'},{code:'whale',name:'الحوت الأزرق',symbol:'🐋',price_cents:10000,effect:'depth'},{code:'lion',name:'الأسد الملكي',symbol:'🦁',price_cents:15000,effect:'depth'}]};
    api=async action=>{if(action)window.sent.push(action);return {...result,seat_status:action?.action==='seat_request'?'pending':''}};drawState(result);
   });
   assert.equal(await page.locator('.audio-seat').count(),15);
   assert.equal(await page.locator('#title').innerText(),'ID: 58321');
   assert.equal(await page.locator('#speaker').isVisible(),true);
   assert.equal(await page.locator('#gift').isVisible(),false);
   assert.equal(await page.locator('#bigGiftBtn').isVisible(),true);
   await page.evaluate(()=>{
     const audio=document.createElement('audio');audio.dataset.liveRemote='1';audio.play=()=>Promise.resolve();document.body.append(audio);
   });
   await page.locator('#speaker').click();
   assert.equal(await page.locator('audio[data-live-remote]').evaluate(el=>el.muted),true);
   assert.equal(await page.locator('#speaker').getAttribute('aria-label'),'تشغيل صوت الغرفة');
   await page.locator('#speaker').click();
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
   await page.locator('.empty-seat').first().click();
   await page.getByRole('button',{name:'طلب الانضمام صوتيًا',exact:true}).click();
   assert.equal(await page.evaluate(()=>sent[0].action),'seat_request');
   await page.locator('#closeSheet').click();await page.locator('#gift').click();
   await page.getByRole('button',{name:'فاخرة',exact:true}).click();
   assert.equal(await page.locator('.gift-card').count(),2);
   assert.equal(await page.locator('.gift-card').first().getAttribute('aria-label'),'الحوت الأزرق · 10000 رصيد');
   page.once('dialog',d=>d.dismiss());await page.locator('.gift-card').first().click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='gift').length),0);
   page.once('dialog',d=>d.accept());await page.locator('.gift-card').first().click();
   assert.equal(await page.evaluate(()=>sent.filter(a=>a.action==='gift').length),1);
   const gift=await page.evaluate(()=>sent.find(a=>a.action==='gift'));assert.equal(gift.price_cents,10000);assert.equal(gift.recipient_id,1);assert.ok(gift.request_key);
   if(process.env.LIVE_PREVIEW_DIR){fs.mkdirSync(process.env.LIVE_PREVIEW_DIR,{recursive:true});await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`audio-room-${width}.png`)});await page.locator('#gift').click();await page.getByRole('button',{name:'فاخرة',exact:true}).click();await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`audio-gifts-${width}.png`)});await page.locator('#closeSheet').click();}
   await page.evaluate(()=>{giftQueue=[{code:'whale',name:'الحوت الأزرق',symbol:'🐋',effect:'depth',sender:'داعم تجريبي',recipient:'مضيف تجريبي'}];playEffects()});
   assert.equal(await page.locator('#giftEffect').getAttribute('class'),'gift-show depth show-whale');
   if(process.env.LIVE_PREVIEW_DIR)await page.screenshot({path:path.join(process.env.LIVE_PREVIEW_DIR,`whale-effect-${width}.png`)});
   await page.close();
  }
  console.log('PASS: 15 seats at 360/430px, five-digit ID, speaker mute/unmute, emoji picker, comment and like, guest request, gift confirmation/cancellation, whale effect. Synthetic UI only.');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
