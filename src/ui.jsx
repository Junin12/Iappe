import { useEffect, useRef, useState } from 'preact/hooks';
import { Gem, CircleCheck, Info } from 'lucide-preact';
import { toast, getBlob, STATUS_LABEL } from './store.js';
import { brl } from './data/copy.js';

// Bottom sheet com <dialog> nativo: foco, Esc e acessibilidade de graça.
export function Sheet({ open, onClose, title, children }) {
  const ref = useRef();
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) d.showModal();
    if (!open && d.open) d.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      class="sheet"
      aria-label={title}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()}
    >
      <div class="sheet-body">
        <div class="sheet-grip" />
        {title && <h2 style="margin: 6px 0 16px">{title}</h2>}
        {open && children}
      </div>
    </dialog>
  );
}

export function Ring({ value, max, size = 92, children }) {
  const r = (size - 10) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.min(1, max ? value / max : 0);
  return (
    <div class="ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden="true">
        <defs>
          <linearGradient id="rg" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0" stop-color="#f6dc94" />
            <stop offset="1" stop-color="#b9862a" />
          </linearGradient>
        </defs>
        <circle cx={size / 2} cy={size / 2} r={r} stroke="#24242e" stroke-width="8" fill="none" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          stroke="url(#rg)"
          stroke-width="8"
          fill="none"
          stroke-linecap="round"
          stroke-dasharray={c}
          stroke-dashoffset={c * (1 - pct)}
          style="transition: stroke-dashoffset .6s ease"
        />
      </svg>
      <div class="val">{children}</div>
    </div>
  );
}

export function Empty({ icon: Icon = Gem, title, text, action }) {
  return (
    <div class="empty">
      <div class="ico">
        <Icon size={28} />
      </div>
      <h3>{title}</h3>
      {text && <p class="muted small" style="margin: 6px 0 16px">{text}</p>}
      {action}
    </div>
  );
}

export function ToastHost() {
  const t = toast.value;
  const [visible, setVisible] = useState(false);
  const ref = useRef();
  useEffect(() => {
    if (!t) return;
    setVisible(true);
    const id = setTimeout(() => setVisible(false), 2800);
    return () => clearTimeout(id);
  }, [t?.at]);
  // Popover entra na "top layer": aparece até por cima de um <dialog> aberto.
  useEffect(() => {
    const el = ref.current;
    if (!el?.showPopover) return;
    try {
      el.hidePopover();
    } catch {}
    try {
      el.showPopover();
    } catch {}
  }, [visible, t?.at]);
  if (!t || !visible) return null;
  const Icon = t.kind === 'err' ? Info : CircleCheck;
  return (
    <div ref={ref} popover="manual" class={`toast ${t.kind === 'err' ? 'err' : ''}`} role="status" aria-live="polite">
      <Icon size={18} color={t.kind === 'err' ? 'var(--rose)' : 'var(--ok)'} />
      {t.msg}
    </div>
  );
}

// Mostra uma foto guardada no IndexedDB.
export function useBlobUrl(blobId) {
  const [url, setUrl] = useState(null);
  useEffect(() => {
    let u;
    let alive = true;
    getBlob(blobId).then((b) => {
      if (alive && b) {
        u = URL.createObjectURL(b);
        setUrl(u);
      }
    });
    return () => {
      alive = false;
      if (u) URL.revokeObjectURL(u);
      setUrl(null);
    };
  }, [blobId]);
  return url;
}

export function PecaFoto({ peca, size = 22 }) {
  const url = useBlobUrl(peca?.fotoId);
  return url ? <img src={url} alt={peca?.nome || ''} loading="lazy" /> : <Gem size={size} aria-hidden="true" />;
}

export function Price({ peca }) {
  if (!peca) return null;
  const promo = peca.precoPromo && peca.precoPromo < peca.preco;
  return (
    <span class="price">
      {brl(promo ? peca.precoPromo : peca.preco)}
      {promo && <s>{brl(peca.preco)}</s>}
    </span>
  );
}

export const StatusBadge = ({ status }) => <span class={`badge ${status}`}>{STATUS_LABEL[status] || status}</span>;

export function Steps({ total, current }) {
  return (
    <div class="steps" aria-label={`Passo ${current + 1} de ${total}`}>
      {Array.from({ length: total }, (_, i) => (
        <i class={i < current ? 'done' : i === current ? 'cur' : ''} />
      ))}
    </div>
  );
}

export function haptic(ms = 12) {
  try {
    navigator.vibrate?.(ms);
  } catch {
    /* iPhone não vibra pela web */
  }
}
