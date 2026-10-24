/**
 * Ekrany i historia. Ekran to moduł z opcjonalnymi funkcjami:
 *   enter(params), leave(), resume(), onBack(), onKey(key), defaultFocus(), rememberFocus
 * Ekran z rememberFocus = false (np. lista wyboru) zawsze startuje od defaultFocus.
 * Router pamięta, który element miał fokus na każdym ekranie, i przywraca go przy powrocie.
 */
import { $ } from './dom.js';
import { focusEl, firstFocusable } from './focus.js';

const screens = new Map();
const stack = [];
const lastFocus = new Map();
let currentName = null;

export function register(name, module) {
  screens.set(name, { ...module, name, el: document.getElementById(`screen-${name}`) });
}

export const current = () => (currentName ? screens.get(currentName) : null);

export async function go(name, { params = {}, push = true, reset = false } = {}) {
  const from = current();
  if (from) {
    lastFocus.set(from.name, document.activeElement?.dataset?.key ?? null);
    from.leave?.();
    if (push && !reset) stack.push(from.name);
  }
  if (reset) stack.length = 0;

  show(name);
  await screens.get(name).enter?.(params);
  restoreFocus(name);
}

/** Zwraca true, gdy coś obsłużono. False oznacza, że nie ma dokąd wracać (Android zamyka wtedy aplikację). */
export function back() {
  const from = current();
  if (from?.onBack?.()) return true;
  if (!stack.length) return false;

  const previous = stack.pop();
  from.leave?.();
  show(previous);
  screens.get(previous).resume?.();
  restoreFocus(previous);
  return true;
}

function show(name) {
  screens.forEach(screen => screen.el.classList.toggle('active', screen.name === name));
  $('#stage').dataset.screen = name;
  currentName = name;
}

function restoreFocus(name) {
  const screen = screens.get(name);
  const key = screen.rememberFocus === false ? null : lastFocus.get(name);
  const remembered = key ? screen.el.querySelector(`[data-key="${CSS.escape(key)}"]`) : null;
  const target = remembered ?? screen.defaultFocus?.() ?? firstFocusable(screen.el);
  if (target) focusEl(target);
}
