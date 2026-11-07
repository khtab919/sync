/* Capture and local rendering are deliberately independent of signaling/publication. */
'use strict';
(function(root){
 class MediaFailure extends Error {
  constructor(stage,cause){super(stage);this.name='MediaFailure';this.stage=stage;this.reason=['NotAllowedError','NotFoundError','NotReadableError','OverconstrainedError','SecurityError','AbortError'].includes(cause?.name)?cause.name:'Failed';}
 }
 async function frameReady(video,timeout=10000){
  await new Promise((resolve,reject)=>{
   let timer,playing=false;const done=error=>{clearTimeout(timer);video.removeEventListener('loadeddata',check);video.removeEventListener('resize',check);video.removeEventListener('error',fail);error?reject(error):resolve()};
   const check=()=>{if(playing&&video.readyState>=2&&video.videoWidth>0&&video.videoHeight>0)done()};
   const fail=()=>done(new Error('video_frame_unavailable'));
   video.addEventListener('loadeddata',check);video.addEventListener('resize',check);video.addEventListener('error',fail);timer=setTimeout(fail,timeout);
   Promise.resolve(video.play()).then(()=>{playing=true;check()},done);
  });
 }
 async function prepare({sdk,video,render,report=()=>{},facing='user',isClosed=()=>false,captureTimeout=15000}){
  const tracks=[];
  async function capture(stage,fn){
   let timer,abandoned=false;
   const pending=Promise.resolve().then(fn).then(track=>{if(abandoned||isClosed()){track.stop();throw Error('closed')}tracks.push(track);return track});
   try{const track=await Promise.race([pending,new Promise((_,reject)=>{timer=setTimeout(()=>reject(Error('capture_timeout')),captureTimeout)})]);report(stage,'ready');return track}
   catch(e){abandoned=true;throw new MediaFailure(stage,e)}finally{clearTimeout(timer)}
  }
  try{
   if(video){
    const camera=await capture('camera_capture',()=>sdk.createLocalVideoTrack({facingMode:facing,resolution:{width:1280,height:720,frameRate:30}}));
    try{await frameReady(render(camera));if(isClosed())throw Error('closed');report('camera_preview','ready')}catch(e){throw new MediaFailure('camera_preview',e)}
   }
   await capture('microphone_capture',()=>sdk.createLocalAudioTrack({echoCancellation:true,noiseSuppression:true,autoGainControl:true}));
   return tracks;
  }catch(e){tracks.forEach(track=>{track.detach().forEach(el=>el.closest('.tile')?.remove());track.stop()});throw e}
 }
 root.RelaxiaMedia={prepare,frameReady,MediaFailure};
 if(typeof module!=='undefined')module.exports=root.RelaxiaMedia;
})(typeof window==='undefined'?globalThis:window);
