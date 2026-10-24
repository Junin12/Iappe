import { describe, it, expect } from 'vitest';
import { engajamento, insights, streakSemanas } from './insights.js';
import { hooksFor, hashtagsFor, captionFor, precoFinal } from '../data/copy.js';

const day = 864e5;

describe('insights', () => {
  it('calcula engajamento', () => {
    expect(engajamento({ views: 100, curtidas: 8, comentarios: 1, compartilhamentos: 1, salvamentos: 0 })).toBeCloseTo(0.1);
    expect(engajamento({ views: 0 })).toBe(0);
  });

  it('não dá dicas com poucos vídeos e aponta o melhor formato', () => {
    const base = { status: 'postado', pecaId: 'p1', postadoEm: Date.now() };
    const v = (formatoId, views) => ({ ...base, formatoId, metricas: { views, vendas: 0 } });
    expect(insights([v('brilho', 10)], []).dicas).toHaveLength(0);
    const r = insights([v('brilho', 100), v('brilho', 300), v('unboxing', 50), v('unboxing', 60)], []);
    expect(r.dicas[0]).toContain('Brilho na luz');
    expect(r.total.views).toBe(510);
  });

  it('conta semanas seguidas com postagem', () => {
    const now = new Date('2026-10-24T12:00:00'); // sábado
    const p = (t) => ({ status: 'postado', postadoEm: t });
    expect(streakSemanas([p(now.getTime() - day), p(now.getTime() - 7 * day), p(now.getTime() - 14 * day)], now)).toBe(3);
    expect(streakSemanas([p(now.getTime() - 21 * day)], now)).toBe(0);
  });
});

describe('textos', () => {
  const peca = { nome: 'Colar Ponto de Luz', tipo: 'Colar', banho: 'Ouro 18k', pedra: 'Nenhuma', preco: 89.9, precoPromo: 69.9 };

  it('usa o preço promocional quando menor', () => {
    expect(precoFinal(peca)).toBe(69.9);
  });

  it('ganchos não citam pedra se a peça não tem', () => {
    const hooks = hooksFor(peca, 20);
    expect(hooks.some((h) => h.includes('nenhuma'))).toBe(false);
    expect(hooks.some((h) => h.includes('R$'))).toBe(true);
  });

  it('legenda cabe no limite e hashtags no máximo 8', () => {
    expect(captionFor(peca, 'Para de rolar se você ama brilho').length).toBeLessThanOrEqual(150);
    expect(hashtagsFor(peca).split(' ').length).toBeLessThanOrEqual(8);
  });
});
