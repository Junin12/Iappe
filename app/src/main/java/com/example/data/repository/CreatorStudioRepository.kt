package com.example.data.repository

import androidx.compose.ui.graphics.Color
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class CreatorStudioRepository {

    fun getSampleClips(): List<SampleJewelryClip> {
        return listOf(
            SampleJewelryClip(
                id = "clip_veludo",
                title = "Caixinha de Veludo & Ródio",
                durationText = "0:14",
                category = "Unboxing & Faceless",
                description = "Mãos com unhas cuidadas abrindo caixinha de veludo verde esmeralda com anel solitário",
                defaultCaption = "✨ NUNCA MAIS COMPRO BIJUTERIA QUE FICA PRETA ✨",
                previewGradient = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            ),
            SampleJewelryClip(
                id = "clip_macro",
                title = "Macro Zircônias 5A Brilhando",
                durationText = "0:11",
                category = "Brilho & Detalhes",
                description = "Lente macro girando colar ponto de luz sob iluminação pontual dourada",
                defaultCaption = "💎 PARECE DIAMANTE MAS CUSTA MENOS DE R$ 60 💎",
                previewGradient = listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF381F4B))
            ),
            SampleJewelryClip(
                id = "clip_agua",
                title = "O Teste da Água & Perfume",
                durationText = "0:18",
                category = "Prova de Qualidade",
                description = "Mergulhando a corrente no copo com água para provar resistência ao banho de 10 milésimos",
                defaultCaption = "💧 PODE MOLHAR SIM! BANHO 10 MILÉSIMOS DE OURO 18K",
                previewGradient = listOf(Color(0xFF051937), Color(0xFF004D7A), Color(0xFF008793))
            ),
            SampleJewelryClip(
                id = "clip_mix",
                title = "Montando Mix de Colares Sem Embolar",
                durationText = "0:22",
                category = "Tutorial Estético",
                description = "Colocando choker fita + colar veneziana + medalhão em busto aveludado",
                defaultCaption = "⛓️ O SEGREDO DO MIX DE MILHÕES POR MENOS DE 150 ⛓️",
                previewGradient = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
            )
        )
    }

    fun getShopBadges(): List<ShopBadge> {
        return listOf(
            ShopBadge(
                id = "banho_10m",
                title = "Banho 10 Milésimos",
                subtitle = "Não escurece nem descasca",
                icon = "🛡️",
                accentColor = Gold18k,
                isEnabled = true
            ),
            ShopBadge(
                id = "promo_shop",
                title = "Promoção TikTok Shop",
                subtitle = "Preço de fábrica com cupom",
                icon = "⚡",
                accentColor = TikTokPink,
                isEnabled = true
            ),
            ShopBadge(
                id = "zirconia_5a",
                title = "Zircônia Cúbica 5A",
                subtitle = "Lapidação brilhante idêntica a diamante",
                icon = "💎",
                accentColor = TikTokCyan,
                isEnabled = false
            ),
            ShopBadge(
                id = "frete_gratis",
                title = "Cupom de Frete Grátis",
                subtitle = "Válido na sacolinha do vídeo",
                icon = "🎟️",
                accentColor = SuccessGreen,
                isEnabled = false
            ),
            ShopBadge(
                id = "hipoalergenico",
                title = "100% Níquel Free",
                subtitle = "Hipoalergênico garantido",
                icon = "✨",
                accentColor = GoldWarm,
                isEnabled = false
            )
        )
    }

    fun getSoundEffects(): List<SoundEffectItem> {
        return listOf(
            SoundEffectItem("snd_box", "Caixa de Veludo Abrindo (Sensorial)", "ASMR", "0:02", true),
            SoundEffectItem("snd_clique", "Clique Suave de Fecho Lagosta", "ASMR", "0:01", true),
            SoundEffectItem("snd_pulseira", "Tilintar de Pulseiras e Charms", "ASMR", "0:03", true),
            SoundEffectItem("snd_sparkle", "Efeito Sonoro Brilho Estelar", "Mágico", "0:02", false),
            SoundEffectItem("snd_pop", "Pop Chamativo de Desconto", "Conversão", "0:01", false)
        )
    }

    fun getWinningTemplates(): List<VideoTemplate> {
        return listOf(
            VideoTemplate(
                id = "tpl_teste_agua",
                title = "O Teste Extremo da Água (Objeção 'Preteja?')",
                hookSummary = "Quebra imediata da maior dúvida de quem compra semijoias online.",
                nicheCategory = "Prova Social & Conversão",
                avgRetentionRate = "76% até o CTA",
                salesConversionRate = "4.8% no TikTok Shop",
                difficulty = "Fácil (Gravação rápida)",
                filmingStyle = "Faceless (Close na mão e copo de vidro)",
                steps = listOf(
                    TimingStep(
                        timeRange = "0s - 3s",
                        stage = "Gancho Hipnótico",
                        visualAction = "Mão mergulha a corrente de ouro 18k dentro de um copo de água cristalina.",
                        spokenAudio = "\"Disseram que semijoia não pode molhar... olha isso aqui!\"",
                        onScreenText = "💧 O TESTE QUE NENHUMA LOJA FAZ!"
                    ),
                    TimingStep(
                        timeRange = "3s - 8s",
                        stage = "Demonstração & Autoridade",
                        visualAction = "Tira a peça, seca no veludo branco e aproxima em macro mostrando o brilho intacto.",
                        spokenAudio = "\"Nosso banho tem 10 milésimos de ouro com verniz de proteção nanotecnológico. Não descasca nem fica verde.\"",
                        onScreenText = "🛡️ BANHO 10 MILÉSIMOS + VERNIZ ALEMÃO"
                    ),
                    TimingStep(
                        timeRange = "8s - 15s",
                        stage = "Ancoragem de Valor",
                        visualAction = "Mostra o reflexo da luz e a caixinha de luxo que acompanha a peça.",
                        spokenAudio = "\"Parece uma joia de vitrine de shopping de 3 mil reais, mas você paga preço de distribuidor.\"",
                        onScreenText = "✨ ACABAMENTO DE JOALHERIA"
                    ),
                    TimingStep(
                        timeRange = "15s - 20s",
                        stage = "CTA TikTok Shop",
                        visualAction = "Dedo aponta para o canto inferior esquerdo onde fica a sacolinha amarela.",
                        spokenAudio = "\"Liberamos cupom de primeira compra na sacolinha aqui embaixo. Aproveita antes que o lote esgote!\"",
                        onScreenText = "👇 CLICA NA SACOLINHA AMARANTE COM FRETE GRÁTIS"
                    )
                ),
                suggestedAudio = "TikTok Viral Sound: 'Luxury Chill Beat 120bpm'",
                recommendedHashtags = listOf("#semijoiasdeluxo", "#tiktokshopbrasil", "#semijoias", "#testedaagua", "#achadinhos"),
                shopCtaText = "Garantir a minha semijoia com banho 10 milésimos na sacolinha!"
            ),
            VideoTemplate(
                id = "tpl_embalando_pedido",
                title = "Embalando Pedido ASMR de Seguidora",
                hookSummary = "Vídeo sensorial que desperta o desejo incontrolável de receber uma encomenda linda.",
                nicheCategory = "ASMR & Conexão",
                avgRetentionRate = "82% até o final",
                salesConversionRate = "5.2% no TikTok Shop",
                difficulty = "Médio (Requer mesa organizada)",
                filmingStyle = "Faceless (Visão de cima - Top Down)",
                steps = listOf(
                    TimingStep(
                        timeRange = "0s - 3s",
                        stage = "Gancho de Curiosidade",
                        visualAction = "Som bem alto de papel seda sendo aberto com unhas fazendo 'tap tap' na caixa.",
                        spokenAudio = "\"A Juliana gastou menos de 90 reais e olha o tanto de coisa que ela vai receber...\"",
                        onScreenText = "📦 PEDIDO DA JU POR MENOS DE R$ 90 😱"
                    ),
                    TimingStep(
                        timeRange = "3s - 10s",
                        stage = "Apresentação Sensorial",
                        visualAction = "Acomoda colar, brinco gota e anel regulável no berço de veludo com borrifada de cheirinho.",
                        spokenAudio = "\"Coloquei a choker fita de 4mm, o brinco gota chunky que tá esgotando e o nosso cheirinho exclusivo de baunilha.\"",
                        onScreenText = "🌸 CHEIRINHO DA RIQUEZA + EMBALAGEM VIP"
                    ),
                    TimingStep(
                        timeRange = "10s - 18s",
                        stage = "Mimo & Surpresa",
                        visualAction = "Coloca brinde surpresa (anel delicado ou brinco ponto de luz) e fecha com laço de cetim.",
                        spokenAudio = "\"E como ela comprou pelo TikTok Shop, mandei um brinco ponto de luz de presente pra ela.\"",
                        onScreenText = "🎁 BRINDE ESPECIAL EM TODOS OS PEDIDOS"
                    ),
                    TimingStep(
                        timeRange = "18s - 25s",
                        stage = "Chamada Direta",
                        visualAction = "Mostra a caixinha lacrada e o link da sacolinha piscando.",
                        spokenAudio = "\"Quer que eu embale o seu no próximo vídeo? Clica na sacolinha aqui embaixo e comenta 'Eu quero'!\"",
                        onScreenText = "👇 PEÇA NA SACOLINHA E GANHE BRINDE"
                    )
                ),
                suggestedAudio = "Original Sound: 'ASMR Velvet Packaging Sounds'",
                recommendedHashtags = listOf("#embalandopedido", "#asmrsemijoias", "#unboxingsemijoias", "#tiktokshop", "#comprinhas"),
                shopCtaText = "Comprar na sacolinha para ganhar embalagem premium e brinde!"
            ),
            VideoTemplate(
                id = "tpl_ancoragem_preco",
                title = "Parece R$ 4.000 custando R$ 49 (Ancoragem)",
                hookSummary = "Gera choque de percepção: alta elegância visual comparada com preço super acessível.",
                nicheCategory = "Compra por Impulso",
                avgRetentionRate = "79% no gancho",
                salesConversionRate = "6.1% no TikTok Shop",
                difficulty = "Muito Fácil",
                filmingStyle = "Faceless ou Apresentado",
                steps = listOf(
                    TimingStep(
                        timeRange = "0s - 3s",
                        stage = "Choque Visual",
                        visualAction = "Colar riviera reluzindo com flash ligado em fundo escuro.",
                        spokenAudio = "\"Minha amiga jurou que essa riviera custou 4 mil reais no shopping.\"",
                        onScreenText = "💎 R$ 4.000 VS R$ 49,90?!"
                    ),
                    TimingStep(
                        timeRange = "3s - 8s",
                        stage = "A Revelação",
                        visualAction = "Mostra o fecho gaveta com trava dupla idêntico a joalheria fina.",
                        spokenAudio = "\"Quando contei que foi menos de cinquenta reais com cupom no TikTok Shop ela quase caiu pra trás.\"",
                        onScreenText = "💸 PREÇO DIRETO DE FÁBRICA"
                    ),
                    TimingStep(
                        timeRange = "8s - 15s",
                        stage = "Detalhe Técnico",
                        visualAction = "Movimento suave mostrando que as pedras não viram nem entortam no pescoço.",
                        spokenAudio = "\"Zircônia 5A cravejada dente a dente, fecho duplo que não abre sozinho e garantia de 1 ano no banho.\"",
                        onScreenText = "✨ CRAVAÇÃO DENTE A DENTE"
                    ),
                    TimingStep(
                        timeRange = "15s - 20s",
                        stage = "Urgência de Estoque",
                        visualAction = "Mostra últimas peças na bandeja de exibição.",
                        spokenAudio = "\"O estoque dessa leva tá acabando. Aproveita o cupom de desconto na sacolinha antes de voltar ao preço normal!\"",
                        onScreenText = "🔥 ÚLTIMAS 17 PEÇAS COM ESTE VALOR"
                    )
                ),
                suggestedAudio = "TikTok Trend: 'Dramatic Luxury Strings Loop'",
                recommendedHashtags = listOf("#achadinhosdotiktok", "#semijoiasfinas", "#riviera", "#tiktokshopachados", "#acessoriosfemininos"),
                shopCtaText = "Garantir a riviera com desconto exclusivo na sacolinha!"
            ),
            VideoTemplate(
                id = "tpl_mix_perfeito",
                title = "POV: Você descobriu o segredo do Mix sem embolar",
                hookSummary = "Conteúdo educativo que vende 3 a 4 peças juntas no mesmo carrinho.",
                nicheCategory = "Ticket Médio Alto (Vende Kits)",
                avgRetentionRate = "74% até o final",
                salesConversionRate = "5.5% no TikTok Shop",
                difficulty = "Fácil",
                filmingStyle = "Faceless (Busto ou pescoço com top neutro)",
                steps = listOf(
                    TimingStep(
                        timeRange = "0s - 3s",
                        stage = "Problema Comum",
                        visualAction = "Mostra colares finos embolados e depois o corte para o mix alinhado e estético.",
                        spokenAudio = "\"Se os seus colares sempre viram um nó quando você sai de casa, você tá montando errado.\"",
                        onScreenText = "❌ CHEGA DE COLAR EMBOLADO!"
                    ),
                    TimingStep(
                        timeRange = "3s - 10s",
                        stage = "A Fórmula 3-Camadas",
                        visualAction = "Coloca primeiro a choker fita (40cm), depois a veneziana com pingente (45cm) e a longa (55cm).",
                        spokenAudio = "\"A regra é simples: base rígida primeiro, pingente no meio para dar peso e corrente lisa no comprimento maior.\"",
                        onScreenText = "📐 A REGRA DOS 3 COMPRIMENTOS"
                    ),
                    TimingStep(
                        timeRange = "10s - 18s",
                        stage = "Demonstração do Kit",
                        visualAction = "Mostra o trio brilhando junto em harmonia perfeita.",
                        spokenAudio = "\"Esse trio foi desenhado pra não embolar e deixa qualquer camiseta básica com cara de look de mil dólares.\"",
                        onScreenText = "✨ TRANSFORMA QUALQUER LOOK BÁSICO"
                    ),
                    TimingStep(
                        timeRange = "18s - 24s",
                        stage = "Oferta Combo",
                        visualAction = "Mostra a caixinha com as 3 peças juntas e selo de desconto no kit.",
                        spokenAudio = "\"Montei esse kit completo com 30% de desconto na sacolinha do TikTok Shop. Clica e garante o seu!\"",
                        onScreenText = "🛍️ KIT COM 30% OFF NA SACOLINHA"
                    )
                ),
                suggestedAudio = "Aesthetic TikTok Beat: 'Girl Boss French Café Beat'",
                recommendedHashtags = listOf("#mixdecolares", "#comousar", "#dicasdeestilo", "#semijoias", "#tiktokshop"),
                shopCtaText = "Pegar o trio completo com 30% de desconto na sacolinha!"
            )
        )
    }

    fun getViralHooks(): List<HookIdea> {
        return listOf(
            HookIdea(
                id = "hook_1",
                category = "Choque & Curiosidade",
                hookText = "\"Se você não quer que as pessoas achem que você é rica, por favor não use esse colar...\"",
                psychologicalReason = "Psicologia reversa gera curiosidade imediata e desejo de status sem parecer vendedor agressivo.",
                retentionScore = 98,
                recommendedPiece = "Colar Riviera de Zircônias ou Choker Fita Ouro 18k"
            ),
            HookIdea(
                id = "hook_2",
                category = "Objeção de Durabilidade",
                hookText = "\"Pode passar perfume, tomar banho de mar e entrar na piscina: se pretejar eu devolvo seu dinheiro dobrado.\"",
                psychologicalReason = "Inversão de risco extrema que destrói na hora a maior objeção de quem compra joias baratas na internet.",
                retentionScore = 96,
                recommendedPiece = "Bracelete Banhado a 10 Milésimos ou Corrente Veneziana"
            ),
            HookIdea(
                id = "hook_3",
                category = "Preço & Ancoragem",
                hookText = "\"A moça da joalheria quase chorou quando descobriu que eu paguei 39 reais nesse anel...\"",
                psychologicalReason = "Storytelling de confronto com o varejo tradicional com vitória do consumidor pelo preço baixo.",
                retentionScore = 95,
                recommendedPiece = "Anel Solitário Regulável ou Trio de Anéis Minimalistas"
            ),
            HookIdea(
                id = "hook_4",
                category = "ASMR & Sensorial (Faceless)",
                hookText = "\"Aumenta o volume e sente a satisfação de abrir a embalagem mais cheirosa do TikTok...\"",
                psychologicalReason = "Comando sensorial direto faz o usuário não passar o vídeo e aguardar a recompensa sonora.",
                retentionScore = 94,
                recommendedPiece = "Caixinha de Veludo Verde com Laço de Cetim"
            ),
            HookIdea(
                id = "hook_5",
                category = "Erro Comum & Estilo",
                hookText = "\"Você tá usando seus brincos totalmente errado e é por isso que sua orelha fica rasgada...\"",
                psychologicalReason = "Alerta de dor e risco pessoal aciona atenção imediata de qualquer mulher que use brincos pesados.",
                retentionScore = 97,
                recommendedPiece = "Tarracha Mágica Coração + Brincos Leves em Gota"
            ),
            HookIdea(
                id = "hook_6",
                category = "Exclusividade TikTok Shop",
                hookText = "\"O algoritmo me fez um favor e escondeu esse link até o lote quase zerar, mas aqui tá a sacolinha secreta...\"",
                psychologicalReason = "Gatilho da escassez e da conspiração amigável do algoritmo para estimular compra por impulso.",
                retentionScore = 93,
                recommendedPiece = "Kit Mix de Pulseiras de Elos Portugueses"
            )
        )
    }

    fun getCompetitorProfiles(): List<CompetitorProfile> {
        return listOf(
            CompetitorProfile(
                id = "comp_1",
                handle = "@aurora.semijoias",
                name = "Aurora Semijoias Finas",
                avatarEmoji = "👑",
                followersCount = "480k",
                monthlySalesEstimated = "R$ 145.000 / mês",
                avgViewsPerVideo = "65.4k visualizações",
                engagementRate = "6.8%",
                postingFrequency = "3 vídeos por dia",
                winningFormat = "Vídeos 100% Faceless com fundo aveludado e luz de estúdio pontual",
                bestPostHours = "12:15h e 19:30h (Pico de almoço e descanso)",
                topSellingJewel = "Choker Fita Banhada a Ouro 18k 4mm",
                growthStrategy = "Nunca mostra rosto: foca 100% no brilho macro e na sacolinha amarela nos primeiros 2 segundos com cupom de desconto.",
                topHashtags = listOf("#semijoias", "#chokerfita", "#tiktokshop", "#ouro18k", "#acessorios")
            ),
            CompetitorProfile(
                id = "comp_2",
                handle = "@lumiere.joias",
                name = "Lumière Acessórios Premium",
                avatarEmoji = "💎",
                followersCount = "290k",
                monthlySalesEstimated = "R$ 98.000 / mês",
                avgViewsPerVideo = "42.1k visualizações",
                engagementRate = "7.4%",
                postingFrequency = "2 vídeos por dia",
                winningFormat = "O teste da água e químicos (Quebra de objeções agressiva)",
                bestPostHours = "18:00h e 21:45h (Noite feminina)",
                topSellingJewel = "Pulseira Riviera com Trava Dupla Anti-Queda",
                growthStrategy = "Responde comentários céticos em vídeo fazendo testes radicais de durabilidade, provando que não preteja.",
                topHashtags = listOf("#testedaagua", "#semijoiasdeluxo", "#riviera", "#tiktokshopbrasil", "#achadinhos")
            ),
            CompetitorProfile(
                id = "comp_3",
                handle = "@boutique.velvet",
                name = "Velvet Joias & Embalagens",
                avatarEmoji = "🌹",
                followersCount = "620k",
                monthlySalesEstimated = "R$ 210.000 / mês",
                avgViewsPerVideo = "110.2k visualizações",
                engagementRate = "8.9%",
                postingFrequency = "4 vídeos curtos por dia",
                winningFormat = "ASMR Embalando Pedidos com som de papel seda e cheirinho borrifado",
                bestPostHours = "11:30h, 17:45h e 22:15h (Horário de relaxamento)",
                topSellingJewel = "Combo Trio de Mix de Colares Banhados",
                growthStrategy = "Vídeos hipnóticos de pedidos reais. O som ultra nítido faz a pessoa assistir 2 ou 3 vezes seguidas, explodindo a retenção.",
                topHashtags = listOf("#embalandopedido", "#asmr", "#semijoiaslovers", "#comprinhas", "#unboxing")
            ),
            CompetitorProfile(
                id = "comp_4",
                handle = "@glow.pratas",
                name = "Glow Prata & Zircônias",
                avatarEmoji = "✨",
                followersCount = "195k",
                monthlySalesEstimated = "R$ 67.000 / mês",
                avgViewsPerVideo = "35.8k visualizações",
                engagementRate = "5.9%",
                postingFrequency = "2 vídeos por dia + Live diária",
                winningFormat = "Comparativo de Bijuteria Barata vs Semijoia Maciça",
                bestPostHours = "13:00h e 20:00h",
                topSellingJewel = "Ear Cuff de Pressão e Argolas Reguláveis",
                growthStrategy = "Mostra peças oxidadas da concorrência e depois a peça dela reluzindo com banho nobre e garantia por escrito.",
                topHashtags = listOf("#prata925", "#earcuff", "#argolinhas", "#semijoiashipoalergenicas", "#dicas")
            )
        )
    }

    fun getTrendRadar(): List<TrendRadarItem> {
        return listOf(
            TrendRadarItem(
                id = "trend_1",
                title = "Choker Fita Dourada 4mm Lisa",
                growthPercent = "+185% buscas",
                tagCategory = "Peça Viral TikTok Shop",
                description = "A peça queridinha da estética 'Old Money' e 'Clean Girl'. Altíssima conversão em compras conjuntas com pingentes.",
                actionAdvice = "Grave um vídeo mostrando ela sozinha e depois adicione um pingente solitário. Anuncie na sacolinha com frete grátis.",
                viralScore = 98
            ),
            TrendRadarItem(
                id = "trend_2",
                title = "Brinco Gota 'Chunky' Oco Banhado",
                growthPercent = "+140% retenção",
                tagCategory = "Peça Viral TikTok Shop",
                description = "Viralizou internacionalmente pelo visual robusto que não pesa na orelha por ser estruturado com núcleo leve.",
                actionAdvice = "Demonstre na balança de precisão que ele pesa menos que uma moeda de 1 real para quebrar o medo de orelha rasgada.",
                viralScore = 95
            ),
            TrendRadarItem(
                id = "trend_3",
                title = "Áudio: 'Chic Luxury Lounge Beats 115bpm'",
                growthPercent = "+92% alcance orgânico",
                tagCategory = "Áudio em Alta",
                description = "Música de fundo elegante sem vocal que o algoritmo do TikTok Shop tem entregue para mais de 70% do público feminino de 24 a 45 anos.",
                actionAdvice = "Deixe o áudio a 15% de volume no app e coloque os efeitos sonoros ASMR da semijoia em 100%.",
                viralScore = 91
            ),
            TrendRadarItem(
                id = "trend_4",
                title = "Gatilho 'Não preteja no suor da academia'",
                growthPercent = "+115% cliques na sacolinha",
                tagCategory = "Gatilho de Venda",
                description = "Com a febre fitness, mulheres procuram semijoias que possam ser usadas no treino sem perder a cor.",
                actionAdvice = "Use o gancho: 'O único colar que você pode treinar, suar e tomar banho sem tirar do pescoço'.",
                viralScore = 94
            )
        )
    }

    fun getAlternativeContentIdeas(): List<ContentAlternativeIdea> {
        return listOf(
            ContentAlternativeIdea(
                id = "alt_1",
                formatType = "Carrossel TikTok (Fotos Estéticas)",
                title = "5 Peças Clássicas que Deixam Qualquer Look com Cara de Rycah",
                objective = "Gera salvamentos massivos e cliques na bio / sacolinha sem gravar nenhum vídeo.",
                stepByStep = listOf(
                    "Slide 1 (Capa): Foto estética em azulejo mármore com o título em fonte elegante.",
                    "Slide 2: Choker fita em close com fundo linho cru.",
                    "Slide 3: Brinco argola fechamento click banhada.",
                    "Slide 4: Anel solitário com luz de janela da tarde.",
                    "Slide 5 (CTA): 'Todas disponíveis no link da sacolinha com desconto de frete hoje!'."
                ),
                scriptOrCopy = "Legenda pronta: Salva esse post para quando você não souber o que usar. Menos de 100 reais para renovar seus acessórios. Link na sacolinha!",
                estimatedReach = "Alto engajamento orgânico de salvamentos (+300%)"
            ),
            ContentAlternativeIdea(
                id = "alt_2",
                formatType = "Live Shop TikTok",
                title = "Roleta de Brindes & Leilão de Peças Únicas",
                objective = "Faturar alto em 2 horas de live com interação instantânea e compras ao vivo.",
                stepByStep = listOf(
                    "Preparação: Tenha uma roleta artesanal ou digital com brindes (brinco ponto de luz, anel regulável, cupom 20%).",
                    "Regra da Live: 'Quem comprar qualquer peça na sacolinha durante a live roda a roleta ao vivo'.",
                    "Apresentação: Mostre as peças direto na mão com luz boa e comente o nome de quem tá comprando.",
                    "Urgência: 'Só temos 4 unidades dessa corrente veneziana, corre na sacolinha!'."
                ),
                scriptOrCopy = "Roteiro de abertura: 'Gente, boa noite! Começou a nossa Live Premiada de Semijoias. Toda compra feita agora ganha presente na hora! Clica no carrinho!'",
                estimatedReach = "Conversão imediata de R$ 1.500 a R$ 5.000 por live"
            ),
            ContentAlternativeIdea(
                id = "alt_3",
                formatType = "Enquete / Quiz nos Stories e TikTok",
                title = "Dourado ou Prata? Qual combina mais com seu tom de pele?",
                objective = "Viralização de comentários e descoberta de preferências de compra.",
                stepByStep = listOf(
                    "Foto/Vídeo lado a lado da mesma modelo com mix ouro no lado esquerdo e mix prata no lado direito.",
                    "Texto provocativo: 'Você é time ouro ou time prata? O resultado surpreende'.",
                    "Comentário fixado: 'Quem for time ouro tem cupom OURO10 na sacolinha e time prata tem PRATA10'."
                ),
                scriptOrCopy = "Copy: 'O debate do século: qual enriquece mais o visual? Comente aqui embaixo qual você nunca tira!'",
                estimatedReach = "Recorde de comentários (+400% interação)"
            )
        )
    }

    fun getInitialAuditCriteria(): List<VideoAuditCriteria> {
        return listOf(
            VideoAuditCriteria("c_hook", "Gancho Visual e Falado nos Primeiros 1.5s", 25, "O vídeo começou com uma pergunta agressiva, corte rápido ou brilho macro sem enrolação?"),
            VideoAuditCriteria("c_faceless_focus", "Foco 100% no Detalhe da Joia (Macro/Brilho)", 20, "A semijoia é a estrela do enquadramento, sem sombras confusas ou mãos trêmulas?"),
            VideoAuditCriteria("c_objecao", "Quebra da Objeção de Oxidação / Banho", 20, "Você mencionou ou provou que a peça tem banho nobre e não preteja?"),
            VideoAuditCriteria("c_captions", "Legendas Grandes e Dinâmicas em Destaque", 15, "Pelo menos 65% das pessoas assistem no mudo; as legendas capturam a atenção sozinhas?"),
            VideoAuditCriteria("c_cta", "Chamada Clara para a Sacolinha do TikTok Shop", 20, "Você indicou onde clicar (sacolinha amarela) e deu um motivo urgente (cupom/frete)?")
        )
    }

    suspend fun generateAiContent(
        promptType: String, // "hooks", "full_script", "content_ideas"
        pieceType: String = "Colar & Choker",
        goal: String = "Venda Rápida TikTok Shop",
        customNotes: String = ""
    ): String {
        // Simulate high-fidelity intelligent generation with realistic latency
        delay(900)
        
        return when (promptType) {
            "hooks" -> {
                """
                🔥 3 GANCHOS DE RETENÇÃO BRUTA PARA $pieceType:
                
                1. "O segredo que as joalherias de shopping não querem que você descubra sobre semijoias no TikTok..."
                   💡 Por que funciona: Ativa o gatilho da informação privilegiada e confronto de autoridade.
                
                2. "Se você usa $pieceType todo dia e ele pretejou, me desculpa mas você caiu num golpe."
                   💡 Por que funciona: Gatilho de dor e validação da frustração do cliente.
                
                3. "Paguei menos de R$ 50 nisso aqui e já me perguntaram três vezes se ganhei de herança!"
                   💡 Por que funciona: Humor leve com desejo irresistível de status e elegância acessível.
                """.trimIndent()
            }
            "full_script" -> {
                """
                🎬 ROTEIRO COMPLETO FACELESS (SEM APARECER)
                Peça: $pieceType | Objetivo: $goal
                
                ⏱️ [00:00 - 00:03] GANCHO HIPNÓTICO
                • Câmera: Macro extremo na pedra reluzindo com flash lateral.
                • Áudio: "Se você ama semijoias finas mas odeia pagar caro em shopping, você precisa ver isso."
                • Texto na tela: ✨ OLHA O BRILHO DESSA PEÇA! ✨
                
                ⏱️ [00:03 - 00:09] O PROBLEMA & A SOLUÇÃO
                • Câmera: Mão segura a peça sobre caixa de veludo, mostrando o acabamento traseiro e o fecho resistente.
                • Áudio: "Diferente de bijuteria descartável que descasca na primeira semana, essa peça tem banho de 10 milésimos de ouro 18k e verniz protetor."
                • Texto na tela: 🛡️ BANHO 10 MILÉSIMOS (NÃO PRETEJA)
                
                ⏱️ [00:09 - 00:15] ANCORAGEM DE PREÇO & SENSAÇÃO
                • Câmera: Movimento suave da corrente escorrendo pelos dedos com som ASMR de metal nobre.
                • Áudio: "O acabamento é idêntico a joalheria de luxo, mas com preço direto do fabricante."
                • Texto na tela: 💎 QUALIDADE DE ALTA JOALHERIA
                
                ⏱️ [00:15 - 00:22] CHAMADA PARA AÇÃO (CTA TIKTOK SHOP)
                • Câmera: Dedo aponta suavemente para o canto inferior esquerdo onde fica a sacolinha amarela.
                • Áudio: "Liberamos cupom de primeira compra com frete grátis na sacolinha amarela aqui embaixo. Corre antes que esgote o lote!"
                • Texto na tela: 👇 CLICA NA SACOLINHA AMARANTE AGORA MESMO!
                """.trimIndent()
            }
            else -> {
                """
                💡 IDEIAS ESTRATÉGICAS DE CONTEÚDO ALÉM DO VÍDEO PADRÃO:
                
                1. CARROSSEL DE LOOKS: "3 Maneiras de Usar $pieceType no Trabalho e na Noite". Foco em estética Clean Girl e fotos de close com camisa de linho e blazer.
                
                2. GUIA DE CUIDADOS (INFOGRÁFICO): "Como Fazer Suas Semijoias Durarem 5 Anos Como Novas". Educar aumenta o valor percebido da sua marca.
                
                3. LIVE SHOP RELÂMPAGO: "Sessão Desapego de Estoque com Brindes na Sacolinha". Ofereça tarrachas mágicas de brinde para quem comprar nos primeiros 10 minutos.
                """.trimIndent()
            }
        }
    }
}
