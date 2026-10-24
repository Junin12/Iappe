import { useEffect, useState } from 'preact/hooks';
import { Plus, Search, Camera, ImagePlus, WandSparkles, ChevronLeft, Pencil, Trash2, Sparkles, Link } from 'lucide-preact';
import { go, back } from '../router.js';
import { pecas, videos, pecaById, savePeca, deletePeca, putBlob, delBlob, notify, saveVideo, newVideoFor } from '../store.js';
import { Sheet, PecaFoto, Price, Empty, useBlobUrl } from '../ui.jsx';
import { downscale } from '../media/image.js';
import { aiAvailable, aiPhoto } from '../ai.js';
import { VideoRow } from './Home.jsx';

export const TIPOS = ['Colar', 'Brinco', 'Anel', 'Pulseira', 'Conjunto', 'Tornozeleira'];
export const BANHOS = ['Ouro 18k', 'Ródio', 'Prata', 'Ouro rosé', 'Outro'];
export const PEDRAS = ['Nenhuma', 'Zircônia', 'Pérola', 'Cristal', 'Madrepérola'];

export function Pecas({ openNew }) {
  const [q, setQ] = useState('');
  const [tipo, setTipo] = useState('');
  const [editing, setEditing] = useState(openNew ? {} : null);

  const list = pecas.value.filter(
    (p) =>
      (!tipo || p.tipo === tipo) &&
      (!q || `${p.nome} ${p.sku || ''}`.toLowerCase().includes(q.toLowerCase())),
  );

  return (
    <div class="screen">
      <div class="header">
        <h1>Peças</h1>
        <button class="btn gold sm" onClick={() => setEditing({})}>
          <Plus size={18} /> Nova
        </button>
      </div>

      {pecas.value.length > 0 && (
        <div class="stack" style="margin-bottom:16px">
          <label class="row card tight" style="gap:8px;padding:0 12px">
            <Search size={18} class="faint" />
            <input class="input" style="border:0;background:transparent;padding-left:0" placeholder="Buscar por nome ou código" value={q} onInput={(e) => setQ(e.currentTarget.value)} aria-label="Buscar peças" />
          </label>
          <div class="chips">
            <button class={`chip ${!tipo ? 'on' : ''}`} onClick={() => setTipo('')}>
              Todas
            </button>
            {TIPOS.map((t) => (
              <button class={`chip ${tipo === t ? 'on' : ''}`} onClick={() => setTipo(tipo === t ? '' : t)}>
                {t}
              </button>
            ))}
          </div>
        </div>
      )}

      {pecas.value.length === 0 ? (
        <Empty
          title="Seu catálogo começa aqui"
          text="Tire uma foto da peça. Com a IA ligada, o app sugere nome, tipo e banho."
          action={
            <button class="btn gold" onClick={() => setEditing({})}>
              <Camera size={18} /> Cadastrar primeira peça
            </button>
          }
        />
      ) : list.length === 0 ? (
        <p class="muted center">Nenhuma peça encontrada.</p>
      ) : (
        <div class="peca-grid">
          {list.map((p) => {
            const n = videos.value.filter((v) => v.pecaId === p.id).length;
            return (
              <button class="peca-card" onClick={() => go(`/peca/${p.id}`)} key={p.id}>
                <div class="thumb">
                  <PecaFoto peca={p} size={32} />
                  {p.precoPromo && p.precoPromo < p.preco && <span class="badge promo">Promo</span>}
                </div>
                <div class="info">
                  <b class="ellipsis" style="display:block">{p.nome}</b>
                  <div class="row between small" style="margin-top:4px">
                    <Price peca={p} />
                    <span class="faint tiny">{n ? `${n} vídeo${n > 1 ? 's' : ''}` : p.tipo}</span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}

      <Sheet open={!!editing} onClose={() => setEditing(null)} title={editing?.id ? 'Editar peça' : 'Nova peça'}>
        <PecaForm
          initial={editing}
          onDone={(p) => {
            setEditing(null);
            if (openNew) go(`/peca/${p.id}`, { replace: true });
          }}
        />
      </Sheet>
    </div>
  );
}

const EMPTY = { nome: '', tipo: 'Colar', banho: 'Ouro 18k', milesimos: 10, pedra: 'Nenhuma', preco: '', precoPromo: '', estoque: 1, link: '', sku: '', obs: '' };

function Chips({ options, value, onChange, label }) {
  return (
    <div class="field">
      <span>{label}</span>
      <div class="chips wrap" role="radiogroup" aria-label={label}>
        {options.map((o) => (
          <button type="button" role="radio" aria-checked={value === o} class={`chip ${value === o ? 'on' : ''}`} onClick={() => onChange(o)}>
            {o}
          </button>
        ))}
      </div>
    </div>
  );
}

export function PecaForm({ initial, onDone }) {
  const [f, setF] = useState({ ...EMPTY, ...initial, preco: initial?.preco ?? '', precoPromo: initial?.precoPromo ?? '' });
  const [foto, setFoto] = useState(null); // Blob novo ainda não salvo
  const [aiOn, setAiOn] = useState(false);
  const [aiBusy, setAiBusy] = useState(false);
  const [ideias, setIdeias] = useState([]);
  const savedUrl = useBlobUrl(initial?.fotoId);
  const [previewUrl, setPreviewUrl] = useState(null);

  useEffect(() => {
    aiAvailable().then(setAiOn);
  }, []);
  useEffect(() => {
    if (!foto) return;
    const u = URL.createObjectURL(foto);
    setPreviewUrl(u);
    return () => URL.revokeObjectURL(u);
  }, [foto]);

  const set = (k) => (e) => {
    const value = e?.currentTarget ? e.currentTarget.value : e;
    setF((cur) => ({ ...cur, [k]: value }));
  };

  const pick = async (e) => {
    const file = e.currentTarget.files?.[0];
    e.currentTarget.value = '';
    if (!file) return;
    try {
      setFoto(await downscale(file));
    } catch {
      notify('Não consegui abrir essa foto', 'err');
    }
  };

  const fillWithAi = async () => {
    const blob = foto || (initial?.fotoId && (await fetch(savedUrl).then((r) => r.blob())));
    if (!blob) return notify('Tire ou escolha uma foto primeiro', 'err');
    setAiBusy(true);
    try {
      const r = await aiPhoto(await downscale(blob, 1024, 0.8));
      setF((cur) => ({
        ...cur,
        nome: cur.nome || r.nome || '',
        tipo: TIPOS.includes(r.tipo) ? r.tipo : cur.tipo,
        banho: BANHOS.includes(r.banho) ? r.banho : cur.banho,
        pedra: PEDRAS.includes(r.pedra) ? r.pedra : cur.pedra,
      }));
      setIdeias(Array.isArray(r.ideias) ? r.ideias.slice(0, 3) : []);
      notify('Confira os dados sugeridos');
    } catch {
      notify('A IA não respondeu agora. Preencha manualmente.', 'err');
    } finally {
      setAiBusy(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    const preco = parseFloat(String(f.preco).replace(',', '.'));
    const promo = parseFloat(String(f.precoPromo).replace(',', '.'));
    if (!f.nome.trim()) return notify('Dê um nome para a peça', 'err');
    if (!(preco > 0)) return notify('Informe o preço', 'err');
    let fotoId = initial?.fotoId || null;
    if (foto) {
      if (fotoId) await delBlob(fotoId);
      fotoId = await putBlob(foto);
    }
    const peca = await savePeca({
      ...initial,
      ...f,
      nome: f.nome.trim(),
      preco,
      precoPromo: promo > 0 && promo < preco ? promo : null,
      milesimos: parseInt(f.milesimos, 10) || null,
      estoque: parseInt(f.estoque, 10) || 0,
      link: f.link.trim(),
      fotoId,
    });
    notify(initial?.id ? 'Peça atualizada' : 'Peça cadastrada ✨');
    onDone(peca);
  };

  const url = previewUrl || savedUrl;

  return (
    <form class="stack lg" onSubmit={submit}>
      <div class="stack">
        <div class="thumb" style="border-radius:18px;aspect-ratio:4/3;border:1px solid var(--line)">
          {url ? <img src={url} alt="Foto da peça" /> : <span class="muted small">Foto da peça</span>}
        </div>
        <div class="grid2">
          <label class="btn">
            <Camera size={18} /> Tirar foto
            <input type="file" accept="image/*" capture="environment" hidden onChange={pick} />
          </label>
          <label class="btn">
            <ImagePlus size={18} /> Galeria
            <input type="file" accept="image/*" hidden onChange={pick} />
          </label>
        </div>
        {aiOn && url && (
          <button type="button" class="btn rose block" onClick={fillWithAi} disabled={aiBusy}>
            {aiBusy ? <span class="spinner" /> : <WandSparkles size={18} />} {aiBusy ? 'Analisando a foto…' : 'Preencher com IA'}
          </button>
        )}
        {ideias.length > 0 && (
          <div class="pill-tip">
            <Sparkles size={18} class="gold" style="flex:none" />
            <div>
              <b>Ideias de vídeo</b>
              <ul style="margin:6px 0 0;padding-left:18px">
                {ideias.map((i) => (
                  <li>{i}</li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </div>

      <label class="field">
        <span>Nome da peça</span>
        <input class="input" value={f.nome} onInput={set('nome')} placeholder="Ex.: Colar Ponto de Luz" required maxLength={60} />
      </label>
      <Chips label="Tipo" options={TIPOS} value={f.tipo} onChange={set('tipo')} />
      <Chips label="Banho" options={BANHOS} value={f.banho} onChange={set('banho')} />
      <Chips label="Pedra" options={PEDRAS} value={f.pedra} onChange={set('pedra')} />

      <div class="grid2">
        <label class="field">
          <span>Preço (R$)</span>
          <input class="input" inputMode="decimal" value={f.preco} onInput={set('preco')} placeholder="89,90" required />
        </label>
        <label class="field">
          <span>Promoção (opcional)</span>
          <input class="input" inputMode="decimal" value={f.precoPromo} onInput={set('precoPromo')} placeholder="69,90" />
        </label>
        <label class="field">
          <span>Milésimos do banho</span>
          <input class="input" inputMode="numeric" value={f.milesimos ?? ''} onInput={set('milesimos')} placeholder="10" />
        </label>
        <label class="field">
          <span>Estoque</span>
          <input class="input" inputMode="numeric" value={f.estoque} onInput={set('estoque')} />
        </label>
      </div>

      <label class="field">
        <span>Link da loja / TikTok Shop (opcional)</span>
        <input class="input" type="url" inputMode="url" value={f.link} onInput={set('link')} placeholder="https://" />
      </label>
      <label class="field">
        <span>Observações (o que tem de especial?)</span>
        <textarea class="textarea" value={f.obs} onInput={set('obs')} maxLength={300} placeholder="Ex.: fecho com som crocante, brilha muito na luz lateral" />
      </label>

      <button class="btn gold lg block" type="submit">
        Salvar peça
      </button>
    </form>
  );
}

export function PecaDetail({ id }) {
  const peca = pecaById(id);
  const [editing, setEditing] = useState(false);
  const [confirm, setConfirm] = useState(false);
  if (!peca) {
    return (
      <div class="screen">
        <Empty title="Peça não encontrada" action={<button class="btn" onClick={() => go('/pecas')}>Ver peças</button>} />
      </div>
    );
  }
  const vids = videos.value.filter((v) => v.pecaId === id);

  const criar = async () => {
    const v = newVideoFor(peca);
    await saveVideo(v);
    go(`/criar/${v.id}/formato`);
  };

  return (
    <div class="screen">
      <div class="header">
        <button class="icon-btn" aria-label="Voltar" onClick={() => back('/pecas')}>
          <ChevronLeft size={22} />
        </button>
        <h2 class="ellipsis">{peca.nome}</h2>
        <button class="icon-btn" aria-label="Editar peça" onClick={() => setEditing(true)}>
          <Pencil size={18} />
        </button>
      </div>

      <div class="thumb" style="border-radius:22px;aspect-ratio:1;border:1px solid var(--line)">
        <PecaFoto peca={peca} size={48} />
      </div>

      <div class="row between" style="margin:16px 0 6px">
        <div>
          <span class="eyebrow">{peca.tipo}</span>
          <div style="font-size:1.4rem;margin-top:4px">
            <Price peca={peca} />
          </div>
        </div>
        <span class="muted small">{peca.estoque} em estoque</span>
      </div>
      <p class="muted small" style="margin:0 0 4px">
        {[peca.banho, peca.milesimos ? `${peca.milesimos} milésimos` : null, peca.pedra !== 'Nenhuma' ? peca.pedra : null].filter(Boolean).join(' · ')}
      </p>
      {peca.obs && <p style="margin:8px 0 0;line-height:1.45">{peca.obs}</p>}
      {peca.link && (
        <a class="row small" style="margin-top:8px;gap:6px" href={peca.link} target="_blank" rel="noopener">
          <Link size={14} /> Link da loja
        </a>
      )}

      <button class="btn gold lg block hero-cta" style="margin-top:18px" onClick={criar}>
        <Sparkles size={20} /> Criar vídeo desta peça
      </button>

      <div class="section-title">
        <h2>Vídeos</h2>
        <span class="muted small">{vids.length}</span>
      </div>
      {vids.length ? (
        <div class="stack">
          {vids.map((v) => (
            <VideoRow v={v} key={v.id} />
          ))}
        </div>
      ) : (
        <p class="muted small">Nenhum vídeo ainda.</p>
      )}

      <button class="btn ghost block" style="margin-top:28px;color:var(--rose)" onClick={() => setConfirm(true)}>
        <Trash2 size={18} /> Excluir peça
      </button>

      <Sheet open={editing} onClose={() => setEditing(false)} title="Editar peça">
        <PecaForm initial={peca} onDone={() => setEditing(false)} />
      </Sheet>
      <Sheet open={confirm} onClose={() => setConfirm(false)} title="Excluir peça?">
        <p class="muted">A peça e {vids.length ? `os ${vids.length} vídeos dela` : 'a foto'} serão apagados deste aparelho.</p>
        <div class="grid2" style="margin-top:16px">
          <button class="btn" onClick={() => setConfirm(false)}>
            Cancelar
          </button>
          <button
            class="btn rose"
            onClick={async () => {
              await deletePeca(id);
              notify('Peça excluída');
              go('/pecas', { replace: true });
            }}
          >
            Excluir
          </button>
        </div>
      </Sheet>
    </div>
  );
}
