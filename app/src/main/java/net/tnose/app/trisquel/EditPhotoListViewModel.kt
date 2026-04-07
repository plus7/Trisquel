package net.tnose.app.trisquel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditPhotoListEvent {
    data class ShowToast(val message: String) : EditPhotoListEvent()
}

class EditPhotoListViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repo = TrisquelRepo(application)

    val filmRollId: Int = savedStateHandle.get<Int>("id") ?: -1

    private val _events = MutableSharedFlow<EditPhotoListEvent>()
    val events = _events.asSharedFlow()

    val filmRoll: StateFlow<FilmRoll?> = repo.getFilmRollAndRelsFlow(filmRollId)
        .map { rels ->
            if (rels == null) null else FilmRoll.fromEntity(rels)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val photos: StateFlow<List<Pair<String, PhotoAndTagIds>>> = repo.getPhotosByFilmRollIdFlow(filmRollId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun deletePhoto(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.deletePhoto(id)
    }

    fun toggleFavPhoto(photo: Photo) = viewModelScope.launch(Dispatchers.IO) {
        photo.favorite = !photo.favorite
        repo.upsertPhoto(photo.toEntity())
    }

    fun updatePhoto(photo: Photo, tags: ArrayList<String>?) = viewModelScope.launch(Dispatchers.IO) {
        if (tags != null) {
            repo.tagPhoto(photo.id, filmRollId, tags)
        }
        repo.upsertPhoto(photo.toEntity())
    }

    fun insertPhoto(photo: Photo, tags: ArrayList<String>?) = viewModelScope.launch(Dispatchers.IO) {
        if (photo.frameIndex == -1) {
            val currentPhotos = photos.value
            photo.frameIndex = if (currentPhotos.isEmpty()) 0 else (currentPhotos.last().second.photo._index ?: 0) + 1
        }
        val id = repo.upsertPhoto(photo.toEntity())
        if (tags != null) {
            repo.tagPhoto(id.toInt(), filmRollId, tags)
        }
    }

    fun shiftFrameIndexFrom(photo: Photo, amount: Int) = viewModelScope.launch(Dispatchers.IO) {
        val currentPhotos = photos.value
        val curPos = currentPhotos.indexOfFirst { it.second.photo.id == photo.id }
        if (curPos == -1) return@launch

        for (i in curPos until currentPhotos.size) {
            val p = currentPhotos[i].second.photo
            val np = p.copy(_index = (p._index ?: 0) + amount)
            repo.upsertPhoto(np)
        }
    }

    fun possibleDownShiftLimit(photo: Photo): Int {
        val currentPhotos = photos.value
        val curPos = currentPhotos.indexOfFirst { it.second.photo.id == photo.id }
        return if (curPos > 0) {
            currentPhotos[curPos - 1].second.photo._index ?: 0
        } else {
            0
        }
    }

    fun addSupplementalImage(photo: Photo, uri: String) = viewModelScope.launch(Dispatchers.IO) {
        photo.supplementalImages.add(uri)
        repo.upsertPhoto(photo.toEntity())
    }

    suspend fun getFilmRollClipboardText(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val fr = filmRoll.value ?: return@withContext ""
        val context = getApplication<Application>().applicationContext

        sb.append(fr.name + "\n")
        if (fr.manufacturer.isNotEmpty()) sb.append(context.getString(R.string.label_manufacturer) + ": " + fr.manufacturer + "\n")
        if (fr.brand.isNotEmpty()) sb.append(context.getString(R.string.label_brand) + ": " + fr.brand + "\n")
        if (fr.iso > 0) sb.append(context.getString(R.string.label_iso) + ": " + fr.iso + "\n")

        val c = fr.camera
        sb.append(context.getString(R.string.label_camera) + ": " + c.manufacturer + " " + c.modelName + "\n")

        val ps = repo.getPhotosByFilmRollIdRaw(fr.id)
        for (pEntity in ps) {
            val p = Photo.fromEntity(pEntity)
            val lEntity = repo.getLens(p.lensid)
            sb.append("------[No. " + (p.frameIndex + 1) + "]------\n")
            sb.append(context.getString(R.string.label_date) + ": " + p.date + "\n")
            if (lEntity != null) {
                sb.append(context.getString(R.string.label_lens_name) + ": " + lEntity.manufacturer + " " + lEntity.modelName + "\n")
            }
            if (p.aperture > 0) sb.append(context.getString(R.string.label_aperture) + ": " + p.aperture + "\n")
            if (p.shutterSpeed > 0) sb.append(context.getString(R.string.label_shutter_speed) + ": " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "\n")
            if (p.expCompensation != 0.0) sb.append(context.getString(R.string.label_exposure_compensation) + ": " + p.expCompensation + "\n")
            if (p.ttlLightMeter != 0.0) sb.append(context.getString(R.string.label_ttl_light_meter) + ": " + p.ttlLightMeter + "\n")
            if (p.location.isNotEmpty()) sb.append(context.getString(R.string.label_location) + ": " + p.location + "\n")
            if (p.latitude != 999.0 && p.longitude != 999.0) sb.append(context.getString(R.string.label_coordinate) + ": " + p.latitude + ", " + p.longitude + "\n")
            if (p.memo.isNotEmpty()) sb.append(context.getString(R.string.label_memo) + ": " + p.memo + "\n")

            if (p.accessories.isNotEmpty()) {
                sb.append(context.getString(R.string.label_accessories) + ": ")
                val accNames = p.accessories.mapNotNull { repo.getAccessory(it)?.name }
                sb.append(accNames.joinToString(", "))
                sb.append("\n")
            }
        }
        sb.toString()
    }

    // thumbnailEditingPhoto handling
    fun setThumbnailEditingPhotoId(id: Int) {
        savedStateHandle["thumbnail_editing_id"] = id
    }

    fun getThumbnailEditingPhotoId(): Int {
        return savedStateHandle.get<Int>("thumbnail_editing_id") ?: -1
    }

    suspend fun getPhotoById(id: Int): Photo? = withContext(Dispatchers.IO) {
        repo.getPhoto(id)?.let { Photo.fromEntity(it) }
    }

    fun handlePickImageResult(uris: List<Uri>) {
        if (uris.isEmpty()) {
            setThumbnailEditingPhotoId(-1)
        } else {
            val id = getThumbnailEditingPhotoId()
            if (id != -1) {
                viewModelScope.launch {
                    val p = getPhotoById(id)
                    if (p != null) {
                        p.supplementalImages.add(uris[0].toString())
                        updatePhoto(p, null)
                        setThumbnailEditingPhotoId(-1)
                    }
                }
            }
        }
    }
}
