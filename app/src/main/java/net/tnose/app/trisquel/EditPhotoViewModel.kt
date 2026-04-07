package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class EditPhotoEvent {
    data class ShowToast(val message: String) : EditPhotoEvent()
    object SaveSuccess : EditPhotoEvent()
}

data class EditPhotoUiState(
    val id: Int = 0,
    val filmRollId: Int = 0,
    val frameIndex: Int = 0,
    val isLoaded: Boolean = false,
    val date: String = "",
    val lensId: Int = -1,
    val aperture: String = "",
    val shutterSpeed: String = "",
    val focalLengthProgress: Int = 0,
    val expCompProgress: Int = 0,
    val ttlProgress: Int = 0,
    val location: String = "",
    val latitude: Double = 999.0,
    val longitude: Double = 999.0,
    val memo: String = "",
    val favorite: Boolean = false,
    val selectedAccessories: List<Int> = emptyList(),
    val accessoriesStr: String = "",
    val supplementalImages: List<String> = emptyList(),
    val allTags: List<String> = emptyList(),
    val tagCheckedStates: List<Boolean> = emptyList(),
    val lensList: List<LensSpec> = emptyList(),
    val ssList: List<String> = emptyList(),
    val apertureList: List<String> = emptyList(),
    val focalLengthRange: Pair<Double, Double> = Pair(0.0, 0.0),
    val evGrainSize: Int = 3,
    val evWidth: Int = 3,
    val isDirty: Boolean = false
)

class EditPhotoViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)
    private val userPrefs = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(EditPhotoUiState())
    val uiState: StateFlow<EditPhotoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditPhotoEvent>()
    val events = _events.asSharedFlow()

    val filmRoll: LiveData<FilmRoll?> = repo.getFilmRollAndRels(savedStateHandle.get<Int>("filmroll") ?: 0).map { rels ->
        if (rels == null) null else FilmRoll.fromEntity(rels)
    }

    init {
        // Normalize ID: use 0 for new photos to ensure auto-increment works correctly in Room
        val idInput = savedStateHandle.get<Int>("id") ?: 0
        val id = if (idInput < 0) 0 else idInput
        val filmRollId = savedStateHandle.get<Int>("filmroll") ?: 0
        val frameIndex = savedStateHandle.get<Int>("frameIndex") ?: -1

        _uiState.update { it.copy(id = id, filmRollId = filmRollId, frameIndex = frameIndex) }

        loadInitialData()
    }

    private fun loadInitialData() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _uiState.value
        val filmRollEntity = repo.getFilmRollRaw(currentState.filmRollId) ?: return@launch
        val cameraEntity = repo.getCamera(filmRollEntity.camera!!) ?: return@launch
        val camera = CameraSpec.fromEntity(cameraEntity)

        val photoEntity = if (currentState.id > 0) repo.getPhoto(currentState.id) else null

        val evGrainSize = camera.evGrainSize
        val evWidth = camera.evWidth

        val ssList = when (camera.shutterSpeedGrainSize) {
            1 -> getApplication<Application>().resources.getStringArray(R.array.shutter_speeds_one)
            2 -> getApplication<Application>().resources.getStringArray(R.array.shutter_speeds_half)
            3 -> getApplication<Application>().resources.getStringArray(R.array.shutter_speeds_one_third)
            else -> camera.shutterSpeedSteps.map { Util.doubleToStringShutterSpeed(it) }.toTypedArray()
        }.filter { s ->
            val ssval = Util.stringToDoubleShutterSpeed(s)
            ssval <= (camera.slowestShutterSpeed ?: 999.0) && ssval >= (camera.fastestShutterSpeed ?: 0.0)
        }

        var lensId = -1
        var date = ""
        var latitude = 999.0
        var longitude = 999.0
        var accessories: List<Int> = emptyList()
        var aperture = ""
        var shutterSpeed = ""
        var location = ""
        var memo = ""
        var expCompProgress = evWidth * evGrainSize
        var ttlProgress = evWidth * evGrainSize
        var focalLengthProgress = 0
        var favorite = false
        var supplementalImages: List<String> = emptyList()
        var allTags: List<String> = emptyList()
        var tagCheckedStates: List<Boolean> = emptyList()

        if (photoEntity != null) {
            val photo = Photo.fromEntity(photoEntity)
            lensId = photo.lensid
            date = if (photo.date.isNotEmpty()) photo.date else ""
            latitude = photo.latitude
            longitude = photo.longitude
            accessories = photo.accessories
            if (photo.aperture > 0) aperture = Util.doubleToStringShutterSpeed(photo.aperture)
            if (photo.shutterSpeed > 0) shutterSpeed = Util.doubleToStringShutterSpeed(photo.shutterSpeed)
            location = photo.location
            memo = photo.memo
            expCompProgress = ((evWidth + photo.expCompensation) * evGrainSize).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
            ttlProgress = ((evWidth + photo.ttlLightMeter) * evGrainSize).toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
            favorite = photo.favorite
            supplementalImages = photo.supplementalImages

            val tagAndTagMaps = repo.getTagMapAndTagsByPhoto(photo.id)
            val allTagsDb = repo.getAllTagsRaw().sortedBy { it.label }
            allTags = allTagsDb.map { it.label }
            tagCheckedStates = allTagsDb.map { t -> tagAndTagMaps.any { it.tag?.id == t.id } }
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            date = LocalDate.now().format(formatter)

            if (camera.type == 1) {
                lensId = repo.getLensByFixedBody(camera.id)?.id ?: -1
            } else {
                if (userPrefs.isAutocompleteFromPreviousShotEnabled()) {
                    val ps = repo.getPhotosByFilmRollIdRaw(currentState.filmRollId)
                    val pos = ps.indexOfFirst { it.id == currentState.id }
                    lensId = if (pos > 0) {
                        ps[pos - 1].lens ?: 0
                    } else if (ps.isNotEmpty()) {
                        ps.last().lens ?: 0
                    } else {
                        0
                    }
                } else {
                    lensId = 0
                }
            }

            val allTagsDb = repo.getAllTagsRaw().sortedBy { it.label }
            allTags = allTagsDb.map { it.label }
            tagCheckedStates = allTagsDb.map { false }
        }

        val lensEntity = repo.getLens(lensId)
        val lens = if (lensEntity != null) LensSpec.fromEntity(lensEntity) else null
        val focalLengthRange = if (lens != null) Util.getFocalLengthRangeFromStr(lens.focalLength) else Pair(0.0, 0.0)
        if (photoEntity != null && lens != null) {
            focalLengthProgress = (Photo.fromEntity(photoEntity).focalLength - focalLengthRange.first).toInt()
        }

        _uiState.update { state ->
            state.copy(
                isLoaded = true,
                evGrainSize = evGrainSize,
                evWidth = evWidth,
                ssList = ssList,
                lensId = lensId,
                date = date,
                latitude = latitude,
                longitude = longitude,
                selectedAccessories = accessories,
                aperture = aperture,
                shutterSpeed = shutterSpeed,
                location = location,
                memo = memo,
                expCompProgress = expCompProgress,
                ttlProgress = ttlProgress,
                focalLengthProgress = focalLengthProgress,
                focalLengthRange = focalLengthRange,
                favorite = favorite,
                supplementalImages = supplementalImages,
                allTags = allTags,
                tagCheckedStates = tagCheckedStates
            )
        }
        updateLensListInternal(lens, camera)
        refreshApertureListInternal(lens)
        updateAccessoriesStrInternal()
    }

    private fun updateLensListInternal(currentLens: LensSpec?, camera: CameraSpec) = viewModelScope.launch(Dispatchers.IO) {
        val lensList = if (camera.type == 1) {
            val fixedLens = repo.getLensByFixedBody(camera.id)
            if (fixedLens != null) listOf(LensSpec.fromEntity(fixedLens)) else emptyList()
        } else {
            val list = repo.getLensesByMount(camera.mount).map { LensSpec.fromEntity(it) }.toMutableList()
            for (s in userPrefs.getSuggestListSub("mount_adapters", camera.mount)) {
                list.addAll(repo.getLensesByMount(s).map { LensSpec.fromEntity(it) })
            }
            if (currentLens != null && camera.mount != currentLens.mount && list.none { it.id == currentLens.id }) {
                list.add(0, currentLens)
            }
            list
        }
        _uiState.update { it.copy(lensList = lensList) }
    }

    private fun refreshApertureListInternal(lens: LensSpec?) {
        val list = lens?.fSteps?.map { it.toString() } ?: emptyList()
        _uiState.update { state ->
            state.copy(
                apertureList = list,
                aperture = if (list.size == 1) list[0] else state.aperture
            )
        }
    }

    private fun updateAccessoriesStrInternal() = viewModelScope.launch(Dispatchers.IO) {
        val names = _uiState.value.selectedAccessories.mapNotNull { repo.getAccessory(it)?.name }
        _uiState.update { it.copy(accessoriesStr = names.joinToString(", ")) }
    }

    fun onDateChange(date: String) {
        _uiState.update { it.copy(date = date, isDirty = true) }
    }

    fun onLensChange(lensId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val lensEntity = repo.getLens(lensId)
        val lens = if (lensEntity != null) LensSpec.fromEntity(lensEntity) else null
        val focalLengthRange = if (lens != null) Util.getFocalLengthRangeFromStr(lens.focalLength) else Pair(0.0, 0.0)
        _uiState.update { it.copy(lensId = lensId, focalLengthRange = focalLengthRange, focalLengthProgress = 0, isDirty = true) }
        refreshApertureListInternal(lens)
    }

    fun onApertureChange(aperture: String) {
        _uiState.update { it.copy(aperture = aperture, isDirty = true) }
    }

    fun onShutterSpeedChange(ss: String) {
        _uiState.update { it.copy(shutterSpeed = ss, isDirty = true) }
    }

    fun onFocalLengthProgressChange(progress: Int) {
        _uiState.update { it.copy(focalLengthProgress = progress, isDirty = true) }
    }

    fun onExpCompProgressChange(progress: Int) {
        _uiState.update { it.copy(expCompProgress = progress, isDirty = true) }
    }

    fun onTtlProgressChange(progress: Int) {
        _uiState.update { it.copy(ttlProgress = progress, isDirty = true) }
    }

    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location, isDirty = true) }
    }

    fun onLatLngChange(lat: Double, lng: Double) {
        _uiState.update { it.copy(latitude = lat, longitude = lng, isDirty = true) }
    }

    fun onMemoChange(memo: String) {
        _uiState.update { it.copy(memo = memo, isDirty = true) }
    }

    fun onFavoriteChange(favorite: Boolean) {
        _uiState.update { it.copy(favorite = favorite, isDirty = true) }
    }

    fun onAccessoriesChange(accessories: List<Int>) = viewModelScope.launch(Dispatchers.IO) {
        val names = accessories.mapNotNull { repo.getAccessory(it)?.name }
        _uiState.update { it.copy(selectedAccessories = accessories, accessoriesStr = names.joinToString(", "), isDirty = true) }
    }

    fun onSupplementalImagesChange(images: List<String>) {
        _uiState.update { it.copy(supplementalImages = images, isDirty = true) }
    }

    fun onTagCheckedChange(index: Int, checked: Boolean) {
        val newCheckedStates = _uiState.value.tagCheckedStates.toMutableList()
        newCheckedStates[index] = checked
        _uiState.update { it.copy(tagCheckedStates = newCheckedStates, isDirty = true) }
    }

    fun onAddTag(tag: String) {
        _uiState.update { state ->
            if (tag.isNotEmpty() && !state.allTags.contains(tag)) {
                state.copy(
                    allTags = state.allTags + tag,
                    tagCheckedStates = state.tagCheckedStates + true,
                    isDirty = true
                )
            } else {
                state
            }
        }
    }

    fun handleAddLensResult(lens: LensSpec) = viewModelScope.launch(Dispatchers.IO) {
        val newId = repo.upsertLens(lens.toEntity()).toInt()
        val filmRollEntity = repo.getFilmRollRaw(_uiState.value.filmRollId) ?: return@launch
        val cameraEntity = repo.getCamera(filmRollEntity.camera!!) ?: return@launch
        val camera = CameraSpec.fromEntity(cameraEntity)
        val lEntity = repo.getLens(newId)
        val l = if (lEntity != null) LensSpec.fromEntity(lEntity) else null
        
        if (l != null) {
            _uiState.update { it.copy(lensId = newId, focalLengthRange = Util.getFocalLengthRangeFromStr(l.focalLength), focalLengthProgress = 0, isDirty = true) }
            updateLensListInternal(l, camera)
            refreshApertureListInternal(l)
        }
    }

    fun onMountAdaptersChanged(mount: String, selectedMounts: ArrayList<String>) = viewModelScope.launch(Dispatchers.IO) {
        userPrefs.saveSuggestListSub("mount_adapters", mount, selectedMounts)
        val filmRollEntity = repo.getFilmRollRaw(_uiState.value.filmRollId) ?: return@launch
        val cameraEntity = repo.getCamera(filmRollEntity.camera!!) ?: return@launch
        val camera = CameraSpec.fromEntity(cameraEntity)
        val currentLensEntity = repo.getLens(_uiState.value.lensId)
        val currentLens = if (currentLensEntity != null) LensSpec.fromEntity(currentLensEntity) else null
        updateLensListInternal(currentLens, camera)
    }

    fun getExpCompensation(): Double {
        val state = _uiState.value
        val bd = BigDecimal((state.expCompProgress - state.evGrainSize * state.evWidth).toDouble() / state.evGrainSize.toDouble())
        return bd.setScale(1, RoundingMode.DOWN).toDouble()
    }

    fun getTtlLightMeter(): Double {
        val state = _uiState.value
        val bd = BigDecimal((state.ttlProgress - state.evGrainSize * state.evWidth).toDouble() / state.evGrainSize.toDouble())
        return bd.setScale(1, RoundingMode.DOWN).toDouble()
    }

    fun toHumanReadableCompensationAmount(progress: Int): String {
        val state = _uiState.value
        val bd = BigDecimal((progress - state.evGrainSize * state.evWidth).toDouble() / state.evGrainSize.toDouble())
        val bd2 = bd.setScale(1, RoundingMode.DOWN)
        return (if (bd2.signum() > 0) "+" else "") + bd2.toPlainString() + "EV"
    }

    fun save() = viewModelScope.launch(Dispatchers.IO) {
        val state = _uiState.value
        val sb = StringBuilder("/")
        for (accessory in state.selectedAccessories) {
            sb.append(accessory)
            sb.append("/")
        }
        val accessoriesStr = sb.toString()

        var frameIndex = state.frameIndex
        if (frameIndex == -1) {
            val ps = repo.getPhotosByFilmRollIdRaw(state.filmRollId)
            frameIndex = if (ps.isEmpty()) 0 else (ps.last()._index ?: 0) + 1
        }

        val photo = Photo(
            state.id, // if it was normalized to 0 in init, Room will auto-generate it correctly
            state.filmRollId,
            frameIndex,
            state.date,
            0,
            state.lensId,
            state.focalLengthProgress.toDouble() + state.focalLengthRange.first,
            Util.safeStr2Dobule(state.aperture),
            Util.stringToDoubleShutterSpeed(state.shutterSpeed),
            getExpCompensation(),
            getTtlLightMeter(),
            state.location,
            state.latitude,
            state.longitude,
            state.memo,
            accessoriesStr,
            JSONArray(state.supplementalImages).toString(),
            state.favorite
        )

        val tags = mutableListOf<String>()
        state.tagCheckedStates.forEachIndexed { index, checked ->
            if (checked) tags.add(state.allTags[index])
        }

        val photoId = repo.upsertPhoto(photo.toEntity())
        repo.tagPhoto(if (state.id > 0) state.id else photoId.toInt(), state.filmRollId, ArrayList(tags))
        
        _events.emit(EditPhotoEvent.SaveSuccess)
    }

    fun getPhotoText(): String {
        val state = _uiState.value
        val context = getApplication<Application>().applicationContext
        val sb = StringBuilder()
        sb.append(context.getString(R.string.label_date) + ": " + state.date + "\n")
        val l = state.lensList.find { it.id == state.lensId }
        if (l != null) {
            sb.append(context.getString(R.string.label_lens_name) + ": " + l.manufacturer + " " + l.modelName + "\n")
            if (state.aperture.isNotEmpty()) sb.append(context.getString(R.string.label_aperture) + ": " + state.aperture + "\n")
            if (state.shutterSpeed.isNotEmpty()) sb.append(context.getString(R.string.label_shutter_speed) + ": " + state.shutterSpeed + "\n")
            val ec = getExpCompensation()
            if (ec != 0.0) sb.append(context.getString(R.string.label_exposure_compensation) + ": " + ec + "\n")
            val ttl = getTtlLightMeter()
            if (ttl != 0.0) sb.append(context.getString(R.string.label_ttl_light_meter) + ": " + ttl + "\n")
            if (state.location.isNotEmpty()) sb.append(context.getString(R.string.label_location) + ": " + state.location + "\n")
            if (state.latitude != 999.0 && state.longitude != 999.0) sb.append(context.getString(R.string.label_coordinate) + ": " + state.latitude + ", " + state.longitude + "\n")
            if (state.memo.isNotEmpty()) sb.append(context.getString(R.string.label_memo) + ": " + state.memo + "\n")
            if (state.accessoriesStr.isNotEmpty()) sb.append(context.getString(R.string.label_accessories) + ": " + state.accessoriesStr + "\n")
        }
        return sb.toString()
    }
}
