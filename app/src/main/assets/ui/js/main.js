/** Punkt wejścia: składa ekrany, podpina klawisze i decyduje, od którego ekranu zacząć. */
import { $ } from './dom.js';
import { call, on } from './native.js';
import * as router from './router.js';
import { handleKey } from './keys.js';
import { formatTime } from './format.js';
import * as login from './screens/login.js';
import * as home from './screens/home.js';
import * as archive from './screens/archive.js';
import * as player from './screens/player.js';

async function boot() {
  // Poza Androidem (zwykła przeglądarka) podstawiamy dane przykładowe.
  if (!window.NativeBridge) await import('./mock.js');

  fitStage();
  startClock();

  router.register('login', login);
  router.register('home', home);
  router.register('archive', archive);
  router.register('player', player);

  window.addEventListener('keydown', event => handleKey(event.key, event));
  on('key', ({ key }) => handleKey(key));                 // CH+ i CH- przekazywane z Androida
  window.zapping.onBack = () => router.back();             // przycisk Wstecz z Androida

  const { hasCredentials } = await call('session.state');
  await router.go(hasCredentials ? 'home' : 'login', { push: false });
}

/** Scena ma stały rozmiar 1920x1080 i skaluje się do dowolnego ekranu. */
function fitStage() {
  const stage = $('#stage');
  const apply = () => {
    const scale = Math.min(innerWidth / 1920, innerHeight / 1080);
    const x = (innerWidth - 1920 * scale) / 2;
    const y = (innerHeight - 1080 * scale) / 2;
    stage.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
  };
  addEventListener('resize', apply);
  console.log('viewport', innerWidth, innerHeight, devicePixelRatio);
  apply();
}

function startClock() {
  const tick = () => { $('#clock').textContent = formatTime(Date.now()); };
  tick();
  setInterval(tick, 20000);
}

boot();
