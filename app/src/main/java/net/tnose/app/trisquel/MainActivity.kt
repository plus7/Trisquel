package net.tnose.app.trisquel

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val lastVersion = pref.getInt("last_version", 0)
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
        pref.edit().putInt("last_version", Util.TRISQUEL_VERSION).apply()
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
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString("pinned_filters", "[]")
        val array = JSONArray(prefstr)
        val arrayOfFilter = ArrayList<Pair<Int, ArrayList<String>>>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val filtertype = obj.getInt("type")
            val jsonfiltervalues = obj.getJSONArray("values")
            val filtervalues = ArrayList<String>()
            for (j in 0 until jsonfiltervalues.length()){
                filtervalues.add(jsonfiltervalues.getString(j))
            }
            arrayOfFilter.add(Pair(filtertype, filtervalues))
        }
        return arrayOfFilter
    }

    fun addPinnedFilter(newfilter: Pair<Int, ArrayList<String>>){
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString("pinned_filters", "[]")
        val array = JSONArray(prefstr)
        val jsonfilter = JSONObject()
        jsonfilter.put("type", newfilter.first)
        jsonfilter.put("values", JSONArray(newfilter.second))
        array.put(jsonfilter)
        pref.edit().putString("pinned_filters", array.toString()).apply()
    }

    fun removePinnedFilter(filter: Pair<Int, ArrayList<String>>){
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefstr = pref.getString("pinned_filters", "[]")
        val array = JSONArray(prefstr)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val filtertype = obj.getInt("type")
            val jsonfiltervalues = obj.getJSONArray("values")
            val filtervalues = ArrayList<String>()
            for (j in 0 until jsonfiltervalues.length()){
                filtervalues.add(jsonfiltervalues.getString(j))
            }
            if(filtertype == filter.first && filtervalues.containsAll(filter.second)){
                array.remove(i)
                break
            }
        }
        pref.edit().putString("pinned_filters", array.toString()).apply()
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
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val key = when (route) {
            ROUTE_FILMROLLS -> "filmroll_sortkey"
            ROUTE_CAMERAS -> "camera_sortkey"
            ROUTE_LENSES -> "lens_sortkey"
            ROUTE_ACCESSORIES -> "accessory_sortkey"
            else -> ""
        }
        if (key.isNotEmpty()) pref.edit().putInt(key, which).apply()

        when (route) {
            ROUTE_FILMROLLS -> filmRollViewModel.viewRule.value = Pair(which, filmRollViewModel.viewRule.value?.second ?: Pair(0, ""))
            ROUTE_CAMERAS -> cameraViewModel.changeSortKey(which)
            ROUTE_LENSES -> lensViewModel.changeSortKey(which)
            ROUTE_ACCESSORIES -> accessoryViewModel.sortingRule.value = which
        }
    }

    data class DrawerItem(val route: String, val title: String, val iconRes: Int)

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

        var showFilterMenu by rememberSaveable { mutableStateOf(false) }
        var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

        val drawerItems = listOf(
            DrawerItem(ROUTE_FILMROLLS, getString(R.string.title_activity_filmroll_list), R.drawable.ic_filmroll_vector),
            DrawerItem(ROUTE_FAVORITES, getString(R.string.title_activity_favorites), R.drawable.ic_fav_border_black)
        )
        val gearItems = listOf(
            DrawerItem(ROUTE_CAMERAS, getString(R.string.title_activity_cam_list), R.drawable.ic_menu_camera),
            DrawerItem(ROUTE_LENSES, getString(R.string.title_activity_lens_list), R.drawable.ic_lens),
            DrawerItem(ROUTE_ACCESSORIES, getString(R.string.title_activity_accessory_list), R.drawable.ic_extension_black_24dp)
        )
        val infoItems = listOf(
            DrawerItem("settings", getString(R.string.action_settings), R.drawable.ic_settings_black_24dp),
            DrawerItem("backup", getString(R.string.title_backup), R.drawable.ic_export),
            DrawerItem("import", getString(R.string.title_import), R.drawable.ic_import)
        )
        val otherItems = listOf(
            DrawerItem("release_notes", getString(R.string.action_releasenotes), 0),
            DrawerItem("license", getString(R.string.action_license), 0)
        )

        TrisquelDialogManager(
            activeDialog = mainViewModel.activeDialog,
            onDismiss = { mainViewModel.dismissDialog() }
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    LazyColumn {
                        items(drawerItems) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = observedRoute == item.route,
                                icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                                onClick = {
                                    navController.navigate(item.route) { launchSingleTop = true }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                        item { Text(getString(R.string.menu_category_gears), Modifier.padding(16.dp, 8.dp)) }
                        items(gearItems) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = observedRoute == item.route,
                                icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                                onClick = {
                                    navController.navigate(item.route) { launchSingleTop = true }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                        items(infoItems) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = false,
                                icon = { if (item.iconRes != 0) Icon(painterResource(item.iconRes), null) },
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    when (item.route) {
                                        "settings" -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                        "backup" -> {
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
                                        }
                                        "import" -> {
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
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                        items(otherItems) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.title) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    when (item.route) {
                                        "release_notes" -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_NOTES_URL)))
                                        "license" -> startActivity(Intent(this@MainActivity, LicenseActivity::class.java))
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    val titleRes = when(observedRoute) {
                        ROUTE_CAMERAS -> R.string.title_activity_cam_list
                        ROUTE_LENSES -> R.string.title_activity_lens_list
                        ROUTE_ACCESSORIES -> R.string.title_activity_accessory_list
                        ROUTE_FAVORITES -> R.string.title_activity_favorites
                        else -> R.string.title_activity_filmroll_list
                    }
                    TopAppBar(
                        title = {
                            Column {
                                Text(getString(titleRes))
                                if (observedRoute == ROUTE_FILMROLLS && mainViewModel.currentSubtitle.isNotEmpty()) {
                                    Text(mainViewModel.currentSubtitle, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        },
                        actions = {
                            if (observedRoute in listOf(ROUTE_FILMROLLS, ROUTE_CAMERAS, ROUTE_LENSES, ROUTE_ACCESSORIES)) {
                                IconButton(onClick = {
                                    val arr = when(observedRoute){
                                        ROUTE_FILMROLLS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_camera), getString(R.string.label_brand))
                                        ROUTE_CAMERAS -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_format))
                                        ROUTE_LENSES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_mount), getString(R.string.label_focal_length))
                                        ROUTE_ACCESSORIES -> arrayOf(getString(R.string.label_created_date), getString(R.string.label_name), getString(R.string.label_accessory_type))
                                        else -> arrayOf()
                                    }
                                    val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                                    val key = when(observedRoute){
                                        ROUTE_FILMROLLS -> pref.getInt("filmroll_sortkey", 0)
                                        ROUTE_CAMERAS -> pref.getInt("camera_sortkey", 0)
                                        ROUTE_LENSES -> pref.getInt("lens_sortkey", 0)
                                        ROUTE_ACCESSORIES -> pref.getInt("accessory_sortkey", 0)
                                        else -> 0
                                    }
                                    mainViewModel.showDialog(ActiveDialog.SingleChoice(
                                        title = getString(R.string.label_sort_by),
                                        items = arr,
                                        selected = key,
                                        onConfirm = { handleSort(observedRoute, it) }
                                    ))
                                }) {
                                    Icon(painterResource(R.drawable.ic_sort_white_24dp), null)
                                }
                            }
                            if (observedRoute == ROUTE_FILMROLLS) {
                                Box {
                                    IconButton(onClick = { showFilterMenu = true }) {
                                        Icon(painterResource(R.drawable.ic_filter_white), null)
                                    }
                                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                        if (mainViewModel.currentFilter.first != 0) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.label_no_filter)) },
                                                onClick = {
                                                    mainViewModel.currentFilter = Pair(0, arrayListOf())
                                                    mainViewModel.currentSubtitle = ""
                                                    filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(0, ""))
                                                    showFilterMenu = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(getString(R.string.label_filter_by_camera)) },
                                            onClick = {
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
                                                            filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(1, id.toString()))
                                                        }
                                                    }
                                                ))
                                                showFilterMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(getString(R.string.label_filter_by_film_brand)) },
                                            onClick = {
                                                val dao = TrisquelDao(this@MainActivity)
                                                dao.connection()
                                                val fbs = dao.availableFilmBrandList
                                                dao.close()
                                                mainViewModel.showDialog(ActiveDialog.Select(
                                                    items = fbs.map { it.first + " " + it.second }.toTypedArray(),
                                                    onSelected = { which, _ ->
                                                        mainViewModel.currentFilter = Pair(2, arrayListOf(fbs[which].first, fbs[which].second))
                                                        mainViewModel.currentSubtitle = getFilterLabel(mainViewModel.currentFilter)
                                                        filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(2, fbs[which].second))
                                                    }
                                                ))
                                                showFilterMenu = false
                                            }
                                        )
                                        val pinnedFilters = getPinnedFilters()
                                        if (pinnedFilters.isNotEmpty()) {
                                            HorizontalDivider()
                                            pinnedFilters.forEach { f ->
                                                DropdownMenuItem(
                                                    text = { Text(getFilterLabel(f)) },
                                                    onClick = {
                                                        mainViewModel.currentFilter = f
                                                        mainViewModel.currentSubtitle = getFilterLabel(f)
                                                        val searchStr = if(f.first == 1) f.second[0].toInt().toString() else f.second[1]
                                                        filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(f.first, searchStr))
                                                        showFilterMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = {
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
                                }) {
                                    Icon(painterResource(R.drawable.ic_search_white_24dp), null)
                                }
                                
                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(Icons.Default.MoreVert, null)
                                    }
                                    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                        val pinnedFilters = getPinnedFilters()
                                        val isPinned = pinnedFilters.any { it.first == mainViewModel.currentFilter.first && it.second.containsAll(mainViewModel.currentFilter.second) }
                                        if (mainViewModel.currentFilter.first != 0 && !isPinned) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.action_pin_current_filter)) },
                                                onClick = { addPinnedFilter(mainViewModel.currentFilter); showOverflowMenu = false }
                                            )
                                        } else if (mainViewModel.currentFilter.first != 0 && isPinned) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.action_unpin_current_filter)) },
                                                onClick = { removePinnedFilter(mainViewModel.currentFilter); showOverflowMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                NavHost(navController, startDestination = initialRoute, modifier = Modifier.padding(paddingValues)) {
                    composable(ROUTE_FILMROLLS) {
                        val addFilmRollLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) filmRollViewModel.handleAddResult(result.data)
                        }
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) filmRollViewModel.handleEditResult(result.data)
                        }
                        val editPhotoListLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

                        val filmrolls by filmRollViewModel.allFilmRollAndRels.observeAsState(emptyList())
                        val isFilmRollsLoading by filmRollViewModel.isLoading.observeAsState(false)
                        Box(Modifier.fillMaxSize()) {
                            FilmRollListScreen(
                                filmrolls = filmrolls,
                                onItemClick = { item ->
                                    val f = FilmRoll.fromEntity(item)
                                    val intent = Intent(application, EditPhotoListActivity::class.java)
                                    intent.putExtra("id", f.id)
                                    editPhotoListLauncher.launch(intent)
                                },
                                onItemLongClick = { onFilmRollDeleteRequest(FilmRoll.fromEntity(it)) },
                                emptyMessage = getString(R.string.warning_filmroll_not_registered),
                                isLoading = isFilmRollsLoading
                            )
                            FloatingActionButton(
                                onClick = {
                                    val intent = Intent(application, EditFilmRollActivity::class.java)
                                    if (mainViewModel.currentFilter.first == 1) {
                                        intent.putExtra("default_camera", mainViewModel.currentFilter.second[0].toInt())
                                    } else if (mainViewModel.currentFilter.first == 2) {
                                        intent.putExtra("default_manufacturer", mainViewModel.currentFilter.second[0])
                                        intent.putExtra("default_brand", mainViewModel.currentFilter.second[1])
                                    }
                                    addFilmRollLauncher.launch(intent)
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_filmroll_vector_white), null, tint = Color.White)
                            }
                        }
                    }
                    composable(ROUTE_CAMERAS) {
                        val addCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) cameraViewModel.handleAddResult(result.data)
                        }
                        val editCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) cameraViewModel.handleEditResult(result.data)
                        }
                        val cameras by cameraViewModel.cameras.observeAsState(emptyList())
                        val isCamerasLoading by cameraViewModel.isLoading.observeAsState(false)
                        var isFabExpanded by rememberSaveable { mutableStateOf(false) }
                        val interactionSource = remember { MutableInteractionSource() }
                        
                        Box(Modifier.fillMaxSize()) {
                            CameraListScreen(
                                cameras = cameras,
                                onItemClick = { item ->
                                    val intent = Intent(application, EditCameraActivity::class.java)
                                    intent.putExtra("id", item.id)
                                    intent.putExtra("type", item.type)
                                    editCameraLauncher.launch(intent)
                                },
                                onItemLongClick = { onCameraDeleteRequest(it) },
                                emptyMessage = getString(R.string.warning_cam_not_registered),
                                scrollTargetIndex = null,
                                onScrollConsumed = {},
                                isLoading = isCamerasLoading
                            )

                            if (isFabExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) { isFabExpanded = false }
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                AnimatedVisibility(visible = isFabExpanded) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                            Text(
                                                text = getString(R.string.register_flc),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(end = 16.dp)
                                            )
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    isFabExpanded = false
                                                    val intent = Intent(application, EditCameraActivity::class.java)
                                                    intent.putExtra("type", 1)
                                                    addCameraLauncher.launch(intent)
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ) {
                                                Icon(painterResource(R.drawable.ic_menu_camera_white), contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                            Text(
                                                text = getString(R.string.register_ilc),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(end = 16.dp)
                                            )
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    isFabExpanded = false
                                                    val intent = Intent(application, EditCameraActivity::class.java)
                                                    intent.putExtra("type", 0)
                                                    addCameraLauncher.launch(intent)
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ) {
                                                Icon(painterResource(R.drawable.ic_menu_camera_white), contentDescription = null, tint = Color.White)
                                            }
                                        }
                                    }
                                }
                                FloatingActionButton(
                                    onClick = { isFabExpanded = !isFabExpanded },
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                ) {
                                    val rotation by animateFloatAsState(targetValue = if (isFabExpanded) 45f else 0f)
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.graphicsLayer(rotationZ = rotation)
                                    )
                                }
                            }
                        }
                    }
                    composable(ROUTE_LENSES) {
                        val addLensLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) lensViewModel.handleAddResult(result.data)
                        }
                        val editLensLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) lensViewModel.handleEditResult(result.data)
                        }
                        val lenses by lensViewModel.lenses.observeAsState(emptyList())
                        val isLensesLoading by lensViewModel.isLoading.observeAsState(false)
                        Box(Modifier.fillMaxSize()) {
                            LensListScreen(
                                lenses = lenses,
                                onItemClick = { item ->
                                    val intent = Intent(application, EditLensActivity::class.java)
                                    intent.putExtra("id", item.id)
                                    editLensLauncher.launch(intent)
                                },
                                onItemLongClick = { onLensDeleteRequest(it) },
                                emptyMessage = getString(R.string.warning_lens_not_registered),
                                scrollTargetIndex = null,
                                onScrollConsumed = {},
                                isLoading = isLensesLoading
                            )
                            FloatingActionButton(
                                onClick = {
                                    val intent = Intent(application, EditLensActivity::class.java)
                                    addLensLauncher.launch(intent)
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_lens_white), null, tint = Color.White)
                            }
                        }
                    }
                    composable(ROUTE_ACCESSORIES) {
                        val addAccessoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) accessoryViewModel.handleAddResult(result.data)
                        }
                        val editAccessoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                            if (result.resultCode == RESULT_OK) accessoryViewModel.handleEditResult(result.data)
                        }
                        val accessories by accessoryViewModel.allAccessories.observeAsState(emptyList())
                        val isAccessoriesLoading by accessoryViewModel.isLoading.observeAsState(false)
                        Box(Modifier.fillMaxSize()) {
                            AccessoryListScreen(
                                accessories = accessories,
                                onItemClick = { item ->
                                    val a = Accessory.fromEntity(item)
                                    val intent = Intent(application, EditAccessoryActivity::class.java)
                                    intent.putExtra("id", a.id)
                                    editAccessoryLauncher.launch(intent)
                                },
                                onItemLongClick = { onAccessoryDeleteRequest(Accessory.fromEntity(it)) },
                                emptyMessage = getString(R.string.warning_accessory_not_registered),
                                isLoading = isAccessoriesLoading
                            )
                            FloatingActionButton(
                                onClick = {
                                    val intent = Intent(application, EditAccessoryActivity::class.java)
                                    addAccessoryLauncher.launch(intent)
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_extension_white), null, tint = Color.White)
                            }
                        }
                    }
                    composable(ROUTE_FAVORITES) {
                        val context = LocalContext.current
                        val groupedPhotos = remember { mutableStateOf<List<Pair<String, List<Photo>>>>(emptyList()) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                val dao = TrisquelDao(context)
                                dao.connection()
                                val list = dao.getAllFavedPhotos()
                                val map = list.groupBy { it.filmrollid }
                                val list2 = map.values.sortedByDescending { it[0].date }
                                val result = list2.map { l ->
                                    val sortedList = l.sortedBy { it.frameIndex }
                                    val filmrollName = dao.getFilmRoll(l[0].filmrollid)?.name ?: ""
                                    Pair(filmrollName, sortedList)
                                }
                                dao.close()
                                withContext(Dispatchers.Main) { groupedPhotos.value = result }
                            }
                        }
                        FavoritePhotoScreen(
                            groupedPhotos = groupedPhotos.value,
                            columnCount = 3,
                            onItemClick = { photo, list -> onPhotoInteraction(photo, list) }
                        )
                    }
                }
            }
        }
    }
}
