package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.ReviewDisplayMode
import com.yourcompany.worklisten.utils.PartOfSpeechHelper
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.heightIn

/**
 * 复习单词卡片
 */
@Composable
fun ReviewWordCard(
    word: Word,
    displayMode: ReviewDisplayMode,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .heightIn(max = 240.dp), // Set a max height for the card
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()), // Make content scrollable if it overflows
                verticalArrangement = Arrangement.spacedBy(2.dp) // Reduce space between text elements
            ) {
                // Word
                if (displayMode != ReviewDisplayMode.HIDE_WORD) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp // Slightly smaller font size
                        )
                    )
                }

                // Part of Speech
                if (displayMode != ReviewDisplayMode.HIDE_WORD && word.wordType.isNotBlank()) {
                    Text(
                        text = PartOfSpeechHelper.getChinesePartOfSpeech(word.wordType),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Meaning
                if (displayMode != ReviewDisplayMode.HIDE_MEANING) {
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp // Improve readability for long text
                    )
                }
            }
            // Spacer to push the icon to the right
            Spacer(modifier = Modifier.width(16.dp))
            // Speak Icon
            IconButton(onClick = onSpeak) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Speak Word",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * 显示模式选择栏
 */
@Composable
fun DisplayModeSelector(
    currentMode: ReviewDisplayMode,
    onModeSelected: (ReviewDisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DisplayModeOption(
            text = "显示全部",
            selected = currentMode == ReviewDisplayMode.SHOW_ALL,
            onClick = { onModeSelected(ReviewDisplayMode.SHOW_ALL) }
        )

        DisplayModeOption(
            text = "隐藏单词",
            selected = currentMode == ReviewDisplayMode.HIDE_WORD,
            onClick = { onModeSelected(ReviewDisplayMode.HIDE_WORD) }
        )

        DisplayModeOption(
            text = "隐藏意思",
            selected = currentMode == ReviewDisplayMode.HIDE_MEANING,
            onClick = { onModeSelected(ReviewDisplayMode.HIDE_MEANING) }
        )
    }
}

/**
 * 显示模式选项
 */
@Composable
private fun DisplayModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
} 