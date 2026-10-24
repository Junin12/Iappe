import { describe, it, expect } from 'vitest';
import { buildTimeline, overlayAt, itemAt, defaultTrim } from './timeline.js';

const video = {
  cenas: [
    { seg: 3 },
    { seg: 4, speed: 0.5 },
    { seg: 3, fim: true },
  ],
  clips: {
    0: { durMs: 5000, inMs: 0, outMs: 3000 },
    1: { durMs: 3000, inMs: 0, outMs: 2000 },
    2: { durMs: 4000, inMs: 1000, outMs: 4000 },
  },
};

describe('timeline', () => {
  it('soma durações considerando câmera lenta', () => {
    const tl = buildTimeline(video);
    expect(tl.items.map((i) => i.outDur)).toEqual([3000, 4000, 3000]);
    expect(tl.total).toBe(10000);
    expect(tl.items[1].slow).toBe(true);
  });

  it('ignora cenas sem clipe', () => {
    const tl = buildTimeline({ ...video, clips: { 0: video.clips[0] } });
    expect(tl.items).toHaveLength(1);
  });

  it('gancho no início e preço na cena final', () => {
    const tl = buildTimeline(video);
    const edit = { mostrarPreco: true, cta: 'Link no perfil' };
    expect(overlayAt(1000, tl, edit)).toBe('hook');
    expect(overlayAt(5000, tl, edit)).toBe(null);
    expect(overlayAt(7500, tl, edit)).toBe('end');
    expect(overlayAt(7500, tl, { mostrarPreco: false, cta: '' })).toBe(null);
  });

  it('acha o trecho do instante', () => {
    const tl = buildTimeline(video);
    expect(itemAt(0, tl)).toBe(0);
    expect(itemAt(3500, tl)).toBe(1);
    expect(itemAt(99999, tl)).toBe(2);
  });

  it('corte padrão respeita a duração da cena', () => {
    expect(defaultTrim(2000, { seg: 3 })).toEqual({ inMs: 0, outMs: 2000 });
    expect(defaultTrim(6000, { seg: 3 })).toEqual({ inMs: 400, outMs: 3400 });
    expect(defaultTrim(6000, { seg: 4, speed: 0.5 })).toEqual({ inMs: 400, outMs: 2400 });
  });
});
