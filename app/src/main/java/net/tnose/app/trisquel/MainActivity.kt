package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import net.tnose.app.trisquel.dummy.DummyContent
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        const val REQCODE_BACKUP_DIR_CHOSEN = 10
        const val REQCODE_SEARCH = 11

        const val RETCODE_ALERT = 200
        const val RETCODE_CAMERA_TYPE = 300
        const val RETCODE_OPEN_RELEASE_NOTES = 100
        const val RETCODE_DELETE_FILMROLL = 101
        const val RETCODE_DELETE_CAMERA = 102
        const val RETCODE_DELETE_LENS = 103
        const val RETCODE_DELETE_ACCESSORY = 104
        const val RETCODE_BACKUP_DB = 400
        const val RETCODE_SDCARD_PERM = 401
        const val RETCODE_SORT = 402
        const val RETCODE_FILTER_CAMERA = 403
        const val RETCODE_FILTER_FILM_BRAND = 404
        const val RETCODE_SEARCH = 405
        const val RETCODE_BACKUP_PROGRESS = 406

        const val ACTION_CLOSE_PROGRESS_DIALOG = "ACTION_CLOSE_PROGRESS_DIALOG"
        const val ACTION_UPDATE_PROGRESS_DIALOG = "ACTION_UPDATE_PROGRESS_DIALOG"
        const val RELEASE_NOTES_URL = "http://pentax.tnose.net/tag/trisquel_releasenotes/"
    }

    private var localBroadcastManager: androidx.localbroadcastmanager.content.LocalBroadcastManager? = null
    private var progressFilter: IntentFilter? = null
    private var progressReceiver: ProgressReceiver? = null

    private lateinit var currentFragment: androidx.fragment.app.Fragment
    private val pinnedFilterViewId: ArrayList<Int> = arrayListOf()

    internal val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localBroadcastManager = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(applicationContext)
        progressFilter = IntentFilter()
        progressFilter?.addAction(ExportIntentService.ACTION_EXPORT_PROGRESS)
        progressReceiver = ProgressReceiver(this)
        localBroadcastManager?.registerReceiver(progressReceiver!!, progressFilter!!)

        setContentView(R.layout.activity_main2)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val currentFragmentId = if (savedInstanceState != null) {
            savedInstanceState.getInt("current_fragment")
        } else {
            ID_FILMROLL
        }

        val f: androidx.fragment.app.Fragment
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
                val filtertype = savedInstanceState?.getInt("filmroll_filtertype") ?: 0
                val filtervalue = savedInstanceState?.getStringArrayList("filmroll_filtervalue") ?: arrayListOf("")
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

        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onResume() {
        super.onResume()

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

    override fun onDestroy() {
        localBroadcastManager?.unregisterReceiver(progressReceiver!!)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_fragment", when(currentFragment){
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
                when(currentFragment){
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
            fragment.arguments?.putStringArray("labels", tags.sortedBy{ it.label }.map { it.label }.toTypedArray())
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
                is LensFragment   -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_mount),
                        getString(R.string.label_focal_length))
                is AccessoryFragment   -> arrayOf(
                        getString(R.string.label_created_date),
                        getString(R.string.label_name),
                        getString(R.string.label_accessory_type))
                else -> arrayOf()
            }
            val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val key = when(currentFragment){
                is FilmRollFragment -> pref.getInt("filmroll_sortkey", 0)
                is CameraFragment   -> pref.getInt("camera_sortkey", 0)
                is LensFragment     -> pref.getInt("lens_sortkey", 0)
                is AccessoryFragment-> pref.getInt("accessory_sortkey", 0)
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
            val dao = TrisquelDao(this)
            dao.connection()
            while(filters.size > pinnedFilterViewId.size){
                pinnedFilterViewId.add(View.generateViewId())
            }
            for((i,f) in filters.withIndex()){
                when(f.first){
                    1 -> {
                        val c = dao.getCamera(f.second[0].toInt())
                        if(c != null)
                            popupMenu.menu.add(0, pinnedFilterViewId[i],0, c.manufacturer + " " + c.modelName)
                    }
                    2 -> {
                        popupMenu.menu.add(0, pinnedFilterViewId[i],0,f.second.joinToString(" "))
                    }
                }
            }

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
                            cs.sortBy { it.manufacturer + " "+ it.modelName }
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
                            fragment.arguments?.putStringArray("items", fbs.map { it.first + " " +it.second }.toTypedArray())
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

    fun backupToZip(zipFile: File) {
        val zos = ZipOutputStream(FileOutputStream(zipFile))
        zos.setMethod(ZipOutputStream.DEFLATED)
        val osw = OutputStreamWriter(zos, "UTF-8")
        val dao = TrisquelDao(this)
        dao.connection()

        // Metadata
        val ze = ZipEntry("metadata.json")
        zos.putNextEntry(ze)
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
                val ze = ZipEntry("imgs" + path)
                zos.putNextEntry(ze)
                try {
                    val buf = ByteArray(1024*128)
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

    fun setProgressPercentage(percentage: Double, status: String){
        val intent = Intent()
        if(percentage >= 100.0){
            intent.action = ACTION_CLOSE_PROGRESS_DIALOG
        }else {
            intent.action = ACTION_UPDATE_PROGRESS_DIALOG
            intent.putExtra("percentage", percentage)
        }
        intent.putExtra("status", status)
        sendBroadcast(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data == null) return
        val frag : androidx.fragment.app.Fragment = currentFragment
        when (requestCode) {
            REQCODE_ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = bundle!!.getParcelable<CameraSpec>("cameraspec")
                if(frag is CameraFragment && c != null) frag.insertCamera(c)
                if (c.type == 1) {
                    val l = bundle.getParcelable<LensSpec>("fixed_lens")
                    l!!.body = c.id
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val id = dao.addLens(l)
                    dao.close()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_EDIT_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = bundle!!.getParcelable<CameraSpec>("cameraspec")
                if(frag is CameraFragment && c != null) frag.updateCamera(c)
                if (c.type == 1) {
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val l = bundle.getParcelable<LensSpec>("fixed_lens")
                    val lensid = dao.getFixedLensIdByBody(c.id)
                    l.id = lensid
                    dao.updateLens(l)
                    dao.close()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                val l = data.extras!!.getParcelable<LensSpec>("lensspec")
                if(frag is LensFragment && l != null) frag.insertLens(l)
            }
            REQCODE_EDIT_LENS -> if (resultCode == Activity.RESULT_OK) {
                val l = data.extras!!.getParcelable<LensSpec>("lensspec")
                if(frag is LensFragment && l != null) frag.updateLens(l)
            }
            REQCODE_ADD_FILMROLL -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val c = dao.getCamera(bundle!!.getInt("camera"))
                dao.close()
                val f = FilmRoll(
                        -1,
                        bundle.getString("name")!!,
                        c!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("brand")!!,
                        bundle.getInt("iso"),
                        36
                )
                if(frag is FilmRollFragment) frag.insertFilmRoll(f)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
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
                if(frag is FilmRollFragment) frag.updateFilmRoll(f)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_EDIT_PHOTO_LIST -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                if(frag is FilmRollFragment) frag.refreshFilmRoll(bundle!!.getInt("filmroll"))
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_ADD_ACCESSORY -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val a = Accessory(-1, Util.dateToStringUTC(Date()), Util.dateToStringUTC(Date()),
                        bundle!!.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"),
                        bundle.getDouble("focal_length_factor"))
                if(frag is AccessoryFragment) frag.insertAccessory(a)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_EDIT_ACCESSORY -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val a = Accessory(bundle!!.getInt("id"), bundle.getString("created")!!, Util.dateToStringUTC(Date()),
                        bundle.getInt("type"), bundle.getString("name")!!, bundle.getString("mount"),
                        bundle.getDouble("focal_length_factor"))
                if(frag is AccessoryFragment) frag.updateAccessory(a)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_BACKUP_DIR_CHOSEN -> if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                val bundle = data.extras
                val dir = bundle!!.getString(DirectoryChooserActivity.RESULT_SELECTED_DIR)
                val sd = File(dir!!)
                val dbpath = this.getDatabasePath("trisquel.db")

                if (sd.canWrite()) {
                    val calendar = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyyMMddHHmmss")
                    //val backupDB = File(sd, "trisquel-" + sdf.format(calendar.time) + ".db")
                    val backupZipFileName = "trisquel-" + sdf.format(calendar.time) + ".zip"
                    val backupZip = File(sd, backupZipFileName)

                    if (dbpath.exists()) {
                        val fragment = ProgressDialog.Builder()
                                .build(RETCODE_BACKUP_PROGRESS)
                        fragment.showOn(this, "dialog")
                        ExportIntentService.shouldContinue = true
                        ExportIntentService.startExport(this, dir, backupZipFileName)
                        /*
                        try {
                            backupToZip(backupZip)
                            / *val src = FileInputStream(dbpath).channel
                            val dst = FileOutputStream(backupDB).channel
                            dst.transferFrom(src, 0, src.size())
                            src.close()
                            dst.close()* \/

                            // MediaScannerに教えないとすぐにはPCから見えない
                            val contentUri = Uri.fromFile(backupZip)
                            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
                            this.sendBroadcast(mediaScanIntent)
                        } catch (e: FileNotFoundException) {
                            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                            return
                        } catch (e: IOException) {
                            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                            return
                        }

                        Toast.makeText(this, "Wrote to " + backupZip.absolutePath, Toast.LENGTH_LONG).show()
                        */
                    }
                }
            }
            REQCODE_SEARCH -> {
                val bundle = data.extras
                val dirtyFilmRolls = bundle?.getIntegerArrayList("dirtyFilmRolls") ?: ArrayList()
                if(dirtyFilmRolls.size > 0 && currentFragment is FilmRollFragment){
                    (currentFragment as FilmRollFragment).refreshAll(ArrayList(dirtyFilmRolls.filterNotNull()))
                }
            }
            else -> {
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        var titleRsc: Int = 0
        var fabRsc: Int = 0
        when(id) {
            R.id.nav_settings ->{
                val intent = Intent(application, SettingsActivity::class.java)
                startActivity(intent)
                drawer.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_backup_sqlite -> {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_BACKUP_DB)
                fragment.arguments?.putString("title", getString(R.string.title_backup))
                fragment.arguments?.putString("message", getString(R.string.description_backup))
                fragment.arguments?.putString("positive", getString(R.string.continue_))
                fragment.showOn(this, "dialog")
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

    override fun onListFragmentInteraction(camera: CameraSpec, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val usedCount = dao.getCameraUsageCount(camera.id)
            dao.close()
            if (usedCount > 0) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments?.putString("message", getString(R.string.msg_cannot_remove_item).format(camera.modelName))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_CAMERA)
                fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(camera.modelName))
                fragment.arguments?.putInt("id", camera.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditCameraActivity::class.java)
            intent.putExtra("id", camera.id)
            intent.putExtra("type", camera.type)
            startActivityForResult(intent, REQCODE_EDIT_CAMERA)
        }
    }

    override fun onListFragmentInteraction(lens: LensSpec, isLong: Boolean) {
        if (isLong) {

            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val usedCount = dao.getLensUsageCount(lens.id)
            dao.close()
            if (usedCount > 0) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments?.putString("message", getString(R.string.msg_cannot_remove_item).format(lens.modelName))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_LENS)
                fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(lens.modelName))
                fragment.arguments?.putInt("id", lens.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditLensActivity::class.java)
            intent.putExtra("id", lens.id)
            startActivityForResult(intent, REQCODE_EDIT_LENS)
        }
    }

    override fun onListFragmentInteraction(filmRoll: FilmRoll, isLong: Boolean) {
        if (isLong) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(RETCODE_DELETE_FILMROLL)
            fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(filmRoll.name))
            fragment.arguments?.putInt("id", filmRoll.id)
            fragment.showOn(this, "dialog")
        } else {
            val intent = Intent(application, EditPhotoListActivity::class.java)
            intent.putExtra("id", filmRoll.id)
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

    fun onListFragmentInteraction(item: DummyContent.DummyItem?) {

    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RETCODE_SDCARD_PERM) {
            onRequestSDCardAccessPermissionsResult(permissions, grantResults)
        }
    }

    internal fun onRequestSDCardAccessPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val granted = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            exportDBDialog()
        } else {
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    fun checkPermAndExportDB() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM)
            return
        }
        exportDBDialog()
    }

    fun exportDBDialog() {
        val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)
        Log.d("path", Environment.getExternalStorageDirectory().absolutePath)
        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("Trisquel")
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .initialDirectory(Environment.getExternalStorageDirectory().absolutePath)
                .build()
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)
        startActivityForResult(chooserIntent, REQCODE_BACKUP_DIR_CHOSEN)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        val frag: androidx.fragment.app.Fragment = currentFragment
        when (requestCode) {
            RETCODE_OPEN_RELEASE_NOTES -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val uri = Uri.parse(RELEASE_NOTES_URL)
                val i = Intent(Intent.ACTION_VIEW, uri)
                startActivity(i)
            }
            RETCODE_BACKUP_DB -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                checkPermAndExportDB()
            }
            RETCODE_DELETE_FILMROLL -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                if (id != -1){
                    if(frag is FilmRollFragment) frag.deleteFilmRoll(id)
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
                val key = when(frag){
                    is FilmRollFragment -> "filmroll_sortkey"
                    is CameraFragment -> "camera_sortkey"
                    is LensFragment -> "lens_sortkey"
                    is AccessoryFragment -> "accessory_sortkey"
                    else -> ""
                }
                val e = pref.edit()
                e.putInt(key, which)
                e.apply()

                when(frag){
                    is FilmRollFragment -> frag.changeSortKey(which)
                    is CameraFragment -> frag.changeSortKey(which)
                    is LensFragment -> frag.changeSortKey(which)
                    is AccessoryFragment -> frag.changeSortKey(which)
                }
            }
            RETCODE_FILTER_CAMERA -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if(currentFragment is FilmRollFragment) {
                    val cameraid = data.getIntExtra("which_id", -1)
                    if(cameraid != -1) {
                        (currentFragment as FilmRollFragment).currentFilter = Pair(1, arrayListOf(cameraid.toString()))
                        val dao = TrisquelDao(this)
                        dao.connection()
                        val c = dao.getCamera(cameraid)
                        dao.close()
                        if(c != null) {
                            supportActionBar?.subtitle = "📷 " + c.manufacturer + " " + c.modelName
                        }
                    }
                }
            }
            RETCODE_FILTER_FILM_BRAND -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if(currentFragment is FilmRollFragment) {
                    val which = data.getIntExtra("which", -1)
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val brands = dao.availableFilmBrandList
                    dao.close()
                    if(which != -1) {
                        (currentFragment as FilmRollFragment).currentFilter =
                                Pair(2, arrayListOf(brands[which].first, brands[which].second))
                        supportActionBar?.subtitle = "🎞 " + brands[which].second
                    }
                }
            }
            RETCODE_SEARCH -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val tags = data.getStringArrayListExtra("checked_labels")
                if(tags.size > 0) {
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
