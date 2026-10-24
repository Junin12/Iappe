// Toca as cenas em sequência num canvas (prévia) e grava esse canvas em MP4 (exportação).
import { buildTimeline, overlayAt, itemAt } from '../logic/timeline.js';
import { createRenderer, finalParams, drawHook, drawEnd, ensureFonts } from './renderer.js';
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

export async function createPlayer({ canvas, video, peca, getBlob, onTime, onEnd, onState }) {
  const timeline = buildTimeline(video);
  const { items, total } = timeline;
  if (!items.length) throw new Error('Grave pelo menos uma cena para editar.');

  const renderer = createRenderer(canvas);
  await ensureFonts();

  // Vídeos ficam no DOM (escondidos): o Safari só decodifica quadros de vídeos anexados.
  const holder = document.createElement('div');
  holder.style.cssText = 'position:fixed;left:0;top:0;width:2px;height:2px;opacity:0;pointer-events:none;overflow:hidden';
  document.body.appendChild(holder);

  const urls = [];
  const els = [];
  for (const it of items) {
    const blob = await getBlob(it.clip.blobId);
    if (!blob) throw new Error('Um dos clipes sumiu do aparelho. Grave a cena de novo.');
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
  let lastKind = undefined;
  let i = 0;
  let playing = false;
  let exporting = false;
  let frameToken = 0;
  let t = 0;
  let stallSince = 0;
  let lastCt = -1;

  const buildOverlays = () => {
    const preco = precoFinal(peca);
    const old = peca?.precoPromo && peca.precoPromo < peca.preco ? brl(peca.preco) : null;
    overlays = {
      hook: drawHook(video.gancho, edit.hookStyle, edit.hookPos),
      end: drawEnd({ price: preco ? brl(preco) : '', oldPrice: old, cta: edit.cta, showPrice: edit.mostrarPreco }),
    };
    lastKind = undefined;
  };
  buildOverlays();

  const applyAudio = (k) => {
    if (exporting) return;
    els[k].muted = edit.audio === 'mudo' || items[k].slow;
  };

  const render = () => {
    const kind = overlayAt(t, timeline, edit);
    if (kind !== lastKind) {
      renderer.setOverlay(kind ? overlays[kind] : null);
      lastKind = kind;
    }
    renderer.draw(els[i], params);
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
  // a reprodução/exportação continua a ~25 qps em vez de travar.
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
      onTime?.(t);
    }
    nextFrame(loop);
  };

  // Precisa rodar dentro do toque: libera o play() com som de todos os clipes no iPhone.
  const unlock = (all = false) =>
    Promise.all(
      els.map((el, k) => {
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
    );

  const api = {
    timeline,
    total,
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
      if (!playing) render();
    },
    setHook(text) {
      video = { ...video, gancho: text };
      buildOverlays();
      if (!playing) render();
    },
    redraw: () => render(),

    // Grava a reprodução do canvas em tempo real (o vídeo leva a própria duração para ficar pronto).
    async exportVideo({ onProgress, signal }) {
      api.pause();
      i = 0;
      t = 0;
      const unlocking = unlock(true);
      const withAudio = edit.audio !== 'mudo';
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
          g.gain.value = items[k].slow ? 0 : 1;
          src.connect(g).connect(dest);
          el.muted = false;
        });
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
    },

    destroy() {
      api.pause();
      els.forEach((el) => {
        el.removeAttribute('src');
        el.load();
      });
      urls.forEach((u) => URL.revokeObjectURL(u));
      holder.remove();
      renderer.destroy();
    },
  };

  render();
  return api;
}
