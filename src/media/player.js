// Toca as cenas em sequência num canvas (prévia) e gera o MP4 final (exportação).
// Exportação: quadro a quadro com WebCodecs quando o aparelho tem; senão grava a reprodução do canvas em tempo real.
import { buildTimeline, overlayAt, itemAt } from '../logic/timeline.js';
import { agendaNarracao, legendasDe, legendaAt } from '../logic/narracao.js';
import { createRenderer, finalParams, drawHook, drawEnd, ensureFonts, createComposer } from './renderer.js';
import { pickMime, baseType } from './mime.js';
import { brl, precoFinal } from '../data/copy.js';

const once = (el, ev, ms = 8000) =>
  new Promise((resolve) => {
    const done = () => {
      el.removeEventListener(ev, done);
      resolve();
    };
    el.addEventListener(ev, done);
    setTimeout(done, ms);
  });

// Volume do som original quando há narração por cima.
const DUCK = 0.25;

export async function createPlayer({ canvas, video, peca, getBlob, onTime, onEnd, onState }) {
  const timeline = buildTimeline(video);
  const { items, total } = timeline;
  if (!items.length) throw new Error('Grave pelo menos uma cena para editar.');

  const renderer = createRenderer(canvas);
  const compose = createComposer();
  await ensureFonts();

  // Vídeos ficam no DOM (escondidos): o Safari só decodifica quadros de vídeos anexados.
  const holder = document.createElement('div');
  holder.style.cssText = 'position:fixed;left:0;top:0;width:2px;height:2px;opacity:0;pointer-events:none;overflow:hidden';
  document.body.appendChild(holder);

  const urls = [];
  const els = [];
  const blobs = [];
  for (const it of items) {
    const blob = await getBlob(it.clip.blobId);
    if (!blob) throw new Error('Um dos clipes sumiu do aparelho. Grave a cena de novo.');
    blobs.push(blob);
    const url = URL.createObjectURL(blob);
    urls.push(url);
    const el = document.createElement('video');
    el.playsInline = true;
    el.setAttribute('playsinline', '');
    el.setAttribute('webkit-playsinline', '');
    el.preload = 'auto';
    el.muted = true;
    el.src = url;
    holder.appendChild(el);
    els.push(el);
  }

  // Narração: um <audio> por fala, tocado no instante certo da linha do tempo.
  const agenda = agendaNarracao(timeline, video);
  const narr = [];
  for (const a of agenda) {
    const blob = await getBlob(a.blobId);
    if (!blob) continue;
    const url = URL.createObjectURL(blob);
    urls.push(url);
    const el = document.createElement('audio');
    el.preload = 'auto';
    el.src = url;
    holder.appendChild(el);
    narr.push({ ...a, blob, el });
  }
  const legendas = legendasDe(narr);

  await Promise.all(els.map((el) => (el.readyState >= 2 ? null : once(el, 'loadeddata'))));
  await Promise.all(
    els.map((el, k) => {
      el.currentTime = items[k].inMs / 1000;
      return once(el, 'seeked', 3000);
    }),
  );

  let edit = video.edit;
  let params = finalParams(edit);
  let overlays = {};
  let lastKey = undefined;
  let i = 0;
  let playing = false;
  let exporting = false;
  let frameToken = 0;
  let t = 0;
  let stallSince = 0;
  let lastCt = -1;

  const narrando = () => !!edit.narrar && narr.length > 0;

  const buildOverlays = () => {
    const preco = precoFinal(peca);
    const old = peca?.precoPromo && peca.precoPromo < peca.preco ? brl(peca.preco) : null;
    overlays = {
      hook: drawHook(video.gancho, edit.hookStyle, edit.hookPos),
      end: drawEnd({ price: preco ? brl(preco) : '', oldPrice: old, cta: edit.cta, showPrice: edit.mostrarPreco }),
    };
    lastKey = undefined;
  };
  buildOverlays();

  const applyAudio = (k) => {
    if (exporting) return;
    els[k].muted = edit.audio === 'mudo' || items[k].slow;
    els[k].volume = narrando() ? DUCK : 1;
  };

  // Sobreposição do instante t: gancho/preço + legenda da fala (se ligadas).
  const setOverlayFor = (time) => {
    const kind = overlayAt(time, timeline, edit);
    const cap = narrando() && edit.legendas !== false ? legendaAt(time, legendas) : -1;
    const key = `${kind}|${cap}`;
    if (key === lastKey) return;
    lastKey = key;
    const base = kind ? overlays[kind] : null;
    renderer.setOverlay(cap >= 0 ? compose(base, legendas[cap].texto, kind === 'end' ? 'alto' : 'baixo') : base);
  };

  const render = () => {
    setOverlayFor(t);
    renderer.draw(els[i], params);
  };

  // Liga/desliga cada fala conforme o tempo (prévia e gravação em tempo real).
  const syncNarr = (time, run) => {
    for (const n of narr) {
      const dentro = run && narrando() && time >= n.start && time < n.start + n.dur;
      if (dentro && n.el.paused) {
        n.el.currentTime = Math.max(0, (time - n.start) / 1000);
        n.el.play().catch(() => {});
      } else if (!dentro && !n.el.paused) n.el.pause();
    }
  };

  const timeOf = (k) => {
    const it = items[k];
    return it.outStart + Math.max(0, els[k].currentTime * 1000 - it.inMs) / it.speed;
  };

  const startEl = (k) => {
    const el = els[k];
    el.playbackRate = items[k].speed;
    applyAudio(k);
    stallSince = performance.now();
    lastCt = -1;
    el.play().catch(() => {
      // Safari bloqueou som fora do toque: segue sem som para não travar.
      el.muted = true;
      el.play().catch(() => {});
    });
  };

  // rAF com reserva por timer: se o navegador pausar o rAF (economia de bateria, tela coberta),
  // a reprodução continua a ~25 qps em vez de travar.
  const nextFrame = (fn) => {
    const tok = ++frameToken;
    let ran = false;
    const run = () => {
      if (ran || tok !== frameToken) return;
      ran = true;
      fn();
    };
    requestAnimationFrame(run);
    setTimeout(run, 40);
  };

  const loop = () => {
    if (!playing) return;
    const it = items[i];
    const el = els[i];
    const ctMs = el.currentTime * 1000;
    if (ctMs >= it.outMs - 15 || el.ended) {
      el.pause();
      if (i + 1 >= items.length) {
        playing = false;
        t = total;
        syncNarr(t, false);
        onTime?.(t);
        onState?.(false);
        onEnd?.();
        return;
      }
      i += 1;
      const prev = i - 1;
      setTimeout(() => {
        els[prev].currentTime = items[prev].inMs / 1000;
      }, 0);
      startEl(i);
    } else {
      if (ctMs !== lastCt) {
        lastCt = ctMs;
        stallSince = performance.now();
      } else if (performance.now() - stallSince > 2000 && !el.muted) {
        el.muted = true;
        el.play().catch(() => {});
        stallSince = performance.now();
      }
      t = timeOf(i);
      render();
      syncNarr(t, true);
      onTime?.(t);
    }
    nextFrame(loop);
  };

  // Precisa rodar dentro do toque: libera o play() com som de todos os clipes e falas no iPhone.
  const unlock = (all = false) =>
    Promise.all([
      ...els.map((el, k) => {
        if (k === i && !all) return null;
        const wasMuted = el.muted;
        el.muted = false;
        const p = el.play();
        if (!p?.then) return null;
        return p
          .then(() => {
            el.pause();
            el.currentTime = items[k].inMs / 1000;
          })
          .catch(() => {})
          .finally(() => {
            el.muted = wasMuted;
          });
      }),
      ...narr.map((n) => {
        n.el.muted = true;
        const p = n.el.play();
        if (!p?.then) return null;
        return p
          .then(() => n.el.pause())
          .catch(() => {})
          .finally(() => {
            n.el.muted = false;
            n.el.currentTime = 0;
          });
      }),
    ]);

  const api = {
    timeline,
    total,
    narr,
    get playing() {
      return playing;
    },
    get time() {
      return t;
    },
    play() {
      if (playing) return;
      if (t >= total - 30) {
        i = 0;
        els[0].currentTime = items[0].inMs / 1000;
        t = 0;
      }
      unlock();
      playing = true;
      onState?.(true);
      startEl(i);
      nextFrame(loop);
    },
    pause() {
      playing = false;
      frameToken++;
      els[i]?.pause();
      syncNarr(t, false);
      onState?.(false);
    },
    toggle() {
      playing ? api.pause() : api.play();
    },
    async seek(ms) {
      api.pause();
      t = Math.max(0, Math.min(total - 1, ms));
      i = itemAt(t, timeline);
      const it = items[i];
      els[i].currentTime = (it.inMs + (t - it.outStart) * it.speed) / 1000;
      await once(els[i], 'seeked', 1500);
      render();
      onTime?.(t);
    },
    setEdit(next) {
      edit = next;
      params = finalParams(edit);
      buildOverlays();
      els.forEach((_, k) => applyAudio(k));
      if (!playing) render();
    },
    setHook(text) {
      video = { ...video, gancho: text };
      buildOverlays();
      if (!playing) render();
    },
    redraw: () => render(),

    async exportVideo({ onProgress, signal }) {
      api.pause();
      // Libera o som ainda dentro do toque, caso precise cair na gravação em tempo real.
      const unlocking = unlock(true);
      const origGain = edit.audio === 'mudo' ? 0 : narrando() ? DUCK : 1;
      const withAudio = origGain > 0 || narrando();
      const { fastSupported, fastExport } = await import('./fastExport.js');
      if (await fastSupported(withAudio)) {
        try {
          exporting = true;
          const blob = await fastExport({
            canvas,
            items,
            total,
            blobs,
            narr: narrando() ? narr : [],
            withAudio,
            origGain,
            onProgress,
            signal,
            renderFrame: (src, time) => {
              setOverlayFor(time);
              renderer.draw(src, params);
            },
          });
          return blob;
        } catch (e) {
          console.warn('Exportação rápida falhou; gravando em tempo real.', e);
          if (signal?.aborted) return null;
        } finally {
          exporting = false;
          lastKey = undefined;
          t = 0;
          i = 0;
          render();
        }
      }
      return recordRealtime({ onProgress, signal, unlocking, withAudio, origGain });
    },

    destroy() {
      api.pause();
      [...els, ...narr.map((n) => n.el)].forEach((el) => {
        el.removeAttribute('src');
        el.load();
      });
      urls.forEach((u) => URL.revokeObjectURL(u));
      holder.remove();
      renderer.destroy();
    },
  };

  // Plano B: grava a reprodução do canvas em tempo real (o vídeo leva a própria duração para ficar pronto).
  async function recordRealtime({ onProgress, signal, unlocking, withAudio, origGain }) {
    i = 0;
    t = 0;
    let ac = null;
    let dest = null;
    if (withAudio) {
      const AC = window.AudioContext || window.webkitAudioContext;
      ac = new AC();
      ac.resume();
      dest = ac.createMediaStreamDestination();
      els.forEach((el, k) => {
        const src = ac.createMediaElementSource(el);
        const g = ac.createGain();
        g.gain.value = items[k].slow ? 0 : origGain;
        src.connect(g).connect(dest);
        el.muted = false;
      });
      if (narrando()) narr.forEach((n) => ac.createMediaElementSource(n.el).connect(dest));
    }
    exporting = true;
    const mime = pickMime(withAudio);
    if (mime === null) throw new Error('Este navegador não grava vídeo. Atualize o iOS/Chrome.');
    const stream = canvas.captureStream(30);
    if (dest) dest.stream.getAudioTracks().forEach((tr) => stream.addTrack(tr));
    const rec = new MediaRecorder(stream, { ...(mime ? { mimeType: mime } : {}), videoBitsPerSecond: 8_000_000, audioBitsPerSecond: 128_000 });
    const chunks = [];
    rec.ondataavailable = (e) => e.data?.size && chunks.push(e.data);
    const stopped = new Promise((resolve) => (rec.onstop = resolve));

    await unlocking;
    els.forEach((el) => (el.muted = !withAudio));
    els[0].currentTime = items[0].inMs / 1000;
    await once(els[0], 'seeked', 1500);
    render();

    const finished = new Promise((resolve) => {
      const prevEnd = onEnd;
      onEnd = () => {
        onEnd = prevEnd;
        resolve('done');
      };
      signal?.addEventListener('abort', () => resolve('abort'));
    });
    const prevTime = onTime;
    onTime = (ms) => {
      prevTime?.(ms);
      onProgress?.(Math.min(1, ms / total));
    };

    rec.start(1000);
    playing = true;
    startEl(0);
    nextFrame(loop);

    const result = await finished;
    api.pause();
    await new Promise((r) => setTimeout(r, 150));
    rec.stop();
    await stopped;
    stream.getTracks().forEach((tr) => tr.stop());
    onTime = prevTime;
    exporting = false;
    ac?.close();
    if (result === 'abort') return null;
    return new Blob(chunks, { type: baseType(rec.mimeType || mime) });
  }

  render();
  return api;
}
