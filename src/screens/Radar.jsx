import { useEffect, useMemo, useState } from 'preact/hooks';
import { Plus, Link2, ClipboardPaste, Eye, Heart, MessageCircle, Repeat2, Bookmark, BookmarkPlus, ExternalLink, WandSparkles, Trash2, Sparkles, RefreshCw, Radar as RadarIcon, Lightbulb, Copy } from 'lucide-preact';
import { back } from '../router.js';
import { radar, videos, saveRadarPost, deleteRadarPost, saveGancho, notify } from '../store.js';
import { Sheet, PecaPicker, SubHeader, Empty } from '../ui.jsx';
import { TIPOS_RADAR, tipoRadar, guessTipo, engajamentoPost, rankPosts, radarInsights, ganchoDaLegenda, fmtCurto } from '../logic/radar.js';
import { FORMATS } from '../data/formats.js';
import { readLink, aiAvailable, aiRadar, aiTendencias } from '../ai.js';
import { copyText } from '../media/share.js';
import { criarVideo } from './Ferramentas.jsx';

const pct = (x) => (x * 100).toFixed(1).replace('.', ',') + '%';
const PLAT = { tiktok: 'TikTok', instagram: 'Instagram' };
const ORDENS = [
  ['views', 'Views'],
  ['curtidas', 'Curtidas'],
  ['engajamento', 'Engajamento'],
  ['comentarios', 'Comentários'],
];
const METRICAS = [
  ['views', 'Visualizações', Eye],
  ['curtidas', 'Curtidas', Heart],
  ['comentarios', 'Comentários', MessageCircle],
  ['compart', 'Compartilh.', Repeat2],
  ['salvos', 'Salvos', Bookmark],
];

// Link compartilhado para o app (Android: menu Compartilhar → GlowStudio).
export const SHARE_KEY = 'glow-share';
const takeShared = () => {
  try {
    const v = sessionStorage.getItem(SHARE_KEY);
    sessionStorage.removeItem(SHARE_KEY);
    return v || '';
  } catch {
    return '';
  }
};

export function Radar({ novo }) {
  const [aba, setAba] = useState('ranking');
  const [ordem, setOrdem] = useState('views');
  const [plat, setPlat] = useState('');
  const [adding, setAdding] = useState(() => (novo ? takeShared() || ' ' : null));
  const [aberto, setAberto] = useState(null);
  const [aiOn, setAiOn] = useState(false);

  useEffect(() => {
    aiAvailable().then(setAiOn);
  }, []);

  const posts = radar.value;
  const lista = rankPosts(posts, ordem, plat);
  const atual = aberto && posts.find((p) => p.id === aberto);

  return (
    <div class="screen">
      <SubHeader title="Radar da concorrência" eyebrow="Ferramentas" back={() => back('/ferramentas')}>
        <button class="icon-btn on" aria-label="Adicionar post" onClick={() => setAdding('')}>
          <Plus size={22} />
        </button>
      </SubHeader>

      {posts.length === 0 ? (
        <Empty
          icon={RadarIcon}
          title="Monte o ranking do seu nicho"
          text="Viu um post de semijoias que bombou no TikTok ou no Instagram? Copie o link e cole aqui. O app lê o perfil, a legenda e os números e mostra o que está dando certo."
          action={
            <button class="btn gold" onClick={() => setAdding('')}>
              <Link2 size={18} /> Colar link de um post
            </button>
          }
        />
      ) : (
        <>
          <div class="seg" role="tablist" style="margin-bottom:14px">
            <button class={aba === 'ranking' ? 'on' : ''} role="tab" aria-selected={aba === 'ranking'} onClick={() => setAba('ranking')}>
              Ranking
            </button>
            <button class={aba === 'insights' ? 'on' : ''} role="tab" aria-selected={aba === 'insights'} onClick={() => setAba('insights')}>
              Insights
            </button>
          </div>
          {aba === 'ranking' ? (
            <>
              <div class="chips" style="margin-bottom:8px" role="group" aria-label="Ordenar por">
                {ORDENS.map(([k, l]) => (
                  <button class={`chip ${ordem === k ? 'on' : ''}`} aria-pressed={ordem === k} onClick={() => setOrdem(k)}>
                    {l}
                  </button>
                ))}
              </div>
              <div class="chips" style="margin-bottom:14px" role="group" aria-label="Rede">
                {[['', 'Todas'], ['tiktok', 'TikTok'], ['instagram', 'Instagram']].map(([k, l]) => (
                  <button class={`chip ${plat === k ? 'on' : ''}`} aria-pressed={plat === k} onClick={() => setPlat(k)}>
                    {l}
                  </button>
                ))}
              </div>
              <ol class="stack rank">
                {lista.map((p, i) => (
                  <li key={p.id}>
                    <RankRow p={p} pos={i + 1} onOpen={() => setAberto(p.id)} />
                  </li>
                ))}
              </ol>
              {lista.length === 0 && <p class="muted small center">Nenhum post dessa rede ainda.</p>}
            </>
          ) : (
            <Insights posts={posts} aiOn={aiOn} />
          )}
        </>
      )}

      <Sheet open={adding !== null} onClose={() => setAdding(null)} title="Adicionar ao Radar">
        {adding !== null && <AddPost initialUrl={adding.trim()} onDone={() => setAdding(null)} />}
      </Sheet>
      <Sheet open={!!atual} onClose={() => setAberto(null)} title="Post da concorrência">
        {atual && <PostDetail p={atual} aiOn={aiOn} onClose={() => setAberto(null)} />}
      </Sheet>
    </div>
  );
}

function Thumb({ p, class: cls = 'rank-thumb' }) {
  return <div class={cls}>{p.thumb ? <img src={p.thumb} alt="" loading="lazy" /> : <span aria-hidden="true">{tipoRadar(p.tipo).emoji}</span>}</div>;
}

function RankRow({ p, pos, onOpen }) {
  const t = tipoRadar(p.tipo);
  return (
    <button class="list-item rank-row" onClick={onOpen}>
      <span class={`rank-pos ${pos <= 3 ? 'top' : ''}`}>{pos}</span>
      <Thumb p={p} />
      <div class="grow stack" style="gap:4px">
        <div class="row between" style="gap:6px">
          <b class="ellipsis">@{p.perfil || 'perfil'}</b>
          <span class={`net ${p.plataforma}`}>{PLAT[p.plataforma] || 'Outro'}</span>
        </div>
        <p class="clamp2 small muted" style="margin:0">{p.legenda || 'Sem legenda'}</p>
        <div class="row small metrics" style="gap:10px">
          <span><Eye size={13} /> {fmtCurto(p.views)}</span>
          <span><Heart size={13} /> {fmtCurto(p.curtidas)}</span>
          <span><MessageCircle size={13} /> {fmtCurto(p.comentarios)}</span>
          {p.views > 0 && <span class="gold">{pct(engajamentoPost(p))}</span>}
        </div>
        <span class="tiny faint">
          {t.emoji} {t.nome}
        </span>
      </div>
    </button>
  );
}

// ---------- Adicionar post ----------
const num = (v) => Math.max(0, parseInt(String(v ?? '').replace(/\D/g, ''), 10) || 0);
const firstUrl = (s) => (s.match(/https?:\/\/\S+/) || [s.trim()])[0];

function AddPost({ initialUrl, onDone }) {
  const [url, setUrl] = useState(initialUrl);
  const [busy, setBusy] = useState(false);
  const [f, setF] = useState(null);
  const [aviso, setAviso] = useState('');

  const buscar = async (u = url) => {
    const link = firstUrl(u);
    if (!link) return;
    setBusy(true);
    setAviso('');
    try {
      const r = await readLink(link);
      setF({
        url: r.url || link,
        plataforma: r.plataforma,
        perfil: r.perfil || '',
        legenda: r.legenda || '',
        thumb: r.thumb || '',
        duracao: r.duracao || 0,
        postadoEm: r.postadoEm || 0,
        formato: r.formato || 'video',
        tipo: guessTipo(r.legenda, r.formato),
        ...Object.fromEntries(METRICAS.map(([k]) => [k, r[k] ? String(r[k]) : ''])),
      });
      if (!r.completo) setAviso('Não consegui ler todos os números. Digite os que aparecem no post.');
    } catch (e) {
      setAviso(`${e.message}. Preencha à mão.`);
      manual(link);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (initialUrl) buscar(initialUrl);
  }, []);

  const manual = (link = url) =>
    setF({ url: firstUrl(link || ''), plataforma: /instagram/i.test(link) ? 'instagram' : 'tiktok', perfil: '', legenda: '', thumb: '', duracao: 0, formato: 'video', tipo: 'outro', ...Object.fromEntries(METRICAS.map(([k]) => [k, ''])) });

  const colar = async () => {
    try {
      const t = await navigator.clipboard.readText();
      if (t) {
        setUrl(t);
        buscar(t);
      }
    } catch {
      notify('Toque e segure no campo para colar', 'err');
    }
  };

  const set = (k) => (e) => {
    const val = e?.currentTarget ? e.currentTarget.value : e;
    setF((cur) => ({ ...cur, [k]: val }));
  };

  const salvar = async (e) => {
    e.preventDefault();
    const dup = radar.value.find((p) => p.url && p.url === f.url);
    await saveRadarPost({
      ...(dup ? { id: dup.id, analise: dup.analise } : {}),
      ...f,
      perfil: f.perfil.replace(/^@/, '').trim(),
      ...Object.fromEntries(METRICAS.map(([k]) => [k, num(f[k])])),
    });
    notify(dup ? 'Post atualizado no Radar' : 'Post salvo no Radar 📡');
    onDone();
  };

  if (!f) {
    return (
      <div class="stack lg">
        <p class="muted small" style="margin:0">
          No TikTok ou Instagram, toque em <b>Compartilhar → Copiar link</b> e cole aqui.
        </p>
        <label class="field">
          <span>Link do post</span>
          <input class="input" type="url" inputMode="url" value={url} placeholder="https://www.tiktok.com/@..." onInput={(e) => setUrl(e.currentTarget.value)} onKeyDown={(e) => e.key === 'Enter' && buscar()} />
        </label>
        <div class="grid2">
          <button class="btn" onClick={colar} disabled={busy}>
            <ClipboardPaste size={18} /> Colar
          </button>
          <button class="btn gold" onClick={() => buscar()} disabled={busy || !url.trim()}>
            {busy ? <span class="spinner" /> : <Link2 size={18} />} {busy ? 'Lendo…' : 'Buscar'}
          </button>
        </div>
        {aviso && <p class="small" style="color:var(--warn);margin:0">{aviso}</p>}
        <button class="btn ghost block" onClick={() => manual()}>
          Preencher à mão
        </button>
      </div>
    );
  }

  return (
    <form class="stack lg" onSubmit={salvar}>
      {aviso && <p class="small" style="color:var(--warn);margin:0">{aviso}</p>}
      <div class="row" style="align-items:flex-start">
        <Thumb p={f} class="rank-thumb big" />
        <div class="grow stack" style="gap:10px">
          <div class="seg">
            {['tiktok', 'instagram'].map((k) => (
              <button type="button" class={f.plataforma === k ? 'on' : ''} onClick={() => set('plataforma')(k)}>
                {PLAT[k]}
              </button>
            ))}
          </div>
          <label class="field">
            <span>Perfil</span>
            <input class="input" value={f.perfil} onInput={set('perfil')} placeholder="@loja" required maxLength={60} />
          </label>
        </div>
      </div>
      <div class="grid2">
        {METRICAS.map(([k, label, Icon]) => (
          <label class="field">
            <span class="row" style="gap:6px">
              <Icon size={14} /> {label}
            </span>
            <input class="input" inputMode="numeric" value={f[k]} onInput={set(k)} placeholder="0" />
          </label>
        ))}
      </div>
      <label class="field">
        <span>Legenda</span>
        <textarea class="textarea" value={f.legenda} onInput={set('legenda')} maxLength={2200} placeholder="Cole a legenda do post" />
      </label>
      <TipoChips value={f.tipo} onChange={set('tipo')} />
      <button class="btn gold lg block" type="submit">
        Salvar no Radar
      </button>
    </form>
  );
}

function TipoChips({ value, onChange }) {
  return (
    <div class="field">
      <span>Tipo de vídeo</span>
      <div class="chips wrap">
        {TIPOS_RADAR.map((t) => (
          <button type="button" class={`chip ${value === t.id ? 'on' : ''}`} aria-pressed={value === t.id} onClick={() => onChange(t.id)}>
            {t.emoji} {t.nome}
          </button>
        ))}
      </div>
    </div>
  );
}

// ---------- Detalhe ----------
function PostDetail({ p, aiOn, onClose }) {
  const [busy, setBusy] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [pick, setPick] = useState(false);
  const a = p.analise;
  const gancho = a?.gancho || ganchoDaLegenda(p.legenda);
  const formatoOk = FORMATS.some((f) => f.id === p.tipo);

  const analisar = async () => {
    setBusy(true);
    try {
      const r = await aiRadar(p);
      await saveRadarPost({ ...p, tipo: r.tipo || p.tipo, analise: r });
      notify('Análise pronta ✨');
    } catch (e) {
      notify(e.message || 'A IA não respondeu agora', 'err');
    } finally {
      setBusy(false);
    }
  };

  const atualizar = async () => {
    setRefreshing(true);
    try {
      const r = await readLink(p.url);
      const patch = Object.fromEntries(METRICAS.map(([k]) => [k, r[k] || p[k] || 0]));
      await saveRadarPost({ ...p, ...patch, thumb: r.thumb || p.thumb, legenda: r.legenda || p.legenda });
      notify(r.completo ? 'Números atualizados' : 'Só consegui atualizar parte dos dados');
    } catch (e) {
      notify(e.message, 'err');
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div class="stack lg">
      <div class="row" style="align-items:flex-start">
        <Thumb p={p} class="rank-thumb big" />
        <div class="grow stack" style="gap:6px">
          <b style="font-size:1.1rem">@{p.perfil}</b>
          <span class={`net ${p.plataforma}`} style="justify-self:start">{PLAT[p.plataforma]}</span>
          {p.postadoEm > 0 && <span class="tiny muted">Postado em {new Date(p.postadoEm).toLocaleDateString('pt-BR')}</span>}
          {p.duracao > 0 && <span class="tiny muted">{p.duracao}s de vídeo</span>}
        </div>
      </div>

      <div class="kpis three">
        {METRICAS.map(([k, label, Icon]) => (
          <div class="kpi">
            <span class="muted tiny row" style="gap:4px">
              <Icon size={12} /> {label}
            </span>
            <b>{fmtCurto(p[k])}</b>
          </div>
        ))}
        <div class="kpi">
          <span class="muted tiny">Engajamento</span>
          <b class="gold">{p.views ? pct(engajamentoPost(p)) : '—'}</b>
        </div>
      </div>

      <TipoChips value={p.tipo} onChange={(tipo) => saveRadarPost({ ...p, tipo })} />

      {p.legenda && (
        <div class="stack" style="gap:6px">
          <div class="row between">
            <h3>Legenda</h3>
            <button class="btn sm ghost" onClick={async () => notify((await copyText(p.legenda)) ? 'Legenda copiada' : 'Não consegui copiar')}>
              <Copy size={14} /> Copiar
            </button>
          </div>
          <p class="card tight small" style="margin:0;white-space:pre-wrap;line-height:1.45">{p.legenda}</p>
        </div>
      )}

      {a ? (
        <div class="card glow stack" style="gap:8px">
          <span class="eyebrow">Análise da IA</span>
          {a.gancho && (
            <p style="margin:0;font-family:var(--serif);font-size:1.1rem">“{a.gancho}”</p>
          )}
          <p class="small" style="margin:0;line-height:1.45"><b>Por que funcionou:</b> {a.porque}</p>
          <p class="small" style="margin:0;line-height:1.45"><b>Como você pode fazer:</b> {a.adaptar}</p>
        </div>
      ) : (
        aiOn && (
          <button class="btn rose block" onClick={analisar} disabled={busy}>
            {busy ? <span class="spinner" /> : <WandSparkles size={18} />} {busy ? 'Analisando…' : 'Analisar com IA: por que bombou?'}
          </button>
        )
      )}

      <div class="stack">
        <button class="btn gold lg block" onClick={() => setPick(true)}>
          <Sparkles size={18} /> Gravar um vídeo nesse estilo
        </button>
        <div class="grid2">
          <button
            class="btn"
            disabled={!gancho}
            onClick={async () => notify((await saveGancho(gancho, 'radar')) ? 'Gancho salvo' : 'Esse gancho já está salvo')}
          >
            <BookmarkPlus size={18} /> Salvar gancho
          </button>
          <a class="btn" href={p.url} target="_blank" rel="noopener" style="text-decoration:none">
            <ExternalLink size={18} /> Abrir post
          </a>
        </div>
        <div class="grid2">
          <button class="btn ghost" onClick={atualizar} disabled={refreshing || !p.url}>
            {refreshing ? <span class="spinner" /> : <RefreshCw size={16} />} Atualizar números
          </button>
          <button
            class="btn ghost"
            onClick={async () => {
              await deleteRadarPost(p.id);
              notify('Post removido do Radar');
              onClose();
            }}
          >
            <Trash2 size={16} /> Remover
          </button>
        </div>
      </div>

      <Sheet open={pick} onClose={() => setPick(false)} title="Com qual peça?">
        {pick && (
          <>
            <p class="muted small" style="margin:-6px 0 14px">
              O vídeo começa no formato {formatoOk ? `“${tipoRadar(p.tipo).nome}”` : '“Brilho na luz”'}
              {gancho ? ' com um gancho parecido. Ajuste com as suas palavras.' : '.'}
            </p>
            <PecaPicker onPick={(peca) => criarVideo(peca, formatoOk ? p.tipo : 'brilho', gancho)} />
          </>
        )}
      </Sheet>
    </div>
  );
}

// ---------- Insights ----------
function Insights({ posts, aiOn }) {
  const ins = useMemo(() => radarInsights(posts, videos.value), [posts]);
  const [ia, setIa] = useState(null);
  const [busy, setBusy] = useState(false);
  const maxViews = Math.max(1, ...ins.porTipo.map((t) => t.mediaViews));

  const resumir = async () => {
    setBusy(true);
    try {
      setIa(await aiTendencias(rankPosts(posts, 'views')));
    } catch (e) {
      notify(e.message || 'A IA não respondeu agora', 'err');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div class="stack lg">
      <div class="kpis">
        <div class="kpi">
          <span class="muted small">Posts analisados</span>
          <b>{ins.total}</b>
        </div>
        <div class="kpi">
          <span class="muted small">Duração típica</span>
          <b>{ins.duracaoMediana ? `${ins.duracaoMediana}s` : '—'}</b>
        </div>
        {ins.porPlataforma.map((pl) => (
          <div class="kpi">
            <span class="muted small">Média de views · {PLAT[pl.plataforma]}</span>
            <b>{pl.n ? fmtCurto(pl.mediaViews) : '—'}</b>
          </div>
        ))}
      </div>

      {ins.dicas.length > 0 && (
        <div class="stack">
          <h3>O que está funcionando na concorrência</h3>
          {ins.dicas.map((d) => (
            <div class="pill-tip">
              <Lightbulb size={18} class="gold" style="flex:none" />
              <span>{d}</span>
            </div>
          ))}
        </div>
      )}
      {posts.length < 5 && <p class="muted small" style="margin:0">Quanto mais posts no Radar, melhores os insights. Tente juntar pelo menos 10.</p>}

      {aiOn && (
        <div class="stack">
          {ia ? (
            <div class="card glow stack" style="gap:10px">
              <span class="eyebrow">Tendências pela IA</span>
              <ul class="ia-list">
                {ia.tendencias.map((t) => (
                  <li>{t}</li>
                ))}
              </ul>
              <span class="eyebrow" style="color:var(--rose)">Ideias para esta semana</span>
              <ul class="ia-list">
                {ia.ideias.map((t) => (
                  <li>{t}</li>
                ))}
              </ul>
            </div>
          ) : (
            <button class="btn rose block" onClick={resumir} disabled={busy || posts.length < 3}>
              {busy ? <span class="spinner" /> : <WandSparkles size={18} />} {busy ? 'Lendo os posts…' : 'Resumir tendências com IA'}
            </button>
          )}
        </div>
      )}

      {ins.porTipo.length > 0 && (
        <div class="card stack">
          <h3>Tipos de vídeo que mais dão views</h3>
          {ins.porTipo.map((t) => (
            <div class="hbar">
              <div class="row between small">
                <span>
                  {t.emoji} {t.nome} <span class="faint">({t.n})</span>
                </span>
                <b>{t.mediaViews ? fmtCurto(t.mediaViews) : '—'}</b>
              </div>
              <div class="bar">
                <i style={{ width: `${Math.round((t.mediaViews / maxViews) * 100)}%` }} />
              </div>
              {t.eng > 0 && <span class="tiny faint">{pct(t.eng)} de engajamento médio</span>}
            </div>
          ))}
        </div>
      )}

      {ins.palavras.length > 0 && (
        <div class="stack">
          <h3>Palavras que mais aparecem</h3>
          <div class="chips wrap">
            {ins.palavras.map((w, i) => (
              <span class={`chip ${i < 3 ? 'on' : ''}`}>
                {w.termo} <span class="faint">{w.n}</span>
              </span>
            ))}
          </div>
        </div>
      )}

      {ins.hashtags.length > 0 && (
        <div class="stack">
          <div class="row between">
            <h3>Hashtags dos posts mais fortes</h3>
            <button class="btn sm ghost" onClick={async () => notify((await copyText(ins.hashtags.map((h) => h.termo).join(' '))) ? 'Hashtags copiadas' : 'Não consegui copiar')}>
              <Copy size={14} /> Copiar
            </button>
          </div>
          <div class="chips wrap">
            {ins.hashtags.map((h) => (
              <span class="chip">
                {h.termo} <span class="faint">{h.n}</span>
              </span>
            ))}
          </div>
        </div>
      )}

      {ins.precos && (
        <div class="card row between">
          <div>
            <b>Preços citados nas legendas</b>
            <p class="muted small" style="margin:2px 0 0">
              de {ins.precos.min.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} a {ins.precos.max.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
            </p>
          </div>
          <b class="gold" style="font-size:1.2rem">{ins.precos.mediana.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</b>
        </div>
      )}
    </div>
  );
}
