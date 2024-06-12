package net.tnose.app.trisquel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel(application: Application?) : AndroidViewModel(
    application!!
) {
    private val mRepository: TrisquelRepo
    val searchTags = MutableLiveData<List<String>>(listOf())
    init {
        mRepository = TrisquelRepo(application)
    }

    val photosByAndQuery: LiveData<List<Pair<Pair<String, Int>, PhotoAndRels>>>  = searchTags.switchMap {
        mRepository.getPhotosByAndQuery(it)
    }
/*
    fun shiftFrameIndexFrom(entity: PhotoEntity, amount : Int) = viewModelScope.launch {
        val list = mRepository.getPhotosByFilmRollId(entity.filmroll!!).value!!

    }

    fun insert(entity: PhotoEntity) = viewModelScope.launch {
        mRepository.upsertPhoto(entity)
    }

    fun insertWithTag(entity: PhotoEntity, filmRollId : Int, tags: java.util.ArrayList<String>) = viewModelScope.launch {
        val photoId = mRepository.upsertPhoto(entity)
        mRepository.tagPhoto(photoId.toInt(), filmRollId, tags)
    }*/

    // 意味ないけどViewModelの段階ではなんとなく分けておく
    fun update(entity: PhotoEntity) = viewModelScope.launch {
        mRepository.upsertPhoto(entity)
    }

    fun delete(id : Int)  = viewModelScope.launch {
        mRepository.deletePhoto(id)
    }

    fun tagPhoto(photoId: Int, filmRollId: Int, tags: ArrayList<String>) = viewModelScope.launch {
        mRepository.tagPhoto(photoId, filmRollId, tags)
    }
}