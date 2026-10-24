import { useState } from 'preact/hooks';
import { Store, Copy, Share2, ExternalLink, Send, Trash2, MessageCircle } from 'lucide-preact';
import { back } from '../router.js';
import { pecas, vitrine, saveVitrine, savePeca, getBlob, notify } from '../store.js';
import { Sheet, SubHeader, PecaFoto, Price } from '../ui.jsx';
import { vitrineCall } from '../ai.js';
import { downscale } from '../media/image.js';
import { copyText } from '../media/share.js';
import { precoFinal } from '../data/copy.js';

export const slugify = (s) =>
  (s || '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40);

// "(11) 98888-7777" → "5511988887777"
export const waNumber = (s) => {
  const d = String(s || '').replace(/\D/g, '');
  return d.length === 10 || d.length === 11 ? '55' + d : d;
};

const newToken = () => {
  const b = crypto.getRandomValues(new Uint8Array(24));
  return btoa(String.fromCharCode(...b)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
};

const toBase64 = (blob) =>
  new Promise((resolve, reject) => {
    const fr = new FileReader();
    fr.onload = () => resolve(String(fr.result).split(',')[1]);
    fr.onerror = () => reject(fr.error);
    fr.readAsDataURL(blob);
  });

export const lojaUrl = (slug) => new URL(`loja/${slug}`, location.href.split('#')[0]).href;

export function Vitrine() {
  const vt = vitrine.value;
  const [f, setF] = useState({
    nome: vt.nome || '',
    whatsapp: vt.whatsapp || '',
    instagram: vt.instagram || '',
    bio: vt.bio || '',
    mensagem: vt.mensagem || 'Oi! Vi sua vitrine e quero:',
    slug: vt.slug || '',
  });
  const [prog, setProg] = useState(null);
  const [confirmDel, setConfirmDel] = useState(false);
  const publicada = !!vt.publicadaEm;
  const slug = f.slug || slugify(f.nome);
  const naVitrine = pecas.value.filter((p) => p.naVitrine !== false && precoFinal(p) > 0);

  const set = (k) => (e) => {
    const val = e.currentTarget.value;
    setF((cur) => ({ ...cur, [k]: k === 'slug' ? slugify(val) : val }));
  };

  const publicar = async (e) => {
    e.preventDefault();
    const whatsapp = waNumber(f.whatsapp);
    if (whatsapp.length < 12) return notify('WhatsApp com DDD, ex.: (11) 98888-7777', 'err');
    if (slug.length < 3) return notify('Escolha um endereço com pelo menos 3 letras', 'err');
    const token = vt.token || newToken();
    setProg({ msg: 'Publicando…', pct: 0.05 });
    try {
      const r = await vitrineCall({
        action: 'publicar',
        slug,
        token,
        loja: { nome: f.nome, bio: f.bio, whatsapp, instagram: f.instagram.replace(/^@/, ''), mensagem: f.mensagem },
        pecas: naVitrine.map((p) => ({
          id: p.id,
          foto: p.fotoId || '',
          nome: p.nome,
          tipo: p.tipo,
          banho: p.banho,
          pedra: p.pedra,
          desc: p.obs || '',
          preco: Number(p.preco) || 0,
          precoPromo: Number(p.precoPromo) || 0,
          esgotado: !!p.esgotado || Number(p.estoque) === 0,
        })),
      });
      // Primeira publicação deu certo: o token passa a ser a "chave" desta vitrine neste aparelho.
      await saveVitrine({ ...f, slug, whatsapp: f.whatsapp, token });
      const faltam = r.faltam || [];
      for (let i = 0; i < faltam.length; i++) {
        setProg({ msg: `Enviando fotos ${i + 1}/${faltam.length}`, pct: 0.1 + (0.9 * i) / faltam.length });
        const p = pecas.value.find((x) => x.id === faltam[i]);
        const blob = p && (await getBlob(p.fotoId));
        if (!blob) continue;
        const small = await downscale(blob, 720, 0.8);
        await vitrineCall({ action: 'foto', slug, token, id: p.id, foto: p.fotoId, image: await toBase64(small) });
      }
      await saveVitrine({ publicadaEm: Date.now() });
      notify(publicada ? 'Vitrine atualizada ✨' : 'Sua vitrine está no ar! ✨');
    } catch (err) {
      notify(err.message || 'Não consegui publicar agora', 'err');
    } finally {
      setProg(null);
    }
  };

  const apagar = async () => {
    try {
      await vitrineCall({ action: 'apagar', slug: vt.slug, token: vt.token });
      await saveVitrine({ publicadaEm: null });
      notify('Vitrine tirada do ar');
    } catch (err) {
      notify(err.message, 'err');
    }
    setConfirmDel(false);
  };

  const link = vt.slug ? lojaUrl(vt.slug) : '';

  return (
    <div class="screen">
      <SubHeader title="Vitrine com WhatsApp" eyebrow="Ferramentas" back={() => back('/ferramentas')} />

      {publicada ? (
        <div class="card glow stack">
          <span class="eyebrow">Sua vitrine está no ar</span>
          <a href={link} target="_blank" rel="noopener" class="vitrine-link">{link.replace(/^https?:\/\//, '')}</a>
          <p class="muted small" style="margin:0">Coloque esse link na bio do TikTok e do Instagram. A cliente escolhe as peças e o pedido chega pronto no seu WhatsApp.</p>
          <div class="grid2">
            <button class="btn" onClick={async () => notify((await copyText(link)) ? 'Link copiado' : 'Não consegui copiar')}>
              <Copy size={18} /> Copiar link
            </button>
            <button class="btn" onClick={() => (navigator.share ? navigator.share({ title: f.nome, url: link }).catch(() => {}) : copyText(link))}>
              <Share2 size={18} /> Compartilhar
            </button>
          </div>
          <a class="btn ghost block" href={link} target="_blank" rel="noopener" style="text-decoration:none">
            <ExternalLink size={18} /> Ver como a cliente vê
          </a>
        </div>
      ) : (
        <div class="pill-tip" style="margin-bottom:4px">
          <MessageCircle size={20} class="gold" style="flex:none" />
          <span>Crie uma vitrine bonita das suas peças para o <b>link na bio</b>. A cliente toca em "Quero esta" e a mensagem sai pronta no WhatsApp, com nome e preço.</span>
        </div>
      )}

      <form class="stack lg" style="margin-top:20px" onSubmit={publicar}>
        <label class="field">
          <span>Nome da loja</span>
          <input class="input" value={f.nome} onInput={set('nome')} required maxLength={50} placeholder="Ex.: Brilho da Ana Semijoias" />
        </label>
        <div class="grid2">
          <label class="field">
            <span>WhatsApp com DDD</span>
            <input class="input" type="tel" inputMode="tel" value={f.whatsapp} onInput={set('whatsapp')} required placeholder="(11) 98888-7777" autocomplete="tel" />
          </label>
          <label class="field">
            <span>Instagram (opcional)</span>
            <input class="input" value={f.instagram} onInput={set('instagram')} placeholder="@sualoja" maxLength={31} autocapitalize="off" />
          </label>
        </div>
        <label class="field">
          <span>Frase de apresentação</span>
          <input class="input" value={f.bio} onInput={set('bio')} maxLength={160} placeholder="Semijoias banhadas a ouro 18k · envio para todo o Brasil" />
        </label>
        <label class="field">
          <span>Começo da mensagem do pedido</span>
          <input class="input" value={f.mensagem} onInput={set('mensagem')} maxLength={120} />
        </label>
        <label class="field">
          <span>Endereço da vitrine</span>
          <input class="input" value={slug} onInput={set('slug')} maxLength={40} disabled={publicada} autocapitalize="off" />
          <span class="tiny faint" style="font-weight:500">{lojaUrl(slug || 'sua-loja').replace(/^https?:\/\//, '')}</span>
        </label>

        <div class="stack">
          <div class="row between">
            <h3>Peças na vitrine</h3>
            <span class="small muted">{naVitrine.length} de {pecas.value.length}</span>
          </div>
          {pecas.value.length === 0 && <p class="muted small">Cadastre peças na aba Peças para aparecerem aqui.</p>}
          {pecas.value.map((p) => {
            const semPreco = !(precoFinal(p) > 0);
            const on = p.naVitrine !== false && !semPreco;
            return (
              <div class={`list-item ${on ? '' : 'off'}`} key={p.id}>
                <div class="avatar">
                  <PecaFoto peca={p} />
                </div>
                <div class="grow">
                  <b class="ellipsis" style="display:block">{p.nome}</b>
                  {semPreco ? <span class="small" style="color:var(--warn)">Sem preço: não aparece</span> : <Price peca={p} />}
                </div>
                <button
                  type="button"
                  class={`chip ${p.esgotado ? 'on' : ''}`}
                  aria-pressed={!!p.esgotado}
                  disabled={!on}
                  onClick={() => savePeca({ ...p, esgotado: !p.esgotado })}
                >
                  Esgotado
                </button>
                <label class="switch">
                  <input type="checkbox" checked={on} disabled={semPreco} onChange={() => savePeca({ ...p, naVitrine: !on })} aria-label={`Mostrar ${p.nome} na vitrine`} />
                  <i />
                </label>
              </div>
            );
          })}
        </div>

        <div style="position:sticky;bottom:calc(var(--nav-h) + var(--safe-bottom) + 12px)">
          <button class="btn gold lg block" type="submit" disabled={!!prog}>
            {prog ? <span class="spinner" /> : publicada ? <Send size={18} /> : <Store size={18} />}
            {prog ? prog.msg : publicada ? 'Atualizar vitrine' : 'Publicar vitrine'}
          </button>
        </div>
      </form>

      {publicada && (
        <button class="btn ghost block" style="margin-top:12px" onClick={() => setConfirmDel(true)}>
          <Trash2 size={16} /> Tirar vitrine do ar
        </button>
      )}

      <Sheet open={confirmDel} onClose={() => setConfirmDel(false)} title="Tirar a vitrine do ar?">
        <p class="muted">O link para de funcionar para as clientes. Suas peças continuam no app e você pode publicar de novo quando quiser.</p>
        <div class="grid2" style="margin-top:16px">
          <button class="btn" onClick={() => setConfirmDel(false)}>Cancelar</button>
          <button class="btn rose" onClick={apagar}>Tirar do ar</button>
        </div>
      </Sheet>
    </div>
  );
}
