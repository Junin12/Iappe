import { useState } from 'preact/hooks';
import { Radar as RadarIcon, Store, CalendarHeart, Bookmark, ChartColumn, ChevronRight, ExternalLink, Trash2, Copy, Plus, BellRing, Sparkles } from 'lucide-preact';
import { go, back } from '../router.js';
import { radar, ganchos, vitrine, saveVideo, newVideoFor, saveGancho, deleteGancho, notify } from '../store.js';
import { Sheet, PecaPicker, SubHeader, Empty } from '../ui.jsx';
import { proximasDatas } from '../data/datas.js';
import { formatById } from '../data/formats.js';
import { copyText, icsFile, shareOrDownload } from '../media/share.js';

// Cria o rascunho já com formato e gancho e abre o roteiro.
export async function criarVideo(peca, formatoId, gancho) {
  const v = newVideoFor(peca, formatById(formatoId).id);
  await saveVideo(gancho ? { ...v, gancho } : v);
  go(`/criar/${v.id}/roteiro`);
}

const GARIMPO = [
  ['TikTok: #semijoias', 'Os vídeos mais vistos da hashtag', 'https://www.tiktok.com/tag/semijoias'],
  ['Instagram: #semijoias', 'Posts e reels em destaque', 'https://www.instagram.com/explore/tags/semijoias/'],
  ['TikTok Creative Center', 'Hashtags, sons e anúncios em alta no Brasil (oficial e grátis)', 'https://ads.tiktok.com/creative/creativeCenter/trends'],
];

export function Ferramentas() {
  const prox = proximasDatas(new Date(), 1)[0];
  const vt = vitrine.value;
  const tools = [
    { icon: RadarIcon, title: 'Radar da concorrência', text: radar.value.length ? `${radar.value.length} posts no ranking` : 'Ranking dos posts que mais bombam no seu nicho', path: '/radar', hot: true },
    { icon: Store, title: 'Vitrine com WhatsApp', text: vt.publicadaEm ? 'No ar: toque para atualizar' : 'Seu link na bio com pedido pronto', path: '/vitrine' },
    { icon: CalendarHeart, title: 'Calendário de vendas', text: prox ? `${prox.emoji} ${prox.nome} em ${prox.dias} ${prox.dias === 1 ? 'dia' : 'dias'}` : 'Datas que mais vendem', path: '/datas' },
    { icon: Bookmark, title: 'Ganchos salvos', text: ganchos.value.length ? `${ganchos.value.length} ganchos guardados` : 'Seu banco de frases que prendem', path: '/ganchos' },
    { icon: ChartColumn, title: 'Meus resultados', text: 'Views, vendas e o que funciona para você', path: '/resultados' },
  ];
  return (
    <div class="screen">
      <div class="header">
        <div class="grow">
          <span class="eyebrow">Para vender mais</span>
          <h1>Ferramentas</h1>
        </div>
      </div>
      <div class="stack">
        {tools.map((t) => (
          <button class={`card press row tool-card ${t.hot ? 'glow' : ''}`} onClick={() => go(t.path)}>
            <span class="tool-ico">
              <t.icon size={24} />
            </span>
            <div class="grow">
              <b>{t.title}</b>
              <p class="muted small" style="margin:2px 0 0">{t.text}</p>
            </div>
            <ChevronRight size={18} class="faint" />
          </button>
        ))}
      </div>

      <div class="section-title">
        <h2>Onde garimpar tendências</h2>
      </div>
      <div class="stack">
        {GARIMPO.map(([t, d, url]) => (
          <a class="list-item" href={url} target="_blank" rel="noopener" style="text-decoration:none;color:inherit">
            <ExternalLink size={18} class="gold" />
            <div class="grow">
              <b>{t}</b>
              <p class="muted small" style="margin:2px 0 0">{d}</p>
            </div>
          </a>
        ))}
        <p class="faint tiny" style="margin:0">Achou um post que bombou? Copie o link e cole no Radar.</p>
      </div>
    </div>
  );
}

// ---------- Calendário de vendas ----------
export function Datas() {
  const [pick, setPick] = useState(null);
  const datas = proximasDatas(new Date(), 10);

  const lembrete = (d) => {
    const ini = new Date(d.data.getTime() - d.aviso * 864e5);
    ini.setHours(9, 0, 0, 0);
    if (ini < new Date()) ini.setTime(Date.now() + 3600e3);
    shareOrDownload(icsFile({ title: `Começar vídeos de ${d.nome} ✨`, description: `${d.dica}\nGancho: ${d.gancho}`, start: ini }), 'Lembrete');
  };

  return (
    <div class="screen">
      <SubHeader title="Calendário de vendas" eyebrow="Ferramentas" back={() => back('/ferramentas')} />
      <p class="muted small" style="margin:-6px 0 16px">Poste antes da data: o vídeo leva dias para circular. O app avisa na tela inicial quando chegar a hora.</p>
      <div class="stack">
        {datas.map((d) => {
          const agora = d.dias <= d.aviso;
          return (
            <div class={`card stack ${agora ? 'glow' : ''}`} style="gap:10px" key={d.chave}>
              <div class="row">
                <span class="tool-ico" style="font-size:1.5rem">{d.emoji}</span>
                <div class="grow">
                  <b>{d.nome}</b>
                  <p class="muted small" style="margin:2px 0 0">
                    {d.data.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}
                  </p>
                </div>
                <span class={`badge ${agora ? 'promo' : ''}`}>{d.dias === 0 ? 'Hoje' : agora ? `Postar já · ${d.dias}d` : `${d.dias} dias`}</span>
              </div>
              <p class="small" style="margin:0;line-height:1.4">{d.dica}</p>
              <p class="small muted" style="margin:0">
                {formatById(d.formato).emoji} {formatById(d.formato).nome} · <i>“{d.gancho}”</i>
              </p>
              <div class="row" style="gap:8px">
                <button class={`btn sm grow ${agora ? 'gold' : ''}`} onClick={() => setPick(d)}>
                  <Sparkles size={16} /> Criar vídeo
                </button>
                <button class="btn sm ghost" onClick={() => lembrete(d)} aria-label={`Lembrete para ${d.nome}`}>
                  <BellRing size={16} /> Lembrete
                </button>
              </div>
            </div>
          );
        })}
      </div>
      <Sheet open={!!pick} onClose={() => setPick(null)} title={pick ? `Vídeo para ${pick.nome}` : ''}>
        {pick && <PecaPicker onPick={(p) => criarVideo(p, pick.formato, pick.gancho)} />}
      </Sheet>
    </div>
  );
}

// ---------- Ganchos salvos ----------
export function Ganchos() {
  const [txt, setTxt] = useState('');
  const add = async (e) => {
    e.preventDefault();
    if (await saveGancho(txt)) {
      setTxt('');
      notify('Gancho salvo');
    } else if (txt.trim()) notify('Esse gancho já está salvo', 'err');
  };
  return (
    <div class="screen">
      <SubHeader title="Ganchos salvos" eyebrow="Ferramentas" back={() => back('/ferramentas')} />
      <p class="muted small" style="margin:-6px 0 16px">Frases que prendem nos primeiros segundos. Eles aparecem como opção na hora de montar o roteiro.</p>
      <form class="row" style="gap:8px;margin-bottom:16px" onSubmit={add}>
        <label class="grow">
          <span class="sr-only">Novo gancho</span>
          <input class="input" value={txt} maxLength={70} placeholder="Ex.: Ninguém acredita no preço desse colar" onInput={(e) => setTxt(e.currentTarget.value)} />
        </label>
        <button class="icon-btn on" style="width:50px;height:50px" aria-label="Salvar gancho" disabled={!txt.trim()}>
          <Plus size={22} />
        </button>
      </form>
      {ganchos.value.length === 0 ? (
        <Empty icon={Bookmark} title="Nenhum gancho ainda" text="Salve ganchos do Radar da concorrência ou escreva os seus." />
      ) : (
        <div class="stack">
          {ganchos.value.map((g) => (
            <div class="list-item" key={g.id}>
              <div class="grow">
                <span style="font-family:var(--serif);font-size:1.05rem;line-height:1.3">“{g.texto}”</span>
                {g.origem === 'radar' && <span class="badge" style="margin-left:8px">Radar</span>}
              </div>
              <button class="icon-btn" aria-label="Copiar gancho" onClick={async () => notify((await copyText(g.texto)) ? 'Copiado' : 'Não consegui copiar')}>
                <Copy size={16} />
              </button>
              <button class="icon-btn" aria-label="Apagar gancho" onClick={() => deleteGancho(g.id)}>
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
