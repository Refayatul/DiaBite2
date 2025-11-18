package com.example.diabite.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",
    val biologicalSex: String = "", // "male", "female", "other"
    val primaryConditions: List<String> = emptyList(),
    val diabetesType: String? = null, // "type1", "type2", "gestational", "other"
    val favoriteFoodIds: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
