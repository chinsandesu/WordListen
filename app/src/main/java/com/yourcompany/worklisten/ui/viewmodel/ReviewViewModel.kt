package com.yourcompany.worklisten.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.paging.PagingData

/**
 * 复习模式视图模型
 */
class ReviewViewModel(
    private val repository: WordRepository,
    private val ttsHelper: TtsHelper
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
    
    // 替换已加载单词列表为分页数据流
    private val _pagedWords = kotlinx.coroutines.flow.MutableStateFlow<PagingData<Word>>(PagingData.empty())
    val pagedWords: kotlinx.coroutines.flow.StateFlow<PagingData<Word>> = _pagedWords.asStateFlow()
    
    // 选中的组ID
    private var selectedGroupIds = listOf<Int>()
    
    init {
        loadData()
    }
    
    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            // 初始化默认播放进度
            repository.initDefaultPlaybackProgress()

            // 加载播放进度
            repository.getPlaybackProgress().collectLatest { progress ->
                if (progress == null) {
                    _uiState.update {
                        it.copy(isLoading = false, hasNoLibrary = true, hasNoWords = true)
                    }
                    return@collectLatest
                }

                val libraryId = progress.activeLibraryId

                // 解析选定的组ID
                selectedGroupIds = if (progress.selectedGroups.isBlank()) {
                    emptyList()
                } else {
                    progress.selectedGroups.split(",").mapNotNull { groupId ->
                        groupId.toIntOrNull()
                    }
                }

                // 如果有激活的词库和选中的组，加载单词和组
                if (libraryId != -1L && selectedGroupIds.isNotEmpty()) {
                    loadWordsAndGroups(libraryId)
                    _uiState.update { state ->
                        state.copy(isLoading = false, hasNoLibrary = false, hasNoWords = false)
                    }
                } else {
                    // 更新UI状态
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            hasNoWords = selectedGroupIds.isEmpty(),
                            hasNoLibrary = libraryId == -1L
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 加载单词和组
     */
    private fun loadWordsAndGroups(libraryId: Long) {
        viewModelScope.launch {
            repository.getPagedWordsFromGroups(libraryId, selectedGroupIds).collectLatest { pagingData ->
                _pagedWords.value = pagingData
            }
        }
        
        viewModelScope.launch {
            repository.getGroupsForLibrary(libraryId).collectLatest { groups ->
                val selectedGroups = groups.filter { it.groupId in selectedGroupIds }
                
                _uiState.update { state ->
                    state.copy(
                        selectedGroups = selectedGroups
                    )
                }
            }
        }
    }
    
    /**
     * 更新显示模式
     */
    fun updateDisplayMode(mode: ReviewDisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
    }
    
    /**
     * 朗读单词
     */
    fun speakWord(word: Word) {
        viewModelScope.launch {
            val speed = repository.getPlaybackProgressOnce()?.playbackSpeed ?: 1.0f
            ttsHelper.speak(word, speed)
        }
    }
    
    /**
     * 朗读中文意思
     */
    fun speakMeaning(word: Word) {
        viewModelScope.launch {
            val speed = repository.getPlaybackProgressOnce()?.playbackSpeed ?: 1.0f
            ttsHelper.speakMeaning(word, speed)
        }
    }
    
    /**
     * 标记单词为已复习
     */
    fun markWordAsReviewed(wordId: Long) {
        viewModelScope.launch {
            repository.updateWordReviewed(wordId, true)
        }
    }
    
    /**
     * 标记选中的组为已复习
     */
    fun markGroupsAsReviewed() {
        viewModelScope.launch {
            val groupIds = uiState.value.selectedGroups.map { it.id }
            groupIds.forEach { groupId ->
                repository.updateGroupReviewed(groupId, true)
            }
        }
    }
    
    /**
     * 设置滚动到底部状态
     */
    fun setScrolledToBottom(scrolled: Boolean) {
        if (scrolled && !uiState.value.hasScrolledToBottom) {
            _uiState.update { it.copy(hasScrolledToBottom = true) }
            markGroupsAsReviewed()
        }
    }
    
    /**
     * 视图模型工厂
     */
    class Factory(
        private val repository: WordRepository,
        private val ttsHelper: TtsHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
                return ReviewViewModel(repository, ttsHelper) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * 复习模式UI状态
 */
data class ReviewUiState(
    val isLoading: Boolean = true,
    val hasNoWords: Boolean = false,
    val hasNoLibrary: Boolean = false,
    val words: List<Word> = emptyList(),
    val selectedGroups: List<WordGroup> = emptyList(),
    val displayMode: ReviewDisplayMode = ReviewDisplayMode.SHOW_ALL,
    val hasScrolledToBottom: Boolean = false
)

/**
 * 显示模式
 */
enum class ReviewDisplayMode {
    SHOW_ALL,    // 显示所有
    HIDE_WORD,   // 隐藏单词
    HIDE_MEANING // 隐藏意思
} 