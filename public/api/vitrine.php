<?php
// GlowStudio — guarda a vitrine pública (link na bio) que o app publica.
// Os dados ficam fora da pasta pública, em ~/glowstudio-data/vitrines/<slug>/.
// Só quem tem o token (gerado no celular na primeira publicação) altera ou apaga a vitrine.
//   POST {action:'publicar', slug, token, loja:{...}, pecas:[...]} → {faltam:[ids sem foto]}
//   POST {action:'foto', slug, token, id, foto, image(base64 jpeg)}
//   POST {action:'apagar', slug, token}
//   GET  ?img=<slug>/<peça>_<foto>.jpg → a foto
declare(strict_types=1);
require __DIR__ . '/_lib.php';

const MAX_PECAS = 120;
const MAX_VITRINES = 40;
$base = "$dataDir/vitrines";

function slug_ok(string $s): bool {
    return (bool)preg_match('/^[a-z0-9](?:[a-z0-9-]{1,38})[a-z0-9]$/', $s);
}

if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['img'])) {
    $parts = explode('/', (string)$_GET['img']);
    if (count($parts) !== 2 || !slug_ok($parts[0]) || !preg_match('/^[a-z0-9]{4,40}_[a-z0-9]{4,40}\.jpg$/', $parts[1])) out(['error' => 'Foto inválida'], 404);
    $file = "$base/{$parts[0]}/{$parts[1]}";
    if (!is_file($file)) out(['error' => 'Foto não encontrada'], 404);
    header('Content-Type: image/jpeg');
    header('Cache-Control: public, max-age=604800, immutable');
    header('X-Content-Type-Options: nosniff');
    header('Content-Length: ' . filesize($file));
    readfile($file);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') out(['error' => 'Método não permitido'], 405);
if (($_SERVER['HTTP_X_GLOWSTUDIO'] ?? '') === '') out(['error' => 'Pedido inválido'], 400);
$in = read_json_body(1_500_000);

$slug = (string)($in['slug'] ?? '');
$token = (string)($in['token'] ?? '');
if (!slug_ok($slug)) out(['error' => 'Endereço da vitrine inválido (use letras, números e hífen)'], 400);
if (!preg_match('/^[A-Za-z0-9_-]{24,80}$/', $token)) out(['error' => 'Token inválido'], 400);

$dir = "$base/$slug";
$dataFile = "$dir/data.json";
$current = is_file($dataFile) ? json_decode((string)file_get_contents($dataFile), true) : null;

// Dona da vitrine: confere o token. Vitrine nova: confere os limites de criação.
if (is_array($current)) {
    if (!password_verify($token, (string)($current['token_hash'] ?? ''))) out(['error' => 'Esse endereço já é de outra vitrine. Escolha outro.'], 409);
} else {
    if (($in['action'] ?? '') !== 'publicar') out(['error' => 'Vitrine não encontrada'], 404);
    if (count(glob("$base/*", GLOB_ONLYDIR) ?: []) >= MAX_VITRINES) out(['error' => 'Limite de vitrines do servidor atingido'], 507);
    if (rate_limited($dataDir, 'vitrine-nova', 3, 20)) out(['error' => 'Muitas vitrines novas hoje'], 429);
}
if (rate_limited($dataDir, 'vitrine', 400, 4000)) out(['error' => 'Muitas publicações hoje. Tente amanhã.'], 429);

function save_json(string $file, array $data): void {
    $tmp = "$file.tmp";
    file_put_contents($tmp, json_encode($data, JSON_UNESCAPED_UNICODE), LOCK_EX);
    rename($tmp, $file);
}

$action = $in['action'] ?? '';

if ($action === 'publicar') {
    $l = is_array($in['loja'] ?? null) ? $in['loja'] : [];
    $whats = preg_replace('/\D/', '', (string)($l['whatsapp'] ?? ''));
    if (strlen($whats) < 12 || strlen($whats) > 13) out(['error' => 'WhatsApp inválido: use DDD + número'], 400);
    $insta = preg_replace('/[^A-Za-z0-9._]/', '', (string)($l['instagram'] ?? ''));
    $pecas = [];
    foreach (array_slice(is_array($in['pecas'] ?? null) ? $in['pecas'] : [], 0, MAX_PECAS) as $p) {
        if (!is_array($p)) continue;
        $id = (string)($p['id'] ?? '');
        $foto = (string)($p['foto'] ?? '');
        if (!preg_match('/^[a-z0-9]{4,40}$/', $id)) continue;
        $preco = round(max(0, (float)($p['preco'] ?? 0)), 2);
        $promo = round(max(0, (float)($p['precoPromo'] ?? 0)), 2);
        $pecas[] = [
            'id' => $id,
            'nome' => s($p['nome'] ?? '', 60),
            'tipo' => s($p['tipo'] ?? '', 20),
            'banho' => s($p['banho'] ?? '', 20),
            'pedra' => s($p['pedra'] ?? '', 20),
            'desc' => s($p['desc'] ?? '', 300),
            'preco' => $preco,
            'precoPromo' => $promo > 0 && $promo < $preco ? $promo : 0,
            'esgotado' => !empty($p['esgotado']),
            'foto' => preg_match('/^[a-z0-9]{4,40}$/', $foto) ? $foto : '',
        ];
    }
    if (!is_dir($dir) && !@mkdir($dir, 0700, true)) out(['error' => 'Não consegui criar a vitrine no servidor'], 500);
    $data = [
        'token_hash' => $current['token_hash'] ?? password_hash($token, PASSWORD_DEFAULT),
        'loja' => [
            'nome' => s($l['nome'] ?? '', 50) ?: 'Minha loja',
            'bio' => s($l['bio'] ?? '', 160),
            'whatsapp' => $whats,
            'instagram' => mb_substr($insta, 0, 30),
            'mensagem' => s($l['mensagem'] ?? '', 120),
        ],
        'pecas' => $pecas,
        'atualizadoEm' => time(),
    ];
    save_json($dataFile, $data);
    // Apaga fotos que saíram da vitrine; devolve as que o app ainda precisa enviar.
    $keep = [];
    foreach ($pecas as $p) if ($p['foto']) $keep["{$p['id']}_{$p['foto']}.jpg"] = $p['id'];
    foreach (glob("$dir/*.jpg") ?: [] as $f) if (!isset($keep[basename($f)])) @unlink($f);
    $faltam = [];
    foreach ($keep as $file => $id) if (!is_file("$dir/$file")) $faltam[] = $id;
    out(['ok' => true, 'faltam' => $faltam]);
}

if ($action === 'foto') {
    $id = (string)($in['id'] ?? '');
    $foto = (string)($in['foto'] ?? '');
    if (!preg_match('/^[a-z0-9]{4,40}$/', $id) || !preg_match('/^[a-z0-9]{4,40}$/', $foto)) out(['error' => 'Foto inválida'], 400);
    $known = false;
    foreach ($current['pecas'] ?? [] as $p) if ($p['id'] === $id && $p['foto'] === $foto) $known = true;
    if (!$known) out(['error' => 'Peça fora da vitrine'], 400);
    $bin = base64_decode((string)($in['image'] ?? ''), true);
    if ($bin === false || strlen($bin) > 600_000) out(['error' => 'Foto grande demais'], 413);
    $info = @getimagesizefromstring($bin);
    if (substr($bin, 0, 3) !== "\xFF\xD8\xFF" || !$info || $info[2] !== IMAGETYPE_JPEG) out(['error' => 'Envie a foto em JPG'], 400);
    file_put_contents("$dir/{$id}_{$foto}.jpg", $bin, LOCK_EX);
    out(['ok' => true]);
}

if ($action === 'apagar') {
    foreach (glob("$dir/*") ?: [] as $f) @unlink($f);
    @rmdir($dir);
    out(['ok' => true]);
}

out(['error' => 'Ação desconhecida'], 400);
