/**
 * Odtwarzacz. Obraz gra w natywnym odtwarzaczu pod spodem, a tu jest tylko nakładka (OSD):
 * numer, nazwa, program, postęp i stan połączenia. Góra i dół albo CH+ i CH- zmieniają kanał,
 * cyfry wybierają kanał po numerze.
 */
import { $ } from '../dom.js';
import { call, on } from '../native.js';
import * as router from '../router.js';
import * as store from '../store.js';
import * as prefs from '../prefs.js';
import { createNumberEntry, isDigit } from '../numberEntry.js';
import { formatTime, minutesLeft, progressPercent } from '../format.js';
import { toast } from '../toast.js';

const OSD_VISIBLE_MS = 4000;
const ZAP_DEBOUNCE_MS = 350;

const ui = {
  osd: $('#p-osd'), flag: $('#p-flag'), flagText: $('#p-flag-text'), logo: $('#p-logo'),
  num: $('#p-num'), name: $('#p-name'), title: $('#p-title'), time: $('#p-time'),
  bar: $('#p-bar'), next: $('#p-next'), status: $('#p-status'),
  statusText: $('#p-status-text'), spinner: $('#p-spinner'),
};

const entry = createNumberEntry({ onCommit: zapToNumber });

let channel = null;
let mode = 'live';              // 'live' albo 'archive'
let program = null;             // program z archiwum
let positionMs = 0;
let status = 'loading';         // 'loading' | 'playing' | 'error'
let message = '';
let generation = 0;             // numer ostatniego żądania odtwarzania
let subscriptions = [];
let ticker = null;
let osdTimer = null;
let zapTimer = null;

export function enter({ channelId, mode: startMode, program: startProgram }) {
  channel = store.byId(channelId);
  mode = startMode;
  program = startProgram ?? null;
  positionMs = 0;

  subscriptions = [
    on('player.state', onState),
    on('player.progress', payload => { positionMs = payload.positionMs; }),
    on('player.stopped', () => router.back()),
  ];
  ticker = setInterval(render, 1000);
  start();
  showOsd();
}

export function leave() {
  subscriptions.forEach(unsubscribe => unsubscribe());
  clearInterval(ticker);
  clearTimeout(osdTimer);
  clearTimeout(zapTimer);
  entry.reset();
  call('stop').catch(() => {});
}

export function onKey(key) {
  if (isDigit(key)) {
    entry.type(key);
    showOsd();
    return true;
  }
  switch (key) {
    case 'ArrowUp': case 'ChannelUp': case 'PageUp': zap(1); return true;
    case 'ArrowDown': case 'ChannelDown': case 'PageDown': zap(-1); return true;
    case 'Enter':
      if (entry.pending) entry.commit();
      else if (status === 'error') start();
      else showOsd();
      return true;
    case 'ArrowLeft': case 'ArrowRight': showOsd(); return true;
    default: return false;      // Escape i Backspace obsługuje router
  }
}

// ---------- odtwarzanie ----------

function start() {
  const mine = ++generation;
  setStatus('loading');
  call('play', playParams()).catch(error => {
    if (mine === generation) setStatus('error', error.message);
  });
}

function playParams() {
  if (mode === 'live') {
    const region = channel.isRegional ? prefs.getRegion(channel.id) : null;
    return region ? { channelId: channel.id, region } : { channelId: channel.id };
  }
  return {
    channelId: channel.id,
    programId: program.id,
    start: Math.floor(program.start / 1000),
    end: Math.floor(program.end / 1000),
  };
}

function onState({ state, message: text }) {
  setStatus(state, text ?? '');
  if (state === 'playing') showOsd();    // od teraz nakładka może się schować po chwili
}

function setStatus(next, text = '') {
  status = next;
  message = text;
  render();
}

// ---------- zmiana kanału ----------

function zap(step) {
  const list = store.allChannels();
  const i = list.indexOf(channel);
  switchTo(list[(i + step + list.length) % list.length]);
}

function zapToNumber(number) {
  const target = store.byNumber(number);
  if (target) switchTo(target);
  else toast(`Nie ma kanału ${number}`);
}

/** Nakładka zmienia się od razu, a samo odtwarzanie rusza po chwili, żeby szybkie zapping nie męczył usługi. */
function switchTo(target) {
  channel = target;
  mode = 'live';
  program = null;
  positionMs = 0;
  setStatus('loading');
  showOsd();
  clearTimeout(zapTimer);
  zapTimer = setTimeout(start, ZAP_DEBOUNCE_MS);
}

// ---------- nakładka ----------

function render() {
  ui.logo.textContent = channel.name;
  ui.num.textContent = channel.number;
  ui.name.textContent = channel.name;
  renderProgram();
  renderStatus();
  if (status !== 'playing') ui.osd.classList.remove('hidden');   // błędu i ładowania nie chowamy
}

function renderProgram() {
  if (mode === 'archive') {
    const length = program.end - program.start;
    ui.title.textContent = program.title;
    ui.time.textContent = `Nagranie z ${formatTime(program.start)}, trwa ${Math.round(length / 60000)} min`;
    ui.bar.style.width = `${Math.min(100, (positionMs / length) * 100)}%`;
    ui.next.textContent = '';
    ui.flag.classList.add('archive');
    ui.flagText.textContent = 'Archiwum';
    return;
  }

  const { now, next } = store.nowNext(channel);
  ui.title.textContent = now ? now.title : 'Brak danych o programie';
  ui.time.textContent = now ? `${formatTime(now.start)} do ${formatTime(now.end)}, jeszcze ${minutesLeft(now)} min` : '';
  ui.bar.style.width = now ? `${progressPercent(now)}%` : '0%';
  ui.next.textContent = next ? `Dalej: ${formatTime(next.start)} ${next.title}` : '';
  ui.flag.classList.remove('archive');
  ui.flagText.textContent = 'Na żywo';
}

function renderStatus() {
  ui.status.classList.toggle('error', status === 'error');
  ui.spinner.hidden = status !== 'loading';
  if (status === 'loading') ui.statusText.textContent = 'Łączenie z kanałem...';
  else if (status === 'error') ui.statusText.textContent = `${message} Naciśnij OK, aby spróbować ponownie.`;
  else ui.statusText.textContent = '';
}

function showOsd() {
  ui.osd.classList.remove('hidden');
  ui.flag.classList.remove('hidden');
  clearTimeout(osdTimer);
  osdTimer = setTimeout(() => {
    if (status !== 'playing') return;
    ui.osd.classList.add('hidden');
    ui.flag.classList.add('hidden');
  }, OSD_VISIBLE_MS);
}
