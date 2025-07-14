package com.yourcompany.worklisten.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.components.PlayerControls
import com.yourcompany.worklisten.ui.components.PlayerSettingsSheet
import com.yourcompany.worklisten.ui.viewmodel.PlayerUiState
import com.yourcompany.worklisten.ui.viewmodel.PlayerViewModel
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode
import com.yourcompany.worklisten.utils.PartOfSpeechHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateBackgroundImage(it) }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Background Image
            if (uiState.backgroundImageUri != null) {
                AsyncImage(
                    model = uiState.backgroundImageUri,
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Translucent Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5DC).copy(alpha = 0.7f))
                )
            }

            // Main Content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                 when {
                    uiState.isLoading -> LoadingIndicator()
                    !uiState.hasWords -> EmptyState(message = stringResource(R.string.no_words_in_group))
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            key(uiState.currentWord?.id) {
                                WordDisplayCard(
                                    word = uiState.currentWord,
                                    playbackMode = uiState.playbackMode,
                                    showMeaning = uiState.showMeaning,
                                    onCardClicked = { viewModel.onCardClicked() }
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            PlayerControls(
                                isPlaying = uiState.isPlaying,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.nextWord() },
                                onPrevious = { viewModel.previousWord() },
                                onSettings = { showSheet = true }
                            )
                        }
                    }
                }
            }

            if (showSheet) {
                PlayerSettingsSheet(
                    sheetState = sheetState,
                    playbackMode = uiState.playbackMode,
                    isRandom = uiState.isRandom,
                    isLoop = uiState.isLoop,
                    playbackSpeed = uiState.playbackSpeed,
                    playbackInterval = uiState.playbackInterval,
                    onDismiss = {
                        scope.launch {
                            sheetState.hide()
                            showSheet = false
                        }
                    },
                    onModeChange = viewModel::setPlaybackMode,
                    onRandomToggle = viewModel::toggleRandomMode,
                    onLoopToggle = viewModel::toggleLoopMode,
                    onSpeedChange = viewModel::setPlaybackSpeed,
                    onIntervalChange = viewModel::setPlaybackInterval,
                    onLaunchImagePicker = { imagePickerLauncher.launch("image/*") },
                    onRemoveBackgroundImage = {
                        viewModel.removeBackgroundImage()
                        scope.launch {
                            sheetState.hide()
                            showSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WordDisplayCard(
    word: Word?,
    showMeaning: Boolean,
    playbackMode: PlaybackMode,
    onCardClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 300.dp) // Use wrapContent and defaultMinSize
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onCardClicked)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (word != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp) // Removed fillMaxSize
            ) {
                // Word
                if (playbackMode != PlaybackMode.HIDE_ALL && playbackMode != PlaybackMode.CN_ONLY) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Part of Speech
                if (playbackMode != PlaybackMode.HIDE_ALL && playbackMode != PlaybackMode.CN_ONLY && word.wordType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = PartOfSpeechHelper.getChinesePartOfSpeech(word.wordType),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Meaning
                if (showMeaning && playbackMode != PlaybackMode.HIDE_ALL && playbackMode != PlaybackMode.WORD_ONLY) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}