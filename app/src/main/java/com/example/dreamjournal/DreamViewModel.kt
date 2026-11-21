package com.example.dreamjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DreamViewModel(private val repository: DreamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DreamUiState())
    val uiState: StateFlow<DreamUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allDreams.collect { list ->
                _uiState.update { state ->
                    val updatedSelected = state.selectedDream?.let { selected ->
                        list.find { it.id == selected.id }
                    }
                    state.copy(
                        dreams = list,
                        selectedDream = updatedSelected
                    )
                }
            }
        }
    }

    fun selectDream(id: String) {
        viewModelScope.launch {
            repository.getDreamById(id).collect { dream ->
                _uiState.update { it.copy(selectedDream = dream) }
            }
        }
    }

    fun setupFormForCreate() {
        _uiState.update {
            it.copy(
                titleInput = "",
                descriptionInput = "",
                interpretationInput = "",
                statusInput = "Draft",
                titleError = null,
                descriptionError = null,
                isEditing = false,
                editingDreamId = null
            )
        }
    }

    fun setupFormForEdit(dream: DreamEntity) {
        _uiState.update {
            it.copy(
                titleInput = dream.title,
                descriptionInput = dream.description,
                interpretationInput = dream.interpretation,
                statusInput = dream.status,
                titleError = null,
                descriptionError = null,
                isEditing = true,
                editingDreamId = dream.id
            )
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                titleInput = value,
                titleError = if (value.trim().length >= 3) null else it.titleError
            )
        }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update {
            it.copy(
                descriptionInput = value,
                descriptionError = if (value.trim().length >= 10) null else it.descriptionError
            )
        }
    }

    fun onInterpretationChanged(value: String) {
        _uiState.update { it.copy(interpretationInput = value) }
    }

    fun onStatusChanged(value: String) {
        val currentInterpretation = _uiState.value.interpretationInput
        if (value == "Analyzed" && currentInterpretation.isBlank()) {
            _uiState.update {
                it.copy(
                    userMessage = "Interpretation cannot be empty when setting status to Analyzed."
                )
            }
            return
        }
        _uiState.update { it.copy(statusInput = value) }
    }

    fun saveDream(): Boolean {
        val state = _uiState.value
        val titleTrimmed = state.titleInput.trim()
        val descriptionTrimmed = state.descriptionInput.trim()
        val interpretationTrimmed = state.interpretationInput.trim()

        var hasError = false
        var tError: String? = null
        var dError: String? = null

        if (titleTrimmed.length < 3) {
            tError = "Title must be at least 3 characters"
            hasError = true
        }

        if (descriptionTrimmed.length < 10) {
            dError = "Description must be at least 10 characters"
            hasError = true
        }

        if (state.statusInput == "Analyzed" && interpretationTrimmed.isBlank()) {
            _uiState.update {
                it.copy(
                    userMessage = "Interpretation is required for Analyzed status.",
                    titleError = tError,
                    descriptionError = dError
                )
            }
            return false
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    titleError = tError,
                    descriptionError = dError
                )
            }
            return false
        }

        viewModelScope.launch {
            if (state.isEditing && state.editingDreamId != null) {
                val existing = state.dreams.find { it.id == state.editingDreamId }
                val updatedDream = DreamEntity(
                    id = state.editingDreamId,
                    title = titleTrimmed,
                    description = descriptionTrimmed,
                    interpretation = interpretationTrimmed,
                    status = state.statusInput,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                repository.updateDream(updatedDream)
                _uiState.update { it.copy(selectedDream = updatedDream, userMessage = "Dream updated successfully") }
            } else {
                val newDream = DreamEntity(
                    title = titleTrimmed,
                    description = descriptionTrimmed,
                    interpretation = interpretationTrimmed,
                    status = state.statusInput
                )
                repository.insertDream(newDream)
                _uiState.update { it.copy(userMessage = "Dream saved successfully") }
            }
        }

        return true
    }

    fun updateDreamStatus(dream: DreamEntity, newStatus: String) {
        if (newStatus == "Analyzed" && dream.interpretation.isBlank()) {
            _uiState.update {
                it.copy(userMessage = "Interpretation cannot be empty for Analyzed status. Add an interpretation first!")
            }
            return
        }

        viewModelScope.launch {
            val updated = dream.copy(status = newStatus)
            repository.updateDream(updated)
            _uiState.update {
                it.copy(
                    selectedDream = updated,
                    userMessage = "Status updated to $newStatus"
                )
            }
        }
    }

    fun deleteDream(dream: DreamEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDream(dream)
            _uiState.update {
                it.copy(
                    lastDeletedDream = dream,
                    selectedDream = null,
                    userMessage = "Dream deleted"
                )
            }
            onSuccess()
        }
    }

    fun undoDelete() {
        val deleted = _uiState.value.lastDeletedDream ?: return
        viewModelScope.launch {
            repository.insertDream(deleted)
            _uiState.update {
                it.copy(
                    lastDeletedDream = null,
                    userMessage = "Dream restored"
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

class DreamViewModelFactory(private val repository: DreamRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DreamViewModel::class.java)) {
            return DreamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
