<?php
// GlowStudio — intermediário da IA (Gemini).
// A chave NUNCA vai para o app: fica em ~/glowstudio-config.php, fora da pasta pública:
//   <?php return ['gemini_key' => 'SUA_CHAVE_AQUI'];
// Só aceita os dois pedidos do app (roteiro e foto), com limite diário por IP.

declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

const LIMIT_PER_IP = 40;
const LIMIT_TOTAL = 400;
const MODELS = ['gemini-flash-latest', 'gemini-2.5-flash'];

$home = dirname(__DIR__, 3);
$config = is_file("$home/glowstudio-config.php") ? (include "$home/glowstudio-config.php") : [];
$key = is_array($config) && !empty($config['gemini_key']) ? (string)$config['gemini_key'] : (getenv('GEMINI_API_KEY') ?: '');
$configured = $key !== '' && strpos($key, 'SUA_CHAVE') === false;

function out(array $data, int $code = 200): void {
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['ping'])) {
    out(['ok' => true, 'configured' => $configured]);
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') out(['error' => 'Método não permitido'], 405);
if (($_SERVER['HTTP_X_GLOWSTUDIO'] ?? '') === '') out(['error' => 'Pedido inválido'], 400);
if (!$configured) out(['error' => 'IA não configurada no servidor'], 503);

$raw = file_get_contents('php://input', false, null, 0, 4_000_000);
$in = json_decode((string)$raw, true);
if (!is_array($in)) out(['error' => 'JSON inválido'], 400);

// ---------- Limite diário ----------
function rate_limited(string $home): bool {
    $dir = "$home/glowstudio-data";
    if (!is_dir($dir)) @mkdir($dir, 0700, true);
    $file = "$dir/uso-" . date('Ymd') . '.json';
    $fh = @fopen($file, 'c+');
    if (!$fh) return false; // sem disco para contar: não bloqueia
    flock($fh, LOCK_EX);
    $data = json_decode((string)stream_get_contents($fh), true) ?: ['total' => 0, 'ip' => []];
    $ip = hash('sha256', ($_SERVER['REMOTE_ADDR'] ?? '') . date('Ymd'));
    $mine = $data['ip'][$ip] ?? 0;
    $blocked = $mine >= LIMIT_PER_IP || $data['total'] >= LIMIT_TOTAL;
    if (!$blocked) {
        $data['ip'][$ip] = $mine + 1;
        $data['total']++;
        ftruncate($fh, 0);
        rewind($fh);
        fwrite($fh, json_encode($data));
    }
    flock($fh, LOCK_UN);
    fclose($fh);
    foreach (glob("$dir/uso-*.json") ?: [] as $old) {
        if ($old !== $file && filemtime($old) < time() - 3 * 86400) @unlink($old);
    }
    return $blocked;
}
if (rate_limited($home)) out(['error' => 'Limite diário da IA atingido. Use os modelos prontos.'], 429);

function s($v, int $max): string {
    return mb_substr(trim(preg_replace('/\s+/u', ' ', (string)$v)), 0, $max);
}

function gemini(string $key, array $body): array {
    foreach (MODELS as $model) {
        $ch = curl_init("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent");
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 35,
            CURLOPT_HTTPHEADER => ['Content-Type: application/json', "x-goog-api-key: $key"],
            CURLOPT_POSTFIELDS => json_encode($body, JSON_UNESCAPED_UNICODE),
        ]);
        $res = curl_exec($ch);
        $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($code === 404) continue; // modelo indisponível: tenta o próximo
        if ($code !== 200 || !$res) out(['error' => 'A IA não respondeu agora'], 502);
        $j = json_decode($res, true);
        $text = $j['candidates'][0]['content']['parts'][0]['text'] ?? '';
        $data = json_decode($text, true);
        if (!is_array($data)) out(['error' => 'Resposta da IA inválida'], 502);
        return $data;
    }
    out(['error' => 'Modelo da IA indisponível'], 502);
}

$regras = 'Você ajuda uma vendedora de semijoias a criar vídeos curtos para o TikTok SEM mostrar o rosto: só mãos, peça, colo, orelha, pulso. '
    . 'Escreva em português do Brasil, tom próximo e animado, frases curtas. '
    . 'Nunca invente características: não diga "ouro maciço", "não escurece nunca", "antialérgica" ou garantia, a menos que esteja nos dados. '
    . 'Use exatamente o banho, milésimos, pedra e preço informados.';

$action = $in['action'] ?? '';

if ($action === 'script') {
    $p = is_array($in['peca'] ?? null) ? $in['peca'] : [];
    $f = is_array($in['formato'] ?? null) ? $in['formato'] : [];
    $cenas = array_slice(is_array($f['cenas'] ?? null) ? $f['cenas'] : [], 0, 8);
    $n = count($cenas);
    if ($n === 0) out(['error' => 'Formato sem cenas'], 400);
    $listaCenas = '';
    foreach ($cenas as $i => $c) {
        $listaCenas .= ($i + 1) . '. ' . s($c['titulo'] ?? '', 40) . ' (' . (int)($c['seg'] ?? 3) . 's): ' . s($c['acao'] ?? '', 140) . "\n";
    }
    $preco = (float)($p['preco'] ?? 0);
    $promo = (float)($p['precoPromo'] ?? 0);
    $dados = 'Peça: ' . s($p['nome'] ?? '', 60) . '. Tipo: ' . s($p['tipo'] ?? '', 20) . '. Banho: ' . s($p['banho'] ?? '', 20)
        . ($p['milesimos'] ?? null ? ', ' . (int)$p['milesimos'] . ' milésimos' : '') . '. Pedra: ' . s($p['pedra'] ?? '', 20)
        . '. Preço: R$ ' . number_format($promo > 0 && $promo < $preco ? $promo : $preco, 2, ',', '.')
        . ($promo > 0 && $promo < $preco ? ' (de R$ ' . number_format($preco, 2, ',', '.') . ')' : '')
        . '. Observações: ' . s($p['obs'] ?? '', 300);
    $prompt = "$dados\n\nFormato do vídeo: " . s($f['nome'] ?? '', 40) . "\nCenas:\n$listaCenas\n"
        . "Crie: 3 ganchos diferentes para os 3 primeiros segundos (máx. 8 palavras cada, aparecem escritos na tela); "
        . "para cada uma das $n cenas, uma ação de mãos mais específica para ESTA peça e uma fala curta opcional (máx. 14 palavras); "
        . 'uma legenda de até 140 caracteres com chamada para ação; e de 5 a 8 hashtags misturando nicho (semijoias) e amplas.';
    $body = [
        'systemInstruction' => ['parts' => [['text' => $regras]]],
        'contents' => [['role' => 'user', 'parts' => [['text' => $prompt]]]],
        'generationConfig' => [
            'temperature' => 0.95,
            'responseMimeType' => 'application/json',
            'responseSchema' => [
                'type' => 'OBJECT',
                'properties' => [
                    'ganchos' => ['type' => 'ARRAY', 'items' => ['type' => 'STRING']],
                    'cenas' => ['type' => 'ARRAY', 'items' => ['type' => 'OBJECT', 'properties' => ['acao' => ['type' => 'STRING'], 'narracao' => ['type' => 'STRING']], 'required' => ['acao', 'narracao']]],
                    'legenda' => ['type' => 'STRING'],
                    'hashtags' => ['type' => 'STRING'],
                ],
                'required' => ['ganchos', 'cenas', 'legenda', 'hashtags'],
            ],
        ],
    ];
    $r = gemini($key, $body);
    out([
        'ganchos' => array_values(array_map(fn($g) => s($g, 70), array_slice((array)($r['ganchos'] ?? []), 0, 3))),
        'cenas' => array_values(array_map(fn($c) => ['acao' => s($c['acao'] ?? '', 160), 'narracao' => s($c['narracao'] ?? '', 120)], array_slice((array)($r['cenas'] ?? []), 0, $n))),
        'legenda' => s($r['legenda'] ?? '', 150),
        'hashtags' => s($r['hashtags'] ?? '', 200),
    ]);
}

if ($action === 'photo') {
    $b64 = (string)($in['image'] ?? '');
    if (strlen($b64) > 3_000_000) out(['error' => 'Foto grande demais'], 413);
    $bin = base64_decode($b64, true);
    if ($bin === false) out(['error' => 'Foto inválida'], 400);
    $isJpeg = substr($bin, 0, 3) === "\xFF\xD8\xFF";
    $isPng = substr($bin, 0, 8) === "\x89PNG\r\n\x1a\n";
    if (!$isJpeg && !$isPng) out(['error' => 'Envie JPG ou PNG'], 400);
    $body = [
        'systemInstruction' => ['parts' => [['text' => $regras]]],
        'contents' => [[
            'role' => 'user',
            'parts' => [
                ['inline_data' => ['mime_type' => $isJpeg ? 'image/jpeg' : 'image/png', 'data' => $b64]],
                ['text' => 'Identifique a semijoia da foto. Sugira um nome comercial curto e bonito (máx. 5 palavras), o tipo, a cor do banho e a pedra, '
                    . 'e 3 ideias de vídeo sem rosto específicas para esta peça (máx. 12 palavras cada). Se não tiver certeza, escolha "Outro" ou "Nenhuma".'],
            ],
        ]],
        'generationConfig' => [
            'temperature' => 0.6,
            'responseMimeType' => 'application/json',
            'responseSchema' => [
                'type' => 'OBJECT',
                'properties' => [
                    'nome' => ['type' => 'STRING'],
                    'tipo' => ['type' => 'STRING', 'enum' => ['Colar', 'Brinco', 'Anel', 'Pulseira', 'Conjunto', 'Tornozeleira']],
                    'banho' => ['type' => 'STRING', 'enum' => ['Ouro 18k', 'Ródio', 'Prata', 'Ouro rosé', 'Outro']],
                    'pedra' => ['type' => 'STRING', 'enum' => ['Nenhuma', 'Zircônia', 'Pérola', 'Cristal', 'Madrepérola']],
                    'ideias' => ['type' => 'ARRAY', 'items' => ['type' => 'STRING']],
                ],
                'required' => ['nome', 'tipo', 'banho', 'pedra', 'ideias'],
            ],
        ],
    ];
    $r = gemini($key, $body);
    out([
        'nome' => s($r['nome'] ?? '', 60),
        'tipo' => s($r['tipo'] ?? '', 20),
        'banho' => s($r['banho'] ?? '', 20),
        'pedra' => s($r['pedra'] ?? '', 20),
        'ideias' => array_values(array_map(fn($i) => s($i, 120), array_slice((array)($r['ideias'] ?? []), 0, 3))),
    ]);
}

out(['error' => 'Ação desconhecida'], 400);
