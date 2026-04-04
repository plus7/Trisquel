package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import net.tnose.app.trisquel.ui.theme.TrisquelTheme
import org.json.JSONArray
import org.json.JSONException

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

class EditAccessoryActivity : AppCompatActivity() {
    private var id: Int = -1
    private var created: String = ""
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        val dao = TrisquelDao(applicationContext)
        dao.connection()
        val data = intent
        id = data.getIntExtra("id", -1)
        
        var initType = Accessory.ACCESSORY_FILTER
        var initName = ""
        var initMount = ""
        var initFlFactor = ""

        if (id > 0 && savedInstanceState == null) {
            val a = dao.getAccessory(id)
            if (a != null) {
                created = Util.dateToStringUTC(a.created)
                initName = a.name
                initType = a.type
                when (a.type) {
                    Accessory.ACCESSORY_TELE_CONVERTER,
                    Accessory.ACCESSORY_WIDE_CONVERTER -> {
                        initMount = a.mount
                        initFlFactor = a.focal_length_factor.toString()
                    }
                    Accessory.ACCESSORY_EXT_TUBE -> {
                        initMount = a.mount
                    }
                }
            }
        } else if (savedInstanceState != null) {
            created = savedInstanceState.getString("created") ?: ""
            initName = savedInstanceState.getString("name") ?: ""
            initType = savedInstanceState.getInt("type")
            when (initType) {
                Accessory.ACCESSORY_TELE_CONVERTER,
                Accessory.ACCESSORY_WIDE_CONVERTER -> {
                    initMount = savedInstanceState.getString("mount") ?: ""
                    initFlFactor = savedInstanceState.getDouble("focal_length_factor").toString()
                }
                Accessory.ACCESSORY_EXT_TUBE -> {
                    initMount = savedInstanceState.getString("mount") ?: ""
                }
            }
        }
        dao.close()

        val titleRes = if (id < 0) R.string.title_activity_add_accessory else R.string.title_activity_edit_accessory
        
        setContent {
            TrisquelTheme {
                EditAccessoryScreen(
                    title = stringResource(id = titleRes),
                    initType = initType,
                    initName = initName,
                    initMount = initMount,
                    initFlFactor = initFlFactor,
                    suggestedMounts = userPreferencesRepository.getSuggestList("camera_mounts", R.array.camera_mounts),
                    onSave = { type, name, mount, flFactor ->
                        saveAndFinish(type, name, mount, flFactor)
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED, Intent())
                        finish()
                    }
                )
            }
        }
    }

    private fun saveAndFinish(type: Int, name: String, mount: String, flFactor: Double) {
        val data = Intent()
        data.putExtra("id", id)
        data.putExtra("type", type)
        data.putExtra("created", created)
        data.putExtra("name", name)
        
        when (type) {
            Accessory.ACCESSORY_TELE_CONVERTER,
            Accessory.ACCESSORY_WIDE_CONVERTER -> {
                data.putExtra("mount", mount)
                data.putExtra("focal_length_factor", flFactor)
                userPreferencesRepository.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(mount))
            }
            Accessory.ACCESSORY_EXT_TUBE -> {
                data.putExtra("mount", mount)
                userPreferencesRepository.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(mount))
            }
        }
        setResult(RESULT_OK, data)
        finish()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("created", created)
    }
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
            // ダイアログの角をシャープにして昔の見た目に近づける
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.msg_save_or_discard_data)) },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onSave(type, name, mount, flFactor)
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
