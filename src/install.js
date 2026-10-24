import { signal } from '@preact/signals';

// Android/Chrome oferece instalação por evento; iPhone precisa do menu Compartilhar.
export const installPrompt = signal(null);
export const isStandalone = () =>
  window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
export const isIOS = () => /iphone|ipad|ipod/i.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  installPrompt.value = e;
});

export async function promptInstall() {
  const e = installPrompt.value;
  if (!e) return false;
  e.prompt();
  const { outcome } = await e.userChoice;
  installPrompt.value = null;
  return outcome === 'accepted';
}
