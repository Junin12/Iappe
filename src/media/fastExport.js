// Exportação rápida: monta o MP4 quadro a quadro com WebCodecs (mediabunny), sem depender do tempo real.
// Um vídeo de 20 s fica pronto em poucos segundos e não falha se a tela apagar ou o app for para o fundo.
// Carregado só na hora de exportar (import dinâmico). Sem WebCodecs (iPhones antigos), o player usa a gravação em tempo real.
import { Input, Output, BlobSource, BufferTarget, Mp4OutputFormat, CanvasSink, CanvasSource, AudioBufferSink, AudioBufferSource, ALL_FORMATS, canEncodeVideo, canEncodeAudio } from 'mediabunny';
import { OUT_W, OUT_H } from './renderer.js';

const FPS = 30;
const RATE = 48000;

export async function fastSupported(withAudio) {
  try {
    if (typeof VideoEncoder === 'undefined' || typeof VideoDecoder === 'undefined') return false;
    if (!(await canEncodeVideo('avc', { width: OUT_W, height: OUT_H, bitrate: 8e6, frameRate: FPS }))) return false;
    if (withAudio && (typeof AudioEncoder === 'undefined' || typeof OfflineAudioContext === 'undefined' || !(await canEncodeAudio('aac', { numberOfChannels: 2, sampleRate: RATE, bitrate: 128e3 })))) return false;
    return true;
  } catch {
    return false;
  }
}

// Mistura o som das cenas (cortado e posicionado na linha do tempo) com a narração.
async function mixAudio({ items, inputs, narr, total, origGain, signal }) {
  const ctx = new OfflineAudioContext(2, Math.ceil((total / 1000) * RATE), RATE);
  const end = total / 1000;
  for (let k = 0; k < items.length && origGain > 0; k++) {
    const it = items[k];
    if (it.slow) continue;
    const track = await inputs[k].getPrimaryAudioTrack();
    if (!track || !(await track.canDecode())) continue;
    const first = await track.getFirstTimestamp().catch(() => 0);
    const ini = first + it.inMs / 1000;
    const fim = first + it.outMs / 1000;
    const base = it.outStart / 1000;
    const stopAt = Math.min(end, base + it.outDur / 1000);
    const gain = ctx.createGain();
    gain.gain.value = origGain;
    gain.connect(ctx.destination);
    for await (const { buffer, timestamp } of new AudioBufferSink(track).buffers(ini, fim)) {
      if (signal?.aborted) return null;
      const src = ctx.createBufferSource();
      src.buffer = buffer;
      src.connect(gain);
      const offset = Math.max(0, ini - timestamp);
      const when = base + Math.max(0, timestamp - ini);
      if (when >= stopAt || offset >= buffer.duration) continue;
      src.start(when, offset);
      src.stop(stopAt);
    }
  }
  for (const n of narr) {
    const buf = await ctx.decodeAudioData(await n.blob.arrayBuffer());
    const src = ctx.createBufferSource();
    src.buffer = buf;
    src.connect(ctx.destination);
    src.start(n.start / 1000);
    src.stop(Math.min(end, (n.start + n.dur) / 1000));
  }
  return ctx.startRendering();
}

/**
 * items: linha do tempo (buildTimeline); blobs: clipe de cada item; narr: [{blob, start, dur}];
 * renderFrame(src, t): desenha o quadro no canvas (cor + gancho/preço/legenda) para o instante t (ms).
 */
export async function fastExport({ canvas, items, total, blobs, narr, withAudio, origGain, renderFrame, onProgress, signal }) {
  const inputs = blobs.map((b) => new Input({ source: new BlobSource(b), formats: ALL_FORMATS }));
  const target = new BufferTarget();
  const output = new Output({ format: new Mp4OutputFormat({ fastStart: 'in-memory' }), target });
  const video = new CanvasSource(canvas, { codec: 'avc', bitrate: 8e6, keyFrameInterval: 2 });
  output.addVideoTrack(video, { frameRate: FPS });
  const audio = withAudio ? new AudioBufferSource({ codec: 'aac', bitrate: 128e3 }) : null;
  if (audio) output.addAudioTrack(audio);

  try {
    await output.start();
    const frameMs = 1000 / FPS;
    const nTotal = Math.max(1, Math.round(total / frameMs));
    let n = 0;
    for (let k = 0; k < items.length; k++) {
      const it = items[k];
      const track = await inputs[k].getPrimaryVideoTrack();
      if (!track) throw new Error('Clipe sem vídeo');
      const first = await track.getFirstTimestamp().catch(() => 0);
      const sink = new CanvasSink(track, { width: OUT_W, height: OUT_H, fit: 'cover', poolSize: 2 });
      const count = Math.max(1, Math.round(it.outDur / frameMs));
      const stamps = Array.from({ length: count }, (_, i) => first + (it.inMs + i * frameMs * it.speed) / 1000);
      let last = null;
      let i = 0;
      for await (const wc of sink.canvasesAtTimestamps(stamps)) {
        if (signal?.aborted) {
          await output.cancel();
          return null;
        }
        if (wc) last = wc.canvas;
        const t = Math.min(total - 1, it.outStart + i * frameMs);
        if (last) renderFrame(last, t);
        await video.add(n / FPS, 1 / FPS);
        i++;
        n++;
        onProgress?.(Math.min(0.97, n / nTotal) * (withAudio ? 0.92 : 1));
      }
    }
    video.close();
    if (audio) {
      const mixed = await mixAudio({ items, inputs, narr, total, origGain, signal });
      if (!mixed) {
        await output.cancel();
        return null;
      }
      await audio.add(mixed);
      audio.close();
    }
    await output.finalize();
    onProgress?.(1);
    return new Blob([target.buffer], { type: 'video/mp4' });
  } catch (e) {
    if (output.state !== 'finalized' && output.state !== 'canceled') await output.cancel().catch(() => {});
    throw e;
  } finally {
    inputs.forEach((inp) => inp.dispose?.());
  }
}
