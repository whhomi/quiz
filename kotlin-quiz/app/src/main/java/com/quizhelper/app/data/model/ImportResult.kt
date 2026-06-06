package com.quizhelper.app.data.model

data class ImportResult(
    val success: Boolean,
    val message: String,
    val totalCount: Int = 0,
    val singleCount: Int = 0,
    val multipleCount: Int = 0,
    val booleanCount: Int = 0,
    val warnings: List<String> = emptyList()
)
