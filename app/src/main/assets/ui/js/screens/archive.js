/** Archiwum jednego kanału: po lewej dni, po prawej zakończone programy z wybranego dnia. */
import { $, h, clear } from '../dom.js';
import * as router from '../router.js';
import * as store from '../store.js';
import { formatDate, formatTime, dayLabel } from '../format.js';
import { onReveal } from '../focus.js';

const daysEl = $('#a-days');
const progsEl = $('#a-progs');

let channel = null;
let loadToken = 0;      // ignoruje spóźnione odpowiedzi przy szybkim przełączaniu dni

onReveal(element => {
  if (element.closest('.progs')) element.scrollIntoView({ block: 'nearest' });
});
document.addEventListener('focusin', event => {
  const day = event.target.dataset?.day;
  if (day !== undefined && router.current()?.name === 'archive') showPrograms(Number(day));
});

export function enter({ channelId }) {
  channel = store.byId(channelId);
  $('#a-title').textContent = channel.name;
  $('#a-sub').textContent = `Archiwum z ostatnich ${channel.catchupDays} dni`;

  clear(daysEl);
  for (let daysAgo = 0; daysAgo < channel.catchupDays; daysAgo++) {
    daysEl.append(dayButton(daysAgo));
  }
  showPrograms(0);
}

export const defaultFocus = () => $('.day');

function dayButton(daysAgo) {
  const date = formatDate(Date.now() - daysAgo * 24 * 3600 * 1000);
  return h('button', {
    class: 'f day',
    dataset: { key: `day:${daysAgo}`, day: String(daysAgo) },
    onclick: () => $('.prog')?.focus(),
  }, dayLabel(daysAgo), h('small', {}, date));
}

async function showPrograms(daysAgo) {
  const mine = ++loadToken;
  document.querySelectorAll('.day').forEach(day => day.classList.toggle('on', Number(day.dataset.day) === daysAgo));
  clear(progsEl).append(h('div', { class: 'empty' }, 'Ładowanie programu...'));

  try {
    await store.ensureGuide(daysAgo);
  } catch (error) {
    if (mine === loadToken) clear(progsEl).append(h('div', { class: 'empty' }, error.message));
    return;
  }
  if (mine !== loadToken) return;

  const now = Date.now();
  const finished = store.programsOfDay(channel.id, daysAgo).filter(program => program.end <= now);
  clear(progsEl);
  if (!finished.length) {
    progsEl.append(h('div', { class: 'empty' }, 'Brak zakończonych programów z tego dnia.'));
    return;
  }
  finished.forEach((program, i) => progsEl.append(programButton(program, daysAgo, i)));
}

function programButton(program, daysAgo, i) {
  return h('button', {
    class: 'f prog',
    dataset: { key: `prog:${daysAgo}:${i}` },
    onclick: () => router.go('player', { params: { channelId: channel.id, mode: 'archive', program } }),
  }, h('span', { class: 't' }, formatTime(program.start)), h('span', {}, program.title));
}
