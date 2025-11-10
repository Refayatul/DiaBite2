package com.example.diabite.data.local

object CacheConfig {
    const val MAX_CACHED_FOODS = 200
    const val CACHE_EXPIRY_DAYS = 30
    const val CACHE_CLEANUP_THRESHOLD = 180 // When to trigger cleanup (90% of max)
    const val SEARCH_HISTORY_LIMIT = 10
    const val SEARCH_HISTORY_EXPIRY_DAYS = 7

    // Cache expiry in milliseconds
    val CACHE_EXPIRY_MS: Long
        get() = CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

    val SEARCH_HISTORY_EXPIRY_MS: Long
        get() = SEARCH_HISTORY_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
}
