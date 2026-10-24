package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PecaEntity::class, VideoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pecaDao(): PecaDao
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glowstudio_semijoias.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialPecas(database.pecaDao())
                    }
                }
            }
        }

        private suspend fun populateInitialPecas(dao: PecaDao) {
            val samplePecas = listOf(
                PecaEntity(
                    nome = "Colar Gravatinha Ponto de Luz",
                    tipo = TipoPecaOptions.COLAR,
                    banho = BanhoPecaOptions.OURO_18K,
                    milesimos = 10,
                    pedra = PedraPecaOptions.ZIRCONIA,
                    preco = 89.90,
                    precoPromocional = 69.90,
                    codigoSku = "COL-GRAV-01",
                    linkTikTokShop = "https://shop.tiktok.com/sample/colargrav",
                    fotos = listOf("https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=600&auto=format&fit=crop&q=80"),
                    estoque = 12,
                    observacoes = "Brilho intenso em luz lateral. Ótimo para gravar com luva preta ou fundo veludo."
                ),
                PecaEntity(
                    nome = "Argola Cravejada Zircônias",
                    tipo = TipoPecaOptions.BRINCO,
                    banho = BanhoPecaOptions.OURO_18K,
                    milesimos = 10,
                    pedra = PedraPecaOptions.ZIRCONIA,
                    preco = 69.90,
                    precoPromocional = null,
                    codigoSku = "BRI-ARG-02",
                    linkTikTokShop = "https://shop.tiktok.com/sample/argola",
                    fotos = listOf("https://images.unsplash.com/photo-1630019852942-f89202989a59?w=600&auto=format&fit=crop&q=80"),
                    estoque = 8,
                    observacoes = "Fecho click com som crocante para ASMR."
                ),
                PecaEntity(
                    nome = "Anel Solitário Cristal Imperial",
                    tipo = TipoPecaOptions.ANEL,
                    banho = BanhoPecaOptions.RODIO,
                    milesimos = 10,
                    pedra = PedraPecaOptions.CRISTAL,
                    preco = 119.90,
                    precoPromocional = 99.90,
                    codigoSku = "ANE-SOL-03",
                    linkTikTokShop = null,
                    fotos = listOf("https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=600&auto=format&fit=crop&q=80"),
                    estoque = 5,
                    observacoes = "Zircônia central de 8mm que reflete arco-íris em macro."
                ),
                PecaEntity(
                    nome = "Pulseira Riviera Tripla",
                    tipo = TipoPecaOptions.PULSEIRA,
                    banho = BanhoPecaOptions.OURO_18K,
                    milesimos = 10,
                    pedra = PedraPecaOptions.ZIRCONIA,
                    preco = 149.90,
                    precoPromocional = 129.90,
                    codigoSku = "PUL-RIV-04",
                    linkTikTokShop = null,
                    fotos = listOf("https://images.unsplash.com/photo-1611591475879-da006628f404?w=600&auto=format&fit=crop&q=80"),
                    estoque = 4,
                    observacoes = "Caimento perfeito no pulso. Mostrar jogo de luz girando a mão."
                )
            )
            samplePecas.forEach { dao.insertPeca(it) }
        }
    }
}
