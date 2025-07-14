package com.yourcompany.worklisten.utils

object PartOfSpeechHelper {

    fun getChinesePartOfSpeech(pos: String?): String {
        if (pos.isNullOrBlank()) {
            return ""
        }
        return when (pos.trim()) {
            // English
            "n.", "n" -> "名词"
            "v.", "v" -> "动词"
            "vt." -> "及物动词"
            "vi." -> "不及物动词"
            "adj.", "adj" -> "形容词"
            "adv.", "adv" -> "副词"
            "prep.", "prep" -> "介词"
            "conj.", "conj" -> "连词"
            "pron.", "pron" -> "代词"
            "num.", "num" -> "数词"
            "art.", "art" -> "冠词"
            "int.", "int" -> "感叹词"

            // Japanese
            "名" -> "名词"
            "形动" -> "形容动词"
            "自五" -> "自动词（五段）"
            "他五" -> "他动词（五段）"
            "他サ" -> "他动词（サ变）"
            "自サ" -> "自动词（サ变）"
            "自一" -> "自动词（一段）"
            "他一" -> "他动词（一段）"
            
            else -> pos // Return original if no match
        }
    }
} 