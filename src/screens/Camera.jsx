import { useEffect, useRef, useState } from 'preact/hooks';
import { X, Zap, ZapOff, Grid3x3, Timer, ImagePlus, RotateCcw, Check, Sun, Gauge } from 'lucide-preact';
import { go, back } from '../router.js';
import { videoById, updateVideo, putBlob, delBlob, notify, allClipsDone } from '../store.js';
import { pickMime, baseType } from '../media/mime.js';
import { videoThumb, videoDuration } from '../media/image.js';
import { defaultTrim } from '../logic/timeline.js';
import { haptic } from '../ui.jsx';

function beep(freq = 880, ms = 90) {
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    const ac = (beep.ac ||= new AC());
    ac.resume();
    const o = ac.createOscillator();
    const g = ac.createGain();
    o.frequency.value = freq;
    g.gain.setValueAtTime(0.15, ac.currentTime);
    g.gain.exponentialRampToValueAtTime(0.001, ac.currentTime + ms / 1000);
    o.connect(g).connect(ac.destination);
    o.start();
    o.stop(ac.currentTime + ms / 1000);
  } catch {
    /* sem áudio: só o número na tela */
  }
}

async function wakeLock() {
  try {
    return await navigator.wakeLock?.request('screen');
  } catch {
    return null;
  }
}

const CAM_ERRORS = {
  NotAllowedError: 'Sem permissão da câmera. No iPhone: Ajustes › Safari › Câmera › Permitir. No Android: toque no cadeado ao lado do endereço › Permissões.',
  NotFoundError: 'Nenhuma câmera encontrada neste aparelho.',
  NotReadableError: 'A câmera está sendo usada por outro app. Feche-o e tente de novo.',
  OverconstrainedError: 'A câmera não aceitou a configuração. Tente de novo.',
};

export async function saveClip(videoId, cenaIdx, blob, measuredMs) {
  const v = videoById(videoId);
  const cena = v.cenas[cenaIdx];
  const durMs = (await videoDuration(blob)) || measuredMs || cena.seg * 1000;
  const [blobId, thumb] = await Promise.all([putBlob(blob), videoThumb(blob)]);
  const old = v.clips?.[cenaIdx];
  if (old?.blobId) await delBlob(old.blobId);
  const next = await updateVideo(videoId, (cur) => {
    const clips = { ...cur.clips, [cenaIdx]: { blobId, durMs, mime: baseType(blob.type), thumb, speed: cena.speed || 1, ...defaultTrim(durMs, cena) } };
    const draft = { ...cur, clips, exportBlobId: null };
    if (cur.exportBlobId) delBlob(cur.exportBlobId);
    draft.status = allClipsDone(draft) ? 'gravado' : cur.status === 'ideia' ? 'roteiro' : cur.status === 'editado' ? 'gravado' : cur.status;
    return draft;
  });
  return next;
}

export function nextMissing(v, from) {
  for (let k = 1; k <= v.cenas.length; k++) {
    const idx = (from + k) % v.cenas.length;
    if (!v.clips?.[idx]) return idx;
  }
  return -1;
}

export function Camera({ videoId, cena }) {
  const v = videoById(videoId);
  const scene = v?.cenas?.[cena];
  const videoRef = useRef();
  const streamRef = useRef(null);
  const recRef = useRef(null);
  const [error, setError] = useState('');
  const [caps, setCaps] = useState({});
  const [torch, setTorch] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [exposure, setExposure] = useState(0);
  const [showExp, setShowExp] = useState(false);
  const [grid, setGrid] = useState(true);
  const [timer, setTimer] = useState(0);
  const [count, setCount] = useState(0);
  const [recording, setRecording] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [review, setReview] = useState(null); // { blob, url, ms }
  const [saving, setSaving] = useState(false);
  const [focus, setFocus] = useState(null);
  const [roll, setRoll] = useState(null);

  const target = scene ? Math.max(1, scene.seg * (scene.speed || 1)) : 3;
  const maxSec = Math.min(30, target + 6);

  // Abre a câmera traseira.
  useEffect(() => {
    if (!scene) return;
    let alive = true;
    let lock = null;
    (async () => {
      if (!navigator.mediaDevices?.getUserMedia) {
        setError('Este navegador não libera a câmera. Use o Safari (iPhone) ou o Chrome (Android), ou importe da galeria.');
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 }, frameRate: { ideal: 30 } },
          audio: { echoCancellation: false, noiseSuppression: false },
        });
        if (!alive) return stream.getTracks().forEach((t) => t.stop());
        streamRef.current = stream;
        videoRef.current.srcObject = stream;
        await videoRef.current.play().catch(() => {});
        const track = stream.getVideoTracks()[0];
        setCaps(track.getCapabilities?.() || {});
        lock = await wakeLock();
      } catch (e) {
        setError(CAM_ERRORS[e?.name] || 'Não foi possível abrir a câmera. Você pode importar o clipe da galeria.');
      }
    })();
    return () => {
      alive = false;
      recRef.current?.state === 'recording' && recRef.current.stop();
      streamRef.current?.getTracks().forEach((t) => t.stop());
      lock?.release?.();
    };
  }, [videoId, cena]);

  // Nível (horizonte) quando a câmera está de pé.
  useEffect(() => {
    const onOri = (e) => {
      if (e.beta == null) return;
      setRoll(Math.abs(e.beta) > 45 ? e.gamma : null);
    };
    window.addEventListener('deviceorientation', onOri);
    return () => window.removeEventListener('deviceorientation', onOri);
  }, []);

  const track = () => streamRef.current?.getVideoTracks()[0];
  const apply = (c) => track()?.applyConstraints({ advanced: [c] }).catch(() => {});

  const toggleTorch = () => {
    apply({ torch: !torch });
    setTorch(!torch);
    haptic();
  };
  const setZoomTo = (z) => {
    const zc = caps.zoom;
    const val = Math.max(zc.min, Math.min(zc.max, z));
    apply({ zoom: val });
    setZoom(val);
    haptic();
  };
  const setExp = (val) => {
    apply({ exposureMode: 'continuous', exposureCompensation: val });
    setExposure(val);
  };

  const tapFocus = (e) => {
    if (recording || review) return;
    const r = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    setFocus({ x: e.clientX - r.left, y: e.clientY - r.top, k: Date.now() });
    if (caps.focusMode?.includes('single-shot') || caps.focusMode?.includes('continuous')) {
      apply({ pointsOfInterest: [{ x, y }], focusMode: caps.focusMode.includes('single-shot') ? 'single-shot' : 'continuous' });
    }
    // iPhone pede permissão do sensor de nível num toque.
    if (typeof DeviceOrientationEvent !== 'undefined' && DeviceOrientationEvent.requestPermission && roll === null) {
      DeviceOrientationEvent.requestPermission().catch(() => {});
    }
  };

  const startRecording = () => {
    const stream = streamRef.current;
    if (!stream) return;
    const mime = pickMime(true);
    if (mime === null) return setError('Este navegador não grava vídeo. Atualize o sistema ou importe da galeria.');
    const rec = new MediaRecorder(stream, { ...(mime ? { mimeType: mime } : {}), videoBitsPerSecond: 10_000_000 });
    const chunks = [];
    const t0 = Date.now();
    rec.ondataavailable = (e) => e.data?.size && chunks.push(e.data);
    rec.onstop = () => {
      const blob = new Blob(chunks, { type: baseType(rec.mimeType || mime) });
      setRecording(false);
      if (blob.size < 1000) return notify('O clipe ficou vazio. Tente de novo.', 'err');
      setReview({ blob, url: URL.createObjectURL(blob), ms: Date.now() - t0 });
    };
    recRef.current = rec;
    rec.start(500);
    setRecording(true);
    setElapsed(0);
    haptic(20);
    const tick = setInterval(() => {
      const s = (Date.now() - t0) / 1000;
      setElapsed(s);
      if (rec.state !== 'recording') return clearInterval(tick);
      if (s >= maxSec) {
        clearInterval(tick);
        rec.stop();
      }
    }, 100);
  };

  const onShutter = () => {
    if (recording) {
      recRef.current?.stop();
      haptic(20);
      return;
    }
    if (count) return;
    if (!timer) return startRecording();
    let n = timer;
    setCount(n);
    beep();
    const id = setInterval(() => {
      n -= 1;
      if (n <= 0) {
        clearInterval(id);
        setCount(0);
        beep(1320, 160);
        startRecording();
      } else {
        setCount(n);
        beep();
      }
    }, 1000);
  };

  const accept = async (blob, ms) => {
    setSaving(true);
    try {
      const next = await saveClip(videoId, cena, blob, ms);
      if (review) URL.revokeObjectURL(review.url);
      setReview(null);
      const nx = nextMissing(next, cena);
      notify(`Cena ${cena + 1} salva`);
      if (nx === -1) go(`/criar/${videoId}/editar`, { replace: true });
      else go(`/camera/${videoId}/${nx}`, { replace: true });
    } catch (e) {
      notify('Não consegui salvar o clipe. Libere espaço e tente de novo.', 'err');
    } finally {
      setSaving(false);
    }
  };

  const importClip = (e) => {
    const file = e.currentTarget.files?.[0];
    e.currentTarget.value = '';
    if (file) accept(file, 0);
  };

  if (!v || !scene) {
    return (
      <div class="screen full">
        <p class="muted">Cena não encontrada.</p>
        <button class="btn" onClick={() => go('/')}>Voltar ao início</button>
      </div>
    );
  }

  const progress = Math.min(1, elapsed / target);
  const R = 39;
  const C = 2 * Math.PI * R;
  const zoomStops = caps.zoom ? [1, 2, 3].filter((z) => z >= caps.zoom.min && z <= caps.zoom.max) : [];

  return (
    <div class="cam" onClick={tapFocus}>
      <video ref={videoRef} playsInline muted autoplay aria-hidden="true" />
      {review && <video src={review.url} playsInline muted autoplay loop style="z-index:2" aria-label="Prévia do clipe gravado" />}

      {!review && (
        <div class="cam-guide">
          {grid && <div class="cam-grid" />}
          <div class="cam-noface">Zona do rosto: fora do quadro</div>
          {roll !== null && <div class={`cam-level ${Math.abs(roll) < 2 ? 'ok' : ''}`} style={{ transform: `rotate(${-roll}deg)` }} />}
          {focus && <div class="focus-ring" key={focus.k} style={{ left: focus.x + 'px', top: focus.y + 'px' }} />}
        </div>
      )}

      <div class="cam-top" onClick={(e) => e.stopPropagation()}>
        <button class="icon-btn clear" aria-label="Fechar câmera" onClick={() => back(`/criar/${videoId}/gravar`)}>
          <X size={22} />
        </button>
        <div class="cam-scene">
          <div class="tiny" style="opacity:.85;font-weight:700;letter-spacing:.08em">
            CENA {cena + 1} DE {v.cenas.length}
          </div>
          <b>{scene.titulo}</b>
        </div>
        {caps.torch ? (
          <button class={`icon-btn clear ${torch ? 'on' : ''}`} aria-label="Lanterna" aria-pressed={torch} onClick={toggleTorch}>
            {torch ? <Zap size={20} /> : <ZapOff size={20} />}
          </button>
        ) : (
          <span style="width:44px" />
        )}
      </div>

      {recording && (
        <div class="rec-time">
          ● {elapsed.toFixed(1)}s / {target.toFixed(0)}s
        </div>
      )}
      {count > 0 && (
        <div class="countdown" key={count}>
          {count}
        </div>
      )}

      {!review && !recording && (
        <div class="cam-side" onClick={(e) => e.stopPropagation()}>
          <button class={`icon-btn clear ${grid ? 'on' : ''}`} aria-label="Grade" aria-pressed={grid} onClick={() => setGrid(!grid)}>
            <Grid3x3 size={20} />
          </button>
          <button class={`icon-btn clear ${timer ? 'on' : ''}`} aria-label={`Contagem: ${timer ? timer + ' segundos' : 'desligada'}`} onClick={() => setTimer(timer === 0 ? 3 : timer === 3 ? 5 : timer === 5 ? 10 : 0)}>
            {timer ? <b style="font-size:.85rem">{timer}s</b> : <Timer size={20} />}
          </button>
          {caps.exposureCompensation && (
            <button class={`icon-btn clear ${showExp ? 'on' : ''}`} aria-label="Exposição" onClick={() => setShowExp(!showExp)}>
              <Sun size={20} />
            </button>
          )}
          <label class="icon-btn clear" aria-label="Importar clipe da galeria">
            <ImagePlus size={20} />
            <input type="file" accept="video/*" hidden onChange={importClip} />
          </label>
        </div>
      )}

      {showExp && caps.exposureCompensation && !review && !recording && (
        <div class="cam-tip" style="bottom:calc(var(--safe-bottom) + 230px)" onClick={(e) => e.stopPropagation()}>
          <label class="row small" style="gap:10px">
            <Gauge size={16} /> Exposição (abaixe se o dourado estourar)
          </label>
          <input class="range" type="range" min={caps.exposureCompensation.min} max={caps.exposureCompensation.max} step={caps.exposureCompensation.step || 0.1} value={exposure} onInput={(e) => setExp(Number(e.currentTarget.value))} aria-label="Compensação de exposição" />
        </div>
      )}

      {!review && !recording && (
        <div class="cam-tip" onClick={(e) => e.stopPropagation()}>
          <div class="row between">
            <span class="eyebrow">Plano {scene.plano}{scene.speed < 1 ? ' · câmera lenta' : ''}</span>
            <span class="tiny muted">~{target.toFixed(0)}s</span>
          </div>
          <p style="margin:6px 0 0;line-height:1.4">{scene.acao}</p>
          {scene.narracao && <p class="small" style="margin:8px 0 0;color:var(--gold-2)">🎙️ “{scene.narracao}”</p>}
        </div>
      )}

      {zoomStops.length > 1 && !review && (
        <div class="zoom-chips" onClick={(e) => e.stopPropagation()}>
          {zoomStops.map((z) => (
            <button class={Math.abs(zoom - z) < 0.05 ? 'on' : ''} onClick={() => setZoomTo(z)} aria-label={`Zoom ${z}x`}>
              {z}x
            </button>
          ))}
        </div>
      )}

      <div class="cam-bottom" onClick={(e) => e.stopPropagation()}>
        {review ? (
          <>
            <button class="btn" style="background:rgba(0,0,0,.55)" onClick={() => { URL.revokeObjectURL(review.url); setReview(null); }} disabled={saving}>
              <RotateCcw size={18} /> Regravar
            </button>
            <button class="btn gold lg" onClick={() => accept(review.blob, review.ms)} disabled={saving}>
              {saving ? <span class="spinner" /> : <Check size={20} />} Usar cena
            </button>
          </>
        ) : (
          <button class={`shutter ${recording ? 'rec' : ''}`} onClick={onShutter} aria-label={recording ? 'Parar gravação' : 'Gravar'} disabled={!!error}>
            <svg width="84" height="84" aria-hidden="true">
              <circle cx="42" cy="42" r={R} stroke="rgba(255,255,255,.85)" stroke-width="4" fill="none" />
              {recording && <circle cx="42" cy="42" r={R} stroke="#e8b84f" stroke-width="5" fill="none" stroke-dasharray={C} stroke-dashoffset={C * (1 - progress)} stroke-linecap="round" />}
            </svg>
            <span class="core" />
          </button>
        )}
      </div>

      {error && (
        <div class="export-cover" style="z-index:5" onClick={(e) => e.stopPropagation()}>
          <div class="card stack" style="max-width:420px">
            <h3>Câmera indisponível</h3>
            <p class="muted small" style="margin:0">{error}</p>
            <label class="btn gold block">
              <ImagePlus size={18} /> Importar clipe da galeria
              <input type="file" accept="video/*" hidden onChange={importClip} />
            </label>
            <button class="btn block" onClick={() => back(`/criar/${videoId}/gravar`)}>
              Voltar
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
