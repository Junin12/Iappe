# GlowStudio 2.0

Estúdio de bolso para criar vídeos de semijoias para o TikTok **sem mostrar o rosto**.
App web instalável (PWA): funciona no **iPhone (Safari)** e no **Android (Chrome)**, direto de
https://wiapps.com.br/glowstudio/.

## O que faz

- **Catálogo de peças** com foto, preço, promoção, banho e pedra (IA opcional preenche pela foto).
- **8 formatos prontos** para vídeos sem rosto (Brilho na luz, Unboxing ASMR, 3 jeitos de usar…), cada um com as cenas e o tempo de cada uma.
- **Câmera guiada cena por cena**: zona do rosto marcada, grade, nível, contagem regressiva, lanterna/zoom/exposição quando o aparelho permite, ou importação da galeria.
- **Editor**: junta as cenas, câmera lenta, visuais ("Brilho de Joia", Dourado, Prata…), gancho escrito nos 3 primeiros segundos, preço e chamada no final, corte de cada cena, com ou sem som.
- **Exporta MP4 9:16 (1080×1920)** e compartilha direto no TikTok, com a legenda copiada.
- **Agenda** com lembrete no calendário do celular (.ics), **meta semanal** e sequência de semanas.
- **Resultados**: anota views/vendas e mostra o formato, a peça, o dia e o horário que funcionam melhor para ela.
- Funciona **offline**; os dados ficam no aparelho (IndexedDB), com backup em arquivo.

## Desenvolvimento

```bash
npm install
npm run dev     # http://localhost:5173
npm test        # testes da lógica (timeline, insights, textos)
npm run build   # gera dist/
```

Stack: Vite + Preact + Signals, WebGL (cor e sobreposições), MediaRecorder (gravação e exportação), IndexedDB.

## Publicação (cPanel)

Envie o conteúdo de `dist/` para `public_html/glowstudio/`.

### IA (opcional)

`dist/api/ai.php` chama o Gemini pelo servidor. A chave **nunca** vai para o app: crie
`/home4/wiliam35/glowstudio-config.php` (fora do `public_html`) com:

```php
<?php return ['gemini_key' => 'SUA_CHAVE_DO_GEMINI'];
```

Sem a chave, o app funciona normalmente com ganchos e legendas prontos (sem IA).
O servidor limita o uso diário por IP para proteger a cota.
