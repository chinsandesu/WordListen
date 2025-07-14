package com.yourcompany.worklisten.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode

@Composable
fun WordCard(
    word: Word?,
    playbackMode: PlaybackMode,
    showMeaning: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp) // 增加卡片高度以适应更大的字体
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = word,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "WordCardContent"
            ) { targetWord ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly // 均匀分配空间
                ) {
                    // 单词区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetWord != null && playbackMode != PlaybackMode.CN_ONLY) {
                            Text(
                                text = targetWord.word,
                                style = MaterialTheme.typography.headlineLarge, // 使用更大的字号
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // 释义区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetWord != null && showMeaning && playbackMode != PlaybackMode.WORD_ONLY) {
                            Text(
                                text = targetWord.meaning,
                                style = MaterialTheme.typography.titleLarge, // 使用更大的字号
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
} 