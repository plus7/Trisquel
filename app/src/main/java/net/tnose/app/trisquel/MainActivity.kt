package net.tnose.app.trisquel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import net.tnose.app.trisquel.dummy.DummyContent
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        CameraFragment.OnListFragmentInteractionListener,
        LensFragment.OnListFragmentInteractionListener,
        FilmRollFragment.OnListFragmentInteractionListener,
        EmptyFragment.OnFragmentInteractionListener,
        AccessoryFragment.OnListFragmentInteractionListener,
        FavoritePhotoFragment.OnListFragmentInteractionListener,
        AbstractDialogFragment.Callback {
    companion object {
        const val ID_FILMROLL = 0
        const val ID_CAMERA = 1
        const val ID_LENS = 2
        const val ID_ACCESSORY = 3
        const val ID_FAVORITES = 4
        const val REQCODE_EDIT_CAMERA = 1
        const val REQCODE_ADD_CAMERA = 2
        const val REQCODE_EDIT_LENS = 3
        const val REQCODE_ADD_LENS = 4
        const val REQCODE_EDIT_FILMROLL = 5
        const val REQCODE_ADD_FILMROLL = 6
        const val REQCODE_EDIT_PHOTO_LIST = 7
        const val REQCODE_EDIT_ACCESSORY = 8
        const val REQCODE_ADD_ACCESSORY = 9
        const val REQCODE_BACKUP_DIR_CHOSEN_FOR_SLIMEX = 10
        const val REQCODE_BACKUP_DIR_CHOSEN_FOR_FULLEX = 11
        const val REQCODE_SEARCH = 12
        const val REQCODE_ZIPFILE_CHOSEN_FOR_MERGEIP = 13
        const val REQCODE_ZIPFILE_CHOSEN_FOR_REPLIP = 14
        const val REQCODE_DBFILE_CHOSEN_FOR_REPLDB = 15

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
        const val RETCODE_SDCARD_PERM_FOR_SLIMEX = 409 /* 汚い実装だがこうするしかないか？ */
        const val RETCODE_SDCARD_PERM_FOR_FULLEX = 410
        const val RETCODE_SDCARD_PERM_FOR_MERGEIP = 411
        const val RETCODE_SDCARD_PERM_FOR_REPLIP = 412
        const val RETCODE_SDCARD_PERM_FOR_REPLDB = 413
        const val RETCODE_IMPORT_PROGRESS = 414

        const val ACTION_CLOSE_PROGRESS_DIALOG = "ACTION_CLOSE_PROGRESS_DIALOG"
        const val ACTION_UPDATE_PROGRESS_DIALOG = "ACTION_UPDATE_PROGRESS_DIALOG"
        const val RELEASE_NOTES_URL = "http://pentax.tnose.net/tag/trisquel_releasenotes/"
    }

    //private var localBroadcastManager: androidx.localbroadcastmanager.content.LocalBroadcastManager? = null
    //private var progressFilter: IntentFilter? = null
    //private var progressReceiver: ProgressReceiver? = null
    private var mExportViewModel: ExportProgressViewModel? = null
    private var mImportViewModel: ImportProgressViewModel? = null
    private var mDbConvViewModel: DbConvProgressViewModel? = null
    private var isIntentServiceWorking: Int = 0 //0: None, 1: DB conversion

    private lateinit var currentFragment: androidx.fragment.app.Fragment
    private val pinnedFilterViewId: ArrayList<Int> = arrayListOf()

    internal val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun selectFragment(currentFragmentId: Int, filtertype: Int, filtervalue: ArrayList<String>){
        //val f: androidx.fragment.app.Fragment
        val transaction = supportFragmentManager.beginTransaction()
        when (currentFragmentId) {
            ID_CAMERA -> {
                currentFragment = CameraFragment()
                setTitle(R.string.title_activity_cam_list)
                supportActionBar?.subtitle = ""
            }
            ID_LENS -> {
                currentFragment = LensFragment()
                setTitle(R.string.title_activity_lens_list)
                supportActionBar?.subtitle = ""
            }
            ID_ACCESSORY -> {
                currentFragment = AccessoryFragment()
                setTitle(R.string.title_activity_accessory_list)
                supportActionBar?.subtitle = ""
            }
            ID_FAVORITES -> {
                currentFragment = FavoritePhotoFragment()
                setTitle(R.string.title_activity_favorites)
                supportActionBar?.subtitle = ""
            }
            else -> {
                currentFragment = FilmRollFragment.newInstance(filtertype, filtervalue)
                setTitle(R.string.title_activity_filmroll_list)
                val dao = TrisquelDao(this)
                dao.connection()
                val subtitle = when(filtertype){
                    1 -> {
                        val c = dao.getCamera(filtervalue[0].toInt())
                        "📷 " + c?.manufacturer + " " + c?.modelName
                    }
                    2 -> "🎞 " + filtervalue.joinToString(" ")
                    else -> ""
                }
                dao.close()
                supportActionBar?.subtitle = subtitle
            }
        }

        //addではなくreplaceでないとonCreateが再び呼ばれたときに変になる（以前作ったfragmentの残骸が残って表示される）
        //この辺の処理は画面回転なども考えるとよろしくないが先送りする
        transaction.replace(R.id.container, currentFragment)
        transaction.commit()
    }

    fun refreshFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        when (currentFragment) {
            is CameraFragment -> fab.setImageResource(R.drawable.ic_menu_camera_white)
            is LensFragment -> fab.setImageResource(R.drawable.ic_lens_white)
            is AccessoryFragment -> fab.setImageResource(R.drawable.ic_extension_white)
            else -> fab.setImageResource(R.drawable.ic_filmroll_vector_white)
        }
        fab.setOnClickListener {
            val intent: Intent
            when (currentFragment) {
                is CameraFragment -> {
                    val fragment = SelectDialogFragment.Builder()
                            .build(RETCODE_CAMERA_TYPE)
                    fragment.arguments?.putInt("id", -1) //dummy value
                    fragment.arguments?.putStringArray("items", arrayOf(getString(R.string.register_ilc), getString(R.string.register_flc)))
                    fragment.showOn(this@MainActivity, "dialog")
                }
                is LensFragment -> {
                    intent = Intent(application, EditLensActivity::class.java)
                    startActivityForResult(intent, REQCODE_ADD_LENS)
                }
                is AccessoryFragment -> {
                    intent = Intent(application, EditAccessoryActivity::class.java)
                    startActivityForResult(intent, REQCODE_ADD_ACCESSORY)
                }
                else -> {
                    intent = Intent(application, EditFilmRollActivity::class.java)
                    if(currentFragment is FilmRollFragment){
                        val filter = (currentFragment as FilmRollFragment).currentFilter
                        when(filter.first){
                            1 -> intent.putExtra("default_camera", filter.second[0].toInt())
                            2 -> {
                                intent.putExtra("default_manufacturer", filter.second[0])
                                intent.putExtra("default_brand", filter.second[1])
                            }
                        }
                    }
                    startActivityForResult(intent, REQCODE_ADD_FILMROLL)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mExportViewModel = ViewModelProvider(this).get(ExportProgressViewModel::class.java)
        mExportViewModel!!.workInfos.observe(this,
            Observer { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) {
                    return@Observer
                }
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    //WorkInfo.State.SUCCEEDED
                    var status = workInfo.outputData.getString(ExportWorker.PARAM_STATUS) ?: ""
                    // XXX: なんかよくわからんがstatusが空で返ってきてしまうパターンがあるのでワークアラウンドを入れる
                    if (status == "") {
                        if(workInfo.state == WorkInfo.State.CANCELLED) {
                            status = "Backup cancelled."
                        }else if(workInfo.state == WorkInfo.State.FAILED) {
                            status = "Backup failed."
                        }
                    }
                    val progress = workInfo.outputData.getDouble(ExportWorker.PARAM_PERCENTAGE, 100.0)
                    setProgressPercentage(progress, status, false)
                    Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
                } else {
                    val status = workInfo.progress.getString(ExportWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(ExportWorker.PARAM_PERCENTAGE, 0.0)
                    setProgressPercentage(progress, status, false)
                }
            }
        )
        mImportViewModel = ViewModelProvider(this).get(ImportProgressViewModel::class.java)
        mImportViewModel!!.workInfos.observe(this,
            Observer { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) {
                    return@Observer
                }
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    //WorkInfo.State.SUCCEEDED
                    var status = workInfo.outputData.getString(ImportWorker.PARAM_STATUS) ?: ""
                    // XXX: なんかよくわからんがstatusが空で返ってきてしまうパターンがあるのでワークアラウンドを入れる
                    if (status == "") {
                        if(workInfo.state == WorkInfo.State.CANCELLED) {
                            status = "Import cancelled."
                        }else if(workInfo.state == WorkInfo.State.FAILED) {
                            status = "Import failed."
                        }
                    }
                    val progress = workInfo.outputData.getDouble(ImportWorker.PARAM_PERCENTAGE, 100.0)
                    setProgressPercentage(progress, status, false)
                    Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
                } else {
                    val status = workInfo.progress.getString(ImportWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(ImportWorker.PARAM_PERCENTAGE, 0.0)
                    setProgressPercentage(progress, status, false)
                }
            }
        )
        mDbConvViewModel = ViewModelProvider(this).get(DbConvProgressViewModel::class.java)
        mDbConvViewModel!!.workInfos.observe(this,
            Observer { listOfWorkInfo ->
                if (listOfWorkInfo.isNullOrEmpty()) {
                    return@Observer
                }
                val workInfo = listOfWorkInfo[0]
                if (workInfo.state.isFinished) {
                    //WorkInfo.State.SUCCEEDED
                    val status = workInfo.outputData.getString(DbConvWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.outputData.getDouble(DbConvWorker.PARAM_PERCENTAGE, 100.0)
                    setProgressPercentage(progress, status, false)
                    Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
                } else {
                    val status = workInfo.progress.getString(DbConvWorker.PARAM_STATUS) ?: ""
                    val progress = workInfo.progress.getDouble(DbConvWorker.PARAM_PERCENTAGE, 0.0)
                    setProgressPercentage(progress, status, false)
                }
            }
        )
        setContentView(R.layout.activity_main2)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val currentFragmentId = if (savedInstanceState != null) {
            savedInstanceState.getInt("current_fragment")
        } else {
            ID_FILMROLL
        }

        isIntentServiceWorking = if (savedInstanceState != null){
            savedInstanceState.getInt("is_intent_service_working")
        } else {
            0
        }

        val filtertype = savedInstanceState?.getInt("filmroll_filtertype") ?: 0
        val filtervalue = savedInstanceState?.getStringArrayList("filmroll_filtervalue") ?: arrayListOf("")
        selectFragment(currentFragmentId, filtertype, filtervalue)

        refreshFab()

        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/db_conv_on_recent_android/")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return
            }

            fixOrientation() // 画面回転時のDialogの挙動が怪しいので一時的にこうする
            isIntentServiceWorking = 1

            val fragment = ProgressDialog.Builder()
                    .build(RETCODE_DBCONV_PROGRESS)
            fragment.arguments?.putString("title", "Updating DB")
            fragment.arguments?.putBoolean("cancellable", false)
            fragment.showOn(this, "dialog")
            mDbConvViewModel?.doDbConv()
            return
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val lastVersion = pref.getInt("last_version", 0)
        if (0 < lastVersion && lastVersion < Util.TRISQUEL_VERSION) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(RETCODE_OPEN_RELEASE_NOTES)
            fragment.arguments?.putString("title", "Trisquel")
            //if (lastVersion != 0) {
                fragment.arguments?.putString("message", getString(R.string.warning_newversion))
            //} else {
                //fragment.arguments?.putString("message", getString(R.string.warning_firstrun))
            //}
            fragment.arguments?.putString("positive", getString(R.string.show_release_notes))
            fragment.arguments?.putString("negative", getString(R.string.close))
            fragment.showOn(this, "dialog")
        }
        val e = pref.edit()
        e.putInt("last_version", Util.TRISQUEL_VERSION)
        e.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_fragment", when (currentFragment) {
            is CameraFragment -> ID_CAMERA
            is LensFragment -> ID_LENS
            is AccessoryFragment -> ID_ACCESSORY
            is FavoritePhotoFragment -> ID_FAVORITES
            else -> ID_FILMROLL
        })
        if(currentFragment is FilmRollFragment){
            val (filtertype, filtervalue) = (currentFragment as FilmRollFragment).currentFilter
            outState.putInt("filmroll_filtertype", filtertype)
            outState.putStringArrayList("filmroll_filtervalue", filtervalue)
        }
        outState.putInt("is_intent_service_working", isIntentServiceWorking)
    }

    override fun onBackPressed() {
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
        val e = pref.edit()
        e.putString("pinned_filters", array.toString())
        e.apply()
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
            if(filtertype == filter.first && filtervalues.containsAll(filtervalues)){
                array.remove(i)
                break
            }
        }

        val e = pref.edit()
        e.putString("pinned_filters", array.toString())
        e.apply()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val sortmenu = menu.findItem(R.id.action_sort)
        sortmenu.isVisible = when(currentFragment){
            is FilmRollFragment, is CameraFragment, is LensFragment, is AccessoryFragment -> true
            else -> false
        }
        sortmenu.setShowAsAction(
                when (currentFragment) {
                    is CameraFragment, is LensFragment, is AccessoryFragment -> SHOW_AS_ACTION_IF_ROOM
                    else -> SHOW_AS_ACTION_NEVER
                }
        )
        val filtermenu = menu.findItem(R.id.action_filter)
        filtermenu.isVisible = when(currentFragment){
            is FilmRollFragment -> true
            else -> false
        }
        val searchmenu = menu.findItem(R.id.action_search)
        searchmenu.isVisible = when(currentFragment){
            is FilmRollFragment -> true
            else -> false
        }

        val pinfiltermenu = menu.findItem(R.id.action_pin_current_filter)
        val unpinfiltermenu = menu.findItem(R.id.action_unpin_current_filter)
        val filters = getPinnedFilters()
        pinfiltermenu.isVisible = when(currentFragment){
            is FilmRollFragment -> {
                val currentFilter = (currentFragment as FilmRollFragment).currentFilter
                currentFilter.first != 0 &&
                        (filters.find {
                            it.first == currentFilter.first &&
                                    it.second.containsAll(currentFilter.second)
                        } == null)
            }
            else -> false
        }
        unpinfiltermenu.isVisible = when(currentFragment){
            is FilmRollFragment -> (currentFragment as FilmRollFragment).currentFilter.first != 0 && !pinfiltermenu.isVisible
            else -> false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(application, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_search) {
            val dao = TrisquelDao(this)
            dao.connection()
            val tags = dao.allTags
            dao.close()

            val fragment = SearchCondDialogFragment.Builder().build(RETCODE_SEARCH)
            fragment.arguments?.putString("title", getString(R.string.title_dialog_search_by_tags))
            fragment.arguments?.putStringArray("labels", tags.sortedBy { it.label }.map { it.label }.toTypedArray())
            fragment.showOn(this, "dialog")
            return true
        } else if (id == R.id.action_sort){
            val fragment = SingleChoiceDialogFragment.Builder().build(RETCODE_SORT)
            val arr = when(currentFragment){
                is FilmRollFragment -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_camera),
                        getString(R.string.label_brand))
                is CameraFragment -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_mount),
                        getString(R.string.label_format))
                is LensFragment -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_mount),
                        getString(R.string.label_focal_length))
                is AccessoryFragment -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_accessory_type))
                else -> arrayOf()
            }
            val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val key = when(currentFragment){
                is FilmRollFragment -> pref.getInt("filmroll_sortkey", 0)
                is CameraFragment -> pref.getInt("camera_sortkey", 0)
                is LensFragment -> pref.getInt("lens_sortkey", 0)
                is AccessoryFragment -> pref.getInt("accessory_sortkey", 0)
                else -> 0
            }
            fragment.arguments?.putString("title", getString(R.string.label_sort_by))
            fragment.arguments?.putInt("selected", key)
            fragment.arguments?.putStringArray("items", arr)
            fragment.arguments?.putString("positive", getString(android.R.string.ok))
            fragment.showOn(this, "dialog")
        } else if (id == R.id.action_filter){
            val vItem: View = findViewById(R.id.action_filter)
            val popupMenu = PopupMenu(this, vItem, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
            popupMenu.inflate(R.menu.menu_filter)
            val nofilter_item = popupMenu.menu.findItem(R.id.action_no_filtering)
            if (currentFragment is FilmRollFragment) {
                nofilter_item.isVisible = (currentFragment as FilmRollFragment).currentFilter.first != 0
            }
            val filters = getPinnedFilters()
            val dao_ = TrisquelDao(this) // 警告が鬱陶しいので仮にアンダースコアをつける
            dao_.connection()
            while(filters.size > pinnedFilterViewId.size){
                pinnedFilterViewId.add(View.generateViewId())
            }
            for((i, f) in filters.withIndex()){
                when(f.first){
                    1 -> {
                        val c = dao_.getCamera(f.second[0].toInt())
                        if (c != null)
                            popupMenu.menu.add(0, pinnedFilterViewId[i], 0, c.manufacturer + " " + c.modelName)
                    }
                    2 -> {
                        popupMenu.menu.add(0, pinnedFilterViewId[i], 0, f.second.joinToString(" "))
                    }
                }
            }
            dao_.close() // いままでどうやらcloseを忘れていたらしい

            //popupMenu.menu.add(R.id.group_recent_filter, View.generateViewId() , 0, "hogehoge")
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_no_filtering ->
                        if (currentFragment is FilmRollFragment) {
                            (currentFragment as FilmRollFragment).currentFilter = Pair(0, arrayListOf(""))
                            supportActionBar?.subtitle = ""
                        }
                    R.id.action_filter_by_camera ->
                        if (currentFragment is FilmRollFragment) {
                            val fragment = SelectDialogFragment.Builder()
                                    .build(RETCODE_FILTER_CAMERA)

                            val dao = TrisquelDao(this)
                            dao.connection()
                            val cs = dao.allCameras
                            dao.close()
                            cs.sortBy { it.manufacturer + " " + it.modelName }
                            fragment.arguments?.putStringArray("items", cs.map { it.manufacturer + " " + it.modelName }.toTypedArray())
                            fragment.arguments?.putIntegerArrayList("ids", ArrayList(cs.map { it.id }))
                            fragment.showOn(this@MainActivity, "dialog")
                        }
                    R.id.action_filter_by_filmbrand ->
                        if (currentFragment is FilmRollFragment) {
                            val fragment = SelectDialogFragment.Builder()
                                    .build(RETCODE_FILTER_FILM_BRAND)
                            val dao = TrisquelDao(this)
                            dao.connection()
                            val fbs = dao.availableFilmBrandList
                            dao.close()
                            fragment.arguments?.putStringArray("items", fbs.map { it.first + " " + it.second }.toTypedArray())
                            fragment.showOn(this@MainActivity, "dialog")
                        }
                    else -> {
                        if(pinnedFilterViewId.contains(it.itemId) && currentFragment is FilmRollFragment){
                            val index = pinnedFilterViewId.indexOf(it.itemId)
                            val f = getPinnedFilters()[index]
                            (currentFragment as FilmRollFragment).currentFilter = f
                            val dao = TrisquelDao(this)
                            dao.connection()
                            val subtitle = when(f.first){
                                1 -> {
                                    val c = dao.getCamera(f.second[0].toInt())
                                    "📷 " + c?.manufacturer + " " + c?.modelName
                                }
                                2 -> "🎞 " + f.second.joinToString(" ")
                                else -> ""
                            }
                            supportActionBar?.subtitle = subtitle
                            dao.close()
                        }
                    }
                }
                true
            }
            popupMenu.show()
        } else if (id == R.id.action_pin_current_filter){
            addPinnedFilter((currentFragment as FilmRollFragment).currentFilter)
        } else if (id == R.id.action_unpin_current_filter){
            removePinnedFilter((currentFragment as FilmRollFragment).currentFilter)
        }

        return super.onOptionsItemSelected(item)
    }
    /*
    fun backupToZip(zipFile: File) {
        val zos = ZipOutputStream(FileOutputStream(zipFile))
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(this)
        dao.connection()

        // Metadata
        val metaze = ZipEntry("metadata.json")
        zos.putNextEntry(metaze)
        val metadata = JSONObject()
        metadata.put("DB_VERSION", DatabaseHelper.DATABASE_VERSION)

        osw.write(metadata.toString())
        osw.flush()
        zos.closeEntry()

        val types = listOf("camera", "lens", "filmroll", "accessory", "tag", "tagmap")

        for(type in types) {
            val ze = ZipEntry(type + ".json")
            zos.putNextEntry(ze)
            val entries = dao.getAllEntriesJSON(type)
            osw.write(entries.toString())
            osw.flush()
            zos.closeEntry()
        }

        val zep = ZipEntry("photo.json")
        zos.putNextEntry(zep)
        val entries = dao.getAllEntriesJSON("photo")
        osw.write(entries.toString())
        osw.flush()
        zos.closeEntry()

        val hs = HashSet<String>()
        for (i in 0..entries.length()-1){
            val e = entries.getJSONObject(i)
            val s = e.getString("suppimgs")
            if(s.isEmpty()) continue
            val imgs = JSONArray(s)
            for (j in 0..imgs.length() - 1) {
                val path = imgs[j].toString()
                if(hs.contains(path)) continue
                hs.add(path)
                val f = File(path)
                if(!f.exists()) continue
                val ze = ZipEntry("imgs$path")
                zos.putNextEntry(ze)
                try {
                    val buf = ByteArray(1024 * 128)
                    val bis = BufferedInputStream(FileInputStream(f))
                    while (true) {
                        val len = bis.read(buf)
                        if (len < 0) break
                        zos.write(buf, 0, len)
                    }
                } catch (e: IOException) {
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                } finally {
                    zos.closeEntry()
                }
            }
        }

        dao.close()
        zos.close()
    }
     */

    @Suppress("UNUSED_PARAMETER")
    fun setProgressPercentage(percentage: Double, status: String, error: Boolean){
        val intent = Intent()
        if(percentage >= 100.0){
            isIntentServiceWorking = 0
            intent.action = ACTION_CLOSE_PROGRESS_DIALOG
            intent.putExtra("status", status)
            sendBroadcast(intent)
            releaseOrientation()
            if(lifecycle.currentState == Lifecycle.State.RESUMED) {
                selectFragment(ID_FILMROLL, 0, arrayListOf(""))
                refreshFab()
            }
        }else {
            intent.action = ACTION_UPDATE_PROGRESS_DIALOG
            intent.putExtra("percentage", percentage)
            intent.putExtra("status", status)
            sendBroadcast(intent)
        }
    }

    fun checkSQLiteFileFormat(pfd: ParcelFileDescriptor): Boolean{
        // "SQLite format 3\0"
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

    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data == null) return
        val frag : androidx.fragment.app.Fragment = currentFragment
        when (requestCode) {
            REQCODE_ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = BundleCompat.getParcelable(bundle!!, "cameraspec", CameraSpec::class.java)!!
                if (frag is CameraFragment) frag.insertCamera(c)
                if (c.type == 1) {
                    val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)!!
                    l.body = c.id
                    val dao = TrisquelDao(this)
                    dao.connection()
                    dao.addLens(l)
                    dao.close()
                }
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_EDIT_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = BundleCompat.getParcelable(bundle!!, "cameraspec", CameraSpec::class.java)!!
                if (frag is CameraFragment) frag.updateCamera(c)
                if (c.type == 1) {
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val l = BundleCompat.getParcelable(bundle, "fixed_lens", LensSpec::class.java)!!
                    val lensid = dao.getFixedLensIdByBody(c.id)
                    l.id = lensid
                    dao.updateLens(l)
                    dao.close()
                }
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                val l = BundleCompat.getParcelable(data.extras!!, "lensspec", LensSpec::class.java)
                if (frag is LensFragment && l != null) frag.insertLens(l)
            }
            REQCODE_EDIT_LENS -> if (resultCode == Activity.RESULT_OK) {
                val l = BundleCompat.getParcelable(data.extras!!, "lensspec", LensSpec::class.java)
                if (frag is LensFragment && l != null) frag.updateLens(l)
            }
            REQCODE_ADD_FILMROLL -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val c = dao.getCamera(bundle!!.getInt("camera"))
                dao.close()
                val f = FilmRoll(
                        0,
                        bundle.getString("name")!!,
                        c!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("brand")!!,
                        bundle.getInt("iso"),
                        36
                )
                if (frag is FilmRollFragment) frag.insertFilmRoll(f)
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_EDIT_FILMROLL -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val c = dao.getCamera(bundle!!.getInt("camera"))
                dao.close()
                val f = FilmRoll(
                        bundle.getInt("id"),
                        bundle.getString("name")!!,
                        bundle.getString("created")!!,
                        Util.dateToStringUTC(Date()),
                        c!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("brand")!!,
                        bundle.getInt("iso"),
                        36
                )
                if (frag is FilmRollFragment) frag.updateFilmRoll(f)
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            /*REQCODE_EDIT_PHOTO_LIST -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                // Room化に伴いいらなくなった
                // if (frag is FilmRollFragment) frag.refreshFilmRoll(bundle!!.getInt("filmroll"))
            }*//* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_ADD_ACCESSORY -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                // Room化したときは、新規作成時のidは0でよい
                val a = Accessory(0, Util.dateToStringUTC(Date()), Util.dateToStringUTC(Date()),
                        bundle!!.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"),
                        bundle.getDouble("focal_length_factor"))
                if (frag is AccessoryFragment) frag.insertAccessory(a)
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_EDIT_ACCESSORY -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val a = Accessory(bundle!!.getInt("id"), bundle.getString("created")!!, Util.dateToStringUTC(Date()),
                        bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"),
                        bundle.getDouble("focal_length_factor"))
                if (frag is AccessoryFragment) frag.updateAccessory(a)
            }/* else if (resultCode == Activity.RESULT_CANCELED) {
            }*/
            REQCODE_BACKUP_DIR_CHOSEN_FOR_SLIMEX,
            REQCODE_BACKUP_DIR_CHOSEN_FOR_FULLEX -> if (resultCode == Activity.RESULT_OK) {
                if (data.data != null) {
                    val uri: Uri = data.data!!
                    val mode = if (requestCode == REQCODE_BACKUP_DIR_CHOSEN_FOR_SLIMEX) 0 else 1
                    val fragment = ProgressDialog.Builder()
                            .build(RETCODE_BACKUP_PROGRESS)
                    fragment.showOn(this, "dialog")

                    isIntentServiceWorking = 2
                    fixOrientation()
                    mExportViewModel?.doExport(uri.toString(), mode)
                }
            }
            REQCODE_ZIPFILE_CHOSEN_FOR_MERGEIP,
            REQCODE_ZIPFILE_CHOSEN_FOR_REPLIP -> if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data.data
                if (uri != null) {
                    Log.d("ZipFile", uri.toString())

                    val fragment = ProgressDialog.Builder()
                            .build(RETCODE_IMPORT_PROGRESS)
                    fragment.showOn(this, "dialog")
                    isIntentServiceWorking = 3
                    fixOrientation()
                    val mode = when (requestCode) {
                        REQCODE_ZIPFILE_CHOSEN_FOR_MERGEIP -> 0
                        REQCODE_ZIPFILE_CHOSEN_FOR_REPLIP -> 1
                        else -> 2
                    }
                    mImportViewModel?.doImport(uri.toString(), mode)
                }
            }
            REQCODE_DBFILE_CHOSEN_FOR_REPLDB -> if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data.data
                Log.d("DBFile", uri.toString())
                val dbpath = this.getDatabasePath("trisquel.db")
                if (uri != null) {
                    val pfd = contentResolver.openFileDescriptor(uri, "r")
                    if (pfd == null) throw FileNotFoundException(uri.toString())

                    if (!checkSQLiteFileFormat(pfd)) {
                        Toast.makeText(this, getString(R.string.error_not_sqlite3_db), Toast.LENGTH_LONG).show()
                        return
                    }

                    // DB置換前に現在のDBを念の為アプリ内ローカルの領域にコピーしておく
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
                    //再起動する。一般ユーザーであればDB_VERSION=16以前のデータしか持っていないので、データ変換処理が走るはず
                    val intent: Intent = RestartActivity.createIntent(applicationContext)
                    startActivity(intent)
                }
            }
            /*REQCODE_SEARCH -> {
            }*/
            else -> {
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        var titleRsc: Int = 0
        var fabRsc: Int = 0
        when(id) {
            R.id.nav_settings -> {
                val intent = Intent(application, SettingsActivity::class.java)
                startActivity(intent)
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_backup_sqlite -> {
                val fragment = RichSelectionDialogFragment.Builder()
                        .build(RETCODE_EXPORT_DB)
                fragment.arguments?.putInt("id", -1) //dummy value
                fragment.arguments?.putString("title", getString(R.string.title_backup_mode_selection))
                fragment.arguments?.putIntegerArrayList("items_icon",
                        arrayListOf(R.drawable.ic_export,
                                R.drawable.ic_export_img,
                                R.drawable.ic_help))
                fragment.arguments?.putStringArray("items_title",
                        arrayOf(getString(R.string.title_slim_backup),
                                getString(R.string.title_whole_backup),
                                getString(R.string.title_backup_help)))
                fragment.arguments?.putStringArray("items_desc",
                        arrayOf(getString(R.string.description_slim_backup),
                                getString(R.string.description_whole_backup),
                                getString(R.string.description_backup_help)))
                fragment.showOn(this@MainActivity, "dialog")
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_import -> {
                val fragment = RichSelectionDialogFragment.Builder()
                        .build(RETCODE_IMPORT_DB)
                fragment.arguments?.putInt("id", -1) //dummy value
                fragment.arguments?.putString("title", getString(R.string.title_import_mode_selection))
                fragment.arguments?.putIntegerArrayList("items_icon",
                        arrayListOf(R.drawable.ic_merge,
                                R.drawable.ic_restore,
                                //R.drawable.ic_db_restore,
                                R.drawable.ic_help))
                fragment.arguments?.putStringArray("items_title",
                        arrayOf(getString(R.string.title_merge_zip),
                                getString(R.string.title_import_zip),
                                //getString(R.string.title_import_db),
                                getString(R.string.title_backup_help)))
                fragment.arguments?.putStringArray("items_desc",
                        arrayOf(getString(R.string.description_merge_zip),
                                getString(R.string.description_import_zip),
                                //getString(R.string.description_import_db),
                                getString(R.string.description_backup_help)))
                fragment.showOn(this@MainActivity, "dialog")
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_license -> {
                val intent = Intent(this, LicenseActivity::class.java)
                startActivity(intent)
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_release_notes -> {
                val uri = Uri.parse(RELEASE_NOTES_URL) // + Integer.toString(Util.TRISQUEL_VERSION)
                val i = Intent(Intent.ACTION_VIEW, uri)
                startActivity(i)
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_camera -> {
                currentFragment = CameraFragment()
                titleRsc = R.string.title_activity_cam_list
                fabRsc = R.drawable.ic_menu_camera_white
            }
            R.id.nav_lens -> {
                currentFragment = LensFragment()
                titleRsc = R.string.title_activity_lens_list
                fabRsc = R.drawable.ic_lens_white
            }
            R.id.nav_filmrolls -> {
                currentFragment = FilmRollFragment()
                titleRsc = R.string.title_activity_filmroll_list
                fabRsc = R.drawable.ic_filmroll_vector_white
            }
            R.id.nav_favorites -> {
                currentFragment = FavoritePhotoFragment()
                titleRsc = R.string.title_activity_favorites
            }
            R.id.nav_accessory -> {
                currentFragment = AccessoryFragment()
                titleRsc = R.string.title_activity_accessory_list
                fabRsc = R.drawable.ic_extension_white
            }
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, currentFragment)
        transaction.commit()
        setTitle(titleRsc)
        supportActionBar?.subtitle = ""

        when(id){
            R.id.nav_favorites -> {
                fab.hide()
            }
            R.id.nav_camera, R.id.nav_lens, R.id.nav_filmrolls, R.id.nav_accessory -> {
                //一旦隠さないと設定したリソースが反映されない。
                //おそらくAndroid側のバグ
                fab.hide()
                fab.setImageResource(fabRsc)
                fab.show()
            }
        }

        invalidateOptionsMenu()

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(item: CameraSpec, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val used = dao.getCameraUsage(item.id)
            dao.close()
            if (used) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments?.putString("message", getString(R.string.msg_cannot_remove_item).format(item.modelName))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_CAMERA)
                fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(item.modelName))
                fragment.arguments?.putInt("id", item.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditCameraActivity::class.java)
            intent.putExtra("id", item.id)
            intent.putExtra("type", item.type)
            startActivityForResult(intent, REQCODE_EDIT_CAMERA)
        }
    }

    override fun onListFragmentInteraction(item: LensSpec, isLong: Boolean) {
        if (isLong) {

            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val used = dao.getLensUsage(item.id)
            dao.close()
            if (used) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments?.putString("message", getString(R.string.msg_cannot_remove_item).format(item.modelName))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_LENS)
                fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(item.modelName))
                fragment.arguments?.putInt("id", item.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditLensActivity::class.java)
            intent.putExtra("id", item.id)
            startActivityForResult(intent, REQCODE_EDIT_LENS)
        }
    }

    override fun onListFragmentInteraction(item: FilmRoll, isLong: Boolean) {
        if (isLong) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(RETCODE_DELETE_FILMROLL)
            fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(item.name))
            fragment.arguments?.putInt("id", item.id)
            fragment.showOn(this, "dialog")
        } else {
            val intent = Intent(application, EditPhotoListActivity::class.java)
            intent.putExtra("id", item.id)
            startActivityForResult(intent, REQCODE_EDIT_PHOTO_LIST)
        }
    }

    override fun onListFragmentInteraction(accessory: Accessory, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val accessoryUsed = dao.getAccessoryUsed(accessory.id)
            dao.close()
            if (accessoryUsed) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments?.putString("message", getString(R.string.msg_cannot_remove_item).format(accessory.name))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_ACCESSORY)
                fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(accessory.name))
                fragment.arguments?.putInt("id", accessory.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditAccessoryActivity::class.java)
            intent.putExtra("id", accessory.id)
            startActivityForResult(intent, REQCODE_EDIT_ACCESSORY)
        }
    }

    override fun onListFragmentInteraction(item: Photo?, list: List<Photo?>) {
        val intent = Intent(application, GalleryActivity::class.java)
        intent.putExtra("photo", item)
        intent.putParcelableArrayListExtra("favList", ArrayList(list))
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onListFragmentInteraction(item: DummyContent.DummyItem?) {

    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            RETCODE_SDCARD_PERM_FOR_SLIMEX,
            RETCODE_SDCARD_PERM_FOR_FULLEX,
            RETCODE_SDCARD_PERM_FOR_MERGEIP,
            RETCODE_SDCARD_PERM_FOR_REPLIP,
            RETCODE_SDCARD_PERM_FOR_REPLDB
            -> onRequestSDCardAccessPermissionsResult(requestCode, permissions, grantResults)
            else -> {}
        }
    }

    private fun onRequestSDCardAccessPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            }
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            when(requestCode){
                RETCODE_SDCARD_PERM_FOR_SLIMEX -> exportDBDialog(0)
                RETCODE_SDCARD_PERM_FOR_FULLEX -> exportDBDialog(1)
                RETCODE_SDCARD_PERM_FOR_MERGEIP -> importDBDialog(0)
                RETCODE_SDCARD_PERM_FOR_REPLIP -> importDBDialog(1)
                RETCODE_SDCARD_PERM_FOR_REPLDB -> importDBDialog(2)
            }
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermAndExportDB(mode: Int) { // 0: Slim 1: Whole
        val writeDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) false
            else
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val mediaLocDenied =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        val notificationDenied =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (writeDenied || readDenied || mediaLocDenied || notificationDenied){
            val retcode =
                    if(mode == 0) RETCODE_SDCARD_PERM_FOR_SLIMEX
                    else RETCODE_SDCARD_PERM_FOR_FULLEX
            ActivityCompat.requestPermissions(this, PERMISSIONS, retcode)
            return
        }

        exportDBDialog(mode)
    }

    fun checkPermAndImportDB(mode: Int) { // 0: merge, 1: replace, 2: replace by .db
        val writeDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) false
            else
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val mediaLocDenied =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
                }else{
                    false
                }
        val notificationDenied =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        if (writeDenied || readDenied || mediaLocDenied || notificationDenied){
            val retcode =
                    when(mode){
                        0 -> RETCODE_SDCARD_PERM_FOR_MERGEIP
                        1 -> RETCODE_SDCARD_PERM_FOR_REPLIP
                        else -> RETCODE_SDCARD_PERM_FOR_REPLIP
                    }
            ActivityCompat.requestPermissions(this, PERMISSIONS, retcode)
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
        val reqcode =
                if(mode==0) REQCODE_BACKUP_DIR_CHOSEN_FOR_SLIMEX
                else        REQCODE_BACKUP_DIR_CHOSEN_FOR_FULLEX
        startActivityForResult(chooserIntent, reqcode)
    }

    private fun importDBDialogSinceN(mode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if(mode == 2){
            intent.type = "*/*" //本当はSQLite3のMIME Typeを登録したいが、うまくいかない
        }else {
            intent.type = "application/zip"
        }
        val reqcode =
                when(mode){
                    0 -> REQCODE_ZIPFILE_CHOSEN_FOR_MERGEIP
                    1 -> REQCODE_ZIPFILE_CHOSEN_FOR_REPLIP
                    else -> REQCODE_DBFILE_CHOSEN_FOR_REPLDB
                }
        startActivityForResult(intent, reqcode)
    }

    private fun importDBDialog(mode: Int) {
        importDBDialogSinceN(mode)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        val frag: androidx.fragment.app.Fragment = currentFragment
        when (requestCode) {
            RETCODE_OPEN_RELEASE_NOTES -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val uri = Uri.parse(RELEASE_NOTES_URL)
                val i = Intent(Intent.ACTION_VIEW, uri)
                startActivity(i)
            }
            RETCODE_EXPORT_DB -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val mode = data.getIntExtra("which", 0)
                if (mode < 2)
                    checkPermAndExportDB(mode)
                else {
                    val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
            RETCODE_BACKUP_PROGRESS -> if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                mExportViewModel?.cancelExport()
            }
            RETCODE_IMPORT_DB -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val mode = data.getIntExtra("which", 0)
                if (mode < 2)
                    checkPermAndImportDB(mode)
                else {
                    val uri = Uri.parse("https://pentax.tnose.net/trisquel-for-android/import_export/")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
            RETCODE_IMPORT_PROGRESS -> if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                mImportViewModel?.cancelExport()
            }
            RETCODE_DELETE_FILMROLL -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                if (id != -1) {
                    if (frag is FilmRollFragment) frag.deleteFilmRoll(id)
                }
            }
            RETCODE_DELETE_CAMERA -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                if (id != -1 && frag is CameraFragment) {
                    frag.deleteCamera(id)
                }
            }
            RETCODE_DELETE_LENS -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                if (id != -1 && frag is LensFragment) {
                    frag.deleteLens(id)
                }
            }
            RETCODE_DELETE_ACCESSORY -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                if (id != -1 && frag is AccessoryFragment) {
                    frag.deleteAccessory(id)
                }
            }
            RETCODE_CAMERA_TYPE -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val which = data.getIntExtra("which", 0)
                val intent = Intent(application, EditCameraActivity::class.java)
                intent.putExtra("type", which)
                startActivityForResult(intent, REQCODE_ADD_CAMERA)
            }
            RETCODE_SORT -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val which = data.getIntExtra("which", 0)

                val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val key = when (frag) {
                    is FilmRollFragment -> "filmroll_sortkey"
                    is CameraFragment -> "camera_sortkey"
                    is LensFragment -> "lens_sortkey"
                    is AccessoryFragment -> "accessory_sortkey"
                    else -> ""
                }
                val e = pref.edit()
                e.putInt(key, which)
                e.apply()

                when (frag) {
                    is FilmRollFragment -> frag.changeSortKey(which)
                    is CameraFragment -> frag.changeSortKey(which)
                    is LensFragment -> frag.changeSortKey(which)
                    is AccessoryFragment -> frag.changeSortKey(which)
                }
            }
            RETCODE_FILTER_CAMERA -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (currentFragment is FilmRollFragment) {
                    val cameraid = data.getIntExtra("which_id", -1)
                    if (cameraid != -1) {
                        (currentFragment as FilmRollFragment).currentFilter = Pair(1, arrayListOf(cameraid.toString()))
                        val dao = TrisquelDao(this)
                        dao.connection()
                        val c = dao.getCamera(cameraid)
                        dao.close()
                        if (c != null) {
                            supportActionBar?.subtitle = "📷 " + c.manufacturer + " " + c.modelName
                        }
                    }
                }
            }
            RETCODE_FILTER_FILM_BRAND -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (currentFragment is FilmRollFragment) {
                    val which = data.getIntExtra("which", -1)
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val brands = dao.availableFilmBrandList
                    dao.close()
                    if (which != -1) {
                        (currentFragment as FilmRollFragment).currentFilter =
                                Pair(2, arrayListOf(brands[which].first, brands[which].second))
                        supportActionBar?.subtitle = "🎞 " + brands[which].second
                    }
                }
            }
            RETCODE_SEARCH -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val tags = data.getStringArrayListExtra("checked_labels")
                if (tags!!.size > 0) {
                    val intent = Intent(application, SearchActivity::class.java)
                    intent.putExtra("tags", tags)
                    startActivityForResult(intent, REQCODE_SEARCH)
                }
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
