package com.example.diabite.data.local

import com.example.diabite.data.model.CachedFoodItem
import com.example.diabite.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    private val foodDao: FoodDao
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Cache a food item with automatic space management
     */
    suspend fun cacheFoodItem(foodItem: FoodItem) {
        try {
            // Check if we need to clean up before inserting
            ensureCacheSpace()

            val cachedItem = CachedFoodItem(
                id = foodItem.id,
                foodItem = foodItem,
                lastAccessed = Date(),
                accessCount = 1,
                cachedAt = Date()
            )

            foodDao.insertOrUpdateFood(cachedItem)
        } catch (e: Exception) {
            // Log error but don't crash - cache is optional
            println("CacheManager: Failed to cache food item ${foodItem.id}: ${e.message}")
        }
    }

    /**
     * Get a cached food item and update access statistics
     */
    suspend fun getCachedFoodItem(foodId: String): FoodItem? {
        return try {
            val cachedItem = foodDao.getFoodById(foodId)
            if (cachedItem != null) {
                // Update access time and count
                foodDao.updateAccessTime(foodId, Date().time)
                cachedItem.foodItem
            } else {
                null
            }
        } catch (e: Exception) {
            println("CacheManager: Failed to get cached food item $foodId: ${e.message}")
            null
        }
    }

    /**
     * Ensure there's enough space in cache before adding new items
     */
    private suspend fun ensureCacheSpace() {
        try {
            val currentCount = foodDao.getCachedCount()

            // If we're approaching the limit, clean up
            if (currentCount >= CacheConfig.CACHE_CLEANUP_THRESHOLD) {
                val itemsToRemove = currentCount - CacheConfig.MAX_CACHED_FOODS + 10 // Remove 10 extra for buffer
                if (itemsToRemove > 0) {
                    foodDao.deleteLeastRecent(itemsToRemove)
                }
            }

            // Clean up expired items
            val expiryTime = Date().time - CacheConfig.CACHE_EXPIRY_MS
            foodDao.deleteExpired(expiryTime)

            // Clean up old search history
            val searchExpiryTime = Date().time - CacheConfig.SEARCH_HISTORY_EXPIRY_MS
            foodDao.deleteOldSearches(searchExpiryTime)

        } catch (e: Exception) {
            println("CacheManager: Failed to ensure cache space: ${e.message}")
        }
    }

    /**
     * Clean up expired cache entries (called periodically)
     */
    suspend fun cleanupExpiredCache() {
        try {
            val expiryTime = Date().time - CacheConfig.CACHE_EXPIRY_MS
            foodDao.deleteExpired(expiryTime)

            val searchExpiryTime = Date().time - CacheConfig.SEARCH_HISTORY_EXPIRY_MS
            foodDao.deleteOldSearches(searchExpiryTime)
        } catch (e: Exception) {
            println("CacheManager: Failed to cleanup expired cache: ${e.message}")
        }
    }

    /**
     * Clear all cache (useful for debugging or user request)
     */
    suspend fun clearAllCache() {
        try {
            foodDao.clearAllCache()
            foodDao.clearSearchHistory()
        } catch (e: Exception) {
            println("CacheManager: Failed to clear cache: ${e.message}")
        }
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats {
        return try {
            val count = foodDao.getCachedCount()
            val totalAccess = foodDao.getTotalAccessCount()
            val avgAccess = foodDao.getAverageAccessCount()

            CacheStats(
                cachedItemsCount = count,
                totalAccessCount = totalAccess,
                averageAccessCount = avgAccess,
                cacheUtilizationPercent = (count.toFloat() / CacheConfig.MAX_CACHED_FOODS) * 100
            )
        } catch (e: Exception) {
            println("CacheManager: Failed to get cache stats: ${e.message}")
            CacheStats(0, 0, 0f, 0f)
        }
    }

    /**
     * Asynchronous cache operation launcher
     */
    fun cacheFoodItemAsync(foodItem: FoodItem) {
        scope.launch {
            cacheFoodItem(foodItem)
        }
    }

    /**
     * Search cached foods by a simple normalized name match.
     * This is intentionally lightweight and best-effort — used before remote lookups.
     */
    suspend fun searchCachedFoods(query: String): List<FoodItem> {
        return try {
            if (query.isBlank()) return emptyList()

            val normalizedQuery = query.lowercase().trim()
            val all = foodDao.getAllCachedFoods()

            all.mapNotNull { it.foodItem }
                .filter { item ->
                    val name = item.name.lowercase()
                    val normalizedName = item.normalizedName.lowercase()
                    name.contains(normalizedQuery) || normalizedName.contains(normalizedQuery)
                }
        } catch (e: Exception) {
            println("CacheManager: Failed to search cache: ${e.message}")
            emptyList()
        }
    }

    /**
     * Periodic cleanup launcher
     */
    fun scheduleCleanup() {
        scope.launch {
            cleanupExpiredCache()
        }
    }
}

data class CacheStats(
    val cachedItemsCount: Int,
    val totalAccessCount: Int,
    val averageAccessCount: Float,
    val cacheUtilizationPercent: Float
)
