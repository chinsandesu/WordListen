package com.yourcompany.worklisten.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordChapter
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.local.model.WordLibrary
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.mozilla.universalchardet.UniversalDetector
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * 文件导入工具类
 */
class FileImporter(private val context: Context) {

    // 内部数据类，用于临时存储解析出的单词信息
    private data class ParsedWord(
        val originalWord: String,
        val meaning: String,
        val wordType: String?
    )

    companion object {
        private const val WORDS_PER_GROUP = 50
        private const val GROUPS_PER_CHAPTER = 10
    }

    /**
     * 从Assets目录导入文件
     */
    fun importFromAssets(fileName: String, libraryName: String): ImportResult {
        return try {
            val inputStream = context.assets.open(fileName)
            when {
                fileName.endsWith(".csv", true) -> importCsv(inputStream, libraryName, ',')
                fileName.endsWith(".txt", true) -> importTxt(inputStream, libraryName)
                else -> ImportResult.Error("不支持的文件类型")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error("从Assets导入失败: ${e.message}")
        }
    }

    /**
     * 导入TXT文件
     */
    private fun importTxt(
        inputStream: InputStream,
        libraryName: String
    ): ImportResult {
        try {
            val reader = detectCharsetAndGetReader(inputStream)
            val parsedWords = mutableListOf<ParsedWord>()
            // Regex to split word from the rest of the line. It handles various whitespaces.
            val lineSplitRegex = Regex("\\s{2,}|\\t")

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isBlank()) return@forEach

                    val parts = line.split(lineSplitRegex, 2)
                    if (parts.size < 2) return@forEach

                    val wordPart = parts[0].trim()
                    val rawMeaningPart = parts[1].trim()

                    if (wordPart.isBlank() || rawMeaningPart.isBlank()) return@forEach

                    // Use WordParser for a more robust extraction of type and meaning
                    val (wordType, finalMeaning) = WordParser.parseMeaningAndType(rawMeaningPart)

                    parsedWords.add(ParsedWord(wordPart, finalMeaning, wordType))
                }
            }
            return processParsedWords(libraryName, parsedWords)
        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Error("导入TXT失败: ${e.message}")
        }
    }


    /**
     * 导入CSV文件
     */
    fun importCsvFile(
        uri: Uri,
        libraryName: String,
        delimiter: Char = ','
    ): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("无法打开文件")
            importCsv(inputStream, libraryName, delimiter)
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error("导入失败: ${e.message}")
        }
    }

    private fun importCsv(
        inputStream: InputStream,
        libraryName: String,
        delimiter: Char
    ): ImportResult {
        try {
            val reader = detectCharsetAndGetReader(inputStream)
            val parsedWords = mutableListOf<ParsedWord>()
            val csvReader = CSVReaderBuilder(reader)
                .withCSVParser(CSVParserBuilder().withSeparator(delimiter).build())
                .withSkipLines(0)
                .build()

            var wordColumn = 0
            var meaningColumn = 1

            // 读取CSV数据
            val rows = csvReader.readAll()
            if (rows.isEmpty()) {
                return ImportResult.Error("文件为空")
            }
            csvReader.close()

            // 处理每一行数据
            rows.forEachIndexed { _, row ->
                if (row.size < 2) return@forEachIndexed

                val wordText = row[wordColumn].trim()
                val meaningText = row[meaningColumn].trim()

                if (wordText.isBlank() || meaningText.isBlank()) return@forEachIndexed

                // 解析单词
                val (wordType, finalMeaning) = WordParser.parseMeaningAndType(meaningText)
                parsedWords.add(ParsedWord(wordText, finalMeaning, wordType))
            }

            return processParsedWords(libraryName, parsedWords)

        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Error("导入失败: ${e.message}")
        }
    }

    /**
     * 导入Excel文件
     */
    fun importExcelFile(
        uri: Uri,
        libraryName: String
    ): ImportResult {
        try {
            val parsedWords = mutableListOf<ParsedWord>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ImportResult.Error("无法打开文件")

            // 使用Apache POI解析Excel文件
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // 只读取第一个工作表

            // 遍历行
            for (row in sheet) {
                if (row.physicalNumberOfCells < 2) continue
                val wordCell = row.getCell(0)
                val meaningCell = row.getCell(1)

                val wordText = wordCell?.stringCellValue?.trim()
                val meaningText = meaningCell?.stringCellValue?.trim()

                if (!wordText.isNullOrBlank() && !meaningText.isNullOrBlank()) {
                    val (wordType, finalMeaning) = WordParser.parseMeaningAndType(meaningText)
                    parsedWords.add(ParsedWord(wordText, finalMeaning, wordType))
                }
            }
            workbook.close()
            inputStream.close()

            return processParsedWords(libraryName, parsedWords)

        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Error("导入Excel失败: ${e.message}")
        }
    }

    /**
     * 核心处理逻辑: 从解析出的单词列表创建数据库实体
     */
    private fun processParsedWords(
        libraryName: String,
        parsedWords: List<ParsedWord>
    ): ImportResult {
        val words = mutableListOf<Word>()
        val groups = mutableListOf<WordGroup>()
        val chapters = mutableListOf<WordChapter>()

        var wordCount = 0
        var skippedCount = 0
        val detectedWords = mutableSetOf<String>()

        var currentGroupWordCount = 0
        var currentGroupId = 0
        var currentChapterGroupCount = 0
        var currentChapterId = 1L // 临时章节ID，将在保存后更新

        // 创建第一个章节
        var currentChapter = WordChapter(
            id = 0,
            libraryId = 0,
            chapterNumber = 1,
            title = "第 1 章 (1-${GROUPS_PER_CHAPTER}组)"
        )
        chapters.add(currentChapter)

        parsedWords.forEach { parsedWord ->
            if (parsedWord.originalWord.isBlank() || parsedWord.meaning.isBlank()) {
                return@forEach
            }

            if (detectedWords.contains(parsedWord.originalWord)) {
                skippedCount++
                return@forEach
            }
            detectedWords.add(parsedWord.originalWord)

            val isJapanese = WordParser.isJapaneseWord(parsedWord.originalWord)

            val word = Word(
                libraryId = 0,
                groupId = currentGroupId,
                word = if (isJapanese) WordParser.extractKana(parsedWord.originalWord) else parsedWord.originalWord,
                originalWord = parsedWord.originalWord,
                meaning = parsedWord.meaning,
                wordType = parsedWord.wordType ?: "",
                isJapanese = isJapanese
            )
            words.add(word)
            wordCount++
            currentGroupWordCount++

            // 当一个组满时，创建新的组
            if (currentGroupWordCount >= WORDS_PER_GROUP) {
                groups.add(
                    WordGroup(
                        libraryId = 0,
                        chapterId = 0, // 临时ID
                        groupId = currentGroupId,
                        wordCount = currentGroupWordCount
                    )
                )

                currentGroupId++
                currentGroupWordCount = 0
                currentChapterGroupCount++

                // 当一个章节满时，创建新的章节
                if (currentChapterGroupCount >= GROUPS_PER_CHAPTER) {
                    val chapterNumber = chapters.size + 1
                    val startGroup = (chapterNumber - 1) * GROUPS_PER_CHAPTER + 1
                    val endGroup = chapterNumber * GROUPS_PER_CHAPTER

                    currentChapter = WordChapter(
                        id = 0,
                        libraryId = 0,
                        chapterNumber = chapterNumber,
                        title = "第 $chapterNumber 章 (${startGroup}-${endGroup}组)"
                    )
                    chapters.add(currentChapter)
                    currentChapterGroupCount = 0
                }
            }
        }

        // 添加最后一个未满的组
        if (currentGroupWordCount > 0) {
            groups.add(
                WordGroup(
                    libraryId = 0,
                    chapterId = 0,
                    groupId = currentGroupId,
                    wordCount = currentGroupWordCount
                )
            )
        }

        val library = WordLibrary(
            name = libraryName,
            wordCount = wordCount,
            groupCount = groups.size,
            chapterCount = chapters.size,
            isActive = false
        )

        return ImportResult.Success(library, words, groups, chapters, wordCount, skippedCount)
    }


    /**
     * 根据文件扩展名导入文件
     */
    fun importFile(
        uri: Uri,
        libraryName: String,
        delimiter: Char = ','
    ): ImportResult {
        val fileName = getFileNameFromUri(uri) ?: return ImportResult.Error("无法获取文件名")

        return when {
            fileName.endsWith(".csv", true) -> importCsvFile(uri, libraryName, delimiter)
            fileName.endsWith(".txt", true) -> importTxtFile(uri, libraryName) // 新增对用户TXT文件的支持
            fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) -> importExcelFile(uri, libraryName)
            else -> ImportResult.Error("不支持的文件类型")
        }
    }

    /**
     *  导入用户TXT文件
     */
    private fun importTxtFile(uri: Uri, libraryName: String): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("无法打开文件")
            importTxt(inputStream, libraryName)
        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult.Error("导入TXT失败: ${e.message}")
        }
    }

    /**
     * 从Uri中获取文件名
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex("_display_name")
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * 检测输入流的字符集并返回一个Reader
     */
    private fun detectCharsetAndGetReader(inputStream: InputStream): InputStreamReader {
        val buffer = inputStream.use { it.readBytes() }
        val detector = UniversalDetector(null)

        detector.handleData(buffer, 0, buffer.size)
        detector.dataEnd()

        val detectedCharset = detector.detectedCharset
        detector.reset()

        val charset = if (detectedCharset != null && Charset.isSupported(detectedCharset)) {
            Charset.forName(detectedCharset)
        } else {
            Charsets.UTF_8 // 默认回退到UTF-8
        }

        return buffer.inputStream().reader(charset)
    }

    /**
     * 导入结果的密封类
     */
    sealed class ImportResult {
        data class Success(
            val library: WordLibrary,
            val words: List<Word>,
            val groups: MutableList<WordGroup>,
            val chapters: MutableList<WordChapter>,
            val importedCount: Int,
            val skippedCount: Int
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }
} 