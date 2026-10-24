/** Ekran główny: zakładki Na żywo i Archiwum (panel "teraz leci" i rzędy kanałów) oraz Ustawienia. */
import { $, h, clear } from '../dom.js';
import * as router from '../router.js';
import * as store from '../store.js';
import { focusEl, onReveal } from '../focus.js';
import { createNumberEntry, isDigit } from '../numberEntry.js';
import { formatTime, progressPercent, minutesLeft, initials } from '../format.js';
import { toast } from '../toast.js';
import * as settings from './settings.js';

const AUTH_ERRORS = ['login_rejected', 'not_logged_in', 'unauthorized'];

const rowsEl = $('#rows');
const statusEl = $('#home-status');
const tabs = [...document.querySelectorAll('.tab')];
const entry = createNumberEntry({ onCommit: focusChannelNumber });
let view = 'live';
let heroChannelId = null;       // kanał, który pokazuje panel "teraz leci"

tabs.forEach(tab => tab.addEventListener('click', () => showView(tab.dataset.tab)));
onReveal(scrollToFocused);
document.addEventListener('focusin', event => {
  const id = event.target.dataset?.channel;
  if (id && router.current()?.name === 'home') updateHero(store.byId(id));
});

export async function enter() {
  entry.reset();
  await load();
}

export const leave = () => entry.reset();

/** Po wejściu fokus ląduje na pierwszym kanale, żeby od razu dało się nacisnąć OK. */
export const defaultFocus = () => $('.tile') ?? $('[data-key="status:retry"]');

/** Powrót z odtwarzacza albo archiwum: tylko odśwież paski postępu. */
export const resume = () => refreshProgress();

export function onKey(key) {
  if (isDigit(key)) {
    entry.type(key);
    return true;
  }
  if (key === 'Enter' && entry.pending) {
    entry.commit();
    return true;
  }
  return false;
}

async function load() {
  showStatus('Ładowanie kanałów...');
  try {
    await store.loadChannels();
  } catch (error) {
    onLoadFailed(error);
    return;
  }
  statusEl.hidden = true;
  showView(view);
  store.loadGuide(0).then(refreshProgress).catch(() => {});   // program dociąga się w tle
}

function onLoadFailed(error) {
  if (AUTH_ERRORS.includes(error.code)) {
    toast('Zaloguj się ponownie.');
    router.go('login', { reset: true });
    return;
  }
  showStatus(error.message, { retry: true });
}

function showStatus(text, { retry = false } = {}) {
  clear(statusEl).append(h('p', {}, text));
  if (retry) {
    const button = h('button', { class: 'f btn', dataset: { key: 'status:retry' }, onclick: load }, 'Spróbuj ponownie');
    statusEl.append(button);
    focusEl(button);
  }
  statusEl.hidden = false;
}

// ---------- zakładki ----------

function showView(next) {
  view = next;
  tabs.forEach(tab => tab.classList.toggle('selected', tab.dataset.tab === next));
  const isSettings = next === 'settings';
  $('#channel-view').hidden = isSettings;
  $('#settings-view').hidden = !isSettings;

  if (isSettings) settings.render();
  else renderRows();
}

// ---------- panel "teraz leci" ----------

function updateHero(channel) {
  heroChannelId = channel.id;
  const { now, next } = store.nowNext(channel);
  $('#h-num').textContent = channel.number;
  $('#h-name').textContent = channel.name;
  $('#h-badge').textContent = channel.catchupDays ? `Archiwum: ${channel.catchupDays} dni` : 'Bez archiwum';

  if (!now) {
    $('#h-title').textContent = 'Brak danych o programie';
    $('#h-time').textContent = '';
    $('#h-bar').style.width = '0%';
    $('#h-next').textContent = '';
    return;
  }
  $('#h-title').textContent = now.title;
  $('#h-time').textContent = `${formatTime(now.start)} do ${formatTime(now.end)}, jeszcze ${minutesLeft(now)} min`;
  $('#h-bar').style.width = `${progressPercent(now)}%`;
  $('#h-next').textContent = next ? `Dalej: ${formatTime(next.start)} ${next.title}` : '';
}

// ---------- rzędy kanałów ----------

function renderRows() {
  const visible = channel => view === 'live' || channel.catchupDays > 0;
  clear(rowsEl);

  store.categories(visible).forEach(category => {
    const items = store.allChannels().filter(channel => visible(channel) && channel.category === category);
    rowsEl.append(h('div', { class: 'row' },
      h('h2', { class: 'row-title' }, category),
      h('div', { class: 'tiles', 'data-nav-row': true }, items.map(tile)),
    ));
  });
  rowsEl.append(h('div', { class: 'rows-end' }));
  rowsEl.scrollTop = 0;

  const first = $('.tile');
  if (first) updateHero(store.byId(first.dataset.channel));
}

function tile(channel) {
  const { now } = store.nowNext(channel);
  return h('button', {
    class: 'f tile',
    dataset: { key: `tile:${channel.id}`, channel: channel.id },
    onclick: () => open(channel),
  },
  h('span', { class: 'tile-num' }, String(channel.number)),
  logo(channel),
  h('span', { class: 'tile-name' }, channel.name),
  progressBar(now));
}

/** Logo z usługi, a gdy się nie wczyta, skrót nazwy. */
function logo(channel) {
  const fallback = h('span', { class: 'tile-logo' }, initials(channel.name));
  if (!channel.logoUrl) return fallback;
  const image = h('img', { class: 'tile-logo', src: channel.logoUrl, alt: '', referrerpolicy: 'no-referrer' });
  image.addEventListener('error', () => image.replaceWith(fallback));
  return image;
}

function progressBar(program) {
  const fill = h('i');
  fill.style.width = program ? `${progressPercent(program)}%` : '0%';
  return h('div', { class: 'bar' }, fill);
}

function refreshProgress() {
  document.querySelectorAll('.tile').forEach(element => {
    const { now } = store.nowNext(store.byId(element.dataset.channel));
    $('.bar > i', element).style.width = now ? `${progressPercent(now)}%` : '0%';
  });
  const hero = store.byId(heroChannelId);
  if (hero) updateHero(hero);
}

function open(channel) {
  if (view === 'archive') router.go('archive', { params: { channelId: channel.id } });
  else router.go('player', { params: { channelId: channel.id, mode: 'live' } });
}

function focusChannelNumber(number) {
  const channel = store.byNumber(number);
  const element = channel && $(`.tile[data-channel="${CSS.escape(channel.id)}"]`);
  if (element) focusEl(element);
  else toast(channel ? `Kanał ${number} nie jest na tej liście` : `Nie ma kanału ${number}`);
}

/** Ustawia zaznaczony kafelek tak, żeby poprzedni był jeszcze widoczny, a rząd był na górze listy. */
function scrollToFocused(element) {
  const tiles = element.closest('.tiles');
  if (!tiles) return;
  tiles.scrollLeft = Math.max(0, element.offsetLeft - 64 - 360);
  rowsEl.scrollTop = element.closest('.row').offsetTop - 8;
}
