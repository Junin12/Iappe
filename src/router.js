import { signal } from '@preact/signals';

// Rotas por hash: funcionam em qualquer subpasta (wiapps.com.br/glowstudio/) e com o botão voltar.
const parse = () => (location.hash.replace(/^#/, '') || '/').split('/').filter(Boolean);

export const route = signal(parse());

window.addEventListener('hashchange', () => {
  route.value = parse();
  window.scrollTo(0, 0);
});

export function go(path, { replace = false } = {}) {
  const hash = '#' + path;
  if (replace) {
    history.replaceState(null, '', hash);
    route.value = parse();
  } else {
    location.hash = path;
  }
}

export function back(fallback = '/') {
  if (history.length > 1) history.back();
  else go(fallback, { replace: true });
}
