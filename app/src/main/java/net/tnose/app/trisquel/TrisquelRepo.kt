package net.tnose.app.trisquel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData


class TrisquelRepo {
    protected lateinit var mTrisquelDao: TrisquelDao2
    protected lateinit var mAllAccessory: LiveData<List<AccessoryEntity>>
    constructor (application: Application?) {
        val db : TrisquelRoomDatabase = TrisquelRoomDatabase.getInstance(application!!.applicationContext)
        mTrisquelDao = db.trisquelDao()
        mAllAccessory = mTrisquelDao.allAccessories()
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