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
- **Ferramentas**:
  - **Radar da concorrência**: cole o link de um post do TikTok/Instagram (ou compartilhe direto para o app no Android) e ele lê perfil, legenda, views e curtidas. Ranking por views/curtidas/engajamento e insights: tipo de vídeo campeão, palavras e hashtags que mais aparecem, duração, preços citados e resumo de tendências pela IA. "Gravar nesse estilo" vira roteiro.
  - **Vitrine com WhatsApp**: página pública (`/glowstudio/loja/<nome>`) para o link na bio; a cliente monta a sacola e o pedido sai pronto no WhatsApp.
  - **Calendário de vendas**: Dia das Mães, Namorados, Black Friday, Natal… com aviso na tela inicial, formato e gancho sugeridos e lembrete no calendário.
  - **Ganchos salvos** que aparecem no roteiro, e legendas adaptadas para **Reels e Shorts**.
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

Automática: cada push na `main` passa nos testes, gera o `dist/` e o GitHub Actions envia por FTPS para
`public_html/glowstudio/` (só o que mudou). Precisa dos segredos do repositório `FTP_USERNAME` e `FTP_PASSWORD`,
de uma conta de FTP do cPanel limitada à pasta `public_html/glowstudio`. Sem os segredos, o CI só testa e compila.

Manual (alternativa): envie o conteúdo de `dist/` para `public_html/glowstudio/`.

### IA (opcional)

`dist/api/ai.php` chama o Gemini pelo servidor. A chave **nunca** vai para o app: crie
`/home4/wiliam35/glowstudio-config.php` (fora do `public_html`) com:

```php
<?php return ['gemini_key' => 'SUA_CHAVE_DO_GEMINI'];
```

Sem a chave, o app funciona normalmente com ganchos e legendas prontos (sem IA).
O servidor limita o uso diário por IP para proteger a cota.

### Outros endpoints

- `api/link.php`: lê um post público do TikTok/Instagram (só esses sites, com limite diário). Não usa a chave.
- `api/vitrine.php` + `loja.php`: guarda e mostra a vitrine. Os dados ficam em `~/glowstudio-data/vitrines/`
  (fora do `public_html`); só o aparelho que criou a vitrine (token) altera ou apaga.
