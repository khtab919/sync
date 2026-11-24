/* Run with: node backend/qa/audience_live_refresh_test.cjs */
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

class Element {
  constructor() {
    this.children = [];
    this.listeners = {};
    this.attributes = {};
    this.classList = { add() {} };
    this.isConnected = true;
    this.textContent = '';
    this.value = '';
  }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = [...children]; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(name, callback) { this.listeners[name] = callback; }
}
const viewers = new Element();
const title = new Element();
title.textContent = 'المشاهدون والمشاركون';
let state = { viewers: 1, participants_visible: [{ user_id: 1, name: 'ليلى', public_id: '101' }] };
let sheet = new Element();
const document = {
  getElementById(id) {
    return id === 'viewers' ? viewers : id === 'sheetTitle' ? title : null;
  },
  createElement() { return new Element(); },
};
const window = {
  RelaxiaRoomModeration: {
    getState: () => state,
    openSheet: () => (sheet = new Element()),
    renderPortrait: () => {},
    showUser: () => {},
  },
  drawState: (updated) => { state = updated; },
};
const source = fs.readFileSync(path.join(__dirname, '../core/static/core/relaxia-audience.js'), 'utf8');
vm.runInNewContext(source, { window, document }, { filename: 'relaxia-audience.js' });
window.RelaxiaAudience.open();
assert.equal(viewers.attributes.role, 'button');
assert.match(sheet.children[0].textContent, /١|1/);
let entries = sheet.children.find(child => child.className === 'audience-entries');
assert.equal(entries.children.length, 1);

window.drawState({ viewers: 2, participants_visible: [
  { user_id: 1, name: 'ليلى', public_id: '101' },
  { user_id: 2, name: 'مريم', public_id: '102' },
] });
entries = sheet.children.find(child => child.className === 'audience-entries');
assert.equal(entries.children.length, 2, 'list refreshes on room state changes');
const search = sheet.children.find(child => child.className === 'audience-search');
search.value = 'مريم';
search.listeners.input();
assert.equal(entries.children.length, 1);
window.drawState({ viewers: 3, participants_visible: [
  { user_id: 1, name: 'ليلى', public_id: '101' },
  { user_id: 2, name: 'مريم', public_id: '102' },
  { user_id: 3, name: 'أحمد', public_id: '103' },
] });
assert.equal(search.value, 'مريم', 'search preserved during updates');
assert.equal(entries.children.length, 1, 'search filter remains active');

search.value = '';
search.listeners.input();
window.drawState({ viewers: 3, participants_visible: [] });
assert.match(entries.children[0].textContent, /أسماء المتصلين لم تصل/, 'never misrepresent missing identities as zero viewers');
title.textContent = 'متجر الهدايا';
const before = entries.children[0].textContent;
window.drawState({ viewers: 0, participants_visible: [] });
assert.equal(entries.children[0].textContent, before, 'do not mutate other room sheets');
console.log('audience live-refresh smoke test passed');
