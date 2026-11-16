"use strict";
(function(){
 const yr=document.getElementById("year");if(yr)yr.textContent=String(new Date().getFullYear());
 const anchor=document.getElementById("apk"),pending=document.getElementById("unavailable"),status=document.getElementById("status"),checksum=document.getElementById("checksum");
 async function check(){
  try{
   const result=await fetch("./api/release",{cache:"no-store",headers:{"Accept":"application/json"}});
   if(!result.ok)throw new Error("API unavailable");
   const r=await result.json();
   if(r.available===true&&typeof r.url==="string"&&typeof r.version==="string"){
    const u=new URL(r.url);
    if(u.protocol!=="https:"||u.hostname!=="github.com"||!u.pathname.startsWith("/khtab919/sync/releases/download/"))throw new Error("Invalid download link");
    anchor.href=u.href;anchor.hidden=false;pending.hidden=true;status.textContent="الإصدار المعتمد: "+r.version;checksum.textContent=r.sha256?"SHA-256: "+r.sha256:"";return;
   }
   status.textContent="لا توجد نسخة نهائية معتمدة حاليًا.";
  }catch(_){status.textContent="تعذر التحقق من الإصدار المعتمد الآن."}
  anchor.hidden=true;anchor.removeAttribute("href");pending.hidden=false;checksum.textContent="";
 }
 check();
})();