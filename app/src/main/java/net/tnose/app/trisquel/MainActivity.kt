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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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

sealed class ActiveDialog {
    data class Alert(val message: String) : ActiveDialog()
    data class Confirm(
        val title: String? = null,
        val message: String,
        val positive: String? = null,
        val negative: String? = null,
        val onConfirm: () -> Unit
    ) : ActiveDialog()
    data class SingleChoice(
        val title: String,
        val items: Array<String>,
        val selected: Int,
        val onConfirm: (Int) -> Unit
    ) : ActiveDialog()
    data class Select(
        val title: String? = null,
        val items: Array<String>,
        val ids: List<Int>? = null,
        val onSelected: (Int, Int?) -> Unit
    ) : ActiveDialog()
    data class RichSelection(
        val title: String,
        val icons: List<Int>,
        val titles: Array<String>,
        val descs: Array<String>,
        val onSelected: (Int) -> Unit
    ) : ActiveDialog()
    data class SearchCond(
        val title: String,
        val labels: Array<String>,
        val onSearch: (ArrayList<String>) -> Unit
    ) : ActiveDialog()
}

class MainActivity : AppCompatActivity(), AbstractDialogFragment.Callback {
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

        const val RETCODE_ALERT = 200
        const val RETCODE_CAMERA_TYPE = 300
        const val RETCODE_OPEN_RELEASE_NOTES = 100
        const val RETCODE_DELETE_FILMROLL = 101
        const val RETCODE_DELETE_CAMERA = 102
        const val RETCODE_DELETE_LENS = 103
        const val RETCODE_DELETE_ACCESSORY = 104
        const val RETCODE_EXPORT_DB = 400
        const val RETCODE_SDCARD_PERM = 401
        const val RETCODE_SORT = 402
        const val RETCODE_FILTER_CAMERA = 403
        const val RETCODE_FILTER_FILM_BRAND = 404
        const val RETCODE_SEARCH = 405
        const val RETCODE_BACKUP_PROGRESS = 406
        const val RETCODE_IMPORT_DB = 407
        const val RETCODE_DBCONV_PROGRESS = 408
        const val RETCODE_IMPORT_PROGRESS = 414

        const val ACTION_CLOSE_PROGRESS_DIALOG = "ACTION_CLOSE_PROGRESS_DIALOG"
        const val ACTION_UPDATE_PROGRESS_DIALOG = "ACTION_UPDATE_PROGRESS_DIALOG"
        const val RELEASE_NOTES_URL = "https://x.com/trisquel_app"
    }

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var lensViewModel: LensViewModel
    private lateinit var accessoryViewModel: AccessoryViewModel
    private lateinit var filmRollViewModel: FilmRollViewModel

    private var mExportViewModel: ExportProgressViewModel? = null
    private var mImportViewModel: ImportProgressViewModel? = null
    private var mDbConvViewModel: DbConvProgressViewModel? = null
    private var isIntentServiceWorking: Int = 0 //0: None, 1: DB conversion

    internal var currentRoute = ROUTE_FILMROLLS
    internal var currentFilter = Pair(0, arrayListOf<String>())
    internal var currentSubtitle = ""

    private val activeDialogState = mutableStateOf<ActiveDialog?>(null)

    private val addCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val c = BundleCompat.getParcelable(bundle, "cameraspec", CameraSpec::class.java)!!
            cameraViewModel.insertCamera(c)
            if (c.type == 1) {
                val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)!!
                l.body = c.id
                lensViewModel.insertLens(l)
            }
        }
    }

    private val editCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val c = BundleCompat.getParcelable(bundle, "cameraspec", CameraSpec::class.java)!!
            cameraViewModel.updateCamera(c)
            if (c.type == 1) {
                val dao = TrisquelDao(this)
                dao.connection()
                val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)!!
                val lensid = dao.getFixedLensIdByBody(c.id)
                l.id = lensid
                dao.close()
                lensViewModel.updateLens(l)
            }
        }
    }

    private val addLensLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val l = BundleCompat.getParcelable(result.data?.extras ?: return@registerForActivityResult, "lensspec", LensSpec::class.java)
            if (l != null) lensViewModel.insertLens(l)
        }
    }

    private val editLensLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val l = BundleCompat.getParcelable(result.data?.extras ?: return@registerForActivityResult, "lensspec", LensSpec::class.java)
            if (l != null) lensViewModel.updateLens(l)
        }
    }

    private val addFilmRollLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val dao = TrisquelDao(this.applicationContext)
            dao.connection()
            val c = dao.getCamera(bundle.getInt("camera"))
            dao.close()
            val f = FilmRoll(0, bundle.getString("name")!!, c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
            filmRollViewModel.insert(f.toEntity())
        }
    }

    private val editFilmRollLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val dao = TrisquelDao(this.applicationContext)
            dao.connection()
            val c = dao.getCamera(bundle.getInt("camera"))
            dao.close()
            val f = FilmRoll(bundle.getInt("id"), bundle.getString("name")!!, bundle.getString("created")!!, Util.dateToStringUTC(Date()), c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
            filmRollViewModel.update(f.toEntity())
        }
    }

    private val editPhotoListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val addAccessoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val a = Accessory(0, Util.dateToStringUTC(Date()), Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
            accessoryViewModel.insert(a.toEntity())
        }
    }

    private val editAccessoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val a = Accessory(bundle.getInt("id"), bundle.getString("created")!!, Util.dateToStringUTC(Date()), bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"), bundle.getDouble("focal_length_factor"))
            accessoryViewModel.update(a.toEntity())
        }
    }

    private val backupDirChosenForSlimExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val fragment = ProgressDialog.Builder().build(RETCODE_BACKUP_PROGRESS)
            fragment.showOn(this, "dialog")
            isIntentServiceWorking = 2
            fixOrientation()
            mExportViewModel?.doExport(uri.toString(), 0)
        }
    }

    private val backupDirChosenForFullExLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val fragment = ProgressDialog.Builder().build(RETCODE_BACKUP_PROGRESS)
            fragment.showOn(this, "dialog")
            isIntentServiceWorking = 2
            fixOrientation()
            mExportViewModel?.doExport(uri.toString(), 1)
        }
    }

    private val zipFileChosenForMergeIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("ZipFile", uri.toString())
            val fragment = ProgressDialog.Builder().build(RETCODE_IMPORT_PROGRESS)
            fragment.showOn(this, "dialog")
            isIntentServiceWorking = 3
            fixOrientation()
            mImportViewModel?.doImport(uri.toString(), 0)
        }
    }

    private val zipFileChosenForReplIpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("ZipFile", uri.toString())
            val fragment = ProgressDialog.Builder().build(RETCODE_IMPORT_PROGRESS)
            fragment.showOn(this, "dialog")
            isIntentServiceWorking = 3
            fixOrientation()
            mImportViewModel?.doImport(uri.toString(), 1)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val dbFileChosenForReplDbLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            Log.d("DBFile", uri.toString())
            val dbpath = this.getDatabasePath("trisquel.db")
            val pfd = contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException(uri.toString())
            if (!checkSQLiteFileFormat(pfd)) {
                Toast.makeText(this, getString(R.string.error_not_sqlite3_db), Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyyMMddHHmmss")
            val backupPath = dbpath.absolutePath + "." + sdf.format(calendar.time) + ".bak"
            val bu_fos = FileOutputStream(backupPath)
            val bu_fis = FileInputStream(dbpath)
            val bu_src = bu_fis.channel
            val bu_dst = bu_fos.channel
            bu_dst.transferFrom(bu_src, 0, bu_src.size())
            bu_src.close()
            bu_dst.close()
            val fis = FileInputStream(pfd.fileDescriptor)
            val src = fis.channel
            val dst = FileOutputStream(dbpath).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()
            val intent = RestartActivity.createIntent(applicationContext)
            startActivity(intent)
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
                val progress = workInfo.outputData.getDouble(ExportWorker.PARAM_PERCENTAGE, 100.0)
                setProgressPercentage(progress, status, false)
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(ExportWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(ExportWorker.PARAM_PERCENTAGE, 0.0)
                setProgressPercentage(progress, status, false)
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
                val progress = workInfo.outputData.getDouble(ImportWorker.PARAM_PERCENTAGE, 100.0)
                setProgressPercentage(progress, status, false)
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(ImportWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(ImportWorker.PARAM_PERCENTAGE, 0.0)
                setProgressPercentage(progress, status, false)
            }
        })
        
        mDbConvViewModel = ViewModelProvider(this)[DbConvProgressViewModel::class.java]
        mDbConvViewModel!!.workInfos.observe(this, Observer { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) return@Observer
            val workInfo = listOfWorkInfo[0]
            if (workInfo.state.isFinished) {
                val status = workInfo.outputData.getString(DbConvWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.outputData.getDouble(DbConvWorker.PARAM_PERCENTAGE, 100.0)
                setProgressPercentage(progress, status, false)
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
            } else {
                val status = workInfo.progress.getString(DbConvWorker.PARAM_STATUS) ?: ""
                val progress = workInfo.progress.getDouble(DbConvWorker.PARAM_PERCENTAGE, 0.0)
                setProgressPercentage(progress, status, false)
            }
        })

        isIntentServiceWorking = savedInstanceState?.getInt("is_intent_service_working") ?: 0
        pendingExportMode = if (savedInstanceState?.containsKey("pending_export_mode") == true) savedInstanceState.getInt("pending_export_mode") else null
        pendingImportMode = if (savedInstanceState?.containsKey("pending_import_mode") == true) savedInstanceState.getInt("pending_import_mode") else null

        val filtertype = savedInstanceState?.getInt("filmroll_filtertype") ?: 0
        val filtervalue = savedInstanceState?.getStringArrayList("filmroll_filtervalue") ?: arrayListOf("")
        currentFilter = Pair(filtertype, filtervalue)
        if (filtertype != 0) {
            currentSubtitle = getFilterLabel(currentFilter)
        }
        
        val routeMap = mapOf(ID_FILMROLL to ROUTE_FILMROLLS, ID_CAMERA to ROUTE_CAMERAS, ID_LENS to ROUTE_LENSES, ID_ACCESSORY to ROUTE_ACCESSORIES, ID_FAVORITES to ROUTE_FAVORITES)
        val initialRoute = routeMap[savedInstanceState?.getInt("current_fragment") ?: ID_FILMROLL] ?: ROUTE_FILMROLLS

        setContent {
            MaterialTheme {
                MainAppScreen(initialRoute)
            }
        }
    }

    private fun fixOrientation(){
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun releaseOrientation(){
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onResume() {
        super.onResume()
        val dao = TrisquelDao(this)
        dao.connection()
        val dbConvForAndroid11Done = dao.getConversionState() >= 1
        dao.close()

        if(!dbConvForAndroid11Done && isIntentServiceWorking == 0) {
            val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/db_conv_on_recent_android/")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            return
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val lastVersion = pref.getInt("last_version", 0)
        if (0 < lastVersion && lastVersion < Util.TRISQUEL_VERSION) {
            activeDialogState.value = ActiveDialog.Confirm(
                title = "Trisquel",
                message = getString(R.string.warning_newversion),
                positive = getString(R.string.show_release_notes),
                negative = getString(R.string.close),
                onConfirm = {
                    val uri = Uri.parse(RELEASE_NOTES_URL)
                    val i = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(i)
                }
            )
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
            outState.putInt("filmroll_filtertype", currentFilter.first)
            outState.putStringArrayList("filmroll_filtervalue", currentFilter.second)
        }
        outState.putInt("is_intent_service_working", isIntentServiceWorking)
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

    @Suppress("UNUSED_PARAMETER")
    fun setProgressPercentage(percentage: Double, status: String, error: Boolean){
        val intent = Intent()
        if(percentage >= 100.0){
            isIntentServiceWorking = 0
            intent.action = ACTION_CLOSE_PROGRESS_DIALOG
            intent.putExtra("status", status)
            sendBroadcast(intent)
            releaseOrientation()
        }else {
            intent.action = ACTION_UPDATE_PROGRESS_DIALOG
            intent.putExtra("percentage", percentage)
            intent.putExtra("status", status)
            sendBroadcast(intent)
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

    fun onCameraInteraction(item: CameraSpec, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val used = dao.getCameraUsage(item.id)
            dao.close()
            if (used) {
                activeDialogState.value = ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(item.modelName))
            } else {
                activeDialogState.value = ActiveDialog.Confirm(
                    message = getString(R.string.msg_confirm_remove_item).format(item.modelName),
                    onConfirm = { cameraViewModel.deleteCamera(item.id) }
                )
            }
        } else {
            val intent = Intent(application, EditCameraActivity::class.java)
            intent.putExtra("id", item.id)
            intent.putExtra("type", item.type)
            editCameraLauncher.launch(intent)
        }
    }

    fun onLensInteraction(item: LensSpec, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val used = dao.getLensUsage(item.id)
            dao.close()
            if (used) {
                activeDialogState.value = ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(item.modelName))
            } else {
                activeDialogState.value = ActiveDialog.Confirm(
                    message = getString(R.string.msg_confirm_remove_item).format(item.modelName),
                    onConfirm = { lensViewModel.deleteLens(item.id) }
                )
            }
        } else {
            val intent = Intent(application, EditLensActivity::class.java)
            intent.putExtra("id", item.id)
            editLensLauncher.launch(intent)
        }
    }

    fun onFilmRollInteraction(item: FilmRoll, isLong: Boolean) {
        if (isLong) {
            activeDialogState.value = ActiveDialog.Confirm(
                message = getString(R.string.msg_confirm_remove_item).format(item.name),
                onConfirm = { filmRollViewModel.delete(item.id) }
            )
        } else {
            val intent = Intent(application, EditPhotoListActivity::class.java)
            intent.putExtra("id", item.id)
            editPhotoListLauncher.launch(intent)
        }
    }

    fun onAccessoryInteraction(accessory: Accessory, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val accessoryUsed = dao.getAccessoryUsed(accessory.id)
            dao.close()
            if (accessoryUsed) {
                activeDialogState.value = ActiveDialog.Alert(getString(R.string.msg_cannot_remove_item).format(accessory.name))
            } else {
                activeDialogState.value = ActiveDialog.Confirm(
                    message = getString(R.string.msg_confirm_remove_item).format(accessory.name),
                    onConfirm = { accessoryViewModel.delete(accessory.id) }
                )
            }
        } else {
            val intent = Intent(application, EditAccessoryActivity::class.java)
            intent.putExtra("id", accessory.id)
            editAccessoryLauncher.launch(intent)
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

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            RETCODE_BACKUP_PROGRESS -> if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                mExportViewModel?.cancelExport()
            }
            RETCODE_IMPORT_PROGRESS -> if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                mImportViewModel?.cancelExport()
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {}

    data class DrawerItem(val route: String, val title: String, val iconRes: Int)

    @Composable
    fun TrisquelDialogManager(
        activeDialog: ActiveDialog?,
        onDismiss: () -> Unit
    ) {
        if (activeDialog == null) return

        when (activeDialog) {
            is ActiveDialog.Alert -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    confirmButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
                    },
                    text = { Text(activeDialog.message) }
                )
            }
            is ActiveDialog.Confirm -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { activeDialog.title?.let { Text(it) } ?: Text("Trisquel") },
                    text = { Text(activeDialog.message) },
                    confirmButton = {
                        TextButton(onClick = { activeDialog.onConfirm(); onDismiss() }) {
                            Text(activeDialog.positive ?: stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(activeDialog.negative ?: stringResource(android.R.string.cancel))
                        }
                    }
                )
            }
            is ActiveDialog.SingleChoice -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(activeDialog.title) },
                    text = {
                        LazyColumn {
                            itemsIndexed(activeDialog.items) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeDialog.onConfirm(index); onDismiss() }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = index == activeDialog.selected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    }
                )
            }
            is ActiveDialog.Select -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { activeDialog.title?.let { Text(it) } },
                    text = {
                        LazyColumn {
                            itemsIndexed(activeDialog.items) { index, item ->
                                Text(
                                    text = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeDialog.onSelected(index, activeDialog.ids?.get(index))
                                            onDismiss()
                                        }
                                        .padding(16.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    }
                )
            }
            is ActiveDialog.RichSelection -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(activeDialog.title) },
                    text = {
                        LazyColumn {
                            itemsIndexed(activeDialog.titles) { index, title ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeDialog.onSelected(index); onDismiss() }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painterResource(activeDialog.icons[index]), null, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.titleMedium)
                                        Text(activeDialog.descs[index], style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    }
                )
            }
            is ActiveDialog.SearchCond -> {
                val selectedLabels = remember { mutableStateListOf<String>() }
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(activeDialog.title) },
                    text = {
                        LazyColumn {
                            items(activeDialog.labels) { label ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedLabels.contains(label)) selectedLabels.remove(label)
                                            else selectedLabels.add(label)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = selectedLabels.contains(label), onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            activeDialog.onSearch(ArrayList(selectedLabels))
                            onDismiss()
                        }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    }
                )
            }
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

        var showFilterMenu by remember { mutableStateOf(false) }
        var showOverflowMenu by remember { mutableStateOf(false) }

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
            activeDialog = activeDialogState.value,
            onDismiss = { activeDialogState.value = null }
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
                                            activeDialogState.value = ActiveDialog.RichSelection(
                                                title = getString(R.string.title_backup_mode_selection),
                                                icons = listOf(R.drawable.ic_export, R.drawable.ic_export_img, R.drawable.ic_help),
                                                titles = arrayOf(getString(R.string.title_slim_backup), getString(R.string.title_whole_backup), getString(R.string.title_backup_help)),
                                                descs = arrayOf(getString(R.string.description_slim_backup), getString(R.string.description_whole_backup), getString(R.string.description_backup_help)),
                                                onSelected = { mode ->
                                                    if (mode < 2) checkPermAndExportDB(mode)
                                                    else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")))
                                                }
                                            )
                                        }
                                        "import" -> {
                                            activeDialogState.value = ActiveDialog.RichSelection(
                                                title = getString(R.string.title_import_mode_selection),
                                                icons = listOf(R.drawable.ic_merge, R.drawable.ic_restore, R.drawable.ic_help),
                                                titles = arrayOf(getString(R.string.title_merge_zip), getString(R.string.title_import_zip), getString(R.string.title_backup_help)),
                                                descs = arrayOf(getString(R.string.description_merge_zip), getString(R.string.description_import_zip), getString(R.string.description_backup_help)),
                                                onSelected = { mode ->
                                                    if (mode < 2) checkPermAndImportDB(mode)
                                                    else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")))
                                                }
                                            )
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
                                if (observedRoute == ROUTE_FILMROLLS && currentSubtitle.isNotEmpty()) {
                                    Text(currentSubtitle, style = MaterialTheme.typography.bodySmall)
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
                                    activeDialogState.value = ActiveDialog.SingleChoice(
                                        title = getString(R.string.label_sort_by),
                                        items = arr,
                                        selected = key,
                                        onConfirm = { handleSort(observedRoute, it) }
                                    )
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
                                        if (currentFilter.first != 0) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.label_no_filter)) },
                                                onClick = {
                                                    currentFilter = Pair(0, arrayListOf())
                                                    currentSubtitle = ""
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
                                                activeDialogState.value = ActiveDialog.Select(
                                                    items = cs.map { it.manufacturer + " " + it.modelName }.toTypedArray(),
                                                    ids = cs.map { it.id },
                                                    onSelected = { _, id ->
                                                        if (id != null) {
                                                            currentFilter = Pair(1, arrayListOf(id.toString()))
                                                            currentSubtitle = getFilterLabel(currentFilter)
                                                            filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(1, id.toString()))
                                                        }
                                                    }
                                                )
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
                                                activeDialogState.value = ActiveDialog.Select(
                                                    items = fbs.map { it.first + " " + it.second }.toTypedArray(),
                                                    onSelected = { which, _ ->
                                                        currentFilter = Pair(2, arrayListOf(fbs[which].first, fbs[which].second))
                                                        currentSubtitle = getFilterLabel(currentFilter)
                                                        filmRollViewModel.viewRule.value = Pair(PreferenceManager.getDefaultSharedPreferences(this@MainActivity).getInt("filmroll_sortkey", 0), Pair(2, fbs[which].second))
                                                    }
                                                )
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
                                                        currentFilter = f
                                                        currentSubtitle = getFilterLabel(f)
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
                                    activeDialogState.value = ActiveDialog.SearchCond(
                                        title = getString(R.string.title_dialog_search_by_tags),
                                        labels = tags.sortedBy { it.label }.map { it.label }.toTypedArray(),
                                        onSearch = { checkedLabels ->
                                            if (checkedLabels.isNotEmpty()) {
                                                val intent = Intent(application, SearchActivity::class.java)
                                                intent.putExtra("tags", checkedLabels)
                                                searchLauncher.launch(intent)
                                            }
                                        }
                                    )
                                }) {
                                    Icon(painterResource(R.drawable.ic_search_white_24dp), null)
                                }
                                
                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(Icons.Default.MoreVert, null)
                                    }
                                    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                        val pinnedFilters = getPinnedFilters()
                                        val isPinned = pinnedFilters.any { it.first == currentFilter.first && it.second.containsAll(currentFilter.second) }
                                        if (currentFilter.first != 0 && !isPinned) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.action_pin_current_filter)) },
                                                onClick = { addPinnedFilter(currentFilter); showOverflowMenu = false }
                                            )
                                        } else if (currentFilter.first != 0 && isPinned) {
                                            DropdownMenuItem(
                                                text = { Text(getString(R.string.action_unpin_current_filter)) },
                                                onClick = { removePinnedFilter(currentFilter); showOverflowMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (observedRoute != ROUTE_FAVORITES) {
                        FloatingActionButton(onClick = {
                            when (observedRoute) {
                                ROUTE_CAMERAS -> {
                                    activeDialogState.value = ActiveDialog.Select(
                                        items = arrayOf(getString(R.string.register_ilc), getString(R.string.register_flc)),
                                        onSelected = { which, _ ->
                                            val intent = Intent(application, EditCameraActivity::class.java)
                                            intent.putExtra("type", which)
                                            addCameraLauncher.launch(intent)
                                        }
                                    )
                                }
                                ROUTE_LENSES -> {
                                    val intent = Intent(application, EditLensActivity::class.java)
                                    addLensLauncher.launch(intent)
                                }
                                ROUTE_ACCESSORIES -> {
                                    val intent = Intent(application, EditAccessoryActivity::class.java)
                                    addAccessoryLauncher.launch(intent)
                                }
                                ROUTE_FILMROLLS -> {
                                    val intent = Intent(application, EditFilmRollActivity::class.java)
                                    if (currentFilter.first == 1) {
                                        intent.putExtra("default_camera", currentFilter.second[0].toInt())
                                    } else if (currentFilter.first == 2) {
                                        intent.putExtra("default_manufacturer", currentFilter.second[0])
                                        intent.putExtra("default_brand", currentFilter.second[1])
                                    }
                                    addFilmRollLauncher.launch(intent)
                                }
                            }
                        }, containerColor = MaterialTheme.colorScheme.secondary) {
                            val iconRes = when (observedRoute) {
                                ROUTE_CAMERAS -> R.drawable.ic_menu_camera_white
                                ROUTE_LENSES -> R.drawable.ic_lens_white
                                ROUTE_ACCESSORIES -> R.drawable.ic_extension_white
                                else -> R.drawable.ic_filmroll_vector_white
                            }
                            Icon(painterResource(iconRes), null, tint = Color.White)
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(navController, startDestination = initialRoute, modifier = Modifier.padding(paddingValues)) {
                    composable(ROUTE_FILMROLLS) {
                        val filmrolls by filmRollViewModel.allFilmRollAndRels.observeAsState(emptyList())
                        FilmRollListScreen(
                            filmrolls = filmrolls,
                            onItemClick = { onFilmRollInteraction(FilmRoll.fromEntity(it), false) },
                            onItemLongClick = { onFilmRollInteraction(FilmRoll.fromEntity(it), true) },
                            emptyMessage = getString(R.string.warning_filmroll_not_registered)
                        )
                    }
                    composable(ROUTE_CAMERAS) {
                        val cameras by cameraViewModel.cameras.observeAsState(emptyList())
                        CameraListScreen(
                            cameras = cameras,
                            onItemClick = { onCameraInteraction(it, false) },
                            onItemLongClick = { onCameraInteraction(it, true) },
                            emptyMessage = getString(R.string.warning_cam_not_registered),
                            scrollTargetIndex = null,
                            onScrollConsumed = {}
                        )
                    }
                    composable(ROUTE_LENSES) {
                        val lenses by lensViewModel.lenses.observeAsState(emptyList())
                        LensListScreen(
                            lenses = lenses,
                            onItemClick = { onLensInteraction(it, false) },
                            onItemLongClick = { onLensInteraction(it, true) },
                            emptyMessage = getString(R.string.warning_lens_not_registered),
                            scrollTargetIndex = null,
                            onScrollConsumed = {}
                        )
                    }
                    composable(ROUTE_ACCESSORIES) {
                        val accessories by accessoryViewModel.allAccessories.observeAsState(emptyList())
                        AccessoryListScreen(
                            accessories = accessories,
                            onItemClick = { onAccessoryInteraction(Accessory.fromEntity(it), false) },
                            onItemLongClick = { onAccessoryInteraction(Accessory.fromEntity(it), true) },
                            emptyMessage = getString(R.string.warning_accessory_not_registered)
                        )
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
