package net.tnose.app.trisquel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var activeDialog by mutableStateOf<ActiveDialog?>(null)

    fun showDialog(dialog: ActiveDialog) {
        activeDialog = dialog
    }

    fun dismissDialog() {
        activeDialog = null
    }

    var currentFilter by mutableStateOf(Pair(0, arrayListOf<String>()))
    var currentSubtitle by mutableStateOf("")
}
