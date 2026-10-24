// Ganchos, legendas e hashtags gerados sem internet a partir dos dados reais da peça.
// Regra: nada de promessa falsa ("ouro maciço", "nunca escurece").

export const brl = (v) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const precoFinal = (peca) =>
  peca?.precoPromo && peca.precoPromo < peca.preco ? peca.precoPromo : peca?.preco || 0;

const HOOKS = [
  'Para de rolar se você ama brilho',
  'Esse {tipo} parece joia de vitrine',
  '{tipo} por {preco} que parece muito mais caro',
  'O {tipo} que mais pediram essa semana',
  'Você usaria esse {tipo} todo dia?',
  'Presente perfeito por menos de {preco}',
  'Repara no brilho dessa {pedra}',
  'Banho de {banho} com {milesimos} milésimos de perto',
  'Achei o {tipo} que combina com tudo',
  'Me conta: dia a dia ou festa?',
  'Quanto você acha que custa esse {tipo}?',
  'Abre comigo esse pedido',
  '3 jeitos de usar o mesmo {tipo}',
  'Antes e depois com um {tipo} só',
  'O detalhe que ninguém repara, mas faz diferença',
  'Esse {tipo} chegou e eu já quero um pra mim',
  'Se você gosta de acessório delicado, olha isso',
  'Ela vai amar ganhar esse {tipo}',
  'Brilho que aparece até de longe',
  'O mix perfeito pra quem ama dourado',
];

function fill(template, peca) {
  const tipo = (peca?.tipo || 'acessório').toLowerCase();
  const text = template
    .replaceAll('{tipo}', tipo)
    .replaceAll('{preco}', brl(precoFinal(peca)))
    .replaceAll('{pedra}', (peca?.pedra && peca.pedra !== 'Nenhuma' ? peca.pedra : 'peça').toLowerCase())
    .replaceAll('{banho}', (peca?.banho || 'ouro 18k').toLowerCase())
    .replaceAll('{milesimos}', String(peca?.milesimos || 10));
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// Ganchos que fazem sentido para a peça (sem {pedra} se não tiver pedra, sem banho se não informado).
export function hooksFor(peca, count = 6, seed = 0) {
  const usable = HOOKS.filter((h) => {
    if (h.includes('{pedra}') && (!peca?.pedra || peca.pedra === 'Nenhuma')) return false;
    if (h.includes('{preco}') && !precoFinal(peca)) return false;
    return true;
  });
  const out = [];
  for (let i = 0; i < Math.min(count, usable.length); i++) {
    out.push(fill(usable[(i + seed) % usable.length], peca));
  }
  return out;
}

const TAGS_TIPO = {
  Colar: ['#colar', '#colarfeminino'],
  Brinco: ['#brinco', '#brincos'],
  Anel: ['#anel', '#aneis'],
  Pulseira: ['#pulseira', '#pulseiras'],
  Conjunto: ['#conjunto', '#kitsemijoias'],
  Tornozeleira: ['#tornozeleira'],
};

export function hashtagsFor(peca) {
  const nicho = ['#semijoias', '#semijoiasfinas', '#acessoriosfemininos'];
  const banho = /ouro/i.test(peca?.banho || '') ? ['#banhadoaouro'] : /ródio|rodio/i.test(peca?.banho || '') ? ['#rodio'] : [];
  const amplas = ['#achadinhos', '#tiktokmefezcomprar', '#fyp'];
  return [...nicho, ...(TAGS_TIPO[peca?.tipo] || []), ...banho, ...amplas].slice(0, 8).join(' ');
}

export function captionFor(peca, gancho) {
  const nome = peca?.nome || 'Peça nova';
  const preco = precoFinal(peca);
  const partes = [gancho ? `${gancho} ✨` : `${nome} ✨`];
  if (preco) partes.push(`${nome} por ${brl(preco)}.`);
  partes.push(peca?.link ? 'Link no perfil 💛' : 'Chama no direct 💛');
  return partes.join(' ').slice(0, 150);
}

// Mesmo vídeo, outras redes: Reels quer menos hashtags de TikTok; Shorts usa título curto + #shorts.
const SO_TIKTOK = /^#(fyp|foryou|foryoupage|paravoce|pravoce|tiktok\w*|viral)$/i;

export function captionsPara(video) {
  const tags = (video.hashtags || '').split(/\s+/).filter(Boolean);
  const nicho = tags.filter((t) => !SO_TIKTOK.test(t));
  const legenda = (video.legenda || '').trim();
  const reels = `${legenda}\n\n${[...nicho.slice(0, 4), '#reels'].join(' ')}`.trim();
  const titulo = (video.gancho || legenda).replace(/\s+/g, ' ').trim().slice(0, 90);
  const shorts = `${titulo}\n${legenda}\n${[...nicho.slice(0, 2), '#shorts'].join(' ')}`.trim();
  return { reels, shorts };
}
