<?php
// GlowStudio — vitrine pública (link na bio). A cliente escolhe as peças e o pedido sai pronto no WhatsApp.
declare(strict_types=1);
require __DIR__ . '/api/_lib.php';

$slug = (string)($_GET['l'] ?? '');
$data = null;
if (preg_match('/^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$/', $slug) && is_file("$dataDir/vitrines/$slug/data.json")) {
    $data = json_decode((string)file_get_contents("$dataDir/vitrines/$slug/data.json"), true);
}
$base = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/glowstudio/loja.php')), '/');
$host = preg_match('/^[a-z0-9.-]+$/i', $_SERVER['HTTP_HOST'] ?? '') ? $_SERVER['HTTP_HOST'] : 'wiapps.com.br';
$h = fn($v) => htmlspecialchars((string)$v, ENT_QUOTES | ENT_HTML5, 'UTF-8');

header('Content-Type: text/html; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header("Content-Security-Policy: default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; base-uri 'none'; form-action 'none'");
header('Referrer-Policy: strict-origin-when-cross-origin');

if (!is_array($data)) {
    http_response_code(404);
    echo '<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Vitrine não encontrada</title>'
        . '<body style="font-family:system-ui;background:#0b0b0f;color:#f6f2ea;display:grid;place-items:center;min-height:100vh;margin:0;text-align:center;padding:16px">'
        . '<div><h1 style="font-family:Georgia,serif">Vitrine não encontrada</h1><p style="color:#a8a3b4">Confira o link com a loja.</p></div></body></html>';
    exit;
}
header('Cache-Control: public, max-age=60');

$loja = $data['loja'];
$pecas = array_values(array_filter($data['pecas'] ?? [], 'is_array'));
$img = fn($p) => $p['foto'] ? "$base/api/vitrine.php?img=" . rawurlencode($slug) . '/' . $p['id'] . '_' . $p['foto'] . '.jpg' : '';
$brl = fn($v) => 'R$ ' . number_format((float)$v, 2, ',', '.');
$first = null;
foreach ($pecas as $p) if ($p['foto']) { $first = $p; break; }
$tipos = array_values(array_unique(array_filter(array_map(fn($p) => $p['tipo'], $pecas))));
$public = [
    'whatsapp' => $loja['whatsapp'],
    'mensagem' => $loja['mensagem'] ?? '',
    'pecas' => array_map(fn($p) => ['id' => $p['id'], 'nome' => $p['nome'], 'preco' => $p['precoPromo'] ?: $p['preco'], 'esgotado' => $p['esgotado']], $pecas),
];
?><!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0B0B0F">
<title><?= $h($loja['nome']) ?></title>
<meta name="description" content="<?= $h($loja['bio'] ?: 'Semijoias com pedido direto pelo WhatsApp') ?>">
<meta property="og:title" content="<?= $h($loja['nome']) ?>">
<meta property="og:description" content="<?= $h($loja['bio'] ?: count($pecas) . ' peças · peça pelo WhatsApp') ?>">
<?php if ($first): ?><meta property="og:image" content="https://<?= $h($host . $img($first)) ?>"><?php endif; ?>
<meta property="og:type" content="website">
<link rel="icon" href="<?= $h($base) ?>/icons/icon.svg" type="image/svg+xml">
<style>
:root{--bg:#0b0b0f;--card:#16161d;--line:#2a2a35;--text:#f6f2ea;--muted:#a8a3b4;--gold:#e8b84f;--gold2:#f6dc94;--rose:#ff5c8a;--wa:#25d366;--grad:linear-gradient(135deg,#f6dc94,#e8b84f 45%,#b9862a)}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
body{margin:0;background:var(--bg);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;-webkit-font-smoothing:antialiased}
.wrap{max-width:980px;margin:0 auto;padding:calc(env(safe-area-inset-top) + 28px) 16px 120px}
header{text-align:center;margin-bottom:22px}
.logo{width:64px;height:64px;border-radius:20px;margin:0 auto 12px;background:var(--grad);display:grid;place-items:center;font:700 1.8rem Georgia,serif;color:#1a1406}
h1{font:700 2rem/1.1 Georgia,'Times New Roman',serif;margin:0;background:var(--grad);-webkit-background-clip:text;background-clip:text;color:transparent}
.bio{color:var(--muted);margin:8px auto 0;max-width:46ch;line-height:1.45}
.ig{display:inline-block;margin-top:10px;color:var(--gold2);font-weight:600;text-decoration:none}
.chips{display:flex;gap:8px;overflow-x:auto;padding:2px 0 14px;scrollbar-width:none}
.chips::-webkit-scrollbar{display:none}
.chip{flex:none;min-height:38px;padding:0 16px;border-radius:99px;border:1px solid var(--line);background:var(--card);color:var(--muted);font:600 .9rem inherit;cursor:pointer}
.chip[aria-pressed=true]{border-color:var(--gold);color:var(--gold2);background:rgba(232,184,79,.12)}
.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}
@media(min-width:700px){.grid{grid-template-columns:repeat(3,1fr)}}
@media(min-width:920px){.grid{grid-template-columns:repeat(4,1fr)}}
.card{background:var(--card);border:1px solid var(--line);border-radius:18px;overflow:hidden;display:flex;flex-direction:column}
.ph{aspect-ratio:1;background:#1e1e27;position:relative}
.ph img{width:100%;height:100%;object-fit:cover;display:block}
.tag{position:absolute;top:8px;left:8px;padding:3px 9px;border-radius:99px;font-size:.72rem;font-weight:800;background:var(--rose);color:#fff}
.tag.off{background:#000a;color:var(--muted)}
.info{padding:10px 12px 12px;display:flex;flex-direction:column;gap:6px;flex:1}
.nome{font-weight:700;line-height:1.25}
.det{color:var(--muted);font-size:.78rem}
.preco{font-weight:800;color:var(--gold2)}
.preco s{color:#6f6a7c;font-weight:500;font-size:.8em;margin-left:6px}
.add{margin-top:auto;min-height:42px;border-radius:12px;border:1px solid var(--line);background:#1e1e27;color:var(--text);font:700 .9rem inherit;cursor:pointer}
.add[aria-pressed=true]{background:var(--grad);color:#1a1406;border:0}
.add:disabled{opacity:.45}
.bag{position:fixed;left:0;right:0;bottom:0;padding:12px 16px calc(env(safe-area-inset-bottom) + 12px);background:linear-gradient(to top,#0b0b0f 70%,#0b0b0f00);display:none}
.bag.on{display:block}
.bag a{max-width:560px;margin:0 auto;display:flex;align-items:center;justify-content:center;gap:10px;min-height:56px;border-radius:16px;background:var(--wa);color:#05290f;font-weight:800;text-decoration:none;font-size:1.02rem;box-shadow:0 10px 30px #25d36640}
.vazio{color:var(--muted);text-align:center;padding:40px 0}
footer{text-align:center;color:#6f6a7c;font-size:.75rem;margin-top:30px}
:focus-visible{outline:2px solid var(--gold);outline-offset:2px}
</style>
</head>
<body>
<div class="wrap">
  <header>
    <div class="logo" aria-hidden="true"><?= $h(mb_strtoupper(mb_substr($loja['nome'], 0, 1))) ?></div>
    <h1><?= $h($loja['nome']) ?></h1>
    <?php if ($loja['bio']): ?><p class="bio"><?= $h($loja['bio']) ?></p><?php endif; ?>
    <?php if ($loja['instagram']): ?><a class="ig" href="https://instagram.com/<?= $h($loja['instagram']) ?>" rel="noopener" target="_blank">@<?= $h($loja['instagram']) ?></a><?php endif; ?>
  </header>

  <?php if (count($tipos) > 1): ?>
  <div class="chips" role="group" aria-label="Filtrar por tipo">
    <button class="chip" aria-pressed="true" data-tipo="">Tudo</button>
    <?php foreach ($tipos as $t): ?><button class="chip" aria-pressed="false" data-tipo="<?= $h($t) ?>"><?= $h($t) ?></button><?php endforeach; ?>
  </div>
  <?php endif; ?>

  <?php if (!$pecas): ?><p class="vazio">Novidades em breve ✨</p><?php endif; ?>
  <div class="grid">
    <?php foreach ($pecas as $p): $promo = $p['precoPromo'] > 0; ?>
    <article class="card" data-tipo="<?= $h($p['tipo']) ?>">
      <div class="ph">
        <?php if ($p['foto']): ?><img src="<?= $h($img($p)) ?>" alt="<?= $h($p['nome']) ?>" loading="lazy"><?php endif; ?>
        <?php if ($p['esgotado']): ?><span class="tag off">Esgotado</span><?php elseif ($promo): ?><span class="tag">Promo</span><?php endif; ?>
      </div>
      <div class="info">
        <span class="nome"><?= $h($p['nome']) ?></span>
        <span class="det"><?= $h(implode(' · ', array_filter([$p['banho'], $p['pedra'] !== 'Nenhuma' ? $p['pedra'] : '']))) ?></span>
        <?php if ($p['desc']): ?><span class="det"><?= $h($p['desc']) ?></span><?php endif; ?>
        <span class="preco"><?= $h($brl($promo ? $p['precoPromo'] : $p['preco'])) ?><?php if ($promo): ?><s><?= $h($brl($p['preco'])) ?></s><?php endif; ?></span>
        <button class="add" data-id="<?= $h($p['id']) ?>" aria-pressed="false" <?= $p['esgotado'] ? 'disabled' : '' ?>><?= $p['esgotado'] ? 'Esgotado' : 'Quero esta' ?></button>
      </div>
    </article>
    <?php endforeach; ?>
  </div>
  <footer>Pedido direto pelo WhatsApp · feito com GlowStudio</footer>
</div>
<div class="bag" id="bag"><a id="bag-link" href="#" target="_blank" rel="noopener"></a></div>
<script type="application/json" id="dados"><?= json_encode($public, JSON_UNESCAPED_UNICODE | JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT) ?></script>
<script>
(function () {
  var D = JSON.parse(document.getElementById('dados').textContent);
  var byId = {};
  D.pecas.forEach(function (p) { byId[p.id] = p; });
  var bag = [];
  var brl = function (v) { return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }); };
  var bagEl = document.getElementById('bag');
  var link = document.getElementById('bag-link');

  function render() {
    bagEl.classList.toggle('on', bag.length > 0);
    if (!bag.length) return;
    var total = bag.reduce(function (s, id) { return s + byId[id].preco; }, 0);
    var linhas = bag.map(function (id) { return '• ' + byId[id].nome + ' — ' + brl(byId[id].preco); });
    var msg = (D.mensagem || 'Oi! Vi sua vitrine e quero:') + '\n' + linhas.join('\n') + (bag.length > 1 ? '\nTotal: ' + brl(total) : '');
    link.href = 'https://wa.me/' + D.whatsapp + '?text=' + encodeURIComponent(msg);
    link.textContent = 'Pedir no WhatsApp (' + bag.length + ') · ' + brl(total);
  }

  document.querySelectorAll('.add').forEach(function (b) {
    b.addEventListener('click', function () {
      var id = b.getAttribute('data-id');
      var i = bag.indexOf(id);
      if (i >= 0) bag.splice(i, 1); else bag.push(id);
      b.setAttribute('aria-pressed', String(i < 0));
      b.textContent = i < 0 ? '✓ Na sacola' : 'Quero esta';
      render();
    });
  });

  document.querySelectorAll('.chip').forEach(function (c) {
    c.addEventListener('click', function () {
      var t = c.getAttribute('data-tipo');
      document.querySelectorAll('.chip').forEach(function (x) { x.setAttribute('aria-pressed', String(x === c)); });
      document.querySelectorAll('.card').forEach(function (card) { card.hidden = !!t && card.getAttribute('data-tipo') !== t; });
    });
  });
})();
</script>
</body>
</html>
