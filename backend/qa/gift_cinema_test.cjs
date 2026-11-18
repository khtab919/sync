/* Motion-and-sound regression smoke checks. Run: node --test backend/qa/gift_cinema_test.cjs */
'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const test=require('node:test');

const root=path.resolve(__dirname,'../core/static/core');
const source=fs.readFileSync(path.join(root,'gift-cinema.js'),'utf8');
const stage=fs.readFileSync(path.join(root,'live-stage.js'),'utf8');
const page=fs.readFileSync(path.resolve(__dirname,'../core/templates/core/live_room.html'),'utf8');
const catalog=JSON.parse(fs.readFileSync(path.join(root,'gifts/catalog.json'),'utf8'));

function harness(){
 const callbacks=[];
 const events=[];
 const canvasEvents=[];
 const canvasContext={
  beginPath(){canvasEvents.push('beginPath')},moveTo(){},
  lineTo(){},closePath(){},fill(){canvasEvents.push('fill')},
  clearRect(){},save(){},restore(){},fillRect(){},
  createRadialGradient(){return{addColorStop(){}}}
 };
 class Audio {
  constructor(){this.currentTime=0;this.sampleRate=8000;this.destination={};this.state='running';}
  createBuffer(){return {getChannelData:()=>new Float32Array(8000*3)}}
  createOscillator(){
   const oscillator={type:'sine',connect(){},start(){events.push(['start',oscillator.type])},stop(){}};
   oscillator.frequency={
    setValueAtTime(freq){events.push(['hz',Math.round(freq)])},
    exponentialRampToValueAtTime(){}
   };
   return oscillator;
  }
  createGain(){return{gain:{
   setValueAtTime(){},exponentialRampToValueAtTime(){}
  },connect(){}}}
  createBufferSource(){return{buffer:null,connect(){},start(){events.push(['noise'])},stop(){}}}
  createBiquadFilter(){return{type:'bandpass',frequency:{value:0},Q:{value:0},connect(){}}}
 }
 function element(tag='div'){
  return{
   tagName:tag,children:[],style:{setProperty(){}},isConnected:false,
   className:'',textContent:'',width:0,height:0,attrs:{},
   append(...items){this.children.push(...items);for(const item of items)item.isConnected=true;},
   remove(){this.isConnected=false;this.children=[]},
   setAttribute(k,v){this.attrs[k]=v;},
   getBoundingClientRect(){return{width:180,height:180}},
   getContext(){return canvasContext},
   replaceChildren(...items){this.children=[];this.append(...items)}
  };
 }
 const fakeWindow={
  document:{visibilityState:'visible',createElement:element},
  localStorage:{getItem(){return'off'===this.value?'off':'on'},setItem(_key,value){this.value=value}},
  AudioContext:Audio,
  devicePixelRatio:1,
  matchMedia(){return{matches:false}}
 };
 const context={
  window:fakeWindow,performance:{now:()=>0},
  requestAnimationFrame(cb){callbacks.push(cb);return callbacks.length},
  cancelAnimationFrame(){}
 };
 vm.runInNewContext(source,context,{filename:'gift-cinema.js'});
 const engine=fakeWindow.RelaxiaGiftCinema;
 return {engine,events,canvasEvents,callbacks,element,
  flushFrame(){const cb=callbacks.shift();assert.ok(cb,'RAF scheduled');cb(300)},
  flushAll(limit=100){for(let i=0;i<limit&&callbacks.length;i++)callbacks.shift()(300)}
 };
}

test('all catalog gifts are animated with procedural 3D geometry; lion/falcon aliases exist',()=>{
 const {engine,canvasEvents,callbacks,element}=harness();
 assert.equal(engine.engine,'procedural-3d');
 assert.equal(engine.codes.length>=21,true);
 assert.equal(engine.resolve('falcon_gold'),'gift_falcon');
 assert.equal(engine.resolve('lion'),'gift_lion');
 assert.equal(engine.resolve('whale'),'gift_blue_whale');
 for(const gift of catalog.gifts){
  const canvas=element('canvas');
  canvas.isConnected=true;
  const before=canvasEvents.length;
  engine.thumbnail(canvas,gift.id);
  const cb=callbacks.shift();assert.ok(cb,'thumbnail queued');
  cb(300);
  assert.ok(canvasEvents.slice(before).includes('fill'),'3D triangles rendered for '+gift.id);
  assert.ok(canvas.width>0&&canvas.height>0,'nonempty pixels for '+gift.id);
 }
});

test('individual sound signatures exist for every gift, with user mute control',()=>{
 const {engine,events}=harness();
 assert.equal(engine.soundEnabled(),true);
 const signatures=new Set();
 for(const gift of catalog.gifts){
  const from=events.length;
  engine.previewSound(gift.id);
  const cue=events.slice(from).filter(e=>e[0]==='hz'||e[0]==='noise');
  assert.ok(cue.length>0,'sound started for '+gift.id);
  signatures.add(JSON.stringify(cue));
 }
 assert.equal(signatures.size,catalog.gifts.length,'21 distinct sound patterns');
 const before=events.length;
 engine.setSound(false);engine.previewSound('gift_lion');
 assert.equal(events.length,before,'muted cues do not autoplay');
 engine.setSound(true);engine.previewSound('gift_eagle');
 assert.ok(events.length>before);
});

test('animated gift overlay is private to a confirmed event and has cleanup',()=>{
 const {engine,element,events,callbacks}=harness();
 const host=element('section');host.isConnected=true;
 const clean=engine.play(host,{code:'gift_eagle',sender:'مرسل',recipient:'صاحب البث',name:'نسر',combo_count:2});
 assert.ok(host.children.length>0);
 assert.ok(host.children[0].children.some(c=>c.className==='gift-3d-canvas'));
 callbacks.shift()(300);
 assert.ok(events.length>0);
 clean();clean();
 assert.equal(host.children[0].isConnected,false);
 assert.equal(engine.soundEnabled(),true);
 engine.stop();
});

test('live gift UI has inline two-step confirmation, no recipient fallback and measured controls',()=>{
 assert.match(stage,/readyFingerprint!==fingerprint/);
 assert.match(stage,/selectedSeatId==null\?host:speakers.find/);
 assert.doesNotMatch(stage,/selectedSeatId==null\?host:\(speakers.find/);
 assert.doesNotMatch(stage,/confirm\('تأكيد إرسال/);
 assert.match(stage,/RelaxiaGiftCinema\.play\(box,g\)/);
 assert.match(stage,/RelaxiaGiftCinema\.preview\(previewArt,selected\)/);
 assert.match(stage,/RelaxiaGiftCinema\.thumbnail\(canvas,g.code\)/);
 assert.match(stage,/function syncRoomFooterMetrics/);
 assert.match(page,/gift-cinema\.js\?v=cinema-1/);
 assert.match(page,/gift-cinema\.css\?v=cinema-1/);
 assert.ok(page.indexOf('gift-cinema.js?v=cinema-1')<page.indexOf('live-stage.js?v=cinema-mic-2'));
});
