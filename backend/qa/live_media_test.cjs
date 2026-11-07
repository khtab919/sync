'use strict';
const {test}=require('node:test');const assert=require('node:assert/strict');
const media=require('../core/static/core/live-media.js');
function fixture({cameraError,audioError,playError}={}){
 const events=[],tracks=[];
 const track=kind=>{const t={kind,stop(){events.push('stop-'+kind)},detach(){return []}};tracks.push(t);return t};
 const sdk={async createLocalVideoTrack(){events.push('camera');if(cameraError)throw cameraError;return track('video')},async createLocalAudioTrack(){events.push('microphone');if(audioError)throw audioError;return track('audio')}};
 const video={readyState:2,videoWidth:1280,videoHeight:720,addEventListener(){},removeEventListener(){},play(){events.push('play');return playError?Promise.reject(playError):Promise.resolve()}};
 return {events,tracks,args:{sdk,video:true,render(){events.push('attach');return video},report(stage){events.push(stage)}}};
}
test('camera is attached and has a rendered frame before microphone capture and before signaling',async()=>{const f=fixture();const tracks=await media.prepare(f.args);assert.equal(tracks.length,2);assert.ok(f.events.indexOf('attach')<f.events.indexOf('microphone'));assert.ok(f.events.indexOf('camera_preview')<f.events.indexOf('microphone'));assert.equal(f.events.some(e=>e.startsWith('stop')),false)});
test('microphone denial has its own stage and releases camera instead of a silent black room',async()=>{const f=fixture({audioError:{name:'NotAllowedError'}});await assert.rejects(media.prepare(f.args),e=>e.stage==='microphone_capture'&&e.reason==='NotAllowedError');assert.ok(f.events.includes('camera_preview'));assert.ok(f.events.includes('stop-video'))});
test('camera unavailable never proceeds to microphone or signaling',async()=>{const f=fixture({cameraError:{name:'NotReadableError'}});await assert.rejects(media.prepare(f.args),e=>e.stage==='camera_capture'&&e.reason==='NotReadableError');assert.ok(!f.events.includes('microphone'))});
test('blocked playback does not falsely mark preview ready',async()=>{const f=fixture({playError:{name:'NotAllowedError'}});await assert.rejects(media.prepare(f.args),e=>e.stage==='camera_preview');assert.ok(f.events.includes('stop-video'));assert.ok(!f.events.includes('microphone'))});
test('audio mode never opens camera',async()=>{const f=fixture();await media.prepare({...f.args,video:false});assert.ok(!f.events.includes('camera'));assert.ok(f.events.includes('microphone'))});
test('leaving during capture stops acquired media and prevents next capture',async()=>{const f=fixture();await assert.rejects(media.prepare({...f.args,isClosed:()=>true}));assert.ok(f.events.includes('stop-video'));assert.ok(!f.events.includes('microphone'))});
test('no decoded frame times out rather than treating attached track as visible',async()=>{const video={readyState:0,videoWidth:0,videoHeight:0,addEventListener(){},removeEventListener(){},play(){return Promise.resolve()}};await assert.rejects(media.frameReady(video,5))});

test('timed-out capture disposes a late camera track',async()=>{let resolve,stopped=false;const sdk={createLocalVideoTrack:()=>new Promise(r=>{resolve=r})};await assert.rejects(media.prepare({sdk,video:true,render(){throw Error('must not render')},captureTimeout:5}),e=>e.stage==='camera_capture');resolve({stop(){stopped=true}});await new Promise(r=>setImmediate(r));assert.equal(stopped,true)});
