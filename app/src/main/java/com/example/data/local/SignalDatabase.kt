package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SignalHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun signalHistoryDao(): SignalHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: SignalDatabase? = null

        fun getInstance(context: Context): SignalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SignalDatabase::class.java,
                    "signalpulse_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
