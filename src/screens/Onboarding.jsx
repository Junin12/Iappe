import { useState } from 'preact/hooks';
import { Sparkles, Clapperboard, Target, Smartphone, Share2 } from 'lucide-preact';
import { saveSettings } from '../store.js';
import { isIOS, isStandalone, installPrompt, promptInstall } from '../install.js';

const SLIDES = [
  {
    icon: Sparkles,
    title: 'Seu estúdio de semijoias',
    text: 'Vídeos para o TikTok que mostram só as peças e as mãos. Nada de rosto.',
  },
  {
    icon: Clapperboard,
    title: 'Grave cena por cena',
    text: 'Escolha um formato pronto e o app guia cada cena. No fim, ele junta tudo com texto, preço e brilho.',
  },
  {
    icon: Target,
    title: 'Poste com constância',
    text: 'Defina uma meta semanal, agende os posts e veja quais vídeos vendem mais.',
  },
];

export function Onboarding() {
  const [i, setI] = useState(0);
  const last = i === SLIDES.length;

  const finish = () => saveSettings({ onboarded: true });

  if (last) {
    return (
      <div class="app">
        <div class="screen full slides">
          <div class="slide-ico">
            <Smartphone size={44} />
          </div>
          <h1>Deixe na tela inicial</h1>
          {isStandalone() ? (
            <p class="muted">Tudo certo: o GlowStudio já está instalado.</p>
          ) : isIOS() ? (
            <div class="card stack" style="text-align:left;margin-top:18px">
              <p style="margin:0">No iPhone, pelo Safari:</p>
              <p class="row" style="margin:0">
                <b>1.</b> Toque em <Share2 size={18} class="gold" /> <b>Compartilhar</b>
              </p>
              <p style="margin:0">
                <b>2.</b> Escolha <b>Adicionar à Tela de Início</b>
              </p>
              <p class="muted small" style="margin:0">Assim ele abre em tela cheia, como um app, e guarda seus vídeos com mais segurança.</p>
            </div>
          ) : installPrompt.value ? (
            <button class="btn gold lg block" style="margin-top:22px" onClick={promptInstall}>
              Instalar o app
            </button>
          ) : (
            <p class="muted">No Chrome, abra o menu ⋮ e toque em <b>Instalar app</b> ou <b>Adicionar à tela inicial</b>.</p>
          )}
          <button class="btn block lg" style="margin-top:16px" onClick={finish}>
            Começar
          </button>
        </div>
      </div>
    );
  }

  const s = SLIDES[i];
  return (
    <div class="app">
      <div class="screen full slides" key={i}>
        <div class="slide-ico">
          <s.icon size={44} />
        </div>
        <p class="eyebrow">GlowStudio</p>
        <h1 style="margin:8px 0 12px">{s.title}</h1>
        <p class="muted" style="font-size:1.05rem;line-height:1.5">{s.text}</p>
        <div class="dots-nav" aria-hidden="true">
          {[...SLIDES, 0].map((_, k) => (
            <i class={k === i ? 'on' : ''} />
          ))}
        </div>
        <button class="btn gold lg block" onClick={() => setI(i + 1)}>
          Continuar
        </button>
        <button class="btn ghost block" style="margin-top:8px" onClick={finish}>
          Pular
        </button>
      </div>
    </div>
  );
}
