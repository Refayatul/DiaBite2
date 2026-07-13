package com.example.diabite.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodNormalizer @Inject constructor() {

    /**
     * Normalize food names for consistent searching and duplicate detection
     */
    fun normalizeFoodName(name: String): String {
        return name
            .lowercase()
            .trim()
            // Remove special characters and extra spaces
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Get search variations for better matching
     */
    fun getSearchVariations(query: String): List<String> {
        val normalized = normalizeFoodName(query)
        val variations = mutableSetOf<String>()

        // Add the normalized query
        variations.add(normalized)

        // Handle common variations
        val words = normalized.split(" ")

        // Singular/plural variations
        words.forEach { word ->
            when {
                word.endsWith("ies") -> {
                    variations.add(word.replace("ies$", "y"))
                }
                word.endsWith("es") -> {
                    variations.add(word.removeSuffix("es"))
                }
                word.endsWith("s") && !word.endsWith("ss") -> {
                    variations.add(word.removeSuffix("s"))
                    variations.add(word) // keep original too
                }
                else -> {
                    variations.add("${word}s") // add plural
                }
            }
        }

        // Handle common synonyms
        words.forEach { word ->
            getSynonyms(word).forEach { synonym ->
                val variation = normalized.replace(word, synonym)
                variations.add(variation)
            }
        }

        return variations.distinct()
    }

    /**
     * Get common food synonyms
     */
    private fun getSynonyms(word: String): List<String> {
        val synonymMap = mapOf(
            "tomato" to listOf("tomatoes"),
            "potato" to listOf("potatoes", "spud", "spuds"),
            "carrot" to listOf("carrots"),
            "onion" to listOf("onions"),
            "garlic" to listOf("garlics"),
            "apple" to listOf("apples"),
            "banana" to listOf("bananas"),
            "orange" to listOf("oranges"),
            "grape" to listOf("grapes"),
            "strawberry" to listOf("strawberries"),
            "blueberry" to listOf("blueberries"),
            "raspberry" to listOf("raspberries"),
            "chicken" to listOf("poultry"),
            "beef" to listOf("steak", "meat"),
            "pork" to listOf("ham", "bacon"),
            "fish" to listOf("seafood", "salmon", "tuna"),
            "rice" to listOf("brown rice", "white rice"),
            "bread" to listOf("whole wheat bread", "white bread", "sourdough"),
            "cheese" to listOf("cheddar", "mozzarella", "parmesan"),
            "milk" to listOf("dairy", "whole milk", "skim milk"),
            "egg" to listOf("eggs"),
            "butter" to listOf("margarine"),
            "oil" to listOf("olive oil", "vegetable oil"),
            "sugar" to listOf("sweetener", "honey"),
            "salt" to listOf("sodium"),
            "pepper" to listOf("black pepper"),
            "flour" to listOf("wheat flour", "all-purpose flour"),
            "pasta" to listOf("spaghetti", "macaroni", "noodles"),
            "bean" to listOf("beans", "kidney bean", "black bean"),
            "pea" to listOf("peas", "green pea"),
            "corn" to listOf("maize"),
            "lettuce" to listOf("salad greens"),
            "spinach" to listOf("leafy greens"),
            "broccoli" to listOf("cruciferous vegetable"),
            "carrot" to listOf("root vegetable"),
            "sweet potato" to listOf("yam"),
            "avocado" to listOf("guacamole"),
            "peanut" to listOf("peanuts", "groundnut"),
            "almond" to listOf("almonds", "nuts"),
            "walnut" to listOf("walnuts"),
            "chocolate" to listOf("cocoa"),
            "coffee" to listOf("espresso"),
            "tea" to listOf("green tea", "black tea"),
            "juice" to listOf("orange juice", "apple juice"),
            "soda" to listOf("soft drink", "cola"),
            "water" to listOf("h2o", "hydration"),
            "yogurt" to listOf("yoghurt"),
            "cereal" to listOf("breakfast cereal", "oatmeal"),
            "soup" to listOf("broth", "stew"),
            "salad" to listOf("green salad", "caesar salad"),
            "sandwich" to listOf("sub", "wrap"),
            "pizza" to listOf("margherita", "pepperoni"),
            "burger" to listOf("hamburger"),
            "fries" to listOf("french fries", "chips"),
            "cake" to listOf("dessert", "pastry"),
            "cookie" to listOf("biscuit", "cracker"),
            "ice cream" to listOf("frozen dessert"),
            "candy" to listOf("sweets", "chocolate bar")
        )

        return synonymMap[word] ?: emptyList()
    }

    /**
     * Check if two food names are duplicates
     */
    fun areDuplicates(name1: String, name2: String): Boolean {
        val normalized1 = normalizeFoodName(name1)
        val normalized2 = normalizeFoodName(name2)

        // Exact match after normalization
        if (normalized1 == normalized2) return true

        // Check if one contains the other (for cases like "banana" vs "ripe banana")
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            return true
        }

        // Check variations
        val variations1 = getSearchVariations(name1)
        val variations2 = getSearchVariations(name2)

        return variations1.any { variation -> variations2.contains(variation) }
    }

    /**
     * Get the canonical name for a food (prefer shorter, more common names)
     */
    fun getCanonicalName(names: List<String>): String {
        if (names.isEmpty()) return ""

        // Prefer shorter names, then alphabetically
        return names
            .map { normalizeFoodName(it) }
            .distinct()
            .sortedWith(compareBy<String> { it.length }.thenBy { it })
            .first()
    }

    /**
     * Remove duplicates from a list of food items
     */
    fun removeDuplicates(items: List<com.example.diabite.data.model.FoodItem>): List<com.example.diabite.data.model.FoodItem> {
        val seen = mutableSetOf<String>()
        return items.filter { item ->
            val canonicalName = getCanonicalName(listOf(item.name))
            if (seen.contains(canonicalName)) {
                false
            } else {
                seen.add(canonicalName)
                true
            }
        }
    }

    /**
     * Score search relevance (higher score = better match)
     */
    fun getSearchRelevanceScore(query: String, foodName: String): Double {
        val normalizedQuery = normalizeFoodName(query)
        val normalizedName = normalizeFoodName(foodName)

        var score = 0.0

        // Exact match gets highest score
        if (normalizedName == normalizedQuery) {
            score += 100.0
        }

        // Starts with query
        if (normalizedName.startsWith(normalizedQuery)) {
            score += 50.0
        }

        // Contains query
        if (normalizedName.contains(normalizedQuery)) {
            score += 25.0
        }

        // Word match (query words in food name)
        val queryWords = normalizedQuery.split(" ")
        val nameWords = normalizedName.split(" ")
        val matchingWords = queryWords.count { queryWord ->
            nameWords.any { nameWord -> nameWord.contains(queryWord) || queryWord.contains(nameWord) }
        }
        score += matchingWords * 10.0

        // Length difference penalty (prefer closer length matches)
        val lengthDiff = kotlin.math.abs(normalizedQuery.length - normalizedName.length)
        score -= lengthDiff * 0.1

        return score
    }
}
