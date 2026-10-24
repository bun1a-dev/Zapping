/** Nawigacja pilotem: element z fokusem i ruch do najbliższego sąsiada w zadanym kierunku. */

const revealers = [];

/** Ekran może zarejestrować funkcję, która przewija listę, żeby zaznaczony element był widoczny. */
export function onReveal(handler) {
  revealers.push(handler);
}

export const focusables = scope => [...scope.querySelectorAll('.f')].filter(el => el.offsetParent !== null);

export const firstFocusable = scope => focusables(scope)[0] ?? null;

export function focusEl(el) {
  el.focus({ preventScroll: true });
  revealers.forEach(handler => handler(el));
}

const center = el => {
  const r = el.getBoundingClientRect();
  return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
};

/**
 * Ruchy poziome najpierw szukają w tym samym rzędzie. Dopiero gdy go nie ma, szukają gdziekolwiek,
 * chyba że element jest w kontenerze z atrybutem data-nav-row (rzędy kanałów): tam koniec rzędu to koniec.
 */
export function move(scope, direction) {
  const items = focusables(scope);
  const current = document.activeElement;
  if (!items.includes(current)) {
    const first = items[0];
    if (first) focusEl(first);
    return;
  }

  const from = center(current);
  const horizontal = direction === 'left' || direction === 'right';

  const pick = strict => {
    let best = null;
    let bestScore = Infinity;
    for (const el of items) {
      if (el === current) continue;
      const to = center(el);
      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const primary = { left: -dx, right: dx, up: -dy, down: dy }[direction];
      const secondary = horizontal ? Math.abs(dy) : Math.abs(dx);
      if (primary <= 8) continue;
      if (strict && horizontal && secondary > 60) continue;
      const score = primary + secondary * 2;
      if (score < bestScore) {
        best = el;
        bestScore = score;
      }
    }
    return best;
  };

  const rowOnly = horizontal && current.closest('[data-nav-row]') !== null;
  const target = pick(true) ?? (rowOnly ? null : pick(false));
  if (target) focusEl(target);
}
