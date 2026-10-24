// Narração por cena (voz da IA) e legendas que acompanham a fala.
// Cada cena com fala gerada vira um trecho de áudio que começa junto com a cena;
// se a fala anterior ainda não terminou, espera ela acabar (as vozes nunca se sobrepõem).
const GAP_MS = 120;

export function agendaNarracao(timeline, video) {
  const cenas = video.narracao?.cenas || {};
  const out = [];
  let fimAnterior = 0;
  for (const it of timeline.items) {
    const n = cenas[it.sceneIdx];
    if (!n?.blobId || !(n.durMs > 0)) continue;
    const start = Math.max(it.outStart, out.length ? fimAnterior + GAP_MS : 0);
    if (start >= timeline.total) break;
    const end = Math.min(start + n.durMs, timeline.total);
    out.push({ sceneIdx: it.sceneIdx, blobId: n.blobId, texto: n.texto || '', start, dur: end - start, corta: start + n.durMs > timeline.total });
    fimAnterior = end;
  }
  return out;
}

// Quebra cada fala em pedaços curtos (até 4 palavras / 26 letras) e distribui o tempo da fala
// pelo tamanho de cada pedaço: a legenda acompanha o ritmo da voz sem precisar de transcrição.
export function legendasDe(agenda) {
  const out = [];
  for (const a of agenda) {
    const words = a.texto.replace(/\s+/g, ' ').trim().split(' ').filter(Boolean);
    if (!words.length) continue;
    const chunks = [];
    let cur = [];
    for (const w of words) {
      const next = [...cur, w].join(' ');
      if (cur.length && (cur.length >= 4 || next.length > 26)) {
        chunks.push(cur.join(' '));
        cur = [w];
      } else cur.push(w);
      if (/[.!?,;:]$/.test(w) && cur.length >= 2) {
        chunks.push(cur.join(' '));
        cur = [];
      }
    }
    if (cur.length) chunks.push(cur.join(' '));
    const peso = (c) => c.length + 3;
    const totalPeso = chunks.reduce((s, c) => s + peso(c), 0);
    let t = a.start;
    for (const c of chunks) {
      const d = (a.dur * peso(c)) / totalPeso;
      out.push({ start: t, end: t + d, texto: c });
      t += d;
    }
  }
  return out;
}

export function legendaAt(t, legendas) {
  for (let i = 0; i < legendas.length; i++) if (t >= legendas[i].start && t < legendas[i].end) return i;
  return -1;
}

// Cenas cuja fala mudou depois de gerar a voz (ou que têm fala e ainda não têm voz).
export function falasPendentes(video) {
  const cenas = video.narracao?.cenas || {};
  return video.cenas
    .map((c, i) => ({ i, texto: (c.narracao || '').trim() }))
    .filter(({ i, texto }) => texto && (!cenas[i]?.blobId || cenas[i].texto !== texto || video.narracao?.voz !== video.edit?.voz));
}
