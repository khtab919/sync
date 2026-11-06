/* Isolated tests of the actual call-room signaling code. No live accounts/media. */
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync(__dirname+'/../core/templates/core/call_room.html','utf8');
const source=html.split('<script nonce="{{ nonce }}">')[1].split('(async()=>{')[0];
function fixture(fetch){
 const nodes=new Map();
 const element=id=>{if(!nodes.has(id))nodes.set(id,{textContent:id==='config'?JSON.stringify({video:true,call:'test',token:'test-token'}):'',style:{},classList:{add(){},remove(){}}});return nodes.get(id)};
 const timers=new Map();let next=0;
 const box={document:{getElementById:element,querySelectorAll:()=>[]},fetch,AbortController,Date,JSON,Error,Math,setTimeout(fn,ms){if(ms===8000){timers.set(++next,fn);return next}queueMicrotask(fn);return ++next},clearTimeout(id){timers.delete(id)}};
 vm.createContext(box);vm.runInContext(source,box);
 return {box,nodes,timers,run:code=>vm.runInContext(code,box)};
}
(async()=>{
 let attempts=0;
 let f=fixture(async()=>{if(++attempts<3)throw Error('offline');return {ok:true,json:async()=>({})}});
 await f.run("send('ice',{candidate:'test'})");assert.equal(attempts,3);assert.equal(f.timers.size,0);
 attempts=0;f=fixture(async()=>{attempts++;throw Error('offline')});
 await assert.rejects(f.run("send('offer',{type:'offer',sdp:'test'})"));assert.equal(attempts,1);
 attempts=0;f=fixture(async()=>{attempts++;return {ok:false,status:403}});
 await assert.rejects(f.run("send('ice',{})"));assert.equal(attempts,1);
 attempts=0;f=fixture(async()=>{attempts++;if(attempts===1)throw Error('offline');return {ok:true,json:async()=>({status:attempts===2?'accepted':'ended',events:[]})}});
 f.run("pc={connectionState:'connected',close(){}};");await f.run('poll()');assert.equal(attempts,3);assert.equal(f.nodes.get('state').textContent,'انتهى الاتصال');
 f=fixture(async()=>({ok:false,status:401}));f.run("pc={connectionState:'connected',close(){}};");await f.run('poll()');assert.equal(f.nodes.get('state').textContent,'انتهت صلاحية الاتصال. أعد فتح المكالمة.');
 console.log('PASS: transient ICE retries, single offer submission, authorization failure, signaling recovery and timer cleanup');
})().catch(e=>{console.error(e);process.exitCode=1});
