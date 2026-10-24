// Ranking e padrões dos posts da concorrência guardados no Radar. Tudo calculado no aparelho.
import { FORMATS } from '../data/formats.js';

export const TIPOS_RADAR = [
  ...FORMATS.map((f) => ({ id: f.id, nome: f.nome, emoji: f.emoji })),
  { id: 'foto', nome: 'Foto ou carrossel', emoji: '🖼️' },
  { id: 'outro', nome: 'Outro', emoji: '✨' },
];
export const tipoRadar = (id) => TIPOS_RADAR.find((t) => t.id === id) || TIPOS_RADAR[TIPOS_RADAR.length - 1];

// Palpite pelo texto da legenda (a IA, quando ligada, corrige).
const PISTAS = [
  ['unboxing', /unbox|abrindo|abre comigo|recebi|chegou|pedido/i],
  ['antes-depois', /antes e depois|antes x depois|transforma/i],
  ['tres-jeitos', /\b\d+ (jeitos|formas|maneiras)|como usar|combina/i],
  ['presente', /presente|presentear|pov|namorad|m[aã]e/i],
  ['adivinha', /adivinha|quanto (custa|acha|voc[eê])|chuta o pre[cç]o/i],
  ['mix', /\bmix\b|mixando|montando|empilha|layer/i],
  ['qualidade', /teste|prova|qualidade|n[aã]o (escurece|descasca)|resist/i],
  ['brilho', /brilh|luz|reflex|sol\b/i],
];

export function guessTipo(legenda = '', formato = 'video') {
  for (const [id, re] of PISTAS) if (re.test(legenda)) return id;
  return formato === 'foto' ? 'foto' : 'outro';
}

export function engajamentoPost(p) {
  const base = p.views || 0;
  const inter = (p.curtidas || 0) + (p.comentarios || 0) + (p.compart || 0) + (p.salvos || 0);
  return base > 0 ? inter / base : 0;
}

const ORDENS = {
  views: (p) => p.views || 0,
  curtidas: (p) => p.curtidas || 0,
  engajamento: (p) => engajamentoPost(p),
  comentarios: (p) => p.comentarios || 0,
};

export function rankPosts(posts, by = 'views', plataforma = '') {
  const key = ORDENS[by] || ORDENS.views;
  return posts.filter((p) => !plataforma || p.plataforma === plataforma).sort((a, b) => key(b) - key(a));
}

const STOP = new Set(
  'a o e é de da do das dos em no na nos nas um uma uns umas que com por para pra pro pros pras se sua seu suas seus meu minha meus minhas eu voce você vc ela ele elas eles isso esse essa este esta esses essas aqui ai aí lá la mais muito muita mas como ou ao aos já ja tem ter foi ser sao são só so nao não sim tá ta to tô nós nos te me mim ti lhe vai vou quem qual quando onde porque pq tudo todo toda todos todas cada também tambem até ate bem dia hoje agora link perfil bio desse dessa deste desta disso nesse nessa neste nesta aquele aquela pelo pela pelos pelas sobre entre ainda depois antes vem'.split(' '),
);

const norm = (w) => w.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');

// Conta palavras e hashtags. Cada post conta uma vez por termo, para um post longo não dominar.
export function termos(posts) {
  const palavras = new Map();
  const grafia = new Map(); // primeira grafia vista (com acento) para mostrar
  const tags = new Map();
  for (const p of posts) {
    const txt = p.legenda || '';
    const vistos = new Set();
    for (const t of txt.match(/#[\p{L}\p{N}_]+/gu) || []) {
      const k = t.toLowerCase();
      if (vistos.has(k)) continue;
      vistos.add(k);
      tags.set(k, (tags.get(k) || 0) + 1);
    }
    const semTags = txt.replace(/#[\p{L}\p{N}_]+/gu, ' ').replace(/@[\w.]+/g, ' ').replace(/https?:\S+/g, ' ');
    for (const w of semTags.match(/\p{L}{3,}/gu) || []) {
      const k = norm(w);
      if (STOP.has(k) || vistos.has(k)) continue;
      vistos.add(k);
      palavras.set(k, (palavras.get(k) || 0) + 1);
      if (!grafia.has(k)) grafia.set(k, w.toLowerCase());
    }
  }
  const top = (m, n) => [...m.entries()].filter(([, c]) => c > 1 || posts.length < 4).sort((a, b) => b[1] - a[1]).slice(0, n).map(([termo, n]) => ({ termo, n }));
  return { palavras: top(palavras, 12).map((p) => ({ ...p, termo: grafia.get(p.termo) })), hashtags: top(tags, 10) };
}

// Preços citados nas legendas ("R$ 89,90", "por 59").
export function precosCitados(posts) {
  const out = [];
  for (const p of posts) {
    for (const m of (p.legenda || '').matchAll(/R\$\s?(\d{1,4}(?:\.\d{3})*(?:,\d{2})?)/g)) {
      const v = Number(m[1].replace(/\./g, '').replace(',', '.'));
      if (v > 0 && v < 10000) out.push(v);
    }
  }
  return out.sort((a, b) => a - b);
}

const mediana = (xs) => (xs.length ? (xs.length % 2 ? xs[(xs.length - 1) / 2] : (xs[xs.length / 2 - 1] + xs[xs.length / 2]) / 2) : 0);

export function radarInsights(posts, meusVideos = []) {
  const comViews = posts.filter((p) => p.views > 0);
  const porTipo = TIPOS_RADAR.map((t) => {
    const xs = posts.filter((p) => p.tipo === t.id);
    const vs = xs.filter((p) => p.views > 0);
    return {
      ...t,
      n: xs.length,
      mediaViews: vs.length ? Math.round(vs.reduce((s, p) => s + p.views, 0) / vs.length) : 0,
      eng: vs.length ? vs.reduce((s, p) => s + engajamentoPost(p), 0) / vs.length : 0,
    };
  })
    .filter((t) => t.n > 0)
    .sort((a, b) => b.mediaViews - a.mediaViews || b.n - a.n);

  const porPlataforma = ['tiktok', 'instagram', 'youtube'].map((pl) => {
    const xs = comViews.filter((p) => p.plataforma === pl);
    return { plataforma: pl, n: posts.filter((p) => p.plataforma === pl).length, mediaViews: xs.length ? Math.round(xs.reduce((s, p) => s + p.views, 0) / xs.length) : 0 };
  });

  const dur = comViews.filter((p) => p.duracao > 0);
  const top = [...comViews].sort((a, b) => b.views - a.views).slice(0, Math.max(3, Math.ceil(comViews.length / 3)));
  const durTop = top.filter((p) => p.duracao > 0).map((p) => p.duracao).sort((a, b) => a - b);
  const precos = precosCitados(posts);
  const { palavras, hashtags } = termos(top.length >= 3 ? top : posts);

  const dicas = [];
  const usados = new Set(meusVideos.map((v) => v.formatoId));
  const melhor = porTipo.find((t) => t.mediaViews > 0);
  if (melhor && posts.length >= 3) {
    dicas.push(
      usados.has(melhor.id) || !FORMATS.some((f) => f.id === melhor.id)
        ? `${melhor.emoji} "${melhor.nome}" é o tipo que mais dá views na concorrência (média de ${fmtCurto(melhor.mediaViews)}).`
        : `${melhor.emoji} "${melhor.nome}" é o que mais dá views na concorrência e você ainda não gravou. Teste esta semana!`,
    );
  }
  if (durTop.length >= 3) dicas.push(`Os vídeos que mais bombam têm por volta de ${Math.round(mediana(durTop))} segundos.`);
  if (precos.length >= 3) dicas.push(`Preço que mais aparece nas legendas: entre ${fmtBRL(precos[Math.floor(precos.length * 0.25)])} e ${fmtBRL(precos[Math.floor(precos.length * 0.75)])}.`);
  const com = top.filter((p) => /coment|me conta|qual (voc|vc)|\?/i.test(p.legenda || '')).length;
  if (top.length >= 3 && com / top.length >= 0.5) dicas.push('A maioria dos posts mais vistos termina com uma pergunta para o público comentar.');
  if (palavras[0]) dicas.push(`Palavras que se repetem nos posts mais fortes: ${palavras.slice(0, 4).map((p) => p.termo).join(', ')}.`);

  return {
    total: posts.length,
    porTipo,
    porPlataforma,
    duracaoMediana: Math.round(mediana(dur.map((p) => p.duracao).sort((a, b) => a - b))),
    precos: precos.length ? { min: precos[0], max: precos[precos.length - 1], mediana: mediana(precos) } : null,
    palavras,
    hashtags,
    dicas,
  };
}

export const fmtCurto = (n) => (n >= 1e6 ? (n / 1e6).toFixed(1).replace('.', ',') + ' mi' : n >= 1e4 ? Math.round(n / 1e3) + ' mil' : (n || 0).toLocaleString('pt-BR'));
const fmtBRL = (v) => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });

// Primeira frase da legenda: costuma ser o gancho escrito.
export function ganchoDaLegenda(legenda = '') {
  const first = legenda
    .replace(/#[\p{L}\p{N}_]+/gu, '')
    .replace(/R\$\s?[\d.,]+/g, '')
    .split(/[\n.!?]|\p{Extended_Pictographic}/u)[0]
    .replace(/\s+/g, ' ')
    .trim();
  return first.length >= 8 ? first.slice(0, 70) : '';
}
