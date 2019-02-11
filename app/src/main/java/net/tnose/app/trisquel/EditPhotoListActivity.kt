package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import java.util.*

class EditPhotoListActivity : AppCompatActivity(), PhotoFragment.OnListFragmentInteractionListener, AbstractDialogFragment.Callback {
    internal val REQCODE_ADD_PHOTO = 100
    internal val REQCODE_EDIT_PHOTO = 101
    internal val REQCODE_EDIT_FILMROLL = 102
    internal val REQCODE_SELECT_THUMBNAIL = 103
    internal val REQCODE_EDIT_PHOTOINDEX = 104
    internal val REQCODE_INDEX_SHIFT = 105
    internal val RETCODE_SDCARD_PERM_IMGPICKER = 106

    private val PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    private var toolbar: Toolbar? = null
    private var mFilmRoll: FilmRoll? = null
    private var thumbnailEditingPhoto: Photo? = null
    private var photo_fragment: PhotoFragment? = null

    private val filmRollText: String
        get() {
            val sb = StringBuilder()
            sb.append(mFilmRoll!!.name + "\n")
            if (mFilmRoll!!.manufacturer.length > 0) sb.append(getString(R.string.label_manufacturer) + ": " + mFilmRoll!!.manufacturer + "\n")
            if (mFilmRoll!!.brand.length > 0) sb.append(getString(R.string.label_brand) + ": " + mFilmRoll!!.brand + "\n")
            if (mFilmRoll!!.iso > 0) sb.append(getString(R.string.label_iso) + ": ")
            sb.append(mFilmRoll!!.iso)
            sb.append('\n')
            val c = mFilmRoll!!.camera
            sb.append(getString(R.string.label_camera) + ": " + c.manufacturer + " " + c.modelName + "\n")

            val dao = TrisquelDao(this)
            dao.connection()
            val ps = dao.getPhotosByFilmRollId(mFilmRoll!!.id)
            for (i in ps.indices) {
                val p = ps[i]
                val l = dao.getLens(p.lensid)
                sb.append("------[No. " + (p.frameIndex + 1) + "]------\n")
                sb.append(getString(R.string.label_date) + ": " + p.date + "\n")
                sb.append(getString(R.string.label_lens_name) + ": " + l!!.manufacturer + " " + l.modelName + "\n")
                if (p.aperture > 0) sb.append(getString(R.string.label_aperture) + ": " + p.aperture + "\n")
                if (p.shutterSpeed > 0) sb.append(getString(R.string.label_shutter_speed) + ": " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "\n")
                if (p.expCompensation != 0.0) sb.append(getString(R.string.label_exposure_compensation) + ": " + p.expCompensation + "\n")
                if (p.ttlLightMeter != 0.0) sb.append(getString(R.string.label_ttl_light_meter) + ": " + p.ttlLightMeter + "\n")
                if (p.location.length > 0) sb.append(getString(R.string.label_location) + ": " + p.location + "\n")
                if (p.latitude != 999.0 && p.longitude != 999.0) sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(p.latitude) + ", " + java.lang.Double.toString(p.longitude) + "\n")
                if (p.memo.length > 0) sb.append(getString(R.string.label_memo) + ": " + p.memo + "\n")
                if (p.accessories.size > 0) {
                    sb.append(getString(R.string.label_accessories) + ": ")
                    var first = true
                    for (a in p.accessories) {
                        if (!first) sb.append(", ")
                        sb.append(dao.getAccessory(a)!!.name)
                        first = false
                    }
                    sb.append("\n")
                }
            }
            dao.close()
            return sb.toString()
        }

    fun setTitles(f: FilmRoll){
        title = if (f.name.isEmpty()) {
            getString(R.string.empty_name)
        } else {
            f.name
        }
        toolbar?.subtitle = f.camera.manufacturer + " " + f.camera.modelName + " / " +
                f.manufacturer + " " + f.brand
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_photo_list)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val data = intent
        val id = data.getIntExtra("id", -1)

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        mFilmRoll = dao.getFilmRoll(id)
        if(savedInstanceState != null){
            val tbid = savedInstanceState.getInt("thumbnail_editing_id")
            if(tbid != -1){
                val p = dao.getPhoto(tbid)
                assert(p?.filmrollid == id)
                thumbnailEditingPhoto = p
            }else{
                thumbnailEditingPhoto = null
            }
        }
        dao.close()

        setTitles(mFilmRoll!!)

        photo_fragment = PhotoFragment.newInstance(1, id)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, photo_fragment!!)
        transaction.commit()

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(application, TabbedActivity::class.java)
            intent.putExtra("filmroll", mFilmRoll!!.id)
            startActivityForResult(intent, REQCODE_ADD_PHOTO)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("thumbnail_editing_id", thumbnailEditingPhoto?.id ?: -1)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_filmroll, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val data: Intent
        when (item.itemId) {
            android.R.id.home -> {
                data = Intent()
                data.putExtra("filmroll", this.mFilmRoll!!.id)
                setResult(Activity.RESULT_OK, data)
                finish()
                return true
            }
            R.id.menu_edit_film -> {
                var intent = Intent(application, EditFilmRollActivity::class.java)
                intent.putExtra("id", mFilmRoll!!.id)
                startActivityForResult(intent, REQCODE_EDIT_FILMROLL)
                return true
            }
            R.id.menu_copy -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip = ClipData.newPlainText("", filmRollText)
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_export_pdf -> {
                intent = Intent(application, PrintPreviewActivity::class.java)
                intent.putExtra("filmroll", mFilmRoll!!.id)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        val data = Intent()
        data.putExtra("filmroll", this.mFilmRoll!!.id)
        setResult(Activity.RESULT_OK, data)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data == null){
            thumbnailEditingPhoto = null
            return
        }

        val bundle = data.extras
        val tags: ArrayList<String>? = bundle?.getStringArrayList("tags")
        when (requestCode) {
            REQCODE_ADD_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val p: Photo? = bundle!!.getParcelable("photo")
                if(p != null) photo_fragment!!.insertPhoto(p, tags)
            }
            REQCODE_EDIT_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val p: Photo? = bundle!!.getParcelable("photo")
                if(p != null) photo_fragment!!.updatePhoto(p, tags)
            }
            REQCODE_EDIT_FILMROLL -> if (resultCode == Activity.RESULT_OK) {
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val c = dao.getCamera(bundle!!.getInt("camera"))
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

                setTitles(f)

                dao.updateFilmRoll(f)
                dao.close()
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
            REQCODE_SELECT_THUMBNAIL -> if (resultCode == RESULT_OK) {
                val paths = Matisse.obtainPathResult(data)
                val p = thumbnailEditingPhoto
                if(paths.size > 0 && p != null){
                    p.supplementalImages.add(paths[0])
                    photo_fragment!!.updatePhoto(p, null)
                    thumbnailEditingPhoto = null
                }
            }
        }
    }

    override fun onListFragmentInteraction(item: Photo, isLong: Boolean) {
        if (isLong) {
            val fragment = SelectDialogFragment.Builder()
                    .build(200)
            fragment.arguments?.putInt("id", item.id)
            fragment.arguments?.putStringArray("items", arrayOf(getString(R.string.delete), getString(R.string.add_photo_same_index)))
            fragment.showOn(this, "dialog")
        } else {
            val intent = Intent(application, TabbedActivity::class.java)
            intent.putExtra("filmroll", mFilmRoll!!.id)
            intent.putExtra("id", item.id)
            intent.putExtra("frameIndex", item.frameIndex)
            startActivityForResult(intent, REQCODE_EDIT_PHOTO)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            RETCODE_SDCARD_PERM_IMGPICKER -> {
                onRequestSDCardAccessPermissionsResult(permissions, grantResults, requestCode)
            }
        }
    }

    internal fun onRequestSDCardAccessPermissionsResult(permissions: Array<String>, grantResults: IntArray, requestCode: Int) {
        val granted = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)
        if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted)) {
            when(requestCode){
                RETCODE_SDCARD_PERM_IMGPICKER -> {
                    editThumbPhoto()
                }
            }
        } else {
            thumbnailEditingPhoto = null
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    fun editThumbPhoto(){
        Matisse.from(this)
                .choose(MimeType.ofImage())
                .captureStrategy(CaptureStrategy(true, "net.tnose.app.trisquel.provider", "Camera"))
                .capture(true)
                .countable(true)
                .maxSelectable(1)
                .thumbnailScale(0.85f)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .imageEngine(Glide4Engine())
                .forResult(REQCODE_SELECT_THUMBNAIL)
    }

    fun checkPermAndEditThumbPhoto(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, RETCODE_SDCARD_PERM_IMGPICKER)
            return
        }
        editThumbPhoto()
    }

    override fun onThumbnailClick(item: Photo) {
        if(item.supplementalImages.size == 0) {
            thumbnailEditingPhoto = item
            checkPermAndEditThumbPhoto()
        }else {

            val dao = TrisquelDao(this)
            dao.connection()
            val ps = dao.getPhotosByFilmRollId(mFilmRoll!!.id)
            dao.close()

            val intent = Intent(application, GalleryActivity::class.java)
            intent.putExtra("photo", item)
            intent.putParcelableArrayListExtra("favList", ps)
            startActivity(intent)

            /*val file = File(item.supplementalImages[0])
            // android.os.FileUriExposedException回避
            val photoURI = FileProvider.getUriForFile(this@EditPhotoListActivity, this@EditPhotoListActivity.applicationContext.packageName + ".provider", file)
            intent.action = android.content.Intent.ACTION_VIEW
            intent.setDataAndType(photoURI, "image/ *")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)*/
        }
    }

    override fun onFavoriteClick(item: Photo) {
        item.favorite = !item.favorite
        photo_fragment!!.toggleFavPhoto(item)
    }

    override fun onIndexClick(item: Photo) {
        val fragment = SimpleInputDialogFragment.Builder()
                .build(REQCODE_EDIT_PHOTOINDEX)
        fragment.arguments?.putInt("id", item.id)
        fragment.arguments?.putString("title", getString(R.string.title_dialog_edit_index))
        fragment.arguments?.putString("default_value", (item.frameIndex + 1).toString())
        fragment.showOn(this, "dialog")
    }

    override fun onIndexLongClick(item: Photo) {
        val fragment = SimpleInputDialogFragment.Builder()
                .build(REQCODE_INDEX_SHIFT)
        fragment.arguments?.putInt("id", item.id)
        fragment.arguments?.putString("title", getString(R.string.title_dialog_shift_index))
        fragment.arguments?.putString("message", getString(R.string.msg_dialog_shift_index))
        val downshiftLimit = photo_fragment?.possibleDownShiftLimit(item) ?: 0
        fragment.arguments?.putString("hint", getString(R.string.hint_dialog_shift_index).format(downshiftLimit+1))
        fragment.arguments?.putString("default_value", (item.frameIndex + 1).toString())
        fragment.showOn(this, "dialog")
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            200 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (data != null) {
                    val which: Int
                    val id: Int
                    val index: Int
                    which = data.getIntExtra("which", -1)
                    id = data.getIntExtra("id", -1)
                    val dao = TrisquelDao(this.applicationContext)
                    dao.connection()
                    val p = dao.getPhoto(id)
                    dao.close()
                    index = p!!.frameIndex
                    when (which) {
                        0 -> if (id != -1) photo_fragment!!.deletePhoto(id)
                        1 -> {
                            val intent = Intent(application, TabbedActivity::class.java)
                            intent.putExtra("filmroll", mFilmRoll!!.id)
                            intent.putExtra("frameIndex", index)
                            startActivityForResult(intent, REQCODE_ADD_PHOTO)
                        }
                    }
                }
            }
            REQCODE_EDIT_PHOTOINDEX -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val newindex = data.getStringExtra("value").toInt() - 1
                if(newindex < 0) return
                val id = data.getIntExtra("id", -1)
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val p = dao.getPhoto(id)
                dao.close()
                if(p != null) {
                    if(p.frameIndex == newindex) return
                    p.frameIndex = newindex
                    photo_fragment!!.updatePhoto(p, null)
                }
            }
            REQCODE_INDEX_SHIFT -> if (resultCode == DialogInterface.BUTTON_POSITIVE){
                val newindex = data.getStringExtra("value").toInt() - 1

                val id = data.getIntExtra("id", -1)
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val p = dao.getPhoto(id)
                dao.close()

                if(p == null) return

                val downshiftLimit = photo_fragment!!.possibleDownShiftLimit(p)
                if(newindex < downshiftLimit) return
                if(p.frameIndex == newindex) return

                photo_fragment!!.shiftFrameIndexFrom(p, newindex - p.frameIndex)
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
