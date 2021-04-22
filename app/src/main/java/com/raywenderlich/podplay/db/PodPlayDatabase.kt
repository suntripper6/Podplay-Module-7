package com.raywenderlich.podplay.db

import android.content.Context
import androidx.room.*
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}

// Defining database
// 1
@Database(entities = arrayOf(Podcast::class, Episode::class), version = 1)
@TypeConverters(Converters::class)

abstract class PodPlayDatabase : RoomDatabase() {
    // 2
    abstract fun podcastDao(): PodcastDao
    // 3
    companion object {
        // 4
        private var instance: PodPlayDatabase? = null
        // 5
        fun getInstance(context: Context): PodPlayDatabase {
            if (instance == null) {
                // 6
                instance = Room.databaseBuilder(context.applicationContext,
                           PodPlayDatabase::class.java, "PodPlayer").build()
            }
            // 7
            return instance as PodPlayDatabase
        }
    }
}