package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.data.model.FoodItem
import com.example.diabite.presentation.viewmodel.FoodDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    navController: NavController,
    viewModel: FoodDetailViewModel = hiltViewModel()
) {
    val foodItem by viewModel.foodItem.collectAsState()
    val alternatives by viewModel.alternatives.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val expandedSections by viewModel.expandedSections.collectAsState()

    val userConditions by viewModel.userConditions.collectAsState()
    val userDiabetesType by viewModel.userDiabetesType.collectAsState()

    val allUserConditions = (userConditions + listOfNotNull(userDiabetesType)).distinct().filter { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavoriteStatus() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingState()
                }
                error != null -> {
                    ErrorState(
                        error = error!!,
                        onRetry = { viewModel.retry() }
                    )
                }
                foodItem == null -> {
                    ErrorState(
                        error = "Food not found",
                        onRetry = { viewModel.retry() }
                    )
                }
                else -> {
                    FoodDetailContent(
                        foodItem = foodItem!!,
                        alternatives = alternatives,
                        userConditions = allUserConditions,
                        expandedSections = expandedSections,
                        onSectionToggle = { viewModel.toggleSection(it) },
                        onAlternativeClick = { alternative ->
                            navController.navigate("foodDetail/${alternative.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading food details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun FoodDetailContent(
    foodItem: FoodItem,
    alternatives: List<FoodItem>,
    userConditions: List<String>,
    expandedSections: Set<String>,
    onSectionToggle: (String) -> Unit,
    onAlternativeClick: (FoodItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Food Header
        FoodHeader(foodItem = foodItem, userConditions = userConditions)

        Spacer(modifier = Modifier.height(24.dp))

        // Expandable Sections
        ExpandableSection(
            title = "📊 Nutritional Facts",
            sectionKey = "nutrition",
            isExpanded = expandedSections.contains("nutrition"),
            onToggle = onSectionToggle
        ) {
            NutritionalFactsSection(foodItem = foodItem)
        }

        ExpandableSection(
            title = "🏥 Condition-Specific Advice",
            sectionKey = "conditions",
            isExpanded = expandedSections.contains("conditions"),
            onToggle = onSectionToggle
        ) {
            ConditionAdviceSection(
                foodItem = foodItem,
                userConditions = userConditions
            )
        }

        ExpandableSection(
            title = "🔄 Smart Alternatives",
            sectionKey = "alternatives",
            isExpanded = expandedSections.contains("alternatives"),
            onToggle = onSectionToggle
        ) {
            AlternativesComparisonSection(
                originalFood = foodItem,
                alternatives = alternatives,
                userConditions = userConditions,
                onAlternativeClick = onAlternativeClick
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FoodHeader(
    foodItem: FoodItem,
    userConditions: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Food icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Food name and category
            Text(
                text = foodItem.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = foodItem.category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Key nutritional info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionalHighlight("Calories", "${foodItem.calories}")
                NutritionalHighlight("Carbs", "${foodItem.carbs}g")
                NutritionalHighlight("Protein", "${foodItem.protein}g")
                NutritionalHighlight("Fiber", "${foodItem.fiber}g")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safety rating
            val safetyRating = getSafetyRating(foodItem, userConditions)
            SafetyRatingDisplay(safetyRating)
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    sectionKey: String,
    isExpanded: Boolean,
    onToggle: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(sectionKey) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun NutritionalFactsSection(foodItem: FoodItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Complete Nutritional Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Macronutrients
            NutritionalRow("Calories", "${foodItem.calories}")
            NutritionalRow("Total Carbohydrates", "${foodItem.carbs}g")
            NutritionalRow("Dietary Fiber", "${foodItem.fiber}g")
            NutritionalRow("Sugars", "${foodItem.sugars}g")
            NutritionalRow("Protein", "${foodItem.protein}g")
            NutritionalRow("Total Fat", "${foodItem.totalFat}g")
            NutritionalRow("Saturated Fat", "${foodItem.saturatedFat}g")

            Spacer(modifier = Modifier.height(16.dp))

            // Micronutrients
            Text(
                text = "Micronutrients",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            NutritionalRow("Sodium", "${foodItem.sodium}mg")
            NutritionalRow("Potassium", "${foodItem.potassium}mg")

            // Glycemic info
            foodItem.glycemicIndex?.let { gi ->
                Spacer(modifier = Modifier.height(8.dp))
                NutritionalRow("Glycemic Index", "$gi")
            }

            foodItem.glycemicLoad?.let { gl ->
                NutritionalRow("Glycemic Load", "$gl")
            }
        }
    }
}

@Composable
private fun ConditionAdviceSection(
    foodItem: FoodItem,
    userConditions: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personalized Health Advice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            userConditions.forEach { condition ->
                // Lookup using the normalized key from userConditions
                foodItem.recommendations[condition]?.let { recommendation ->
                    ConditionAdviceCard(
                        // Display the readable version of the key
                        condition = formatConditionKeyForDisplay(condition),
                        recommendation = recommendation
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (userConditions.all { foodItem.recommendations[it] == null }) {
                Text(
                    text = "No specific recommendations available for your conditions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlternativesComparisonSection(
    originalFood: FoodItem,
    alternatives: List<FoodItem>,
    userConditions: List<String>,
    onAlternativeClick: (FoodItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Compare with Alternatives",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Comparison table header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Food",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Calories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Carbs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Safety",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Original food row
            ComparisonRow(
                foodName = "${originalFood.name} (Current)",
                calories = originalFood.calories,
                carbs = originalFood.carbs,
                safetyRating = getSafetyRating(originalFood, userConditions),
                isOriginal = true,
                onClick = { /* Original food, no action */ }
            )

            // Alternative rows
            alternatives.forEach { alternative ->
                ComparisonRow(
                    foodName = alternative.name,
                    calories = alternative.calories,
                    carbs = alternative.carbs,
                    safetyRating = getSafetyRating(alternative, userConditions),
                    isOriginal = false,
                    onClick = { onAlternativeClick(alternative) }
                )
            }
        }
    }
}

// Helper composables
@Composable
private fun NutritionalHighlight(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SafetyRatingDisplay(safetyRating: String) {
    val (backgroundColor, textColor) = when (safetyRating.lowercase()) {
        "avoid" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "caution", "moderate" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "good" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Safety: $safetyRating",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NutritionalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConditionAdviceCard(
    condition: String,
    recommendation: com.example.diabite.data.model.ConditionRecommendation
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.status.lowercase()) {
                "avoid" -> MaterialTheme.colorScheme.errorContainer
                "caution", "moderate" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = condition, // Use the formatted string from the screen
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recommendation.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            recommendation.serving?.standard?.let {
                if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 ${it}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            recommendation.warnings.firstOrNull()?.let {
                 if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ ${it}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    foodName: String,
    calories: Int,
    carbs: Double,
    safetyRating: String,
    isOriginal: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOriginal, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = foodName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$calories",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${carbs}g",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = safetyRating,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = when (safetyRating.lowercase()) {
                "avoid" -> MaterialTheme.colorScheme.error
                "caution", "moderate" -> MaterialTheme.colorScheme.tertiary
                "good" -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper function
private fun getSafetyRating(foodItem: FoodItem, userConditions: List<String>): String {
    if (userConditions.isEmpty()) return "Unknown"

    val relevantRecommendations = userConditions.mapNotNull { condition ->
        foodItem.recommendations[condition]
    }

    if (relevantRecommendations.isEmpty()) return "Unknown"

    val statuses = relevantRecommendations.map { it.status }

    return when {
        statuses.any { it.equals("avoid", ignoreCase = true) } -> "Avoid"
        statuses.any { it.equals("caution", ignoreCase = true) } -> "Caution"
        statuses.any { it.equals("moderate", ignoreCase = true) } -> "Caution"
        statuses.any { it.equals("good", ignoreCase = true) } -> "Good"
        else -> "Unknown"
    }
}

private fun formatConditionKeyForDisplay(normalizedKey: String): String {
    return when (normalizedKey) {
        "diabetes_type1" -> "Type 1 Diabetes"
        "diabetes_type2" -> "Type 2 Diabetes"
        "prediabetes" -> "Prediabetes"
        "gestational_diabetes" -> "Gestational Diabetes"
        "hypertension" -> "Hypertension"
        "hypotension" -> "Hypotension"
        "high_cholesterol" -> "High Cholesterol"
        "coronary_artery_disease" -> "Coronary Artery Disease"
        "kidney_disease" -> "Kidney Disease"
        "obesity" -> "Obesity"
        "pcos" -> "PCOS"
        "thyroid_disorders" -> "Thyroid Disorders"
        "pregnancy" -> "Pregnancy"
        else -> normalizedKey.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}
