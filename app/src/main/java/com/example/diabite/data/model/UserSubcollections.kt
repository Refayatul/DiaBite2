package com.example.diabite.data.model

import java.util.Date

data class UserFavorite(
    val foodId: String = "",
    val addedAt: Date = Date()
)

data class UserHistory(
    val foodId: String = "",
    val eatenAt: Date = Date()
)
