package com.yourcompany.worklisten.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.ui.components.DisplayModeSelector
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.components.ReviewWordCard
import com.yourcompany.worklisten.ui.viewmodel.ReviewViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.lazy.items
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.ReviewDisplayMode

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> {
            LoadingIndicator()
        }
        uiState.hasNoLibrary -> {
            EmptyState(message = stringResource(id = R.string.no_library))
        }
        uiState.hasNoWords -> {
            EmptyState(message = stringResource(id = R.string.no_words))
        }
        else -> {
            val pagingItems = viewModel.pagedWords.collectAsLazyPagingItems()
            ReviewContent(
                pagingItems = pagingItems,
                displayMode = uiState.displayMode,
                onDisplayModeChanged = viewModel::updateDisplayMode,
                onSpeak = viewModel::speakWord, // Pass a single speak handler
                onScrolledToBottom = viewModel::setScrolledToBottom
            )
        }
    }
}

@Composable
private fun ReviewContent(
    pagingItems: androidx.paging.compose.LazyPagingItems<Word>,
    displayMode: ReviewDisplayMode,
    onDisplayModeChanged: (ReviewDisplayMode) -> Unit,
    onSpeak: (Word) -> Unit,
    onScrolledToBottom: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 标题
        Text(
            text = "刷词模式",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        
        // 显示模式选择器
        DisplayModeSelector(
            currentMode = displayMode,
            onModeSelected = onDisplayModeChanged
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
        
        // 单词列表
        val listState = rememberLazyListState()
        
        // 检测是否已滚动到底部
        val isAtBottom by remember {
            derivedStateOf {
                val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isEmpty()) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    val lastIndex = pagingItems.itemCount - 1
                    lastVisibleItem.index == lastIndex && lastVisibleItem.offset + lastVisibleItem.size <= listState.layoutInfo.viewportEndOffset
                }
            }
        }
        
        // 当滚动到底部时通知ViewModel
        LaunchedEffect(isAtBottom) {
            if (isAtBottom) {
                onScrolledToBottom(true)
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            items(pagingItems.itemCount) { index ->
                val word = pagingItems[index]
                word?.let {
                    ReviewWordCard(
                        word = it,
                        displayMode = displayMode,
                        onSpeak = { onSpeak(it) } // Correctly call onSpeak
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
} 