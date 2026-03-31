package net.tnose.app.trisquel

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy

class SearchActivity : AppCompatActivity() {
    companion object {
        const val REQCODE_SELECT_THUMBNAIL = 103
    }

    private lateinit var searchViewModel: SearchViewModel
    private val mainViewModel: MainViewModel by viewModels()
    private var thumbnailEditingPhoto: Photo? = null
    private var isDirty: Boolean = false
    private val dirtyFilmRolls: ArrayList<Int> = arrayListOf()

    private val PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

    private val editPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras ?: return@registerForActivityResult
            val p = BundleCompat.getParcelable(bundle, "photo", Photo::class.java)
            val tags: ArrayList<String>? = bundle.getStringArrayList("tags")
            if(p != null){
                searchViewModel.tagPhoto(p.id, p.filmrollid, tags ?: arrayListOf())
                searchViewModel.update(p.toEntity())
                if (!dirtyFilmRolls.contains(p.filmrollid)) dirtyFilmRolls.add(p.filmrollid)
                isDirty = true
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            editThumbPhoto()
        } else {
            thumbnailEditingPhoto = null
            Toast.makeText(this, getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        val tags = intent.getStringArrayListExtra("tags") ?: arrayListOf()
        searchViewModel.searchTags.value = tags.toList()

        if (savedInstanceState != null) {
            val tbid = savedInstanceState.getInt("thumbnail_editing_id", -1)
            if (tbid != -1) {
                val dao = TrisquelDao(applicationContext)
                dao.connection()
                thumbnailEditingPhoto = dao.getPhoto(tbid)
                dao.close()
            }
            isDirty = savedInstanceState.getBoolean("is_dirty", false)
            savedInstanceState.getIntegerArrayList("dirty_filmrolls")?.let {
                dirtyFilmRolls.clear()
                dirtyFilmRolls.addAll(it)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        })

        setContent {
            MaterialTheme {
                SearchScreen(tags.joinToString(", "))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("thumbnail_editing_id", thumbnailEditingPhoto?.id ?: -1)
        outState.putBoolean("is_dirty", isDirty)
        outState.putIntegerArrayList("dirty_filmrolls", dirtyFilmRolls)
    }

    private fun goBack() {
        val data = Intent()
        data.putExtra("dirtyFilmRolls", dirtyFilmRolls)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun editThumbPhoto() {
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

    private fun checkPermAndEditThumbPhoto() {
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        else ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        val cameraDenied = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (readDenied || cameraDenied) {
            requestPermissionLauncher.launch(PERMISSIONS)
            return
        }
        editThumbPhoto()
    }

    fun onPhotoInteraction(item: Photo, isLong: Boolean) {
        if (isLong) {
            mainViewModel.showDialog(ActiveDialog.Confirm(
                message = getString(R.string.msg_confirm_remove_item).format(getString(R.string.this_photo)),
                onConfirm = {
                    if (!dirtyFilmRolls.contains(item.filmrollid)) dirtyFilmRolls.add(item.filmrollid)
                    searchViewModel.delete(item.id)
                    isDirty = true
                }
            ))
        } else {
            val intent = Intent(application, EditPhotoActivity::class.java)
            intent.putExtra("filmroll", item.filmrollid)
            intent.putExtra("id", item.id)
            intent.putExtra("frameIndex", item.frameIndex)
            editPhotoLauncher.launch(intent)
        }
    }

    fun onThumbnailClick(item: Photo) {
        if (item.supplementalImages.isEmpty()) {
            thumbnailEditingPhoto = item
            checkPermAndEditThumbPhoto()
        } else {
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

    fun onFavoriteClick(item: Photo) {
        item.favorite = !item.favorite
        searchViewModel.update(item.toEntity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            if (requestCode == REQCODE_SELECT_THUMBNAIL) thumbnailEditingPhoto = null
            return
        }

        if (requestCode == REQCODE_SELECT_THUMBNAIL) {
            val uris = Matisse.obtainResult(data)
            val p = thumbnailEditingPhoto
            if (uris != null && uris.isNotEmpty() && p != null) {
                p.supplementalImages.add(uris[0].toString())
                searchViewModel.update(p.toEntity())
                thumbnailEditingPhoto = null
                isDirty = true
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen(tagString: String) {
        TrisquelDialogManager(
            activeDialog = mainViewModel.activeDialog,
            onDismiss = { mainViewModel.dismissDialog() }
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.action_search))
                            Text(tagString, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            val photos by searchViewModel.photosByAndQuery.observeAsState(emptyList())
            val isLoading by searchViewModel.isLoading.observeAsState(false)
            Column(modifier = Modifier.padding(paddingValues)) {
                SearchListScreen(
                    photos = photos,
                    onItemClick = { onPhotoInteraction(it, false) },
                    onItemLongClick = { onPhotoInteraction(it, true) },
                    onIndexClick = { },
                    onIndexLongClick = { },
                    onThumbnailClick = { onThumbnailClick(it) },
                    onFavoriteClick = { onFavoriteClick(it) },
                    isLoading = isLoading
                )
            }
        }
    }
}
