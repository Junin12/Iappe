// Backup das peças e roteiros (com fotos). Clipes e vídeos exportados ficam de fora: são grandes
// e o vídeo final já fica salvo na galeria quando ela compartilha/salva.
import { db } from '../db.js';
import { getBlob, putBlob, loadAll } from '../store.js';

const toDataUrl = (blob) =>
  new Promise((resolve, reject) => {
    const fr = new FileReader();
    fr.onload = () => resolve(fr.result);
    fr.onerror = () => reject(fr.error);
    fr.readAsDataURL(blob);
  });

export async function exportBackup() {
  const [pecas, videos, settings, radar, ganchos, vitrine] = await Promise.all([
    db.all('pecas'),
    db.all('videos'),
    db.get('kv', 'settings'),
    db.get('kv', 'radar'),
    db.get('kv', 'ganchos'),
    db.get('kv', 'vitrine'),
  ]);
  const pecasOut = [];
  for (const p of pecas) {
    const foto = p.fotoId ? await getBlob(p.fotoId) : null;
    pecasOut.push({ ...p, fotoData: foto ? await toDataUrl(foto) : null });
  }
  const videosOut = videos.map(({ clips, exportBlobId, ...v }) => ({ ...v, clips: {} }));
  const data = { app: 'glowstudio', version: 2, exportedAt: new Date().toISOString(), settings, pecas: pecasOut, videos: videosOut, radar, ganchos, vitrine };
  const blob = new Blob([JSON.stringify(data)], { type: 'application/json' });
  const name = `glowstudio-backup-${new Date().toISOString().slice(0, 10)}.json`;
  return new File([blob], name, { type: 'application/json' });
}

export async function importBackup(file) {
  const data = JSON.parse(await file.text());
  if (data?.app !== 'glowstudio' || !Array.isArray(data.pecas)) throw new Error('Arquivo não é um backup do GlowStudio');
  for (const p of data.pecas) {
    if (typeof p?.id !== 'string' || typeof p.nome !== 'string') continue;
    const { fotoData, ...peca } = p;
    if (fotoData?.startsWith('data:image/')) {
      const blob = await (await fetch(fotoData)).blob();
      peca.fotoId = await putBlob(blob);
    } else {
      delete peca.fotoId;
    }
    await db.put('pecas', peca);
  }
  for (const v of data.videos || []) {
    if (typeof v?.id !== 'string' || !Array.isArray(v.cenas)) continue;
    await db.put('videos', { ...v, clips: {}, exportBlobId: null, status: v.status === 'postado' ? 'postado' : 'roteiro' });
  }
  // Radar, ganchos e vitrine (o token da vitrine permite continuar editando o link num aparelho novo).
  if (Array.isArray(data.radar)) await db.put('kv', data.radar.filter((r) => typeof r?.id === 'string'), 'radar');
  if (Array.isArray(data.ganchos)) await db.put('kv', data.ganchos.filter((g) => typeof g?.texto === 'string'), 'ganchos');
  if (data.vitrine && typeof data.vitrine === 'object') await db.put('kv', data.vitrine, 'vitrine');
  await loadAll();
  return data.pecas.length;
}
