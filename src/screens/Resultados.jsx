import { useState } from 'preact/hooks';
import { Eye, Heart, MessageCircle, Repeat2, Bookmark, ShoppingBag, TrendingUp, Lightbulb, ChartColumn } from 'lucide-preact';
import { go } from '../router.js';
import { videos, pecas, pecaById, updateVideo, notify } from '../store.js';
import { Sheet, PecaFoto, Empty } from '../ui.jsx';
import { insights, engajamento } from '../logic/insights.js';
import { formatById } from '../data/formats.js';

const fmtN = (n) => (n >= 1e6 ? (n / 1e6).toFixed(1).replace('.', ',') + ' mi' : n >= 1e4 ? Math.round(n / 1e3) + ' mil' : (n || 0).toLocaleString('pt-BR'));
const pct = (x) => (x * 100).toFixed(1).replace('.', ',') + '%';

const CAMPOS = [
  ['views', 'Visualizações', Eye],
  ['curtidas', 'Curtidas', Heart],
  ['comentarios', 'Comentários', MessageCircle],
  ['compartilhamentos', 'Compartilhamentos', Repeat2],
  ['salvamentos', 'Salvamentos', Bookmark],
  ['vendas', 'Vendas', ShoppingBag],
];

export function MetricsForm({ video, onDone }) {
  const [m, setM] = useState({ views: '', curtidas: '', comentarios: '', compartilhamentos: '', salvamentos: '', vendas: '', ...(video.metricas || {}) });
  const save = async (e) => {
    e.preventDefault();
    const clean = Object.fromEntries(CAMPOS.map(([k]) => [k, Math.max(0, parseInt(String(m[k]).replace(/\D/g, ''), 10) || 0)]));
    await updateVideo(video.id, { metricas: clean });
    notify('Números salvos');
    onDone?.();
  };
  return (
    <form class="stack lg" onSubmit={save}>
      <p class="muted small" style="margin:0">Copie do TikTok (toque no vídeo → ⋯ → Análises). Pode atualizar depois.</p>
      <div class="grid2">
        {CAMPOS.map(([k, label, Icon]) => (
          <label class="field">
            <span class="row" style="gap:6px">
              <Icon size={14} /> {label}
            </span>
            <input class="input big" inputMode="numeric" value={m[k]} onInput={(e) => {
                const val = e.currentTarget.value;
                setM((cur) => ({ ...cur, [k]: val }));
              }} placeholder="0" />
          </label>
        ))}
      </div>
      <button class="btn gold lg block" type="submit">
        Salvar números
      </button>
    </form>
  );
}

function Bars({ items }) {
  const max = Math.max(1, ...items.map((i) => i.value));
  const w = 320;
  const h = 130;
  const bw = w / items.length;
  return (
    <svg class="chart" viewBox={`0 0 ${w} ${h + 18}`} width="100%" role="img" aria-label="Visualizações dos últimos vídeos">
      <defs>
        <linearGradient id="bg" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0" stop-color="#f6dc94" />
          <stop offset="1" stop-color="#b9862a" />
        </linearGradient>
      </defs>
      {items.map((it, i) => {
        const bh = Math.max(3, (it.value / max) * h);
        return (
          <g>
            <rect x={i * bw + bw * 0.18} y={h - bh} width={bw * 0.64} height={bh} rx="5" fill="url(#bg)">
              <title>{`${it.label}: ${it.value.toLocaleString('pt-BR')} views`}</title>
            </rect>
            <text x={i * bw + bw / 2} y={h + 13} text-anchor="middle">{it.short}</text>
          </g>
        );
      })}
    </svg>
  );
}

export function Resultados() {
  const [editing, setEditing] = useState(null);
  const ins = insights(videos.value, pecas.value);
  const postados = videos.value.filter((v) => v.status === 'postado').sort((a, b) => (b.postadoEm || 0) - (a.postadoEm || 0));
  const comNumeros = postados.filter((v) => v.metricas).slice(0, 8).reverse();

  return (
    <div class="screen">
      <div class="header">
        <h1>Resultados</h1>
      </div>

      {postados.length === 0 ? (
        <Empty icon={ChartColumn} title="Seus números aparecem aqui" text="Depois de postar, anote views e vendas de cada vídeo. O app mostra o que está funcionando." action={<button class="btn gold" onClick={() => go('/criar')}>Criar vídeo</button>} />
      ) : (
        <>
          <div class="kpis">
            <div class="kpi">
              <span class="muted small row" style="gap:6px"><Eye size={14} /> Visualizações</span>
              <b>{fmtN(ins.total.views)}</b>
            </div>
            <div class="kpi">
              <span class="muted small row" style="gap:6px"><ShoppingBag size={14} /> Vendas</span>
              <b>{fmtN(ins.total.vendas)}</b>
            </div>
            <div class="kpi">
              <span class="muted small row" style="gap:6px"><TrendingUp size={14} /> Engajamento</span>
              <b>{pct(ins.engMedio)}</b>
            </div>
            <div class="kpi">
              <span class="muted small row" style="gap:6px"><ChartColumn size={14} /> Postados</span>
              <b>{postados.length}</b>
            </div>
          </div>

          {comNumeros.length >= 2 && (
            <div class="card" style="margin-top:14px">
              <h3 style="margin-bottom:10px">Últimos vídeos</h3>
              <Bars items={comNumeros.map((v) => ({ value: v.metricas.views || 0, label: pecaById(v.pecaId)?.nome || '', short: new Date(v.postadoEm).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }) }))} />
            </div>
          )}

          <div class="section-title">
            <h2>O que está funcionando</h2>
          </div>
          {ins.dicas.length ? (
            <div class="stack">
              {ins.dicas.map((d) => (
                <div class="pill-tip">
                  <Lightbulb size={18} class="gold" style="flex:none" />
                  <span>{d}</span>
                </div>
              ))}
            </div>
          ) : (
            <p class="muted small">Anote os números de pelo menos 3 vídeos para ver dicas baseadas nos seus resultados.</p>
          )}

          <div class="section-title">
            <h2>Vídeos postados</h2>
          </div>
          <div class="stack">
            {postados.map((v) => {
              const peca = pecaById(v.pecaId);
              return (
                <button class="list-item" onClick={() => setEditing(v)}>
                  <div class="avatar">
                    <PecaFoto peca={peca} />
                  </div>
                  <div class="grow">
                    <b class="ellipsis" style="display:block">{peca?.nome}</b>
                    <span class="muted small">
                      {formatById(v.formatoId).nome} · {new Date(v.postadoEm).toLocaleDateString('pt-BR')}
                    </span>
                  </div>
                  {v.metricas ? (
                    <div style="text-align:right">
                      <b>{fmtN(v.metricas.views)}</b>
                      <div class="tiny muted">{pct(engajamento(v.metricas))} eng.</div>
                    </div>
                  ) : (
                    <span class="badge postado">Anotar</span>
                  )}
                </button>
              );
            })}
          </div>
        </>
      )}

      <Sheet open={!!editing} onClose={() => setEditing(null)} title="Números do vídeo">
        {editing && <MetricsForm video={editing} onDone={() => setEditing(null)} />}
      </Sheet>
    </div>
  );
}
