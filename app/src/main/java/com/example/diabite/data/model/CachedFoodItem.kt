package com.example.diabite.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cached_foods")
data class CachedFoodItem(
    @PrimaryKey
    val id: String,
    val foodItem: FoodItem,
    val lastAccessed: Date = Date(),
    val accessCount: Int = 1,
    val cachedAt: Date = Date()
)
