package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.PecaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var pecaDao: PecaDao
    private lateinit var videoDao: VideoDao
    private lateinit var repository: PecaRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pecaDao = db.pecaDao()
        videoDao = db.videoDao()
        repository = PecaRepository(pecaDao, videoDao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GlowStudio", appName)
    }

    @Test
    fun testCameraRecordingAndReviewWorkflow() {
        val repo = com.example.data.repository.CreatorStudioRepository()
        val viewModel = com.example.ui.viewmodel.StudioViewModel(repo)
        assertEquals(com.example.ui.viewmodel.StudioTab.GRAVAR, viewModel.uiState.value.currentTab)
        assertNull(viewModel.uiState.value.recordedVideoReviewUri)

        val uri = android.net.Uri.parse("content://media/external/video/media/999")

        viewModel.onVideoRecorded(uri)
        assertEquals("content://media/external/video/media/999", viewModel.uiState.value.recordedVideoReviewUri)

        // Test Retake
        viewModel.retakeVideo()
        assertNull(viewModel.uiState.value.recordedVideoReviewUri)
        assertEquals(com.example.ui.viewmodel.StudioTab.GRAVAR, viewModel.uiState.value.currentTab)

        // Test Dismiss Review to proceed with Video
        viewModel.onVideoRecorded(uri)
        viewModel.dismissReview()
        assertNull(viewModel.uiState.value.recordedVideoReviewUri)
    }

    @Test
    fun testRoomPecaDaoOperations() = runTest {
        val peca = PecaEntity(
            nome = "Colar Gravatinha Ouro 18k",
            tipo = TipoPecaOptions.COLAR,
            banho = BanhoPecaOptions.OURO_18K,
            milesimos = 10,
            pedra = PedraPecaOptions.ZIRCONIA,
            preco = 89.90,
            precoPromocional = 79.90,
            codigoSku = "COL-GRAV-01",
            linkTikTokShop = "https://shop.tiktok.com/colar",
            fotos = listOf("content://media/external/images/media/1"),
            estoque = 15,
            observacoes = "Brilho excepcional com iluminação suave"
        )

        // Insert
        val id = pecaDao.insertPeca(peca)
        assertTrue(id > 0)

        // Query all
        val pecas = pecaDao.getAllPecas().first()
        assertEquals(1, pecas.size)
        assertEquals("Colar Gravatinha Ouro 18k", pecas[0].nome)
        assertEquals(89.90, pecas[0].preco, 0.001)
        assertEquals(1, pecas[0].fotos.size)

        // Search
        val searchResult = pecaDao.searchPecas("Gravatinha").first()
        assertEquals(1, searchResult.size)

        val searchNotFound = pecaDao.searchPecas("BrincoInexistente").first()
        assertEquals(0, searchNotFound.size)

        // Update
        val updated = pecas[0].copy(preco = 99.90, estoque = 20)
        pecaDao.updatePeca(updated)
        val loadedUpdated = pecaDao.getPecaById(id).first()
        assertNotNull(loadedUpdated)
        assertEquals(99.90, loadedUpdated!!.preco, 0.001)
        assertEquals(20, loadedUpdated.estoque)

        // Delete
        pecaDao.deletePeca(loadedUpdated)
        val afterDelete = pecaDao.getAllPecas().first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testRoomVideoDaoAndPecaLinkage() = runTest {
        // Create a piece first
        val peca = PecaEntity(
            nome = "Argola Cravejada Zircônia",
            tipo = TipoPecaOptions.BRINCO,
            banho = BanhoPecaOptions.OURO_18K,
            milesimos = 10,
            pedra = PedraPecaOptions.ZIRCONIA,
            preco = 69.90,
            codigoSku = "BRI-01"
        )
        val pecaId = pecaDao.insertPeca(peca)

        // Create a video tied to the piece
        val video = VideoEntity(
            pecaId = pecaId,
            uri = "content://media/external/video/media/100",
            duracaoSeg = 15,
            gancho = "Olha esse brilho em luz natural!",
            roteiroJson = "{}",
            legenda = "Argola perfeita para o dia a dia",
            hashtags = "#semijoias #brinco #tiktokshopbrasil",
            status = StatusVideoOptions.GRAVADO,
            views = 1200,
            curtidas = 340,
            vendas = 18
        )
        val videoId = videoDao.insertVideo(video)
        assertTrue(videoId > 0)

        // Retrieve video for piece
        val videos = videoDao.getVideosForPeca(pecaId).first()
        assertEquals(1, videos.size)
        assertEquals(StatusVideoOptions.GRAVADO, videos[0].status)
        assertEquals(1200L, videos[0].views)
        assertEquals(18L, videos[0].vendas)

        // Cascade delete: deleting the piece should cascade delete associated videos
        pecaDao.deletePecaById(pecaId)
        val videosAfterCascade = videoDao.getVideosForPeca(pecaId).first()
        assertEquals(0, videosAfterCascade.size)
    }

    @Test
    fun testTypeConverterStringList() {
        val converter = Converters()
        val list = listOf("content://photo1", "content://photo2", "https://img.com/3.jpg")
        val json = converter.fromStringList(list)
        val restored = converter.toStringList(json)
        assertEquals(3, restored.size)
        assertEquals("content://photo1", restored[0])
        assertEquals("https://img.com/3.jpg", restored[2])

        // Empty handling
        assertEquals("[]", converter.fromStringList(emptyList()))
        assertEquals(0, converter.toStringList("[]").size)
        assertEquals(0, converter.toStringList(null).size)
    }

    @Test
    fun testVideoDaoMetricsAndScheduleUpdates() = runTest {
        val peca = PecaEntity(
            nome = "Choker Fita 4mm",
            tipo = TipoPecaOptions.COLAR,
            banho = BanhoPecaOptions.OURO_18K,
            milesimos = 10,
            preco = 98.0
        )
        val pecaId = pecaDao.insertPeca(peca)

        val video = VideoEntity(
            pecaId = pecaId,
            uri = "content://test/video",
            duracaoSeg = 30,
            gancho = "O segredo do brilho semijoia",
            roteiroJson = "{}",
            status = StatusVideoOptions.GRAVADO
        )
        val videoId = videoDao.insertVideo(video)

        // Test updating schedule
        val scheduleTime = 1770000000000L
        videoDao.updateVideoSchedule(videoId, scheduleTime)
        var loaded = videoDao.getVideoById(videoId).first()
        assertNotNull(loaded)
        assertEquals(scheduleTime, loaded!!.dataAgendada)

        // Test status update
        videoDao.updateVideoStatus(videoId, StatusVideoOptions.POSTADO)
        loaded = videoDao.getVideoById(videoId).first()
        assertNotNull(loaded)
        assertEquals(StatusVideoOptions.POSTADO, loaded!!.status)

        // Test metrics update
        val postTime = 1770000500000L
        videoDao.updateVideoMetrics(
            id = videoId,
            views = 15400L,
            curtidas = 890L,
            comentarios = 120L,
            compartilhamentos = 45L,
            salvamentos = 210L,
            vendas = 32L,
            dataPostagem = postTime,
            status = StatusVideoOptions.POSTADO
        )
        loaded = videoDao.getVideoById(videoId).first()
        assertNotNull(loaded)
        assertEquals(15400L, loaded!!.views)
        assertEquals(890L, loaded.curtidas)
        assertEquals(120L, loaded.comentarios)
        assertEquals(45L, loaded.compartilhamentos)
        assertEquals(210L, loaded.salvamentos)
        assertEquals(32L, loaded.vendas)
        assertEquals(postTime, loaded.dataPostagem)
        assertEquals(StatusVideoOptions.POSTADO, loaded.status)

        // Test getVideosByStatus
        val postados = videoDao.getVideosByStatus(StatusVideoOptions.POSTADO).first()
        assertEquals(1, postados.size)
        assertEquals(videoId, postados[0].id)
    }
}
