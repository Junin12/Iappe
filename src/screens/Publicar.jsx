import { useEffect, useState } from 'preact/hooks';
import { Share2, Copy, CalendarPlus, CircleCheck, Pencil, ChartColumn, Download, Info, Repeat2 } from 'lucide-preact';
import { go } from '../router.js';
import { updateVideo, getBlob, notify, freeSpace } from '../store.js';
import { Sheet } from '../ui.jsx';
import { shareOrDownload, copyText } from '../media/share.js';
import { extFor } from '../media/mime.js';
import { ScheduleForm } from './Agenda.jsx';
import { MetricsForm } from './Resultados.jsx';
import { captionsPara } from '../data/copy.js';

const slug = (s) => (s || 'video').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').toLowerCase().slice(0, 30);

export function Publicar({ v, peca, onEdit }) {
  const [url, setUrl] = useState(null);
  const [file, setFile] = useState(null);
  const [legenda, setLegenda] = useState(v.legenda || '');
  const [hashtags, setHashtags] = useState(v.hashtags || '');
  const [scheduling, setScheduling] = useState(false);
  const [metrics, setMetrics] = useState(false);

  useEffect(() => {
    let u;
    getBlob(v.exportBlobId).then((b) => {
      if (!b) return;
      const name = `glowstudio-${slug(peca?.nome)}-${new Date().toISOString().slice(0, 10)}.${extFor(b.type)}`;
      setFile(new File([b], name, { type: b.type || 'video/mp4' }));
      u = URL.createObjectURL(b);
      setUrl(u);
    });
    return () => u && URL.revokeObjectURL(u);
  }, [v.exportBlobId]);

  const texto = `${legenda.trim()}\n\n${hashtags.trim()}`.trim();

  const saveText = () => {
    if (legenda !== v.legenda || hashtags !== v.hashtags) updateVideo(v.id, { legenda, hashtags });
  };

  const share = async () => {
    if (!file) return;
    saveText();
    copyText(texto); // sem await: o compartilhamento precisa acontecer ainda dentro do toque
    const r = await shareOrDownload(file, peca?.nome || 'GlowStudio', texto);
    if (r === 'shared') notify('Legenda copiada: é só colar no TikTok');
    else if (r === 'downloaded') notify('Vídeo baixado. Legenda copiada!');
  };

  // Mesmo vídeo no Reels e no Shorts: copia a legenda adaptada e abre o menu de compartilhar.
  const shareOn = async (rede) => {
    if (!file) return;
    saveText();
    const txt = captionsPara({ ...v, legenda, hashtags })[rede];
    copyText(txt);
    const r = await shareOrDownload(file, peca?.nome || 'GlowStudio', txt);
    if (r !== 'cancelled') notify(`Legenda do ${rede === 'reels' ? 'Reels' : 'Shorts'} copiada`);
  };

  const posted = async () => {
    saveText();
    await updateVideo(v.id, { status: 'postado', postadoEm: Date.now() });
    notify('Postado! Volte amanhã para anotar os números 📈');
  };

  if (!v.exportBlobId && v.status === 'postado') {
    return (
      <div class="card glow stack">
        <div class="row">
          <CircleCheck size={22} color="var(--ok)" />
          <b class="grow">Postado em {new Date(v.postadoEm).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long' })}</b>
        </div>
        <p class="muted small" style="margin:0">Os clipes deste vídeo já foram apagados para liberar espaço.</p>
        <button class="btn gold block" onClick={() => setMetrics(true)}>
          <ChartColumn size={18} /> {v.metricas ? 'Atualizar números' : 'Anotar números do vídeo'}
        </button>
        <Sheet open={metrics} onClose={() => setMetrics(false)} title="Números do vídeo">
          <MetricsForm video={v} onDone={() => setMetrics(false)} />
        </Sheet>
      </div>
    );
  }

  if (!v.exportBlobId) {
    return (
      <div class="card stack">
        <p style="margin:0">O vídeo ainda não foi gerado (ou foi alterado depois).</p>
        <button class="btn gold block" onClick={onEdit}>
          <Pencil size={18} /> Ir para o editor
        </button>
      </div>
    );
  }

  return (
    <div class="stack lg">
      <div class="stage" style="width:min(100%, calc(48dvh * 9 / 16))">
        {url ? <video src={url} controls playsInline loop style="width:100%;height:100%;object-fit:cover" aria-label="Vídeo final" /> : <div class="export-cover"><span class="spinner" /></div>}
      </div>

      <div class="stack">
        <button class="btn rose lg block" onClick={share} disabled={!file}>
          <Share2 size={20} /> Compartilhar no TikTok
        </button>
        <p class="muted tiny center" style="margin:0">
          No menu, escolha TikTok, ou <b>Salvar vídeo</b> para guardar na galeria. A legenda é copiada automaticamente.
        </p>
      </div>

      <div class="card tight stack" style="gap:10px">
        <span class="row small" style="gap:6px">
          <Repeat2 size={16} class="gold" /> <b>Poste também em outras redes</b>
        </span>
        <div class="grid2">
          <button class="btn sm" onClick={() => shareOn('reels')} disabled={!file}>
            Instagram Reels
          </button>
          <button class="btn sm" onClick={() => shareOn('shorts')} disabled={!file}>
            YouTube Shorts
          </button>
        </div>
        <span class="tiny faint">Um vídeo, três posts. A legenda já vem adaptada para cada rede.</span>
      </div>

      <div class="stack">
        <label class="field">
          <span class="row between">
            Legenda
            <span class="faint">{legenda.length}/150</span>
          </span>
          <textarea class="textarea" value={legenda} maxLength={150} onInput={(e) => setLegenda(e.currentTarget.value)} onBlur={saveText} />
        </label>
        <label class="field">
          <span>Hashtags</span>
          <input class="input" value={hashtags} onInput={(e) => setHashtags(e.currentTarget.value)} onBlur={saveText} />
        </label>
        <button
          class="btn block"
          onClick={async () => {
            saveText();
            (await copyText(texto)) ? notify('Legenda e hashtags copiadas') : notify('Não consegui copiar', 'err');
          }}
        >
          <Copy size={18} /> Copiar legenda + hashtags
        </button>
      </div>

      {v.status === 'postado' ? (
        <div class="card glow stack">
          <div class="row">
            <CircleCheck size={22} color="var(--ok)" />
            <b class="grow">Postado em {new Date(v.postadoEm).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long' })}</b>
          </div>
          <button class="btn gold block" onClick={() => setMetrics(true)}>
            <ChartColumn size={18} /> {v.metricas ? 'Atualizar números' : 'Anotar números do vídeo'}
          </button>
        </div>
      ) : (
        <div class="grid2">
          <button class="btn" onClick={() => setScheduling(true)}>
            <CalendarPlus size={18} /> {v.agendadoPara ? new Date(v.agendadoPara).toLocaleString('pt-BR', { weekday: 'short', hour: '2-digit', minute: '2-digit' }) : 'Agendar'}
          </button>
          <button class="btn gold" onClick={posted}>
            <CircleCheck size={18} /> Já postei
          </button>
        </div>
      )}

      <div class="row" style="gap:8px">
        <button class="btn ghost grow" onClick={onEdit}>
          <Pencil size={16} /> Editar de novo
        </button>
        <button class="btn ghost grow" onClick={() => file && shareOrDownload(file, 'GlowStudio')}>
          <Download size={16} /> Salvar arquivo
        </button>
      </div>

      {v.status === 'postado' && (
        <div class="pill-tip">
          <Info size={18} class="gold" style="flex:none" />
          <div class="stack" style="gap:8px">
            <span class="small">Já salvou o vídeo na galeria? Libere espaço apagando os clipes deste vídeo. O roteiro e os números continuam guardados.</span>
            <button
              class="btn sm"
              onClick={async () => {
                await freeSpace(v.id);
                notify('Espaço liberado');
                go('/resultados');
              }}
            >
              Liberar espaço
            </button>
          </div>
        </div>
      )}

      <button class="btn ghost block" onClick={() => go('/')}>
        Voltar ao início
      </button>

      <Sheet open={scheduling} onClose={() => setScheduling(false)} title="Quando postar?">
        <ScheduleForm video={{ ...v, legenda, hashtags }} onDone={() => setScheduling(false)} />
      </Sheet>
      <Sheet open={metrics} onClose={() => setMetrics(false)} title="Números do vídeo">
        <MetricsForm video={v} onDone={() => setMetrics(false)} />
      </Sheet>
    </div>
  );
}
