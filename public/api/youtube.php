<?php
// GlowStudio — busca Shorts no YouTube pela API oficial (YouTube Data API v3).
//   GET ?q=semijoias&dias=30   → os Shorts mais vistos do período
//   GET ?id=<id do vídeo>      → um vídeo só (link colado no Radar)
// Usa 'youtube_key' do ~/glowstudio-config.php ou, sem ela, a mesma chave do Gemini
// (funciona se a "YouTube Data API v3" estiver ativada no mesmo projeto do Google).
// Uma busca gasta 101 das 10.000 unidades diárias grátis, então os resultados ficam guardados por 6 horas.
declare(strict_types=1);
require __DIR__ . '/_lib.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') out(['error' => 'Método não permitido'], 405);
if (($_SERVER['HTTP_X_GLOWSTUDIO'] ?? '') === '') out(['error' => 'Pedido inválido'], 400);

$config = is_file("$home/glowstudio-config.php") ? (include "$home/glowstudio-config.php") : [];
$key = (string)($config['youtube_key'] ?? '') ?: (string)($config['gemini_key'] ?? '');
if ($key === '' || strpos($key, 'SUA_CHAVE') !== false) out(['error' => 'Chave do Google não configurada no servidor'], 503);

function yt(string $endpoint, array $params, string $key): array {
    $ch = curl_init("https://www.googleapis.com/youtube/v3/$endpoint?" . http_build_query($params + ['key' => $key]));
    curl_setopt_array($ch, [CURLOPT_RETURNTRANSFER => true, CURLOPT_TIMEOUT => 15, CURLOPT_HTTPHEADER => ['Accept: application/json']]);
    $res = curl_exec($ch);
    $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    $j = json_decode((string)$res, true) ?: [];
    if ($code === 200) return $j;
    $reason = $j['error']['errors'][0]['reason'] ?? '';
    if ($reason === 'quotaExceeded') out(['error' => 'A cota diária do YouTube acabou. Tente amanhã.'], 429);
    if ($code === 403) out(['error' => 'Ative a "YouTube Data API v3" no projeto do Google da sua chave', 'motivo' => $reason], 503);
    out(['error' => 'O YouTube não respondeu agora'], 502);
}

// PT1M5S → 65
function iso_seg(string $d): int {
    if (!preg_match('/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/', $d, $m)) return 0;
    return (int)($m[1] ?? 0) * 3600 + (int)($m[2] ?? 0) * 60 + (int)($m[3] ?? 0);
}

function detalhes(array $ids, string $key): array {
    if (!$ids) return [];
    $j = yt('videos', ['part' => 'snippet,statistics,contentDetails', 'id' => implode(',', $ids), 'maxResults' => 50], $key);
    $out = [];
    foreach ($j['items'] ?? [] as $v) {
        $sn = $v['snippet'] ?? [];
        $st = $v['statistics'] ?? [];
        $dur = iso_seg((string)($v['contentDetails']['duration'] ?? ''));
        $out[] = [
            'plataforma' => 'youtube',
            'url' => 'https://www.youtube.com/shorts/' . $v['id'],
            'perfil' => s($sn['channelTitle'] ?? '', 60),
            'legenda' => s(trim(($sn['title'] ?? '') . "\n" . mb_substr((string)($sn['description'] ?? ''), 0, 600)), 2200),
            'views' => (int)($st['viewCount'] ?? 0),
            'curtidas' => (int)($st['likeCount'] ?? 0),
            'comentarios' => (int)($st['commentCount'] ?? 0),
            'duracao' => $dur,
            'postadoEm' => strtotime((string)($sn['publishedAt'] ?? '')) * 1000,
            'formato' => 'video',
            'thumb' => (string)($sn['thumbnails']['high']['url'] ?? $sn['thumbnails']['medium']['url'] ?? ''),
            'completo' => true,
        ];
    }
    return $out;
}

if (isset($_GET['id'])) {
    $id = (string)$_GET['id'];
    if (!preg_match('/^[A-Za-z0-9_-]{11}$/', $id)) out(['error' => 'Link do YouTube inválido'], 400);
    if (rate_limited($dataDir, 'yt-id', 150, 2000)) out(['error' => 'Muitos links hoje'], 429);
    $r = detalhes([$id], $key);
    if (!$r) out(['error' => 'Vídeo não encontrado'], 404);
    out($r[0]);
}

$q = s($_GET['q'] ?? '', 60);
if (mb_strlen($q) < 2) out(['error' => 'Digite o que buscar'], 400);
$dias = in_array((int)($_GET['dias'] ?? 30), [7, 30, 90, 365], true) ? (int)$_GET['dias'] : 30;

$cacheDir = "$dataDir/yt-cache";
if (!is_dir($cacheDir)) @mkdir($cacheDir, 0700, true);
$cache = "$cacheDir/" . hash('sha256', mb_strtolower($q) . "|$dias") . '.json';
if (is_file($cache) && filemtime($cache) > time() - 6 * 3600) {
    header('X-Cache: hit');
    out(json_decode((string)file_get_contents($cache), true) ?: ['posts' => []]);
}
if (rate_limited($dataDir, 'yt-busca', 25, 80)) out(['error' => 'Limite de buscas de hoje atingido. Tente amanhã.'], 429);

$busca = yt('search', [
    'part' => 'id',
    'type' => 'video',
    'videoDuration' => 'short',
    'q' => "$q #shorts",
    'order' => 'viewCount',
    'regionCode' => 'BR',
    'relevanceLanguage' => 'pt',
    'publishedAfter' => gmdate('Y-m-d\TH:i:s\Z', time() - $dias * 86400),
    'maxResults' => 30,
], $key);
$ids = array_values(array_filter(array_map(fn($i) => (string)($i['id']['videoId'] ?? ''), $busca['items'] ?? [])));
// Shorts têm até 3 minutos.
$posts = array_values(array_filter(detalhes($ids, $key), fn($p) => $p['duracao'] > 0 && $p['duracao'] <= 180));
usort($posts, fn($a, $b) => $b['views'] <=> $a['views']);
$data = ['posts' => $posts, 'buscadoEm' => time()];
file_put_contents($cache, json_encode($data, JSON_UNESCAPED_UNICODE), LOCK_EX);
foreach (glob("$cacheDir/*.json") ?: [] as $old) if (filemtime($old) < time() - 2 * 86400) @unlink($old);
out($data);
