package com.example.ui.screens

/**
 * Compatibility repair for older Relaxia live-room pages still served by the
 * deployment. Keep existing DOM elements and event listeners; never replace
 * the room itself, change tickets, or alter the media connection.
 *
 * This script is injected only after LiveScreens verifies its exact trusted
 * origin and /api/live/.../room pathname.
 */
internal fun relaxiaLiveFooterRepairScript(): String = """
    (function () {
      var stage = document.getElementById('stage');
      if (!stage) return 'missing-stage';
      var footer = stage.querySelector('footer');
      var bottom = footer && footer.querySelector('.bottom');
      if (!bottom) return 'missing-footer';

      var actions = bottom.querySelector('.quick-actions');
      if (!actions) {
        actions = document.createElement('div');
        actions.className = 'quick-actions';
        actions.setAttribute('role', 'group');
        actions.setAttribute('aria-label', 'أدوات البث');
        bottom.insertBefore(actions, bottom.firstChild);
      }
      // Move existing nodes rather than cloning them: all click handlers stay intact.
      ['more', 'gift', 'speaker', 'guests', 'micQuick', 'share', 'bigGiftBtn']
        .forEach(function (id) {
          var button = document.getElementById(id);
          if (button && footer.contains(button) && button.parentElement !== actions) {
            actions.appendChild(button);
          }
        });
      var composer = footer.querySelector('#composer');
      if (composer && composer.parentElement !== bottom) bottom.appendChild(composer);
      if (composer) bottom.appendChild(composer); // Always keep composer on its own row.

      if (!document.getElementById('relaxia-native-live-controls-repair')) {
        var style = document.createElement('style');
        style.id = 'relaxia-native-live-controls-repair';
        style.textContent = [
          '#stage footer{background:linear-gradient(0deg,#08152E 0%,#122B54 100%)!important;color:#F5F9FF!important;border-top:1px solid #86ADE080!important;padding:8px 10px max(9px,env(safe-area-inset-bottom,0px))!important}',
          '#stage footer .bottom{display:flex!important;flex-direction:column!important;align-items:stretch!important;justify-content:flex-start!important;gap:8px!important;min-width:0!important;width:100%!important}',
          '#stage footer .quick-actions{display:flex!important;flex-direction:row!important;align-items:center!important;justify-content:flex-start!important;width:100%!important;max-width:100%!important;min-height:44px!important;gap:clamp(4px,1.5vw,8px)!important;direction:rtl!important}',
          '#stage footer .quick-actions>button[hidden]{display:none!important}',
          '#stage footer .quick-actions .tool-circle{box-sizing:border-box!important;display:grid;place-items:center;flex:0 0 clamp(39px,11vw,44px)!important;width:clamp(39px,11vw,44px)!important;height:clamp(39px,11vw,44px)!important;min-height:39px!important;padding:0!important;background:#214B83!important;color:#FFFFFF!important;border:1px solid #A0C7FF!important;opacity:1!important;box-shadow:none!important}',
          '#stage footer .quick-actions .tool-circle svg{width:22px!important;height:22px!important;color:#FFFFFF!important;stroke:currentColor!important;opacity:1!important}',
          '#stage footer .quick-actions .tool-circle svg[fill="currentColor"]{fill:currentColor!important}',
          '#stage footer .quick-actions .gift-fab{margin-inline-start:auto!important;flex:0 0 44px!important;width:44px!important;height:44px!important;min-height:44px!important}',
          '#stage footer #composer{box-sizing:border-box!important;display:flex!important;flex:0 0 50px!important;align-items:center!important;width:100%!important;max-width:100%!important;min-width:0!important;min-height:50px!important;padding:4px 7px!important;gap:4px!important;background:#173C73!important;border:1px solid #A4C9FF!important;border-radius:22px!important}',
          '#stage footer #composer input{flex:1 1 auto!important;width:0!important;min-width:0!important;padding:9px 6px!important;color:#FFFFFF!important;caret-color:#FFFFFF!important;font-size:15px!important;background:transparent!important}',
          '#stage footer #composer input::placeholder{color:#ECF3FF!important;opacity:1!important}',
          '#stage footer #composer .composer-send{display:grid!important;place-items:center!important;flex:0 0 40px!important;width:40px!important;height:40px!important;min-height:40px!important;background:#397ACB!important;color:#FFFFFF!important;border-radius:50%!important}',
          '#stage footer #composer .composer-emoji{color:#FFFFFF!important;opacity:1!important}',
          'html[data-theme="light"] body.audio-mode #stage footer .tool-circle,html[data-theme="dark"] body.audio-mode #stage footer .tool-circle{color:#FFFFFF!important}',
          '#stage footer .tool-circle:focus-visible,#stage footer #composer:focus-within{outline:2px solid #FFFFFF!important;outline-offset:2px!important}',
          'body.chat-focused #stage footer .quick-actions{display:flex!important;gap:4px!important}',
          'body.chat-focused #stage footer .bottom{gap:0!important}',
          'body.chat-focused #stage footer{padding-top:5px!important}',
          '@media(max-height:600px){body.audio-mode.chat-focused #audioStage.seat-grid{display:none!important}body.audio-mode.chat-focused #stage{--room-chat-height:clamp(70px,calc(var(--room-viewport-height,100dvh) - var(--room-footer-height) - 30px),220px)}}',
          '#stage footer #composer input{-webkit-user-select:text!important;user-select:text!important}',
          '#stage footer #composer .composer-send{min-width:44px!important;width:44px!important;min-height:44px!important;height:44px!important}',
          '#sheet[open]{position:fixed!important;inset:auto 9px 7px!important;max-height:min(74dvh,calc(var(--room-viewport-height,100dvh) - 24px))!important;overflow-y:auto!important;touch-action:pan-y!important}'
        ].join('');
        style.textContent += `
/* The room keeps its own viewport, media tiles, moderation and keyboard layout. */
#stage {
  --chat-ink: #fff;
  --chat-name: #c7e0ff;
  --chat-shadow: 0 1px 2px #081321, 0 0 4px #081321, 0 1px 8px #081321;
}
body.audio-mode #stage {
  --chat-ink: #102643;
  --chat-name: #194e91;
  --chat-shadow: none;
}
html[data-theme=dark] body.audio-mode #stage {
  background: radial-gradient(ellipse at 80% 0%, #244b77, transparent 58%),
              linear-gradient(155deg, #152b47, #101e33) !important;
  --chat-ink: #f6f9ff;
  --chat-name: #b4d6ff;
}
html body:not(.audio-mode) #stage .shade {
  background: linear-gradient(to bottom, rgba(7,18,36,.35), transparent 18%,
              transparent 64%, rgba(7,18,36,.48)) !important;
  pointer-events: none;
}
html[data-theme] body #stage #messages[role=log],
html[data-theme] body #stage #messages[role=log] :is(.comment-bubble,.comment-empty) {
  background: none !important;
  border: 0 !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  -webkit-backdrop-filter: none !important;
  backdrop-filter: none !important;
}
html body #stage #messages[role=log] {
  justify-content: flex-start;
  touch-action: pan-y;
}
html body #stage #messages[role=log] .comment-bubble {
  max-width: min(100%, 38rem) !important;
  flex: none;
  gap: 8px;
  margin: 1px 0 !important;
  padding: 3px 2px !important;
  align-items: center;
  touch-action: pan-y;
}
html body #stage #messages[role=log] :is(.comment-body,.comment-empty) {
  color: var(--chat-ink) !important;
  text-shadow: var(--chat-shadow);
  font-size: 14px !important;
  line-height: 1.55 !important;
  overflow-wrap: anywhere;
}
html body #stage #messages[role=log] .comment-author {
  color: var(--chat-name) !important;
  font-weight: 750;
}
html body #stage #messages[role=log] .comment-actions {
  color: var(--chat-ink) !important;
  text-shadow: var(--chat-shadow);
  background: transparent !important;
  border: 0 !important;
  box-shadow: none !important;
}
html body:not(.audio-mode) #stage footer {
  background: linear-gradient(0deg, rgba(10,26,51,.72), transparent) !important;
  border: 0 !important;
  box-shadow: none !important;
}
html body:not(.audio-mode) #stage :is(header .identity,header .pill,header .round, footer #composer, footer .quick-actions .tool-circle) {
  background: rgba(14,38,70,.72) !important;
  border: 1px solid rgba(205,226,255,.29) !important;
  box-shadow: none !important;
  color: white !important;
}
html body:not(.audio-mode) #stage footer #composer:focus-within {
  outline: 2px solid #bbd9ff !important;
}
html body #stage #messages .comment-bubble:focus-visible {
  outline: 2px solid var(--chat-name);
  outline-offset: 2px;
}
html[data-theme=dark] body.audio-mode #stage #audioStage .audio-seat:not(.host-seat) .audio-name {
  color: #edf5ff !important;
  background: #244363;
}
html[data-theme=dark] body.audio-mode #stage #audioStage .audio-seat small {
  color: #cee1f7 !important;
}
`;
        document.head.appendChild(style);
      }
      // The WebView has now been constrained by Compose imePadding. Re-measure
      // the browser viewport during keyboard animation and keep the composer
      // in that visible area, including on servers with older live-stage.js.
      // Legacy live assets have conflicting bottom-sheet inset rules. Use
      // explicit pixel coordinates so the modal stays in the Android viewport.
      function positionNativeRoomSheet() {
        if(document.querySelector("link[href*=royal4]"))return;
        var modal = document.getElementById('sheet');
        if (!modal || !modal.open) return;
        var layoutHeight = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);
        var v = window.visualViewport;
        var usable = v && v.height >= 240 && v.height <= layoutHeight + 2;
        var visibleHeight = usable ? Math.min(layoutHeight, v.height) : layoutHeight;
        if (visibleHeight < 160) return;
        var offset = usable ? Math.max(0, v.offsetTop || 0) : 0;
        var h = Math.max(180, Math.min(760, Math.floor(visibleHeight * .78)));
        var y = Math.max(8, Math.floor(offset + visibleHeight - h - 8));
        var rules = {
          position:'fixed', top:y+'px', bottom:'auto',
          left:'10px', right:'10px', margin:'0 auto',
          width:'calc(100% - 20px)', height:h+'px',
          maxHeight:h+'px', minHeight:'0px',
          overflowY:'auto', overscrollBehavior:'contain'
        };
        Object.keys(rules).forEach(function(k){
          modal.style.setProperty(k.replace(/[A-Z]/g, function(c){return '-'+c.toLowerCase()}), rules[k], 'important');
        });
      }
      function measureVisibleRoom() {
        var viewport = window.visualViewport;
        var layoutHeight = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);
        var usable = viewport && viewport.height >= 240 && viewport.height <= layoutHeight + 2;
        var height = Math.max(1, Math.floor(usable ? Math.min(layoutHeight, viewport.height) : layoutHeight));
        document.documentElement.style.setProperty('--room-viewport-height', height + 'px');
        positionNativeRoomSheet();
        // Native repair mirrors the production footer measurement for older
        // server assets, so messages and seats never sit underneath controls.
        var footerHeight = Math.ceil(footer.getBoundingClientRect().height);
        if (footerHeight > 0) stage.style.setProperty('--room-footer-height', Math.max(124, footerHeight + 6) + 'px');
      }
      if (!window.relaxiaLiveKeyboardFixAttached) {
        window.relaxiaLiveKeyboardFixAttached = true;
        var input = document.getElementById('text');
        if (input) {
          input.setAttribute('enterkeyhint', 'send');
          input.addEventListener('focus', function () {
            document.body.classList.add('chat-focused');
            requestAnimationFrame(measureVisibleRoom);
          });
          input.addEventListener('blur', function () {
            document.body.classList.remove('chat-focused');
            requestAnimationFrame(measureVisibleRoom);
          });
          document.addEventListener('click', function (event) {
            var button = event.target.closest('#more,#guests,#bigGiftBtn,#emojiBtn,#gift,#speaker,#micQuick,#share');
            if (button && document.activeElement === input) input.blur();
          }, true);
        }
        window.addEventListener('resize', measureVisibleRoom, {passive:true});
        if (window.visualViewport) {
          window.visualViewport.addEventListener('resize', measureVisibleRoom, {passive:true});
          window.visualViewport.addEventListener('scroll', positionNativeRoomSheet, {passive:true});
        }
        var liveSheet = document.getElementById('sheet');
        if (liveSheet && typeof MutationObserver !== 'undefined') {
          window.relaxiaLiveDialogObserver = new MutationObserver(function() { positionNativeRoomSheet(); });
          window.relaxiaLiveDialogObserver.observe(liveSheet, {attributes:true, attributeFilter:['open']});
        }
        if (typeof ResizeObserver !== 'undefined') {
          window.relaxiaLiveFooterObserver = new ResizeObserver(measureVisibleRoom);
          window.relaxiaLiveFooterObserver.observe(footer);
        }
      }
      // Compatibility for APKs connected to a server that has not yet
      // published rooms-controls-24. Do not alter the room connection, tokens,
      // media tracks, existing approved speakers or server account data.
      if (document.body.classList.contains('audio-mode') &&
          !document.querySelector('link[href*="rooms-controls-24"]') &&
          !window.relaxiaEightSeatFallbackInstalled) {
        window.relaxiaEightSeatFallbackInstalled = true;
        var audioGrid = document.getElementById('audioStage');
        if (audioGrid) {
          var legacyStyle = document.createElement('style');
          legacyStyle.id = 'relaxia-legacy-eight-seats';
          legacyStyle.textContent = [
            'body.audio-mode #stage{background:radial-gradient(ellipse at 77% 20%,#6D89B24A,transparent 46%),radial-gradient(ellipse at 14% 77%,#955A7A38,transparent 54%),linear-gradient(155deg,#0B2039,#1C334B 50%,#27223A)!important}',
            'html[data-theme="light"] body.audio-mode #stage{background:radial-gradient(ellipse at 78% 15%,#B7C9DEAA,transparent 52%),linear-gradient(155deg,#EDF1F4,#E3E9EF 55%,#EFE7ED)!important}',
            'body.audio-mode #audioStage.seat-grid{display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;align-content:center!important;gap:10px 6px!important}',
            'body.audio-mode #audioStage .audio-seat{border:1px solid #B5CEE655!important;border-radius:19px!important;padding:9px 2px!important;background:linear-gradient(155deg,#C9DDF31E,#233C5B20)!important;min-height:105px!important}',
            'body.audio-mode #audioStage .audio-seat:first-child{border-color:#D8B88799!important}',
            'body.audio-mode #audioStage .audio-seat .seat-plus{background:linear-gradient(140deg,#547CA14D,#24496B7A)!important;border:1px solid #9FC5EEA0!important;color:#F1F6FD!important}',
            'body.audio-mode #audioStage .seat-plus svg{display:block;width:25px;height:25px}',
            'body.audio-mode #audioStage .audio-seat .audio-name{color:#F2F7FC!important;font-weight:650!important}',
            'html[data-theme="light"] body.audio-mode #audioStage .audio-seat{background:#FFFFFFB8!important;border-color:#A9BCCD70!important}',
            'html[data-theme="light"] body.audio-mode #audioStage .audio-seat .audio-name{color:#24405D!important}',
            '#stage footer .gift-fab svg,#stage footer .composer-emoji svg,#stage footer .composer-send svg{display:block!important;width:23px;height:23px}'
          ].join('');
          document.head.appendChild(legacyStyle);
          var microphonePlus = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 10v2a7 7 0 0 0 11.3 5.5M12 19v2M8.5 21h7M19 11v6M16 14h6"/></svg>';
          var giftIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M2.5 10h19V7H2.5zM12 7v14M12 7c-4 0-6-1-6-3.1 0-2.9 4-2.4 6 3.1zm0 0c4 0 6-1 6-3.1 0-2.9-4-2.4-6 3.1z"/></svg>';
          var smileIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M8 14.5q4 4 8 0M9 9h.01M15 9h.01"/></svg>';
          var sendIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 14-8-4 16-4-7-6-1zM11 13l8-9"/></svg>';
          [['bigGiftBtn',giftIcon],['emojiBtn',smileIcon],['sendComment',sendIcon]].forEach(function(pair){
            var button = document.getElementById(pair[0]);
            if (button) button.innerHTML = pair[1];
          });
          // Keep any previously approved extra speakers visible until their
          // legacy live room ends; never revoke a seat from a rendering fix.
          function refreshLegacyAudioSeats() {
            var seats = audioGrid.querySelectorAll('.audio-seat');
            var occupied = audioGrid.querySelectorAll('.audio-seat:not(.empty-seat)').length;
            var count = Math.max(8, occupied);
            Array.prototype.forEach.call(seats,function(seat,index){
              seat.style.display = index < count ? '' : 'none';
              if (seat.classList.contains('empty-seat')) {
                var plus = seat.querySelector('.seat-plus');
                if (plus && !plus.querySelector('svg')) plus.innerHTML = microphonePlus;
              }
            });
          }
          refreshLegacyAudioSeats();
          if (typeof MutationObserver !== 'undefined') {
            window.relaxiaEightSeatObserver = new MutationObserver(refreshLegacyAudioSeats);
            window.relaxiaEightSeatObserver.observe(audioGrid, {childList:true});
          }
        }
      }
      measureVisibleRoom();
      return 'repaired';
    })();
""".trimIndent()
