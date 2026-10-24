import { describe, it, expect } from 'vitest';
import { guessTipo, engajamentoPost, rankPosts, termos, precosCitados, radarInsights, ganchoDaLegenda } from './radar.js';
import { proximasDatas, dataEmFoco } from '../data/datas.js';
import { captionsPara } from '../data/copy.js';

const post = (o) => ({ plataforma: 'tiktok', views: 0, curtidas: 0, comentarios: 0, compart: 0, salvos: 0, legenda: '', tipo: 'outro', ...o });

describe('radar', () => {
  it('adivinha o tipo pela legenda', () => {
    expect(guessTipo('Abre comigo esse pedido ✨')).toBe('unboxing');
    expect(guessTipo('Quanto você acha que custa?')).toBe('adivinha');
    expect(guessTipo('olha o brilho na luz do sol')).toBe('brilho');
    expect(guessTipo('lindo', 'foto')).toBe('foto');
  });

  it('calcula engajamento e ordena', () => {
    expect(engajamentoPost(post({ views: 1000, curtidas: 80, comentarios: 10, compart: 5, salvos: 5 }))).toBeCloseTo(0.1);
    expect(engajamentoPost(post({ views: 0, curtidas: 10 }))).toBe(0);
    const a = post({ id: 'a', views: 100, curtidas: 50 });
    const b = post({ id: 'b', views: 900, curtidas: 9, plataforma: 'instagram' });
    expect(rankPosts([a, b], 'views').map((p) => p.id)).toEqual(['b', 'a']);
    expect(rankPosts([a, b], 'engajamento').map((p) => p.id)).toEqual(['a', 'b']);
    expect(rankPosts([a, b], 'views', 'tiktok').map((p) => p.id)).toEqual(['a']);
  });

  it('conta palavras e hashtags uma vez por post, sem palavras vazias', () => {
    const r = termos([
      post({ legenda: 'Brilho brilho que você vai amar #semijoias #Semijoias' }),
      post({ legenda: 'Muito brilho no colar #semijoias #fyp' }),
      post({ legenda: 'Colar dourado com brilho' }),
    ]);
    expect(r.palavras[0]).toEqual({ termo: 'brilho', n: 3 });
    expect(r.palavras.find((p) => p.termo === 'que')).toBeUndefined();
    expect(r.hashtags[0]).toEqual({ termo: '#semijoias', n: 2 });
  });

  it('lê preços das legendas', () => {
    expect(precosCitados([post({ legenda: 'por R$ 89,90 ou R$1.200,00' }), post({ legenda: 'R$ 59' })])).toEqual([59, 89.9, 1200]);
  });

  it('aponta o tipo campeão que ela ainda não gravou', () => {
    const ps = [post({ tipo: 'adivinha', views: 50000 }), post({ tipo: 'adivinha', views: 30000 }), post({ tipo: 'brilho', views: 2000 })];
    const r = radarInsights(ps, [{ formatoId: 'brilho' }]);
    expect(r.porTipo[0].id).toBe('adivinha');
    expect(r.dicas[0]).toContain('ainda não gravou');
  });

  it('extrai o gancho da primeira frase', () => {
    expect(ganchoDaLegenda('Para tudo e olha esse brilho! Link na bio #fyp')).toBe('Para tudo e olha esse brilho');
    expect(ganchoDaLegenda('#fyp')).toBe('');
    expect(ganchoDaLegenda('Olha o brilho desse brinco na luz do sol ☀️ R$ 59,90 qual você usaria?')).toBe('Olha o brilho desse brinco na luz do sol');
  });
});

describe('datas comerciais', () => {
  it('calcula Dia das Mães, Black Friday e Carnaval', () => {
    const ds = proximasDatas(new Date(2027, 0, 2), 20);
    const get = (id) => ds.find((d) => d.id === id).data;
    expect(get('maes').toDateString()).toBe(new Date(2027, 4, 9).toDateString());
    expect(get('blackfriday').toDateString()).toBe(new Date(2027, 10, 26).toDateString());
    expect(get('carnaval').toDateString()).toBe(new Date(2027, 1, 9).toDateString());
  });

  it('mostra a data em foco só dentro da janela de aviso', () => {
    expect(dataEmFoco(new Date(2026, 9, 24))).toBeNull(); // Black Friday 27/11 ainda longe (34 dias)
    expect(dataEmFoco(new Date(2026, 10, 20)).id).toBe('blackfriday');
    expect(proximasDatas(new Date(2026, 11, 25), 1)[0].dias).toBe(0);
  });
});

describe('legendas por rede', () => {
  it('adapta para Reels e Shorts', () => {
    const v = { gancho: 'Olha esse brilho', legenda: 'Olha esse brilho ✨ Colar por R$ 89,90.', hashtags: '#semijoias #colar #fyp #tiktokmefezcomprar' };
    const r = captionsPara(v);
    expect(r.reels).not.toContain('#fyp');
    expect(r.reels).toContain('#reels');
    expect(r.shorts.split('\n')[0].length).toBeLessThanOrEqual(100);
    expect(r.shorts).toContain('#shorts');
  });
});
