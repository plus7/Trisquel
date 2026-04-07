package net.tnose.app.trisquel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class FilmRollEvent {
    data class ShowDeleteConfirm(val id: Int, val name: String) : FilmRollEvent()
}

class FilmRollViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(
    application
) {
    private val mRepository: TrisquelRepo

    private val _events = MutableSharedFlow<FilmRollEvent>()
    val events = _events.asSharedFlow()

    init {
        mRepository = TrisquelRepo(application)
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val viewRule: MutableLiveData<Pair<Int, Pair<Int, String>>> = 
        savedStateHandle.getLiveData("view_rule", Pair(0, Pair(0, "")))

    val allFilmRollAndRels: LiveData<List<FilmRollAndRels>> = viewRule.switchMap {
        val sortBy = it.first
        val filterByKind = it.second.first
        val filterByValue = it.second.second
        _isLoading.value = true
        mRepository.getAllFilmRolls(sortBy, filterByKind, filterByValue)
    }.map {
        _isLoading.value = false
        it
    }

    fun handleAddResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val cEntity = mRepository.getCamera(bundle.getInt("camera"))
        val c = cEntity?.let { CameraSpec.fromEntity(it) }
        val f = FilmRoll(0, bundle.getString("name")!!, c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
        mRepository.upsertFilmRoll(f.toEntity())
    }

    fun handleEditResult(intent: Intent?) = viewModelScope.launch(Dispatchers.IO) {
        val bundle = intent?.extras ?: return@launch
        val cEntity = mRepository.getCamera(bundle.getInt("camera"))
        val c = cEntity?.let { CameraSpec.fromEntity(it) }
        val f = FilmRoll(bundle.getInt("id"), bundle.getString("name")!!, bundle.getString("created")!!, Util.dateToStringUTC(Date()), c!!, bundle.getString("manufacturer")!!, bundle.getString("brand")!!, bundle.getInt("iso"), 36)
        mRepository.upsertFilmRoll(f.toEntity())
    }

    fun insert(entity: FilmRollEntity) = viewModelScope.launch {
        mRepository.upsertFilmRoll(entity)
    }

    fun update(entity: FilmRollEntity) = viewModelScope.launch {
        mRepository.upsertFilmRoll(entity)
    }

    fun refresh(id: Int) = viewModelScope.launch {
        val entity = mRepository.getFilmRoll(id)
        entity.value?.let { mRepository.upsertFilmRoll(it) }
    }

    fun requestDelete(filmRoll: FilmRoll) = viewModelScope.launch {
        _events.emit(FilmRollEvent.ShowDeleteConfirm(filmRoll.id, filmRoll.name))
    }

    fun delete(id : Int)  = viewModelScope.launch {
        mRepository.deleteFilmRoll(id)
    }
}
