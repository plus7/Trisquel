package net.tnose.app.trisquel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import java.util.Date

class EditPhotoListActivity : AppCompatActivity(), PhotoFragment.OnListFragmentInteractionListener {
    internal val REQCODE_ADD_PHOTO = 100
    internal val REQCODE_EDIT_PHOTO = 101
    internal val REQCODE_EDIT_FILMROLL = 102
    internal val REQCODE_SELECT_THUMBNAIL = 103

    private val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
        }

    var mFilmRoll by mutableStateOf<FilmRoll?>(null)
    private var thumbnailEditingPhoto: Photo? = null
    var photo_fragment: PhotoFragment? = null
    private var mFilmRollViewModel: FilmRollViewModel? = null

    // Compose state variables
    var showOpDialog by mutableStateOf<Photo?>(null)
    var showIndexDialog by mutableStateOf<Photo?>(null)
    var showShiftDialog by mutableStateOf<Photo?>(null)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            editThumbPhoto()
        } else {
            thumbnailEditingPhoto = null
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    val filmRollText: String
        get() {
            val sb = StringBuilder()
            val fr = mFilmRoll ?: return ""
            sb.append(fr.name + "\n")
            if (fr.manufacturer.isNotEmpty()) sb.append(getString(R.string.label_manufacturer) + ": " + fr.manufacturer + "\n")
            if (fr.brand.isNotEmpty()) sb.append(getString(R.string.label_brand) + ": " + fr.brand + "\n")
            if (fr.iso > 0) sb.append(getString(R.string.label_iso) + ": ")
            sb.append(fr.iso)
            sb.append('\n')
            val c = fr.camera
            sb.append(getString(R.string.label_camera) + ": " + c.manufacturer + " " + c.modelName + "\n")

            val dao = TrisquelDao(this)
            dao.connection()
            val ps = dao.getPhotosByFilmRollId(fr.id)
            for (i in ps.indices) {
                val p = ps[i]
                val l = dao.getLens(p.lensid)
                sb.append("------[No. " + (p.frameIndex + 1) + "]------\n")
                sb.append(getString(R.string.label_date) + ": " + p.date + "\n")
                if (l != null) {
                    sb.append(getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n")
                }
                if (p.aperture > 0) sb.append(getString(R.string.label_aperture) + ": " + p.aperture + "\n")
                if (p.shutterSpeed > 0) sb.append(getString(R.string.label_shutter_speed) + ": " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "\n")
                if (p.expCompensation != 0.0) sb.append(getString(R.string.label_exposure_compensation) + ": " + p.expCompensation + "\n")
                if (p.ttlLightMeter != 0.0) sb.append(getString(R.string.label_ttl_light_meter) + ": " + p.ttlLightMeter + "\n")
                if (p.location.isNotEmpty()) sb.append(getString(R.string.label_location) + ": " + p.location + "\n")
                if (p.latitude != 999.0 && p.longitude != 999.0) sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(p.latitude) + ", " + java.lang.Double.toString(p.longitude) + "\n")
                if (p.memo.isNotEmpty()) sb.append(getString(R.string.label_memo) + ": " + p.memo + "\n")
                if (p.accessories.isNotEmpty()) {
                    sb.append(getString(R.string.label_accessories) + ": ")
                    var first = true
                    for (a in p.accessories) {
                        if (!first) sb.append(", ")
                        val acc = dao.getAccessory(a)
                        if (acc != null) sb.append(acc.name)
                        first = false
                    }
                    sb.append("\n")
                }
            }
            dao.close()
            return sb.toString()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent
        val id = data.getIntExtra("id", 0)

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

        mFilmRollViewModel = ViewModelProvider(this).get(FilmRollViewModel::class.java)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val resData = Intent()
                resData.putExtra("filmroll", mFilmRoll!!.id)
                setResult(RESULT_OK, resData)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        setContent {
            TrisquelTheme {
                EditPhotoListScreen(
                    filmRoll = mFilmRoll,
                    id = id,
                    onBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("thumbnail_editing_id", thumbnailEditingPhoto?.id ?: -1)
        super.onSaveInstanceState(outState)
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
            REQCODE_ADD_PHOTO -> if (resultCode == RESULT_OK) {
                val p: Photo? = bundle!!.getParcelable("photo")
                if(p != null) photo_fragment?.insertPhoto(p, tags)
            }
            REQCODE_EDIT_PHOTO -> if (resultCode == RESULT_OK) {
                val p: Photo? = bundle!!.getParcelable("photo")
                if(p != null) photo_fragment?.updatePhoto(p, tags)
            }
            REQCODE_EDIT_FILMROLL -> if (resultCode == RESULT_OK) {
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
                mFilmRoll = f
                mFilmRollViewModel!!.update(f.toEntity())
            }
            REQCODE_SELECT_THUMBNAIL -> if (resultCode == RESULT_OK) {
                val uris = Matisse.obtainResult(data)
                val p = thumbnailEditingPhoto
                if(uris.size > 0 && p != null){
                    p.supplementalImages.add(uris[0].toString())
                    photo_fragment?.updatePhoto(p, null)
                    thumbnailEditingPhoto = null
                }
            }
        }
    }

    override fun onListFragmentInteraction(item: Photo, isLong: Boolean) {
        if (isLong) {
            showOpDialog = item
        } else {
            val intent = Intent(application, EditPhotoActivity::class.java)
            intent.putExtra("filmroll", mFilmRoll!!.id)
            intent.putExtra("id", item.id)
            intent.putExtra("frameIndex", item.frameIndex)
            startActivityForResult(intent, REQCODE_EDIT_PHOTO)
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
        val readDenied =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                 ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
            else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val cameraDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (readDenied || cameraDenied) {
            requestPermissionLauncher.launch(PERMISSIONS)
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
        }
    }

    override fun onFavoriteClick(item: Photo) {
        item.favorite = !item.favorite
        photo_fragment?.toggleFavPhoto(item)
    }

    override fun onIndexClick(item: Photo) {
        showIndexDialog = item
    }

    override fun onIndexLongClick(item: Photo) {
        showShiftDialog = item
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoListScreen(
    filmRoll: FilmRoll?,
    id: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current as EditPhotoListActivity

    val titleText = if (filmRoll == null || filmRoll.name.isEmpty()) {
        stringResource(R.string.empty_name)
    } else {
        filmRoll.name
    }
    
    val subtitleText = filmRoll?.let {
        "${it.camera.manufacturer} ${it.camera.modelName} / ${it.manufacturer} ${it.brand}"
    } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.foundation.layout.Column {
                        Text(titleText, style = MaterialTheme.typography.titleLarge)
                        if (subtitleText.isNotEmpty()) {
                            Text(subtitleText, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_film)) },
                            onClick = {
                                menuExpanded = false
                                val intent = Intent(context, EditFilmRollActivity::class.java)
                                intent.putExtra("id", filmRoll?.id ?: 0)
                                context.startActivityForResult(intent, context.REQCODE_EDIT_FILMROLL)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_copy_to_clipboard)) },
                            onClick = {
                                menuExpanded = false
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("", context.filmRollText))
                                Toast.makeText(context, context.getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_print)) },
                            onClick = {
                                menuExpanded = false
                                val intent = Intent(context, PrintPreviewActivity::class.java)
                                intent.putExtra("filmroll", filmRoll?.id ?: 0)
                                context.startActivity(intent)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, EditPhotoActivity::class.java)
                    intent.putExtra("filmroll", filmRoll?.id ?: 0)
                    context.startActivityForResult(intent, context.REQCODE_ADD_PHOTO)
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    this.id = R.id.container
                }
            },
            update = { view ->
                if (context.photo_fragment == null) {
                    context.photo_fragment = PhotoFragment.newInstance(1, id)
                    context.supportFragmentManager.beginTransaction()
                        .replace(view.id, context.photo_fragment!!)
                        .commit()
                }
            }
        )
    }

    if (context.showOpDialog != null) {
        val photo = context.showOpDialog!!
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { context.showOpDialog = null },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    val options = listOf(
                        stringResource(R.string.delete),
                        stringResource(R.string.add_photo_same_index)
                    )
                    options.forEachIndexed { index, option ->
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.showOpDialog = null
                                    val dao = TrisquelDao(context)
                                    dao.connection()
                                    val p = dao.getPhoto(photo.id)
                                    dao.close()
                                    if (p != null) {
                                        when (index) {
                                            0 -> context.photo_fragment?.deletePhoto(photo.id)
                                            1 -> {
                                                val intent = Intent(context, EditPhotoActivity::class.java)
                                                intent.putExtra("filmroll", context.mFilmRoll!!.id)
                                                intent.putExtra("frameIndex", p.frameIndex)
                                                context.startActivityForResult(intent, context.REQCODE_ADD_PHOTO)
                                            }
                                        }
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (context.showIndexDialog != null) {
        val photo = context.showIndexDialog!!
        var input by remember { mutableStateOf((photo.frameIndex + 1).toString()) }
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { context.showIndexDialog = null },
            title = { Text(stringResource(R.string.title_dialog_edit_index)) },
            text = {
                ClassicTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.showIndexDialog = null
                    val newindex = (input.toIntOrNull() ?: 1) - 1
                    if (newindex >= 0) {
                        val dao = TrisquelDao(context)
                        dao.connection()
                        val p = dao.getPhoto(photo.id)
                        dao.close()
                        if (p != null && p.frameIndex != newindex) {
                            p.frameIndex = newindex
                            context.photo_fragment?.updatePhoto(p, null)
                        }
                    }
                }) {
                    Text(stringResource(android.R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { context.showIndexDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (context.showShiftDialog != null) {
        val photo = context.showShiftDialog!!
        var input by remember { mutableStateOf((photo.frameIndex + 1).toString()) }
        val downshiftLimit = context.photo_fragment?.possibleDownShiftLimit(photo) ?: 0
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { context.showShiftDialog = null },
            title = { Text(stringResource(R.string.title_dialog_shift_index)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(stringResource(R.string.msg_dialog_shift_index))
                    ClassicTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(R.string.hint_dialog_shift_index).format(downshiftLimit + 1),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.showShiftDialog = null
                    val newindex = (input.toIntOrNull() ?: 1) - 1
                    val dao = TrisquelDao(context)
                    dao.connection()
                    val p = dao.getPhoto(photo.id)
                    dao.close()
                    if (p != null) {
                        val limit = context.photo_fragment?.possibleDownShiftLimit(p) ?: 0
                        if (newindex >= limit && p.frameIndex != newindex) {
                            context.photo_fragment?.shiftFrameIndexFrom(p, newindex - p.frameIndex)
                        }
                    }
                }) {
                    Text(stringResource(android.R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { context.showShiftDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
