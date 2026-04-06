package net.tnose.app.trisquel

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.focus.onFocusChanged
import androidx.core.os.BundleCompat
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import java.util.regex.Pattern

class EditFilmRollViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)

    val idInput: Int = savedStateHandle.get<Int>("id") ?: -1
    val id = if (idInput < 0) 0 else idInput
    val defaultCameraId = savedStateHandle.get<Int>("default_camera") ?: -1
    val defaultManufacturer = savedStateHandle.get<String>("default_manufacturer") ?: ""
    val defaultBrand = savedStateHandle.get<String>("default_brand") ?: ""

    private val _uiState = MutableStateFlow(EditFilmRollUiState())
    val uiState: StateFlow<EditFilmRollUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _cameras = MutableStateFlow<List<CameraSpec>>(emptyList())
    val cameras: StateFlow<List<CameraSpec>> = _cameras.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            reloadCamerasInternal()
            
            if (id > 0) {
                val fEntity = repo.getFilmRollRaw(id)
                if (fEntity != null) {
                    val initIso = if (fEntity.iso.isNotEmpty() && fEntity.iso != "0") fEntity.iso else ""
                    _uiState.update { it.copy(
                        created = fEntity.created,
                        name = fEntity.name,
                        cameraId = fEntity.camera ?: -1,
                        manufacturer = fEntity.manufacturer,
                        brand = fEntity.brand,
                        iso = initIso,
                        isLoaded = true
                    ) }
                } else {
                    _uiState.update { it.copy(isLoaded = true) }
                }
            } else {
                _uiState.update { it.copy(
                    cameraId = defaultCameraId,
                    manufacturer = defaultManufacturer,
                    brand = defaultBrand,
                    isLoaded = true
                ) }
            }
        }
    }

    private suspend fun reloadCamerasInternal() {
        val entities = repo.getAllCamerasRaw()
        _cameras.value = entities.map { CameraSpec.fromEntity(it) }
    }

    // New camera may have been added, reload
    fun reloadCameras() {
        viewModelScope.launch(Dispatchers.IO) {
            reloadCamerasInternal()
        }
    }

    fun save(name: String, cameraId: Int, manufacturer: String, brand: String, isoStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = if (_uiState.value.created.isNotEmpty()) _uiState.value.created else Util.dateToStringUTC(Date())
            
            val cameraEntity = repo.getCamera(cameraId) ?: return@launch
            
            val f = FilmRollEntity(
                id = id,
                name = name,
                created = created,
                lastModified = Util.dateToStringUTC(Date()),
                camera = cameraId,
                format = cameraEntity.format?.toString() ?: "0",
                manufacturer = manufacturer,
                brand = brand,
                iso = isoStr.ifEmpty { "0" }
            )

            repo.upsertFilmRoll(f)
            _isSaved.value = true
        }
    }
}

data class EditFilmRollUiState(
    val isLoaded: Boolean = false,
    val created: String = "",
    val name: String = "",
    val cameraId: Int = -1,
    val manufacturer: String = "",
    val brand: String = "",
    val iso: String = ""
)

@Composable
fun EditFilmRollRoute(
    id: Int,
    onCancel: () -> Unit,
    onNavigateToEditCamera: () -> Unit,
    viewModel: EditFilmRollViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val cameras by viewModel.cameras.collectAsState()
    
    LaunchedEffect(isSaved) {
        if (isSaved) {
            onCancel() // go back
        }
    }

    // Call reloadCameras when returning to this screen just in case a camera was added
    LaunchedEffect(Unit) {
        viewModel.reloadCameras()
    }

    if (!uiState.isLoaded) {
        return
    }

    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    
    val titleRes = if (id <= 0) R.string.title_activity_reg_filmroll else R.string.title_activity_edit_filmroll

    EditFilmRollScreen(
        title = stringResource(id = titleRes),
        initName = uiState.name,
        initCameraId = uiState.cameraId,
        initManufacturer = uiState.manufacturer,
        initBrand = uiState.brand,
        initIso = uiState.iso,
        cameras = cameras,
        suggestedManufacturers = userPreferencesRepository.getSuggestList("film_manufacturer", R.array.film_manufacturer),
        onBrandSuggestionsRequested = { manufacturer ->
            userPreferencesRepository.getSuggestListSub("film_brand", manufacturer)
        },
        onSave = { name, cameraId, manufacturer, brand, isoStr ->
            userPreferencesRepository.saveSuggestList("film_manufacturer", R.array.film_manufacturer, arrayOf(manufacturer))
            userPreferencesRepository.saveSuggestListSub("film_brand", manufacturer, brand)
            viewModel.save(name, cameraId, manufacturer, brand, isoStr)
        },
        onCancel = onCancel,
        onAddCamera = onNavigateToEditCamera
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFilmRollScreen(
    title: String,
    initName: String,
    initCameraId: Int,
    initManufacturer: String,
    initBrand: String,
    initIso: String,
    cameras: List<CameraSpec>,
    suggestedManufacturers: List<String>,
    onBrandSuggestionsRequested: (String) -> List<String>,
    onSave: (String, Int, String, String, String) -> Unit,
    onCancel: () -> Unit,
    onAddCamera: () -> Unit
) {

    var name by rememberSaveable { mutableStateOf(initName) }
    var cameraId by rememberSaveable { mutableIntStateOf(initCameraId) }
    var manufacturer by rememberSaveable { mutableStateOf(initManufacturer) }
    var brand by rememberSaveable { mutableStateOf(initBrand) }
    var iso by rememberSaveable { mutableStateOf(initIso) }
    var isDirty by rememberSaveable { mutableStateOf(false) }

    // If there is exactly one camera, select it by default.
    // Ensure this only happens once, or when list changes and cameraId is still invalid
    LaunchedEffect(cameras.size) {
        if (cameras.size == 1 && cameraId <= 0) {
            cameraId = cameras[0].id
        }
    }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showAskCreateCameraDialog by rememberSaveable { mutableStateOf(false) }

    val canSave = name.isNotEmpty() && cameraId > 0

    val onBackPressed = {
        if (!isDirty) {
            onCancel()
        } else {
            if (canSave) showSaveDialog = true else showDiscardDialog = true
        }
    }

    BackHandler(onBack = onBackPressed)

    if (showSaveDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onSave(name, cameraId, manufacturer, brand, iso)
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.msg_continue_editing_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showAskCreateCameraDialog) {
        AlertDialog(
            onDismissRequest = { showAskCreateCameraDialog = false },
            title = null,
            text = { Text(stringResource(R.string.msg_ask_create_camera)) },
            confirmButton = {
                TextButton(onClick = {
                    showAskCreateCameraDialog = false
                    onAddCamera()
                }) {
                    Text(stringResource(android.R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskCreateCameraDialog = false }) {
                    Text(stringResource(android.R.string.no))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSave(name, cameraId, manufacturer, brand, iso) },
                        enabled = canSave
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Name
            ClassicTextField(
                value = name,
                onValueChange = { name = it; isDirty = true },
                label = stringResource(R.string.label_name),
                modifier = Modifier.fillMaxWidth()
            )

            // Camera
            var expandedCamera by rememberSaveable { mutableStateOf(false) }
            val selectedCameraName = cameras.find { it.id == cameraId }?.let { "${it.manufacturer} ${it.modelName}" } ?: ""
            
            ExposedDropdownMenuBox(
                expanded = expandedCamera,
                onExpandedChange = { 
                    if (cameras.isEmpty()) {
                        showAskCreateCameraDialog = true
                    } else {
                        expandedCamera = it
                    }
                }
            ) {
                ClassicTextField(
                    value = selectedCameraName,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_camera),
                    isError = cameras.isEmpty(),
                    supportingText = if (cameras.isEmpty()) { { Text(stringResource(R.string.error_nocamera)) } } else null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCamera) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (cameras.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedCamera,
                        onDismissRequest = { expandedCamera = false }
                    ) {
                        cameras.forEach { camera ->
                            DropdownMenuItem(
                                text = { Text("${camera.manufacturer} ${camera.modelName}") },
                                onClick = {
                                    cameraId = camera.id
                                    isDirty = true
                                    expandedCamera = false
                                }
                            )
                        }
                    }
                }
            }

            // Manufacturer
            var expandedManufacturer by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedManufacturer,
                onExpandedChange = { expandedManufacturer = it }
            ) {
                ClassicTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it; isDirty = true; expandedManufacturer = true },
                    label = stringResource(R.string.label_manufacturer),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedManufacturer) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredManufacturers = suggestedManufacturers.filter { it.contains(manufacturer, ignoreCase = true) }
                if (filteredManufacturers.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedManufacturer,
                        onDismissRequest = { expandedManufacturer = false }
                    ) {
                        filteredManufacturers.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    manufacturer = suggestion
                                    isDirty = true
                                    expandedManufacturer = false
                                }
                            )
                        }
                    }
                }
            }

            // Brand
            var expandedBrand by rememberSaveable { mutableStateOf(false) }
            val suggestedBrands = remember(manufacturer) { onBrandSuggestionsRequested(manufacturer) }
            ExposedDropdownMenuBox(
                expanded = expandedBrand,
                onExpandedChange = { expandedBrand = it }
            ) {
                ClassicTextField(
                    value = brand,
                    onValueChange = {
                        brand = it
                        isDirty = true
                        expandedBrand = true
                    },
                    label = stringResource(R.string.label_brand),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                // Extract ISO from brand when losing focus
                                val zoom = Pattern.compile(".*?(\\d++).*")
                                val m = zoom.matcher(brand)
                                if (m.find() && iso.isEmpty()) {
                                    iso = m.group(1) ?: ""
                                }
                            }
                        }
                )
                val filteredBrands = suggestedBrands.filter { it.contains(brand, ignoreCase = true) }
                if (filteredBrands.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedBrand,
                        onDismissRequest = { expandedBrand = false }
                    ) {
                        filteredBrands.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    brand = suggestion
                                    isDirty = true
                                    expandedBrand = false
                                    
                                    val zoom = Pattern.compile(".*?(\\d++).*")
                                    val m = zoom.matcher(suggestion)
                                    if (m.find() && iso.isEmpty()) {
                                        iso = m.group(1) ?: ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ISO
            ClassicTextField(
                value = iso,
                onValueChange = { iso = it; isDirty = true },
                label = stringResource(R.string.label_iso),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
