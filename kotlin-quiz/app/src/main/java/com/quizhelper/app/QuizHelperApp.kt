package com.quizhelper.app

import android.app.Application
import com.quizhelper.app.data.db.AppDatabase
import com.quizhelper.app.util.Logger

class QuizHelperApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val log = Logger.create("App")

    override fun onCreate() {
        super.onCreate()
        instance = this
        log.i("QuizHelperApp 启动")
    }

    companion object {
        lateinit var instance: QuizHelperApp
            private set
    }
}
