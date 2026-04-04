package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class MainEvent {
    data class RestoreDatabaseResult(val success: Boolean) : MainEvent()
    object RequireDbConvAction : MainEvent()
    object ShowReleaseNotesConfirm : MainEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var activeDialog by mutableStateOf<ActiveDialog?>(null)

    private val _events = MutableSharedFlow<MainEvent>()
    val events = _events.asSharedFlow()

    fun showDialog(dialog: ActiveDialog) {
        activeDialog = dialog
    }

    fun dismissDialog() {
        activeDialog = null
    }

    var currentFilter by mutableStateOf(Pair(0, arrayListOf<String>()))
    var currentSubtitle by mutableStateOf("")

    private val userPreferencesRepository = UserPreferencesRepository(application)

    fun getPinnedFilters(): ArrayList<Pair<Int, ArrayList<String>>> {
        return userPreferencesRepository.getPinnedFilters()
    }

    fun addPinnedFilter(newfilter: Pair<Int, ArrayList<String>>) {
        userPreferencesRepository.addPinnedFilter(newfilter)
    }

    fun removePinnedFilter(filter: Pair<Int, ArrayList<String>>) {
        userPreferencesRepository.removePinnedFilter(filter)
    }

    fun getFilterLabel(f: Pair<Int, ArrayList<String>>): String {
        return when(f.first) {
            1 -> {
                val dao = TrisquelDao(getApplication())
                dao.connection()
                val c = dao.getCamera(f.second[0].toInt())
                dao.close()
                if (c != null) "📷 " + c.manufacturer + " " + c.modelName else ""
            }
            2 -> "🎞 " + f.second.joinToString(" ")
            else -> ""
        }
    }

    private suspend fun getAllCameras(): List<CameraSpec> = withContext(Dispatchers.IO) {
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val list = dao.allCameras
        dao.close()
        list
    }

    private suspend fun getAvailableFilmBrandList(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val list = dao.availableFilmBrandList
        dao.close()
        list
    }

    private suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val list = dao.allTags
        dao.close()
        list
    }

    fun requestFilterByCamera(onSelected: (Int, Int?) -> Unit) = viewModelScope.launch {
        val cs = getAllCameras().toMutableList()
        cs.sortBy { it.manufacturer + " " + it.modelName }
        showDialog(ActiveDialog.Select(
            items = cs.map { it.manufacturer + " " + it.modelName }.toTypedArray(),
            ids = cs.map { it.id },
            onSelected = onSelected
        ))
    }

    fun requestFilterByFilmBrand(onSelected: (Pair<String, String>) -> Unit) = viewModelScope.launch {
        val fbs = getAvailableFilmBrandList()
        showDialog(ActiveDialog.Select(
            items = fbs.map { it.first + " " + it.second }.toTypedArray(),
            onSelected = { which, _ ->
                onSelected(fbs[which])
            }
        ))
    }

    fun requestSearch(title: String, onSearch: (ArrayList<String>) -> Unit) = viewModelScope.launch {
        val tags = getAllTags()
        showDialog(ActiveDialog.SearchCond(
            title = title,
            labels = tags.sortedBy { it.label }.map { it.label }.toTypedArray(),
            onSearch = onSearch
        ))
    }

    private suspend fun isDbConvForAndroid11Done(): Boolean = withContext(Dispatchers.IO) {
        val dao = TrisquelDao(getApplication())
        dao.connection()
        val state = dao.getConversionState()
        dao.close()
        state >= 1
    }

    fun checkAppStartupState(isDbConvActive: Boolean, currentVersion: Int) = viewModelScope.launch {
        val dbConvForAndroid11Done = isDbConvForAndroid11Done()
        if(!dbConvForAndroid11Done && !isDbConvActive) {
            _events.emit(MainEvent.RequireDbConvAction)
            return@launch
        }

        val lastVersion = userPreferencesRepository.getLastVersion()
        if (0 < lastVersion && lastVersion < currentVersion) {
            _events.emit(MainEvent.ShowReleaseNotesConfirm)
        }
        userPreferencesRepository.setLastVersion(currentVersion)
    }

    fun requestRestoreDatabase(uri: android.net.Uri, contentResolver: android.content.ContentResolver) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val dbpath = context.getDatabasePath("trisquel.db")
        
        // Check SQLite format
        val header = byteArrayOf(
            0x53, 0x51, 0x4c, 0x69,
            0x74, 0x65, 0x20, 0x66,
            0x6f, 0x72, 0x6d, 0x61,
            0x74, 0x20, 0x33, 0x00
        )
        try {
            contentResolver.openInputStream(uri)?.use { fis ->
                val buffer = ByteArray(16)
                val readsize = fis.read(buffer)
                if (readsize != 16 || !header.contentEquals(buffer)) {
                    _events.emit(MainEvent.RestoreDatabaseResult(false))
                    return@launch
                }
            } ?: run {
                _events.emit(MainEvent.RestoreDatabaseResult(false))
                return@launch
            }
        } catch (e: Exception) {
            _events.emit(MainEvent.RestoreDatabaseResult(false))
            return@launch
        }

        val calendar = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
        val backupPath = dbpath.absolutePath + "." + sdf.format(calendar.time) + ".bak"

        if (dbpath.exists()) {
            java.io.FileInputStream(dbpath).use { fis ->
                java.io.FileOutputStream(backupPath).use { fos ->
                    fis.copyTo(fos)
                }
            }
        }

        contentResolver.openInputStream(uri)?.use { fis ->
            java.io.FileOutputStream(dbpath).use { fos ->
                fis.copyTo(fos)
            }
        }
        _events.emit(MainEvent.RestoreDatabaseResult(true))
    }
}
