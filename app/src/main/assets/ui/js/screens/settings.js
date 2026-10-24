/** Zawartość zakładki Ustawienia. Renderowana w ekranie głównym, nie jest osobnym ekranem. */
import { $, h, clear } from '../dom.js';
import { call } from '../native.js';
import * as router from '../router.js';
import * as store from '../store.js';
import { toast } from '../toast.js';

export function render() {
  const box = clear($('#settings-view'));
  box.append(
    h('h2', {}, 'Konto'),
    h('p', { class: 'lead' }, 'Jesteś zalogowany. Dane logowania są zaszyfrowane i zostają tylko na tym urządzeniu.'),
    h('div', { class: 'actions' },
      button('Wyczyść pamięć podręczną', 'settings:cache', clearCache),
      button('Wyloguj', 'settings:logout', logout),
    ),
    h('p', { class: 'about' },
      'Zapping jest niezależnym, nieoficjalnym klientem. Nie jest powiązany z operatorem usługi ani przez niego wspierany. Wymaga własnej, aktywnej subskrypcji.'),
  );
}

const button = (label, key, onclick) =>
  h('button', { class: 'f btn quiet', dataset: { key }, onclick }, label);

async function clearCache() {
  await call('cache.clear');
  toast('Pamięć podręczna wyczyszczona');
}

async function logout() {
  await call('session.logout');
  store.reset();
  await router.go('login', { reset: true });
  toast('Wylogowano');
}
