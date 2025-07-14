package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词实体，表示单个单词的所有信息
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordLibrary::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["libraryId"])
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryId: Long,
    val groupId: Int, // 50个单词一组
    val word: String, // 单词原形，日语单词时存储假名形式
    val originalWord: String, // 原始单词，包含汉字等
    val meaning: String, // 中文含义，最多保留三个
    val wordType: String, // 词性标记，如 "n."、"v."、"自五" 等
    val isJapanese: Boolean, // 是否为日语单词
    val hasListened: Boolean = false, // 是否已经听过
    val hasReviewed: Boolean = false // 是否已经复习过
) 