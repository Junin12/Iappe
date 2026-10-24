import { useEffect, useRef, useState } from 'preact/hooks';
import { Play, Palette, Type, Scissors, Volume2, VolumeX, Sparkles, X, Turtle, Camera as CamIcon } from 'lucide-preact';
import { go } from '../router.js';
import { updateVideo, getBlob, putBlob, delBlob, notify, DEFAULT_EDIT } from '../store.js';
import { createPlayer } from '../media/player.js';
import { LOOKS } from '../media/renderer.js';

const TABS = [
  ['visual', 'Visual', Palette],
  ['texto', 'Texto', Type],
  ['cenas', 'Cenas', Scissors],
  ['som', 'Som', Volume2],
];
const CTAS = ['Link no perfil', 'Chama no direct', 'Comenta QUERO', 'Disponível na loja'];
const AJUSTES = [
  ['brilho', 'Brilho'],
  ['contraste', 'Contraste'],
  ['saturacao', 'Cor'],
  ['calor', 'Tom quente'],
];

const clipsKey = (v) => JSON.stringify(Object.entries(v.clips || {}).map(([k, c]) => [k, c.blobId, c.inMs, c.outMs, c.speed]));

async function wakeLock() {
  try {
    return await navigator.wakeLock?.request('screen');
  } catch {
    return null;
  }
}

export function Editor({ v, peca, onDone, onRecord }) {
  const canvasRef = useRef();
  const playerRef = useRef(null);
  const [tab, setTab] = useState('visual');
  const [edit, setEdit] = useState({ ...DEFAULT_EDIT, ...v.edit, ajustes: { ...DEFAULT_EDIT.ajustes, ...(v.edit?.ajustes || {}) } });
  const [gancho, setGancho] = useState(v.gancho);
  const [time, setTime] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(null); // { progress, ctrl }
  const [total, setTotal] = useState(0);
  const saveTimer = useRef(0);
  const key = clipsKey(v);

  // (Re)cria o player quando os clipes ou cortes mudam.
  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError('');
    createPlayer({
      canvas: canvasRef.current,
      video: { ...v, edit, gancho },
      peca,
      getBlob,
      onTime: (t) => alive && setTime(t),
      onState: (p) => alive && setPlaying(p),
    })
      .then((p) => {
        if (!alive) return p.destroy();
        playerRef.current = p;
        setTotal(p.total);
        setLoading(false);
      })
      .catch((e) => alive && (setError(e.message || 'Não foi possível abrir os clipes.'), setLoading(false)));
    return () => {
      alive = false;
      playerRef.current?.destroy();
      playerRef.current = null;
    };
  }, [key]);

  const persist = (patch) => {
    clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => updateVideo(v.id, (cur) => ({ ...cur, ...patch(cur), exportBlobId: null })), 350);
  };

  const changeEdit = (patch) => {
    const next = { ...edit, ...patch };
    setEdit(next);
    playerRef.current?.setEdit(next);
    persist(() => ({ edit: next }));
  };
  const changeAjuste = (k, val) => changeEdit({ ajustes: { ...edit.ajustes, [k]: val } });
  const changeGancho = (text) => {
    setGancho(text);
    playerRef.current?.setHook(text);
    persist(() => ({ gancho: text }));
  };

  const setClip = (idx, patch) => {
    updateVideo(v.id, (cur) => ({ ...cur, clips: { ...cur.clips, [idx]: { ...cur.clips[idx], ...patch } }, exportBlobId: null }));
  };

  const timelineItems = playerRef.current?.timeline.items || [];

  const doExport = async () => {
    const player = playerRef.current;
    if (!player) return;
    clearTimeout(saveTimer.current);
    const ctrl = new AbortController();
    setExporting({ progress: 0, ctrl });
    // exportVideo começa dentro deste toque (o iPhone exige isso para liberar o som).
    const job = player.exportVideo({ onProgress: (p) => setExporting((s) => s && { ...s, progress: p }), signal: ctrl.signal });
    const lock = await wakeLock();
    try {
      const blob = await job;
      if (!blob) return notify('Exportação cancelada');
      if (blob.size < 10000) throw new Error('vazio');
      const blobId = await putBlob(blob);
      await updateVideo(v.id, (cur) => {
        if (cur.exportBlobId) delBlob(cur.exportBlobId);
        return { ...cur, edit, gancho, exportBlobId: blobId, exportMime: blob.type, status: cur.status === 'postado' ? 'postado' : 'editado' };
      });
      notify('Vídeo pronto ✨');
      onDone();
    } catch (e) {
      notify('Não consegui gerar o vídeo. Feche outros apps e tente de novo.', 'err');
    } finally {
      lock?.release?.();
      setExporting(null);
    }
  };

  if (error) {
    return (
      <div class="stack">
        <div class="card">
          <p style="margin:0 0 12px">{error}</p>
          <button class="btn gold block" onClick={onRecord}>
            <CamIcon size={18} /> Voltar para as cenas
          </button>
        </div>
      </div>
    );
  }

  return (
    <div class="stack">
      <div class="stage">
        <canvas ref={canvasRef} aria-label="Prévia do vídeo" />
        {!playing && !loading && !exporting && (
          <button class="play" onClick={() => playerRef.current?.play()} aria-label="Reproduzir prévia">
            <span>
              <Play size={30} fill="#fff" />
            </span>
          </button>
        )}
        {playing && !exporting && <button class="play" onClick={() => playerRef.current?.pause()} aria-label="Pausar prévia" />}
        {loading && (
          <div class="export-cover">
            <span class="spinner" />
          </div>
        )}
        {exporting && (
          <div class="export-cover" style="background:rgba(0,0,0,.35)">
            <div class="stack" style="width:80%">
              <b>Gerando vídeo… {Math.round(exporting.progress * 100)}%</b>
              <div class="bar">
                <i style={{ width: `${exporting.progress * 100}%` }} />
              </div>
              <span class="tiny">Mantenha esta tela aberta</span>
              <button class="btn sm" onClick={() => exporting.ctrl.abort()}>
                <X size={16} /> Cancelar
              </button>
            </div>
          </div>
        )}
      </div>

      <div class="timeline" aria-label="Linha do tempo">
        {timelineItems.map((it) => (
          <div
            class={it.slow ? 'slow' : ''}
            style={{ flex: it.outDur, backgroundImage: it.clip.thumb ? `url(${it.clip.thumb})` : undefined }}
            onClick={() => playerRef.current?.seek(it.outStart + 1)}
            title={v.cenas[it.sceneIdx]?.titulo}
          />
        ))}
        {total > 0 && <span class="playhead" style={{ left: `${(time / total) * 100}%` }} />}
      </div>
      <div class="row between tiny faint">
        <span>{(time / 1000).toFixed(1)}s</span>
        <span>{(total / 1000).toFixed(1)}s</span>
      </div>

      <div class="seg" role="tablist">
        {TABS.map(([id, label, Icon]) => (
          <button role="tab" aria-selected={tab === id} class={tab === id ? 'on' : ''} onClick={() => setTab(id)}>
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      {tab === 'visual' && (
        <div class="stack">
          <div class="looks">
            {Object.entries(LOOKS).map(([id, l]) => (
              <button class={`look ${edit.look === id ? 'on' : ''}`} onClick={() => changeEdit({ look: id })} aria-pressed={edit.look === id}>
                <i style={{ background: l.cor }} />
                {l.nome}
              </button>
            ))}
          </div>
          <details class="card tight">
            <summary style="cursor:pointer;font-weight:650">Ajuste fino</summary>
            <div class="stack" style="margin-top:12px">
              {AJUSTES.map(([k, label]) => (
                <label class="field">
                  <span class="row between">
                    {label}
                    <span class="faint">{Math.round(edit.ajustes[k] * 100)}</span>
                  </span>
                  <input class="range" type="range" min="-0.3" max="0.3" step="0.01" value={edit.ajustes[k]} onInput={(e) => changeAjuste(k, Number(e.currentTarget.value))} />
                </label>
              ))}
              <button class="btn sm" onClick={() => changeEdit({ ajustes: { ...DEFAULT_EDIT.ajustes } })}>
                Zerar ajustes
              </button>
            </div>
          </details>
        </div>
      )}

      {tab === 'texto' && (
        <div class="stack">
          <label class="field">
            <span>Gancho na tela (3 primeiros segundos)</span>
            <input class="input" value={gancho} maxLength={70} onInput={(e) => changeGancho(e.currentTarget.value)} />
          </label>
          <div class="seg" aria-label="Estilo do texto">
            {[
              ['tiktok', 'TikTok'],
              ['gold', 'Dourado'],
              ['outline', 'Contorno'],
            ].map(([id, l]) => (
              <button class={edit.hookStyle === id ? 'on' : ''} onClick={() => changeEdit({ hookStyle: id })}>
                {l}
              </button>
            ))}
          </div>
          <div class="seg" aria-label="Posição do texto">
            {[
              ['top', 'Topo'],
              ['center', 'Meio'],
              ['bottom', 'Baixo'],
            ].map(([id, l]) => (
              <button class={edit.hookPos === id ? 'on' : ''} onClick={() => changeEdit({ hookPos: id })}>
                {l}
              </button>
            ))}
          </div>
          <label class="card tight row between" style="cursor:pointer">
            <span>
              <b>Mostrar preço no final</b>
              <span class="muted small" style="display:block">Usa o preço promocional se houver</span>
            </span>
            <input type="checkbox" checked={edit.mostrarPreco} onChange={(e) => changeEdit({ mostrarPreco: e.currentTarget.checked })} style="width:24px;height:24px;accent-color:var(--gold)" />
          </label>
          <label class="field">
            <span>Chamada no final</span>
            <input class="input" value={edit.cta} maxLength={32} onInput={(e) => changeEdit({ cta: e.currentTarget.value })} placeholder="Ex.: Link no perfil" />
          </label>
          <div class="chips">
            {CTAS.map((c) => (
              <button class={`chip ${edit.cta === c ? 'on' : ''}`} onClick={() => changeEdit({ cta: c })}>
                {c}
              </button>
            ))}
          </div>
        </div>
      )}

      {tab === 'cenas' && (
        <div class="stack">
          {v.cenas.map((c, i) => {
            const clip = v.clips?.[i];
            if (!clip) return null;
            const dur = clip.durMs;
            const inS = (clip.inMs || 0) / 1000;
            const outS = (clip.outMs ?? dur) / 1000;
            return (
              <div class="card tight stack" style="gap:8px">
                <div class="row">
                  <div class="shot" style="width:40px;aspect-ratio:9/16;border-radius:8px;overflow:hidden;background:var(--card-2);flex:none">
                    {clip.thumb && <img src={clip.thumb} alt="" style="width:100%;height:100%;object-fit:cover" />}
                  </div>
                  <div class="grow">
                    <b class="ellipsis" style="display:block">
                      {i + 1}. {c.titulo}
                    </b>
                    <span class="muted small">
                      {(outS - inS).toFixed(1)}s de {(dur / 1000).toFixed(1)}s
                    </span>
                  </div>
                  <button class={`icon-btn ${clip.speed < 1 ? 'on' : ''}`} aria-label="Câmera lenta" aria-pressed={clip.speed < 1} onClick={() => setClip(i, { speed: clip.speed < 1 ? 1 : 0.5 })}>
                    <Turtle size={18} />
                  </button>
                  <button class="icon-btn" aria-label={`Regravar cena ${i + 1}`} onClick={() => go(`/camera/${v.id}/${i}`)}>
                    <CamIcon size={18} />
                  </button>
                </div>
                <label class="field">
                  <span class="tiny">Início: {inS.toFixed(1)}s</span>
                  <input class="range" type="range" min="0" max={dur / 1000} step="0.1" value={inS} onChange={(e) => setClip(i, { inMs: Math.min(Number(e.currentTarget.value) * 1000, (clip.outMs ?? dur) - 300) })} />
                </label>
                <label class="field">
                  <span class="tiny">Fim: {outS.toFixed(1)}s</span>
                  <input class="range" type="range" min="0" max={dur / 1000} step="0.1" value={outS} onChange={(e) => setClip(i, { outMs: Math.max(Number(e.currentTarget.value) * 1000, (clip.inMs || 0) + 300) })} />
                </label>
              </div>
            );
          })}
        </div>
      )}

      {tab === 'som' && (
        <div class="stack">
          <button class={`card press row ${edit.audio === 'manter' ? 'format-card on' : ''}`} onClick={() => changeEdit({ audio: 'manter' })}>
            <Volume2 size={22} class="gold" />
            <div class="grow">
              <b>Manter som original</b>
              <p class="muted small" style="margin:2px 0 0">Para unboxing ASMR e quando você narra. Cenas em câmera lenta ficam mudas.</p>
            </div>
          </button>
          <button class={`card press row ${edit.audio === 'mudo' ? 'format-card on' : ''}`} onClick={() => changeEdit({ audio: 'mudo' })}>
            <VolumeX size={22} class="gold" />
            <div class="grow">
              <b>Sem som</b>
              <p class="muted small" style="margin:2px 0 0">Recomendado: escolha um som em alta no próprio TikTok na hora de postar.</p>
            </div>
          </button>
        </div>
      )}

      <div style="position:sticky;bottom:calc(var(--safe-bottom) + 12px);margin-top:6px">
        <button class="btn gold lg block hero-cta" onClick={doExport} disabled={loading || !!exporting}>
          <Sparkles size={20} /> Gerar vídeo ({Math.round(total / 1000)}s)
        </button>
      </div>
    </div>
  );
}
