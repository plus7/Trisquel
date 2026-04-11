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
        // Arrange
        val camera = CameraEntity(
            id = 0,
            created = "2024-01-01T12:00:00",
            type = 0,
            lastModified = "2024-01-01T12:00:00",
            mount = "F Mount",
            manufacturer = "Nikon",
            modelName = "F3",
            format = 1,
            ssGrainSize = 1,
            fastestSs = 2000.0,
            slowestSs = 8.0,
            bulbAvailable = 1,
            shutterSpeeds = "",
            evGrainSize = 1,
            evWidth = 10
        )
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
        // Arrange
        val camera = CameraEntity(
            id = 0,
            created = "2024-01-01T12:00:00",
            type = 0,
            lastModified = "2024-01-01T12:00:00",
            mount = "K Mount",
            manufacturer = "Pentax",
            modelName = "LX",
            format = 1,
            ssGrainSize = 1,
            fastestSs = 2000.0,
            slowestSs = 1.0,
            bulbAvailable = 1,
            shutterSpeeds = "",
            evGrainSize = 1,
            evWidth = 10
        )
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

        val camera = CameraEntity(
            id = 0,
            created = "2024-01-01T12:00:00",
            type = 0,
            lastModified = "2024-01-01T12:00:00",
            mount = "FD Mount",
            manufacturer = "Canon",
            modelName = "AE-1",
            format = 1,
            ssGrainSize = 1,
            fastestSs = 1000.0,
            slowestSs = 2.0,
            bulbAvailable = 1,
            shutterSpeeds = "",
            evGrainSize = 1,
            evWidth = 10
        )

        // Act: データを挿入
        dao.upsertCamera(camera)

        // Assert: Flowから最新のリストが流れてくることを確認
        cameraList = dao.allCamerasFlow().first()
        assertEquals("リストに1件追加されていること", 1, cameraList.size)
        assertEquals("Canon", cameraList[0].manufacturer)
    }
}
