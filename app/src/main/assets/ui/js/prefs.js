/** Ustawienia użytkownika trzymane w localStorage WebView. Na razie tylko wybrany region kanału. */

const REGION_KEY = 'zapping.region.';

export function getRegion(channelId) {
  try {
    return localStorage.getItem(REGION_KEY + channelId);
  } catch {
    return null;
  }
}

export function setRegion(channelId, name) {
  try {
    localStorage.setItem(REGION_KEY + channelId, name);
  } catch {
    // bez pamięci wybór działa tylko do zamknięcia aplikacji
  }
}
