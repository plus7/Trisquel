package net.tnose.app.trisquel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
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
        mTrisquelDao.deleteFilmRoll(FilmRollEntity(id, "", "","",0, "", "", "", ""))
    }

    // 日付の切り替わりタイミングで日付を出すという表示の都合上、
    // 直前のショットのDateとPairにするという変則的なデータとなっている。
    fun getPhotosByFilmRollId(filmRollId: Int): LiveData<List<Pair<String, PhotoEntity>>> {
        return mTrisquelDao.photosByFilmRollId(filmRollId).map{
            it -> it.runningFold(
            Pair("", PhotoEntity(
                0, 0, 0, "", 0, 0,
                0.0, 0.0, 0.0, 0.0,
                0.0, "", 0.0, 0.0,
                "", "", "", 0))) {
                acc, value -> Pair(acc.second.date, value)
            }.drop(1)
        }.asLiveData()
    }

    @WorkerThread
    suspend fun upsertPhoto(entity: PhotoEntity) {
        mTrisquelDao.upsertPhoto(entity)
    }

    @WorkerThread
    suspend fun deletePhoto(id: Int) {
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
}