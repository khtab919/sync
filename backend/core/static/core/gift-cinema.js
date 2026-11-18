/* Relaxia Gift Cinema • self-contained 3-D mesh renderer + individual Web Audio cues.
   No remote assets, external SDKs, video recording or automatic financial actions.
   Uses triangles in perspective (actual 3D meshes), not a stretched WebP. */
(function(global){
'use strict';
const TWO=Math.PI*2;
const PALETTE={
 gold:[232,187,105],ivory:[242,224,190],bronze:[127,84,44],
 navy:[17,51,102],sapphire:[37,116,196],cyan:[102,213,236],
 rose:[243,112,153],ruby:[190,43,90],white:[237,247,255],
 violet:[142,118,233],green:[100,202,130],chocolate:[121,70,51],
 brown:[117,72,48],dark:[40,47,67],cream:[226,188,145]
};
const KNOWN=[
 'gift_clap','gift_coffee','gift_flower','gift_chocolate','gift_rose','gift_teddy',
 'gift_butterfly','gift_balloons','gift_heart','gift_bouquet','gift_cake','gift_dolphin',
 'gift_star','gift_fireworks','gift_diamond','gift_crown','gift_supercar','gift_yacht',
 'gift_eagle','gift_palace','gift_blue_whale','gift_lion','gift_falcon'
];
const THEMES={
 gift_clap:['gold',392],gift_coffee:['cream',294],gift_flower:['rose',440],
 gift_chocolate:['chocolate',330],gift_rose:['ruby',466],gift_teddy:['brown',262],
 gift_butterfly:['violet',554],gift_balloons:['sapphire',370],gift_heart:['ruby',587],
 gift_bouquet:['rose',523],gift_cake:['cream',659],gift_dolphin:['cyan',440],
 gift_star:['gold',784],gift_fireworks:['gold',880],gift_diamond:['cyan',988],
 gift_crown:['gold',698],gift_supercar:['sapphire',196],gift_yacht:['ivory',294],
 gift_eagle:['gold',988],gift_palace:['ivory',523],gift_blue_whale:['cyan',130],
 gift_lion:['gold',165],gift_falcon:['gold',1046]
};
function clean(code){return String(code||'').replace(/[^a-z0-9_]/g,'').slice(0,40)}
function resolve(code){
 const value=clean(code);
 if(value==='lion')return 'gift_lion';
 if(value==='falcon_gold'||value==='falcon')return 'gift_falcon';
 if(value==='whale')return 'gift_blue_whale';
 return KNOWN.includes(value)?value:'gift_star';
}
function rgba(c,light,alpha){
 const rgb=PALETTE[c]||PALETTE.gold;
 return 'rgba('+rgb.map(v=>Math.max(0,Math.min(255,Math.round(v*light)))).join(',')+','+(alpha===undefined?1:alpha)+')';
}
function rotate(p,rx,ry,rz){
 let [x,y,z]=p;
 let t=y*Math.cos(rx)-z*Math.sin(rx);z=y*Math.sin(rx)+z*Math.cos(rx);y=t;
 t=x*Math.cos(ry)+z*Math.sin(ry);z=-x*Math.sin(ry)+z*Math.cos(ry);x=t;
 t=x*Math.cos(rz)-y*Math.sin(rz);y=x*Math.sin(rz)+y*Math.cos(rz);x=t;
 return [x,y,z];
}
function createScene(code,progress){
 const tris=[],t=progress*TWO,phase=Math.sin(t*3),premium=!['gift_clap','gift_coffee','gift_flower','gift_chocolate','gift_rose','gift_teddy'].includes(code);
 let globalX=0,globalY=0,globalR=0;
 const flying=code==='gift_eagle'||code==='gift_falcon';
 if(flying){globalX=1.8-progress*3.6;globalY=Math.sin(progress*Math.PI)*.45;globalR=(progress-.5)*.22}
 if(code==='gift_blue_whale'||code==='gift_dolphin'){
  globalX=1.3-progress*2.6;globalY=-.16+Math.sin(t)*.22;globalR=-.12
 }
 const baseYaw=Math.sin(t*.45)*.32;
 function tri(a,b,c,col,emissive=0){
  tris.push({v:[a,b,c],color:col,emissive});
 }
 function point(x,y,z){return [x+globalX,y+globalY,z]}
 function ellipsoid(x,y,z,sx,sy,sz,col,segments=12,rings=7){
  for(let j=0;j<rings;j++){
   const a=-Math.PI/2+j*Math.PI/rings,b=-Math.PI/2+(j+1)*Math.PI/rings;
   for(let i=0;i<segments;i++){
    const l=i*TWO/segments,r=(i+1)*TWO/segments;
    const f=(u,v)=>point(x+Math.cos(u)*Math.cos(v)*sx,y+Math.sin(u)*sy,z+Math.cos(u)*Math.sin(v)*sz);
    const p=f(a,l),q=f(a,r),u=f(b,l),v=f(b,r);
    tri(p,u,q,col);tri(q,u,v,col);
   }
  }
 }
 function cone(x,y,z,sx,sy,sz,col,count=10){
  const tip=point(x,y+sy,z);
  for(let i=0;i<count;i++){
   let a=i*TWO/count,b=(i+1)*TWO/count;
   tri(point(x+sx*Math.cos(a),y,z+sz*Math.sin(a)),tip,
       point(x+sx*Math.cos(b),y,z+sz*Math.sin(b)),col);
  }
 }
 function wedge(a,b,c,col){tri(point(...a),point(...b),point(...c),col)}
 function star(x,y,z,r,color,offset=0){
  for(let i=0;i<10;i++){
   let a=i*Math.PI/5+offset,b=(i+1)*Math.PI/5+offset;
   let ra=(i%2?r*.45:r),rb=((i+1)%2?r*.45:r);
   wedge([x+Math.cos(a)*ra,y+Math.sin(a)*ra,z],
         [x+Math.cos(b)*rb,y+Math.sin(b)*rb,z],[x,y,z-.20],color);
  }
 }
 function heart(x,y,z,color,scale=1){
  ellipsoid(x-.32*scale,y+.20*scale,z,.44*scale,.46*scale,.28*scale,color,10,6);
  ellipsoid(x+.32*scale,y+.20*scale,z,.44*scale,.46*scale,.28*scale,color,10,6);
  cone(x,y-.35*scale,z,.64*scale,-.65*scale,.28*scale,color,12);
 }
 function flower(x,y,z,col,scale=1){
  ellipsoid(x,y-.28*scale,z,.05*scale,.60*scale,.05*scale,'green',8,5);
  for(let i=0;i<7;i++){
   let a=i*TWO/7+t*.10;
   ellipsoid(x+Math.sin(a)*.30*scale,y+Math.cos(a)*.30*scale,z-.22*scale,
      .19*scale,.26*scale,.12*scale,col,9,5);
  }
  ellipsoid(x,y,z-.32*scale,.19*scale,.19*scale,.12*scale,'gold');
 }
 function bird(){
  const flap=Math.sin(progress*25)*.65;
  ellipsoid(0,0,0,.34,.38,.88,'bronze');
  ellipsoid(0,.20,-.65,.27,.25,.34,'cream');
  ellipsoid(0,.18,-.99,.18,.1,.26,'gold',10,5);
  ellipsoid(-.12,.27,-.85,.06,.06,.06,'dark',8,4);
  ellipsoid(.12,.27,-.85,.06,.06,.06,'dark',8,4);
  for(let dir of [-1,1]){
   const shoulder=point(dir*.22,.15,0),tip=point(dir*1.66,.28+flap,-.03);
   const back=point(dir*.55,-.14,.68);
   tri(shoulder,tip,back,'gold');
   for(let j=0;j<7;j++){
    let p=j/7;
    wedge([dir*(.28+1.30*p),.12+flap*p,.03],
          [dir*(.42+1.42*p),-.24+flap*(p+.1),.30],
          [dir*(.49+1.13*p),-.14+flap*p,.56],'bronze');
   }
  }
  for(let i=-2;i<=2;i++)wedge([i*.12,-.12,.55],[(i+.4)*.16,-.09,1.28],
                                [(i+1)*.12,-.12,.59],'ivory');
 }
 function lion(){
  const open=Math.max(0,Math.sin(progress*3.8*Math.PI))*0.22;
  for(let i=0;i<16;i++){
   const a=i*TWO/16;
   cone(Math.cos(a)*.75,Math.sin(a)*.7,-.22,
        .23,.23,.29,i%3?'gold':'bronze',6);
  }
  ellipsoid(0,.06,-.19,.81,.80,.57,'gold',14,9);
  for(let dir of [-1,1]){
   ellipsoid(dir*.65,.63,-.10,.27,.25,.20,'bronze');
   ellipsoid(dir*.34,.18,-.68,.09,.11,.08,'dark',8,5);
   ellipsoid(dir*.33,.20,-.75,.033,.045,.04,'white',8,4);
   ellipsoid(dir*.21,-.24,-.73,.33,.23,.21,'cream');
   for(let n=0;n<3;n++)wedge([dir*.19,-.32,-.88],[dir*(.26+.12*n),-.42-open,-.92],
                               [dir*(.31+.12*n),-.31-open,-.82],'ivory');
  }
  ellipsoid(0,-.38-open*.45,-.61,.39,.18+open,.21,'dark');
  ellipsoid(0,-.22,-.91,.21,.14,.13,'dark');
  ellipsoid(0,-.66-open*.75,-.49,.32,.22,.19,'gold');
 }
 function whale(){
  ellipsoid(0,0,0,1.2,.50,.62,'sapphire',16,9);
  ellipsoid(-.37,-.23,-.25,.56,.25,.35,'cyan',13,7);
  for(const dir of [-1,1]){
   wedge([dir*.4,-.15,.03],[dir*1.2,-.36,.40],[dir*.5,-.22,.35],'navy');
   wedge([dir*.84,0,.44],[dir*1.2,.30,.84],[dir*.74,-.10,.80],'sapphire');
  }
  ellipsoid(-.72,.12,-.53,.06,.06,.06,'white',8,4);
  ellipsoid(.72,.12,-.53,.06,.06,.06,'white',8,4);
  for(let i=0;i<4;i++)ellipsoid(-.4+i*.25, .66+Math.sin(t*3+i)*.10, .10,
                       .05+i*.01,.12+i*.02,.05,'cyan',8,4);
 }
 function trophy(){
  cone(0,-.80,0,.54,.25,.54,'bronze');
  cone(0,-.50,0,.12,.70,.12,'gold');
  for(let i=0;i<8;i++){
   let a=i*TWO/8;
   cone(Math.cos(a)*.63,.14,Math.sin(a)*.45,.16,.62,.17,'gold');
  }
  ellipsoid(0,.05,0,.70,.20,.50,'gold');
  for(let i=0;i<5;i++){
   let a=i*TWO/5;
   ellipsoid(Math.cos(a)*.53,.72,Math.sin(a)*.38,.11,.12,.10,'ruby',8,5);
  }
 }
 function automobile(){
  ellipsoid(0,-.15,0,1.2,.29,.53,'sapphire',14,7);
  ellipsoid(-.10,.16,0,.68,.36,.46,'navy',12,7);
  for(let x of [-.75,.72])for(let z of [-.55,.55])
   ellipsoid(x,-.49,z,.25,.29,.12,'dark',10,6);
  for(let z of [-.35,.35])ellipsoid(-1.1,-.14,z,.08,.12,.13,'ivory');
 }
 function ship(){
  ellipsoid(0,-.35,0,1.25,.28,.54,'navy',16,6);
  ellipsoid(0,-.13,0,1.18,.15,.46,'ivory',16,6);
  ellipsoid(-.08,.28,0,.70,.33,.35,'white',12,6);
  ellipsoid(-.32,.55,.02,.28,.16,.30,'sapphire',10,5);
  cone(-.4,.55,0,.05,.55,.05,'gold');
 }
 function fortress(){
  for(let x of [-.85,0,.85]){
   ellipsoid(x,-.1,.05,.29,.85,.33,'ivory',10,6);
   cone(x,.82,.05,.41,.48,.41,'sapphire');
  }
  ellipsoid(0,-.6,0,1.25,.25,.64,'bronze');
  ellipsoid(0,-.42,-.33,.27,.43,.08,'navy');
  for(let x of [-.84,0,.84])ellipsoid(x,.1,-.30,.10,.16,.08,'gold',8,5);
 }
 function gemstone(){
  const top=[0,.96,0],low=[0,-.99,0];
  for(let i=0;i<10;i++){
   const a=i*TWO/10,b=(i+1)*TWO/10;
   wedge(top,[Math.cos(a)*.75,.25,Math.sin(a)*.53],
      [Math.cos(b)*.75,.25,Math.sin(b)*.53],i%2?'cyan':'white');
   wedge(low,[Math.cos(b)*.75,.25,Math.sin(b)*.53],
      [Math.cos(a)*.75,.25,Math.sin(a)*.53],i%2?'sapphire':'cyan');
  }
 }
 function butterfly(){
  ellipsoid(0,0,-.25,.10,.75,.12,'bronze');
  for(let d of [-1,1]){
   const flap=Math.sin(progress*24)*.52;
   ellipsoid(d*(.66+flap*.26),.35,-.14,.61,.71,.14,'violet',11,6);
   ellipsoid(d*(.48+flap*.17),-.42,-.12,.43,.48,.12,'rose',11,6);
  }
 }
 function teddy(){
  ellipsoid(0,-.44,0,.56,.70,.41,'brown');
  ellipsoid(0,.42,-.18,.58,.59,.48,'brown');
  for(let d of [-1,1]){
   ellipsoid(d*.48,.89,-.19,.27,.28,.22,'cream');
   ellipsoid(d*.24,.49,-.61,.075,.08,.06,'dark',8,5);
   ellipsoid(d*.58,-.4,0,.25,.40,.27,'brown');
  }
  ellipsoid(0,.19,-.61,.29,.23,.17,'cream');
  ellipsoid(0,.26,-.79,.12,.10,.08,'dark');
 }
 function categoryModel(){
  switch(code){
   case 'gift_eagle':case 'gift_falcon':bird();break;
   case 'gift_lion':lion();break;
   case 'gift_blue_whale':case 'gift_dolphin':whale();break;
   case 'gift_supercar':automobile();break;
   case 'gift_yacht':ship();break;
   case 'gift_palace':fortress();break;
   case 'gift_crown':trophy();break;
   case 'gift_diamond':gemstone();break;
   case 'gift_teddy':teddy();break;
   case 'gift_butterfly':butterfly();break;
   case 'gift_heart':heart(0,0,-.20,'ruby');break;
   case 'gift_rose':flower(0,0,0,'ruby',1.2);break;
   case 'gift_flower':flower(0,0,0,'rose',1);break;
   case 'gift_bouquet':
    for(let i=-1;i<=1;i++)flower(i*.46,-.12,0,i===0?'ruby':'rose',.77);
    break;
   case 'gift_star':star(0,0,0,1.03,'gold',t*.22);break;
   case 'gift_fireworks':
    for(let i=0;i<8;i++)star(Math.sin(i*2.1)*.8,Math.cos(i*1.5)*.7,0,.23,'gold',t*.4+i);
    break;
   case 'gift_balloons':
    for(let i=-1;i<=1;i++)ellipsoid(i*.54,.45+Math.sin(t+i)*.16,0,.28,.42,.27,i===0?'rose':i<0?'gold':'sapphire');
    break;
   case 'gift_cake':
    ellipsoid(0,-.55,0,1,.38,.70,'rose');ellipsoid(0,-.18,0,.84,.20,.64,'ivory');
    for(let i=-2;i<=2;i++){cone(i*.30,.1,0,.05,.28,.06,'ivory');ellipsoid(i*.30,.40+Math.sin(t+i)*.04,0,.10,.16,.09,'gold')}
    break;
   case 'gift_coffee':
    ellipsoid(0,-.17,0,.73,.62,.55,'ivory');ellipsoid(0,.37,0,.65,.10,.47,'chocolate');
    for(let i=-1;i<=1;i++)ellipsoid(i*.27,.70+Math.sin(t*2+i)*.10,0,.06,.30,.06,'cyan');
    break;
   case 'gift_clap':
    ellipsoid(-.32,.03,0,.29,.65,.15,'gold');ellipsoid(.32+phase*.19,.03,-.3,.29,.65,.15,'cream');
    break;
   case 'gift_chocolate':
    for(let x of [-.41,0,.41])for(let y of [-.30,.30])
     ellipsoid(x,y,0,.26,.25,.34,'chocolate',9,5);
    break;
   default:gemstone();
  }
 }
 categoryModel();
 return {tris,premium,baseYaw,globalR};
}
function paint(canvas,code,progress){
 if(!canvas||!canvas.isConnected)return;
 const rect=canvas.getBoundingClientRect();
 if(rect.width<10||rect.height<10)return;
 const dpr=Math.min(1.5,global.devicePixelRatio||1);
 const w=Math.min(1000,Math.ceil(rect.width*dpr)),h=Math.min(800,Math.ceil(rect.height*dpr));
 if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h}
 const ctx=canvas.getContext('2d',{alpha:true});if(!ctx)return;
 ctx.clearRect(0,0,w,h);
 const model=createScene(code,progress),phase=Math.sin(progress*TWO),rot=model.baseYaw+(code==='gift_eagle'||code==='gift_falcon'?Math.sin(progress*TWO)*.13:0);
 const light=[-.4,.8,-.6],projected=[];
 for(let f of model.tris){
  const v=f.v.map(p=>rotate(p,Math.sin(progress*TWO)*.09,rot,model.globalR));
  const a=v[0],b=v[1],c=v[2];
  const u=[b[0]-a[0],b[1]-a[1],b[2]-a[2]],v2=[c[0]-a[0],c[1]-a[1],c[2]-a[2]];
  const normal=[u[1]*v2[2]-u[2]*v2[1],u[2]*v2[0]-u[0]*v2[2],u[0]*v2[1]-u[1]*v2[0]];
  const length=Math.hypot(...normal)||1;
  const diff=Math.abs(normal[0]*light[0]+normal[1]*light[1]+normal[2]*light[2])/length;
  const shade=.56+.49*diff;
  const p=v.map(q=>{
   const depth=5.4+q[2],scale=Math.min(2,4.7/Math.max(2,depth));
   return [w*.5+q[0]*w*.29*scale,h*.50-q[1]*h*.31*scale];
  });
  projected.push({p,z:(a[2]+b[2]+c[2])/3,shade,color:f.color});
 }
 projected.sort((a,b)=>b.z-a.z);
 ctx.save();ctx.shadowColor='rgba(7,11,35,.16)';ctx.shadowBlur=3*dpr;
 for(let q of projected){
  ctx.beginPath();ctx.moveTo(...q.p[0]);ctx.lineTo(...q.p[1]);ctx.lineTo(...q.p[2]);ctx.closePath();
  ctx.fillStyle=rgba(q.color,q.shade);ctx.fill();
 }
 ctx.restore();
 // A procedural luminous orbital ring stays behind the animals, never a flat image.
 if(model.premium){
  const gradient=ctx.createRadialGradient(w*.5,h*.54,0,w*.5,h*.54,Math.min(w,h)*.46);
  gradient.addColorStop(0,'rgba(91,163,243,.13)');
  gradient.addColorStop(1,'rgba(91,163,243,0)');
  ctx.globalCompositeOperation='destination-over';ctx.fillStyle=gradient;ctx.fillRect(0,0,w,h);
  ctx.globalCompositeOperation='source-over';
 }
}
let audioContext=null,noiseBuffer=null,muteChoice=false,playing=null;
try{muteChoice=global.localStorage.getItem('relaxia_gift_sound')==='off'}catch(_){}
function audioAvailable(){return !!(global.AudioContext||global.webkitAudioContext)}
function context(){
 if(!audioAvailable())return null;
 if(!audioContext){
  const Type=global.AudioContext||global.webkitAudioContext;
  audioContext=new Type();
  noiseBuffer=audioContext.createBuffer(1,audioContext.sampleRate*3,audioContext.sampleRate);
  const values=noiseBuffer.getChannelData(0);let random=137;
  for(let i=0;i<values.length;i++){
   random=(random*1664525+1013904223)>>>0;
   values[i]=(random/2147483648)-1;
  }
 }
 if(audioContext.state==='suspended')audioContext.resume().catch(()=>{});
 return audioContext;
}
function note(ctx,start,freq,end,volume,type='sine',glide=null){
 const osc=ctx.createOscillator(),gain=ctx.createGain();
 osc.type=type;osc.frequency.setValueAtTime(freq,start);
 if(glide)osc.frequency.exponentialRampToValueAtTime(Math.max(25,glide),end);
 gain.gain.setValueAtTime(.0001,start);
 gain.gain.exponentialRampToValueAtTime(Math.max(.0002,volume),start+.035);
 gain.gain.exponentialRampToValueAtTime(.0001,end);
 osc.connect(gain);gain.connect(ctx.destination);osc.start(start);osc.stop(end+.02);
}
function noise(ctx,start,seconds,frequency,volume){
 if(!noiseBuffer)return;
 const src=ctx.createBufferSource(),filter=ctx.createBiquadFilter(),gain=ctx.createGain();
 src.buffer=noiseBuffer;filter.type='bandpass';filter.frequency.value=frequency;filter.Q.value=.58;
 gain.gain.setValueAtTime(.0001,start);
 gain.gain.exponentialRampToValueAtTime(Math.max(.0002,volume),start+.04);
 gain.gain.exponentialRampToValueAtTime(.0001,start+seconds);
 src.connect(filter);filter.connect(gain);gain.connect(ctx.destination);
 src.start(start);src.stop(start+Math.min(seconds,2.9));
}
function cue(code){
 if(muteChoice||!audioAvailable()||global.document.visibilityState==='hidden')return;
 try{
  const ctx=context();if(!ctx)return;
  const start=ctx.currentTime+.03,base=THEMES[code]?.[1]||440;
  // Special animal acoustics: layered growl (lion), wing rush and cry (eagle),
  // and sub-bass ocean song (whale), all mixed below active voice level.
  if(code==='gift_lion'){
   noise(ctx,start,.78,190,.18);
   note(ctx,start,130,start+.88,.09,'sawtooth',65);
   note(ctx,start+.16,86,start+1.24,.10,'triangle',44);
   noise(ctx,start+.25,.9,510,.10);
   return;
  }
  if(code==='gift_eagle'||code==='gift_falcon'){
   noise(ctx,start,.25,1150,.095);noise(ctx,start+.28,.27,850,.095);
   note(ctx,start+.2,680,start+.72,.075,'sawtooth',1340);
   note(ctx,start+.68,1180,start+1.05,.05,'triangle',680);
   return;
  }
  if(code==='gift_blue_whale'||code==='gift_dolphin'){
   note(ctx,start,160,start+1.5,.09,'sine',75);
   note(ctx,start+.38,330,start+1.62,.045,'sine',175);
   noise(ctx,start+.05,1.2,210,.05);
   return;
  }
  if(code==='gift_fireworks'){
   for(let i=0;i<4;i++){noise(ctx,start+i*.24,.18,1200+i*220,.08);note(ctx,start+i*.24,base+i*80,start+i*.37,.055,'sine',base*1.8)}
   return;
  }
  if(code==='gift_supercar'){note(ctx,start,110,start+.9,.085,'sawtooth',490);noise(ctx,start+.22,.58,410,.06);return}
  if(code==='gift_yacht'){note(ctx,start,220,start+1.12,.07,'triangle',165);noise(ctx,start+.14,.8,260,.07);return}
  if(code==='gift_coffee'){noise(ctx,start,.42,620,.075);note(ctx,start+.1,base,start+.38,.055,'sine');return}
  // All remaining catalog entries have their own base note, pattern and
  // modulation; no shared generic sound file.
  const index=KNOWN.indexOf(code),steps=3+(index%3);
  for(let i=0;i<steps;i++){
   const offset=i*(.12+(index%3)*.015);
   const frequency=base*Math.pow(2,[0,4,7,11,14][(i+index)%5]/12);
   note(ctx,start+offset,frequency,start+offset+.33+.03*(index%4),
       .036+(index%3)*.012,index%3===0?'triangle':'sine',frequency*(1.05+(index%2)*.12));
  }
  if(index%4===0)noise(ctx,start+.15,.28,1050+(index*55),.028);
 }catch(_){
  // Android WebView may block autoplay before a user interaction; motion stays live.
 }
}
function hasMotion(){return !global.matchMedia?.('(prefers-reduced-motion: reduce)').matches}
function mountCanvas(host,code,preview){
 const canvas=global.document.createElement('canvas');
 canvas.className=preview?'gift-3d-canvas preview':'gift-3d-canvas';
 canvas.setAttribute('aria-label',(preview?'معاينة حركة ثلاثية الأبعاد: ':'هدية ثلاثية الأبعاد: ')+code);
 canvas.setAttribute('role','img');host.append(canvas);
 let stopped=false,raf=0,start=performance.now();
 function loop(now){
  if(stopped||!canvas.isConnected)return;
  const elapsed=(now-start)/1000,normalized=hasMotion()?elapsed/(preview?4.0:4.1):.45;
  paint(canvas,code,preview?normalized%1:Math.min(1,normalized));
  if(preview||normalized<1)raf=requestAnimationFrame(loop);
 }
 raf=requestAnimationFrame(loop);
 return ()=>{stopped=true;cancelAnimationFrame(raf);canvas.remove()};
}
function stop(){
 if(!playing)return;
 playing.stop();playing=null;
}
function play(host,g){
 stop();
 const code=resolve(g.code),box=global.document.createElement('div');
 box.className='gift-cinema-stage gift-tone-'+code;
 const glow=global.document.createElement('div');glow.className='gift-cinema-aurora';
 box.append(glow);
 const stopCanvas=mountCanvas(box,code,false);
 const info=global.document.createElement('div');info.className='gift-cinema-caption';
 const symbol=global.document.createElement('span');symbol.className='gift-cinema-symbol';symbol.textContent='✦';
 const title=global.document.createElement('strong');
 title.textContent=(g.sender||'')+' أرسل '+(g.name||'هدية');
 const recipient=global.document.createElement('small');recipient.textContent='إلى '+(g.recipient||'المضيف');
 info.append(symbol,title,recipient);
 if(Number(g.combo_count)>1){
  const combo=global.document.createElement('b');combo.className='gift-cinema-combo';
  combo.textContent='×'+Math.max(1,Math.min(99,Number(g.combo_count)||1));
  info.append(combo);
 }
 box.append(info);host.append(box);
 cue(code);
 let finished=false;
 const end=()=>{if(finished)return;finished=true;stopCanvas();box.remove();if(playing&&playing.stop===end)playing=null};
 playing={stop:end};
 return end;
}
function preview(host,g){
 while(host.firstChild)host.removeChild(host.firstChild);
 return mountCanvas(host,resolve(g.code),true);
}
function thumbnail(canvas,code){requestAnimationFrame(()=>paint(canvas,resolve(code),.33))}
function previewSound(code){cue(resolve(code))}
function soundEnabled(){return !muteChoice}
function setSound(enabled){
 muteChoice=!enabled;
 try{global.localStorage.setItem('relaxia_gift_sound',enabled?'on':'off')}catch(_){}
 if(enabled)context();
 return !muteChoice;
}
global.RelaxiaGiftCinema={play,preview,previewSound,thumbnail,stop,resolve,soundEnabled,setSound,
 codes:KNOWN.slice(),engine:'procedural-3d',sound:'web-audio'};
})(window);
