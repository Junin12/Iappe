<?php
// GlowStudio — lê um post público do TikTok ou Instagram a partir do link que a usuária colou
// (um pedido por link, como a prévia de link do WhatsApp). Só aceita esses dois sites.
// Devolve perfil, legenda, números e a capa. O que não der para ler, o app pede para digitar.
declare(strict_types=1);
require __DIR__ . '/_lib.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') out(['error' => 'Método não permitido'], 405);
if (($_SERVER['HTTP_X_GLOWSTUDIO'] ?? '') === '') out(['error' => 'Pedido inválido'], 400);

const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36';
const PAGE_HOSTS = ['tiktok.com', 'www.tiktok.com', 'm.tiktok.com', 'vm.tiktok.com', 'vt.tiktok.com', 'instagram.com', 'www.instagram.com'];
const IMG_HOSTS = ['tiktokcdn.com', 'tiktokcdn-us.com', 'tiktokcdn-eu.com', 'ibyteimg.com', 'cdninstagram.com', 'fbcdn.net'];

function host_ok(string $url, array $allowed, bool $suffix = false): bool {
    $p = parse_url($url);
    if (($p['scheme'] ?? '') !== 'https' || empty($p['host'])) return false;
    $h = strtolower($p['host']);
    foreach ($allowed as $a) {
        if ($h === $a || ($suffix && str_ends_with($h, ".$a"))) return true;
    }
    return false;
}

// GET com redirecionamentos manuais: cada salto precisa continuar num site permitido.
function fetch(string $url, array $allowed, bool $suffix = false, int $maxBytes = 3_000_000, string $ua = UA): array {
    for ($hop = 0; $hop < 5; $hop++) {
        if (!host_ok($url, $allowed, $suffix)) return [0, '', '', $url];
        $body = '';
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => false,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_PROTOCOLS => CURLPROTO_HTTPS,
            CURLOPT_TIMEOUT => 15,
            CURLOPT_CONNECTTIMEOUT => 6,
            CURLOPT_ENCODING => '',
            CURLOPT_HTTPHEADER => ["User-Agent: $ua", 'Accept-Language: pt-BR,pt;q=0.9,en;q=0.6'],
            CURLOPT_WRITEFUNCTION => function ($ch, $chunk) use (&$body, $maxBytes) {
                $body .= $chunk;
                return strlen($body) > $maxBytes ? 0 : strlen($chunk);
            },
        ]);
        curl_exec($ch);
        $code = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        $type = (string)curl_getinfo($ch, CURLINFO_CONTENT_TYPE);
        $next = (string)curl_getinfo($ch, CURLINFO_REDIRECT_URL);
        curl_close($ch);
        if ($code >= 300 && $code < 400 && $next !== '') {
            $url = $next;
            continue;
        }
        return [$code, $body, $type, $url];
    }
    return [0, '', '', $url];
}

function thumb_data_url(string $url): string {
    if ($url === '' || !host_ok($url, IMG_HOSTS, true)) return '';
    [$code, $bin, $type] = fetch($url, IMG_HOSTS, true, 1_500_000);
    if ($code !== 200 || $bin === '' || !preg_match('#^image/(jpeg|png|webp)#', $type, $m)) return '';
    if (@getimagesizefromstring($bin) === false) return '';
    return "data:image/{$m[1]};base64," . base64_encode($bin);
}

function n($v): int {
    return max(0, (int)preg_replace('/\D/', '', (string)$v));
}

// "1.234", "12,5 mil", "1.2M", "3K" → número.
function human_n(string $s): int {
    $s = strtolower(trim($s));
    if (!preg_match('/([\d.,]+)\s*(mil|k|mi|m|milhões|milhão)?/u', $s, $m)) return 0;
    $num = $m[1];
    $mult = ['mil' => 1e3, 'k' => 1e3, 'mi' => 1e6, 'm' => 1e6, 'milhões' => 1e6, 'milhão' => 1e6][$m[2] ?? ''] ?? 1;
    if ($mult > 1) return (int)round((float)str_replace(',', '.', $num) * $mult);
    return n($num);
}

function tiktok(string $url): array {
    [$code, $html, , $final] = fetch($url, PAGE_HOSTS);
    $r = ['plataforma' => 'tiktok', 'url' => $final];
    if ($code === 200 && preg_match('#<script id="__UNIVERSAL_DATA_FOR_REHYDRATION__"[^>]*>(.*?)</script>#s', $html, $m)) {
        $j = json_decode($m[1], true);
        $it = $j['__DEFAULT_SCOPE__']['webapp.video-detail']['itemInfo']['itemStruct'] ?? null;
        if (is_array($it)) {
            $st = $it['statsV2'] ?? $it['stats'] ?? [];
            $user = (string)($it['author']['uniqueId'] ?? '');
            $id = (string)($it['id'] ?? '');
            return $r + [
                'url' => $user && $id ? "https://www.tiktok.com/@$user/video/$id" : $final,
                'perfil' => $user,
                'legenda' => s($it['desc'] ?? '', 2200),
                'views' => n($st['playCount'] ?? 0),
                'curtidas' => n($st['diggCount'] ?? 0),
                'comentarios' => n($st['commentCount'] ?? 0),
                'compart' => n($st['shareCount'] ?? 0),
                'salvos' => n($st['collectCount'] ?? 0),
                'duracao' => (int)($it['video']['duration'] ?? 0),
                'postadoEm' => ((int)($it['createTime'] ?? 0)) * 1000,
                'formato' => !empty($it['imagePost']) ? 'foto' : 'video',
                'thumb' => thumb_data_url((string)($it['video']['cover'] ?? $it['video']['originCover'] ?? '')),
                'completo' => true,
            ];
        }
    }
    // Plano B: o oEmbed oficial dá perfil, legenda e capa (sem números).
    [$c2, $body] = fetch('https://www.tiktok.com/oembed?url=' . rawurlencode($final), PAGE_HOSTS);
    $o = $c2 === 200 ? json_decode($body, true) : null;
    if (!is_array($o) || empty($o['author_unique_id'] ?? $o['author_name'] ?? '')) return $r + ['completo' => false];
    return $r + [
        'perfil' => s($o['author_unique_id'] ?? $o['author_name'] ?? '', 60),
        'legenda' => s($o['title'] ?? '', 2200),
        'thumb' => thumb_data_url((string)($o['thumbnail_url'] ?? '')),
        'completo' => false,
    ];
}

function instagram(string $url): array {
    $p = parse_url($url);
    if (!preg_match('#/(p|reel|reels|tv)/([A-Za-z0-9_-]{5,40})#', $p['path'] ?? '', $m)) out(['error' => 'Link do Instagram precisa ser de um post ou reel'], 400);
    $kind = $m[1] === 'p' ? 'p' : 'reel';
    $canon = "https://www.instagram.com/$kind/{$m[2]}/";
    $r = ['plataforma' => 'instagram', 'url' => $canon, 'formato' => $kind === 'p' ? 'foto' : 'video', 'completo' => false];

    // A página de incorporação pública traz perfil, legenda e capa.
    [$code, $html] = fetch("https://www.instagram.com/p/{$m[2]}/embed/captioned/", PAGE_HOSTS);
    if ($code === 200) {
        if (preg_match('#class="UsernameText"[^>]*>([^<]+)<#', $html, $u)) $r['perfil'] = s(html_entity_decode($u[1]), 60);
        if (preg_match('#<div class="Caption">(.*?)<div class="CaptionComments">#s', $html, $c)) {
            $cap = preg_replace('#<a class="CaptionUsername".*?</a>#s', '', $c[1]);
            $cap = preg_replace('#<br\s*/?>#i', "\n", $cap);
            $r['legenda'] = s(html_entity_decode(strip_tags($cap), ENT_QUOTES | ENT_HTML5), 2200);
        }
        if (preg_match('#class="EmbeddedMediaImage"[^>]*src="([^"]+)"#', $html, $img)) $r['thumb'] = thumb_data_url(html_entity_decode($img[1]));
        if (preg_match('#([\d.,]+\s*(?:mil|k|m)?)\s*(?:curtidas|likes)#iu', $html, $l)) $r['curtidas'] = human_n($l[1]);
    }
    // A descrição para prévias de link às vezes traz "123 likes, 4 comments - perfil on ...: "legenda"".
    [$c2, $page] = fetch($canon, PAGE_HOSTS, false, 1_500_000, 'facebookexternalhit/1.1');
    if ($c2 === 200 && preg_match('#<meta[^>]+(?:property|name)="(?:og:)?description"[^>]+content="([^"]*)"#', $page, $d)) {
        $desc = html_entity_decode($d[1], ENT_QUOTES | ENT_HTML5);
        if (preg_match('#^([\d.,]+\s*[KkMm]?)\s+(?:likes|curtidas),\s*([\d.,]+\s*[KkMm]?)\s+(?:comments|comentários)\s*-\s*([A-Za-z0-9._]+)#u', $desc, $x)) {
            $r['curtidas'] = $r['curtidas'] ?? human_n($x[1]);
            $r['comentarios'] = human_n($x[2]);
            $r['perfil'] = $r['perfil'] ?? $x[3];
        }
        if (empty($r['legenda']) && preg_match('#:\s*"(.*)"\s*\.?$#su', $desc, $y)) $r['legenda'] = s($y[1], 2200);
        if (empty($r['thumb']) && preg_match('#<meta[^>]+property="og:image"[^>]+content="([^"]+)"#', $page, $oi)) $r['thumb'] = thumb_data_url(html_entity_decode($oi[1]));
    }
    return $r;
}

$url = trim((string)($_GET['url'] ?? ''));
if (strlen($url) > 600) out(['error' => 'Link longo demais'], 400);
if (!preg_match('#^https?://#i', $url)) $url = "https://$url";
$url = preg_replace('#^http://#i', 'https://', $url);
if (!host_ok($url, PAGE_HOSTS)) out(['error' => 'Cole um link do TikTok ou do Instagram'], 400);
if (rate_limited($dataDir, 'link', 150, 3000)) out(['error' => 'Muitos links hoje. Tente amanhã ou preencha à mão.'], 429);

$isTikTok = str_contains(strtolower((string)parse_url($url, PHP_URL_HOST)), 'tiktok');
out($isTikTok ? tiktok($url) : instagram($url));
