package com.yourcompany.worklisten.ui.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsHelper: TtsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initDefaultPlaybackProgress()
        }

        // Main data flow that listens to all states needed for playback
        viewModelScope.launch {
            combine(
                repository.getPlaybackProgress(),
                settingsRepository.backgroundImageUri
            ) { progress, bgImageUri ->
                if (progress == null) {
                    return@combine PlayerUiState(isLoading = true, backgroundImageUri = bgImageUri)
                }

                val activeLibrary = progress.activeLibraryId?.let { repository.getActiveLibrary() }
                val words = if (activeLibrary != null && progress.selectedGroups.isNotBlank()) {
                    val selectedGroupIds = progress.selectedGroups.split(",").mapNotNull { it.toIntOrNull() }
                    repository.getWordsFromGroupsOnce(activeLibrary.id, selectedGroupIds)
                } else {
                    emptyList()
                }

                // Create a temporary state object to pass to the main update
                PlayerUiState(
                    isLoading = false,
                    words = words,
                    currentWord = words.getOrNull(progress.currentIndex),
                    currentIndex = progress.currentIndex,
                    playbackMode = PlaybackMode.fromString(progress.playbackMode),
                    isRandom = progress.isRandom,
                    isLoop = progress.isLoop,
                    playbackSpeed = progress.playbackSpeed,
                    playbackInterval = progress.playbackInterval,
                    activeLibraryName = activeLibrary?.name ?: "未选择词库",
                    hasWords = words.isNotEmpty(),
                    backgroundImageUri = bgImageUri
                )
            }.collectLatest { state ->
                // Update the main UI state
                _uiState.update {
                    it.copy(
                        isLoading = state.isLoading,
                        words = state.words,
                        currentWord = state.currentWord,
                        currentIndex = state.currentIndex,
                        playbackMode = state.playbackMode,
                        isRandom = state.isRandom,
                        isLoop = state.isLoop,
                        playbackSpeed = state.playbackSpeed,
                        playbackInterval = state.playbackInterval,
                        activeLibraryName = state.activeLibraryName,
                        hasWords = state.hasWords,
                        backgroundImageUri = state.backgroundImageUri
                    )
                }
                // Restart playback with new settings if it was already playing
                if (_uiState.value.isPlaying) {
                    startPlayback()
                }
            }
        }
    }

    private fun startPlayback() {
        stopPlayback()
        val state = _uiState.value
        if (state.words.isEmpty() || state.currentWord == null) {
            _uiState.update { it.copy(isPlaying = false) }
            return
        }

        playbackJob = viewModelScope.launch {
            val word = state.currentWord
            val onDone: () -> Unit = {
                viewModelScope.launch {
                    if (_uiState.value.isPlaying) { // Re-check state after delay
                        moveToNextWord()
                    }
                }
            }

            // Set meaning visibility based on the current playback mode
            val shouldShowMeaning = when (state.playbackMode) {
                PlaybackMode.CN_ONLY, PlaybackMode.WORD_TO_CN -> true
                else -> false
            }
            _uiState.update { it.copy(showMeaning = shouldShowMeaning) }

            when (state.playbackMode) {
                PlaybackMode.HIDE_ALL, PlaybackMode.WORD_ONLY -> {
                    ttsHelper.speak(word, state.playbackSpeed) {
                        ttsHelper.playSilence((state.playbackInterval * 1000).toLong(), onDone)
                    }
                }
                PlaybackMode.CN_ONLY -> {
                    ttsHelper.speakMeaning(word, state.playbackSpeed) {
                        ttsHelper.playSilence((state.playbackInterval * 1000).toLong(), onDone)
                    }
                }
                PlaybackMode.WORD_TO_CN -> {
                    ttsHelper.speak(word, state.playbackSpeed) {
                        // Meaning is already visible, just speak it after the word.
                        ttsHelper.speakMeaning(word, state.playbackSpeed) {
                            ttsHelper.playSilence((state.playbackInterval * 1000).toLong(), onDone)
                        }
                    }
                }
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        ttsHelper.stop()
    }

    fun togglePlayPause() {
        val isNowPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = isNowPlaying) }
        if (isNowPlaying) {
            startPlayback()
        } else {
            stopPlayback()
        }
    }

    fun nextWord() = moveToNextWord()

    fun previousWord() {
        stopPlayback()
        val state = _uiState.value
        if (state.words.isEmpty()) return

        val newIndex = if (state.isRandom) {
            (0 until state.words.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
        } else {
            if (state.currentIndex > 0) state.currentIndex - 1 else if (state.isLoop) state.words.size - 1 else state.currentIndex
        }
        updateCurrentIndex(newIndex, state.isPlaying)
    }

    private fun moveToNextWord() {
        val state = _uiState.value
        if (state.words.isEmpty()) return

        val newIndex = if (state.isRandom) {
            (0 until state.words.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
        } else {
            if (state.currentIndex < state.words.size - 1) state.currentIndex + 1 else if (state.isLoop) 0 else -1
        }
        
        if (newIndex == -1) { // Reached the end and not looping
            _uiState.update { it.copy(isPlaying = false) }
            stopPlayback()
        } else {
            updateCurrentIndex(newIndex, state.isPlaying)
        }
    }

    private fun updateCurrentIndex(index: Int, shouldAutoPlay: Boolean) {
        viewModelScope.launch {
            repository.updateCurrentIndex(index)
            // The flow will automatically update the UI. If autoplay is needed, restart playback.
            if (shouldAutoPlay) {
                startPlayback()
            }
        }
    }

    fun updateBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.saveBackgroundImageUri(uri.toString())
        }
    }

    fun removeBackgroundImage() {
        viewModelScope.launch {
            settingsRepository.saveBackgroundImageUri(null)
        }
    }

    fun onCardClicked() {
        _uiState.update { it.copy(showMeaning = !it.showMeaning) }
    }

    fun setPlaybackMode(mode: PlaybackMode) = viewModelScope.launch { repository.updatePlaybackMode(mode.name) }
    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch { repository.updatePlaybackSpeed(speed) }
    fun setPlaybackInterval(interval: Float) = viewModelScope.launch { repository.updatePlaybackInterval(interval) }
    fun toggleRandomMode() = viewModelScope.launch { repository.updateRandomMode(!_uiState.value.isRandom) }
    fun toggleLoopMode() = viewModelScope.launch { repository.updateLoopMode(!_uiState.value.isLoop) }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val currentWord: Word? = null,
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val showMeaning: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.WORD_TO_CN,
    val isRandom: Boolean = false,
    val isLoop: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackInterval: Float = 1.0f,
    val activeLibraryName: String = "未选择词库",
    val hasWords: Boolean = false,
    val backgroundImageUri: String? = null
)

enum class PlaybackMode(@StringRes val displayNameResId: Int) {
    HIDE_ALL(R.string.playback_mode_hide_all),
    CN_ONLY(R.string.playback_mode_cn_only),
    WORD_ONLY(R.string.playback_mode_word_only),
    WORD_TO_CN(R.string.playback_mode_word_to_cn);

    companion object {
        fun fromString(name: String?): PlaybackMode {
            return try {
                if (name == null) WORD_TO_CN else valueOf(name)
            } catch (e: IllegalArgumentException) {
                WORD_TO_CN // Default value
            }
        }
    }
} 