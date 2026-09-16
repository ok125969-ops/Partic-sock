package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UltronMemoryEntity::class], version = 1, exportSchema = false)
abstract class UltronDatabase : RoomDatabase() {
    abstract fun ultronDao(): UltronDao

    companion object {
        @Volatile
        private var INSTANCE: UltronDatabase? = null

        fun getDatabase(context: Context): UltronDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UltronDatabase::class.java,
                    "ultron_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
