package net.tnose.app.trisquel

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrisquelRepo @Inject constructor(
    private val mTrisquelDao: TrisquelDao2,
    private val db: TrisquelRoomDatabase
) {
    constructor(application: Application) : this(
        TrisquelRoomDatabase.getInstance(application.applicationContext).trisquelDao(),
        TrisquelRoomDatabase.getInstance(application.applicationContext)
    )

    /* Camera */
    fun getAllCamerasFlow(): Flow<List<CameraEntity>> = mTrisquelDao.allCamerasFlow()

    suspend fun getAllCamerasRaw(): List<CameraEntity> = mTrisquelDao.allCamerasRaw()

    suspend fun getCamera(id: Int): CameraEntity? = mTrisquelDao.getCamera(id)

    suspend fun isCameraUsed(id: Int): Boolean = mTrisquelDao.isCameraUsed(id)

    @WorkerThread
    suspend fun upsertCamera(entity: CameraEntity): Long = mTrisquelDao.upsertCamera(entity)

    @WorkerThread
    suspend fun deleteCamera(id: Int) {
        mTrisquelDao.deleteCamera(CameraEntity(id, "", 0, "", "", "", "", 0, 0, 0.0, 0.0, 0, "", 0, 0))
    }

    /* Lens */
    fun getAllLensesFlow(): Flow<List<LensEntity>> = mTrisquelDao.allLensesFlow()

    suspend fun getAllLensesRaw(): List<LensEntity> = mTrisquelDao.allLensesRaw()

    suspend fun getLens(id: Int): LensEntity? = mTrisquelDao.getLens(id)

    suspend fun getLensByFixedBody(bodyId: Int): LensEntity? = mTrisquelDao.getLensByFixedBody(bodyId)

    suspend fun getLensesByMount(mount: String): List<LensEntity> = mTrisquelDao.getLensesByMount(mount)

    suspend fun isLensUsed(id: Int): Boolean = mTrisquelDao.isLensUsed(id)

    suspend fun getAvailableMountList(): List<String> = mTrisquelDao.getAvailableMountList()

    @WorkerThread
    suspend fun upsertLens(entity: LensEntity): Long = mTrisquelDao.upsertLens(entity)

    @WorkerThread
    suspend fun deleteLens(id: Int) {
        mTrisquelDao.deleteLens(LensEntity(id, "", "", "", 0, "", "", "", ""))
    }

    /* Film roll */
    fun getFilmRollFlow(id : Int) : Flow<FilmRollEntity> {
        return mTrisquelDao.getFilmRollFlow(id)
    }

    suspend fun getFilmRollRaw(id: Int): FilmRollEntity? = mTrisquelDao.getFilmRollRaw(id)

    fun getFilmRollAndRelsFlow(id : Int) : Flow<FilmRollAndRels> {
        return mTrisquelDao.getFilmRollAndRelsFlow(id)
    }

    fun getAllFilmRollsFlow(sortBy : Int, filterByKind : Int, filterByValue : String): Flow<List<FilmRollAndRels>> {
        var cameraVal = "%"
        var filmBrandVal = "%"

        if (filterByKind == 1) {
            cameraVal = filterByValue
        }else if (filterByKind == 2){
            filmBrandVal = filterByValue
        }

        return when(sortBy) {
            1 -> {
                if (filterByKind == 0) {
                    mTrisquelDao.allFilmRollAndRelsFlow().map {
                            it -> it.sortedBy { it.filmRoll.name }
                    }
                } else {
                    mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { it.filmRoll.name }
                    }
                }
            }

            2 -> {
                if (filterByKind == 0) {
                    mTrisquelDao.allFilmRollAndRelsFlow().map {
                        it -> it.sortedBy { it.camera?.let { c -> "${c.manufacturer} ${c.modelName}" } ?: "" }
                    }
                } else {
                    mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { it.camera?.let { c -> "${c.manufacturer} ${c.modelName}" } ?: "" }
                    }
                }
            }

            3 -> {
                if (filterByKind == 0) {
                    mTrisquelDao.allFilmRollAndRelsFlow().map {
                            it -> it.sortedBy { "${it.filmRoll.manufacturer} ${it.filmRoll.brand}" }
                    }
                } else {
                    mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { "${it.filmRoll.manufacturer} ${it.filmRoll.brand}" }
                    }
                }
            }

            else -> {
                if (filterByKind == 0) {
                    mTrisquelDao.allFilmRollAndRelsFlow()
                } else {
                    mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal)
                }
            }
        }
    }

    suspend fun getAvailableFilmBrandList(): List<FilmBrand> = mTrisquelDao.getAvailableFilmBrandList()

    @WorkerThread
    suspend fun upsertFilmRoll(entity: FilmRollEntity) {
        mTrisquelDao.upsertFilmRoll(entity)
    }

    @WorkerThread
    suspend fun deleteFilmRoll(id: Int) {
        val photos = mTrisquelDao.photosByFilmRollIdRaw(id)
        for (p in photos) {
            deletePhoto(p.id)
        }
        mTrisquelDao.deleteFilmRoll(FilmRollEntity(id, "", "","",0, "", "", "", ""))
    }

    /* Photo */

    fun getPhotosByFilmRollIdFlow(filmRollId: Int): Flow<List<Pair<String, PhotoAndTagIds>>> {
        return mTrisquelDao.photosByFilmRollId(filmRollId).map{
            it.runningFold(
                Pair("", PhotoAndTagIds(PhotoEntity(
                    0, 0, 0, "", 0, 0,
                    0.0, 0.0, 0.0, 0.0,
                    0.0, "", 0.0, 0.0,
                    "", "", "", 0), listOf())
                )) {
                    acc, value -> Pair(acc.second.photo.date, value)
            }.drop(1)
        }
    }

    suspend fun getPhotosByFilmRollIdRaw(filmRollId: Int): List<PhotoEntity> = mTrisquelDao.photosByFilmRollIdRaw(filmRollId)

    suspend fun getPhoto(id: Int): PhotoEntity? = mTrisquelDao.getPhoto(id)

    suspend fun getAllFavedPhotosRaw(): List<PhotoEntity> = mTrisquelDao.getAllFavedPhotosRaw()

    suspend fun getPhotos4Conversion(): List<PhotoEntity> = mTrisquelDao.getPhotos4Conversion()

    @WorkerThread
    suspend fun upsertPhoto(entity: PhotoEntity) : Long {
        return mTrisquelDao.upsertPhoto(entity)
    }

    @WorkerThread
    suspend fun deletePhoto(id: Int) {
        val tagmaps = getTagMapAndTagsByPhoto(id)
        for(tm in tagmaps) {
            deleteTagMap(tm.tagMap.id)
        }
        for(tm in tagmaps){
            if(tm.tag!!.refcnt == 1){
                deleteTag(tm.tag.id)
            }else{
                upsertTag(TagEntity(tm.tag.id, tm.tag.label, tm.tag.refcnt!! - 1))
            }
        }
        mTrisquelDao.deletePhoto(
            PhotoEntity(id, 0, 0, "",0,0,
                0.0, 0.0,0.0,0.0,
                0.0, "", 0.0, 0.0,"","", "",0
                )
        )
    }

    /* Accessory */

    fun getAllAccessoriesFlow(sortBy : Int): Flow<List<AccessoryEntity>> {
        return when(sortBy) {
            0 -> mTrisquelDao.allAccessoriesFlow()
            1 -> mTrisquelDao.allAccessoriesSortByNameFlow()
            2 -> mTrisquelDao.allAccessoriesSortByTypeFlow()
            else -> mTrisquelDao.allAccessoriesFlow()
        }
    }

    suspend fun getAllAccessoriesRaw(): List<AccessoryEntity> = mTrisquelDao.allAccessoriesRaw()

    suspend fun getAccessory(id: Int): AccessoryEntity? = mTrisquelDao.getAccessory(id)

    suspend fun isAccessoryUsed(id: Int): Boolean = mTrisquelDao.isAccessoryUsed(id)

    @WorkerThread
    suspend fun upsertAccessory(entity: AccessoryEntity) {
        mTrisquelDao.upsertAccessory(entity)
    }

    @WorkerThread
    suspend fun deleteAccessory(id: Int) {
        mTrisquelDao.deleteAccessory(AccessoryEntity(id, "", "",0,"", "", 0.0))
    }

    /* Tag */
    fun getTagFlow(id : Int) : Flow<TagEntity> {
        return mTrisquelDao.getTagFlow(id)
    }

    suspend fun getAllTagsRaw(): List<TagEntity> = mTrisquelDao.allTagsRaw()
    fun getAllTagsFlow(): Flow<List<TagEntity>> = mTrisquelDao.allTagsFlow()

    suspend fun getTagByLabel(label : String) : TagEntity? {
        return mTrisquelDao.getTagByLabel(label)
    }
    @WorkerThread
    suspend fun tagPhoto(photoId: Int, filmRollId: Int, tags: ArrayList<String>) {
        val currentTags = getTagMapAndTagsByPhoto(photoId)
        val createList = ArrayList<String>()
        val removeList = currentTags.toMutableList()
        for(label in tags) {
            val existingTag = currentTags.find { it.tag!!.label == label }
            if (existingTag == null){
                createList.add(label)
            }else{
                removeList.remove(existingTag)
            }
        }
        //作成またはrefcntをインクリメント
        for(label in createList){
            val t = getTagByLabel(label)
            if(t == null) {
                val tagId = mTrisquelDao.upsertTag(TagEntity(0, label, 1))
                mTrisquelDao.upsertTagMap(TagMapEntity(0, photoId, tagId.toInt(), filmRollId))
            }else{
                mTrisquelDao.upsertTag(TagEntity(t.id, label, t.refcnt!! + 1))
                mTrisquelDao.upsertTagMap(TagMapEntity(0, photoId, t.id, filmRollId))
            }
        }
        //currentTagsとcurrentTagMapsに残ったものはrefcntをデクリメントもしくは削除の対象
        for(t in removeList){
            deleteTagMap(t.tagMap.tagId!!)
            if(t.tag!!.refcnt == 1){ // 削除対象
                deleteTag(t.tag.id)
            }else{ // デクリメントだけ
                upsertTag(TagEntity(t.tag.id, t.tag.label, t.tag.refcnt!! - 1))
            }
        }
    }
    @WorkerThread
    suspend fun upsertTag(entity: TagEntity) : Long {
        return mTrisquelDao.upsertTag(entity)
    }

    @WorkerThread
    suspend fun deleteTag(id: Int) {
        mTrisquelDao.deleteTag(TagEntity(id, "", 0))
    }

    fun getTagMapsByPhoto(photoId : Int) : Flow<List<TagMapEntity>> {
        return mTrisquelDao.getTagMapsByPhoto(photoId)
    }
    @WorkerThread
    suspend fun getTagMapAndTagsByPhoto(photoId : Int) : List<TagMapAndTag> {
        return mTrisquelDao.getTagMapAndTagsByPhoto(photoId)
    }
    fun getTagMapAndTagsByPhotoFlow(photoId : Int) : Flow<List<TagMapAndTag>> {
        return mTrisquelDao.getTagMapAndTagsByPhotoFlow(photoId)
    }

    @WorkerThread
    suspend fun upsertTagMap(entity: TagMapEntity) : Long {
        return mTrisquelDao.upsertTagMap(entity)
    }

    @WorkerThread
    suspend fun deleteTagMap(id: Int) {
        mTrisquelDao.deleteTagMap(TagMapEntity(id, 0, 0,0))
    }

    /* Search */
    fun getPhotosByAndQuery(tags: List<String>): Flow<List<Pair<Pair<String, Int>, PhotoAndRels>>> {
        return mTrisquelDao.getPhotosByAndQuery(tags, tags.count()).map {
                it -> it.sortedByDescending { it.filmRollDate }
        }.map{
            it.runningFold(
            Pair(Pair("",0),
                PhotoAndRels(PhotoEntity(
                            0, 0, 0, "", 0, 0,
                    0.0, 0.0, 0.0, 0.0,
                    0.0, "", 0.0, 0.0,
                        "", "", "", 0),
                    "", "", listOf()
                ))) {
                acc, value -> Pair(Pair(acc.second.photo.date, acc.second.photo.filmroll!!), value)
            }.drop(1)
        }
    }

    /* Metadata */
    suspend fun getMetadata(): TrisquelMetadataEntity? = mTrisquelDao.getMetadata()

    @WorkerThread
    suspend fun setConversionState(value: Int) {
        val metadata = mTrisquelDao.getMetadata()
        if (metadata != null) {
            mTrisquelDao.upsertMetadata(metadata.copy(pathConvDone = value))
        } else {
            mTrisquelDao.upsertMetadata(TrisquelMetadataEntity(0, value))
        }
    }

    /* Legacy JSON Support for Workers */
    fun getAllEntriesJSON(type: String): JSONArray {
        val cursor = db.query("select * from $type order by _id desc;", null)
        val ja = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                val obj = JSONObject()
                for (i in 0 until it.columnCount) {
                    val cname = it.getColumnName(i)
                    when (it.getType(i)) {
                        // もともとFIELD_TYPE_NULLの場合は第二引数にnullを指定していたが、
                        // Geminiによる書き換えを行った当初、JSONObject.NULLに変わっていた。
                        // これは下記仕様により挙動の変化をもたらし、インポート時に悪影響があったので、もとにもどしてある。
                        // https://developer.android.com/reference/org/json/JSONObject
                        // "In particular, calling put(name, null) removes the named entry
                        // from the object but put(name, JSONObject.NULL) stores an entry
                        // whose value is JSONObject.NULL. "
                        Cursor.FIELD_TYPE_NULL -> obj.put(cname, null)
                        Cursor.FIELD_TYPE_FLOAT -> obj.put(cname, it.getDouble(i))
                        Cursor.FIELD_TYPE_INTEGER -> obj.put(cname, it.getInt(i))
                        Cursor.FIELD_TYPE_STRING -> obj.put(cname, it.getString(i))
                        Cursor.FIELD_TYPE_BLOB -> { /* BLOB not expected in this app */ }
                    }
                }
                ja.put(obj)
            }
        }
        return ja
    }

    suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return db.withTransaction {
            block()
        }
    }

    @WorkerThread
    fun deleteAll() {
        val sdb = db.openHelper.writableDatabase
        for(table in listOf("camera", "lens", "filmroll", "photo", "accessory", "tag", "tagmap")) {
            sdb.delete(table, null, null)
            val cval = ContentValues()
            cval.put("seq", 0)
            //reset autoincrement
            sdb.update("sqlite_sequence", SQLiteDatabase.CONFLICT_ABORT, cval, "name = ?", arrayOf(table))
        }
    }

    @WorkerThread
    fun mergeCameraJSON(obj: JSONObject): Pair<Int, Int> {
        val oldId = obj.getInt("_id")
        val cval = ContentValues()
        obj.keys().forEach { key ->
            if (key != "_id") {
                when (val value = obj.get(key)) {
                    is Int -> cval.put(key, value)
                    is Double -> cval.put(key, value)
                    is String -> cval.put(key, value)
                    is Long -> cval.put(key, value)
                }
            }
        }
        val newId = db.openHelper.writableDatabase.insert("camera", SQLiteDatabase.CONFLICT_ABORT, cval)
        return Pair(oldId, newId.toInt())
    }

    @WorkerThread
    fun mergeLensJSON(obj: JSONObject, cameraOld2NewId: Map<Int, Int>): Pair<Int, Int> {
        val oldId = obj.getInt("_id")
        val cval = ContentValues()
        obj.keys().forEach { key ->
            if (key != "_id") {
                if (key == "body") cval.put("body", cameraOld2NewId[obj.getInt(key)])
                else {
                    when (val value = obj.get(key)) {
                        is Int -> cval.put(key, value)
                        is Double -> cval.put(key, value)
                        is String -> cval.put(key, value)
                        is Long -> cval.put(key, value)
                    }
                }
            }
        }
        val newId = db.openHelper.writableDatabase.insert("lens", SQLiteDatabase.CONFLICT_ABORT, cval)
        return Pair(oldId, newId.toInt())
    }

    @WorkerThread
    fun mergeAccessoryJSON(obj: JSONObject): Pair<Int, Int> {
        val oldId = obj.getInt("_id")
        val cval = ContentValues()
        obj.keys().forEach { key ->
            if (key != "_id") {
                when (val value = obj.get(key)) {
                    is Int -> cval.put(key, value)
                    is Double -> cval.put(key, value)
                    is String -> cval.put(key, value)
                    is Long -> cval.put(key, value)
                }
            }
        }
        val newId = db.openHelper.writableDatabase.insert("accessory", SQLiteDatabase.CONFLICT_ABORT, cval)
        return Pair(oldId, newId.toInt())
    }

    @WorkerThread
    fun mergeFilmRollJSON(obj: JSONObject, cameraOld2NewId: Map<Int, Int>): Pair<Int, Int> {
        val oldId = obj.getInt("_id")
        val cval = ContentValues()
        obj.keys().forEach { key ->
            if (key != "_id") {
                if (key == "camera") cval.put("camera", cameraOld2NewId[obj.getInt(key)])
                else {
                    when (val value = obj.get(key)) {
                        is Int -> cval.put(key, value)
                        is Double -> cval.put(key, value)
                        is String -> cval.put(key, value)
                        is Long -> cval.put(key, value)
                    }
                }
            }
        }
        val newId = db.openHelper.writableDatabase.insert("filmroll", SQLiteDatabase.CONFLICT_ABORT, cval)
        return Pair(oldId, newId.toInt())
    }

    @WorkerThread
    fun mergePhotoJSON(obj: JSONObject,
                       newpaths: ArrayList<String>,
                       importErrorStr: String,
                       cameraOld2NewId: Map<Int, Int>,
                       lensOld2NewId: Map<Int, Int>,
                       filmrollOld2NewId: Map<Int, Int>,
                       accessoryOld2NewId: Map<Int, Int>): Pair<Int, Int> {
        val oldId = obj.getInt("_id")
        val cval = ContentValues()
        obj.keys().forEach { key ->
            if (key != "_id") {
                when (key) {
                    "camera" -> cval.put(key, cameraOld2NewId[obj.getInt(key)])
                    "lens" -> cval.put(key, lensOld2NewId[obj.getInt(key)])
                    "filmroll" -> cval.put(key, filmrollOld2NewId[obj.getInt(key)])
                    "accessories" -> { //アクセサリは少し特殊。横着してLIKEで検索できるようにしたかったため。
                        val accessories = obj.getString(key)
                            .split(Photo.splitter)
                            .filter { it.isNotEmpty() }
                            .map { accessoryOld2NewId[it.toInt()] }
                            .joinToString("/", "/", "/")
                        cval.put(key, accessories)
                    }
                    // newpathsで置換
                    "suppimgs" -> cval.put(key, JSONArray(newpaths).toString())
                    "memo" -> {
                        val value = obj.getString(key)
                        val appendStr = if (importErrorStr.isNotEmpty()) {
                            (if (value.isEmpty()) "" else "\n") + importErrorStr
                        } else ""
                        cval.put(key, value + appendStr)
                    }
                    else -> {
                        if (!key.startsWith("suppimgs_")) {
                            when (val value = obj.get(key)) {
                                is Int -> cval.put(key, value)
                                is Double -> cval.put(key, value)
                                is String -> cval.put(key, value)
                                is Long -> cval.put(key, value)
                            }
                        }
                    }
                }
            }
        }
        val newId = db.openHelper.writableDatabase.insert("photo", SQLiteDatabase.CONFLICT_ABORT, cval)
        return Pair(oldId, newId.toInt())
    }

    //tagはそれなりに特殊
    @WorkerThread
    suspend fun mergeTagMapJSON(tagmaps: JSONArray, tags: JSONArray,
                        filmrollOld2NewId: Map<Int, Int>, photoOld2NewId: Map<Int, Int>) {
        val tagOldId2Label = HashMap<Int, String>()
        for (i in 0 until tags.length()) {
            val tag = tags.getJSONObject(i)
            tagOldId2Label[tag.getInt("_id")] = tag.getString("label")
        }

        val tagmapsArray = ArrayList<Triple<Int, Int, String>>()
        for (i in 0 until tagmaps.length()) {
            val tagmap = tagmaps.getJSONObject(i)
            tagmapsArray.add(Triple(
                photoOld2NewId[tagmap.getInt("photo_id")] ?: -1,
                filmrollOld2NewId[tagmap.getInt("filmroll_id")] ?: -1,
                tagOldId2Label[tagmap.getInt("tag_id")] ?: ""
            ))
        }

        for (group in tagmapsArray.groupBy { it.first }) { // group by photo_id
            if (group.key == -1) continue
            val filmrollId = group.value[0].second  // filmroll_id must be same
            if (filmrollId == -1) continue
            
            val tagLabels = ArrayList(group.value.map { it.third })

            tagPhoto(group.key, filmrollId, tagLabels)

        }
    }
}
