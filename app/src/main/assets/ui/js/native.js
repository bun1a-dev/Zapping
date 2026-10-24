/**
 * Most do Kotlina. Wywołanie metody zwraca obietnicę, zdarzenia z Kotlina odbiera się przez on().
 * Po stronie Androida odpowiada mu UiBridge, a w podglądzie w przeglądarce mock.js.
 */

const pending = new Map();
const listeners = new Map();
let counter = 0;

export function call(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = String(++counter);
    pending.set(id, { resolve, reject });
    window.NativeBridge.post(JSON.stringify({ id, method, params }));
  });
}

/** Zwraca funkcję wypisującą z nasłuchu. */
export function on(event, handler) {
  if (!listeners.has(event)) listeners.set(event, new Set());
  listeners.get(event).add(handler);
  return () => listeners.get(event).delete(handler);
}

function receive(raw) {
  const message = JSON.parse(raw);

  if (message.event) {
    (listeners.get(message.event) ?? []).forEach(handler => handler(message.payload));
    return;
  }

  const waiting = pending.get(message.id);
  if (!waiting) return;
  pending.delete(message.id);

  if (message.ok) {
    waiting.resolve(message.result);
  } else {
    const error = new Error(message.error.message);
    error.code = message.error.code;
    waiting.reject(error);
  }
}

// Kotlin wywołuje window.zapping.receive(...) oraz window.zapping.onBack()
window.zapping = Object.assign(window.zapping ?? {}, { receive });
