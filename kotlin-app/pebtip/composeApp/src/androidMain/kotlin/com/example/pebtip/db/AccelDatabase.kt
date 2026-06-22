package com.example.pebtip.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AccelSampleEntity::class], version = 12, exportSchema = false)
abstract class AccelDatabase : RoomDatabase() {

    abstract fun accelSampleDao(): AccelSampleDao

    companion object {
        fun create(context: Context): AccelDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AccelDatabase::class.java,
                "accel_samples.db",
            )
            .fallbackToDestructiveMigration(true)
            .build()
        }
    }
}
