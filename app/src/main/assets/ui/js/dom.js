/** Najmniejszy możliwy zestaw narzędzi do budowania DOM. Bez innerHTML, więc bez ryzyka wstrzyknięć. */

export const $ = (selector, root = document) => root.querySelector(selector);

/** h('button', { class: 'f tile', dataset: { key: 'x' }, onclick: fn }, 'tekst', dziecko) */
export function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [name, value] of Object.entries(props)) {
    if (value == null || value === false) continue;
    if (name === 'class') node.className = value;
    else if (name === 'dataset') Object.assign(node.dataset, value);
    else if (name.startsWith('on')) node.addEventListener(name.slice(2), value);
    else node.setAttribute(name, value === true ? '' : value);
  }
  node.append(...children.flat().filter(child => child != null && child !== false));
  return node;
}

export function clear(node) {
  node.replaceChildren();
  return node;
}
