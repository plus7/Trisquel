package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import net.tnose.app.trisquel.dummy.DummyContent
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CameraFragment.OnListFragmentInteractionListener, LensFragment.OnListFragmentInteractionListener, FilmRollFragment.OnListFragmentInteractionListener, EmptyFragment.OnFragmentInteractionListener, AccessoryFragment.OnListFragmentInteractionListener, AbstractDialogFragment.Callback {
    companion object {
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

        const val RETCODE_CAMERA_TYPE = 300
        const val RETCODE_OPEN_RELEASE_NOTES = 100
        const val RETCODE_DELETE_FILMROLL = 101
        const val RETCODE_DELETE_CAMERA = 102
        const val RETCODE_DELETE_LENS = 103
        const val RETCODE_DELETE_ACCESSORY = 104
        const val RETCODE_BACKUP_DB = 400
        const val RETCODE_SDCARD_PERM = 401

        const val RELEASE_NOTES_URL = "http://pentax.tnose.net/tag/trisquel_releasenotes/"
    }

    private lateinit var currentFragment: Fragment

    internal val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val currentFragmentId = if (savedInstanceState != null) {
            savedInstanceState.getInt("current_fragment")
        } else {
            0
        }

        val f: Fragment
        val transaction = supportFragmentManager.beginTransaction()
        when (currentFragmentId) {
            1 -> {
                currentFragment = CameraFragment()
                setTitle(R.string.title_activity_cam_list)
            }
            2 -> {
                currentFragment = LensFragment()
                setTitle(R.string.title_activity_lens_list)
            }
            3 -> {
                currentFragment = AccessoryFragment()
                setTitle(R.string.title_activity_accessory_list)
            }
            else -> {
                currentFragment = FilmRollFragment()
                setTitle(R.string.title_activity_filmroll_list)
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
                    fragment.arguments.putInt("id", -1) //dummy value
                    fragment.arguments.putStringArray("items", arrayOf(getString(R.string.register_ilc), getString(R.string.register_flc)))
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
                    startActivityForResult(intent, REQCODE_ADD_FILMROLL)
                }
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
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
        if (lastVersion < Util.TRISQUEL_VERSION) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(RETCODE_OPEN_RELEASE_NOTES)
            fragment.arguments.putString("title", "Trisquel")
            if (lastVersion != 0) {
                fragment.arguments.putString("message", getString(R.string.warning_newversion))
            } else {
                fragment.arguments.putString("message", getString(R.string.warning_firstrun))
            }
            fragment.arguments.putString("positive", getString(R.string.show_release_notes))
            fragment.arguments.putString("negative", getString(R.string.close))
            fragment.showOn(this, "dialog")
        }
        val e = pref.edit()
        e.putInt("last_version", Util.TRISQUEL_VERSION)
        e.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_fragment", when(currentFragment){
            is CameraFragment -> 1
            is LensFragment -> 2
            is AccessoryFragment -> 3
            else -> 0
        })
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(application, SettingsActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_release_notes) {
            val uri = Uri.parse(RELEASE_NOTES_URL) // + Integer.toString(Util.TRISQUEL_VERSION)
            val i = Intent(Intent.ACTION_VIEW, uri)
            startActivity(i)
        } else if (id == R.id.action_backup_sqlite) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(RETCODE_BACKUP_DB)
            fragment.arguments.putString("title", getString(R.string.title_backup))
            fragment.arguments.putString("message", getString(R.string.description_backup))
            fragment.arguments.putString("positive", getString(R.string.continue_))
            fragment.showOn(this, "dialog")
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        val frag : Fragment = currentFragment
        when (requestCode) {
            REQCODE_ADD_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = CameraSpec(
                        -1,
                        bundle!!.getInt("type"),
                        bundle.getString("mount")!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("model_name")!!,
                        bundle.getInt("format"),
                        bundle.getInt("ss_grain_size"),
                        bundle.getDouble("fastest_ss"),
                        bundle.getDouble("slowest_ss"),
                        bundle.getInt("bulb_available") != 0,
                        "",
                        bundle.getInt("ev_grain_size"),
                        bundle.getInt("ev_width"))
                if(frag is CameraFragment) frag.insertCamera(c)
                if (c.type == 1) {
                    val l = LensSpec(
                            -1,
                            "",
                            c.id,
                            bundle.getString("manufacturer")!!,
                            bundle.getString("fixedlens_name")!!,
                            bundle.getString("fixedlens_focal_length")!!,
                            bundle.getString("fixedlens_f_steps")!!
                    )
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val id = dao.addLens(l)
                    dao.close()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_EDIT_CAMERA -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val c = CameraSpec(
                        bundle!!.getInt("id"),
                        bundle.getInt("type"),
                        bundle.getString("created")!!,
                        Util.dateToStringUTC(Date()),
                        bundle.getString("mount")!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("model_name")!!,
                        bundle.getInt("format"),
                        bundle.getInt("ss_grain_size"),
                        bundle.getDouble("fastest_ss"),
                        bundle.getDouble("slowest_ss"),
                        bundle.getInt("bulb_available") != 0,
                        "",
                        bundle.getInt("ev_grain_size"),
                        bundle.getInt("ev_width"))
                if(frag is CameraFragment) frag.updateCamera(c)
                if (c.type == 1) {
                    val dao = TrisquelDao(this)
                    dao.connection()
                    val lensid = dao.getFixedLensIdByBody(c.id)
                    val l = LensSpec(
                            lensid,
                            "",
                            c.id,
                            bundle.getString("manufacturer")!!,
                            bundle.getString("fixedlens_name")!!,
                            bundle.getString("fixedlens_focal_length")!!,
                            bundle.getString("fixedlens_f_steps")!!
                    )
                    dao.updateLens(l)
                    dao.close()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_ADD_LENS -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val l = LensSpec(
                        -1,
                        bundle!!.getString("mount")!!,
                        0,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("model_name")!!,
                        bundle.getString("focal_length")!!,
                        bundle.getString("f_steps")!!
                )
                Log.d("new lens", l.toString())
                if(frag is LensFragment) frag.insertLens(l)
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_EDIT_LENS -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val l = LensSpec(
                        bundle!!.getInt("id"),
                        bundle.getString("created")!!,
                        Util.dateToStringUTC(Date()),
                        bundle.getString("mount")!!,
                        0,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("model_name")!!,
                        bundle.getString("focal_length")!!,
                        bundle.getString("f_steps")!!
                )
                //Log.d("new lens", l.toString());
                if(frag is LensFragment) frag.updateLens(l)
            } else if (resultCode == Activity.RESULT_CANCELED) {
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
                    val backupDB = File(sd, "trisquel-" + sdf.format(calendar.time) + ".db")

                    if (dbpath.exists()) {
                        try {
                            val src = FileInputStream(dbpath).channel
                            val dst = FileOutputStream(backupDB).channel
                            dst.transferFrom(src, 0, src.size())
                            src.close()
                            dst.close()
                            // MediaScannerに教えないとすぐにはPCから見えない
                            val contentUri = Uri.fromFile(backupDB)
                            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
                            this.sendBroadcast(mediaScanIntent)
                        } catch (e: FileNotFoundException) {
                            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                            return
                        } catch (e: IOException) {
                            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                            return
                        }

                        Toast.makeText(this, "Wrote to " + backupDB.absolutePath, Toast.LENGTH_LONG).show()
                    }
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
        if (id == R.id.nav_camera) {
            currentFragment = CameraFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, currentFragment)
            transaction.commit()
            setTitle(R.string.title_activity_cam_list)
            fab.setImageResource(R.drawable.ic_menu_camera_white)
        } else if (id == R.id.nav_lens) {
            currentFragment = LensFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, currentFragment)
            transaction.commit()
            setTitle(R.string.title_activity_lens_list)
            fab.setImageResource(R.drawable.ic_lens_white)
        } else if (id == R.id.nav_filmrolls) {
            currentFragment = FilmRollFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, currentFragment)
            transaction.commit()
            setTitle(R.string.title_activity_filmroll_list)
            fab.setImageResource(R.drawable.ic_filmroll_vector_white)
        } else if (id == R.id.nav_accessory) {
            currentFragment = AccessoryFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, currentFragment)
            transaction.commit()
            setTitle(R.string.title_activity_accessory_list)
            fab.setImageResource(R.drawable.ic_extension_white)
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(camera: CameraSpec, isLong: Boolean) {
        if (isLong) {
            val dao = TrisquelDao(applicationContext)
            dao.connection()
            val usedCount = dao.getCameraUsageCount(camera.id)
            dao.close()
            //TODO: i18n
            if (usedCount > 0) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments.putString("title", "カメラを削除できません")
                fragment.arguments.putString("message",
                        camera.modelName + "は既存のフィルム記録から参照されているため、削除することができません。")
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_CAMERA)
                fragment.arguments.putString("title", "カメラの削除")
                fragment.arguments.putString("message", camera.modelName + "を削除しますか？この操作は元に戻せません！")
                fragment.arguments.putInt("id", camera.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditCameraActivity::class.java)
            Log.d("camera", "type=" + Integer.toString(camera.type))
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
            //TODO: i18n
            if (usedCount > 0) {
                val fragment = AlertDialogFragment.Builder().build(99)
                fragment.arguments.putString("title", "レンズを削除できません")
                fragment.arguments.putString("message",
                        lens.modelName + "は既存の写真記録から参照されているため、削除することができません。")
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_LENS)
                fragment.arguments.putString("title", "レンズの削除")
                fragment.arguments.putString("message", lens.modelName + "を削除しますか？この操作は元に戻せません！")
                fragment.arguments.putInt("id", lens.id)
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
            //TODO: i18n
            fragment.arguments.putString("title", "フィルムの削除")
            fragment.arguments.putString("message", filmRoll.name + "を削除しますか？この操作は元に戻せません！")
            fragment.arguments.putInt("id", filmRoll.id)
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
                //TODO: i18n
                fragment.arguments.putString("title", "アクセサリを削除できません")
                fragment.arguments.putString("message",
                        accessory.name + "は既存のフィルム記録から参照されているため、削除することができません。")
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(RETCODE_DELETE_ACCESSORY)
                //TODO: i18n
                fragment.arguments.putString("title", "アクセサリの削除")
                fragment.arguments.putString("message", accessory.name + "を削除しますか？この操作は元に戻せません！")
                fragment.arguments.putInt("id", accessory.id)
                fragment.showOn(this, "dialog")
            }
        } else {
            val intent = Intent(application, EditAccessoryActivity::class.java)
            intent.putExtra("id", accessory.id)
            startActivityForResult(intent, REQCODE_EDIT_ACCESSORY)
        }
    }

    fun onListFragmentInteraction(item: DummyContent.DummyItem) {

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
        val frag: Fragment = currentFragment
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
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}