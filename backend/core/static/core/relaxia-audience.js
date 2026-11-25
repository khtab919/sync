/* Relaxia live audience panel: server-provided presence only.
 * Do not fabricate viewers, fetch profiles from an untrusted origin, or grant
 * moderator privileges from the client. Server validates every control. */
(function () {
  'use strict';
  const viewerButton = document.getElementById('viewers');
  if (!viewerButton) return;

  // Header count is actionable, including for ordinary viewers.
  viewerButton.setAttribute('role', 'button');
  viewerButton.setAttribute('tabindex', '0');
  viewerButton.setAttribute('aria-label', 'عرض الأشخاص الذين يشاهدون البث الآن');
  viewerButton.setAttribute('title', 'عرض المشاهدين');
  viewerButton.classList.add('audience-trigger');

  let active = false;
  let refreshOpenAudience = null;
  function panel() {
    const list = window.RelaxiaRoomModeration?.openSheet?.('المشاهدون والمشاركون');
    if (!list) return;
    const summary = document.createElement('p');
    summary.className = 'audience-summary';
    list.append(summary);
    const search = document.createElement('input');
    search.type = 'search';
    search.className = 'audience-search';
    search.placeholder = 'ابحث عن مشاهد';
    search.setAttribute('aria-label', 'البحث في مشاهدي البث');
    list.append(search);
    const entries = document.createElement('div');
    entries.className = 'audience-entries';
    list.append(entries);

    const extra=new Map();
    let nextPage=null,firstCursor=null,depth=0,lastRefresh=0;
    const more=document.createElement('button');more.textContent='عرض المزيد';
    list.append(more);
    more.addEventListener('click',async()=>{
      if(nextPage==null||more.disabled)return;
      more.disabled=true;
      try{
        const cursor=nextPage;
        const page=await window.RelaxiaRoomModeration.loadAudience(cursor);
        if(cursor!==nextPage)return;
        depth++;
        for(const person of page.participants_visible)extra.set(person.user_id,person);
        nextPage=page.audience_next;draw(true);
      }catch(e){more.textContent='إعادة محاولة تحميل المزيد'}finally{more.disabled=false}
    });
    let lastSnapshot = '';
    function draw(force = false) {
      // A separate room sheet may have replaced this one; never rewrite it.
      if (document.getElementById('sheetTitle')?.textContent !== 'المشاهدون والمشاركون' || !list.isConnected) {
        if (refreshOpenAudience === draw) refreshOpenAudience = null;
        return;
      }
      const data = window.RelaxiaRoomModeration?.getState?.() || {};
      const participants = Array.isArray(data.participants_visible) ? data.participants_visible : [];
      if(firstCursor!==data.audience_next){
        firstCursor=data.audience_next;extra.clear();nextPage=firstCursor;depth=0;
      }
      if(!force && depth && !more.disabled && Date.now()-lastRefresh>5000){
        const initial=firstCursor, limit=depth;
        more.disabled=true;
        lastRefresh=Date.now();
        (async()=>{
          const fresh=new Map();
          let cursor=initial;
          for(let i=0;i<limit && cursor!=null;i++){
            const page=await window.RelaxiaRoomModeration.loadAudience(cursor);
            for(const person of page.participants_visible)fresh.set(person.user_id,person);
            cursor=page.audience_next;
          }
          if(initial!==firstCursor || !list.isConnected)return;
          extra.clear();
          for(const [id,person] of fresh)extra.set(id,person);
          nextPage=cursor;draw(true);
        })().catch(()=>{}).finally(()=>{more.disabled=false});
      }
      more.hidden=nextPage==null;
      const users = [...new Map([...extra.values(),...participants]
        .filter(p => Number.isInteger(Number(p.user_id)) && Number(p.user_id) > 0)
        .map(p => [Number(p.user_id), p])).values()];
      const count = Math.max(0, Number(data.viewers || 0));
      summary.textContent = 'المتصلون الآن: ' + count.toLocaleString('ar') +
        ' · الحسابات الظاهرة: ' + users.length.toLocaleString('ar');
      const term = search.value.trim().toLocaleLowerCase();
      const snapshot = JSON.stringify([count, term, users]);
      if (!force && lastSnapshot === snapshot) return;
      lastSnapshot = snapshot;
      entries.replaceChildren();
      if (!users.length) {
        const empty = document.createElement('p');
        empty.textContent = count > 0
          ? 'عدد المشاهدين متاح، لكن أسماء المتصلين لم تصل من الخادم. حاول تحديث الغرفة.'
          : 'لا يوجد مشاهدون متصلون حالياً.';
        entries.append(empty);
        return;
      }
      const found = users.filter(p => {
        const name = String(p.name || '').toLocaleLowerCase();
        return name.includes(term) || String(p.public_id || '').includes(term);
      });
      for (const person of found) {
        const entry = document.createElement('button');
        entry.type = 'button';
        entry.className = 'audience-person';
        const avatar = document.createElement('span');
        avatar.className = 'audience-avatar';
        if (window.RelaxiaRoomModeration?.renderPortrait) window.RelaxiaRoomModeration.renderPortrait(avatar, person);
        else avatar.textContent = (person.name || 'م').slice(0, 1);
        const description = document.createElement('span');
        description.className = 'audience-person-info';
        const label = document.createElement('strong');
        label.textContent = person.name || 'مستخدم';
        const detail = document.createElement('small');
        const level = Number(person.level) > 0 ? 'مستوى ' + Number(person.level) : '';
        detail.textContent = [person.vip ? 'VIP' : '', level, person.public_id ? 'ID: ' + person.public_id : '']
          .filter(Boolean).join(' · ') || 'مشاهد';
        description.append(label, detail);
        entry.append(avatar, description);
        entry.addEventListener('click', () => {
          if (window.RelaxiaRoomModeration?.showUser)
            window.RelaxiaRoomModeration.showUser(person);
          else if (window.LayaliLive?.canOpenProfile?.())
            window.LayaliLive.openProfile(Number(person.user_id));
        });
        entries.append(entry);
      }
      if (!found.length) {
        const empty = document.createElement('p');
        empty.textContent = 'لا يوجد مستخدم مطابق للبحث.';
        entries.append(empty);
      }
    }
    search.addEventListener('input', () => draw(true));
    refreshOpenAudience = draw;
    draw(true);
  }

  function open(e) {
    if (e?.type === 'keydown' && e.key !== 'Enter' && e.key !== ' ') return;
    e?.preventDefault();
    if (active) return;
    active = true;
    try { panel(); } finally { active = false; }
  }
  window.RelaxiaAudience = {open, refresh: () => refreshOpenAudience?.()};
  // The room's normal server poll refreshes the visible audience without
  // reopening the sheet or losing the visitor's search text.
  const previousDrawState = window.drawState;
  if (typeof previousDrawState === 'function') {
    window.drawState = function (newState) {
      const result = previousDrawState.apply(this, arguments);
      window.RelaxiaAudience.refresh();
      return result;
    };
  }
  viewerButton.addEventListener('click', open);
  viewerButton.addEventListener('keydown', open);
})();
