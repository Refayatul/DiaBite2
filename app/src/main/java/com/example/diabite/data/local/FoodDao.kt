package com.example.diabite.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.diabite.data.model.CachedFoodItem
import com.example.diabite.data.model.CachedSearch
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // Cached Food Items operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFood(cachedFoodItem: CachedFoodItem)

    @Query("SELECT * FROM cached_foods WHERE id = :foodId")
    suspend fun getFoodById(foodId: String): CachedFoodItem?

    @Query("SELECT * FROM cached_foods ORDER BY lastAccessed DESC")
    suspend fun getAllCachedFoods(): List<CachedFoodItem>

    @Query("SELECT COUNT(*) FROM cached_foods")
    suspend fun getCachedCount(): Int

    @Query("UPDATE cached_foods SET lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE id = :foodId")
    suspend fun updateAccessTime(foodId: String, timestamp: Long)

    @Query("DELETE FROM cached_foods WHERE id IN (SELECT id FROM cached_foods ORDER BY lastAccessed ASC LIMIT :count)")
    suspend fun deleteLeastRecent(count: Int)

    @Query("DELETE FROM cached_foods WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)

    @Query("DELETE FROM cached_foods")
    suspend fun clearAllCache()

    // Search History operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(cachedSearch: CachedSearch)

    @Query("SELECT * FROM search_history WHERE query = :query ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSearchByQuery(query: String): CachedSearch?

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSearches(): List<CachedSearch>

    @Query("DELETE FROM search_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSearches(cutoffTime: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Utility queries
    @Query("SELECT SUM(accessCount) FROM cached_foods")
    suspend fun getTotalAccessCount(): Int

    @Query("SELECT AVG(accessCount) FROM cached_foods")
    suspend fun getAverageAccessCount(): Float
}
