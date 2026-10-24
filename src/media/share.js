// Compartilhar arquivo (iPhone e Android abrem o menu nativo: TikTok, Salvar vídeo, WhatsApp...).
// Sem suporte, baixa o arquivo.
export async function shareOrDownload(file, title, text) {
  if (navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], title, ...(text ? { text } : {}) });
      return 'shared';
    } catch (e) {
      if (e?.name === 'AbortError') return 'cancelled';
      /* cai no download */
    }
  }
  const url = URL.createObjectURL(file);
  const a = document.createElement('a');
  a.href = url;
  a.download = file.name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
  return 'downloaded';
}

export async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    const ok = document.execCommand('copy');
    ta.remove();
    return ok;
  }
}

// Lembrete no calendário do celular (.ics abre direto no app Calendário do iPhone e Android).
export function icsFile({ title, description, start, minutes = 15 }) {
  const fmt = (d) => d.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
  const esc = (s) => String(s || '').replace(/[\\,;]/g, (m) => '\\' + m).replace(/\n/g, '\\n');
  const s = new Date(start);
  const e = new Date(s.getTime() + minutes * 60000);
  const body = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//GlowStudio//PT-BR',
    'BEGIN:VEVENT',
    `UID:${s.getTime()}-${Math.random().toString(36).slice(2)}@glowstudio`,
    `DTSTAMP:${fmt(new Date())}`,
    `DTSTART:${fmt(s)}`,
    `DTEND:${fmt(e)}`,
    `SUMMARY:${esc(title)}`,
    `DESCRIPTION:${esc(description)}`,
    'BEGIN:VALARM',
    'TRIGGER:-PT10M',
    'ACTION:DISPLAY',
    `DESCRIPTION:${esc(title)}`,
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n');
  return new File([body], 'post-glowstudio.ics', { type: 'text/calendar' });
}
