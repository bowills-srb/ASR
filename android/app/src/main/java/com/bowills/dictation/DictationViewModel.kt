package com.bowills.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowills.dictation.network.DictationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the debug dictation loop. */
sealed interface DictationUiState {
    data object Idle : DictationUiState
    data object Recording : DictationUiState
    data object Sending : DictationUiState
    data class Result(val response: DictationResponse) : DictationUiState
    data class Error(val message: String) : DictationUiState
}

class DictationViewModel : ViewModel() {

    private val engine = DictationEngine()

    private val _state = MutableStateFlow<DictationUiState>(DictationUiState.Idle)
    val state: StateFlow<DictationUiState> = _state.asStateFlow()

    fun startRecording() {
        if (_state.value == DictationUiState.Recording) return
        runCatching { engine.start() }
            .onSuccess { _state.value = DictationUiState.Recording }
            .onFailure { _state.value = DictationUiState.Error("Mic error: ${it.message}") }
    }

    fun stopAndSend() {
        if (_state.value != DictationUiState.Recording) return
        _state.value = DictationUiState.Sending
        viewModelScope.launch {
            runCatching { engine.stopAndSend() }
                .onSuccess { _state.value = DictationUiState.Result(it) }
                .onFailure { _state.value = DictationUiState.Error("Request failed: ${it.message}") }
        }
    }

    fun reset() {
        _state.value = DictationUiState.Idle
    }
}
