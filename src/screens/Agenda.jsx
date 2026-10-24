import { useState } from 'preact/hooks';
import { ChevronLeft, ChevronRight, CalendarPlus, Clock, CircleCheck } from 'lucide-preact';
import { go } from '../router.js';
import { videos, pecaById, updateVideo, startOfWeek, notify } from '../store.js';
import { Sheet, StatusBadge, PecaFoto, Empty } from '../ui.jsx';
import { formatById } from '../data/formats.js';
import { icsFile, shareOrDownload } from '../media/share.js';
import { nextStep } from './Home.jsx';

const DIAS = ['seg', 'ter', 'qua', 'qui', 'sex', 'sáb', 'dom'];
const sameDay = (a, b) => a.toDateString() === b.toDateString();
const dayOf = (v) => (v.status === 'postado' ? v.postadoEm : v.agendadoPara);

export function toLocalInput(ms) {
  const d = new Date(ms);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

export function ScheduleForm({ video, onDone }) {
  const peca = pecaById(video.pecaId);
  const sugestao = (() => {
    const d = new Date();
    d.setDate(d.getDate() + (d.getHours() >= 19 ? 1 : 0));
    d.setHours(19, 0, 0, 0);
    return d.getTime();
  })();
  const [when, setWhen] = useState(toLocalInput(video.agendadoPara || sugestao));

  const save = async (withCalendar) => {
    const ms = new Date(when).getTime();
    if (!Number.isFinite(ms)) return notify('Escolha data e hora', 'err');
    await updateVideo(video.id, { agendadoPara: ms });
    if (withCalendar) {
      await shareOrDownload(
        icsFile({ title: `Postar no TikTok: ${peca?.nome || 'vídeo'}`, description: `${video.legenda}\n\n${video.hashtags}`, start: ms }),
        'Lembrete de post',
      );
    }
    notify('Post agendado');
    onDone?.();
  };

  return (
    <div class="stack lg">
      <label class="field">
        <span>Data e hora do post</span>
        <input class="input" type="datetime-local" value={when} onInput={(e) => setWhen(e.currentTarget.value)} />
      </label>
      <p class="muted small" style="margin:0">
        Dica: entre 19h e 21h costuma ter mais gente no TikTok. Depois de 5 posts, a aba Resultados mostra o seu melhor horário.
      </p>
      <button class="btn gold block lg" onClick={() => save(true)}>
        <CalendarPlus size={18} /> Agendar e lembrar no calendário
      </button>
      <button class="btn block" onClick={() => save(false)}>
        Só agendar
      </button>
      {video.agendadoPara && (
        <button
          class="btn ghost block"
          onClick={async () => {
            await updateVideo(video.id, { agendadoPara: null });
            onDone?.();
          }}
        >
          Remover data
        </button>
      )}
    </div>
  );
}

export function Agenda() {
  const [weekStart, setWeekStart] = useState(startOfWeek());
  const [sel, setSel] = useState(new Date());
  const [scheduling, setScheduling] = useState(null);
  const days = Array.from({ length: 7 }, (_, i) => new Date(weekStart.getTime() + i * 864e5));
  const today = new Date();

  const all = videos.value;
  const doDia = all.filter((v) => dayOf(v) && sameDay(new Date(dayOf(v)), sel)).sort((a, b) => dayOf(a) - dayOf(b));
  const semData = all.filter((v) => v.status !== 'postado' && !v.agendadoPara);

  const shift = (n) => {
    const ws = new Date(weekStart.getTime() + n * 7 * 864e5);
    setWeekStart(ws);
    setSel(n === 0 ? today : ws);
  };

  const Item = ({ v }) => {
    const peca = pecaById(v.pecaId);
    const t = dayOf(v);
    return (
      <div class="list-item" style="cursor:default">
        <div class="avatar">
          <PecaFoto peca={peca} />
        </div>
        <button class="grow" style="background:none;border:0;text-align:left;padding:0;cursor:pointer" onClick={() => go(`/criar/${v.id}/${nextStep(v).step}`)}>
          <b class="ellipsis" style="display:block">{peca?.nome}</b>
          <span class="muted small">
            {formatById(v.formatoId).emoji} {formatById(v.formatoId).nome}
            {t ? ` · ${new Date(t).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}` : ''}
          </span>
        </button>
        {v.status === 'postado' ? (
          <StatusBadge status="postado" />
        ) : (
          <button class="icon-btn" aria-label="Agendar" onClick={() => setScheduling(v)}>
            <Clock size={18} />
          </button>
        )}
      </div>
    );
  };

  return (
    <div class="screen">
      <div class="header">
        <h1>Agenda</h1>
      </div>

      <div class="row between" style="margin-bottom:10px">
        <button class="icon-btn" aria-label="Semana anterior" onClick={() => shift(-1)}>
          <ChevronLeft size={20} />
        </button>
        <button class="btn ghost sm" onClick={() => { setWeekStart(startOfWeek()); setSel(today); }}>
          {days[0].toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })} – {days[6].toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })}
        </button>
        <button class="icon-btn" aria-label="Próxima semana" onClick={() => shift(1)}>
          <ChevronRight size={20} />
        </button>
      </div>

      <div class="week">
        {days.map((d, i) => {
          const n = all.filter((v) => dayOf(v) && sameDay(new Date(dayOf(v)), d)).length;
          return (
            <button class={`${sameDay(d, sel) ? 'on' : ''} ${sameDay(d, today) ? 'today' : ''}`} onClick={() => setSel(d)} aria-label={d.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric' })}>
              {DIAS[i]}
              <b>{d.getDate()}</b>
              <span class="dots">{Array.from({ length: Math.min(3, n) }, () => <i />)}</span>
            </button>
          );
        })}
      </div>

      <div class="section-title">
        <h2>{sel.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}</h2>
      </div>
      {doDia.length ? (
        <div class="stack">{doDia.map((v) => <Item v={v} key={v.id} />)}</div>
      ) : (
        <p class="muted small">Nada marcado para este dia.</p>
      )}

      <div class="section-title">
        <h2>Sem data</h2>
        <span class="muted small">{semData.length}</span>
      </div>
      {semData.length ? (
        <div class="stack">{semData.map((v) => <Item v={v} key={v.id} />)}</div>
      ) : all.length === 0 ? (
        <Empty icon={CircleCheck} title="Nenhum vídeo ainda" text="Crie um vídeo e escolha quando postar." action={<button class="btn gold" onClick={() => go('/criar')}>Criar vídeo</button>} />
      ) : (
        <p class="muted small">Todos os rascunhos têm data. 👏</p>
      )}

      <Sheet open={!!scheduling} onClose={() => setScheduling(null)} title="Quando postar?">
        {scheduling && <ScheduleForm video={scheduling} onDone={() => setScheduling(null)} />}
      </Sheet>
    </div>
  );
}
