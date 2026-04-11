package net.tnose.app.trisquel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrisquelDao2Test {

    private lateinit var database: TrisquelRoomDatabase
    private lateinit var dao: TrisquelDao2

    @Before
    fun setup() {
        // テスト用のコンテキストを取得
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // メモリ上で動作するテスト用データベースを構築（ファイルに保存されないため高速で安全）
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrisquelRoomDatabase::class.java
        ).allowMainThreadQueries().build() // テスト中はメインスレッドでの実行を許可

        dao = database.trisquelDao()
    }

    @After
    fun teardown() {
        // テストごとにデータベースを閉じてクリーンアップ
        database.close()
    }

    // ==========================================
    // CameraEntity Tests
    // ==========================================
    @Test
    fun insertAndGetCamera() = runTest {
        // 1. Arrange (準備): テスト用のCameraEntityを作成
        val camera = CameraEntity(
            id = 0, // 0を指定するとRoomが自動採番(AutoIncrement)してくれる
            created = "2024-01-01T12:00:00",
            type = 0,
            lastModified = "2024-01-01T12:00:00",
            mount = "M Mount",
            manufacturer = "Leica",
            modelName = "M3",
            format = 1,
            ssGrainSize = 1,
            fastestSs = 1000.0,
            slowestSs = 1.0,
            bulbAvailable = 1,
            shutterSpeeds = "",
            evGrainSize = 1,
            evWidth = 10
        )

        // 2. Act (実行): DBに挿入し、その結果（ID）で再取得
        val insertedId = dao.upsertCamera(camera)
        val loaded = dao.getCamera(insertedId.toInt())

        // 3. Assert (検証): 取得したデータが挿入したものと一致するか
        assertNotNull("挿入したカメラが取得できること", loaded)
        assertEquals("メーカー名が一致すること", "Leica", loaded?.manufacturer)
        assertEquals("モデル名が一致すること", "M3", loaded?.modelName)
    }

    @Test
    fun updateCamera() = runTest {
        val camera = CameraEntity(id = 0, created = "2024-01-01T12:00:00", type = 0, lastModified = "2024-01-01T12:00:00", mount = "F Mount", manufacturer = "Nikon", modelName = "F3", format = 1, ssGrainSize = 1, fastestSs = 2000.0, slowestSs = 8.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10)
        val insertedId = dao.upsertCamera(camera)
        val loaded = dao.getCamera(insertedId.toInt())!!

        // Act: 値を変更して再びupsert (idが同じなら更新される)
        val updatedCamera = loaded.copy(modelName = "F3 HP", fastestSs = 4000.0)
        dao.upsertCamera(updatedCamera)

        // Assert
        val reloaded = dao.getCamera(insertedId.toInt())
        assertEquals("モデル名が更新されていること", "F3 HP", reloaded?.modelName)
        assertEquals("シャッタースピードが更新されていること", 4000.0, reloaded?.fastestSs)
    }

    @Test
    fun deleteCamera() = runTest {
        val camera = CameraEntity(id = 0, created = "2024-01-01T12:00:00", type = 0, lastModified = "2024-01-01T12:00:00", mount = "K Mount", manufacturer = "Pentax", modelName = "LX", format = 1, ssGrainSize = 1, fastestSs = 2000.0, slowestSs = 1.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10)
        val insertedId = dao.upsertCamera(camera)
        val loaded = dao.getCamera(insertedId.toInt())!!

        // Act: 削除
        dao.deleteCamera(loaded)

        // Assert: 削除後は取得できないこと
        val afterDelete = dao.getCamera(insertedId.toInt())
        assertNull("削除されたカメラはnullを返すこと", afterDelete)
    }

    @Test
    fun getAllCamerasFlow_emitsUpdates() = runTest {
        // Arrange: 最初は空であることをFlow経由で確認
        var cameraList = dao.allCamerasFlow().first()
        assertTrue("最初はリストが空であること", cameraList.isEmpty())

        val camera = CameraEntity(id = 0, created = "2024-01-01T12:00:00", type = 0, lastModified = "2024-01-01T12:00:00", mount = "FD Mount", manufacturer = "Canon", modelName = "AE-1", format = 1, ssGrainSize = 1, fastestSs = 1000.0, slowestSs = 2.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10)
        dao.upsertCamera(camera)

        cameraList = dao.allCamerasFlow().first()
        assertEquals("リストに1件追加されていること", 1, cameraList.size)
    }

    // ==========================================
    // LensEntity Tests
    // ==========================================
    @Test
    fun insertAndGetLens() = runTest {
        val lens = LensEntity(
            id = 0,
            created = "2024-01-01T12:00:00",
            lastModified = "2024-01-01T12:00:00",
            mount = "M Mount",
            body = null,
            manufacturer = "Leica",
            modelName = "Summicron 50mm f/2",
            focalLength = "50",
            fSteps = "2,2.8,4,5.6,8,11,16"
        )
        val insertedId = dao.upsertLens(lens)
        val loaded = dao.getLens(insertedId.toInt())

        assertNotNull("挿入したレンズが取得できること", loaded)
        assertEquals("メーカー名が一致すること", "Leica", loaded?.manufacturer)
        assertEquals("モデル名が一致すること", "Summicron 50mm f/2", loaded?.modelName)
    }

    @Test
    fun deleteLens() = runTest {
        val lens = LensEntity(id = 0, created = "", lastModified = "", mount = "M Mount", body = null, manufacturer = "Leica", modelName = "Test", focalLength = "50", fSteps = "")
        val insertedId = dao.upsertLens(lens)
        val loaded = dao.getLens(insertedId.toInt())!!

        dao.deleteLens(loaded)
        val afterDelete = dao.getLens(insertedId.toInt())
        assertNull("削除されたレンズはnullを返すこと", afterDelete)
    }

    // ==========================================
    // FilmRollEntity Tests
    // ==========================================
    @Test
    fun insertAndGetFilmRoll() = runTest {
        // upsertFilmRoll は戻り値が Unit なので、IDを明示的に1として指定しテストする
        val film = FilmRollEntity(
            id = 1,
            created = "2024-01-01T12:00:00",
            name = "Tokyo Trip 2024",
            lastModified = "2024-01-01T12:00:00",
            camera = null,
            format = "35mm",
            manufacturer = "Kodak",
            brand = "Portra 400",
            iso = "400"
        )
        dao.upsertFilmRoll(film)
        
        val loaded = dao.getFilmRollRaw(1)
        assertNotNull("挿入したフィルムが取得できること", loaded)
        assertEquals("フィルム名が一致すること", "Tokyo Trip 2024", loaded?.name)
        assertEquals("ブランド名が一致すること", "Portra 400", loaded?.brand)
    }

    @Test
    fun deleteFilmRoll() = runTest {
        val film = FilmRollEntity(id = 2, created = "", name = "Test Film", lastModified = "", camera = null, format = "35mm", manufacturer = "Fujifilm", brand = "Superia 400", iso = "400")
        dao.upsertFilmRoll(film)
        
        dao.deleteFilmRoll(film)
        val afterDelete = dao.getFilmRollRaw(2)
        assertNull("削除されたフィルムはnullを返すこと", afterDelete)
    }

    // ==========================================
    // AccessoryEntity Tests
    // ==========================================
    @Test
    fun insertAndGetAccessory() = runTest {
        // upsertAccessory も Unit 戻り値のため、IDを明示的に指定
        val accessory = AccessoryEntity(
            id = 1,
            created = "2024-01-01T12:00:00",
            lastModified = "2024-01-01T12:00:00",
            type = 1,
            name = "ND8 Filter",
            mount = "52mm",
            focalLengthFactor = 1.0
        )
        dao.upsertAccessory(accessory)
        
        val loaded = dao.getAccessory(1)
        assertNotNull("挿入したアクセサリーが取得できること", loaded)
        assertEquals("アクセサリー名が一致すること", "ND8 Filter", loaded?.name)
    }

    // ==========================================
    // TagEntity Tests
    // ==========================================
    @Test
    fun insertAndGetTag() = runTest {
        val tag = TagEntity(
            id = 0,
            label = "Portrait",
            refcnt = 0
        )
        dao.upsertTag(tag) // upsertTag は Long 戻り値
        
        // タグ名から取得するAPI(getTagByLabel)を用いて検証
        val loaded = dao.getTagByLabel("Portrait")
        assertNotNull("挿入したタグが取得できること", loaded)
        assertEquals("タグ名が一致すること", "Portrait", loaded?.label)
    }
}
