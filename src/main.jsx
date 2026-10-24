import { render } from 'preact';
import './styles.css';
import { App } from './app.jsx';
import { SHARE_KEY } from './screens/Radar.jsx';

// Android: "Compartilhar → GlowStudio" abre o app com o link do post na URL. Leva direto ao Radar.
const q = new URLSearchParams(location.search);
const shared = [q.get('url'), q.get('text'), q.get('title')].filter(Boolean).join(' ');
if (shared) {
  try {
    sessionStorage.setItem(SHARE_KEY, shared);
  } catch {
    /* sem armazenamento: abre o Radar vazio */
  }
  history.replaceState(null, '', location.pathname + '#/radar/novo');
}

render(<App />, document.getElementById('app'));

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
