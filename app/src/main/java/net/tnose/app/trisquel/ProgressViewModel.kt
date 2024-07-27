package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

class ExportProgressViewModel(application: Application?) : AndroidViewModel(application!!) {
    private val progress = 0.0 // UI data
    private val workManager = WorkManager.getInstance(application!!.applicationContext).also{
        // XXX: WorkInfoはpersistentっぽい挙動のようなのでこうやって消してあげる必要があるっぽい
        // とはいえ警告されている通りたぶん想定された使い方ではない
        it.pruneWork()
    }
    internal val workInfos: LiveData<List<WorkInfo>>

    init {
        workInfos = workManager.getWorkInfosByTagLiveData(Util.WORKER_TAG_EXPORT)
    }

    fun doExport(zipFile : String, mode : Int) {
        // XXX: ここも同様
        workManager.pruneWork()
        val req = ExportWorker.createExportRequest(zipFile, mode)
        workManager.enqueue(req)
    }

    internal fun cancelExport() {
        workManager.cancelAllWorkByTag(Util.WORKER_TAG_EXPORT)
    }

}
