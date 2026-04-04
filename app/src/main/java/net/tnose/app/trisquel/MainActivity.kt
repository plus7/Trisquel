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
            val uri = result.data?.data ?: return@registerForActivityResult
            mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_backup), 0.0, "") { mExportViewModel?.cancelExport() })
            mExportViewModel?.doExport(uri.toString(), 0)
        }
    }

    private val backupDirChosenForFullExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_backup), 0.0, "") { mExportViewModel?.cancelExport() })
            mExportViewModel?.doExport(uri.toString(), 1)
        }
    }

    private val zipFileChosenForMergeIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("ZipFile", uri.toString())
            mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_import), 0.0, "") { mImportViewModel?.cancelExport() })
            mImportViewModel?.doImport(uri.toString(), 0)
        }
    }

    private val zipFileChosenForReplIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("ZipFile", uri.toString())
            mainViewModel.showDialog(ActiveDialog.Progress(getString(R.string.title_import), 0.0, "") { mImportViewModel?.cancelExport() })
            mImportViewModel?.doImport(uri.toString(), 1)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val dbFileChosenForReplDbLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("DBFile", uri.toString())
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val dbpath = getDatabasePath("trisquel.db")
                    val pfd = contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException(uri.toString())
                    if (!checkSQLiteFileFormat(pfd)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, getString(R.string.error_not_sqlite3_db), Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }
                    val calendar = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyyMMddHHmmss")
                    val backupPath = dbpath.absolutePath + "." + sdf.format(calendar.time) + ".bak"
                    
                    if (dbpath.exists()) {
                        FileInputStream(dbpath).use { fis ->
                            FileOutputStream(backupPath).use { fos ->
                                fis.copyTo(fos)
                            }
                        }
                    }
                    
                    FileInputStream(pfd.fileDescriptor).use { fis ->
                        FileOutputStream(dbpath).use { fos ->
                            fis.copyTo(fos)
                        }
                    }
                }
                val intent = RestartActivity.createIntent(applicationContext)
                startActivity(intent)
            }
        }
    }

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private var pendingExportMode: Int? = null
    private val requestExportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pendingExportMode?.let { exportDBDialog(it) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        pendingExportMode = null
    }

    private var pendingImportMode: Int? = null
    private val requestImportPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            pendingImportMode?.let { importDBDialog(it) }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
        pendingImportMode = null
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
            mainViewModel.currentSubtitle = getFilterLabel(mainViewModel.currentFilter)
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

        val dao = TrisquelDao(this)
        dao.connection()
        val dbConvForAndroid11Done = dao.getConversionState() >= 1
        dao.close()

        val isDbConvActive = mDbConvViewModel?.workInfos?.value?.firstOrNull()?.state?.isFinished == false
        if(!dbConvForAndroid11Done && !isDbConvActive) {
            val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/db_conv_on_recent_android/")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            return
        }

        val lastVersion = userPreferencesRepository.getLastVersion()
        if (0 < lastVersion && lastVersion < Util.TRISQUEL_VERSION) {
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
        userPreferencesRepository.setLastVersion(Util.TRISQUEL_VERSION)
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

    fun getPinnedFilters(): ArrayList<Pair<Int, ArrayList<String>>>{
        return userPreferencesRepository.getPinnedFilters()
    }

    fun addPinnedFilter(newfilter: Pair<Int, ArrayList<String>>){
        userPreferencesRepository.addPinnedFilter(newfilter)
    }

    fun removePinnedFilter(filter: Pair<Int, ArrayList<String>>){
        userPreferencesRepository.removePinnedFilter(filter)
    }

    fun getFilterLabel(f: Pair<Int, ArrayList<String>>): String {
        return when(f.first) {
            1 -> {
                val dao = TrisquelDao(this)
                dao.connection()
                val c = dao.getCamera(f.second[0].toInt())
                dao.close()
                if (c != null) "📷 " + c.manufacturer + " " + c.modelName else ""
            }
            2 -> "🎞 " + f.second.joinToString(" ")
            else -> ""
        }
    }

    fun checkSQLiteFileFormat(pfd: ParcelFileDescriptor): Boolean{
        val header = byteArrayOf(
                0x53, 0x51, 0x4c, 0x69,
                0x74, 0x65, 0x20, 0x66,
                0x6f, 0x72, 0x6d, 0x61,
                0x74, 0x20, 0x33, 0x00)
        try {
            val fis = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteArray(16)
            val readsize = fis.read(buffer)
            return readsize == 16 && header.contentEquals(buffer)
        }catch (e: Exception){
            return false
        }
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
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val used = dao.getCameraUsage(item.id)
        dao.close()
        if (used) {
            mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(item.modelName)))
        } else {
            mainViewModel.showDialog(ActiveDialog.Confirm(
                message = getString(R.string.msg_confirm_remove_item).format(item.modelName),
                onConfirm = { cameraViewModel.deleteCamera(item.id) }
            ))
        }
    }

    fun onLensDeleteRequest(item: LensSpec) {
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val used = dao.getLensUsage(item.id)
        dao.close()
        if (used) {
            mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(item.modelName)))
        } else {
            mainViewModel.showDialog(ActiveDialog.Confirm(
                message = getString(R.string.msg_confirm_remove_item).format(item.modelName),
                onConfirm = { lensViewModel.deleteLens(item.id) }
            ))
        }
    }

    fun onFilmRollDeleteRequest(item: FilmRoll) {
        mainViewModel.showDialog(ActiveDialog.Confirm(
            message = getString(R.string.msg_confirm_remove_item).format(item.name),
            onConfirm = { filmRollViewModel.delete(item.id) }
        ))
    }

    fun onAccessoryDeleteRequest(accessory: Accessory) {
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val accessoryUsed = dao.getAccessoryUsed(accessory.id)
        dao.close()
        if (accessoryUsed) {
            mainViewModel.showDialog(ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(accessory.name)))
        } else {
            mainViewModel.showDialog(ActiveDialog.Confirm(
                message = getString(R.string.msg_confirm_remove_item).format(accessory.name),
                onConfirm = { accessoryViewModel.delete(accessory.id) }
            ))
        }
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
                            val dao = TrisquelDao(this@MainActivity)
                            dao.connection()
                            val cs = dao.allCameras
                            dao.close()
                            cs.sortBy { it.manufacturer + " " + it.modelName }
                            mainViewModel.showDialog(ActiveDialog.Select(
                                items = cs.map { it.manufacturer + " " + it.modelName }.toTypedArray(),
                                ids = cs.map { it.id },
                                onSelected = { _, id ->
                                    if (id != null) {
                                        mainViewModel.currentFilter = Pair(1, arrayListOf(id.toString()))
                                        mainViewModel.currentSubtitle = getFilterLabel(mainViewModel.currentFilter)
                                        filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(1, id.toString()))
                                    }
                                }
                            ))
                        },
                        onFilterByFilmBrandClick = {
                            val dao = TrisquelDao(this@MainActivity)
                            dao.connection()
                            val fbs = dao.availableFilmBrandList
                            dao.close()
                            mainViewModel.showDialog(ActiveDialog.Select(
                                items = fbs.map { it.first + " " + it.second }.toTypedArray(),
                                onSelected = { which, _ ->
                                    mainViewModel.currentFilter = Pair(2, arrayListOf(fbs[which].first, fbs[which].second))
                                    mainViewModel.currentSubtitle = getFilterLabel(mainViewModel.currentFilter)
                                    filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(2, fbs[which].second))
                                }
                            ))
                        },
                        onPinnedFilterClick = { f ->
                            mainViewModel.currentFilter = f
                            mainViewModel.currentSubtitle = getFilterLabel(f)
                            val searchStr = if(f.first == 1) f.second[0].toInt().toString() else f.second[1]
                            filmRollViewModel.viewRule.value = Pair(userPreferencesRepository.getSortKey(ROUTE_FILMROLLS), Pair(f.first, searchStr))
                        },
                        onSearchClick = {
                            val dao = TrisquelDao(this@MainActivity)
                            dao.connection()
                            val tags = dao.allTags
                            dao.close()
                            mainViewModel.showDialog(ActiveDialog.SearchCond(
                                title = getString(R.string.title_dialog_search_by_tags),
                                labels = tags.sortedBy { it.label }.map { it.label }.toTypedArray(),
                                onSearch = { checkedLabels ->
                                    if (checkedLabels.isNotEmpty()) {
                                        val intent = Intent(application, SearchActivity::class.java)
                                        intent.putExtra("tags", checkedLabels)
                                        searchLauncher.launch(intent)
                                    }
                                }
                            ))
                        },
                        onPinFilterClick = { addPinnedFilter(mainViewModel.currentFilter) },
                        onUnpinFilterClick = { removePinnedFilter(mainViewModel.currentFilter) },
                        getPinnedFilters = { getPinnedFilters() },
                        getFilterLabel = { getFilterLabel(it) }
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
