package com.example.data.service

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.local.PecaEntity
import com.example.data.model.CenaScript
import com.example.data.model.GeminiCaptionResult
import com.example.data.model.GeminiPhotoAnalysisResult
import com.example.data.model.GeminiScriptResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class GeminiErrorType {
    NO_API_KEY,
    NETWORK_ERROR,
    INVALID_JSON,
    SERVER_ERROR,
    CANCELLED
}

sealed class GeminiServiceResult<out T> {
    data class Success<T>(val data: T) : GeminiServiceResult<T>()
    data class Error<T>(
        val errorMessage: String,
        val errorType: GeminiErrorType,
        val fallbackTemplate: T? = null
    ) : GeminiServiceResult<T>()
}

class GeminiScriptService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Retorna a chave configurada no BuildConfig (Secrets panel) sem expor ou logar.
     */
    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key.startsWith("YOUR_")) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 1. Gera roteiro faceless estruturado em JSON com dados REAIS da Peca.
     */
    suspend fun generateStructuredScript(
        peca: PecaEntity,
        duracaoSeg: Int = 15,
        estilo: String = "Old Money Elegance",
        objetivo: String = "Choque & Objeção",
        customNotes: String = ""
    ): GeminiServiceResult<GeminiScriptResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val fallback = buildOfflineScriptTemplate(peca, duracaoSeg, estilo, objetivo, customNotes)

        if (apiKey.isBlank()) {
            return@withContext GeminiServiceResult.Error(
                errorMessage = "Chave da API do Gemini não configurada no Secrets. Usando modelo pronto offline.",
                errorType = GeminiErrorType.NO_API_KEY,
                fallbackTemplate = fallback
            )
        }

        try {
            ensureActive()
            val prompt = buildScriptPrompt(peca, duracaoSeg, estilo, objetivo, customNotes)
            val systemInstruction = getScriptSystemInstruction(duracaoSeg)

            val payload = buildJsonPayload(
                prompt = prompt,
                systemInstruction = systemInstruction
            )

            val rawResponse = executeGeminiRequest(apiKey, payload)
            ensureActive()

            val cleanJson = cleanJsonString(rawResponse)
            val parsedResult = try {
                GeminiConfig.jsonParser.decodeFromString<GeminiScriptResult>(cleanJson)
            } catch (e: Exception) {
                return@withContext GeminiServiceResult.Error(
                    errorMessage = "A resposta da IA não pôde ser interpretada como JSON estruturado: ${e.localizedMessage}",
                    errorType = GeminiErrorType.INVALID_JSON,
                    fallbackTemplate = fallback
                )
            }

            // Normalização: garantir que o gancho principal está preenchido
            val normalizedResult = if (parsedResult.gancho.isBlank() && parsedResult.ganchosVariacoes.isNotEmpty()) {
                parsedResult.copy(gancho = parsedResult.ganchosVariacoes.first(), isOfflineTemplate = false)
            } else {
                parsedResult.copy(isOfflineTemplate = false)
            }

            GeminiServiceResult.Success(normalizedResult)
        } catch (ce: CancellationException) {
            GeminiServiceResult.Error(
                errorMessage = "Geração cancelada pelo usuário.",
                errorType = GeminiErrorType.CANCELLED,
                fallbackTemplate = fallback
            )
        } catch (ioe: IOException) {
            GeminiServiceResult.Error(
                errorMessage = "Sem conexão ou tempo limite excedido. Verifique sua internet.",
                errorType = GeminiErrorType.NETWORK_ERROR,
                fallbackTemplate = fallback
            )
        } catch (e: Exception) {
            GeminiServiceResult.Error(
                errorMessage = "Erro ao chamar o Gemini: ${e.localizedMessage ?: "Erro desconhecido"}",
                errorType = GeminiErrorType.SERVER_ERROR,
                fallbackTemplate = fallback
            )
        }
    }

    /**
     * 2. Analisa foto da peça (Multimodal) e retorna dados de catálogo em JSON estruturado.
     */
    suspend fun analyzeJewelryPhoto(
        context: Context,
        imageUri: Uri
    ): GeminiServiceResult<GeminiPhotoAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val fallback = buildOfflinePhotoAnalysisFallback()

        if (apiKey.isBlank()) {
            return@withContext GeminiServiceResult.Error(
                errorMessage = "Chave da API do Gemini não configurada no Secrets. Preencha os dados manualmente.",
                errorType = GeminiErrorType.NO_API_KEY,
                fallbackTemplate = fallback
            )
        }

        try {
            ensureActive()
            // Redimensiona para no máximo 1024px antes de codificar em Base64
            val base64Image = ImageProcessingUtils.loadAndResizeImageAsBase64(context, imageUri)
                ?: return@withContext GeminiServiceResult.Error(
                    errorMessage = "Não foi possível carregar e processar a foto selecionada.",
                    errorType = GeminiErrorType.INVALID_JSON,
                    fallbackTemplate = fallback
                )

            val prompt = """
                Analise esta foto de semijoia para cadastro no catálogo da criadora de conteúdo do TikTok.
                Identifique os detalhes da peça com precisão.
                
                Retorne estritamente um JSON com estes campos:
                {
                  "nomeComercial": "Nome comercial elegante e chamativo para venda (ex: 'Colar Choker Fita Banhado a Ouro 18k')",
                  "tipo": "Uma das opções exatas: 'Colar', 'Brinco', 'Anel', 'Pulseira', 'Conjunto', 'Tornozeleira'",
                  "pedra": "Uma das opções exatas: 'Zircônia', 'Pérola', 'Cristal', 'Nenhuma'",
                  "corDoBanho": "Uma das opções exatas: 'Ouro 18k', 'Ródio', 'Prata', 'Outro'",
                  "descricaoCurta": "Descrição concisa dos detalhes visuais, acabamento e brilho (máximo 2 frases)",
                  "ideiasDeVideo": [
                    "Ideia 1 de vídeo faceless focando em close das mãos",
                    "Ideia 2 de teste de resistência ou água",
                    "Ideia 3 de combinação com look elegante"
                  ]
                }
            """.trimIndent()

            val systemInstruction = "Você é um especialista em gemologia e semijoias finas. Responda em português do Brasil estritamente em JSON."

            val payload = buildMultimodalJsonPayload(
                prompt = prompt,
                systemInstruction = systemInstruction,
                base64Image = base64Image
            )

            val rawResponse = executeGeminiRequest(apiKey, payload)
            ensureActive()

            val cleanJson = cleanJsonString(rawResponse)
            val parsedResult = try {
                GeminiConfig.jsonParser.decodeFromString<GeminiPhotoAnalysisResult>(cleanJson)
            } catch (e: Exception) {
                return@withContext GeminiServiceResult.Error(
                    errorMessage = "Falha ao analisar os dados da foto: ${e.localizedMessage}",
                    errorType = GeminiErrorType.INVALID_JSON,
                    fallbackTemplate = fallback
                )
            }

            GeminiServiceResult.Success(parsedResult)
        } catch (ce: CancellationException) {
            GeminiServiceResult.Error("Operação cancelada.", GeminiErrorType.CANCELLED, fallback)
        } catch (ioe: IOException) {
            GeminiServiceResult.Error("Falha de rede ao enviar a imagem.", GeminiErrorType.NETWORK_ERROR, fallback)
        } catch (e: Exception) {
            GeminiServiceResult.Error("Erro ao processar imagem: ${e.localizedMessage}", GeminiErrorType.SERVER_ERROR, fallback)
        }
    }

    /**
     * 3. Gerador avulso de legenda e hashtags para vídeo já gravado.
     */
    suspend fun generateCaptionsAndHashtags(
        peca: PecaEntity,
        gancho: String = ""
    ): GeminiServiceResult<GeminiCaptionResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val fallback = buildOfflineCaptionFallback(peca, gancho)

        if (apiKey.isBlank()) {
            return@withContext GeminiServiceResult.Error(
                errorMessage = "Chave da API do Gemini não configurada. Usando legenda pronta.",
                errorType = GeminiErrorType.NO_API_KEY,
                fallbackTemplate = fallback
            )
        }

        try {
            ensureActive()
            val prompt = """
                Crie legenda e hashtags virais para TikTok Shop de semijoias.
                Dados da peça:
                - Nome: ${peca.nome}
                - Tipo: ${peca.tipo}
                - Banho: ${peca.banho} (${peca.milesimos} milésimos)
                - Pedra: ${peca.pedra}
                - Preço: R$ ${String.format(Locale.US, "%.2f", peca.preco)}
                ${if (peca.precoPromocional != null) "- Preço Promocional: R$ ${String.format(Locale.US, "%.2f", peca.precoPromocional)}" else ""}
                - Gancho usado no vídeo: "${if (gancho.isNotBlank()) gancho else "Brilho impecável no banho nobre"}"

                Retorne estritamente um JSON com o esquema:
                {
                  "legenda": "Texto persuasivo com chamada para a sacolinha amarela (máximo 150 caracteres)",
                  "hashtags": ["#semijoias", "#tiktokshopbrasil", "#ouro18k", "#acessoriosfemininos", "#achadinhos"],
                  "ganchoSugerido": "Gancho complementar em 1 frase"
                }
            """.trimIndent()

            val systemInstruction = "Você é redatora profissional de TikTok Shop no Brasil. Responda apenas em JSON válido. Nunca prometa ouro maciço ou antialérgico absoluto."
            val payload = buildJsonPayload(prompt, systemInstruction)
            val rawResponse = executeGeminiRequest(apiKey, payload)
            ensureActive()

            val cleanJson = cleanJsonString(rawResponse)
            val parsedResult = try {
                GeminiConfig.jsonParser.decodeFromString<GeminiCaptionResult>(cleanJson)
            } catch (e: Exception) {
                return@withContext GeminiServiceResult.Error(
                    errorMessage = "Erro ao decodificar legenda da IA: ${e.localizedMessage}",
                    errorType = GeminiErrorType.INVALID_JSON,
                    fallbackTemplate = fallback
                )
            }

            GeminiServiceResult.Success(parsedResult.copy(isOfflineTemplate = false))
        } catch (e: Exception) {
            GeminiServiceResult.Error(
                errorMessage = "Erro ao gerar legenda: ${e.localizedMessage}",
                errorType = GeminiErrorType.SERVER_ERROR,
                fallbackTemplate = fallback
            )
        }
    }

    // --- Helpers de Montagem de Prompt & Payload ---

    fun buildScriptPrompt(
        peca: PecaEntity,
        duracaoSeg: Int,
        estilo: String,
        objetivo: String,
        customNotes: String
    ): String {
        val precoFormatado = String.format(Locale.US, "%.2f", peca.preco)
        val promoFormatado = peca.precoPromocional?.let { String.format(Locale.US, "%.2f", it) }

        return """
            Gere um roteiro de vídeo comercial para TikTok Shop focado exclusivamente em venda da seguinte semijoia:
            
            DADOS REAIS DA PEÇA DO CATÁLOGO:
            - Nome: ${peca.nome}
            - Categoria/Tipo: ${peca.tipo}
            - Banho: ${peca.banho} com ${peca.milesimos} milésimos de camada nobre
            - Pedra: ${peca.pedra}
            - Preço de Venda: R$ $precoFormatado
            ${if (promoFormatado != null) "- Preço Promocional Exclusivo: R$ $promoFormatado" else ""}
            ${if (peca.codigoSku.isNotBlank()) "- SKU: ${peca.codigoSku}" else ""}
            - Estilo de Conteúdo Desejado: $estilo
            - Estratégia de Gancho: $objetivo
            - Duração Alvo do Vídeo: ${duracaoSeg}s
            ${if (customNotes.isNotBlank()) "- Observações Especiais / Mimo: $customNotes" else ""}

            REGRAS OBRIGATÓRIAS DE SAÍDA:
            Retorne APENAS um objeto JSON válido, sem texto fora dele, seguindo este formato exato:
            {
              "ganchosVariacoes": [
                "Variação 1 de gancho viral falado nos primeiros 3s (focado em curiosidade/choque)",
                "Variação 2 de gancho viral falado nos primeiros 3s (focado em valor/preço)",
                "Variação 3 de gancho viral falado nos primeiros 3s (focado em sensorial/ASMR)"
              ],
              "gancho": "O gancho recomendado selecionado por padrão",
              "textoNaTela": "Texto impactante para tela (máximo 6 palavras)",
              "cenas": [
                {
                  "segundos": 3,
                  "acaoDasMaos": "Ação visual precisa apenas com mãos/peça (ex: segurando no veludo)",
                  "enquadramento": "MACRO",
                  "narracao": "Fala da criadora para este trecho"
                }
              ],
              "cta": "Chamada para ação para a sacolinha amarela do TikTok Shop",
              "legendaPost": "Legenda persuasiva para o post com máximo 150 caracteres",
              "hashtags": ["#semijoias", "#tiktokshopbrasil", "#ouro18k", "#acessorios", "#mixdejoias"],
              "sugestaoDeSom": "Descrição do estilo da trilha sonora instrumental recomendada"
            }
        """.trimIndent()
    }

    fun getScriptSystemInstruction(duracaoSeg: Int): String {
        return """
            Você é um roteirista sênior do TikTok Shop especializado em vídeos FACELESS (100% sem rosto) de semijoias finas.
            
            DIRETRIZES FUNDAMENTAIS E INEGOCIÁVEIS:
            1. SEM ROSTO: O vídeo NUNCA filma o rosto. Filma exclusivamente as mãos da criadora, a semijoia sob luz direta, o colo/pescoço, pulso ou orelha.
            2. PORTUGUÊS DO BRASIL NATURAL: Linguagem falada, coloquial, moderna, envolvente e comercial.
            3. VERACIDADE E CONFORMIDADE: NUNCA diga 'ouro maciço', 'não escurece nunca' ou 'garantia vitalícia/antialérgica absoluta'. Use estritamente as especificações informadas (ex: banho nobre com X milésimos de camada protetora).
            4. TEMPO EXATO: A soma das durações das cenas deve totalizar exatamente ${duracaoSeg} segundos (tolerância máxima de ±2s).
            5. GANCHO EM 3 SEGUNDOS: O gancho deve ser imediato para prender o scroll no primeiro segundo.
            6. RETORNE EXCLUSIVAMENTE JSON no formato MIME application/json solicitado.
        """.trimIndent()
    }

    suspend fun generateFacelessScript(
        jewelryStyle: String,
        pieceCategory: String,
        hookStrategy: String,
        targetPrice: String,
        storeDetails: String
    ): String = withContext(Dispatchers.IO) {
        val rawPrice = targetPrice.replace("R$", "").replace(",", ".").trim().toDoubleOrNull() ?: 49.90
        val peca = PecaEntity(
            nome = "$pieceCategory GlowStudio",
            tipo = pieceCategory,
            banho = "Ouro 18k",
            milesimos = 10,
            pedra = "Zircônia",
            preco = rawPrice
        )
        val result = generateStructuredScript(
            peca = peca,
            duracaoSeg = 15,
            estilo = jewelryStyle,
            objetivo = hookStrategy,
            customNotes = storeDetails
        )
        when (result) {
            is GeminiServiceResult.Success -> result.data.toTeleprompterText()
            is GeminiServiceResult.Error -> result.fallbackTemplate?.toTeleprompterText() ?: buildOfflineScriptTemplate(peca, 15, jewelryStyle, hookStrategy, storeDetails).toTeleprompterText()
        }
    }

    fun generateFallbackFacelessScript(
        jewelryStyle: String,
        pieceCategory: String,
        hookStrategy: String,
        targetPrice: String,
        storeDetails: String
    ): String {
        val rawPrice = targetPrice.replace("R$", "").replace(",", ".").trim().toDoubleOrNull() ?: 49.90
        val peca = PecaEntity(
            nome = "$pieceCategory GlowStudio",
            tipo = pieceCategory,
            banho = "Ouro 18k",
            milesimos = 10,
            pedra = "Zircônia",
            preco = rawPrice
        )
        return buildOfflineScriptTemplate(peca, 15, jewelryStyle, hookStrategy, storeDetails).toTeleprompterText()
    }

    private fun buildJsonPayload(
        prompt: String,
        systemInstruction: String
    ): String {
        val root = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }
        return root.toString()
    }

    private fun buildMultimodalJsonPayload(
        prompt: String,
        systemInstruction: String,
        base64Image: String
    ): String {
        val root = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                        add(buildJsonObject {
                            put("inlineData", buildJsonObject {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }
        return root.toString()
    }

    private fun executeGeminiRequest(apiKey: String, jsonPayload: String): String {
        return tryExecuteGeminiRequest(apiKey, jsonPayload, GeminiConfig.GEMINI_MODEL)
    }

    private fun tryExecuteGeminiRequest(apiKey: String, jsonPayload: String, model: String): String {
        val url = GeminiConfig.getGenerateContentUrl(apiKey, model)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404 && model != GeminiConfig.FALLBACK_MODEL) {
                // Tenta automaticamente o fallback "gemini-2.5-flash" antes de mostrar erro
                return tryExecuteGeminiRequest(apiKey, jsonPayload, GeminiConfig.FALLBACK_MODEL)
            }

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                if ((response.code == 404 || errorBody.contains("not found", ignoreCase = true) || errorBody.contains("is not supported", ignoreCase = true))
                    && model != GeminiConfig.FALLBACK_MODEL) {
                    return tryExecuteGeminiRequest(apiKey, jsonPayload, GeminiConfig.FALLBACK_MODEL)
                }
                throw IOException("Gemini API error ${response.code}: $errorBody")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body from Gemini API")
            
            // Extrai o conteúdo do primeiro candidato
            val jsonRoot = GeminiConfig.jsonParser.parseToJsonElement(body).jsonObject
            val candidates = jsonRoot["candidates"]?.jsonArray
            if (candidates.isNullOrEmpty()) {
                throw IOException("No candidates returned from Gemini API")
            }
            val firstCandidate = candidates[0].jsonObject
            val content = firstCandidate["content"]?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            if (parts.isNullOrEmpty()) {
                throw IOException("No parts returned from Gemini API candidate")
            }
            return parts[0].jsonObject["text"]?.jsonPrimitive?.content ?: ""
        }
    }

    fun cleanJsonString(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    // --- Templates Offline Labeled "Modelo pronto" ---

    fun buildOfflineScriptTemplate(
        peca: PecaEntity,
        duracaoSeg: Int,
        estilo: String,
        objetivo: String,
        customNotes: String
    ): GeminiScriptResult {
        val precoFormatado = "R$ " + String.format(Locale.US, "%.2f", peca.preco)
        val banhoDesc = "${peca.banho} (${peca.milesimos} milésimos)"

        val ganchos = listOf(
            "\"Disseram que semijoia banhada em $banhoDesc preteja no primeiro banho... olha o teste que eu fiz com esse ${peca.tipo}!\"",
            "\"Minha amiga achou que esse ${peca.tipo} cravejado em ${peca.pedra} custou 2 mil reais em loja de shopping. Paguei só $precoFormatado!\"",
            "\"Aumenta o som e repara no brilho impecável desse acabamento de alta joalheria no veludo...\""
        )

        val cenas = when (duracaoSeg) {
            15 -> listOf(
                CenaScript(
                    segundos = 3,
                    acaoDasMaos = "Mão segura o ${peca.tipo} contra fundo de veludo escuro, girando para reluzir a luz.",
                    enquadramento = "MACRO",
                    narracao = ganchos[0]
                ),
                CenaScript(
                    segundos = 8,
                    acaoDasMaos = "Dedos demonstram a resistência do fecho e o brilho uniforme do banho de ${peca.milesimos} milésimos.",
                    enquadramento = "DETALHE",
                    narracao = "Tem padrão de alta joalheria com camada nobre de ${peca.banho}. Não descasca e tem brilho espelhado incrível!"
                ),
                CenaScript(
                    segundos = 4,
                    acaoDasMaos = "Dedo indicador aponta para o canto inferior esquerdo onde fica a sacolinha amarela.",
                    enquadramento = "MEDIO",
                    narracao = "Tá com preço direto de fábrica por apenas $precoFormatado na sacolinha amarela. Clica e aproveita!"
                )
            )
            30 -> listOf(
                CenaScript(
                    segundos = 3,
                    acaoDasMaos = "Close imediato nos reflexos da peça sob luz de estúdio girando suavemente.",
                    enquadramento = "MACRO",
                    narracao = ganchos[1]
                ),
                CenaScript(
                    segundos = 9,
                    acaoDasMaos = "Mãos deslizam a peça no colo ou pulso neutro, sem mostrar o rosto, evidenciando caimento e peso nobre.",
                    enquadramento = "MEDIO",
                    narracao = "Esse ${peca.tipo} tem acabamento impecável em $banhoDesc e pedras de ${peca.pedra} com facetação brilhante."
                ),
                CenaScript(
                    segundos = 10,
                    acaoDasMaos = "Abre caixinha de envio aveludada mostrando o cuidado com a embalagem e o certificado do banho.",
                    enquadramento = "DETALHE",
                    narracao = "Você recebe com embalagem de luxo e cupom especial. Parece peça de vitrine de grife, mas sem aquele preço abusivo."
                ),
                CenaScript(
                    segundos = 8,
                    acaoDasMaos = "Mão aponta para o selo da sacolinha amarela do TikTok Shop com leve movimento de pinça.",
                    enquadramento = "MEDIO",
                    narracao = "Garante a sua agora por apenas $precoFormatado na sacolinha amarela aqui embaixo com envio rápido!"
                )
            )
            else -> listOf( // 60 segundos
                CenaScript(
                    segundos = 3,
                    acaoDasMaos = "Mãos cuidadas posicionam o ${peca.tipo} na frente da lente sob iluminação quente.",
                    enquadramento = "MACRO",
                    narracao = ganchos[0]
                ),
                CenaScript(
                    segundos = 15,
                    acaoDasMaos = "Mostra detalhe da solda perfeita, fecho premium e a espessura generosa do banho nobre.",
                    enquadramento = "DETALHE",
                    narracao = "Se você é exigente com acessórios, repara nos ${peca.milesimos} milésimos de ${peca.banho}. Não perde a cor no uso diário e tem toque macio na pele."
                ),
                CenaScript(
                    segundos = 20,
                    acaoDasMaos = "Demonstra como compor um mix ou look de bom gosto com o ${peca.tipo} sem precisar de outros acessórios caros.",
                    enquadramento = "MEDIO",
                    narracao = "Ele transforma qualquer roupa básica numa produção elegante de estilo Old Money em segundos. Quem olha jura que é joia de família."
                ),
                CenaScript(
                    segundos = 12,
                    acaoDasMaos = "Exibe caixinha perfumada com mimo e laço delicado.",
                    enquadramento = "DETALHE",
                    narracao = "Chega na sua casa embalado como presente com todo o carinho e garantia do banho."
                ),
                CenaScript(
                    segundos = 10,
                    acaoDasMaos = "Mão aponta para a sacolinha amarela com energia.",
                    enquadramento = "MEDIO",
                    narracao = "Últimas unidades desse lote por apenas $precoFormatado. Clica na sacolinha amarela antes que esgote!"
                )
            )
        }

        return GeminiScriptResult(
            ganchosVariacoes = ganchos,
            gancho = ganchos[0],
            textoNaTela = "✨ ${peca.nome.take(24).uppercase()} ✨",
            cenas = cenas,
            cta = "Clica na sacolinha amarela aqui embaixo para garantir com frete e cupom especial!",
            legendaPost = "Olha esse brilho no banho ${peca.milesimos}M! ✨ Por apenas $precoFormatado na sacolinha amarela. Corre que o estoque é limitado!",
            hashtags = listOf(
                "#semijoias",
                "#tiktokshopbrasil",
                "#" + peca.tipo.lowercase().replace(" ", ""),
                "#ouro18k",
                "#achadinhos",
                "#acessoriosdeluxo"
            ),
            sugestaoDeSom = "Batida lo-fi elegante acústica suave ou instrumental chic em 110bpm (volume 20%)",
            isOfflineTemplate = true
        )
    }

    fun buildOfflinePhotoAnalysisFallback(): GeminiPhotoAnalysisResult {
        return GeminiPhotoAnalysisResult(
            nomeComercial = "Colar Dourado Delicado Banhado Ouro 18k",
            tipo = "Colar",
            pedra = "Zircônia",
            corDoBanho = "Ouro 18k",
            descricaoCurta = "Acabamento polido espelhado com brilho refinado e design atemporal.",
            ideiasDeVideo = listOf(
                "Close macro no reflexo da peça girando na luz",
                "Teste sensorial de toque e som da peça no veludo",
                "Demonstração do caimento elegante no colo neutro"
            )
        )
    }

    fun buildOfflineCaptionFallback(peca: PecaEntity, gancho: String): GeminiCaptionResult {
        val precoFormatado = "R$ " + String.format(Locale.US, "%.2f", peca.preco)
        return GeminiCaptionResult(
            legenda = "Perfeição em cada detalhe do banho ${peca.milesimos}M! ✨ Garanta o seu por apenas $precoFormatado na sacolinha amarela 👇",
            hashtags = listOf(
                "#semijoias",
                "#tiktokshopbrasil",
                "#" + peca.tipo.lowercase().replace(" ", ""),
                "#ouro18k",
                "#acessoriosfemininos",
                "#joias"
            ),
            ganchoSugerido = if (gancho.isNotBlank()) gancho else "Disseram que semijoia não brilhava como ouro...",
            isOfflineTemplate = true
        )
    }
}
