/* Relaxia gift artwork: locally generated layered SVG, animated by live-stage.css.
 * No remote CDN, no external tracking, and no untrusted SVG markup from the server. */
(function (window) {
  'use strict';
  const A = Object.freeze({
    rose:['#fa5c86','#ffd0d5','🌹','bloom'],
    heart:['#ff618e','#ffb5cb','💗','heartbeat'],
    clap:['#f7b96d','#ffe0a0','👏','bounce'],
    thumbs_up:['#f4bc68','#fff0b7','👍','bounce'],
    star:['#ffcc6a','#fff0ae','⭐','spin'],
    crown:['#f4bb55','#ffe79d','👑','royal'],
    diamond:['#c96fff','#ffbbee','💎','spin'],
    teddy:['#d4a46e','#ffe3b7','🧸','bounce'],
    gift_box:['#f55a92','#fbc9e8','🎁','bounce'],
    cake:['#ff8a89','#ffe1b4','🎂','bounce'],
    balloons:['#a47cff','#ffd5f1','🎈','float'],
    fireworks:['#ffab54','#f1caff','🎆','burst'],
    castle:['#edc479','#ffe9af','🏰','royal'],
    sports_car:['#fd5363','#ffb48f','🏎️','drive'],
    yacht:['#62b9f0','#c5ecff','🛥️','sail'],
    airplane:['#8ab8fb','#e1edff','✈️','fly'],
    unicorn:['#b998ff','#ffd2ef','🦄','float'],
    lion:['#db9c48','#ffe0a0','🦁','royal'],
    galaxy:['#a579ff','#e4a4ff','🌌','orbit'],
    whale:['#59bbeb','#a4e8ff','🐋','swim'],
    dolphin:['#55bfe6','#b3f0ff','🐬','swim'],
    eagle:['#bd9a71','#fbe4ba','🦅','fly'],
    coffee:['#caa37b','#fff1c2','☕','float'],
    flower:['#f19ac4','#ffdded','🌷','bloom'],
    chocolate:['#aa795e','#eac29e','🍫','bounce'],
    butterfly:['#b77afd','#c9f4ff','🦋','float'],
    kiss:['#f47794','#ffd0d9','💋','heartbeat'],
    bouquet:['#fa86a2','#ffd5c9','💐','bloom']
  });
  const cache = Object.create(null);
  const durations = {galaxy:4600, fireworks:3600, whale:4000, lion:4100, castle:4100, airplane:3800, yacht:4000, sports_car:3600, unicorn:3500, eagle:3800};
  function background(code) {
    const a = A[code];
    if (!a) return null;
    if (cache[code]) return cache[code];
    const c1 = a[0], c2 = a[1];
    const markup = '<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180" viewBox="0 0 180 180"><defs>'
      + '<radialGradient id="light" cx="35%" cy="20%" r="75%"><stop stop-color="'+c2+'" stop-opacity=".92"/><stop offset=".58" stop-color="'+c1+'" stop-opacity=".43"/><stop offset="1" stop-color="'+c1+'" stop-opacity=".04"/></radialGradient>'
      + '<linearGradient id="rim" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#fff" stop-opacity=".94"/><stop offset=".45" stop-color="'+c2+'" stop-opacity=".64"/><stop offset="1" stop-color="'+c1+'" stop-opacity=".14"/></linearGradient>'
      + '<filter id="blur"><feGaussianBlur stdDeviation="9"/></filter></defs>'
      + '<ellipse cx="90" cy="155" rx="47" ry="10" fill="'+c1+'" opacity=".42" filter="url(#blur)"/>'
      + '<circle cx="90" cy="83" r="70" fill="'+c1+'" opacity=".25" filter="url(#blur)"/>'
      + '<circle cx="90" cy="82" r="63" fill="url(#light)" stroke="url(#rim)" stroke-width="2"/>'
      + '<path d="M39 63A59 59 0 0 1 119 29" fill="none" stroke="#fff" stroke-opacity=".64" stroke-width="5" stroke-linecap="round"/>'
      + '<path d="M132 116A58 58 0 0 1 75 140" fill="none" stroke="'+c2+'" stroke-opacity=".8" stroke-width="3" stroke-linecap="round"/>'
      + '<path d="M137 27l3 9 9 3-9 3-3 9-3-9-9-3 9-3z" fill="#fff" opacity=".88"/>'
      + '<circle cx="37" cy="122" r="3.8" fill="'+c2+'" opacity=".85"/></svg>';
    cache[code] = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(markup);
    return cache[code];
  }
  function create(g, large) {
    const a = A[g.code], el = document.createElement('span');
    const motion = a ? a[3] : 'bounce';
    el.className = 'gift-figure ' + (large ? 'gift-hero ' : 'gift-art ') + 'gift-motion-' + motion;
    el.setAttribute('aria-hidden', 'true');
    if (a) {
      const img = document.createElement('img');
      img.className = 'gift-backplate';
      img.src = background(g.code);
      img.alt = '';
      img.decoding = 'async';
      img.onerror = () => { img.hidden = true; };
      el.append(img);
    }
    const glyph = document.createElement('span');
    glyph.className = 'gift-glyph';
    glyph.textContent = a ? a[2] : (g.symbol || '🎁');
    el.append(glyph);
    const sparkle = document.createElement('span');
    sparkle.className = 'gift-glint';
    sparkle.textContent = '✦';
    el.append(sparkle);
    return el;
  }
  window.RelaxiaGiftArt = Object.freeze({
    create,
    duration(code,effect) { return durations[code] || (effect === 'depth' ? 3800 : effect === 'animated' ? 2700 : 1600); }
  });
})(window);
