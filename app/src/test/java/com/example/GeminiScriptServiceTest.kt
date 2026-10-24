package com.example

import com.example.data.local.PecaEntity
import com.example.data.model.GeminiCaptionResult
import com.example.data.model.GeminiPhotoAnalysisResult
import com.example.data.model.GeminiScriptResult
import com.example.data.service.GeminiConfig
import com.example.data.service.GeminiScriptService
import org.junit.Assert.*
import org.junit.Test

class GeminiScriptServiceTest {

    private val service = GeminiScriptService()

    @Test
    fun testBuildScriptPromptWithRealPecaData() {
        val peca = PecaEntity(
            id = 42L,
            nome = "Choker Riviera Zircônia 3mm",
            tipo = "Colar",
            banho = "Ouro 18k",
            milesimos = 10,
            pedra = "Zircônia",
            preco = 149.90,
            precoPromocional = 129.90,
            codigoSku = "RIV-001"
        )

        val prompt = service.buildScriptPrompt(
            peca = peca,
            duracaoSeg = 30,
            estilo = "Old Money Elegance",
            objetivo = "Choque & Objeção",
            customNotes = "Cupom RIVIERA10 com frete grátis"
        )

        // Verificações dos dados reais da peça no prompt
        assertTrue(prompt.contains("Choker Riviera Zircônia 3mm"))
        assertTrue(prompt.contains("Colar"))
        assertTrue(prompt.contains("Ouro 18k"))
        assertTrue(prompt.contains("10 milésimos"))
        assertTrue(prompt.contains("Zircônia"))
        assertTrue(prompt.contains("149.90"))
        assertTrue(prompt.contains("129.90"))
        assertTrue(prompt.contains("RIV-001"))
        assertTrue(prompt.contains("30s"))
        assertTrue(prompt.contains("Cupom RIVIERA10 com frete grátis"))

        // Verificações da system instruction de conformidade e faceless
        val systemInstruction = service.getScriptSystemInstruction(30)
        assertTrue(systemInstruction.contains("SEM ROSTO"))
        assertTrue(systemInstruction.contains("NUNCA filma o rosto"))
        assertTrue(systemInstruction.contains("ouro maciço")) // Proibição explícita
        assertTrue(systemInstruction.contains("não escurece nunca")) // Proibição explícita
        assertTrue(systemInstruction.contains("30 segundos"))
    }

    @Test
    fun testJsonParsingValidScript() {
        val validJson = """
            {
              "ganchosVariacoes": [
                "Disseram que esse colar ia perder a cor no primeiro uso...",
                "Minha amiga pagou 3 mil num colar idêntico no shopping!",
                "Ouve o clique desse fecho de alta joalheria..."
              ],
              "gancho": "Disseram que esse colar ia perder a cor no primeiro uso...",
              "textoNaTela": "TESTE DE RESISTÊNCIA DO BANHO",
              "cenas": [
                {
                  "segundos": 3,
                  "acaoDasMaos": "Mão gira o colar no veludo preto refletindo a luz",
                  "enquadramento": "MACRO",
                  "narracao": "Disseram que esse colar ia perder a cor no primeiro uso..."
                },
                {
                  "segundos": 8,
                  "acaoDasMaos": "Close no fecho e nas pedras com pinça",
                  "enquadramento": "DETALHE",
                  "narracao": "São 10 milésimos de banho nobre com verniz protetor."
                },
                {
                  "segundos": 4,
                  "acaoDasMaos": "Dedo aponta para a sacolinha amarela",
                  "enquadramento": "MEDIO",
                  "narracao": "Garante o seu com cupom na sacolinha amarela aqui embaixo!"
                }
              ],
              "cta": "Clica na sacolinha amarela e garante com cupom!",
              "legendaPost": "Olha o acabamento desse colar banhado a ouro 18k! ✨ Link na sacolinha",
              "hashtags": ["#semijoias", "#tiktokshopbrasil", "#ouro18k", "#colar", "#acessorios"],
              "sugestaoDeSom": "Lo-fi instrumental elegante acústico em 115bpm"
            }
        """.trimIndent()

        val parsed = GeminiConfig.jsonParser.decodeFromString<GeminiScriptResult>(validJson)

        assertEquals(3, parsed.ganchosVariacoes.size)
        assertEquals("Disseram que esse colar ia perder a cor no primeiro uso...", parsed.gancho)
        assertEquals("TESTE DE RESISTÊNCIA DO BANHO", parsed.textoNaTela)
        assertEquals(3, parsed.cenas.size)
        assertEquals(3, parsed.cenas[0].segundos)
        assertEquals("MACRO", parsed.cenas[0].enquadramento)
        assertEquals(8, parsed.cenas[1].segundos)
        assertEquals(5, parsed.hashtags.size)
        assertEquals("Clica na sacolinha amarela e garante com cupom!", parsed.cta)
        assertFalse(parsed.isOfflineTemplate)

        // Testa geração de texto para o teleprompter
        val teleprompter = parsed.toTeleprompterText()
        assertTrue(teleprompter.contains("🔥 GANCHO:"))
        assertTrue(teleprompter.contains("🎬 CENA 1"))
        assertTrue(teleprompter.contains("🛒 CTA:"))
    }

    @Test
    fun testJsonParsingMissingFields() {
        // JSON com apenas o gancho e cenas, faltando outros campos opcionais
        val minimalJson = """
            {
              "gancho": "Gancho essencial da peça",
              "cenas": [
                {
                  "segundos": 15,
                  "narracao": "Narração contínua"
                }
              ]
            }
        """.trimIndent()

        val parsed = GeminiConfig.jsonParser.decodeFromString<GeminiScriptResult>(minimalJson)
        assertEquals("Gancho essencial da peça", parsed.gancho)
        assertEquals(1, parsed.cenas.size)
        assertEquals(15, parsed.cenas[0].segundos)
        assertEquals("MACRO", parsed.cenas[0].enquadramento) // Default value
        assertEquals("", parsed.textoNaTela) // Default value
        assertEquals(emptyList<String>(), parsed.hashtags) // Default value
    }

    @Test
    fun testJsonParsingInvalidSyntaxThrowsFriendly() {
        val brokenJson = "{ \"gancho\": \"incompleto sem fechar chave"

        try {
            GeminiConfig.jsonParser.decodeFromString<GeminiScriptResult>(brokenJson)
            fail("Deveria lançar exceção ao decodificar JSON inválido")
        } catch (e: Exception) {
            assertTrue(e is kotlinx.serialization.SerializationException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testOfflineTemplateExplicitlyLabeled() {
        val peca = PecaEntity(
            nome = "Brinco Gota Chunky",
            tipo = "Brinco",
            banho = "Ouro 18k",
            milesimos = 10,
            preco = 59.90
        )

        val template = service.buildOfflineScriptTemplate(
            peca = peca,
            duracaoSeg = 15,
            estilo = "Old Money",
            objetivo = "Choque & Objeção",
            customNotes = ""
        )

        assertTrue(template.isOfflineTemplate)
        assertEquals(3, template.ganchosVariacoes.size)
        assertTrue(template.gancho.isNotBlank())
        assertTrue(template.cenas.isNotEmpty())
        assertEquals(15, template.cenas.sumOf { it.segundos })
    }

    @Test
    fun testPhotoAnalysisResultParsing() {
        val json = """
            {
              "nomeComercial": "Brinco Gota Dourada Banhado Ouro 18k",
              "tipo": "Brinco",
              "pedra": "Nenhuma",
              "corDoBanho": "Ouro 18k",
              "descricaoCurta": "Formato de gota volumosa polida espelhada com fechamento de tarracha reforçada.",
              "ideiasDeVideo": [
                "Vídeo 1: Teste de peso e caimento segurando a gota na ponta dos dedos",
                "Vídeo 2: Comparação de brilho com bijuteria sem verniz",
                "Vídeo 3: Look office chic minimalista"
              ]
            }
        """.trimIndent()

        val parsed = GeminiConfig.jsonParser.decodeFromString<GeminiPhotoAnalysisResult>(json)
        assertEquals("Brinco Gota Dourada Banhado Ouro 18k", parsed.nomeComercial)
        assertEquals("Brinco", parsed.tipo)
        assertEquals("Nenhuma", parsed.pedra)
        assertEquals("Ouro 18k", parsed.corDoBanho)
        assertEquals(3, parsed.ideiasDeVideo.size)
    }

    @Test
    fun testCaptionResultParsing() {
        val json = """
            {
              "legenda": "Brilho e sofisticação no banho 10 milésimos! Garanta na sacolinha amarela 👇",
              "hashtags": ["#semijoias", "#tiktokshopbrasil", "#ouro18k", "#brincos", "#estilo"],
              "ganchoSugerido": "Achei o brinco perfeito por menos de 60 reais"
            }
        """.trimIndent()

        val parsed = GeminiConfig.jsonParser.decodeFromString<GeminiCaptionResult>(json)
        assertEquals("Brilho e sofisticação no banho 10 milésimos! Garanta na sacolinha amarela 👇", parsed.legenda)
        assertEquals(5, parsed.hashtags.size)
        assertEquals("Achei o brinco perfeito por menos de 60 reais", parsed.ganchoSugerido)
    }

    @Test
    fun testGeminiModelAndFallbackConfiguration() {
        assertEquals("gemini-flash-latest", GeminiConfig.GEMINI_MODEL)
        assertEquals("gemini-2.5-flash", GeminiConfig.FALLBACK_MODEL)
    }
}
