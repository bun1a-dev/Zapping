/**
 * Logowanie klawiaturą numeryczną na ekranie. Login i PIN w tej usłudze są liczbowe,
 * a systemowa klawiatura ekranowa na TV jest uciążliwa. Cyfry z pilota też działają.
 */
import { $, h, clear } from '../dom.js';
import { call } from '../native.js';
import * as router from '../router.js';
import { focusEl } from '../focus.js';
import { isDigit } from '../numberEntry.js';
import { toast } from '../toast.js';

const LIMITS = { login: 15, pin: 8 };
const KEYPAD = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'delete', '0', 'next'];
const KEY_LABELS = { delete: 'Usuń', next: 'Dalej' };

const state = { login: '', pin: '', active: 'login', busy: false };

const fieldEls = { login: $('#field-login'), pin: $('#field-pin') };
const errorEl = $('#login-error');
let nextKey = null;

buildKeypad();
fieldEls.login.addEventListener('click', () => setActive('login'));
fieldEls.pin.addEventListener('click', () => setActive('pin'));

export function enter() {
  Object.assign(state, { login: '', pin: '', active: 'login', busy: false });
  showError('');
  render();
}

export const defaultFocus = () => $('[data-key="key:5"]');

export function onKey(key) {
  if (!isDigit(key)) return false;
  type(key);
  return true;
}

function buildKeypad() {
  const box = clear($('#keypad'));
  KEYPAD.forEach(value => {
    const isAction = value === 'delete' || value === 'next';
    const button = h('button', {
      class: `f key${isAction ? ' action' : ''}`,
      dataset: { key: `key:${value}` },
      onclick: () => press(value),
    }, KEY_LABELS[value] ?? value);
    if (value === 'next') nextKey = button;
    box.append(button);
  });
}

function press(value) {
  if (value === 'delete') erase();
  else if (value === 'next') advance();
  else type(value);
}

function type(digit) {
  const field = state.active;
  state[field] = (state[field] + digit).slice(0, LIMITS[field]);
  showError('');
  render();
}

function erase() {
  state[state.active] = state[state.active].slice(0, -1);
  render();
}

function setActive(field) {
  state.active = field;
  render();
}

/** "Dalej" przechodzi z loginu do PIN-u, a przy PIN-ie zatwierdza. */
function advance() {
  if (state.active === 'login') {
    if (!state.login) return showError('Wpisz login.');
    setActive('pin');
  } else {
    submit();
  }
}

async function submit() {
  if (state.busy) return;
  if (!state.login || !state.pin) return showError('Wpisz login i PIN.');

  state.busy = true;
  showError('');
  try {
    await call('session.login', { login: state.login, pin: state.pin });
    state.pin = '';
    render();
    await router.go('home', { reset: true });
    toast('Zalogowano');
  } catch (error) {
    state.pin = '';
    showError(error.message);
    setActive('pin');
    focusEl(fieldEls.pin);
  } finally {
    state.busy = false;
  }
}

function render() {
  $('b', fieldEls.login).textContent = state.login;
  $('b', fieldEls.pin).textContent = '•'.repeat(state.pin.length);
  fieldEls.login.classList.toggle('active', state.active === 'login');
  fieldEls.pin.classList.toggle('active', state.active === 'pin');
  nextKey.textContent = state.active === 'login' ? 'Dalej' : 'Zaloguj';
}

function showError(text) {
  errorEl.textContent = text;
}
