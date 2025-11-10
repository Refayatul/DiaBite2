package com.example.diabite.data.model

data class Alternative(
    val foodId: String = "",
    val advantage: String = "",
    val improvement: String = "",
    val bestFor: List<String> = emptyList()
)
