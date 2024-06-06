package net.tnose.app.trisquel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData


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
                return mTrisquelDao.allFilmRollAndRelsSortByName(cameraVal, filmBrandVal)
            }

            /*2 -> {
                return mTrisquelDao.allAccessoriesSortByType()
            }*/

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