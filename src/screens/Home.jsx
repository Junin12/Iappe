import { useMemo, useState } from 'preact/hooks';
import { Settings, Sparkles, Flame, ChevronRight, Lightbulb, ChartColumn, CalendarDays, Download, Upload, Minus, Plus, Smartphone } from 'lucide-preact';
import { go } from '../router.js';
import { videos, pecas, settings, postadosSemana, rascunhos, pecaById, saveSettings, notify, clipsDone, allClipsDone, saveVideo, newVideoFor } from '../store.js';
import { Ring, Sheet, PecaFoto, StatusBadge, Empty } from '../ui.jsx';
import { formatById, FORMATS } from '../data/formats.js';
import { hooksFor } from '../data/copy.js';
import { streakSemanas } from '../logic/insights.js';
import { exportBackup, importBackup } from '../logic/backup.js';
import { isIOS, isStandalone, installPrompt, promptInstall } from '../install.js';
import { shareOrDownload } from '../media/share.js';

export function nextStep(v) {
  if (v.status === 'postado') return { step: 'publicar', label: 'Ver vídeo' };
  if (v.exportBlobId) return { step: 'publicar', label: 'Publicar' };
  if (allClipsDone(v)) return { step: 'editar', label: 'Editar e gerar' };
  if (clipsDone(v) > 0) return { step: 'gravar', label: `Gravar cenas (${clipsDone(v)}/${v.cenas.length})` };
  if (v.status === 'roteiro') return { step: 'gravar', label: 'Gravar' };
  return { step: 'roteiro', label: 'Ver roteiro' };
}

function greeting() {
  const h = new Date().getHours();
  return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
}

export function VideoRow({ v }) {
  const peca = pecaById(v.pecaId);
  const ns = nextStep(v);
  const pct = v.status === 'postado' ? 1 : v.exportBlobId ? 0.85 : (clipsDone(v) / Math.max(1, v.cenas.length)) * 0.7 + 0.1;
  return (
    <button class="list-item" onClick={() => go(`/criar/${v.id}/${ns.step}`)}>
      <div class="avatar">
        <PecaFoto peca={peca} />
      </div>
      <div class="grow stack" style="gap:6px">
        <div class="row between">
          <b class="ellipsis">{peca?.nome || 'Peça removida'}</b>
          <StatusBadge status={v.status} />
        </div>
        <div class="row between small muted">
          <span class="ellipsis">
            {formatById(v.formatoId).emoji} {formatById(v.formatoId).nome}
          </span>
          <span class="gold" style="flex:none">{ns.label}</span>
        </div>
        <div class="bar">
          <i style={{ width: `${Math.round(pct * 100)}%` }} />
        </div>
      </div>
    </button>
  );
}

export function Home() {
  const [openSettings, setOpenSettings] = useState(false);
  const meta = settings.value.metaSemanal;
  const feitos = postadosSemana.value;
  const streak = streakSemanas(videos.value);
  const drafts = rascunhos.value.slice(0, 4);
  const now = Date.now();
  const paraAnotar = videos.value.filter((v) => v.status === 'postado' && !v.metricas && now - (v.postadoEm || now) > 20 * 3600e3);
  const proximos = videos.value
    .filter((v) => v.agendadoPara && v.status !== 'postado' && v.agendadoPara > now - 3600e3)
    .sort((a, b) => a.agendadoPara - b.agendadoPara)
    .slice(0, 3);

  // Ideia do dia: muda uma vez por dia, usando peças reais.
  const ideia = useMemo(() => {
    const list = pecas.value;
    if (!list.length) return null;
    const dayN = Math.floor(Date.now() / 864e5);
    const peca = list[dayN % list.length];
    const formato = FORMATS[dayN % FORMATS.length];
    return { peca, formato, gancho: hooksFor(peca, 1, dayN)[0] };
  }, [pecas.value]);

  const startIdea = async () => {
    const v = { ...newVideoFor(ideia.peca, ideia.formato.id), gancho: ideia.gancho };
    await saveVideo(v);
    go(`/criar/${v.id}/roteiro`);
  };

  const msg = feitos >= meta ? 'Meta da semana batida! ✨' : feitos === 0 ? 'Bora postar o primeiro da semana?' : `Faltam ${meta - feitos} para a meta.`;

  return (
    <div class="screen">
      <div class="header">
        <div class="grow">
          <p class="muted small" style="margin:0 0 2px">{new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}</p>
          <h1>
            {greeting()}, <span class="shine">brilho</span>
          </h1>
        </div>
        <button class="icon-btn" aria-label="Ajustes" onClick={() => setOpenSettings(true)}>
          <Settings size={20} />
        </button>
      </div>

      <div class="card glow row" style="gap:16px">
        <Ring value={feitos} max={meta}>
          <div>
            <b style="font-size:1.6rem">{feitos}</b>
            <span class="muted small">/{meta}</span>
          </div>
        </Ring>
        <div class="grow stack" style="gap:6px">
          <span class="eyebrow">Meta da semana</span>
          <b style="font-size:1.05rem">{msg}</b>
          {streak > 0 && (
            <span class="row small" style="gap:6px;color:var(--warn)">
              <Flame size={16} /> {streak} {streak === 1 ? 'semana' : 'semanas'} seguidas postando
            </span>
          )}
        </div>
      </div>

      <button class="btn gold lg block hero-cta" style="margin-top:16px" onClick={() => go('/criar')}>
        <Sparkles size={20} /> Criar vídeo
      </button>

      {paraAnotar.length > 0 && (
        <button class="card press row" style="margin-top:14px;border-color:rgba(255,92,138,.35)" onClick={() => go('/resultados')}>
          <ChartColumn size={22} color="var(--rose)" />
          <div class="grow">
            <b>Anote os números de {paraAnotar.length} {paraAnotar.length === 1 ? 'vídeo' : 'vídeos'}</b>
            <p class="muted small" style="margin:2px 0 0">Views, curtidas e vendas: é assim que o app descobre o que funciona.</p>
          </div>
          <ChevronRight size={18} class="faint" />
        </button>
      )}

      {drafts.length > 0 && (
        <>
          <div class="section-title">
            <h2>Continue de onde parou</h2>
          </div>
          <div class="stack">
            {drafts.map((v) => (
              <VideoRow v={v} key={v.id} />
            ))}
          </div>
        </>
      )}

      {proximos.length > 0 && (
        <>
          <div class="section-title">
            <h2>Próximos posts</h2>
            <a href="#/agenda" class="small">Agenda</a>
          </div>
          <div class="stack">
            {proximos.map((v) => (
              <button class="list-item" onClick={() => go(`/criar/${v.id}/${nextStep(v).step}`)}>
                <CalendarDays size={20} class="gold" />
                <div class="grow">
                  <b class="ellipsis">{pecaById(v.pecaId)?.nome}</b>
                  <p class="muted small" style="margin:2px 0 0">
                    {new Date(v.agendadoPara).toLocaleString('pt-BR', { weekday: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
                <StatusBadge status={v.status} />
              </button>
            ))}
          </div>
        </>
      )}

      <div class="section-title">
        <h2>Ideia do dia</h2>
      </div>
      {ideia ? (
        <div class="card stack">
          <div class="row">
            <div class="avatar">
              <PecaFoto peca={ideia.peca} />
            </div>
            <div class="grow">
              <span class="eyebrow">
                {ideia.formato.emoji} {ideia.formato.nome}
              </span>
              <p style="margin:4px 0 0;font-family:var(--serif);font-size:1.15rem;line-height:1.3">“{ideia.gancho}”</p>
            </div>
          </div>
          <button class="btn block" onClick={startIdea}>
            <Lightbulb size={18} class="gold" /> Gravar essa ideia
          </button>
        </div>
      ) : (
        <Empty title="Cadastre sua primeira peça" text="Com as peças cadastradas, o app sugere ideias de vídeo todo dia." action={<button class="btn gold" onClick={() => go('/pecas/nova')}>Cadastrar peça</button>} />
      )}

      <Sheet open={openSettings} onClose={() => setOpenSettings(false)} title="Ajustes">
        <SettingsBody />
      </Sheet>
    </div>
  );
}

function SettingsBody() {
  const meta = settings.value.metaSemanal;
  const setMeta = (n) => saveSettings({ metaSemanal: Math.max(1, Math.min(14, n)) });

  const doExport = async () => {
    try {
      const file = await exportBackup();
      await shareOrDownload(file, 'Backup do GlowStudio');
      notify('Backup pronto');
    } catch (e) {
      notify('Não foi possível gerar o backup', 'err');
    }
  };

  const doImport = async (e) => {
    const file = e.currentTarget.files?.[0];
    e.currentTarget.value = '';
    if (!file) return;
    try {
      const n = await importBackup(file);
      notify(`${n} peças restauradas`);
    } catch (err) {
      notify(err.message || 'Backup inválido', 'err');
    }
  };

  return (
    <div class="stack lg">
      <div class="card row between">
        <div>
          <b>Meta semanal</b>
          <p class="muted small" style="margin:2px 0 0">Vídeos postados por semana</p>
        </div>
        <div class="row">
          <button class="icon-btn" aria-label="Diminuir meta" onClick={() => setMeta(meta - 1)}>
            <Minus size={18} />
          </button>
          <b style="font-size:1.4rem;min-width:28px;text-align:center">{meta}</b>
          <button class="icon-btn" aria-label="Aumentar meta" onClick={() => setMeta(meta + 1)}>
            <Plus size={18} />
          </button>
        </div>
      </div>

      <div class="stack">
        <h3>Seus dados</h3>
        <p class="muted small" style="margin:0">
          Tudo fica guardado só neste aparelho. Faça um backup de vez em quando (peças, fotos e roteiros).
        </p>
        <button class="btn block" onClick={doExport}>
          <Download size={18} /> Fazer backup
        </button>
        <label class="btn block">
          <Upload size={18} /> Restaurar backup
          <input type="file" accept="application/json,.json" hidden onChange={doImport} />
        </label>
      </div>

      {!isStandalone() && (
        <div class="pill-tip">
          <Smartphone size={20} class="gold" style="flex:none" />
          <div>
            {isIOS() ? (
              <>No Safari, toque em <b>Compartilhar</b> e depois em <b>Adicionar à Tela de Início</b>.</>
            ) : installPrompt.value ? (
              <button class="btn sm gold" onClick={promptInstall}>Instalar o app</button>
            ) : (
              <>No Chrome, use o menu ⋮ → <b>Instalar app</b>.</>
            )}
          </div>
        </div>
      )}
      <p class="faint tiny center" style="margin:0">GlowStudio 2.0</p>
    </div>
  );
}
