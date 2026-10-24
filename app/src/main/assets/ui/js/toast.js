import { $ } from './dom.js';

let timer = null;

export function toast(text) {
  const box = $('#toast');
  box.textContent = text;
  box.classList.add('show');
  clearTimeout(timer);
  timer = setTimeout(() => box.classList.remove('show'), 2200);
}
