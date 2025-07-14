package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode

/**
 * 单词卡片
 */
@Composable
fun WordCard(
    word: Word?,
    showMeaning: Boolean,
    playbackMode: PlaybackMode,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 显示单词部分
            if (word != null) {
                if (playbackMode != PlaybackMode.HIDE_ALL && playbackMode != PlaybackMode.CN_ONLY) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                    
                    if (word.wordType.isNotBlank()) {
                        Text(
                            text = word.wordType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 显示意思部分
                if ((playbackMode == PlaybackMode.CN_ONLY || playbackMode == PlaybackMode.WORD_TO_CN && showMeaning)) {
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 播放控制栏
 */
@Composable
fun PlaybackControlBar(
    isPlaying: Boolean,
    isRandom: Boolean,
    isLoop: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRandomToggle: () -> Unit,
    onLoopToggle: () -> Unit
) {
    Column {
        // 播放进度信息
        Text(
            text = "${currentIndex + 1} / $totalCount",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 随机按钮
            IconButton(onClick = onRandomToggle) {
                Icon(
                    imageVector = Icons.Default.ShuffleOn,
                    contentDescription = "随机播放",
                    tint = if (isRandom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // 上一首按钮
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一个",
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // 播放/暂停按钮
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // 下一首按钮
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一个",
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // 循环按钮
            IconButton(onClick = onLoopToggle) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "循环播放",
                    tint = if (isLoop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 播放设置
 */
@Composable
fun PlaybackSettings(
    playbackMode: PlaybackMode,
    playbackSpeed: Float,
    playbackInterval: Float,
    onPlaybackModeChanged: (PlaybackMode) -> Unit,
    onPlaybackSpeedChanged: (Float) -> Unit,
    onPlaybackIntervalChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 播放模式选择
        Text(
            text = "播放模式",
            style = MaterialTheme.typography.titleMedium
        )
        
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            PlaybackModeOption(
                selected = playbackMode == PlaybackMode.WORD_TO_CN,
                text = "单词 -> 中文",
                onClick = { onPlaybackModeChanged(PlaybackMode.WORD_TO_CN) }
            )
            
            PlaybackModeOption(
                selected = playbackMode == PlaybackMode.WORD_ONLY,
                text = "只读单词",
                onClick = { onPlaybackModeChanged(PlaybackMode.WORD_ONLY) }
            )
            
            PlaybackModeOption(
                selected = playbackMode == PlaybackMode.CN_ONLY,
                text = "只读中文",
                onClick = { onPlaybackModeChanged(PlaybackMode.CN_ONLY) }
            )
            
            PlaybackModeOption(
                selected = playbackMode == PlaybackMode.HIDE_ALL,
                text = "隐藏单词和意思",
                onClick = { onPlaybackModeChanged(PlaybackMode.HIDE_ALL) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 播放速度
        Text(
            text = "播放速度: ${String.format("%.1f", playbackSpeed)}x",
            style = MaterialTheme.typography.titleMedium
        )
        
        var localPlaybackSpeed by remember { mutableFloatStateOf(playbackSpeed) }
        Slider(
            value = localPlaybackSpeed,
            onValueChange = { localPlaybackSpeed = it },
            onValueChangeFinished = { onPlaybackSpeedChanged(localPlaybackSpeed) },
            valueRange = 0.5f..2.0f,
            steps = 14
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 播放间隔
        Text(
            text = "播放间隔: ${String.format("%.1f", playbackInterval)}秒",
            style = MaterialTheme.typography.titleMedium
        )
        
        var localPlaybackInterval by remember { mutableFloatStateOf(playbackInterval) }
        Slider(
            value = localPlaybackInterval,
            onValueChange = { localPlaybackInterval = it },
            onValueChangeFinished = { onPlaybackIntervalChanged(localPlaybackInterval) },
            valueRange = 0.5f..5.0f,
            steps = 9
        )
    }
}

/**
 * 播放模式选项
 */
@Composable
private fun PlaybackModeOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 