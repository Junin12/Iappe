// Datas que mais vendem semijoias no Brasil, com o formato e o gancho sugeridos.
// "aviso" = quantos dias antes começar a postar (o vídeo precisa de tempo para circular).

// Enésimo dia da semana do mês (ex.: 2º domingo de maio). wd: 0 = domingo.
const nth = (y, m, wd, n) => {
  const d = new Date(y, m, 1);
  d.setDate(1 + ((wd - d.getDay() + 7) % 7) + (n - 1) * 7);
  return d;
};

// Domingo de Páscoa (algoritmo de Meeus), base do Carnaval.
function pascoa(y) {
  const a = y % 19, b = Math.floor(y / 100), c = y % 100, d = Math.floor(b / 4), e = b % 4;
  const f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30, i = Math.floor(c / 4), k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7, m = Math.floor((a + 11 * h + 22 * l) / 451);
  const month = Math.floor((h + l - 7 * m + 114) / 31) - 1;
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return new Date(y, month, day);
}

const DATAS = [
  { id: 'carnaval', nome: 'Carnaval', emoji: '🎭', quando: (y) => new Date(pascoa(y).getTime() - 47 * 864e5), aviso: 14, formato: 'brilho', gancho: 'Brilho pronto para o Carnaval', dica: 'Peças grandes e douradas, muito brilho na luz.' },
  { id: 'mulher', nome: 'Dia da Mulher', emoji: '💐', quando: (y) => new Date(y, 2, 8), aviso: 14, formato: 'presente', gancho: 'Um mimo para você mesma', dica: 'Fale de autocuidado: "se presenteie".' },
  { id: 'maes', nome: 'Dia das Mães', emoji: '💝', quando: (y) => nth(y, 4, 0, 2), aviso: 25, formato: 'presente', gancho: 'O presente que ela vai usar todo dia', dica: 'A maior data do ano para semijoias. Comece cedo e mostre a embalagem.' },
  { id: 'namorados', nome: 'Dia dos Namorados', emoji: '❤️', quando: (y) => new Date(y, 5, 12), aviso: 21, formato: 'presente', gancho: 'Ela vai amar ganhar isso', dica: 'Fale com quem vai dar o presente: "marca ele aqui".' },
  { id: 'amiga', nome: 'Dia da Amiga', emoji: '👯', quando: (y) => new Date(y, 6, 20), aviso: 10, formato: 'mix', gancho: 'Marca a amiga que merece esse kit', dica: 'Peças em dupla e kits combinando.' },
  { id: 'cliente', nome: 'Dia do Cliente', emoji: '🛍️', quando: (y) => new Date(y, 8, 15), aviso: 7, formato: 'adivinha', gancho: 'Preço especial só essa semana', dica: 'Bom para promoção relâmpago e cupom.' },
  { id: 'professor', nome: 'Dia do Professor', emoji: '🍎', quando: (y) => new Date(y, 9, 15), aviso: 10, formato: 'presente', gancho: 'Presente delicado para a professora', dica: 'Peças pequenas e baratas vendem mais.' },
  { id: 'blackfriday', nome: 'Black Friday', emoji: '🖤', quando: (y) => new Date(y, 10, nth(y, 10, 4, 4).getDate() + 1), aviso: 14, formato: 'adivinha', gancho: 'Adivinha o preço na Black Friday', dica: 'Mostre o preço antigo riscado. Avise a data antes para criar expectativa.' },
  { id: 'natal', nome: 'Natal', emoji: '🎄', quando: (y) => new Date(y, 11, 25), aviso: 30, formato: 'presente', gancho: 'O presente de Natal que cabe no bolso', dica: 'Faça vídeos por faixa de preço: até R$ 50, até R$ 100.' },
  { id: 'reveillon', nome: 'Réveillon', emoji: '🥂', quando: (y) => new Date(y, 11, 31), aviso: 10, formato: 'brilho', gancho: 'Dourado ou prata para a virada?', dica: 'Pergunte a cor da virada: gera muito comentário.' },
];

// Próximas datas a partir de hoje (inclui o próprio dia), com os dias que faltam.
export function proximasDatas(now = new Date(), count = DATAS.length) {
  const hoje = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const out = [];
  for (const y of [hoje.getFullYear(), hoje.getFullYear() + 1]) {
    for (const d of DATAS) {
      const data = d.quando(y);
      const dias = Math.round((data - hoje) / 864e5);
      if (dias >= 0) out.push({ ...d, data, dias, chave: `${d.id}-${y}` });
    }
  }
  return out.sort((a, b) => a.dias - b.dias).slice(0, count);
}

// A data mais próxima que já entrou na janela de aviso (para o card da tela inicial).
export const dataEmFoco = (now = new Date()) => proximasDatas(now, 3).find((d) => d.dias <= d.aviso) || null;
