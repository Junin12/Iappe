package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CenaScript(
    val segundos: Int = 3,
    val acaoDasMaos: String = "",
    val enquadramento: String = "MACRO", // MACRO, MEDIO, DETALHE
    val narracao: String = ""
)

@Serializable
data class GeminiScriptResult(
    val ganchosVariacoes: List<String> = emptyList(),
    val gancho: String = "",
    val textoNaTela: String = "",
    val cenas: List<CenaScript> = emptyList(),
    val cta: String = "",
    val legendaPost: String = "",
    val hashtags: List<String> = emptyList(),
    val sugestaoDeSom: String = "",
    val isOfflineTemplate: Boolean = false
) {
    /**
     * Formats all scene narrations into a continuous teleprompter text.
     */
    fun toTeleprompterText(): String {
        val sb = StringBuilder()
        if (gancho.isNotBlank()) {
            sb.append("🔥 GANCHO: $gancho\n\n")
        }
        cenas.forEachIndexed { index, cena ->
            sb.append("🎬 CENA ${index + 1} (${cena.segundos}s - ${cena.enquadramento}):\n")
            sb.append("👐 Ação: ${cena.acaoDasMaos}\n")
            sb.append("🗣️ Fala: ${cena.narracao}\n\n")
        }
        if (cta.isNotBlank()) {
            sb.append("🛒 CTA: $cta")
        }
        return sb.toString().trim()
    }
}

@Serializable
data class GeminiPhotoAnalysisResult(
    val nomeComercial: String = "",
    val tipo: String = "Colar",
    val pedra: String = "Zircônia",
    val corDoBanho: String = "Ouro 18k",
    val descricaoCurta: String = "",
    val ideiasDeVideo: List<String> = emptyList()
)

@Serializable
data class GeminiCaptionResult(
    val legenda: String = "",
    val hashtags: List<String> = emptyList(),
    val ganchoSugerido: String = "",
    val isOfflineTemplate: Boolean = false
)
