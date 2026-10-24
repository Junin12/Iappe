// IndexedDB mínimo: peças, vídeos, blobs (clipes, fotos, exportações) e chave-valor.
const DB_NAME = 'glowstudio';
const DB_VERSION = 1;
const STORES = ['pecas', 'videos', 'blobs', 'kv'];

let dbPromise;

function open() {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      for (const name of STORES) {
        if (!db.objectStoreNames.contains(name)) {
          db.createObjectStore(name, name === 'kv' || name === 'blobs' ? undefined : { keyPath: 'id' });
        }
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
  return dbPromise;
}

function run(store, mode, fn) {
  return open().then(
    (db) =>
      new Promise((resolve, reject) => {
        const tx = db.transaction(store, mode);
        const req = fn(tx.objectStore(store));
        tx.oncomplete = () => resolve(req && req.result);
        tx.onerror = () => reject(tx.error);
        tx.onabort = () => reject(tx.error || new Error('Transação cancelada'));
      }),
  );
}

export const db = {
  all: (store) => run(store, 'readonly', (s) => s.getAll()),
  get: (store, key) => run(store, 'readonly', (s) => s.get(key)),
  put: (store, value, key) => run(store, 'readwrite', (s) => (key === undefined ? s.put(value) : s.put(value, key))),
  del: (store, key) => run(store, 'readwrite', (s) => s.delete(key)),
};

export function newId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
}

// Pede ao navegador para não apagar os dados sob pouco espaço (importante no iPhone).
export async function requestPersistence() {
  try {
    if (navigator.storage?.persist && !(await navigator.storage.persisted())) {
      await navigator.storage.persist();
    }
  } catch {
    /* sem suporte: segue sem garantia */
  }
}
