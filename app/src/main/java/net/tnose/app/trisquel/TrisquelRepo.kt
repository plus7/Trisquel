package net.tnose.app.trisquel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrisquelRepo {
    protected lateinit var mTrisquelDao: TrisquelDao2
    constructor (application: Application?) {
        val db : TrisquelRoomDatabase = TrisquelRoomDatabase.getInstance(application!!.applicationContext)
        mTrisquelDao = db.trisquelDao()
    }

    fun getFilmRoll(id : Int) : LiveData<FilmRollEntity> {
        return mTrisquelDao.getFilmRoll(id)
    }

    fun getAllFilmRolls(sortBy : Int, filterByKind : Int, filterByValue : String): LiveData<List<FilmRollAndRels>> {
        var cameraVal = "%"
        var filmBrandVal = "%"

        if (filterByKind == 1) {
            cameraVal = filterByValue
        }else if (filterByKind == 2){
            filmBrandVal = filterByValue
        }

        when(sortBy) {
            0 -> {
                if (filterByKind == 0) {
                    return mTrisquelDao.allFilmRollAndRels()
                } else {
                    return mTrisquelDao.allFilmRollAndRelsWithFilter(cameraVal, filmBrandVal)
                }
            }

            1 -> {
                if (filterByKind == 0) {
                    return mTrisquelDao.allFilmRollAndRelsFlow().map {
                            it -> it.sortedBy { it.filmRoll.name }
                    }.asLiveData()
                } else {
                    return mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { it.filmRoll.name }
                    }.asLiveData()
                }
            }

            2 -> {
                if (filterByKind == 0) {
                    return mTrisquelDao.allFilmRollAndRelsFlow().map {
                        it -> it.sortedBy { it.camera.manufacturer + " " + it.camera.modelName }
                    }.asLiveData()
                } else {
                    return mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { it.camera.manufacturer + " " + it.camera.modelName }
                    }.asLiveData()
                }
            }

            3 -> {
                if (filterByKind == 0) {
                    return mTrisquelDao.allFilmRollAndRelsFlow().map {
                            it -> it.sortedBy { it.filmRoll.manufacturer + " " + it.filmRoll.brand }
                    }.asLiveData()
                } else {
                    return mTrisquelDao.allFilmRollAndRelsWithFilterFlow(cameraVal, filmBrandVal).map {
                            it -> it.sortedBy { it.filmRoll.manufacturer + " " + it.filmRoll.brand }
                    }.asLiveData()
                }
            }

            else -> {
                if (filterByKind == 0) {
                    return mTrisquelDao.allFilmRollAndRels()
                } else {
                    return mTrisquelDao.allFilmRollAndRelsWithFilter(cameraVal, filmBrandVal)
                }
            }
        }
    }
    @WorkerThread
    suspend fun upsertFilmRoll(entity: FilmRollEntity) {
        mTrisquelDao.upsertFilmRoll(entity)
    }

    @WorkerThread
    suspend fun deleteFilmRoll(id: Int) {
        val photos = mTrisquelDao.photosByFilmRollIdRaw(id)
        //val photos = getPhotosByFilmRollId(id).value // これだと読み込みが終わる間がないためnullが帰ってくる？
        for (p in photos) {
            deletePhoto(p.id) //tagのリファレンスカウントも管理する必要があるため
        }
        mTrisquelDao.deleteFilmRoll(FilmRollEntity(id, "", "","",0, "", "", "", ""))
    }

    // 日付の切り替わりタイミングで日付を出すという表示の都合上、
    // 直前のショットのDateとPairにするという変則的なデータとなっている。
    fun getPhotosByFilmRollId(filmRollId: Int): LiveData<List<Pair<String, PhotoAndTagIds>>> {
        return mTrisquelDao.photosByFilmRollId(filmRollId).map{
            it -> it.runningFold(
            Pair("", PhotoAndTagIds(PhotoEntity(
                0, 0, 0, "", 0, 0,
                0.0, 0.0, 0.0, 0.0,
                0.0, "", 0.0, 0.0,
                "", "", "", 0), listOf())
            )) {
                acc, value -> Pair(acc.second.photo.date, value)
            }.drop(1)
        }.asLiveData()
    }

    fun getPhotosByAndQuery(tags: List<String>): LiveData<List<Pair<Pair<String, Int>, PhotoAndRels>>> {
        return mTrisquelDao.getPhotosByAndQuery(tags, tags.count()).map{
            it -> it.runningFold(
            Pair(Pair("",0),
                PhotoAndRels(PhotoEntity(
                            0, 0, 0, "", 0, 0,
                    0.0, 0.0, 0.0, 0.0,
                    0.0, "", 0.0, 0.0,
                        "", "", "", 0),
                    "", listOf()
                ))) {
                acc, value -> Pair(Pair(acc.second.photo.date, acc.second.photo.filmroll!!), value)
            }.drop(1)
        }.asLiveData()
    }

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
            if(tm.tag.refcnt == 1){
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

    fun getAllAccessories(sortBy : Int): LiveData<List<AccessoryEntity>> {
        when(sortBy) {
            0 -> { return mTrisquelDao.allAccessories() }
            1 -> { return mTrisquelDao.allAccessoriesSortByName() }
            2 -> { return mTrisquelDao.allAccessoriesSortByType() }
            else -> { return mTrisquelDao.allAccessories() }
        }
    }
    @WorkerThread
    suspend fun upsertAccessory(entity: AccessoryEntity) {
        mTrisquelDao.upsertAccessory(entity)
    }

    @WorkerThread
    suspend fun deleteAccessory(id: Int) {
        mTrisquelDao.deleteAccessory(AccessoryEntity(id, "", "",0,"", "", 0.0))
    }

    fun getTag(id : Int) : LiveData<TagEntity> {
        return mTrisquelDao.getTag(id)
    }
    suspend fun getTagByLabel(label : String) : TagEntity? {
        return mTrisquelDao.getTagByLabel(label)
    }
    @WorkerThread
    suspend fun tagPhoto(photoId: Int, filmRollId: Int, tags: ArrayList<String>) {
        val currentTags = getTagMapAndTagsByPhoto(photoId)
        val createList = ArrayList<String>()
        val removeList = currentTags.toMutableList()
        for(label in tags) {
            val existingTag = currentTags.find { it.tag.label == label }
            if (existingTag == null){
                createList.add(label)
            }else{
                removeList.remove(existingTag) //existingTagは現状維持の対象なのでremoveListから外す
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
        //removeList内はrefcntをデクリメントもしくは削除の対象
        for(t in removeList){
            deleteTagMap(t.tagMap.tagId!!)
            if(t.tag.refcnt == 1){ // 削除対象
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
}