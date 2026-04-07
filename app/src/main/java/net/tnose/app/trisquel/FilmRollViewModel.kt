package net.tnose.app.trisquel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

sealed class FilmRollEvent {
    data class ShowDeleteConfirm(val id: Int, val name: String) : FilmRollEvent()
}

class FilmRollViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(
    application
) {
    private val mRepository: TrisquelRepo = TrisquelRepo(application)

    private val _events = MutableSharedFlow<FilmRollEvent>()
    val events = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // SavedStateHandleからStateFlowを取得
    val viewRule = savedStateHandle.getStateFlow("view_rule", Pair(0, Pair(0, "")))

    @OptIn(ExperimentalCoroutinesApi::class)
    val allFilmRollAndRels: StateFlow<List<FilmRollAndRels>> = viewRule
        .onEach { _isLoading.value = true }
        .flatMapLatest { rule ->
            mRepository.getAllFilmRollsFlow(rule.first, rule.second.first, rule.second.second)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateViewRule(rule: Pair<Int, Pair<Int, String>>) {
        savedStateHandle["view_rule"] = rule
    }

    fun update(entity: FilmRollEntity) = viewModelScope.launch {
        mRepository.upsertFilmRoll(entity)
    }

    fun refresh(id: Int) = viewModelScope.launch {
        val entity = mRepository.getFilmRollRaw(id)
        entity?.let { mRepository.upsertFilmRoll(it) }
    }

    fun requestDelete(filmRoll: FilmRoll) = viewModelScope.launch {
        _events.emit(FilmRollEvent.ShowDeleteConfirm(filmRoll.id, filmRoll.name))
    }

    fun delete(id : Int)  = viewModelScope.launch {
        mRepository.deleteFilmRoll(id)
    }
}
