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
          '#stage footer #composer input{-webkit-user-select:text!important;user-select:text!important}',
          '#stage footer #composer .composer-send{min-width:44px!important;width:44px!important;min-height:44px!important;height:44px!important}',
          '#sheet[open]{position:fixed!important;inset:auto 9px 7px!important;max-height:min(74dvh,calc(var(--room-viewport-height,100dvh) - 24px))!important;overflow-y:auto!important;touch-action:pan-y!important}'
        ].join('');
        document.head.appendChild(style);
      }
      // The WebView has now been constrained by Compose imePadding. Re-measure
      // the browser viewport during keyboard animation and keep the composer
      // in that visible area, including on servers with older live-stage.js.
      function measureVisibleRoom() {
        var viewport = window.visualViewport;
        var layoutHeight = document.documentElement.clientHeight || window.innerHeight;
        var height = Math.max(1, Math.floor(Math.min(layoutHeight, viewport ? viewport.height : layoutHeight)));
        document.documentElement.style.setProperty('--room-viewport-height', height + 'px');
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
        if (window.visualViewport) window.visualViewport.addEventListener('resize', measureVisibleRoom, {passive:true});
        if (typeof ResizeObserver !== 'undefined') {
          window.relaxiaLiveFooterObserver = new ResizeObserver(measureVisibleRoom);
          window.relaxiaLiveFooterObserver.observe(footer);
        }
      }
      measureVisibleRoom();
      return 'repaired';
    })();
""".trimIndent()
