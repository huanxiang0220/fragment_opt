package com.mystery.fragment_opt.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FragmentStateEntity::class], version = 1, exportSchema = false)
abstract class OptDatabase : RoomDatabase() {
    
    abstract fun fragmentStateDao(): FragmentStateDao
    
    companion object {
        @Volatile
        private var INSTANCE: OptDatabase? = null
        
        fun getDatabase(context: Context): OptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OptDatabase::class.java,
                    "fragment_opt_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
