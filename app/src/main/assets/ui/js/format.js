/** Czas i etykiety. Wszystkie czasy w aplikacji to milisekundy od epoki. */

const pad = n => String(n).padStart(2, '0');
const DAY_NAMES = ['niedziela', 'poniedziałek', 'wtorek', 'środa', 'czwartek', 'piątek', 'sobota'];

export const formatTime = ms => {
  const d = new Date(ms);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export const formatDate = ms => {
  const d = new Date(ms);
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}`;
};

export const progressPercent = (program, now = Date.now()) =>
  Math.max(0, Math.min(100, ((now - program.start) / (program.end - program.start)) * 100));

export const minutesLeft = (program, now = Date.now()) =>
  Math.max(0, Math.round((program.end - now) / 60000));

/** Początek doby sprzed [daysAgo] dni (0 = dziś). */
export function startOfDay(daysAgo = 0) {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - daysAgo);
  return d.getTime();
}

export function dayLabel(daysAgo) {
  if (daysAgo === 0) return 'Dzisiaj';
  if (daysAgo === 1) return 'Wczoraj';
  const name = DAY_NAMES[new Date(startOfDay(daysAgo)).getDay()];
  return name[0].toUpperCase() + name.slice(1);
}

/**
 * Skrót nazwy kanału, gdy nie ma logo. "TVN24 HD" daje "TVN24", "Polsat Sport 1" daje "PS1",
 * a jednowyrazowe nazwy ("Polsat") dają pierwsze trzy litery.
 */
export function initials(name) {
  const words = name.replace(/\s+HD$/i, '').split(/\s+/).filter(Boolean);
  const first = words[0] ?? '';
  if (/^[A-Z0-9+]{2,6}$/.test(first)) return first;
  if (words.length === 1) return first.slice(0, 3).toUpperCase();
  return words.map(word => word[0]).join('').slice(0, 3).toUpperCase();
}
