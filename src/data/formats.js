// Formatos de vídeo sem rosto que funcionam para semijoias.
// Cada cena vira um clipe gravado (ou importado) separadamente; o editor junta tudo.
// speed 0.5 = câmera lenta (bom para brilho); o áudio dessa cena é silenciado.

export const FORMATS = [
  {
    id: 'brilho',
    nome: 'Brilho na luz',
    emoji: '✨',
    resumo: 'Câmera lenta girando a peça na luz. O formato mais seguro para começar.',
    cenas: [
      { titulo: 'Gancho em close', acao: 'Peça parada bem perto da câmera, brilho batendo.', plano: 'macro', seg: 3 },
      { titulo: 'Girar na luz', acao: 'Gire devagar a peça perto da janela ou da luz.', plano: 'macro', seg: 4, speed: 0.5 },
      { titulo: 'Detalhe', acao: 'Mostre o fecho, a pedra ou o acabamento.', plano: 'detalhe', seg: 3 },
      { titulo: 'No corpo', acao: 'Peça no pescoço, orelha, dedo ou pulso. Sem rosto.', plano: 'médio', seg: 4 },
      { titulo: 'Preço e chamada', acao: 'Peça na mão ou na caixinha enquanto o preço aparece.', plano: 'médio', seg: 3, fim: true },
    ],
  },
  {
    id: 'unboxing',
    nome: 'Unboxing ASMR',
    emoji: '🎁',
    resumo: 'Som de caixinha abrindo e a peça aparecendo. Grave em silêncio.',
    cenas: [
      { titulo: 'Caixinha fechada', acao: 'Caixa no centro, dedos tocando a tampa.', plano: 'médio', seg: 3 },
      { titulo: 'Abrindo', acao: 'Abra devagar, deixe o som aparecer.', plano: 'detalhe', seg: 4 },
      { titulo: 'Revelação', acao: 'Tire a peça da caixa e levante para a luz.', plano: 'macro', seg: 3, speed: 0.5 },
      { titulo: 'Close', acao: 'Aproxime até ver os detalhes.', plano: 'macro', seg: 3 },
      { titulo: 'Vestindo', acao: 'Coloque a peça e mostre o resultado.', plano: 'médio', seg: 3, fim: true },
    ],
  },
  {
    id: 'tres-jeitos',
    nome: '3 jeitos de usar',
    emoji: '👗',
    resumo: 'A mesma peça em três looks. Faz a cliente imaginar o dia a dia.',
    cenas: [
      { titulo: 'Gancho', acao: 'Peça na palma da mão, bem iluminada.', plano: 'detalhe', seg: 2 },
      { titulo: 'Look 1: dia a dia', acao: 'Com camiseta ou blusa básica.', plano: 'médio', seg: 4 },
      { titulo: 'Look 2: trabalho', acao: 'Com camisa ou blazer.', plano: 'médio', seg: 4 },
      { titulo: 'Look 3: festa', acao: 'Com decote ou roupa de noite.', plano: 'médio', seg: 4 },
      { titulo: 'Chamada', acao: 'Peça em close enquanto o preço aparece.', plano: 'macro', seg: 2, fim: true },
    ],
  },
  {
    id: 'antes-depois',
    nome: 'Antes e depois',
    emoji: '🔄',
    resumo: 'Look sem acessório e depois com a peça. Transformação em 10 segundos.',
    cenas: [
      { titulo: 'Antes', acao: 'Colo ou orelha sem nenhum acessório.', plano: 'médio', seg: 3 },
      { titulo: 'Colocando', acao: 'Mãos colocando a peça com calma.', plano: 'detalhe', seg: 4 },
      { titulo: 'Depois', acao: 'Mesmo enquadramento do "antes", agora com a peça.', plano: 'médio', seg: 4 },
      { titulo: 'Close final', acao: 'Aproxime para mostrar o brilho.', plano: 'macro', seg: 3, fim: true },
    ],
  },
  {
    id: 'presente',
    nome: 'POV: o presente',
    emoji: '💝',
    resumo: 'Sugere a peça como presente. Ótimo perto de datas especiais.',
    cenas: [
      { titulo: 'Embrulho', acao: 'Caixinha com laço sobre a mesa.', plano: 'médio', seg: 3 },
      { titulo: 'Entregando', acao: 'Mãos estendendo o presente para a câmera.', plano: 'médio', seg: 3 },
      { titulo: 'Abrindo', acao: 'Desfaça o laço e abra a caixa.', plano: 'detalhe', seg: 4 },
      { titulo: 'A peça', acao: 'Levante a peça para a luz.', plano: 'macro', seg: 3, speed: 0.5 },
      { titulo: 'Chamada', acao: 'Peça na caixinha aberta enquanto o preço aparece.', plano: 'médio', seg: 2, fim: true },
    ],
  },
  {
    id: 'adivinha',
    nome: 'Adivinha o preço',
    emoji: '💰',
    resumo: 'Mostra a peça e só revela o preço no fim. Gera comentários.',
    cenas: [
      { titulo: 'Peça girando', acao: 'Gire a peça na luz sem mostrar etiqueta.', plano: 'macro', seg: 4, speed: 0.5 },
      { titulo: 'No corpo', acao: 'Mostre como fica vestida.', plano: 'médio', seg: 3 },
      { titulo: 'Suspense', acao: 'Mão cobrindo e descobrindo a peça.', plano: 'detalhe', seg: 3 },
      { titulo: 'Revelação', acao: 'Peça em destaque enquanto o preço aparece.', plano: 'médio', seg: 3, fim: true },
    ],
  },
  {
    id: 'mix',
    nome: 'Montando o mix',
    emoji: '🧷',
    resumo: 'Combina peças uma a uma. Ideal para vender conjuntos.',
    cenas: [
      { titulo: 'Base', acao: 'Comece com uma peça só.', plano: 'médio', seg: 3 },
      { titulo: 'Segunda peça', acao: 'Adicione outra peça ao look.', plano: 'médio', seg: 3 },
      { titulo: 'Terceira peça', acao: 'Complete o mix.', plano: 'médio', seg: 3 },
      { titulo: 'Resultado', acao: 'Mostre o conjunto com movimento lento.', plano: 'médio', seg: 4, speed: 0.5, fim: true },
    ],
  },
  {
    id: 'qualidade',
    nome: 'Prova de qualidade',
    emoji: '🔍',
    resumo: 'Mostra banho, acabamento e embalagem. Passa confiança.',
    cenas: [
      { titulo: 'Banho de perto', acao: 'Close no banho mostrando a cor.', plano: 'macro', seg: 3 },
      { titulo: 'Acabamento', acao: 'Fecho, solda e cravação das pedras.', plano: 'detalhe', seg: 4 },
      { titulo: 'Embalagem', acao: 'Caixinha, saquinho e cartão de garantia.', plano: 'médio', seg: 3 },
      { titulo: 'Chamada', acao: 'Peça vestida enquanto o preço aparece.', plano: 'médio', seg: 3, fim: true },
    ],
  },
];

export const formatById = (id) => FORMATS.find((f) => f.id === id) || FORMATS[0];

export const totalSeconds = (format) => format.cenas.reduce((s, c) => s + c.seg, 0);
