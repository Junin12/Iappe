import { useEffect } from 'preact/hooks';
import { House, Gem, Plus, CalendarDays, LayoutGrid } from 'lucide-preact';
import { route } from './router.js';
import { loadAll, ready, settings } from './store.js';
import { requestPersistence } from './db.js';
import { ToastHost } from './ui.jsx';
import { Home } from './screens/Home.jsx';
import { Pecas, PecaDetail } from './screens/Pecas.jsx';
import { Criar } from './screens/Criar.jsx';
import { Camera } from './screens/Camera.jsx';
import { Agenda } from './screens/Agenda.jsx';
import { Resultados } from './screens/Resultados.jsx';
import { Onboarding } from './screens/Onboarding.jsx';
import { Ferramentas, Datas, Ganchos } from './screens/Ferramentas.jsx';
import { Radar } from './screens/Radar.jsx';
import { Vitrine } from './screens/Vitrine.jsx';

const TABS = [
  { path: '', label: 'Início', icon: House },
  { path: 'pecas', label: 'Peças', icon: Gem },
  { path: 'criar', label: 'Criar', icon: Plus, fab: true },
  { path: 'agenda', label: 'Agenda', icon: CalendarDays },
  { path: 'ferramentas', label: 'Ferramentas', icon: LayoutGrid },
];

// Telas que ficam dentro da aba Ferramentas.
const TOOLS = ['resultados', 'radar', 'vitrine', 'datas', 'ganchos'];

function Nav({ current }) {
  return (
    <nav class="nav" aria-label="Navegação principal">
      <div class="nav-inner">
        {TABS.map((t) =>
          t.fab ? (
            <a href="#/criar" aria-label="Criar vídeo">
              <span class="fab">
                <t.icon size={28} strokeWidth={2.4} />
              </span>
            </a>
          ) : (
            <a href={'#/' + t.path} class={current === t.path ? 'on' : ''} aria-current={current === t.path ? 'page' : undefined}>
              <t.icon size={23} />
              {t.label}
            </a>
          ),
        )}
      </div>
    </nav>
  );
}

export function App() {
  useEffect(() => {
    loadAll();
    requestPersistence();
  }, []);

  if (!ready.value) return <div class="app" />;
  if (!settings.value.onboarded) return <Onboarding />;

  const [head, a, b] = route.value;
  let screen;
  let showNav = true;
  switch (head) {
    case undefined:
      screen = <Home />;
      break;
    case 'pecas':
      screen = <Pecas openNew={a === 'nova'} />;
      break;
    case 'peca':
      screen = <PecaDetail id={a} />;
      break;
    case 'criar':
      screen = <Criar videoId={a} step={b} />;
      showNav = !a;
      break;
    case 'camera':
      screen = <Camera videoId={a} cena={Number(b) || 0} />;
      showNav = false;
      break;
    case 'agenda':
      screen = <Agenda />;
      break;
    case 'resultados':
      screen = <Resultados />;
      break;
    case 'ferramentas':
      screen = <Ferramentas />;
      break;
    case 'radar':
      screen = <Radar novo={a === 'novo'} key={a} />;
      break;
    case 'vitrine':
      screen = <Vitrine />;
      break;
    case 'datas':
      screen = <Datas />;
      break;
    case 'ganchos':
      screen = <Ganchos />;
      break;
    default:
      screen = <Home />;
  }

  return (
    <div class="app">
      {screen}
      {showNav && <Nav current={TOOLS.includes(head) ? 'ferramentas' : head || ''} />}
      <ToastHost />
    </div>
  );
}
