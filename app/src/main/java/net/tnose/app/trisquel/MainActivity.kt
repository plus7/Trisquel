package net.tnose.app.trisquel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import net.tnose.app.trisquel.ui.theme.TrisquelTheme

class MainActivity : AppCompatActivity() {
    companion object {
        const val ID_FILMROLL = 0
        const val ID_CAMERA = 1
        const val ID_LENS = 2
        const val ID_ACCESSORY = 3
        const val ID_FAVORITES = 4
        
        const val ROUTE_FILMROLLS = "filmrolls"
        const val ROUTE_CAMERAS = "cameras"
        const val ROUTE_LENSES = "lenses"
        const val ROUTE_ACCESSORIES = "accessories"
        const val ROUTE_FAVORITES = "favorites"
        const val RELEASE_NOTES_URL = "https://x.com/trisquel_app"
    }

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var lensViewModel: LensViewModel
    private lateinit var accessoryViewModel: AccessoryViewModel
    private lateinit var filmRollViewModel: FilmRollViewModel

    private var mExportViewModel: ExportProgressViewModel? = null
    private var mImportViewModel: ImportProgressViewModel? = null
    private var mDbConvViewModel: DbConvProgressViewModel? = null
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    internal var currentRoute = ROUTE_FILMROLLS

    private val backupDirChosenForSlimExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleBackupDirChosen(result.data, 0)
        }
    }

    private val backupDirChosenForFullExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleBackupDirChosen(result.data, 1)
        }
    }

    private val zipFileChosenForMergeIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleZipFileChosen(result.data, 0)
        }
    }

    private val zipFileChosenForReplIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleZipFileChosen(result.data, 1)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val dbFileChosenForReplDbLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            handleDbFileChosen(result.data)
        }
    }

    fun handleBackupDirChosen(data: Intent?, mode: Int) {
        val uri = data?.data ?: return
        mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_backup), 0.0, "") { mExportViewModel?.cancelExport() })
        mExportViewModel?.doExport(uri.toString(), mode)
    }

    fun handleZipFileChosen(data: Intent?, mode: Int) {
        val uri = data?.data ?: return
        Log.d("ZipFile", uri.toString())
        mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_import), 0.0, "") { mImportViewModel?.cancelExport() })
        mImportViewModel?.doImport(uri.toString(), mode)
    }

    @SuppressLint("SimpleDateFormat")
    fun handleDbFileChosen(data: Intent?) {
        val uri = data?.data ?: return
        Log.d("DBFile", uri.toString())
        mainViewModel.requestRestoreDatabase(uri, contentResolver)
    }

    fun handleExportPermissionsResult(permissions: Map<String, Boolean>) {
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pendingExportMode?.let { exportDBDialog(it) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        pendingExportMode = null
    }

    fun handleImportPermissionsResult(permissions: Map<String, Boolean>) {
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pendingImportMode?.let { importDBDialog(it) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        pendingImportMode = null
    }

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private var pendingExportMode: Int? = null
    private val requestExportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        handleExportPermissionsResult(permissions)
    }

    private var pendingImportMode: Int? = null
    private val requestImportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        handleImportPermissionsResult(permissions)
    }

    internal val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        lensViewModel = ViewModelProvider(this)[LensViewModel::class.java]
        accessoryViewModel = ViewModelProvider(this)[AccessoryViewModel::class.java]
        filmRollViewModel = ViewModelProvider(this)[FilmRollViewModel::class.java]

        lifecycleScope.launch {
            mainViewModel.events.collect { event ->
                when (event) {
                    is MainEvent.RestoreDatabaseResult -> {
                        if (event.success) {
                            val intent = RestartActivity.createIntent(applicationContext)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, getString(R.string.error_not_sqlite3_db), Toast.LENGTH_LONG).show()
                        }
                    }
                    is MainEvent.RequireDbConvAction -> {
                        val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/db_conv_on_recent_android/")
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    is MainEvent.ShowReleaseNotesConfirm -> {
                        mainViewModel.showDialog(ActiveDialog.Confirm(
                            title = "Trisquel",
                            message = getString(R.string.warning_newversion),
                            positive = getString(R.string.show_release_notes),
                            negative = getString(R.string.close),
                            onConfirm = {
                                val uri = Uri.parse(RELEASE_NOTES_URL)
                                val i = Intent(Intent.ACTION_VIEW, uri)
                                startActivity(i)
                            }
                        ))
                    }
                }
            }
        }
        lifecycleScope.launch {
            cameraViewModel.events.collect { event ->
                when (event) {
                    is CameraEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.modelName)))
                    is CameraEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.modelName), onConfirm = { cameraViewModel.deleteCamera(event.id) }))
                }
            }
        }
        lifecycleScope.launch {
            lensViewModel.events.collect { event ->
                when (event) {
                    is LensEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.modelName)))
                    is LensEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.modelName), onConfirm = { lensViewModel.deleteLens(event.id) }))
                }
            }
        }
        lifecycleScope.launch {
            accessoryViewModel.events.collect { event ->
                when (event) {
                    is AccessoryEvent.ShowCannotDeleteAlert -> mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(event.name)))
                    is AccessoryEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.name), onConfirm = { accessoryViewModel.delete(event.id) }))
                }
            }
        }
        lifecycleScope.launch {
            filmRollViewModel.events.collect { event ->
                when (event) {
                    is FilmRollEvent.ShowDeleteConfirm -> mainViewModel.showDialog(ActiveDialog.Confirm(message = getString(R.string.msg_confirm_remove_item).format(event.name), onConfirm = { filmRollViewModel.delete(event.id) }))
                }
            }
        }

        mExportViewModel = ViewModelProvider(this)[ExportProgressViewModel::class.java]
        mExportViewModel!!.workInfos.observe(this, Observer { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) return@Observer
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                var status = workInfo.outputData.getString(ExportWorker.PARAM_STATUS) ?: ""
                if (status == "") {
                    if (workInfo.state == WorkInfo.State.CANCELLED) status = "Backup cancelled."
                    else if (workInfo.state == WorkInfo.State.FAILED) status = "Backup failed."
                }
                mainViewModel.dismissDialog()
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(ExportWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(ExportWorker.PARAM_PERCENTAGE, 0.0)
                mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_backup), progress, status) { mExportViewModel?.cancelExport() })
            }
        })
        
        mImportViewModel = ViewModelProvider(this)[ImportProgressViewModel::class.java]
        mImportViewModel!!.workInfos.observe(this, Observer { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) return@Observer
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                var status = workInfo.outputData.getString(ImportWorker.PARAM_STATUS) ?: ""
                if (status == "") {
                    if (workInfo.state == WorkInfo.State.CANCELLED) status = "Import cancelled."
                    else if (workInfo.state == WorkInfo.State.FAILED) status = "Import failed."
                }
                mainViewModel.dismissDialog()
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(ImportWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(ImportWorker.PARAM_PERCENTAGE, 0.0)
                mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_import), progress, status) { mImportViewModel?.cancelExport() })
            }
        })
        
        mDbConvViewModel = ViewModelProvider(this)[DbConvProgressViewModel::class.java]
        mDbConvViewModel!!.workInfos.observe(this, Observer { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) return@Observer
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                val status = workInfo.outputData.getString(DbConvWorker.PARAM_STATUS) ?: ""
                mainViewModel.dismissDialog()
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(DbConvWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(DbConvWorker.PARAM_PERCENTAGE, 0.0)
                mainViewModel.showDialog(ActiveDialog.Progress("DB Conversion", progress, status, {}))
            }
        })

        pendingExportMode = if (savedInstanceState?.containsKey("pending_export_mode") == true) savedInstanceState.getInt("pending_export_mode") else null
        pendingImportMode = if (savedInstanceState?.containsKey("pending_import_mode") == true) savedInstanceState.getInt("pending_import_mode") else null

        val filtertype = savedInstanceState?.getInt("filmroll_filtertype") ?: 0
        val filtervalue = savedInstanceState?.getStringArrayList("filmroll_filtervalue") ?: arrayListOf("")
        mainViewModel.currentFilter = Pair(filtertype, filtervalue)
        if (filtertype != 0) {
            mainViewModel.currentSubtitle = mainViewModel.getFilterLabel(mainViewModel.currentFilter)
        }
        
        val routeMap = mapOf(ID_FILMROLL to ROUTE_FILMROLLS, ID_CAMERA to ROUTE_CAMERAS, ID_LENS to ROUTE_LENSES, ID_ACCESSORY to ROUTE_ACCESSORIES, ID_FAVORITES to ROUTE_FAVORITES)
        val initialRoute = routeMap[savedInstanceState?.getInt("current_fragment") ?: ID_FILMROLL] ?: ROUTE_FILMROLLS

        setContent {
            TrisquelTheme {
                MainAppScreen(initialRoute)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Issue #55 を修正する案としてGeminiが提案してきたがこれでいいのか半信半疑
        cameraViewModel.load()
        lensViewModel.load()

        val isDbConvActive = mDbConvViewModel?.workInfos?.value?.firstOrNull()?.state?.isFinished == false
        mainViewModel.checkAppStartupState(isDbConvActive, Util.TRISQUEL_VERSION)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_fragment", when (currentRoute) {
            ROUTE_CAMERAS -> ID_CAMERA
            ROUTE_LENSES -> ID_LENS
            ROUTE_ACCESSORIES -> ID_ACCESSORY
            ROUTE_FAVORITES -> ID_FAVORITES
            else -> ID_FILMROLL
        })
        if (currentRoute == ROUTE_FILMROLLS) {
            outState.putInt("filmroll_filtertype", mainViewModel.currentFilter.first)
            outState.putStringArrayList("filmroll_filtervalue", mainViewModel.currentFilter.second)
        }
        pendingExportMode?.let { outState.putInt("pending_export_mode", it) }
        pendingImportMode?.let { outState.putInt("pending_import_mode", it) }
    }





    private fun checkPermAndExportDB(mode: Int) {
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val mediaLocDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        val notificationDenied = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) false
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (readDenied || mediaLocDenied || notificationDenied){
            pendingExportMode = mode
            requestExportPermissionsLauncher.launch(PERMISSIONS)
            return
        }
        exportDBDialog(mode)
    }

    private fun checkPermAndImportDB(mode: Int) {
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val mediaLocDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        val notificationDenied = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) false
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (readDenied || mediaLocDenied || notificationDenied){
            pendingImportMode = mode
            requestImportPermissionsLauncher.launch(PERMISSIONS)
            return
        }
        importDBDialog(mode)
    }

    @SuppressLint("SimpleDateFormat")
    private fun exportDBDialog(mode: Int) {
        val chooserIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        chooserIntent.addCategory(Intent.CATEGORY_OPENABLE)
        chooserIntent.type = "application/zip"
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss")
        val backupZipFileName = "trisquel-" + sdf.format(calendar.time) + ".zip"
        chooserIntent.putExtra(Intent.EXTRA_TITLE, backupZipFileName)
        if(mode==0) backupDirChosenForSlimExLauncher.launch(chooserIntent)
        else        backupDirChosenForFullExLauncher.launch(chooserIntent)
    }

    private fun importDBDialog(mode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if(mode == 2){
            intent.type = "*/*"
        }else {
            intent.type = "application/zip"
        }
        when(mode){
            0 -> zipFileChosenForMergeIpLauncher.launch(intent)
            1 -> zipFileChosenForReplIpLauncher.launch(intent)
            else -> dbFileChosenForReplDbLauncher.launch(intent)
        }
    }

    fun onCameraDeleteRequest(item: CameraSpec) {
        cameraViewModel.requestDeleteCamera(item)
    }

    fun onLensDeleteRequest(item: LensSpec) {
        lensViewModel.requestDeleteLens(item)
    }

    fun onFilmRollDeleteRequest(item: FilmRoll) {
        filmRollViewModel.requestDelete(item)
    }

    fun onAccessoryDeleteRequest(accessory: Accessory) {
        accessoryViewModel.requestDeleteAccessory(accessory)
    }

    fun onPhotoInteraction(item: Photo?, list: List<Photo?>) {
        val intent = Intent(application, GalleryActivity::class.java)
        intent.putExtra("photo", item)
        intent.putParcelableArrayListExtra("favList", ArrayList(list))
        startActivity(intent)
    }

    private fun handleSort(route: String, which: Int) {
        userPreferencesRepository.setSortKey(route, which)

        when (route) {
            ROUTE_FILMROLLS -> filmRollViewModel.viewRule.value = Pair(which, filmRollViewModel.viewRule.value?.second ?: Pair(0, ""))
            ROUTE_CAMERAS -> cameraViewModel.changeSortKey(which)
            ROUTE_LENSES -> lensViewModel.changeSortKey(which)
            ROUTE_ACCESSORIES -> accessoryViewModel.sortingRule.value = which
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppScreen(initialRoute: String) {
        val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val observedRoute = navBackStackEntry?.destination?.route ?: initialRoute

        LaunchedEffect(observedRoute) {
            currentRoute = observedRoute
        }

        TrisquelDialogManager(
            activeDialog = mainViewModel.activeDialog,
            onDismiss = { mainViewModel.dismissDialog() }
        )

        TrisquelNavigationDrawer(
            drawerState = drawerState,
            navController = navController,
            observedRoute = observedRoute,
            scope = scope,
            onSettingsClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
            onBackupClick = {
                mainViewModel.showDialog(ActiveDialog.RichSelection(
                    title = getString(R.string.title_backup_mode_selection),
                    icons = listOf(R.drawable.ic_export, R.drawable.ic_export_img, R.drawable.ic_help),
                    titles = arrayOf(getString(R.string.title_slim_backup), getString(R.string.title_whole_backup), getString(R.string.title_backup_help)),
                    descs = arrayOf(getString(R.string.description_slim_backup), getString(R.string.description_whole_backup), getString(R.string.description_backup_help)),
                    onSelected = { mode ->
                        if (mode < 2) checkPermAndExportDB(mode)
                        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")))
                    }
                ))
            },
            onImportClick = {
                mainViewModel.showDialog(ActiveDialog.RichSelection(
                    title = getString(R.string.title_import_mode_selection),
                    icons = listOf(R.drawable.ic_merge, R.drawable.ic_restore, R.drawable.ic_help),
                    titles = arrayOf(getString(R.string.title_merge_zip), getString(R.string.title_import_zip), getString(R.string.title_backup_help)),
                    descs = arrayOf(getString(R.string.description_merge_zip), getString(R.string.description_import_zip), getString(R.string.description_backup_help)),
                    onSelected = { mode ->
                        if (mode < 2) checkPermAndImportDB(mode)
                        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")))
                    }
                ))
            },
            onReleaseNotesClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_NOTES_URL))) },
            onLicenseClick = { startActivity(Intent(this@MainActivity, LicenseActivity::class.java)) }
        ) {
            Scaffold(
                topBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val observedRoute = navBackStackEntry?.destination?.route ?: initialRoute

                    TrisquelTopAppBar(
                        observedRoute = observedRoute,
                        currentSubtitle = mainViewModel.currentSubtitle,
                        currentFilter = mainViewModel.currentFilter,
                        drawerState = drawerState,
                        scope = scope,
                        onSortClick = {
                            val arr = when(observedRoute){
                                ROUTE_FILMROLLS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_camera), getString(R.string.label_brand))
                                ROUTE_CAMERAS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_format))
                                ROUTE_LENSES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_focal_length))
                                ROUTE_ACCESSORIES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_accessory_type))
                                else -> arrayOf()
                            }
                            val key = userPreferencesRepository.getSortKey(observedRoute)
                            mainViewModel.showDialog(ActiveDialog.SingleChoice(
                                title = getString(R.string.label_sort_by),
                                items = arr,
                                selected = key,
                                onConfirm = { handleSort(observedRoute, it) }
                            ))
                        },
                        onFilterNoFilterClick = {
                            mainViewModel.currentFilter = Pair(0, arrayListOf())
                            mainViewModel.currentSubtitle = ""
                            filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(0, ""))
                        },
                        onFilterByCameraClick = {
                            mainViewModel.requestFilterByCamera { _, id ->
                                if (id != null) {
                                    mainViewModel.currentFilter = Pair(1, arrayListOf(id.toString()))
                                    mainViewModel.currentSubtitle = mainViewModel.getFilterLabel(mainViewModel.currentFilter)
                                    filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(1, id.toString()))
                                }
                            }
                        },
                        onFilterByFilmBrandClick = {
                            mainViewModel.requestFilterByFilmBrand { fb ->
                                mainViewModel.currentFilter = Pair(2, arrayListOf(fb.first, fb.second))
                                mainViewModel.currentSubtitle = mainViewModel.getFilterLabel(mainViewModel.currentFilter)
                                filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(2, fb.second))
                            }
                        },
                        onPinnedFilterClick = { f ->
                            mainViewModel.currentFilter = f
                            mainViewModel.currentSubtitle = mainViewModel.getFilterLabel(f)
                            val searchStr = if(f.first == 1) f.second[0].toInt().toString() else f.second[1]
                            filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(f.first, searchStr))
                        },
                        onSearchClick = {
                            val title = getString(R.string.title_dialog_search_by_tags)
                            mainViewModel.requestSearch(title) { checkedLabels ->
                                if (checkedLabels.isNotEmpty()) {
                                    val intent = Intent(application, SearchActivity::class.java)
                                    intent.putExtra("tags", checkedLabels)
                                    searchLauncher.launch(intent)
                                }
                            }
                        },
                        onPinFilterClick = { mainViewModel.addPinnedFilter(mainViewModel.currentFilter) },
                        onUnpinFilterClick = { mainViewModel.removePinnedFilter(mainViewModel.currentFilter) },
                        getPinnedFilters = { mainViewModel.getPinnedFilters() },
                        getFilterLabel = { mainViewModel.getFilterLabel(it) }
                    )
                }
            ) { paddingValues ->
                TrisquelNavHost(
                    navController = navController,
                    initialRoute = initialRoute,
                    modifier = Modifier.padding(paddingValues),
                    mainViewModel = mainViewModel,
                    filmRollViewModel = filmRollViewModel,
                    cameraViewModel = cameraViewModel,
                    lensViewModel = lensViewModel,
                    accessoryViewModel = accessoryViewModel,
                    onFilmRollDeleteRequest = { onFilmRollDeleteRequest(it) },
                    onCameraDeleteRequest = { onCameraDeleteRequest(it) },
                    onLensDeleteRequest = { onLensDeleteRequest(it) },
                    onAccessoryDeleteRequest = { onAccessoryDeleteRequest(it) },
                    onPhotoInteraction = { photo, list -> onPhotoInteraction(photo, list) }
                )
            }

        }
    }
}
