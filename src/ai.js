// Cliente da IA. A chave do Gemini fica só no servidor (api/ai.php); o app nunca a vê.
// Sem internet ou sem servidor configurado, as telas usam os textos prontos de data/copy.js.
const ENDPOINT = 'api/ai.php';

let status = null; // null = não verificado, true/false depois do ping

export async function aiAvailable() {
  if (status !== null) return status;
  if (!navigator.onLine) return false;
  try {
    const r = await fetch(`${ENDPOINT}?ping=1`, { cache: 'no-store' });
    const j = await r.json();
    status = !!j.configured;
  } catch {
    status = false;
  }
  return status;
}

async function call(payload, timeoutMs = 30000, endpoint = ENDPOINT) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-GlowStudio': '2' },
      body: JSON.stringify(payload),
      signal: ctrl.signal,
    });
    const j = await r.json().catch(() => null);
    if (!r.ok || !j || j.error) throw new Error(j?.error || `Erro ${r.status}`);
    return j;
  } finally {
    clearTimeout(t);
  }
}

const pecaPayload = (p) => ({
  nome: p.nome,
  tipo: p.tipo,
  banho: p.banho,
  milesimos: p.milesimos,
  pedra: p.pedra,
  preco: p.preco,
  precoPromo: p.precoPromo || null,
  obs: p.obs || '',
});

export function aiScript(peca, formato) {
  return call({
    action: 'script',
    peca: pecaPayload(peca),
    formato: { nome: formato.nome, cenas: formato.cenas.map((c) => ({ titulo: c.titulo, acao: c.acao, seg: c.seg })) },
  });
}

export async function aiPhoto(blob) {
  const base64 = await new Promise((resolve, reject) => {
    const fr = new FileReader();
    fr.onload = () => resolve(String(fr.result).split(',')[1]);
    fr.onerror = () => reject(fr.error);
    fr.readAsDataURL(blob);
  });
  return call({ action: 'photo', image: base64 }, 40000);
}

// ---------- Radar da concorrência ----------
// Lê um post público pelo link (não precisa da chave do Gemini).
export async function readLink(url) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), 25000);
  try {
    const r = await fetch(`api/link.php?url=${encodeURIComponent(url)}`, { headers: { 'X-GlowStudio': '2' }, signal: ctrl.signal });
    const j = await r.json().catch(() => null);
    if (!r.ok || !j || j.error) throw new Error(j?.error || 'Não consegui ler esse link');
    return j;
  } finally {
    clearTimeout(t);
  }
}

const radarPayload = (p) => ({
  plataforma: p.plataforma,
  perfil: p.perfil,
  legenda: (p.legenda || '').slice(0, 1200),
  views: p.views,
  curtidas: p.curtidas,
  comentarios: p.comentarios,
  duracao: p.duracao,
  tipo: p.tipo,
});

export function aiRadar(post) {
  const image = post.thumb?.startsWith('data:image/') ? post.thumb.split(',')[1] : '';
  return call({ action: 'radar', post: radarPayload(post), image }, 40000);
}

export const aiTendencias = (posts) => call({ action: 'tendencias', posts: posts.slice(0, 30).map(radarPayload) }, 40000);

// ---------- Vitrine pública ----------
export const vitrineCall = (payload) => call(payload, 40000, 'api/vitrine.php').catch((e) => {
  throw new Error(e.name === 'AbortError' ? 'O servidor demorou para responder' : e.message);
});
