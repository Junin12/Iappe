import { describe, it, expect } from 'vitest';
import { agendaNarracao, legendasDe, legendaAt, falasPendentes } from './narracao.js';

const timeline = {
  items: [
    { sceneIdx: 0, outStart: 0, outDur: 3000 },
    { sceneIdx: 1, outStart: 3000, outDur: 3000 },
    { sceneIdx: 2, outStart: 6000, outDur: 3000 },
  ],
  total: 9000,
};

describe('narração', () => {
  it('começa cada fala com a cena e nunca sobrepõe vozes', () => {
    const video = { narracao: { cenas: { 0: { blobId: 'a', durMs: 4000, texto: 'um' }, 1: { blobId: 'b', durMs: 1000, texto: 'dois' }, 2: { blobId: 'c', durMs: 5000, texto: 'três' } } } };
    const ag = agendaNarracao(timeline, video);
    expect(ag.map((a) => a.start)).toEqual([0, 4120, 6000]);
    expect(ag[2].dur).toBe(3000); // cortada no fim do vídeo
    expect(ag[2].corta).toBe(true);
  });

  it('ignora cenas sem voz', () => {
    expect(agendaNarracao(timeline, { narracao: { cenas: { 1: { texto: 'x' } } } })).toEqual([]);
  });

  it('quebra a fala em legendas curtas que cobrem o tempo todo da voz', () => {
    const leg = legendasDe([{ start: 1000, dur: 3000, texto: 'Olha o brilho desse colar na luz do sol, é lindo demais!' }]);
    expect(leg.length).toBeGreaterThan(2);
    expect(leg.every((l) => l.texto.split(' ').length <= 4 && l.texto.length <= 26 + 10)).toBe(true);
    expect(leg[0].start).toBe(1000);
    expect(leg[leg.length - 1].end).toBeCloseTo(4000);
    expect(legendaAt(1001, leg)).toBe(0);
    expect(legendaAt(5000, leg)).toBe(-1);
  });

  it('aponta falas sem voz ou alteradas', () => {
    const v = {
      cenas: [{ narracao: 'Oi' }, { narracao: 'Mudou' }, { narracao: '' }],
      edit: { voz: 'Aoede' },
      narracao: { voz: 'Aoede', cenas: { 0: { blobId: 'a', texto: 'Oi' }, 1: { blobId: 'b', texto: 'Antes' } } },
    };
    expect(falasPendentes(v).map((p) => p.i)).toEqual([1]);
    expect(falasPendentes({ ...v, edit: { voz: 'Kore' } }).map((p) => p.i)).toEqual([0, 1]);
  });
});
