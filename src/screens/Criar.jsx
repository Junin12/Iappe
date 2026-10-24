import { useEffect, useState } from 'preact/hooks';
import { ChevronLeft, ChevronRight, Plus, WandSparkles, RotateCcw, Camera as CamIcon, Check, Trash2, Lightbulb, Clapperboard, ImagePlus, BookmarkPlus } from 'lucide-preact';
import { go, back } from '../router.js';
import { pecas, pecaById, videoById, saveVideo, updateVideo, newVideoFor, deleteVideo, notify, clipsDone, allClipsDone, delBlob, ganchos, saveGancho } from '../store.js';
import { Sheet, PecaFoto, Price, Steps, Empty } from '../ui.jsx';
import { FORMATS, formatById, totalSeconds } from '../data/formats.js';
import { hooksFor, captionFor, hashtagsFor } from '../data/copy.js';
import { aiAvailable, aiScript } from '../ai.js';
import { PecaForm } from './Pecas.jsx';
import { Editor } from './Editor.jsx';
import { Publicar } from './Publicar.jsx';
import { saveClip, nextMissing } from './Camera.jsx';

const STEPS = ['formato', 'roteiro', 'gravar', 'editar', 'publicar'];
const TITLES = { formato: 'Escolha o formato', roteiro: 'Gancho e roteiro', gravar: 'Grave as cenas', editar: 'Edite o vídeo', publicar: 'Publicar' };

export function Criar({ videoId, step }) {
  if (!videoId) return <EscolherPeca />;
  const v = videoById(videoId);
  if (!v) {
    return (
      <div class="screen full">
        <Empty title="Rascunho não encontrado" action={<button class="btn" onClick={() => go('/')}>Início</button>} />
      </div>
    );
  }
  const s = STEPS.includes(step) ? step : 'formato';
  return <Wizard v={v} step={s} />;
}

function EscolherPeca() {
  const [adding, setAdding] = useState(false);
  const start = async (peca) => {
    const v = newVideoFor(peca);
    await saveVideo(v);
    go(`/criar/${v.id}/formato`);
  };
  return (
    <div class="screen">
      <div class="header">
        <div class="grow">
          <span class="eyebrow">Novo vídeo</span>
          <h1>Qual peça vai brilhar?</h1>
        </div>
      </div>
      <button class="card press row" style="margin-bottom:14px" onClick={() => setAdding(true)}>
        <div class="avatar" style="background:rgba(232,184,79,.12);color:var(--gold)">
          <Plus size={24} />
        </div>
        <div class="grow">
          <b>Cadastrar peça nova</b>
          <p class="muted small" style="margin:2px 0 0">Foto, nome e preço em menos de 1 minuto</p>
        </div>
        <ChevronRight size={18} class="faint" />
      </button>
      {pecas.value.length === 0 ? (
        <p class="muted small center">Cadastre uma peça para começar.</p>
      ) : (
        <div class="peca-grid">
          {pecas.value.map((p) => (
            <button class="peca-card" onClick={() => start(p)} key={p.id}>
              <div class="thumb">
                <PecaFoto peca={p} size={32} />
              </div>
              <div class="info">
                <b class="ellipsis" style="display:block">{p.nome}</b>
                <Price peca={p} />
              </div>
            </button>
          ))}
        </div>
      )}
      <Sheet open={adding} onClose={() => setAdding(false)} title="Nova peça">
        <PecaForm
          initial={{}}
          onDone={(p) => {
            setAdding(false);
            start(p);
          }}
        />
      </Sheet>
    </div>
  );
}

function Wizard({ v, step }) {
  const peca = pecaById(v.pecaId);
  const idx = STEPS.indexOf(step);
  const [confirmDel, setConfirmDel] = useState(false);
  const goStep = (s) => go(`/criar/${v.id}/${s}`, { replace: true });

  return (
    <div class="screen full">
      <div class="header" style="margin-bottom:8px">
        <button class="icon-btn" aria-label="Voltar" onClick={() => (idx > 0 ? goStep(STEPS[idx - 1]) : back('/'))}>
          <ChevronLeft size={22} />
        </button>
        <div class="grow">
          <span class="eyebrow ellipsis" style="display:block">{peca?.nome || 'Vídeo'}</span>
          <h2>{TITLES[step]}</h2>
        </div>
        <button class="icon-btn" aria-label="Excluir rascunho" onClick={() => setConfirmDel(true)}>
          <Trash2 size={18} />
        </button>
      </div>
      <Steps total={STEPS.length} current={idx} />

      {step === 'formato' && <StepFormato v={v} onNext={() => goStep('roteiro')} />}
      {step === 'roteiro' && <StepRoteiro v={v} peca={peca} onNext={() => goStep('gravar')} />}
      {step === 'gravar' && <StepGravar v={v} onNext={() => goStep('editar')} />}
      {step === 'editar' && <Editor v={v} peca={peca} onDone={() => goStep('publicar')} onRecord={() => goStep('gravar')} />}
      {step === 'publicar' && <Publicar v={v} peca={peca} onEdit={() => goStep('editar')} />}

      <Sheet open={confirmDel} onClose={() => setConfirmDel(false)} title="Excluir este vídeo?">
        <p class="muted">O roteiro e os clipes gravados serão apagados deste aparelho.</p>
        <div class="grid2" style="margin-top:16px">
          <button class="btn" onClick={() => setConfirmDel(false)}>Cancelar</button>
          <button
            class="btn rose"
            onClick={async () => {
              await deleteVideo(v.id);
              notify('Vídeo excluído');
              go('/', { replace: true });
            }}
          >
            Excluir
          </button>
        </div>
      </Sheet>
    </div>
  );
}

function StepFormato({ v, onNext }) {
  const [sel, setSel] = useState(v.formatoId);
  const hasClips = clipsDone(v) > 0;

  const confirm = async () => {
    if (sel !== v.formatoId) {
      if (hasClips) for (const c of Object.values(v.clips)) await delBlob(c.blobId);
      const f = formatById(sel);
      await updateVideo(v.id, { formatoId: sel, cenas: f.cenas.map((c) => ({ ...c })), clips: {}, exportBlobId: null, status: 'ideia' });
    }
    onNext();
  };

  return (
    <div class="stack">
      <p class="muted small" style="margin:0 0 4px">Formatos sem rosto que funcionam para semijoias. Cada um vem com as cenas prontas.</p>
      {FORMATS.map((f) => (
        <button class={`card press format-card ${sel === f.id ? 'on' : ''}`} onClick={() => setSel(f.id)} aria-pressed={sel === f.id}>
          <span class="emoji" aria-hidden="true">{f.emoji}</span>
          <div>
            <b>{f.nome}</b>
            <p class="muted small" style="margin:3px 0 0;line-height:1.35">{f.resumo}</p>
          </div>
          <span class="tiny faint" style="text-align:right">
            {f.cenas.length} cenas
            <br />~{totalSeconds(f)}s
          </span>
        </button>
      ))}
      {hasClips && sel !== v.formatoId && <p class="small" style="color:var(--warn);margin:0">Trocar o formato apaga as cenas já gravadas deste vídeo.</p>}
      <div style="position:sticky;bottom:calc(var(--safe-bottom) + 12px);margin-top:8px">
        <button class="btn gold lg block" onClick={confirm}>
          Continuar <ChevronRight size={18} />
        </button>
      </div>
    </div>
  );
}

function StepRoteiro({ v, peca, onNext }) {
  const [seed, setSeed] = useState(0);
  const [aiHooks, setAiHooks] = useState([]);
  const [aiOn, setAiOn] = useState(false);
  const [busy, setBusy] = useState(false);
  const [gancho, setGancho] = useState(v.gancho);
  const [cenas, setCenas] = useState(v.cenas);

  useEffect(() => {
    aiAvailable().then(setAiOn);
  }, []);

  const options = [...aiHooks, ...hooksFor(peca, 4, seed)].filter((h, i, a) => a.indexOf(h) === i);
  const formato = formatById(v.formatoId);

  const gerarIA = async () => {
    setBusy(true);
    try {
      const r = await aiScript(peca, { ...formato, cenas });
      if (Array.isArray(r.ganchos) && r.ganchos.length) {
        setAiHooks(r.ganchos.slice(0, 3).map(String));
        setGancho(String(r.ganchos[0]));
      }
      if (Array.isArray(r.cenas)) {
        setCenas(cenas.map((c, i) => ({ ...c, acao: r.cenas[i]?.acao || c.acao, narracao: r.cenas[i]?.narracao || c.narracao || '' })));
      }
      await updateVideo(v.id, {
        ...(r.legenda ? { legenda: String(r.legenda).slice(0, 150) } : {}),
        ...(r.hashtags ? { hashtags: String(r.hashtags) } : {}),
      });
      notify('Roteiro gerado com IA ✨');
    } catch {
      notify('A IA não respondeu agora. Use os ganchos prontos.', 'err');
    } finally {
      setBusy(false);
    }
  };

  const next = async () => {
    const legendaPadrao = captionFor(peca, v.gancho);
    await updateVideo(v.id, (cur) => ({
      ...cur,
      gancho: gancho.trim(),
      cenas,
      // Atualiza a legenda se ela ainda era a automática do gancho anterior.
      legenda: cur.legenda === legendaPadrao ? captionFor(peca, gancho.trim()) : cur.legenda,
      hashtags: cur.hashtags || hashtagsFor(peca),
      status: cur.status === 'ideia' ? 'roteiro' : cur.status,
      exportBlobId: cur.exportBlobId && gancho.trim() === cur.gancho ? cur.exportBlobId : null,
    }));
    onNext();
  };

  return (
    <div class="stack lg">
      <div class="stack">
        <div class="row between">
          <h3>Gancho (primeiros 3 segundos)</h3>
          <button class="btn sm ghost" onClick={() => setSeed(seed + 4)}>
            <RotateCcw size={15} /> Outros
          </button>
        </div>
        {options.map((h) => (
          <button class={`card press hook-option ${gancho === h ? 'on' : ''}`} onClick={() => setGancho(h)} aria-pressed={gancho === h}>
            <span style="font-family:var(--serif);font-size:1.08rem;line-height:1.3">“{h}”</span>
            {aiHooks.includes(h) && <span class="badge" style="margin-left:8px;color:var(--rose)">IA</span>}
          </button>
        ))}
        {ganchos.value.length > 0 && (
          <div class="field">
            <span>Seus ganchos salvos</span>
            <div class="chips">
              {ganchos.value.map((g) => (
                <button class={`chip ${gancho === g.texto ? 'on' : ''}`} aria-pressed={gancho === g.texto} onClick={() => setGancho(g.texto)}>
                  {g.texto}
                </button>
              ))}
            </div>
          </div>
        )}
        <label class="field">
          <span>Ou escreva o seu</span>
          <div class="row" style="gap:8px">
            <input class="input" value={gancho} maxLength={70} onInput={(e) => setGancho(e.currentTarget.value)} />
            <button
              type="button"
              class="icon-btn"
              style="width:50px;height:50px"
              aria-label="Salvar este gancho"
              disabled={!gancho.trim()}
              onClick={async () => notify((await saveGancho(gancho)) ? 'Gancho salvo nos seus ganchos' : 'Esse gancho já está salvo')}
            >
              <BookmarkPlus size={18} />
            </button>
          </div>
        </label>
        {aiOn && (
          <button class="btn rose block" onClick={gerarIA} disabled={busy}>
            {busy ? <span class="spinner" /> : <WandSparkles size={18} />} {busy ? 'Criando roteiro…' : 'Criar ganchos e falas com IA'}
          </button>
        )}
      </div>

      <div class="stack">
        <h3>Cenas</h3>
        {cenas.map((c, i) => (
          <div class="card tight stack" style="gap:8px">
            <div class="row between">
              <b>
                {i + 1}. {c.titulo}
              </b>
              <span class="tiny faint">
                {c.seg}s{c.speed < 1 ? ' · lenta' : ''}
              </span>
            </div>
            <p class="muted small" style="margin:0;line-height:1.4">{c.acao}</p>
            <input
              class="input"
              style="min-height:42px;font-size:.9rem"
              placeholder="Fala para ler na câmera (opcional)"
              value={c.narracao || ''}
              maxLength={160}
              onInput={(e) => setCenas(cenas.map((x, k) => (k === i ? { ...x, narracao: e.currentTarget.value } : x)))}
              aria-label={`Fala da cena ${i + 1}`}
            />
          </div>
        ))}
      </div>

      <div style="position:sticky;bottom:calc(var(--safe-bottom) + 12px)">
        <button class="btn gold lg block" onClick={next} disabled={!gancho.trim()}>
          Ir gravar <ChevronRight size={18} />
        </button>
      </div>
    </div>
  );
}

function StepGravar({ v, onNext }) {
  const done = clipsDone(v);
  const all = allClipsDone(v);
  const nx = nextMissing(v, -1);
  const [busy, setBusy] = useState(false);

  const removeClip = async (i) => {
    const c = v.clips[i];
    if (c) await delBlob(c.blobId);
    await updateVideo(v.id, (cur) => {
      const clips = { ...cur.clips };
      delete clips[i];
      return { ...cur, clips, exportBlobId: null, status: cur.status === 'gravado' || cur.status === 'editado' ? 'roteiro' : cur.status };
    });
  };

  const importMany = async (e) => {
    const files = [...(e.currentTarget.files || [])];
    e.currentTarget.value = '';
    if (!files.length) return;
    setBusy(true);
    try {
      let cur = v;
      for (const file of files) {
        const idx = nextMissing(cur, -1);
        if (idx === -1) break;
        cur = await saveClip(v.id, idx, file, 0);
      }
      notify('Clipes importados');
    } catch {
      notify('Não consegui importar algum clipe', 'err');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div class="stack lg">
      <div class="pill-tip">
        <Lightbulb size={18} class="gold" style="flex:none" />
        <span>Luz de janela de lado e um fundo escuro (veludo, tecido) fazem o banho brilhar. Toque na cena para gravar.</span>
      </div>

      <div class="scene-list">
        {v.cenas.map((c, i) => {
          const clip = v.clips?.[i];
          return (
            <div class={`scene ${clip ? 'done' : ''}`}>
              <button class="shot" style="border:0;padding:0;cursor:pointer" onClick={() => go(`/camera/${v.id}/${i}`)} aria-label={`Gravar cena ${i + 1}`}>
                {clip?.thumb ? <img src={clip.thumb} alt="" /> : i + 1}
              </button>
              <button style="background:none;border:0;text-align:left;padding:0;cursor:pointer;min-width:0" onClick={() => go(`/camera/${v.id}/${i}`)}>
                <b class="ellipsis" style="display:block">{c.titulo}</b>
                <span class="muted small">{clip ? `${(clip.durMs / 1000).toFixed(1)}s gravados` : `${c.seg}s · ${c.plano}${c.speed < 1 ? ' · lenta' : ''}`}</span>
              </button>
              {clip ? (
                <button class="icon-btn" aria-label={`Apagar clipe da cena ${i + 1}`} onClick={() => removeClip(i)}>
                  <RotateCcw size={16} />
                </button>
              ) : (
                <button class="icon-btn" aria-label={`Gravar cena ${i + 1}`} onClick={() => go(`/camera/${v.id}/${i}`)}>
                  <CamIcon size={18} />
                </button>
              )}
            </div>
          );
        })}
      </div>

      <div class="stack" style="position:sticky;bottom:calc(var(--safe-bottom) + 12px)">
        {all ? (
          <button class="btn gold lg block" onClick={onNext}>
            <Clapperboard size={20} /> Montar o vídeo
          </button>
        ) : (
          <button class="btn gold lg block" onClick={() => go(`/camera/${v.id}/${nx}`)}>
            <CamIcon size={20} /> {done ? `Gravar cena ${nx + 1}` : 'Começar a gravar'}
          </button>
        )}
        {!all && (
          <label class="btn block">
            {busy ? <span class="spinner" /> : <ImagePlus size={18} />} Importar clipes da galeria
            <input type="file" accept="video/*" multiple hidden onChange={importMany} disabled={busy} />
          </label>
        )}
        {done > 0 && !all && (
          <button class="btn ghost block" onClick={onNext}>
            Montar só com {done} {done === 1 ? 'cena' : 'cenas'}
          </button>
        )}
      </div>
    </div>
  );
}
