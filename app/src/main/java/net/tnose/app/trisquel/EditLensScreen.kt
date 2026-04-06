package net.tnose.app.trisquel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.regex.Pattern

private val mFArray = listOf(0.95, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 4.8, 5.0, 5.6, 6.3, 6.7, 7.1, 8.0, 9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0)

@Composable
fun EditLensRoute(
    id: Int,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EditLensViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditLensEvent.SaveSuccess -> onSaveSuccess()
            }
        }
    }

    if (!uiState.isLoaded) return

    EditLensScreen(
        uiState = uiState,
        onManufacturerChange = { viewModel.onManufacturerChange(it) },
        onMountChange = { viewModel.onMountChange(it) },
        onModelChange = { viewModel.onModelChange(it) },
        onFocalLengthChange = { viewModel.onFocalLengthChange(it) },
        onFStepsChange = { viewModel.onFStepsChange(it) },
        onSave = { viewModel.save(mFArray) },
        onCancel = onCancel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLensScreen(
    uiState: EditLensUiState,
    onManufacturerChange: (String) -> Unit,
    onMountChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onFocalLengthChange: (String) -> Unit,
    onFStepsChange: (Set<Double>) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val focalLengthOk = remember(uiState.focalLength) {
        if (uiState.focalLength.isNotEmpty()) {
            val zoom = Pattern.compile("(\\d++)-(\\d++)")
            if (zoom.matcher(uiState.focalLength).find()) return@remember true

            val prime = Pattern.compile("(\\d++)")
            if (prime.matcher(uiState.focalLength).find()) return@remember true
        }
        false
    }

    val canSave = uiState.mount.isNotEmpty() && uiState.model.isNotEmpty() && focalLengthOk

    val onBackPressed = {
        if (!uiState.isDirty) {
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
                    onSave()
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
                title = { 
                    val titleRes = if (uiState.id < 0) R.string.title_activity_reg_lens else R.string.title_activity_edit_lens
                    Text(stringResource(titleRes)) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
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
            // Mount
            var expandedMount by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedMount,
                onExpandedChange = { expandedMount = it }
            ) {
                ClassicTextField(
                    value = uiState.mount,
                    onValueChange = { onMountChange(it); expandedMount = true },
                    label = stringResource(R.string.label_mount),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMount) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredMounts = uiState.suggestedMounts.filter { it.contains(uiState.mount, ignoreCase = true) }
                if (filteredMounts.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedMount,
                        onDismissRequest = { expandedMount = false }
                    ) {
                        filteredMounts.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onMountChange(suggestion)
                                    expandedMount = false
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
                    value = uiState.manufacturer,
                    onValueChange = { onManufacturerChange(it); expandedManufacturer = true },
                    label = stringResource(R.string.label_manufacturer),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedManufacturer) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                val filteredManufacturers = uiState.suggestedManufacturers.filter { it.contains(uiState.manufacturer, ignoreCase = true) }
                if (filteredManufacturers.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedManufacturer,
                        onDismissRequest = { expandedManufacturer = false }
                    ) {
                        filteredManufacturers.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onManufacturerChange(suggestion)
                                    expandedManufacturer = false
                                }
                            )
                        }
                    }
                }
            }

            // Model
            ClassicTextField(
                value = uiState.model,
                onValueChange = { onModelChange(it) },
                label = stringResource(R.string.label_model),
                modifier = Modifier.fillMaxWidth()
            )

            // Focal Length
            ClassicTextField(
                value = uiState.focalLength,
                onValueChange = { onFocalLengthChange(it) },
                label = stringResource(R.string.label_focal_length),
                supportingText = { Text(stringResource(R.string.hint_zoom)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused && uiState.focalLength.isEmpty()) {
                            val zoom = Pattern.compile(".*?(\\d++)-(\\d++)mm.*")
                            val mZoom = zoom.matcher(uiState.model)
                            if (mZoom.find()) {
                                onFocalLengthChange("${mZoom.group(1)}-${mZoom.group(2)}")
                            } else {
                                val prime = Pattern.compile(".*?(\\d++)mm.*")
                                val mPrime = prime.matcher(uiState.model)
                                if (mPrime.find()) {
                                    onFocalLengthChange(mPrime.group(1) ?: "")
                                }
                            }
                        }
                    }
            )

            // F-Stops Grid
            Text(
                text = stringResource(R.string.label_f_stops),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                mFArray.forEach { fValue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(84.dp)
                            .clickable {
                                val newSet = uiState.fSteps.toMutableSet()
                                if (uiState.fSteps.contains(fValue)) newSet.remove(fValue) else newSet.add(fValue)
                                onFStepsChange(newSet)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = uiState.fSteps.contains(fValue),
                            onCheckedChange = null // Managed by the Row's clickable
                        )
                        Text(
                            text = fValue.toString(),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
