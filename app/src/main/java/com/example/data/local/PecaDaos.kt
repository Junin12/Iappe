package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PecaDao {

    @Query("SELECT * FROM pecas ORDER BY criadoEm DESC")
    fun getAllPecas(): Flow<List<PecaEntity>>

    @Query("SELECT * FROM pecas WHERE tipo = :tipo ORDER BY criadoEm DESC")
    fun getPecasByTipo(tipo: String): Flow<List<PecaEntity>>

    @Query("SELECT * FROM pecas WHERE nome LIKE '%' || :query || '%' OR codigoSku LIKE '%' || :query || '%' ORDER BY criadoEm DESC")
    fun searchPecas(query: String): Flow<List<PecaEntity>>

    @Query("SELECT * FROM pecas WHERE id = :id LIMIT 1")
    fun getPecaById(id: Long): Flow<PecaEntity?>

    @Query("SELECT * FROM pecas WHERE id = :id LIMIT 1")
    suspend fun getPecaByIdSync(id: Long): PecaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeca(peca: PecaEntity): Long

    @Update
    suspend fun updatePeca(peca: PecaEntity)

    @Delete
    suspend fun deletePeca(peca: PecaEntity)

    @Query("DELETE FROM pecas WHERE id = :id")
    suspend fun deletePecaById(id: Long)

    @Query("SELECT COUNT(*) FROM pecas")
    suspend fun getPecaCount(): Int
}

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY id DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE pecaId = :pecaId ORDER BY id DESC")
    fun getVideosForPeca(pecaId: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun getVideoById(id: Long): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoByIdSync(id: Long): VideoEntity?

    @Query("UPDATE videos SET legenda = :legenda, hashtags = :hashtags WHERE id = :id")
    suspend fun updateCaptionAndHashtags(id: Long, legenda: String, hashtags: String)

    @Query("SELECT * FROM videos WHERE status = :status ORDER BY dataPostagem DESC")
    fun getVideosByStatus(status: String): Flow<List<VideoEntity>>

    @Query("UPDATE videos SET views = :views, curtidas = :curtidas, comentarios = :comentarios, compartilhamentos = :compartilhamentos, salvamentos = :salvamentos, vendas = :vendas, dataPostagem = :dataPostagem, status = :status WHERE id = :id")
    suspend fun updateVideoMetrics(
        id: Long,
        views: Long,
        curtidas: Long,
        comentarios: Long,
        compartilhamentos: Long,
        salvamentos: Long,
        vendas: Long,
        dataPostagem: Long,
        status: String
    )

    @Query("UPDATE videos SET status = :status WHERE id = :id")
    suspend fun updateVideoStatus(id: Long, status: String)

    @Query("UPDATE videos SET dataAgendada = :dataAgendada WHERE id = :id")
    suspend fun updateVideoSchedule(id: Long, dataAgendada: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("SELECT COUNT(*) FROM videos WHERE pecaId = :pecaId")
    suspend fun getVideoCountForPeca(pecaId: Long): Int
}
