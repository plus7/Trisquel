package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

sealed class EditLensEvent {
    object SaveSuccess : EditLensEvent()
}

data class EditLensUiState(
    val id: Int = -1,
    val isLoaded: Boolean = false,
    val created: String = "",
    val manufacturer: String = "",
    val mount: String = "",
    val model: String = "",
    val focalLength: String = "",
    val fSteps: Set<Double> = emptySet(),
    val suggestedManufacturers: List<String> = emptyList(),
    val suggestedMounts: List<String> = emptyList(),
    val isDirty: Boolean = false
)

class EditLensViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val dao = TrisquelDao(application)
    private val userPrefs = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(EditLensUiState())
    val uiState: StateFlow<EditLensUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditLensEvent>()
    val events = _events.asSharedFlow()

    init {
        val id = savedStateHandle.get<Int>("id") ?: -1
        _uiState.update { it.copy(id = id) }
        loadInitialData(id)
    }

    private fun loadInitialData(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        dao.connection()
        val suggestedManufacturers = userPrefs.getSuggestList("lens_manufacturer", R.array.lens_manufacturer)
        val suggestedMounts = userPrefs.getSuggestList("camera_mounts", R.array.camera_mounts)

        if (id >= 0) {
            val l = dao.getLens(id)
            if (l != null) {
                _uiState.update { it.copy(
                    isLoaded = true,
                    created = Util.dateToStringUTC(l.created),
                    manufacturer = l.manufacturer,
                    mount = l.mount,
                    model = l.modelName,
                    focalLength = l.focalLength,
                    fSteps = l.fSteps.toSet(),
                    suggestedManufacturers = suggestedManufacturers,
                    suggestedMounts = suggestedMounts
                ) }
            } else {
                _uiState.update { it.copy(isLoaded = true) }
            }
        } else {
            _uiState.update { it.copy(
                isLoaded = true,
                suggestedManufacturers = suggestedManufacturers,
                suggestedMounts = suggestedMounts
            ) }
        }
        dao.close()
    }

    fun onManufacturerChange(value: String) {
        _uiState.update { it.copy(manufacturer = value, isDirty = true) }
    }

    fun onMountChange(value: String) {
        _uiState.update { it.copy(mount = value, isDirty = true) }
    }

    fun onModelChange(value: String) {
        _uiState.update { it.copy(model = value, isDirty = true) }
    }

    fun onFocalLengthChange(value: String) {
        _uiState.update { it.copy(focalLength = value, isDirty = true) }
    }

    fun onFStepsChange(value: Set<Double>) {
        _uiState.update { it.copy(fSteps = value, isDirty = true) }
    }

    fun save(mFArray: List<Double>) = viewModelScope.launch(Dispatchers.IO) {
        val state = _uiState.value
        val fStepsString = mFArray.filter { state.fSteps.contains(it) }.joinToString(", ")
        
        val l = LensSpec(
            state.id,
            if (state.created.isNotEmpty()) state.created else Util.dateToStringUTC(Date()),
            Util.dateToStringUTC(Date()),
            state.mount, 0,
            state.manufacturer,
            state.model,
            state.focalLength,
            fStepsString
        )

        dao.connection()
        if (state.id >= 0) {
            dao.updateLens(l)
        } else {
            dao.addLens(l)
        }
        dao.close()

        userPrefs.saveSuggestList("lens_manufacturer", R.array.lens_manufacturer, arrayOf(state.manufacturer))
        userPrefs.saveSuggestList("camera_mounts", R.array.camera_mounts, arrayOf(state.mount))

        _events.emit(EditLensEvent.SaveSuccess)
    }
}
