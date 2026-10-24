/**
 * Jedyne miejsce, które tłumaczy klawisze pilota na akcje.
 * Najpierw pyta ekran (onKey), a gdy ten nie obsłuży, działają reguły wspólne.
 */
import * as router from './router.js';
import { move } from './focus.js';

const ARROWS = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right' };

export function handleKey(key, event = null) {
  const screen = router.current();
  if (!screen) return;

  if (screen.onKey?.(key) === true) {
    event?.preventDefault();
    return;
  }

  if (key in ARROWS) {
    event?.preventDefault();
    move(screen.el, ARROWS[key]);
  } else if (key === 'Enter') {
    event?.preventDefault();
    const focused = document.activeElement;
    if (focused?.classList.contains('f')) focused.click();
  } else if (key === 'Escape' || key === 'Backspace') {
    event?.preventDefault();
    router.back();
  }
}
