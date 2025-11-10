package com.example.diabite.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "search_history")
data class CachedSearch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val results: List<String>, // List of food IDs
    val timestamp: Date = Date(),
    val resultCount: Int = results.size
)
