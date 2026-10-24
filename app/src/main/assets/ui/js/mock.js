/**
 * Atrapa Kotlina do podglądu interfejsu w zwykłej przeglądarce (bez Androida).
 * Udaje te same metody co bridge/Routes.kt, na danych przykładowych.
 * Uruchom serwer w tym katalogu, np. "python -m http.server", i otwórz index.html.
 */

document.documentElement.classList.add('mock');

const CHANNELS = [
  [1, 'TVP 1 HD', 'Ogólne', 0], [2, 'TVP 2 HD', 'Ogólne', 0], [3, 'TVN HD', 'Ogólne', 2],
  [5, 'Polsat HD', 'Ogólne', 7], [6, 'TV4 HD', 'Ogólne', 7],
  [22, 'TVN24 HD', 'Informacje', 7], [29, 'Polsat News', 'Informacje', 7],
  [111, 'Eurosport 1 HD', 'Sport', 0], [114, 'Polsat Sport 1', 'Sport', 7], [120, 'Eleven Sports 1 HD', 'Sport', 7],
  [222, 'TVN Fabuła HD', 'Filmy i seriale', 0], [226, 'Kino Polska HD', 'Filmy i seriale', 7], [243, 'Polsat Film HD', 'Filmy i seriale', 7],
  [338, 'TLC HD', 'Lifestyle i rozrywka', 7],
  [400, 'Discovery Channel HD', 'Nauka i świat', 7], [444, 'National Geographic HD', 'Nauka i świat', 7],
  [556, 'MiniMini+ HD', 'Dzieci', 7], [567, 'Disney Channel HD', 'Dzieci', 7],
].map(([number, name, category, catchupDays]) => ({ id: String(number), number, name, category, logoUrl: '', catchupDays }));

const TITLES = {
  'Ogólne': ['Poranek z kawą', 'Wiadomości', 'Serial dnia', 'Teleturniej', 'Magazyn reporterów', 'Film wieczoru', 'Kabaret'],
  'Informacje': ['Serwis informacyjny', 'Rozmowa dnia', 'Raport z kraju', 'Świat w godzinę', 'Biznes i giełda', 'Debata'],
  'Sport': ['Mecz na żywo', 'Studio przed meczem', 'Skróty kolejki', 'Magazyn żużlowy', 'Liga w pigułce'],
  'Filmy i seriale': ['Kino popołudnia', 'Film fabularny', 'Serial kryminalny', 'Komedia romantyczna', 'Klasyka kina'],
  'Lifestyle i rozrywka': ['Remont w tydzień', 'Gotowanie z pasją', 'Podróże smakowe', 'Dom i ogród'],
  'Nauka i świat': ['Tajemnice oceanów', 'Wielkie konstrukcje', 'Dzika przyroda', 'Historia w kolorze'],
  'Dzieci': ['Bajka na dobranoc', 'Przygody w lesie', 'Kreatywne zabawy', 'Szkoła superbohaterów'],
};
const SLOT_MINUTES = [30, 45, 60, 25, 90, 55, 40, 50];
const DAY = 24 * 3600 * 1000;

/** Program jednej doby sprzed [daysAgo] dni, deterministyczny. */
function dayPrograms(channel, daysAgo) {
  const base = new Date();
  base.setHours(0, 0, 0, 0);
  const dayStart = base.getTime() - daysAgo * DAY;
  const titles = TITLES[channel.category];
  const seed = channel.number;
  const out = [{ id: `${channel.id}-${daysAgo}-n`, title: 'Nocne powtórki', start: dayStart, end: dayStart + 6 * 3600e3 }];
  let t = dayStart + 6 * 3600e3;
  for (let n = 0; t < dayStart + DAY; n++) {
    const end = Math.min(t + SLOT_MINUTES[(seed + daysAgo * 3 + n) % SLOT_MINUTES.length] * 60e3, dayStart + DAY);
    out.push({ id: `${channel.id}-${daysAgo}-${n}`, title: titles[(seed * 7 + daysAgo * 5 + n) % titles.length], start: t, end });
    t = end;
  }
  return out;
}

let progressTimer = null;

const fail = (code, message) => Object.assign(new Error(message), { code });

const handlers = {
  'session.state': () => ({ hasCredentials: false }),
  'session.login': ({ login, pin }) => {
    if (!login || !pin) throw fail('login_rejected', 'Nieprawidłowy login lub PIN.');
  },
  'session.logout': () => null,
  'cache.clear': () => null,
  channels: () => CHANNELS,
  epg: ({ day }) => Object.fromEntries(CHANNELS.map(channel =>
    [channel.id, [...dayPrograms(channel, day + 1), ...dayPrograms(channel, day)]])),
  play: () => {
    clearInterval(progressTimer);
    setTimeout(() => emit('player.state', { state: 'playing' }), 500);
    let positionMs = 0;
    progressTimer = setInterval(() => emit('player.progress', { positionMs: positionMs += 1000 }), 1000);
  },
  stop: () => clearInterval(progressTimer),
};

const send = message => window.zapping.receive(JSON.stringify(message));
const emit = (event, payload) => send({ event, payload });

window.NativeBridge = {
  post(raw) {
    const { id, method, params } = JSON.parse(raw);
    setTimeout(() => {
      try {
        send({ id, ok: true, result: handlers[method](params) ?? null });
      } catch (error) {
        send({ id, ok: false, error: { code: error.code ?? 'unexpected', message: error.message } });
      }
    }, 120);
  },
};
