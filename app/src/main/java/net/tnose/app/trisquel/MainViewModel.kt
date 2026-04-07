package net.tnose.app.trisquel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

sealed class MainEvent {
    data class RestoreDatabaseResult(val success: Boolean) : MainEvent()
    object RequireDbConvAction : MainEvent()
    object ShowReleaseNotesConfirm : MainEvent()
    data class ShowToast(val message: String) : MainEvent()
    data class RequestExportPermissions(val mode: Int) : MainEvent()
    data class RequestImportPermissions(val mode: Int) : MainEvent()
    data class LaunchExportDocumentTree(val mode: Int, val fileName: String) : MainEvent()
    data class LaunchImportDocumentPicker(val mode: Int) : MainEvent()
}

class MainViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    var activeDialog by mutableStateOf<ActiveDialog?>(null)

    private val _events = MutableSharedFlow<MainEvent>()
    val events = _events.asSharedFlow()

    fun showDialog(dialog: ActiveDialog) {
        activeDialog = dialog
    }

    fun dismissDialog() {
        activeDialog = null
    }

    private val _currentRoute = mutableStateOf(savedStateHandle.get<String>("current_route") ?: MainActivity.ROUTE_FILMROLLS)
    var currentRoute: String
        get() = _currentRoute.value
        set(value) {
            _currentRoute.value = value
            savedStateHandle["current_route"] = value
        }

    private val _currentFilter = mutableStateOf(Pair(
        savedStateHandle.get<Int>("filter_type") ?: 0,
        savedStateHandle.get<ArrayList<String>>("filter_value") ?: arrayListOf<String>()
    ))
    var currentFilter: Pair<Int, ArrayList<String>>
        get() = _currentFilter.value
        set(value) {
            _currentFilter.value = value
            savedStateHandle["filter_type"] = value.first
            savedStateHandle["filter_value"] = value.second
            currentSubtitle = if (value.first != 0) getFilterLabel(value) else ""
        }

    private val _currentSubtitle = mutableStateOf(savedStateHandle.get<String>("current_subtitle") ?: "")
    var currentSubtitle: String
        get() = _currentSubtitle.value
        set(value) {
            _currentSubtitle.value = value
            savedStateHandle["current_subtitle"] = value
        }

    private val _pendingExportMode = mutableStateOf(savedStateHandle.get<Int?>("pending_export_mode"))
    var pendingExportMode: Int?
        get() = _pendingExportMode.value
        set(value) {
            _pendingExportMode.value = value
            savedStateHandle["pending_export_mode"] = value
        }

    private val _pendingImportMode = mutableStateOf(savedStateHandle.get<Int?>("pending_import_mode"))
    var pendingImportMode: Int?
        get() = _pendingImportMode.value
        set(value) {
            _pendingImportMode.value = value
            savedStateHandle["pending_import_mode"] = value
        }

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val repo = TrisquelRepo(application)
    private val workManager = WorkManager.getInstance(application)

    private val cameraNameCache = mutableMapOf<Int, String>()

    init {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(Util.WORKER_TAG_EXPORT).collect { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) return@collect
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    var status = workInfo.outputData.getString(ExportWorker.PARAM_STATUS) ?: ""
                    if (status == "") {
                        if (workInfo.state == WorkInfo.State.CANCELLED) status = "Backup cancelled."
                        else if (workInfo.state == WorkInfo.State.FAILED) status = "Backup failed."
                    }
                    dismissDialog()
                    _events.emit(MainEvent.ShowToast(status))
                } else {
                    val status = workInfo.progress.getString(ExportWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(ExportWorker.PARAM_PERCENTAGE, 0.0)
                    showDialog(ActiveDialog.Progress(
                        getApplication<Application>().getString(R.string.title_backup), progress, status
                    ) { workManager.cancelAllWorkByTag(Util.WORKER_TAG_EXPORT) })
                }
            }
        }
        
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(Util.WORKER_TAG_IMPORT).collect { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) return@collect
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    var status = workInfo.outputData.getString(ImportWorker.PARAM_STATUS) ?: ""
                    if (status == "") {
                        if (workInfo.state == WorkInfo.State.CANCELLED) status = "Import cancelled."
                        else if (workInfo.state == WorkInfo.State.FAILED) status = "Import failed."
                    }
                    dismissDialog()
                    _events.emit(MainEvent.ShowToast(status))
                } else {
                    val status = workInfo.progress.getString(ImportWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(ImportWorker.PARAM_PERCENTAGE, 0.0)
                    showDialog(ActiveDialog.Progress(
                        getApplication<Application>().getString(R.string.title_import), progress, status
                    ) { workManager.cancelAllWorkByTag(Util.WORKER_TAG_IMPORT) })
                }
            }
        }

        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(Util.WORKER_TAG_DBCONV).collect { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) return@collect
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    val status = workInfo.outputData.getString(DbConvWorker.PARAM_STATUS) ?: ""
                    dismissDialog()
                    if (status.isNotEmpty()) _events.emit(MainEvent.ShowToast(status))
                } else {
                    val status = workInfo.progress.getString(DbConvWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(DbConvWorker.PARAM_PERCENTAGE, 0.0)
                    showDialog(ActiveDialog.Progress("DB Conversion", progress, status, {}))
                }
            }
        }

        // Cache camera names
        viewModelScope.launch {
            repo.getAllCameras().observeForever { cameras ->
                cameras?.forEach {
                    cameraNameCache[it.id] = "${it.manufacturer} ${it.modelName}"
                }
            }
        }
    }

    fun requestBackup(mode: Int, hasPermissions: Boolean) {
        if (!hasPermissions) {
            pendingExportMode = mode
            viewModelScope.launch { _events.emit(MainEvent.RequestExportPermissions(mode)) }
            return
        }
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
        val backupZipFileName = "trisquel-" + LocalDateTime.now().format(formatter) + ".zip"
        viewModelScope.launch { _events.emit(MainEvent.LaunchExportDocumentTree(mode, backupZipFileName)) }
    }

    fun requestImport(mode: Int, hasPermissions: Boolean) {
        if (!hasPermissions) {
            pendingImportMode = mode
            viewModelScope.launch { _events.emit(MainEvent.RequestImportPermissions(mode)) }
            return
        }
        viewModelScope.launch { _events.emit(MainEvent.LaunchImportDocumentPicker(mode)) }
    }

    fun onBackupDirChosen(uri: Uri?, mode: Int) {
        if (uri == null) return
        workManager.pruneWork()
        val req = ExportWorker.createExportRequest(uri.toString(), mode)
        workManager.enqueue(req)
        showDialog(ActiveDialog.Progress(
            getApplication<Application>().getString(R.string.title_backup), 0.0, ""
        ) { workManager.cancelAllWorkByTag(Util.WORKER_TAG_EXPORT) })
    }

    fun onImportFileChosen(uri: Uri?, mode: Int) {
        if (uri == null) return
        workManager.pruneWork()
        val req = ImportWorker.createImportRequest(uri.toString(), mode)
        workManager.enqueue(req)
        showDialog(ActiveDialog.Progress(
            getApplication<Application>().getString(R.string.title_import), 0.0, ""
        ) { workManager.cancelAllWorkByTag(Util.WORKER_TAG_IMPORT) })
    }

    fun onDbFileChosen(uri: Uri?, contentResolver: android.content.ContentResolver) {
        if (uri == null) return
        requestRestoreDatabase(uri, contentResolver)
    }

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
                val name = cameraNameCache[f.second[0].toInt()]
                if (name != null) "📷 $name" else ""
            }
            2 -> "🎞 " + f.second.joinToString(" ")
            else -> ""
        }
    }

    private suspend fun getAllCameras(): List<CameraSpec> = withContext(Dispatchers.IO) {
        repo.getAllCamerasRaw().map { CameraSpec.fromEntity(it) }
    }

    private suspend fun getAvailableFilmBrandList(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        repo.getAvailableFilmBrandList().map { it.manufacturer to it.brand }
    }

    private suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        repo.getAllTagsRaw().map { Tag(it.id, it.label, it.refcnt ?: 0) }
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
        val metadata = repo.getMetadata()
        (metadata?.pathConvDone ?: 0) >= 1
    }

    fun checkAppStartupState(currentVersion: Int) = viewModelScope.launch {
        val dbConvForAndroid11Done = isDbConvForAndroid11Done()
        val isDbConvActive = workManager.getWorkInfosByTag(Util.WORKER_TAG_DBCONV).get().firstOrNull()?.state?.isFinished == false
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

    fun requestRestoreDatabase(uri: Uri, contentResolver: android.content.ContentResolver) = viewModelScope.launch(Dispatchers.IO) {
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

        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)
        val backupPath = dbpath.absolutePath + "." + LocalDateTime.now().format(formatter) + ".bak"

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
