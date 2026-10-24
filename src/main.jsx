import { render } from 'preact';
import './styles.css';
import { App } from './app.jsx';

render(<App />, document.getElementById('app'));

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
