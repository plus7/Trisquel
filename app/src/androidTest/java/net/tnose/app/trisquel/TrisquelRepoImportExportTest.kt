package net.tnose.app.trisquel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrisquelRepoImportExportTest {

    private lateinit var database: TrisquelRoomDatabase
    private lateinit var dao: TrisquelDao2
    private lateinit var repo: TrisquelRepo

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TrisquelRoomDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.trisquelDao()
        repo = TrisquelRepo(dao, database) // Inject the memory DB manually
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun exportCameras_createsCorrectJsonArray() = runTest {
        // 1. Arrange: Insert multiple cameras
        dao.upsertCamera(CameraEntity(id = 1, created = "2024-01-01", type = 0, lastModified = "2024-01-01", mount = "F", manufacturer = "Nikon", modelName = "F3", format = 1, ssGrainSize = 1, fastestSs = 2000.0, slowestSs = 8.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10))
        dao.upsertCamera(CameraEntity(id = 2, created = "2024-01-02", type = 0, lastModified = "2024-01-02", mount = "M", manufacturer = "Leica", modelName = "M6", format = 1, ssGrainSize = 1, fastestSs = 1000.0, slowestSs = 1.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10))

        // 2. Act: Call getAllEntriesJSON (export)
        val jsonArray = repo.getAllEntriesJSON("camera")

        // 3. Assert: Verify the exported JSON structure and values
        assertEquals(2, jsonArray.length())
        
        // Find Leica in the array (order is order by _id desc)
        var leicaFound = false
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("manufacturer") == "Leica") {
                assertEquals("M6", obj.getString("model_name"))
                assertEquals("M", obj.getString("mount"))
                leicaFound = true
            }
        }
        assertTrue("Leica M6 should be in the exported JSON", leicaFound)
    }

    @Test
    fun importCamera_mergesJsonCorrectly() = runTest {
        // 1. Arrange: Create a JSON representation of a camera (like from a backup)
        val cameraJson = JSONObject()
            .put("_id", 99) // ID from the old backup
            .put("created", "2023-05-01T10:00:00")
            .put("type", 0)
            .put("last_modified", "2023-05-01T10:00:00")
            .put("mount", "EF")
            .put("manufacturer", "Canon")
            .put("model_name", "EOS-1V")
            .put("format", 1)
            .put("ss_grain_size", 1)
            .put("fastest_ss", 8000.0)
            .put("slowest_ss", 30.0)
            .put("bulb_available", 1)
            .put("shutter_speeds", "")
            .put("ev_grain_size", 1)
            .put("ev_width", 10)

        // 2. Act: Call mergeCameraJSON
        val resultPair = repo.mergeCameraJSON(cameraJson)
        val originalId = resultPair.first
        val newId = resultPair.second

        // 3. Assert: 
        assertEquals("Original ID should match", 99, originalId)
        assertTrue("New ID should be a valid inserted ID (greater than 0)", newId > 0)

        // Verify it was actually inserted into the DB
        val loadedCamera = dao.getCamera(newId)
        assertNotNull(loadedCamera)
        assertEquals("Canon", loadedCamera?.manufacturer)
        assertEquals("EOS-1V", loadedCamera?.modelName)
        assertEquals(8000.0, loadedCamera?.fastestSs)
    }

    @Test
    fun importLens_mapsCameraBodyIdCorrectly() = runTest {
        // 1. Arrange: Insert a new camera and get its actual new ID
        val camera = CameraEntity(id = 0, created = "", type = 0, lastModified = "", mount = "M", manufacturer = "Leica", modelName = "M3", format = 1, ssGrainSize = 1, fastestSs = 1000.0, slowestSs = 1.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10)
        val newCameraId = dao.upsertCamera(camera).toInt()

        // Create a mapping from old backup camera ID (e.g., 5) to the newly inserted ID
        val idMapping = mapOf(5 to newCameraId)

        // Create Lens JSON from backup that references the OLD camera body ID (5)
        val lensJson = JSONObject()
            .put("_id", 10)
            .put("created", "2023-01-01")
            .put("last_modified", "2023-01-01")
            .put("mount", "M")
            .put("body", 5) // Old ID!
            .put("manufacturer", "Leica")
            .put("model_name", "Summicron 50mm")
            .put("focal_length", "50")
            .put("f_steps", "")

        // 2. Act: Merge lens JSON providing the camera mapping
        val resultPair = repo.mergeLensJSON(lensJson, idMapping)
        val newLensId = resultPair.second

        // 3. Assert: The new lens should be linked to the NEW camera ID
        val loadedLens = dao.getLens(newLensId)
        assertNotNull(loadedLens)
        assertEquals("Summicron 50mm", loadedLens?.modelName)
        assertEquals("Lens should be linked to the new mapped Camera ID", newCameraId, loadedLens?.body)
    }

    @Test
    fun importPhoto_mapsMultipleIdsCorrectly() = runTest {
        // This is the ultimate test of the ID mapping logic used during import
        
        // 1. Arrange: Create required references with their new IDs
        val newFilmId = dao.upsertFilmRoll(FilmRollEntity(id = 1, created = "", name = "Test Roll", lastModified = "", camera = null, format = "", manufacturer = "", brand = "", iso = "")).run { 1 } // UPSERT does not return ID for this entity in the DAO
        val newCameraId = dao.upsertCamera(CameraEntity(id = 0, created = "", type = 0, lastModified = "", mount = "", manufacturer = "", modelName = "C1", format = 1, ssGrainSize = 1, fastestSs = 1.0, slowestSs = 1.0, bulbAvailable = 1, shutterSpeeds = "", evGrainSize = 1, evWidth = 10)).toInt()
        val newLensId = dao.upsertLens(LensEntity(id = 0, created = "", lastModified = "", mount = "", body = null, manufacturer = "", modelName = "L1", focalLength = "", fSteps = "")).toInt()
        
        // Maps: Old Backup ID -> New DB ID
        val filmMap = mapOf(100 to newFilmId)
        val cameraMap = mapOf(200 to newCameraId)
        val lensMap = mapOf(300 to newLensId)
        val accMap = emptyMap<Int, Int>() // No accessories for this test

        // Create Photo JSON from backup using OLD IDs
        val photoJson = JSONObject()
            .put("_id", 999)
            .put("filmroll", 100)
            .put("_index", 1)
            .put("date", "2024-01-01T12:00:00")
            .put("camera", 200)
            .put("lens", 300)
            .put("focal_length", 50.0)
            .put("aperture", 2.8)
            .put("shutter_speed", 125.0)
            .put("exp_compensation", 0.0)
            .put("ttl_light_meter", 0.0)
            .put("location", "Tokyo")
            .put("latitude", 35.6895)
            .put("longitude", 139.6917)
            .put("memo", "Nice view")
            .put("accessories", "")
            .put("suppimgs", "")
            .put("favorite", 1)

        // 2. Act: Import Photo with the maps
        val emptySuppImgList = arrayListOf<String>()
        val resultPair = repo.mergePhotoJSON(photoJson, emptySuppImgList, "files_dir", cameraMap, lensMap, filmMap, accMap)
        val newPhotoId = resultPair.second

        // 3. Assert: Photo imported successfully and all old IDs mapped to the newly inserted ones
        val loadedPhoto = dao.getPhoto(newPhotoId)
        assertNotNull(loadedPhoto)
        assertEquals("Tokyo", loadedPhoto?.location)
        assertEquals("Film ID must be mapped", newFilmId, loadedPhoto?.filmroll)
        assertEquals("Camera ID must be mapped", newCameraId, loadedPhoto?.camera)
        assertEquals("Lens ID must be mapped", newLensId, loadedPhoto?.lens)
    }
}
