package com.quizhelper.app.data.parser

import android.content.Context
import android.net.Uri
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.data.model.QuestionType
import com.quizhelper.app.util.Logger
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes

/**
 * Ultra-fast xlsx parser using Android's built-in SAX parser + ZipInputStream.
 * xlsx is just a zip of XML files — no need for Apache POI.
 */
object ExcelParser {
    private val log = Logger.create("ExcelParser")

    data class ParseResult(val questions: List<Question>, val warnings: List<String>)

    data class ColumnMap(
        val type: Int = -1, val stem: Int = -1,
        val optionA: Int = -1, val optionB: Int = -1,
        val optionC: Int = -1, val optionD: Int = -1,
        val optionE: Int = -1, val optionF: Int = -1,
        val answer: Int = -1, val analysis: Int = -1
    )

    private val headerAliases = mapOf(
        "type" to listOf("题型", "type", "题目类型", "题目类别"),
        "stem" to listOf("题干", "stem", "题目", "问题", "题目内容"),
        "optionA" to listOf("选项A", "optionA", "A", "选项 A", "选项a"),
        "optionB" to listOf("选项B", "optionB", "B", "选项 B", "选项b"),
        "optionC" to listOf("选项C", "optionC", "C", "选项 C", "选项c"),
        "optionD" to listOf("选项D", "optionD", "D", "选项 D", "选项d"),
        "optionE" to listOf("选项E", "optionE", "E", "选项 E", "选项e"),
        "optionF" to listOf("选项F", "optionF", "F", "选项 F", "选项f"),
        "answer" to listOf("正确答案", "answer", "答案", "正确选项", "答案选项"),
        "analysis" to listOf("解析", "analysis", "题目解析", "答案解析", "新题依据", "备注", "备注说明")
    )

    fun parse(context: Context, uri: Uri): ParseResult {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法打开文件")

        val rows = readXlsxRows(inputStream)
        inputStream.close()

        if (rows.size < 2) {
            throw IllegalStateException("Excel 文件中没有足够的数据（至少需要表头和一行数据）")
        }

        val headerRowIndex = findHeaderRow(rows)
        val headers = rows[headerRowIndex]
        val colMap = detectColumns(headers)

        if (colMap.type == -1) throw IllegalStateException("缺少必填列: 题型")
        if (colMap.stem == -1) throw IllegalStateException("缺少必填列: 题干")
        if (colMap.optionA == -1) throw IllegalStateException("缺少必填列: 选项A")
        if (colMap.optionB == -1) throw IllegalStateException("缺少必填列: 选项B")
        if (colMap.answer == -1) throw IllegalStateException("缺少必填列: 正确答案")

        val questions = mutableListOf<Question>()
        val warnings = mutableListOf<String>()
        val optionKeys = listOf("optionA", "optionB", "optionC", "optionD", "optionE", "optionF")

        for (i in (headerRowIndex + 1) until rows.size) {
            val row = rows[i]
            if (row.all { it.isBlank() }) continue

            val stem = row.getOrElse(colMap.stem) { "" }
            if (stem.isBlank()) { warnings.add("第 ${i + 1} 行: 题干为空"); continue }

            val options = mutableListOf<String>()
            var filledCount = 0
            for (key in optionKeys) {
                val colIdx = colOf(key, colMap)
                if (colIdx >= 0) {
                    val opt = row.getOrElse(colIdx) { "" }
                    options.add(opt)
                    if (opt.isNotBlank()) filledCount++
                }
            }
            if (filledCount < 2) { warnings.add("第 ${i + 1} 行: 至少需要两个非空选项"); continue }

            while (options.isNotEmpty() && options.last().isBlank()) options.removeAt(options.size - 1)

            val rawAnswer = row.getOrElse(colMap.answer) { "" }
            val answer = parseAnswer(rawAnswer, options.size)

            val typeRaw = row.getOrElse(colMap.type) { "" }
            val type = when {
                typeRaw.contains("判断") -> QuestionType.BOOLEAN
                typeRaw.contains("多选") -> QuestionType.MULTIPLE
                answer.size > 1 -> QuestionType.MULTIPLE
                else -> QuestionType.SINGLE
            }

            if (answer.isEmpty() && rawAnswer.isNotBlank()) {
                warnings.add("第 ${i + 1} 行: 答案格式无法解析（已导入，答案为空）")
            }

            val analysis = if (colMap.analysis >= 0) row.getOrElse(colMap.analysis) { "" } else ""

            questions.add(Question.create(
                questionId = "q${questions.size + 1}",
                type = type, stem = stem, options = options,
                answer = answer, analysis = analysis
            ))
        }

        if (questions.isEmpty()) {
            val msg = if (warnings.isNotEmpty()) warnings.joinToString("; ") else "未解析到有效题目"
            throw IllegalStateException(msg)
        }
        if (questions.size > 10000) {
            throw IllegalStateException("题库超过 10000 题限制（当前 ${questions.size} 题）")
        }

        log.i("解析完成: ${questions.size} 题, ${warnings.size} 个警告")
        return ParseResult(questions, warnings)
    }

    /**
     * Read xlsx as zip → extract sheet1.xml → parse with SAX → rows of strings.
     * First reads each entry into a byte array, then parses — avoids ZipInputStream
     * boundary issues with SAX.
     */
    private fun readXlsxRows(inputStream: InputStream): List<List<String>> {
        val zip = ZipInputStream(inputStream)
        val entries = mutableMapOf<String, ByteArray>()

        // Read all entries into memory first
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            val baos = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var len: Int
            while (zip.read(buf, 0, buf.size).also { len = it } != -1) {
                baos.write(buf, 0, len)
            }
            entries[name] = baos.toByteArray()
            zip.closeEntry()
        }
        zip.close()

        // Parse shared strings
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { data ->
            val parser = SAXParserFactory.newInstance().newSAXParser()
            val handler = object : DefaultHandler() {
                private val chars = StringBuilder()
                var inT = false
                override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                    if (qName == "t") inT = true
                }
                override fun characters(ch: CharArray, start: Int, length: Int) {
                    if (inT) chars.append(ch, start, length)
                }
                override fun endElement(uri: String, localName: String, qName: String) {
                    if (qName == "t") { sharedStrings.add(chars.toString()); chars.clear(); inT = false }
                }
            }
            parser.parse(java.io.ByteArrayInputStream(data), handler)
        }

        // Parse sheet data
        val sheetData = entries["xl/worksheets/sheet1.xml"]
            ?: throw IllegalStateException("无法找到工作表数据（sheet1.xml）")
        val rows = mutableListOf<List<String>>()
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val currentRow = mutableListOf<String>()
        val cellChars = StringBuilder()
        var inCell = false
        var inValue = false
        var cellType = ""
        var colLetter = ""
        var maxCol = 0

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String, localName: String, qName: String, atts: Attributes) {
                when (qName) {
                    "c" -> {
                        colLetter = atts.getValue("r")?.replace(Regex("\\d+"), "") ?: "A"
                        cellType = atts.getValue("t") ?: ""
                        inCell = true; cellChars.setLength(0)
                    }
                    "v" -> if (inCell) inValue = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inValue) cellChars.append(ch, start, length)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                when (qName) {
                    "v" -> {
                        inValue = false
                        val text = cellChars.toString().trim()
                        val idx = letterToCol(colLetter)
                        while (currentRow.size <= idx) currentRow.add("")
                        if (cellType == "s" && text.isNotEmpty()) {
                            val si = text.toIntOrNull()
                            if (si != null && si < sharedStrings.size) currentRow[idx] = sharedStrings[si]
                        } else {
                            currentRow[idx] = text
                        }
                        if (idx > maxCol) maxCol = idx
                    }
                    "c" -> inCell = false
                    "row" -> {
                        while (currentRow.size > maxCol + 1) currentRow.removeAt(currentRow.lastIndex)
                        rows.add(currentRow.toList())
                        currentRow.clear(); maxCol = 0
                    }
                }
            }
        }
        parser.parse(java.io.ByteArrayInputStream(sheetData), handler)

        return rows
    }

    private fun letterToCol(letter: String): Int {
        var col = 0
        for (ch in letter.uppercase()) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun colOf(key: String, map: ColumnMap): Int = when (key) {
        "optionA" -> map.optionA; "optionB" -> map.optionB
        "optionC" -> map.optionC; "optionD" -> map.optionD
        "optionE" -> map.optionE; "optionF" -> map.optionF
        else -> -1
    }

    fun parseAnswer(raw: String, optionCount: Int): List<Int> {
        if (raw.isBlank()) return emptyList()
        val s = raw.trim()
        if (Regex("^(正确|对|true)", RegexOption.IGNORE_CASE).containsMatchIn(s)) return listOf(0)
        if (Regex("^(错误|错|false)", RegexOption.IGNORE_CASE).containsMatchIn(s)) return listOf(1)
        val upper = s.uppercase()
        if (Regex("^[A-Z]+$").matches(upper)) return upper.map { it - 'A' }.filter { it in 0 until optionCount }
        if (Regex("^[A-Z]([\\s,，、]+[A-Z])*$").matches(upper))
            return upper.split(Regex("[\\s,，、]+")).map { it[0] - 'A' }.filter { it in 0 until optionCount }
        if (s.replace(Regex("[,，、\\s]"), "").matches(Regex("^\\d+$")))
            return s.split(Regex("[,，、\\s]+")).mapNotNull { it.toIntOrNull() }.filter { it in 0 until optionCount }
        val stripped = s.replace(Regex("[（(][^）)]*[）)]"), "").replace(Regex("[（(].*$"), "").trim()
        if (stripped.isNotBlank() && stripped != s) {
            val retry = extractLetters(stripped, optionCount)
            if (retry.isNotEmpty()) return retry
        }
        val letterMatch = Regex("[A-Z]{1,6}([\\s,，、]+[A-Z])*", RegexOption.IGNORE_CASE).find(s)
        if (letterMatch != null) return extractLetters(letterMatch.value, optionCount)
        return emptyList()
    }

    private fun extractLetters(s: String, optionCount: Int): List<Int> {
        val upper = s.uppercase().replace(Regex("[^A-Z]"), "")
        if (upper.isEmpty()) return emptyList()
        return upper.map { it - 'A' }.filter { it in 0 until optionCount }
    }

    private fun findHeaderRow(rows: List<List<String>>, maxScan: Int = 10): Int {
        val keywords = listOf("题型", "题干", "选项", "答案", "解析", "备注",
            "type", "stem", "option", "answer", "analysis", "题目", "问题")
        var bestIdx = 0; var bestScore = 0
        val limit = minOf(maxScan, rows.size)
        for (i in 0 until limit) {
            val score = rows[i].sumOf { cell ->
                val cl = cell.lowercase()
                keywords.count { cl.contains(it.lowercase()) }
            }
            if (score > bestScore) { bestScore = score; bestIdx = i }
        }
        return bestIdx
    }

    private fun detectColumns(headers: List<String>): ColumnMap {
        var map = ColumnMap()
        headers.forEachIndexed { i, h ->
            val clean = h.trim().lowercase()
            for ((key, aliases) in headerAliases) {
                if (aliases.any { it.lowercase() == clean }) {
                    if (getCol(map, key) == -1) map = setCol(map, key, i)
                    break
                }
            }
        }
        return map
    }

    private fun getCol(map: ColumnMap, key: String): Int = when (key) {
        "type" -> map.type; "stem" -> map.stem; "optionA" -> map.optionA
        "optionB" -> map.optionB; "optionC" -> map.optionC; "optionD" -> map.optionD
        "optionE" -> map.optionE; "optionF" -> map.optionF; "answer" -> map.answer
        "analysis" -> map.analysis; else -> -1
    }

    private fun setCol(map: ColumnMap, key: String, value: Int): ColumnMap = when (key) {
        "type" -> map.copy(type = value); "stem" -> map.copy(stem = value)
        "optionA" -> map.copy(optionA = value); "optionB" -> map.copy(optionB = value)
        "optionC" -> map.copy(optionC = value); "optionD" -> map.copy(optionD = value)
        "optionE" -> map.copy(optionE = value); "optionF" -> map.copy(optionF = value)
        "answer" -> map.copy(answer = value); "analysis" -> map.copy(analysis = value)
        else -> map
    }
}
