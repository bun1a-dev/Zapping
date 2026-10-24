/**
 * Wybór wersji regionalnej kanału (np. TVP 3). Lista pochodzi z usługi, więc zawsze zgadza się z ofertą.
 * Z parametrem thenPlay po wyborze od razu rusza odtwarzanie, bez niego wracamy na poprzedni ekran.
 */
import { $, h, clear } from '../dom.js';
import { call } from '../native.js';
import * as router from '../router.js';
import * as store from '../store.js';
import * as prefs from '../prefs.js';
import { toast } from '../toast.js';

const listEl = $('#r-list');

let channel = null;
let thenPlay = false;

export async function enter({ channelId, thenPlay: play = false }) {
  channel = store.byId(channelId);
  thenPlay = play;
  $('#r-title').textContent = channel.name;
  clear(listEl).append(h('div', { class: 'empty' }, h('i', { class: 'spinner' }), 'Ładowanie listy regionów...'));

  try {
    render(await call('regions', { channelId }));
  } catch (error) {
    clear(listEl).append(h('div', { class: 'empty' }, error.message));
  }
}

/** Lista wyboru zawsze startuje na aktualnie wybranym regionie, a nie na ostatnio oglądanym. */
export const rememberFocus = false;
export const defaultFocus = () => $('.region.on') ?? $('.region');

function render(regions) {
  clear(listEl);
  if (!regions.length) {
    listEl.append(h('div', { class: 'empty' }, 'Usługa nie zwróciła listy regionów.'));
    return;
  }
  const chosen = prefs.getRegion(channel.id);
  regions.forEach(({ name }) => listEl.append(h('button', {
    class: `f region${name === chosen ? ' on' : ''}`,
    dataset: { key: `region:${name}` },
    onclick: () => choose(name),
  }, name)));
}

function choose(name) {
  prefs.setRegion(channel.id, name);
  if (thenPlay) {
    // push: false zastępuje ten ekran w historii, więc Wstecz z odtwarzacza wraca na listę kanałów
    router.go('player', { params: { channelId: channel.id, mode: 'live' }, push: false });
  } else {
    router.back();
    toast(`Region: ${name}`);
  }
}
