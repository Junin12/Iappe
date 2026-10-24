// Insights calculados só com os dados que ela registrou. Nada inventado.
import { formatById } from '../data/formats.js';

const DIAS = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado'];

export function engajamento(m) {
  if (!m || !m.views) return 0;
  const inter = (m.curtidas || 0) + (m.comentarios || 0) + (m.compartilhamentos || 0) + (m.salvamentos || 0);
  return inter / m.views;
}

function bestBy(items, keyFn, valueFn, min = 2) {
  const groups = new Map();
  for (const it of items) {
    const k = keyFn(it);
    if (k == null) continue;
    const g = groups.get(k) || [];
    g.push(valueFn(it));
    groups.set(k, g);
  }
  let best = null;
  for (const [k, vals] of groups) {
    if (vals.length < min) continue;
    const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
    if (!best || avg > best.avg) best = { key: k, avg, n: vals.length };
  }
  return best;
}

export function insights(videos, pecas) {
  const posted = videos.filter((v) => v.status === 'postado' && v.metricas && v.postadoEm);
  const total = posted.reduce(
    (t, v) => ({
      views: t.views + (v.metricas.views || 0),
      vendas: t.vendas + (v.metricas.vendas || 0),
    }),
    { views: 0, vendas: 0 },
  );
  const engMedio = posted.length ? posted.reduce((s, v) => s + engajamento(v.metricas), 0) / posted.length : 0;

  const out = { n: posted.length, total, engMedio, dicas: [] };
  if (posted.length < 3) return out;

  const fmt = bestBy(posted, (v) => v.formatoId, (v) => v.metricas.views || 0);
  if (fmt) out.dicas.push(`O formato "${formatById(fmt.key).nome}" é o seu campeão: média de ${Math.round(fmt.avg).toLocaleString('pt-BR')} views em ${fmt.n} vídeos.`);

  const peca = bestBy(posted, (v) => v.pecaId, (v) => v.metricas.vendas || 0, 1);
  const pecaNome = peca && pecas.find((p) => p.id === peca.key)?.nome;
  if (peca && peca.avg > 0 && pecaNome) out.dicas.push(`"${pecaNome}" é a peça que mais vende pelos vídeos. Vale gravar outro formato com ela.`);

  if (posted.length >= 5) {
    const dia = bestBy(posted, (v) => new Date(v.postadoEm).getDay(), (v) => v.metricas.views || 0);
    if (dia) out.dicas.push(`Seus vídeos de ${DIAS[dia.key]} têm mais views. Tente postar nesse dia.`);
    const hora = bestBy(posted, (v) => Math.floor(new Date(v.postadoEm).getHours() / 3) * 3, (v) => v.metricas.views || 0);
    if (hora) out.dicas.push(`O melhor horário para você tem sido entre ${hora.key}h e ${hora.key + 3}h.`);
  }
  return out;
}

// Semanas seguidas (até a atual) com pelo menos uma postagem.
export function streakSemanas(videos, now = new Date()) {
  const weekKey = (t) => {
    const d = new Date(t);
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
    return d.getTime();
  };
  const weeks = new Set(videos.filter((v) => v.status === 'postado' && v.postadoEm).map((v) => weekKey(v.postadoEm)));
  let k = weekKey(now);
  let n = 0;
  if (!weeks.has(k)) k -= 7 * 864e5; // semana atual ainda pode estar em andamento
  while (weeks.has(k)) {
    n++;
    k -= 7 * 864e5;
  }
  return n;
}
