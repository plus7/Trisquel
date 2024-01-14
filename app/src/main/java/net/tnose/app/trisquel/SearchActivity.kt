package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import java.util.Arrays

// 実装がEditPhotoListActivityなどから丸コピ状態＆微妙に違うところがあるのが気に食わないが、仕方がない
class SearchActivity : AppCompatActivity(), SearchFragment.OnListFragmentInteractionListener, AbstractDialogFragment.Callback {
    internal val REQCODE_EDIT_PHOTO = 101
    internal val REQCODE_SELECT_THUMBNAIL = 103
    internal val REQCODE_EDIT_PHOTOINDEX = 104
    internal val REQCODE_INDEX_SHIFT = 105
    internal val RETCODE_SDCARD_PERM_IMGPICKER = 106
    internal val DIALOG_DELETE_PHOTO = 200

    private val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        }

    private var thumbnailEditingPhoto: Photo? = null
    var fragment: SearchFragment? = null
    var isDirty: Boolean = false
    var dirtyFilmRolls: ArrayList<Int> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tags = intent.getStringArrayListExtra("tags")
        title = getString(R.string.action_search) + ": " + tags!!.joinToString(", ")

        if(savedInstanceState != null){
            val tbid = savedInstanceState.getInt("thumbnail_editing_id")
            if(tbid != -1){
                val dao = TrisquelDao(applicationContext)
                dao.connection()
                val p = dao.getPhoto(tbid)
                thumbnailEditingPhoto = p
                dao.close()
            }else{
                thumbnailEditingPhoto = null
            }
            isDirty = intent.getBooleanExtra("isDirty", false)
        }

        val transaction = supportFragmentManager.beginTransaction()
        val f = SearchFragment.newInstance(tags)
        fragment = f
        transaction.replace(R.id.container, f)
        transaction.commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("thumbnail_editing_id", thumbnailEditingPhoto?.id ?: -1)
        outState.putBoolean("isDirty", isDirty)
        super.onSaveInstanceState(outState)
    }

    override fun onListFragmentInteraction(item: Photo, isLong: Boolean) {
        if (isLong) {
            val fragment = YesNoDialogFragment.Builder()
                    .build(DIALOG_DELETE_PHOTO)
            fragment.arguments?.putString("message", getString(R.string.msg_confirm_remove_item).format(getString(R.string.this_photo)))
            fragment.arguments?.putInt("id", item.id)
            fragment.showOn(this, "dialog")
        } else {
            val intent = Intent(application, EditPhotoActivity::class.java)
            intent.putExtra("filmroll", item.filmrollid)
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
        val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            } else {
                intArrayOf(PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED)
            }
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
        val writeDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) false
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val cameraDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (writeDenied || readDenied || cameraDenied) {
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
            val ps = dao.getPhotosByFilmRollId(item.filmrollid)
            dao.close()

            val intent = Intent(application, GalleryActivity::class.java)
            intent.putExtra("photo", item)
            intent.putParcelableArrayListExtra("favList", ps)
            startActivity(intent)
        }
    }

    override fun onFavoriteClick(item: Photo) {
        item.favorite = !item.favorite
        fragment!!.toggleFavPhoto(item)
    }

    override fun onIndexClick(item: Photo) {
        /*val fragment = SimpleInputDialogFragment.Builder()
                .build(REQCODE_EDIT_PHOTOINDEX)
        fragment.arguments?.putInt("id", item.id)
        fragment.arguments?.putString("title", getString(R.string.title_dialog_edit_index))
        fragment.arguments?.putString("default_value", (item.frameIndex + 1).toString())
        fragment.showOn(this, "dialog")*/
    }

    override fun onIndexLongClick(item: Photo) {
        /*val fragment = SimpleInputDialogFragment.Builder()
                .build(REQCODE_INDEX_SHIFT)
        fragment.arguments?.putInt("id", item.id)
        fragment.arguments?.putString("title", getString(R.string.title_dialog_shift_index))
        fragment.arguments?.putString("message", getString(R.string.msg_dialog_shift_index))
        val downshiftLimit = photo_fragment?.possibleDownShiftLimit(item) ?: 0
        fragment.arguments?.putString("hint", getString(R.string.hint_dialog_shift_index).format(downshiftLimit+1))
        fragment.arguments?.putString("default_value", (item.frameIndex + 1).toString())
        fragment.showOn(this, "dialog")*/
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            DIALOG_DELETE_PHOTO -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val id = data.getIntExtra("id", -1)
                val dao = TrisquelDao(this)
                dao.connection()
                val p = dao.getPhoto(id)
                dao.close()
                if(p != null) {
                    dirtyFilmRolls.add(p.filmrollid)
                    fragment!!.deletePhoto(id)
                    isDirty = true
                }
            }
            REQCODE_EDIT_PHOTOINDEX -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {}
            REQCODE_INDEX_SHIFT -> if (resultCode == DialogInterface.BUTTON_POSITIVE){}
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data == null){
            thumbnailEditingPhoto = null
            return
        }

        val bundle = data.extras
        when (requestCode) {
            REQCODE_EDIT_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val p: Photo? = bundle!!.getParcelable("photo")
                val tags: ArrayList<String>? = bundle.getStringArrayList("tags")
                if(p != null){
                    fragment!!.updatePhoto(p, tags)
                    dirtyFilmRolls.add(p.filmrollid)
                }
            }
            REQCODE_SELECT_THUMBNAIL -> if (resultCode == RESULT_OK) {
                //val paths = Matisse.obtainPathResult(data)
                val uris = Matisse.obtainResult(data)
                val p = thumbnailEditingPhoto
                if(uris.size > 0 && p != null){
                    p.supplementalImages.add(uris[0].toString())
                    fragment!!.updatePhoto(p, null)
                    thumbnailEditingPhoto = null
                    //dirtyFilmRolls.add(p.filmrollid) // 今の所不要
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val data = Intent()
                data.putExtra("dirtyFilmRolls", dirtyFilmRolls)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val data = Intent()
        data.putExtra("dirtyFilmRolls", dirtyFilmRolls)
        setResult(Activity.RESULT_OK, data)
        super.onBackPressed()
    }
}