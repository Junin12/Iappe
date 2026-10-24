// Compositor WebGL: um único passe por quadro (cor + vinheta + sobreposição de texto/preço).
// O mesmo canvas serve para a prévia e para a exportação, então o que ela vê é o que sai no vídeo.
export const OUT_W = 1080;
export const OUT_H = 1920;

export const LOOKS = {
  natural: { nome: 'Natural', cor: '#9a9aa5', v: { brilho: 0, contraste: 0, saturacao: 0, calor: 0, vinheta: 0 } },
  joia: { nome: 'Brilho de Joia', cor: 'linear-gradient(135deg,#f6dc94,#e8b84f)', v: { brilho: 0.03, contraste: 0.16, saturacao: 0.22, calor: 0.18, vinheta: 0.25 } },
  dourado: { nome: 'Dourado', cor: 'linear-gradient(135deg,#ffd27a,#c27c1b)', v: { brilho: 0.02, contraste: 0.1, saturacao: 0.15, calor: 0.4, vinheta: 0.2 } },
  prata: { nome: 'Prata', cor: 'linear-gradient(135deg,#eef2f7,#8a94a6)', v: { brilho: 0.04, contraste: 0.15, saturacao: -0.12, calor: -0.3, vinheta: 0.2 } },
  suave: { nome: 'Suave', cor: 'linear-gradient(135deg,#f7d9e3,#c9a6b4)', v: { brilho: 0.07, contraste: -0.1, saturacao: -0.05, calor: 0.08, vinheta: 0 } },
  pb: { nome: 'P&B', cor: 'linear-gradient(135deg,#fff,#222)', v: { brilho: 0.02, contraste: 0.22, saturacao: -1, calor: 0, vinheta: 0.3 } },
};

export function finalParams(edit) {
  const base = (LOOKS[edit.look] || LOOKS.natural).v;
  const a = edit.ajustes || {};
  const c = (x, lo, hi) => Math.max(lo, Math.min(hi, x));
  return {
    brilho: c(base.brilho + (a.brilho || 0), -0.4, 0.4),
    contraste: c(base.contraste + (a.contraste || 0), -0.6, 0.8),
    saturacao: c(base.saturacao + (a.saturacao || 0), -1, 1),
    calor: c(base.calor + (a.calor || 0), -1, 1),
    vinheta: base.vinheta,
  };
}

const VS = `attribute vec2 p; varying vec2 uv;
void main(){ uv = p * 0.5 + 0.5; gl_Position = vec4(p, 0.0, 1.0); }`;

const FS = `precision mediump float;
varying vec2 uv;
uniform sampler2D vid; uniform sampler2D ovl;
uniform vec4 crop; uniform float bright, contrast, sat, warm, vign, hasOvl;
void main(){
  vec3 c = texture2D(vid, crop.zw + uv * crop.xy).rgb;
  c += bright;
  c = (c - 0.5) * (1.0 + contrast) + 0.5;
  float l = dot(c, vec3(0.299, 0.587, 0.114));
  c = mix(vec3(l), c, 1.0 + sat);
  c += vec3(0.09, 0.025, -0.09) * warm;
  c *= 1.0 - vign * smoothstep(0.38, 0.9, distance(uv, vec2(0.5)));
  c = clamp(c, 0.0, 1.0);
  if (hasOvl > 0.5) { vec4 o = texture2D(ovl, uv); c = c * (1.0 - o.a) + o.rgb; }
  gl_FragColor = vec4(c, 1.0);
}`;

export function createRenderer(canvas) {
  canvas.width = OUT_W;
  canvas.height = OUT_H;
  const gl = canvas.getContext('webgl', { alpha: false, antialias: false, premultipliedAlpha: false, preserveDrawingBuffer: true }); // true: a exportação quadro a quadro lê o canvas depois de desenhar
  if (!gl) throw new Error('Este aparelho não tem WebGL, necessário para editar vídeos.');

  const sh = (type, src) => {
    const s = gl.createShader(type);
    gl.shaderSource(s, src);
    gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  };
  const prog = gl.createProgram();
  gl.attachShader(prog, sh(gl.VERTEX_SHADER, VS));
  gl.attachShader(prog, sh(gl.FRAGMENT_SHADER, FS));
  gl.linkProgram(prog);
  gl.useProgram(prog);

  const buf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
  const loc = gl.getAttribLocation(prog, 'p');
  gl.enableVertexAttribArray(loc);
  gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);

  const tex = (unit) => {
    const t = gl.createTexture();
    gl.activeTexture(gl.TEXTURE0 + unit);
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array([0, 0, 0, 0]));
    return t;
  };
  const vidTex = tex(0);
  const ovlTex = tex(1);
  const u = (n) => gl.getUniformLocation(prog, n);
  gl.uniform1i(u('vid'), 0);
  gl.uniform1i(u('ovl'), 1);
  gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
  gl.viewport(0, 0, OUT_W, OUT_H);

  let overlay = null;

  return {
    setOverlay(canvasOrNull) {
      overlay = canvasOrNull;
      if (!overlay) return;
      gl.activeTexture(gl.TEXTURE1);
      gl.bindTexture(gl.TEXTURE_2D, ovlTex);
      gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, true);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, overlay);
      gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    },
    // src: <video> (prévia) ou canvas já decodificado (exportação rápida).
    draw(video, params) {
      const vw = video.videoWidth || video.width;
      const vh = video.videoHeight || video.height;
      if (!vw || !vh) return false;
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, vidTex);
      try {
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, video);
      } catch {
        return false;
      }
      // "cover": preenche 9:16 cortando as sobras, como o TikTok faz.
      const target = OUT_W / OUT_H;
      const a = vw / vh;
      let sx = 1, sy = 1;
      if (a > target) sx = target / a;
      else sy = a / target;
      gl.uniform4f(u('crop'), sx, sy, (1 - sx) / 2, (1 - sy) / 2);
      gl.uniform1f(u('bright'), params.brilho);
      gl.uniform1f(u('contrast'), params.contraste);
      gl.uniform1f(u('sat'), params.saturacao);
      gl.uniform1f(u('warm'), params.calor);
      gl.uniform1f(u('vign'), params.vinheta);
      gl.uniform1f(u('hasOvl'), overlay ? 1 : 0);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
      return true;
    },
    destroy() {
      gl.getExtension('WEBGL_lose_context')?.loseContext();
    },
  };
}

// ---------- Sobreposições (desenhadas uma vez, reaproveitadas a cada quadro) ----------
const HOOK_Y = { top: 0.15, center: 0.42, bottom: 0.6 };

function wrap(ctx, text, maxW) {
  const words = text.split(/\s+/).filter(Boolean);
  const lines = [];
  let line = '';
  for (const w of words) {
    const test = line ? line + ' ' + w : w;
    if (ctx.measureText(test).width > maxW && line) {
      lines.push(line);
      line = w;
    } else line = test;
  }
  if (line) lines.push(line);
  return lines.slice(0, 4);
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

const SANS = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
const SERIF = '"Playfair Display", Georgia, serif';

export async function ensureFonts() {
  try {
    await Promise.all([document.fonts.load(`700 72px ${SERIF}`), document.fonts.load(`600 72px ${SERIF}`)]);
  } catch {
    /* usa a fonte de reserva */
  }
}

export function drawHook(text, style, pos) {
  const c = document.createElement('canvas');
  c.width = OUT_W;
  c.height = OUT_H;
  const ctx = c.getContext('2d');
  if (!text?.trim()) return c;
  const size = style === 'gold' ? 76 : style === 'outline' ? 80 : 66;
  ctx.font = style === 'gold' ? `700 ${size}px ${SERIF}` : `800 ${size}px ${SANS}`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  const lines = wrap(ctx, text.trim(), 820);
  const lh = size * 1.22;
  const blockH = lines.length * lh;
  const cy = OUT_H * (HOOK_Y[pos] || HOOK_Y.top) + blockH / 2;
  const maxLine = Math.max(...lines.map((l) => ctx.measureText(l).width));

  if (style === 'tiktok') {
    // Caixa branca por linha, como o texto nativo do TikTok.
    lines.forEach((l, i) => {
      const w = ctx.measureText(l).width + 44;
      const y = cy - blockH / 2 + i * lh;
      ctx.fillStyle = '#ffffff';
      roundRect(ctx, OUT_W / 2 - w / 2, y + 2, w, lh - 4, 18);
      ctx.fill();
      ctx.fillStyle = '#111111';
      ctx.fillText(l, OUT_W / 2, y + lh / 2 + 2);
    });
  } else if (style === 'gold') {
    const pad = 46;
    const w = maxLine + pad * 2;
    const h = blockH + pad * 1.2;
    const x = OUT_W / 2 - w / 2;
    const y = cy - blockH / 2 - pad * 0.6;
    ctx.fillStyle = 'rgba(12,12,16,0.78)';
    roundRect(ctx, x, y, w, h, 28);
    ctx.fill();
    ctx.lineWidth = 3;
    const g = ctx.createLinearGradient(x, y, x + w, y + h);
    g.addColorStop(0, '#f6dc94');
    g.addColorStop(0.5, '#e8b84f');
    g.addColorStop(1, '#b9862a');
    ctx.strokeStyle = g;
    ctx.stroke();
    ctx.fillStyle = g;
    lines.forEach((l, i) => ctx.fillText(l, OUT_W / 2, cy - blockH / 2 + i * lh + lh / 2));
  } else {
    ctx.lineJoin = 'round';
    ctx.lineWidth = 14;
    ctx.strokeStyle = '#000000';
    ctx.fillStyle = '#ffffff';
    lines.forEach((l, i) => {
      const y = cy - blockH / 2 + i * lh + lh / 2;
      ctx.strokeText(l, OUT_W / 2, y);
      ctx.fillText(l, OUT_W / 2, y);
    });
  }
  return c;
}

export function drawEnd({ price, oldPrice, cta, showPrice }) {
  const c = document.createElement('canvas');
  c.width = OUT_W;
  c.height = OUT_H;
  const ctx = c.getContext('2d');
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  let y = OUT_H * 0.55;

  if (showPrice && price) {
    ctx.font = `700 118px ${SERIF}`;
    const pw = ctx.measureText(price).width;
    const boxW = Math.max(pw + 120, 520);
    const boxH = oldPrice ? 250 : 190;
    const x = OUT_W / 2 - boxW / 2;
    ctx.fillStyle = 'rgba(12,12,16,0.8)';
    roundRect(ctx, x, y - boxH / 2, boxW, boxH, 36);
    ctx.fill();
    const g = ctx.createLinearGradient(x, y - boxH / 2, x + boxW, y + boxH / 2);
    g.addColorStop(0, '#f6dc94');
    g.addColorStop(0.5, '#e8b84f');
    g.addColorStop(1, '#b9862a');
    ctx.strokeStyle = g;
    ctx.lineWidth = 3;
    ctx.stroke();
    if (oldPrice) {
      ctx.font = `600 50px ${SANS}`;
      ctx.fillStyle = 'rgba(255,255,255,0.6)';
      const oy = y - 62;
      ctx.fillText(oldPrice, OUT_W / 2, oy);
      const ow = ctx.measureText(oldPrice).width;
      ctx.fillRect(OUT_W / 2 - ow / 2 - 6, oy, ow + 12, 4);
    }
    ctx.font = `700 118px ${SERIF}`;
    ctx.fillStyle = g;
    ctx.fillText(price, OUT_W / 2, y + (oldPrice ? 40 : 4));
    y += boxH / 2 + 80;
  }

  if (cta?.trim()) {
    ctx.font = `800 54px ${SANS}`;
    const t = cta.trim().slice(0, 32);
    const w = ctx.measureText(t).width + 80;
    const g = ctx.createLinearGradient(OUT_W / 2 - w / 2, 0, OUT_W / 2 + w / 2, 0);
    g.addColorStop(0, '#ff8fb0');
    g.addColorStop(1, '#e0336b');
    ctx.fillStyle = g;
    roundRect(ctx, OUT_W / 2 - w / 2, y - 50, w, 100, 50);
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    ctx.fillText(t, OUT_W / 2, y + 2);
  }
  return c;
}

// Legenda da narração: texto grande e curto, branco com contorno, fora da área dos botões do TikTok.
const CAPTION_Y = { baixo: 0.7, alto: 0.3 };

export function drawCaption(ctx, text, pos = 'baixo') {
  if (!text) return;
  ctx.save();
  ctx.font = `900 74px ${SANS}`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.lineJoin = 'round';
  const lines = wrap(ctx, text, 900);
  const lh = 88;
  const y0 = OUT_H * (CAPTION_Y[pos] || CAPTION_Y.baixo) - ((lines.length - 1) * lh) / 2;
  lines.forEach((l, i) => {
    const y = y0 + i * lh;
    ctx.lineWidth = 16;
    ctx.strokeStyle = 'rgba(0,0,0,0.9)';
    ctx.strokeText(l, OUT_W / 2, y);
    ctx.fillStyle = '#ffffff';
    ctx.fillText(l, OUT_W / 2, y);
  });
  ctx.restore();
}

// Junta a sobreposição fixa (gancho/preço) com a legenda do momento num canvas só (o compositor tem uma camada).
export function createComposer() {
  const c = document.createElement('canvas');
  c.width = OUT_W;
  c.height = OUT_H;
  const ctx = c.getContext('2d');
  return (base, caption, pos) => {
    if (!caption) return base;
    ctx.clearRect(0, 0, OUT_W, OUT_H);
    if (base) ctx.drawImage(base, 0, 0);
    drawCaption(ctx, caption, pos);
    return c;
  };
}
