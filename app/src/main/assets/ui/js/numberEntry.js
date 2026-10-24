import { $ } from './dom.js';

/**
 * Wpisywanie numeru kanału cyframi z pilota. Numer zatwierdza się po chwili ciszy,
 * po osiągnięciu maksymalnej liczby cyfr albo ręcznie przez commit().
 */
export function createNumberEntry({ onCommit, maxDigits = 3, delayMs = 1500 }) {
  const box = $('#entry');
  let text = '';
  let timer = null;

  const render = () => {
    box.textContent = text;
    box.classList.toggle('show', text !== '');
  };

  function reset() {
    clearTimeout(timer);
    text = '';
    render();
  }

  function commit() {
    clearTimeout(timer);
    const value = text;
    reset();
    if (value) onCommit(Number(value));
  }

  function type(digit) {
    text = (text + digit).slice(0, maxDigits);
    render();
    clearTimeout(timer);
    if (text.length >= maxDigits) commit();
    else timer = setTimeout(commit, delayMs);
  }

  return { type, commit, reset, get pending() { return text !== ''; } };
}

export const isDigit = key => key.length === 1 && key >= '0' && key <= '9';
