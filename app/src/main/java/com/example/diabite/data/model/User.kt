package com.example.diabite.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val diabetesType: String = "",
    val favoriteFoodIds: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList()
)
