package com.yourcompany.worklisten.utils

/**
 * 单词解析工具类
 * 用于解析日语单词、英语单词和中文意思等
 */
object WordParser {
    // Regex to find all occurrences of part-of-speech tags.
    private val POS_FINDER_REGEX = Regex("\\b(adv|adj|art|aux|conj|int|n|num|prep|pron|v|vi|vt)\\b\\.?", RegexOption.IGNORE_CASE)

    // General patterns for cleanup and identification
    private val CLEANUP_BRACKETS_PATTERN = Regex("\\[(.*?)\\]") // To remove content like [交]
    private val JAPANESE_CHAR_PATTERN = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF]+")
    private val JAPANESE_WORD_MAIN_PART_PATTERN = Regex("^([\\u3040-\\u309F\\u30A0-\\u30FFー]+)(?:【.+】|\\s+[\\u4E00-\\u9FFF]+)?")
    private val ENGLISH_WORD_PATTERN = Regex("^[a-zA-Z]+(?:[-'][a-zA-Z]+)*$")

    /**
     * A robust parsing function that handles simple (single POS) and complex (multiple POS) cases.
     */
    fun parseMeaningAndType(rawText: String): Pair<String?, String> {
        var text = rawText.trim().removeSurrounding("\"").replace("\r\n", " ").replace("\n", " ")
        text = CLEANUP_BRACKETS_PATTERN.replace(text, "").trim()

        val matches = POS_FINDER_REGEX.findAll(text).toList()

        // Case 1: No POS tags found.
        if (matches.isEmpty()) {
            return Pair(null, cleanMeaningString(text))
        }

        // Case 2: Exactly one POS tag at the start. This is a simple case.
        // We allow it to be slightly indented (up to 2 chars).
        if (matches.size == 1 && matches.first().range.first < 2) {
            val match = matches.first()
            var wordType = match.value
            if (!wordType.endsWith(".")) wordType += "."
            
            val meaning = text.substring(match.range.last + 1)
            return Pair(wordType, cleanMeaningString(meaning))
        }

        // Case 3: Multiple POS tags, or a single tag not at the beginning. This is a complex case.
        // We segment the string by the found POS tags and join them with newlines.
        val matchIndices = matches.map { it.range.first }
        
        val segments = mutableListOf<String>()
        for (i in matchIndices.indices) {
            val start = matchIndices[i]
            val end = if (i + 1 < matchIndices.size) matchIndices[i + 1] else text.length
            val segment = text.substring(start, end).trim()
            if (segment.isNotBlank()) {
                segments.add(segment)
            }
        }
        
        val finalMeaning = segments.joinToString("\n")
        
        return Pair(null, cleanMeaningString(finalMeaning, preserveNewlines = true))
    }
    
    /**
     * Helper function to normalize and clean up meaning strings.
     */
    private fun cleanMeaningString(meaning: String, preserveNewlines: Boolean = false): String {
        val lines = if (preserveNewlines) meaning.lines() else listOf(meaning)

        return lines.joinToString(if (preserveNewlines) "\n" else "； ") { line ->
            line.replace(Regex("[;；。]"), "；")
                .split("；")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("； ")
        }.trim()
    }

    fun isJapaneseWord(word: String): Boolean {
        return JAPANESE_CHAR_PATTERN.containsMatchIn(word) && !ENGLISH_WORD_PATTERN.matches(word)
    }

    fun extractKana(japaneseWord: String): String {
        val matcher = JAPANESE_WORD_MAIN_PART_PATTERN.find(japaneseWord.trim())
        return matcher?.groups?.get(1)?.value ?: japaneseWord
    }
} 