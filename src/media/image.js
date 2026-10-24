// Reduz fotos antes de guardar (fotos de celular têm 4–12 MB; aqui ficam ~150 KB).
export async function downscale(file, max = 1280, quality = 0.85) {
  const bitmap = await loadBitmap(file);
  const scale = Math.min(1, max / Math.max(bitmap.width, bitmap.height));
  const w = Math.round(bitmap.width * scale);
  const h = Math.round(bitmap.height * scale);
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  canvas.getContext('2d').drawImage(bitmap, 0, 0, w, h);
  bitmap.close?.();
  return new Promise((resolve) => canvas.toBlob((b) => resolve(b || file), 'image/jpeg', quality));
}

async function loadBitmap(file) {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(file, { imageOrientation: 'from-image' });
    } catch {
      /* Safari antigo: cai no <img> */
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();
    return img;
  } finally {
    URL.revokeObjectURL(url);
  }
}

// Miniatura de um clipe de vídeo (primeiro quadro útil) como data URL leve.
export function videoThumb(blob, atSec = 0.3) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(blob);
    const v = document.createElement('video');
    v.muted = true;
    v.playsInline = true;
    v.preload = 'auto';
    v.src = url;
    const done = (val) => {
      URL.revokeObjectURL(url);
      resolve(val);
    };
    v.onloadeddata = () => {
      v.currentTime = Math.min(atSec, (v.duration || 1) / 2);
    };
    v.onseeked = () => {
      const c = document.createElement('canvas');
      const w = 180;
      c.width = w;
      c.height = Math.round(w * 16 / 9);
      const ctx = c.getContext('2d');
      const vr = v.videoWidth / v.videoHeight;
      const cr = c.width / c.height;
      let sw = v.videoWidth, sh = v.videoHeight, sx = 0, sy = 0;
      if (vr > cr) { sw = sh * cr; sx = (v.videoWidth - sw) / 2; } else { sh = sw / cr; sy = (v.videoHeight - sh) / 2; }
      ctx.drawImage(v, sx, sy, sw, sh, 0, 0, c.width, c.height);
      done(c.toDataURL('image/jpeg', 0.7));
    };
    v.onerror = () => done(null);
    setTimeout(() => done(null), 8000);
  });
}

// Duração real de um clipe (webm do Chrome vem sem duração: força a leitura).
export function videoDuration(blob) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(blob);
    const v = document.createElement('video');
    v.preload = 'metadata';
    v.muted = true;
    v.src = url;
    const finish = (d) => {
      URL.revokeObjectURL(url);
      resolve(Number.isFinite(d) && d > 0 ? Math.round(d * 1000) : 0);
    };
    v.onloadedmetadata = () => {
      if (Number.isFinite(v.duration)) return finish(v.duration);
      v.currentTime = 1e7;
      v.ontimeupdate = () => {
        v.ontimeupdate = null;
        finish(v.duration);
      };
    };
    v.onerror = () => finish(0);
    setTimeout(() => finish(0), 8000);
  });
}
