<?php
// GlowStudio — intermediário da IA (Gemini).
// A chave NUNCA vai para o app: fica em ~/glowstudio-config.php, fora da pasta pública:
//   <?php return ['gemini_key' => 'SUA_CHAVE_AQUI'];
// Só aceita os pedidos do app (roteiro, foto, post da concorrência e tendências), com limite diário por IP.

declare(strict_types=1);
require __DIR__ . '/_lib.php';

const MODELS = ['gemini-flash-latest', 'gemini-2.5-flash'];

$config = is_file("$home/glowstudio-config.php") ? (include "$home/glowstudio-config.php") : [];
$key = is_array($config) && !empty($config['gemini_key']) ? (string)$config['gemini_key'] : (getenv('GEMINI_API_KEY') ?: '');
$configured = $key !== '' && strpos($key, 'SUA_CHAVE') === false;

if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['ping'])) {
    out(['ok' => true, 'configured' => $configured]);
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') out(['error' => 'Método não permitido'], 405);
if (($_SERVER['HTTP_X_GLOWSTUDIO'] ?? '') === '') out(['error' => 'Pedido inválido'], 400);
if (!$configured) out(['error' => 'IA não configurada no servidor'], 503);

$in = read_json_body();
if (rate_limited($dataDir, 'uso', 40, 400)) out(['error' => 'Limite diário da IA atingido. Use os modelos prontos.'], 429);

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

// Post da concorrência: classifica o tipo de vídeo e explica o que dá para aproveitar (sem copiar o texto).
const TIPOS_RADAR = ['brilho', 'unboxing', 'tres-jeitos', 'antes-depois', 'presente', 'adivinha', 'mix', 'qualidade', 'foto', 'outro'];

if ($action === 'radar') {
    $p = is_array($in['post'] ?? null) ? $in['post'] : [];
    $txt = 'Post de ' . s($p['plataforma'] ?? '', 12) . ' do perfil @' . s($p['perfil'] ?? '', 40)
        . '. Visualizações: ' . (int)($p['views'] ?? 0) . '. Curtidas: ' . (int)($p['curtidas'] ?? 0) . '. Comentários: ' . (int)($p['comentarios'] ?? 0)
        . '. Duração: ' . (int)($p['duracao'] ?? 0) . "s.\nLegenda: " . s($p['legenda'] ?? '', 1200);
    $parts = [];
    $b64 = (string)($in['image'] ?? '');
    if ($b64 !== '' && strlen($b64) < 2_000_000 && ($bin = base64_decode($b64, true)) !== false && ($info = @getimagesizefromstring($bin))) {
        $parts[] = ['inline_data' => ['mime_type' => $info['mime'], 'data' => $b64]];
    }
    $parts[] = ['text' => "$txt\n\nEsse post é de uma concorrente que vende semijoias. Analise (pela capa e legenda): "
        . 'o tipo de vídeo (brilho = peça brilhando na luz; unboxing; tres-jeitos = vários jeitos de usar; antes-depois; presente = POV presente; adivinha = adivinha o preço; mix = montando mix de peças; qualidade = prova de qualidade/teste; foto = foto ou carrossel; outro), '
        . 'o gancho usado nos primeiros segundos (resuma em até 8 palavras, com suas palavras), por que funcionou (até 25 palavras) '
        . 'e como ela pode fazer um vídeo parecido SEM rosto e sem copiar (até 30 palavras).'];
    $body = [
        'systemInstruction' => ['parts' => [['text' => $regras]]],
        'contents' => [['role' => 'user', 'parts' => $parts]],
        'generationConfig' => [
            'temperature' => 0.5,
            'responseMimeType' => 'application/json',
            'responseSchema' => [
                'type' => 'OBJECT',
                'properties' => [
                    'tipo' => ['type' => 'STRING', 'enum' => TIPOS_RADAR],
                    'gancho' => ['type' => 'STRING'],
                    'porque' => ['type' => 'STRING'],
                    'adaptar' => ['type' => 'STRING'],
                ],
                'required' => ['tipo', 'gancho', 'porque', 'adaptar'],
            ],
        ],
    ];
    $r = gemini($key, $body);
    out([
        'tipo' => in_array($r['tipo'] ?? '', TIPOS_RADAR, true) ? $r['tipo'] : 'outro',
        'gancho' => s($r['gancho'] ?? '', 80),
        'porque' => s($r['porque'] ?? '', 220),
        'adaptar' => s($r['adaptar'] ?? '', 260),
    ]);
}

// Tendências: resume o que os posts mais fortes da concorrência têm em comum.
if ($action === 'tendencias') {
    $lista = '';
    foreach (array_slice(is_array($in['posts'] ?? null) ? $in['posts'] : [], 0, 30) as $i => $p) {
        if (!is_array($p)) continue;
        $lista .= ($i + 1) . '. [' . s($p['plataforma'] ?? '', 10) . ', ' . s($p['tipo'] ?? '', 14) . '] views ' . (int)($p['views'] ?? 0)
            . ', curtidas ' . (int)($p['curtidas'] ?? 0) . ', coment. ' . (int)($p['comentarios'] ?? 0) . ': ' . s($p['legenda'] ?? '', 280) . "\n";
    }
    if ($lista === '') out(['error' => 'Sem posts para analisar'], 400);
    $body = [
        'systemInstruction' => ['parts' => [['text' => $regras]]],
        'contents' => [['role' => 'user', 'parts' => [['text' => "Posts de concorrentes de semijoias, do mais visto para o menos visto:\n$lista\n"
            . 'Encontre de 3 a 5 tendências concretas (tipo de vídeo, gancho, preço, palavras, chamada para ação) que explicam os melhores resultados, cada uma com até 22 palavras, '
            . 'e 3 ideias de vídeo sem rosto para ela gravar esta semana (até 16 palavras cada). Baseie-se só nos dados.']]]],
        'generationConfig' => [
            'temperature' => 0.6,
            'responseMimeType' => 'application/json',
            'responseSchema' => [
                'type' => 'OBJECT',
                'properties' => [
                    'tendencias' => ['type' => 'ARRAY', 'items' => ['type' => 'STRING']],
                    'ideias' => ['type' => 'ARRAY', 'items' => ['type' => 'STRING']],
                ],
                'required' => ['tendencias', 'ideias'],
            ],
        ],
    ];
    $r = gemini($key, $body);
    out([
        'tendencias' => array_values(array_map(fn($t) => s($t, 200), array_slice((array)($r['tendencias'] ?? []), 0, 5))),
        'ideias' => array_values(array_map(fn($t) => s($t, 160), array_slice((array)($r['ideias'] ?? []), 0, 3))),
    ]);
}

out(['error' => 'Ação desconhecida'], 400);
