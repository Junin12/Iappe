// Linha do tempo do vídeo final: cada cena gravada vira um trecho (corte + velocidade).
export const HOOK_MS = 3000;

// Corte padrão: pula o tremido do toque no botão e fica perto da duração da cena.
export function defaultTrim(durMs, cena) {
  const target = Math.max(800, cena.seg * (cena.speed || 1) * 1000);
  if (durMs <= target) return { inMs: 0, outMs: durMs };
  const inMs = Math.min(400, durMs - target);
  return { inMs, outMs: Math.min(durMs, inMs + target) };
}

export function buildTimeline(video) {
  const items = [];
  let t = 0;
  video.cenas.forEach((cena, sceneIdx) => {
    const clip = video.clips?.[sceneIdx];
    if (!clip) return;
    const inMs = Math.max(0, clip.inMs ?? 0);
    const outMs = Math.min(clip.durMs || Infinity, clip.outMs ?? clip.durMs);
    if (!(outMs > inMs)) return;
    const speed = clip.speed ?? cena.speed ?? 1;
    const outDur = (outMs - inMs) / speed;
    items.push({ sceneIdx, clip, inMs, outMs, speed, slow: speed < 1, outStart: t, outDur, fim: !!cena.fim });
    t += outDur;
  });
  return { items, total: t };
}

// Qual sobreposição aparece no instante t (ms do vídeo final).
export function overlayAt(t, timeline, edit) {
  const { items, total } = timeline;
  if (!items.length) return null;
  const last = items[items.length - 1];
  const endStart = last.fim ? last.outStart : Math.max(0, total - 3000);
  const wantsEnd = edit.mostrarPreco || (edit.cta || '').trim();
  if (wantsEnd && t >= endStart) return 'end';
  if (t < Math.min(HOOK_MS, total)) return 'hook';
  return null;
}

export function itemAt(t, timeline) {
  const { items } = timeline;
  for (let i = 0; i < items.length; i++) {
    if (t < items[i].outStart + items[i].outDur) return i;
  }
  return items.length - 1;
}
