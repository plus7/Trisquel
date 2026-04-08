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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = null,
                label = { Text(label) },
                leadingIcon = null,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                shape = TextFieldDefaults.shape,
                singleLine = true,
                enabled = true,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 8.dp)
            )
        }
    )
}

class EditAccessoryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo: TrisquelRepo = TrisquelRepo(application)

    val idInput: Int = savedStateHandle.get<Int>("id") ?: -1
    val id = if (idInput < 0) 0 else idInput

    private val _uiState = MutableStateFlow(EditAccessoryUiState(id = id))
    val uiState: StateFlow<EditAccessoryUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    init {
        if (id > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                val entity = repo.getAccessory(id)
                if (entity != null) {
                    val a = Accessory.fromEntity(entity)
                    val flFactorStr = if (a.type == Accessory.ACCESSORY_TELE_CONVERTER || a.type == Accessory.ACCESSORY_WIDE_CONVERTER) {
                        a.focal_length_factor.toString()
                    } else ""
                    val mount = if (a.type != Accessory.ACCESSORY_FILTER && a.type != Accessory.ACCESSORY_UNKNOWN) a.mount else ""
                    
                    _uiState.update { it.copy(
                        created = Util.dateToStringUTC(a.created),
                        type = a.type,
                        name = a.name,
                        mount = mount,
                        flFactor = flFactorStr,
                        isLoaded = true
                    ) }
                } else {
                    _uiState.update { it.copy(isLoaded = true) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoaded = true) }
        }
    }

    fun save(type: Int, name: String, mount: String, flFactor: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val created = if (state.created.isNotEmpty()) state.created else Util.dateToStringUTC(Date())
            val updated = Util.dateToStringUTC(Date())
            
            val m = if (type != Accessory.ACCESSORY_FILTER && type != Accessory.ACCESSORY_UNKNOWN) mount else null
            val f = if (type == Accessory.ACCESSORY_TELE_CONVERTER || type == Accessory.ACCESSORY_WIDE_CONVERTER) flFactor else 0.0
            
            val a = Accessory(
                id = state.id,
                created = created,
                last_modified = updated,
                type = type,
                name = name,
                mount = m,
                focal_length_factor = f
            )
            repo.upsertAccessory(a.toEntity())
            
            _isSaved.value = true
        }
    }
}

data class EditAccessoryUiState(
    val id: Int = 0,
    val isLoaded: Boolean = false,
    val created: String = "",
    val type: Int = Accessory.ACCESSORY_FILTER,
    val name: String = "",
    val mount: String = "",
    val flFactor: String = "",
    val isDirty: Boolean = false
)

@Composable
fun EditAccessoryRoute(
    id: Int,
    onCancel: () -> Unit,
    viewModel: EditAccessoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    
    LaunchedEffect(isSaved) {
        if (isSaved) {
            onCancel() // go back
        }
    }

    if (!uiState.isLoaded) {
        // Loading state, empty box
        return
    }

    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    val suggestedMounts = remember { userPreferencesRepository.getSuggestList("camera_mounts", R.array.camera_mounts) }

    val titleRes = if (id < 0) R.string.title_activity_add_accessory else R.string.title_activity_edit_accessory
    
    EditAccessoryScreen(
        title = stringResource(id = titleRes),
        initType = uiState.type,
        initName = uiState.name,
        initMount = uiState.mount,
        initFlFactor = uiState.flFactor,
        suggestedMounts = suggestedMounts,
        onSave = { type, name, mount, flFactor ->
            if (type == Accessory.ACCESSORY_TELE_CONVERTER || type == Accessory.ACCESSORY_WIDE_CONVERTER || type == Accessory.ACCESSORY_EXT_TUBE) {
                userPreferencesRepository.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(mount))
            }
            viewModel.save(type, name, mount, flFactor)
        },
        onCancel = onCancel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccessoryScreen(
    title: String,
    initType: Int,
    initName: String,
    initMount: String,
    initFlFactor: String,
    suggestedMounts: List<String>,
    onSave: (type: Int, name: String, mount: String, flFactor: Double) -> Unit,
    onCancel: () -> Unit
) {
    var type by rememberSaveable { mutableIntStateOf(initType) }
    var name by rememberSaveable { mutableStateOf(initName) }
    var mount by rememberSaveable { mutableStateOf(initMount) }
    var flFactorStr by rememberSaveable { mutableStateOf(initFlFactor) }
    var isDirty by rememberSaveable { mutableStateOf(false) }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val flFactor = Util.safeStr2Dobule(flFactorStr)

    val typeOptions = listOf(
        stringResource(id = R.string.label_accessory_filter) to Accessory.ACCESSORY_FILTER,
        stringResource(id = R.string.label_accessory_tc) to Accessory.ACCESSORY_TELE_CONVERTER,
        stringResource(id = R.string.label_accessory_wc) to Accessory.ACCESSORY_WIDE_CONVERTER,
        stringResource(id = R.string.label_accessory_ext_tube) to Accessory.ACCESSORY_EXT_TUBE,
        stringResource(id = R.string.label_accessory_unknown) to Accessory.ACCESSORY_UNKNOWN
    )
    val selectedTypeIndex = typeOptions.indexOfFirst { it.second == type }.takeIf { it >= 0 } ?: 4
    
    val canSave = remember(type, name, mount, flFactorStr) {
        val baseCond = name.isNotEmpty()
        val mountIsNotEmpty = mount.isNotEmpty()
        val additionalCond = when (type) {
            Accessory.ACCESSORY_FILTER, Accessory.ACCESSORY_UNKNOWN -> true
            Accessory.ACCESSORY_TELE_CONVERTER -> mountIsNotEmpty && flFactor > 1.0
            Accessory.ACCESSORY_WIDE_CONVERTER -> mountIsNotEmpty && flFactor < 1.0 && flFactor > 0.0
            Accessory.ACCESSORY_EXT_TUBE -> mountIsNotEmpty
            else -> false
        }
        baseCond && additionalCond
    }

    val flFactorError = remember(type, flFactorStr) {
        if (flFactorStr.isEmpty()) return@remember null
        val f = Util.safeStr2Dobule(flFactorStr)
        when (type) {
            Accessory.ACCESSORY_TELE_CONVERTER -> if (f <= 1.0) R.string.error_flfactor_toosmall else null
            Accessory.ACCESSORY_WIDE_CONVERTER -> if (f >= 1.0 || f == 0.0) R.string.error_flfactor_toobig else null
            else -> null
        }
    }

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
            onDismissRequest = {  },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {
                    onSave(type, name, mount, flFactor)
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
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
            onDismissRequest = {  },
            title = { Text(stringResource(R.string.msg_continue_editing_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {  }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(stringResource(R.string.discard))
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
                        onClick = { onSave(type, name, mount, flFactor) },
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
            var expandedType by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = it }
            ) {
                ClassicTextField(
                    value = typeOptions[selectedTypeIndex].first,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.label_accessory_type),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    typeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.first) },
                            onClick = {
                                type = option.second
                                isDirty = true
                                expandedType = false
                            }
                        )
                    }
                }
            }

            ClassicTextField(
                value = name,
                onValueChange = { name = it; isDirty = true },
                label = stringResource(R.string.label_name),
                modifier = Modifier.fillMaxWidth()
            )

            if (type == Accessory.ACCESSORY_TELE_CONVERTER || type == Accessory.ACCESSORY_WIDE_CONVERTER || type == Accessory.ACCESSORY_EXT_TUBE) {
                var expandedMount by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMount,
                    onExpandedChange = { expandedMount = it }
                ) {
                    ClassicTextField(
                        value = mount,
                        onValueChange = { mount = it; isDirty = true; expandedMount = true },
                        label = stringResource(R.string.label_mount),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMount) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    val filteredMounts = suggestedMounts.filter { it.contains(mount, ignoreCase = true) }
                    if (filteredMounts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedMount,
                            onDismissRequest = { expandedMount = false }
                        ) {
                            filteredMounts.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        mount = suggestion
                                        isDirty = true
                                        expandedMount = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (type == Accessory.ACCESSORY_TELE_CONVERTER || type == Accessory.ACCESSORY_WIDE_CONVERTER) {
                ClassicTextField(
                    value = flFactorStr,
                    onValueChange = { flFactorStr = it; isDirty = true },
                    label = stringResource(R.string.label_fl_factor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = flFactorError != null,
                    supportingText = flFactorError?.let {
                        { Text(stringResource(it)) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
