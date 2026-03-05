package com.apkstore.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkstore.shared.data.ApkRepository
import com.apkstore.shared.domain.ApkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApkStoreState(
    val apks: List<ApkInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedApk: ApkInfo? = null,
    val showUploadDialog: Boolean = false
)

class ApkStoreViewModel(
    private val repository: ApkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ApkStoreState())
    val state: StateFlow<ApkStoreState> = _state.asStateFlow()

    init {
        loadApks()
        observeRepository()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.apkList.collect { apps ->
                _state.value = _state.value.copy(apks = apps)
            }
        }
        viewModelScope.launch {
            repository.isLoading.collect { loading ->
                _state.value = _state.value.copy(isLoading = loading)
            }
        }
        viewModelScope.launch {
            repository.error.collect { error ->
                _state.value = _state.value.copy(error = error)
            }
        }
    }

    fun loadApks() {
        viewModelScope.launch {
            repository.refreshApks()
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            repository.searchApks(query)
        }
    }

    fun selectApk(apk: ApkInfo?) {
        _state.value = _state.value.copy(selectedApk = apk)
    }

    fun showUploadDialog(show: Boolean) {
        _state.value = _state.value.copy(showUploadDialog = show)
    }

    fun deleteApk(id: Int) {
        viewModelScope.launch {
            repository.deleteApk(id)
            _state.value = _state.value.copy(selectedApk = null)
        }
    }

    fun getDownloadUrl(id: Int): String = repository.getDownloadUrl(id)

    fun clearError() {
        repository.clearError()
    }
}
