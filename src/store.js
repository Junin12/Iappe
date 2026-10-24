import { signal, computed } from '@preact/signals';
import { db, newId } from './db.js';
import { formatById } from './data/formats.js';
import { hooksFor, captionFor, hashtagsFor } from './data/copy.js';

export const pecas = signal([]);
export const videos = signal([]);
export const settings = signal({ metaSemanal: 3, onboarded: false });
export const ready = signal(false);
export const toast = signal(null);
export const radar = signal([]); // posts da concorrência
export const ganchos = signal([]); // ganchos salvos
export const vitrine = signal({}); // configuração da vitrine pública

export const STATUS = ['ideia', 'roteiro', 'gravado', 'editado', 'postado'];
export const STATUS_LABEL = {
  ideia: 'Ideia',
  roteiro: 'Roteiro',
  gravado: 'Gravado',
  editado: 'Pronto',
  postado: 'Postado',
};

export const DEFAULT_EDIT = {
  look: 'joia',
  ajustes: { brilho: 0, contraste: 0, saturacao: 0, calor: 0 },
  hookStyle: 'tiktok',
  hookPos: 'top',
  mostrarPreco: true,
  cta: 'Link no perfil',
  audio: 'manter',
};

export function notify(msg, kind = 'ok') {
  toast.value = { msg, kind, at: Date.now() };
}

export async function loadAll() {
  const [p, v, s, r, g, vt] = await Promise.all([
    db.all('pecas'),
    db.all('videos'),
    db.get('kv', 'settings'),
    db.get('kv', 'radar'),
    db.get('kv', 'ganchos'),
    db.get('kv', 'vitrine'),
  ]);
  pecas.value = p.sort((a, b) => b.criadoEm - a.criadoEm);
  videos.value = v.sort((a, b) => b.atualizadoEm - a.atualizadoEm);
  if (s) settings.value = { ...settings.value, ...s };
  radar.value = Array.isArray(r) ? r : [];
  ganchos.value = Array.isArray(g) ? g : [];
  vitrine.value = vt || {};
  ready.value = true;
}

export async function saveSettings(patch) {
  settings.value = { ...settings.value, ...patch };
  await db.put('kv', settings.value, 'settings');
}

// ---------- Listas guardadas no chave-valor ----------
// dev-senior: radar e ganchos são listas pequenas (dezenas de itens); se passarem de ~2 mil, mover para um store próprio.
async function putList(sig, key, list) {
  sig.value = list;
  await db.put('kv', list, key);
}

export function saveRadarPost(post) {
  const p = { salvoEm: Date.now(), ...post, id: post.id || newId() };
  return putList(radar, 'radar', [p, ...radar.value.filter((x) => x.id !== p.id)]).then(() => p);
}
export const deleteRadarPost = (id) => putList(radar, 'radar', radar.value.filter((x) => x.id !== id));

export function saveGancho(texto, origem = 'manual') {
  const t = texto.trim();
  if (!t || ganchos.value.some((g) => g.texto.toLowerCase() === t.toLowerCase())) return Promise.resolve(false);
  return putList(ganchos, 'ganchos', [{ id: newId(), texto: t, origem, criadoEm: Date.now() }, ...ganchos.value]).then(() => true);
}
export const deleteGancho = (id) => putList(ganchos, 'ganchos', ganchos.value.filter((x) => x.id !== id));

export async function saveVitrine(patch) {
  vitrine.value = { ...vitrine.value, ...patch };
  await db.put('kv', vitrine.value, 'vitrine');
}

// ---------- Peças ----------
export const pecaById = (id) => pecas.value.find((p) => p.id === id);

export async function savePeca(data) {
  const peca = { criadoEm: Date.now(), ...data, id: data.id || newId() };
  await db.put('pecas', peca);
  const rest = pecas.value.filter((p) => p.id !== peca.id);
  pecas.value = [peca, ...rest].sort((a, b) => b.criadoEm - a.criadoEm);
  return peca;
}

export async function deletePeca(id) {
  const peca = pecaById(id);
  for (const v of videos.value.filter((x) => x.pecaId === id)) await deleteVideo(v.id);
  if (peca?.fotoId) await db.del('blobs', peca.fotoId);
  await db.del('pecas', id);
  pecas.value = pecas.value.filter((p) => p.id !== id);
}

// ---------- Blobs ----------
export async function putBlob(blob) {
  const id = newId();
  await db.put('blobs', blob, id);
  return id;
}
export const getBlob = (id) => (id ? db.get('blobs', id) : Promise.resolve(null));
export const delBlob = (id) => (id ? db.del('blobs', id) : Promise.resolve());

// ---------- Vídeos ----------
export const videoById = (id) => videos.value.find((v) => v.id === id);

export async function saveVideo(video) {
  const v = { ...video, atualizadoEm: Date.now() };
  await db.put('videos', v);
  videos.value = [v, ...videos.value.filter((x) => x.id !== v.id)];
  return v;
}

export async function updateVideo(id, patch) {
  const current = videoById(id);
  if (!current) return null;
  const next = typeof patch === 'function' ? patch(current) : { ...current, ...patch };
  return saveVideo(next);
}

export function newVideoFor(peca, formatoId = 'brilho') {
  const formato = formatById(formatoId);
  const gancho = hooksFor(peca, 1)[0] || '';
  return {
    id: newId(),
    pecaId: peca.id,
    formatoId: formato.id,
    gancho,
    legenda: captionFor(peca, gancho),
    hashtags: hashtagsFor(peca),
    cenas: formato.cenas.map((c) => ({ ...c })),
    clips: {},
    edit: structuredClone(DEFAULT_EDIT),
    status: 'ideia',
    agendadoPara: null,
    postadoEm: null,
    metricas: null,
    criadoEm: Date.now(),
  };
}

export async function deleteVideo(id) {
  const v = videoById(id);
  if (v) {
    for (const c of Object.values(v.clips || {})) await delBlob(c.blobId);
    await delBlob(v.exportBlobId);
  }
  await db.del('videos', id);
  videos.value = videos.value.filter((x) => x.id !== id);
}

// Apaga clipes e o vídeo final, mas mantém o registro (roteiro, data e números).
export async function freeSpace(id) {
  const v = videoById(id);
  if (!v) return;
  for (const c of Object.values(v.clips || {})) await delBlob(c.blobId);
  await delBlob(v.exportBlobId);
  await saveVideo({ ...v, clips: {}, exportBlobId: null });
}

export const clipsDone =(v) => v.cenas.filter((_, i) => v.clips?.[i]).length;
export const allClipsDone = (v) => v.cenas.length > 0 && clipsDone(v) === v.cenas.length;

// ---------- Derivados para a tela inicial ----------
export function startOfWeek(d = new Date()) {
  const x = new Date(d);
  const day = (x.getDay() + 6) % 7; // segunda = 0
  x.setHours(0, 0, 0, 0);
  x.setDate(x.getDate() - day);
  return x;
}

export const postadosSemana = computed(() => {
  const ini = startOfWeek().getTime();
  return videos.value.filter((v) => v.status === 'postado' && (v.postadoEm || 0) >= ini).length;
});

export const rascunhos = computed(() => videos.value.filter((v) => v.status !== 'postado'));
