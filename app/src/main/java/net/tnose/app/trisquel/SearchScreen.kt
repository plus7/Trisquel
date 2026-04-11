package net.tnose.app.trisquel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.CaptureStrategy

@Composable
fun SearchRoute(
    tags: List<String>,
    onBack: () -> Unit,
    onNavigateToEditPhoto: (Int, Int, Int) -> Unit,
    onNavigateToGallery: (Photo, List<Photo>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val photos by viewModel.photosByAndQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var thumbnailEditingPhoto by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(tags) {
        viewModel.searchTags.value = tags
    }

    class PickImageUriContract : ActivityResultContract<Any, List<Uri>>() {
        override fun createIntent(context: Context, input: Any): Intent {
            return Matisse.from(context as Activity)
                .choose(MimeType.ofImage())
                .captureStrategy(CaptureStrategy(true, "net.tnose.app.trisquel.provider", "Camera"))
                .capture(true)
                .countable(true)
                .maxSelectable(1)
                .thumbnailScale(0.85f)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .imageEngine(Glide4Engine())
                .createIntent()!!
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
            return if (resultCode == Activity.RESULT_OK) Matisse.obtainResult(intent) else emptyList()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(PickImageUriContract()) { uris ->
        if (uris.isNotEmpty()) {
            thumbnailEditingPhoto?.let { p ->
                p.supplementalImages.add(uris[0].toString())
                viewModel.update(p.toEntity())
            }
        }
        thumbnailEditingPhoto = null
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.all { it.value }) {
            pickImageLauncher.launch(42)
        } else {
            thumbnailEditingPhoto = null
            Toast.makeText(context, context.getString(R.string.error_permission_denied_sdcard), Toast.LENGTH_LONG).show()
        }
    }

    val checkPermAndEditThumbPhoto = { photo: Photo ->
        thumbnailEditingPhoto = photo
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }
        val readDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        }
        val cameraDenied = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED

        if (readDenied || cameraDenied) {
            requestPermissionLauncher.launch(permissions)
        } else {
            pickImageLauncher.launch(42)
        }
    }

    SearchScreen(
        tagString = tags.joinToString(", "),
        photos = photos,
        isLoading = isLoading,
        onBack = onBack,
        onItemClick = { photo ->
            onNavigateToEditPhoto(photo.filmrollid, photo.id, photo.frameIndex)
        },
        onItemLongClick = { photo ->
            mainViewModel.showDialog(ActiveDialog.Confirm(
                message = context.getString(R.string.msg_confirm_remove_item).format(context.getString(R.string.this_photo)),
                onConfirm = {
                    viewModel.delete(photo.id)
                }
            ))
        },
        onThumbnailClick = { photo ->
            if (photo.supplementalImages.isEmpty()) {
                checkPermAndEditThumbPhoto(photo)
            } else {
                val allPhotos = photos.map { Photo.fromEntity(it.second.photo) }
                onNavigateToGallery(photo, allPhotos)
            }
        },
        onFavoriteClick = { photo ->
            photo.favorite = !photo.favorite
            viewModel.update(photo.toEntity())
        },
        mainViewModel = mainViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    tagString: String,
    photos: List<Pair<Pair<String, Int>, PhotoAndRels>>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onItemClick: (Photo) -> Unit,
    onItemLongClick: (Photo) -> Unit,
    onThumbnailClick: (Photo) -> Unit,
    onFavoriteClick: (Photo) -> Unit,
    mainViewModel: MainViewModel
) {
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
                    IconButton(onClick = onBack) {
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
        Column(modifier = Modifier.padding(paddingValues)) {
            SearchListScreen(
                photos = photos,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onIndexClick = { },
                onIndexLongClick = { },
                onThumbnailClick = onThumbnailClick,
                onFavoriteClick = onFavoriteClick,
                isLoading = isLoading
            )
        }
    }
}
