package com.example.data.repository

import com.example.data.local.PecaDao
import com.example.data.local.PecaEntity
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import kotlinx.coroutines.flow.Flow

class PecaRepository(
    private val pecaDao: PecaDao,
    private val videoDao: VideoDao
) {
    val allPecas: Flow<List<PecaEntity>> = pecaDao.getAllPecas()

    fun searchPecas(query: String): Flow<List<PecaEntity>> {
        return if (query.isBlank()) {
            pecaDao.getAllPecas()
        } else {
            pecaDao.searchPecas(query.trim())
        }
    }

    fun getPecasByTipo(tipo: String): Flow<List<PecaEntity>> {
        return pecaDao.getPecasByTipo(tipo)
    }

    fun getPecaById(id: Long): Flow<PecaEntity?> {
        return pecaDao.getPecaById(id)
    }

    suspend fun insertPeca(peca: PecaEntity): Long {
        return pecaDao.insertPeca(peca)
    }

    suspend fun updatePeca(peca: PecaEntity) {
        pecaDao.updatePeca(peca)
    }

    suspend fun deletePeca(peca: PecaEntity) {
        pecaDao.deletePeca(peca)
    }

    suspend fun deletePecaById(id: Long) {
        pecaDao.deletePecaById(id)
    }

    // Video functions
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()

    fun getVideosForPeca(pecaId: Long): Flow<List<VideoEntity>> {
        return videoDao.getVideosForPeca(pecaId)
    }

    suspend fun getVideoByIdSync(id: Long): VideoEntity? {
        return videoDao.getVideoByIdSync(id)
    }

    suspend fun updateCaptionAndHashtags(videoId: Long, legenda: String, hashtags: String) {
        videoDao.updateCaptionAndHashtags(videoId, legenda, hashtags)
    }

    fun getVideosByStatus(status: String): Flow<List<VideoEntity>> {
        return videoDao.getVideosByStatus(status)
    }

    suspend fun updateVideoMetrics(
        id: Long,
        views: Long,
        curtidas: Long,
        comentarios: Long,
        compartilhamentos: Long,
        salvamentos: Long,
        vendas: Long,
        dataPostagem: Long,
        status: String = com.example.data.local.StatusVideoOptions.POSTADO
    ) {
        videoDao.updateVideoMetrics(
            id = id,
            views = views,
            curtidas = curtidas,
            comentarios = comentarios,
            compartilhamentos = compartilhamentos,
            salvamentos = salvamentos,
            vendas = vendas,
            dataPostagem = dataPostagem,
            status = status
        )
    }

    suspend fun updateVideoStatus(id: Long, status: String) {
        videoDao.updateVideoStatus(id, status)
    }

    suspend fun updateVideoSchedule(id: Long, dataAgendada: Long?) {
        videoDao.updateVideoSchedule(id, dataAgendada)
    }

    suspend fun insertVideo(video: VideoEntity): Long {
        return videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun deleteVideo(video: VideoEntity) {
        videoDao.deleteVideo(video)
    }

    suspend fun deleteVideoById(id: Long) {
        videoDao.deleteVideoById(id)
    }
}
