package com.quizhelper.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quizhelper.app.data.model.HistoryDetail
import com.quizhelper.app.data.model.HistoryRecord
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.data.model.QuestionBankMeta

@Database(
    entities = [
        Question::class,
        QuestionBankMeta::class,
        HistoryRecord::class,
        HistoryDetail::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quiz_helper.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
