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

async function call(payload, timeoutMs = 30000) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch(ENDPOINT, {
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
