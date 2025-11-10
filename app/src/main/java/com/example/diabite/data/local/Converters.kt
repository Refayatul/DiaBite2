package com.example.diabite.data.local

import androidx.room.TypeConverter
import com.example.diabite.data.model.Alternative
import com.example.diabite.data.model.ConditionRecommendation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // String List converters
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, String::class.java).type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return gson.toJson(list.orEmpty())
    }

    // Alternative List converters
    @TypeConverter
    fun fromAlternativeList(value: String?): List<Alternative>? {
        if (value.isNullOrEmpty()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, Alternative::class.java).type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun alternativeListToString(list: List<Alternative>?): String? {
        return gson.toJson(list.orEmpty())
    }

    // ConditionRecommendation Map converters
    @TypeConverter
    fun fromRecommendationMap(value: String?): Map<String, ConditionRecommendation>? {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = TypeToken.getParameterized(Map::class.java, String::class.java, ConditionRecommendation::class.java).type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun recommendationMapToString(map: Map<String, ConditionRecommendation>?): String? {
        return gson.toJson(map.orEmpty())
    }

    // FoodItem converter (for complex object storage)
    @TypeConverter
    fun fromFoodItemJson(value: String?): com.example.diabite.data.model.FoodItem? {
        if (value.isNullOrEmpty()) return null
        val type = object : TypeToken<com.example.diabite.data.model.FoodItem>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun foodItemToJson(foodItem: com.example.diabite.data.model.FoodItem?): String? {
        return gson.toJson(foodItem)
    }
}
