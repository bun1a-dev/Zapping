/** Stan aplikacji: lista kanałów i program. Jedyne miejsce, które rozmawia z Kotlinem o danych. */
import { call } from './native.js';
import { startOfDay } from './format.js';

let channels = [];
const index = new Map();
const guide = new Map();        // id kanału -> programy posortowane po starcie
const loadedDays = new Set();   // doby, dla których program już pobrano

export const allChannels = () => channels;
export const byId = id => index.get(id);
export const byNumber = number => channels.find(channel => channel.number === number);

export async function loadChannels() {
  const list = await call('channels');
  channels = [...list].sort((a, b) => a.number - b.number);
  index.clear();
  channels.forEach(channel => index.set(channel.id, channel));
}

/** Pobiera program doby sprzed [daysAgo] dni. Odpowiedź obejmuje też dobę wcześniejszą. */
export async function loadGuide(daysAgo = 0) {
  const data = await call('epg', { day: daysAgo });
  for (const [id, programs] of Object.entries(data)) merge(id, programs);
  loadedDays.add(daysAgo);
  loadedDays.add(daysAgo + 1);
}

export async function ensureGuide(daysAgo) {
  if (!loadedDays.has(daysAgo)) await loadGuide(daysAgo);
}

export function nowNext(channel, now = Date.now()) {
  const list = guide.get(channel.id) ?? [];
  const i = list.findIndex(program => program.start <= now && now < program.end);
  if (i < 0) return { now: null, next: null };
  return { now: list[i], next: list[i + 1] ?? null };
}

/** Programy, które zaczęły się danego dnia (czas lokalny). */
export function programsOfDay(channelId, daysAgo) {
  const from = startOfDay(daysAgo);
  const to = from + 24 * 3600 * 1000;
  return (guide.get(channelId) ?? []).filter(program => program.start >= from && program.start < to);
}

/** Kategorie w kolejności pierwszego wystąpienia wśród kanałów spełniających warunek. */
export function categories(predicate = () => true) {
  return [...new Set(channels.filter(predicate).map(channel => channel.category))];
}

export function reset() {
  channels = [];
  index.clear();
  guide.clear();
  loadedDays.clear();
}

function merge(channelId, programs) {
  const known = new Map((guide.get(channelId) ?? []).map(program => [program.start, program]));
  programs.forEach(program => known.set(program.start, program));
  guide.set(channelId, [...known.values()].sort((a, b) => a.start - b.start));
}
