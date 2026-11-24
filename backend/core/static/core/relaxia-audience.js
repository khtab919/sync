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
  function panel() {
    const data = typeof state === 'object' && state ? state : {};
    const participants = Array.isArray(data.participants_visible) ? data.participants_visible : [];
    const users = [...new Map(participants
      .filter(p => Number.isInteger(Number(p.user_id)) && Number(p.user_id) > 0)
      .map(p => [Number(p.user_id), p])).values()];
    const list = sheet('المشاهدون والمشاركون');
    const summary = document.createElement('p');
    summary.className = 'audience-summary';
    summary.textContent = 'المتصلون الآن: ' + Number(data.viewers || 0).toLocaleString('ar') +
      ' · الحسابات الظاهرة: ' + users.length.toLocaleString('ar');
    list.append(summary);
    if (!users.length) {
      note(list, 'لا يوجد مشاهدون متصلون حالياً.');
      return;
    }
    const search = document.createElement('input');
    search.type = 'search';
    search.className = 'audience-search';
    search.placeholder = 'ابحث عن مشاهد';
    search.setAttribute('aria-label', 'البحث في مشاهدي البث');
    list.append(search);
    const entries = document.createElement('div');
    entries.className = 'audience-entries';
    list.append(entries);

    function draw() {
      entries.replaceChildren();
      const term = search.value.trim().toLocaleLowerCase();
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
        renderPortrait(avatar, person);
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
      if (!found.length) note(entries, 'لا يوجد مستخدم مطابق للبحث.');
    }
    search.addEventListener('input', draw);
    draw();
  }

  function open(e) {
    if (e?.type === 'keydown' && e.key !== 'Enter' && e.key !== ' ') return;
    e?.preventDefault();
    if (active) return;
    active = true;
    try { panel(); } finally { active = false; }
  }
  window.RelaxiaAudience = {open};
  viewerButton.addEventListener('click', open);
  viewerButton.addEventListener('keydown', open);
})();
