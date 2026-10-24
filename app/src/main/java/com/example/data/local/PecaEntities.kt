package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object TipoPecaOptions {
    const val COLAR = "Colar"
    const val BRINCO = "Brinco"
    const val ANEL = "Anel"
    const val PULSEIRA = "Pulseira"
    const val CONJUNTO = "Conjunto"
    const val TORNOZELEIRA = "Tornozeleira"

    val all = listOf(COLAR, BRINCO, ANEL, PULSEIRA, CONJUNTO, TORNOZELEIRA)
}

object BanhoPecaOptions {
    const val OURO_18K = "Ouro 18k"
    const val RODIO = "Ródio"
    const val PRATA = "Prata"
    const val OUTRO = "Outro"

    val all = listOf(OURO_18K, RODIO, PRATA, OUTRO)
}

object PedraPecaOptions {
    const val ZIRCONIA = "Zircônia"
    const val PEROLA = "Pérola"
    const val CRISTAL = "Cristal"
    const val NENHUMA = "Nenhuma"

    val all = listOf(ZIRCONIA, PEROLA, CRISTAL, NENHUMA)
}

object StatusVideoOptions {
    const val IDEIA = "IDEIA"
    const val ROTEIRIZADO = "ROTEIRIZADO"
    const val GRAVADO = "GRAVADO"
    const val EDITADO = "EDITADO"
    const val POSTADO = "POSTADO"

    val all = listOf(IDEIA, ROTEIRIZADO, GRAVADO, EDITADO, POSTADO)
}

@Entity(tableName = "pecas")
data class PecaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nome: String,
    val tipo: String, // Colar, Brinco, Anel, Pulseira, Conjunto, Tornozeleira
    val banho: String, // Ouro 18k, Ródio, Prata, Outro
    val milesimos: Int = 10,
    val pedra: String = PedraPecaOptions.ZIRCONIA, // Zircônia, Pérola, Cristal, Nenhuma
    val preco: Double,
    val precoPromocional: Double? = null,
    val codigoSku: String = "",
    val linkTikTokShop: String? = null,
    val fotos: List<String> = emptyList(), // URIs do MediaStore
    val estoque: Int = 1,
    val observacoes: String = "",
    val criadoEm: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = PecaEntity::class,
            parentColumns = ["id"],
            childColumns = ["pecaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("pecaId")
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pecaId: Long,
    val uri: String,
    val duracaoSeg: Int = 0,
    val gancho: String = "",
    val roteiroJson: String = "",
    val legenda: String = "",
    val hashtags: String = "",
    val status: String = StatusVideoOptions.GRAVADO, // IDEIA, ROTEIRIZADO, GRAVADO, EDITADO, POSTADO
    val dataAgendada: Long? = null,
    val dataPostagem: Long? = null,
    // Métricas manuais
    val views: Long = 0L,
    val curtidas: Long = 0L,
    val comentarios: Long = 0L,
    val compartilhamentos: Long = 0L,
    val salvamentos: Long = 0L,
    val vendas: Long = 0L
)
