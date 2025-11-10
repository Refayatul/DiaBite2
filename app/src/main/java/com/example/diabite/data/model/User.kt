package com.example.diabite.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",
    val biologicalSex: String = "", // "male", "female", "other"
    val primaryConditions: List<String> = emptyList(),
    val diabetesType: String = "", // "type1", "type2", "gestational", "other"
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
