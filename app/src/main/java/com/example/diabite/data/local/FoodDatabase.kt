package com.example.diabite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.diabite.data.model.CachedFoodItem
import com.example.diabite.data.model.CachedSearch

@Database(
    entities = [CachedFoodItem::class, CachedSearch::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        const val DATABASE_NAME = "food_cache.db"
    }
}
