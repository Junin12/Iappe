<?php
// Funções comuns dos endpoints do GlowStudio. Não é um endpoint: só é incluído.
declare(strict_types=1);
if (realpath($_SERVER['SCRIPT_FILENAME'] ?? '') === __FILE__) {
    http_response_code(404);
    exit;
}

// Servidores com PHP 7.4.
if (!function_exists('str_contains')) {
    function str_contains(string $h, string $n): bool { return $n === '' || strpos($h, $n) !== false; }
    function str_ends_with(string $h, string $n): bool { return $n === '' || substr($h, -strlen($n)) === $n; }
}

// public_html/glowstudio/api → home do usuário (fora da pasta pública).
$home = dirname(__DIR__, 3);
$dataDir = "$home/glowstudio-data";

// Lê ~/glowstudio-config.php sem derrubar o site se o arquivo tiver erro de digitação.
function load_config(string $home): array {
    $f = "$home/glowstudio-config.php";
    if (!is_file($f)) return [];
    try {
        $c = include $f;
        return is_array($c) ? $c : ['_erro' => 'o arquivo não devolve a lista de chaves'];
    } catch (Throwable $e) {
        return ['_erro' => 'erro de digitação na linha ' . $e->getLine() . ': ' . $e->getMessage()];
    }
}

function out(array $data, int $code = 200): void {
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function s($v, int $max): string {
    return mb_substr(trim(preg_replace('/\s+/u', ' ', (string)$v)), 0, $max);
}

// Conta pedidos por dia (por IP e no total) num arquivo com trava. $bucket separa os usos.
function rate_limited(string $dataDir, string $bucket, int $perIp, int $total): bool {
    if (!is_dir($dataDir)) @mkdir($dataDir, 0700, true);
    $file = "$dataDir/$bucket-" . date('Ymd') . '.json';
    $fh = @fopen($file, 'c+');
    if (!$fh) return false; // sem disco para contar: não bloqueia
    flock($fh, LOCK_EX);
    $data = json_decode((string)stream_get_contents($fh), true) ?: ['total' => 0, 'ip' => []];
    $ip = hash('sha256', ($_SERVER['REMOTE_ADDR'] ?? '') . date('Ymd'));
    $mine = $data['ip'][$ip] ?? 0;
    $blocked = $mine >= $perIp || $data['total'] >= $total;
    if (!$blocked) {
        $data['ip'][$ip] = $mine + 1;
        $data['total']++;
        ftruncate($fh, 0);
        rewind($fh);
        fwrite($fh, json_encode($data));
    }
    flock($fh, LOCK_UN);
    fclose($fh);
    foreach (glob("$dataDir/$bucket-*.json") ?: [] as $old) {
        if ($old !== $file && filemtime($old) < time() - 3 * 86400) @unlink($old);
    }
    return $blocked;
}

function read_json_body(int $max = 4_000_000): array {
    $raw = file_get_contents('php://input', false, null, 0, $max);
    $in = json_decode((string)$raw, true);
    if (!is_array($in)) out(['error' => 'JSON inválido'], 400);
    return $in;
}
