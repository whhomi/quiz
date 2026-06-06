package com.quizhelper.app.util

import android.util.Log

class Logger(private val tag: String) {
    companion object {
        private const val PREFIX = "QuizHelper"
        fun create(name: String): Logger = Logger("$PREFIX/$name")
    }

    fun d(msg: String) = Log.d(tag, msg)
    fun i(msg: String) = Log.i(tag, msg)
    fun w(msg: String) = Log.w(tag, msg)
    fun e(msg: String, tr: Throwable? = null) {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    }
}
