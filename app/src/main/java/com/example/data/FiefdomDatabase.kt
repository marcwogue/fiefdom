package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, CommunityMember::class, TaxLog::class, GameState::class],
    version = 3,
    exportSchema = false
)
abstract class FiefdomDatabase : RoomDatabase() {

    abstract fun fiefdomDao(): FiefdomDao

    companion object {
        @Volatile
        private var INSTANCE: FiefdomDatabase? = null

        fun getDatabase(context: Context): FiefdomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FiefdomDatabase::class.java,
                    "fiefdom_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
