package com.example.diabite.data.model

import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val dateOfBirth: Date? = null,
    val biologicalSex: String = "",
    val primaryConditions: List<String> = emptyList(),
    val diabetesType: String? = null,
    val diabetesMedications: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date()
)
