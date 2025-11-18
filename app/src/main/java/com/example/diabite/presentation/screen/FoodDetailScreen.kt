package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    val userDiabetesType by viewModel.userDiabetesType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Food Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavoriteStatus() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> LoadingState()
                error != null -> ErrorState(error = error!!, onRetry = { viewModel.retry() })
                foodItem == null -> ErrorState(error = "Food not found", onRetry = { viewModel.retry() })
                else -> FoodDetailContent(
                    foodItem = foodItem!!,
                    alternatives = alternatives,
                    diabetesType = userDiabetesType,
                    expandedSections = expandedSections,
                    onSectionToggle = { viewModel.toggleSection(it) },
                    onAlternativeClick = { alternative -> navController.navigate("foodDetail/${alternative.id}") }
                )
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading food details...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun FoodDetailContent(
    foodItem: FoodItem,
    alternatives: List<FoodItem>,
    diabetesType: String?,
    expandedSections: Set<String>,
    onSectionToggle: (String) -> Unit,
    onAlternativeClick: (FoodItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        FoodHeader(foodItem = foodItem, diabetesType = diabetesType)

        Spacer(modifier = Modifier.height(24.dp))

        // Diabetes Type Advice - First and auto-expanded
        ExpandableSection(
            title = "🏥 Diabetes Type Advice",
            sectionKey = "diabetes_advice",
            isExpanded = expandedSections.contains("diabetes_advice"),
            onToggle = onSectionToggle
        ) {
            ConditionAdviceSection(foodItem = foodItem, diabetesType = diabetesType)
        }

        // Smart Alternatives - Second
        if (alternatives.isNotEmpty()) {
            ExpandableSection(
                title = "🔄 Smart Alternatives",
                sectionKey = "alternatives",
                isExpanded = expandedSections.contains("alternatives"),
                onToggle = onSectionToggle
            ) {
                AlternativesComparisonSection(
                    originalFood = foodItem!!,
                    alternatives = alternatives,
                    diabetesType = diabetesType,
                    onAlternativeClick = onAlternativeClick
                )
            }
        }

        // Nutritional Facts - Third and auto-expanded
        ExpandableSection(
            title = "📊 Nutritional Facts",
            sectionKey = "nutrition",
            isExpanded = expandedSections.contains("nutrition"),
            onToggle = onSectionToggle
        ) {
            NutritionalFactsSection(foodItem = foodItem)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DiabetesTypeIndicator(diabetesType: String) {
    val (backgroundColor, textColor, icon) = when (diabetesType.lowercase()) {
        "diabetes type 1", "type 1" -> {
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
                Icons.Filled.Favorite
            )
        }
        "diabetes type 2", "type 2" -> {
            Triple(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.secondary,
                Icons.Filled.FavoriteBorder
            )
        }
        else -> {
            Triple(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.tertiary,
                Icons.Filled.QuestionMark
            )
        }
    }

    val borderColor = when (diabetesType.lowercase()) {
        "diabetes type 1", "type 1" -> MaterialTheme.colorScheme.primary
        "diabetes type 2", "type 2" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Personalized for: $diabetesType",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FoodHeader(foodItem: FoodItem, diabetesType: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Food Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Food Name
            Text(
                text = foodItem.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category
            Text(
                text = foodItem.category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nutritional Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionalHighlight("Calories", "${foodItem.calories}")
                NutritionalHighlight("Carbs", "${foodItem.totalCarbohydrates}g")
                NutritionalHighlight("Protein", "${foodItem.protein}g")
                NutritionalHighlight("Fiber", "${foodItem.fiber}g")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safety Rating with reasoning
            val safetyRating = getSafetyRating(foodItem, diabetesType)
            val reasoning = getSafetyReasoning(foodItem, diabetesType)
            SafetyRatingDisplay(safetyRating, reasoning)
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
    var rotation by rememberSaveable { mutableStateOf(0f) }
    val targetRotation = if (isExpanded) 180f else 0f
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(sectionKey) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(animatedRotation),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun NutritionalFactsSection(foodItem: FoodItem) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Complete Nutritional Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Macronutrients
        Text(
            text = "Macronutrients",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        NutritionalRow("Calories", "${foodItem.calories} kcal")
        NutritionalRow("Total Carbohydrates", "${foodItem.totalCarbohydrates}g")
        NutritionalRow("Dietary Fiber", "${foodItem.fiber}g")
        NutritionalRow("Sugars", "${foodItem.sugars}g")
        NutritionalRow("Protein", "${foodItem.protein}g")
        NutritionalRow("Total Fat", "${foodItem.totalFat}g")
        NutritionalRow("Saturated Fat", "${foodItem.saturatedFat}g")

        Spacer(modifier = Modifier.height(16.dp))

        // Micronutrients
        Text(
            text = "Additional Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        NutritionalRow("Sodium", "${foodItem.sodium}mg")
        NutritionalRow("Potassium", "${foodItem.potassium}mg")
        if (foodItem.glycemicIndex > 0) NutritionalRow("Glycemic Index", "${foodItem.glycemicIndex}")
        if (foodItem.glycemicLoad > 0) NutritionalRow("Glycemic Load", "${foodItem.glycemicLoad}")
    }
}

@Composable
private fun ConditionAdviceSection(foodItem: FoodItem, diabetesType: String?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Personalized Diabetes Advice",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (diabetesType == null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "No diabetes type selected. Please set your diabetes type in settings for personalized advice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            return
        }

        val key = normalizeConditionKey(diabetesType)
        val recommendation = foodItem.recommendations[key]

        if (recommendation != null) {
            ConditionAdviceCard(condition = formatConditionKeyForDisplay(key), recommendation = recommendation, diabetesType = diabetesType)
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.QuestionMark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "No specific recommendations available for your diabetes type.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlternativesComparisonSection(
    originalFood: FoodItem,
    alternatives: List<FoodItem>,
    diabetesType: String?,
    onAlternativeClick: (FoodItem) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Smart Alternatives",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (alternatives.isEmpty()) {
            Text(
                text = "No alternatives available for this food.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Food",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = "Calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Carbs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Advantage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.2f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        alternatives.forEach { alternative ->
            val advantage = diabetesType?.let { dt ->
                val normalizedKey = normalizeConditionKey(dt)
                val recommendation = originalFood.recommendations[normalizedKey]
                recommendation?.alternatives?.find { it.foodId == alternative.id }?.advantage
                    ?: "Better alternative"
            } ?: "Better alternative"

            ComparisonRow(
                alternative.name,
                alternative.calories,
                alternative.totalCarbohydrates,
                advantage,
                isOriginal = false,
                diabetesType = diabetesType
            ) {
                onAlternativeClick(alternative)
            }
        }
    }
}

@Composable
private fun NutritionalHighlight(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
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
private fun SafetyRatingDisplay(safetyRating: String, reasoning: String? = null) {
    val (backgroundColor, textColor) = when (safetyRating.lowercase()) {
        "avoid" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "caution", "moderate" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "good" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Safety Level Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(textColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Safety: $safetyRating",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (reasoning != null) {
                Spacer(modifier = Modifier.height(12.dp))

                // Reasoning Section
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = textColor.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Assessment",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = reasoning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConditionAdviceCard(
    condition: String,
    recommendation: com.example.diabite.data.model.ConditionRecommendation,
    diabetesType: String
) {
    val diabetesColor = when (diabetesType.lowercase()) {
        "diabetes type 1", "type 1" -> MaterialTheme.colorScheme.primary
        "diabetes type 2", "type 2" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with condition
            Text(
                text = condition,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = diabetesColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Serving and Timing Advice in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Serving Advice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = diabetesColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = recommendation.servingAdvice,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Timing Advice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = diabetesColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = recommendation.timingAdvice,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health Impacts
            Text(
                text = "Health Impacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = diabetesColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthImpactChip("Blood Sugar", recommendation.bloodSugarImpact)
                HealthImpactChip("Heart Health", recommendation.heartHealthImpact)
                HealthImpactChip("Kidney", recommendation.kidneyImpact)
                HealthImpactChip("Blood Pressure", recommendation.bloodPressureImpact)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Points
            if (recommendation.keyPoints.isNotEmpty()) {
                Text(
                    text = "Key Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = diabetesColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                recommendation.keyPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = diabetesColor
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pairing Suggestions
            if (recommendation.pairingSuggestions.isNotEmpty()) {
                Text(
                    text = "Pairing Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = diabetesColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendation.pairingSuggestions.forEach { pairing ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = pairing.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = diabetesColor.copy(alpha = 0.1f),
                                labelColor = diabetesColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Nutritional Benefits
            if (recommendation.nutritionalBenefits.isNotEmpty()) {
                Text(
                    text = "Nutritional Benefits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = diabetesColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                recommendation.nutritionalBenefits.forEach { benefit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = diabetesColor.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, diabetesColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = benefit.category.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = diabetesColor
                                )
                                Text(
                                    text = benefit.strength.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = diabetesColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(diabetesColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = benefit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Potential Concerns
            if (recommendation.potentialConcerns.isNotEmpty()) {
                Text(
                    text = "Potential Concerns",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                recommendation.potentialConcerns.forEach { concern ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = concern.category.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = concern.severity.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = concern.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Preparation Tips
            if (recommendation.preparationTips.isNotEmpty()) {
                Text(
                    text = "Preparation Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = diabetesColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                recommendation.preparationTips.forEach { tip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = diabetesColor.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, diabetesColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = diabetesColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tip.category.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = diabetesColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = tip.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthImpactChip(impactType: String, impactLevel: String) {
    val (backgroundColor, textColor) = when (impactLevel.lowercase()) {
        "high", "positive" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "moderate", "neutral" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "low", "negative" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        Text(
            text = impactType,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = impactLevel.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ComparisonRow(
    foodName: String,
    calories: Int,
    carbs: Double,
    advantage: String,
    isOriginal: Boolean,
    diabetesType: String?,
    onClick: () -> Unit
) {
    val diabetesColor = when (diabetesType?.lowercase()) {
        "diabetes type 1", "type 1" -> MaterialTheme.colorScheme.primary
        "diabetes type 2", "type 2" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val clickableModifier = if (!isOriginal) Modifier.clickable { onClick() } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(clickableModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = foodName,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal,
            color = if (isOriginal) diabetesColor else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$calories",
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "${carbs.toInt()}g",
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = advantage,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isOriginal) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getSafetyRating(foodItem: FoodItem, diabetesType: String?): String {
    if (diabetesType == null) return "Unknown"
    val key = normalizeConditionKey(diabetesType)
    val recommendation = foodItem.recommendations[key] ?: return "Unknown"
    return recommendation.safetyLevel.replaceFirstChar { it.uppercase() }
}

private fun getSafetyReasoning(foodItem: FoodItem, diabetesType: String?): String? {
    if (diabetesType == null) return null
    val key = normalizeConditionKey(diabetesType)
    val recommendation = foodItem.recommendations[key] ?: return null
    return recommendation.reasoning
}


private fun normalizeConditionKey(condition: String): String {
    return when (condition.lowercase()) {
        "diabetes type 1", "type 1", "diabetes_type_1", "diabetes_type1" -> "diabetes_type1"
        "diabetes type 2", "type 2", "diabetes_type_2", "diabetes_type2" -> "diabetes_type2"
        else -> condition.lowercase().replace(" ", "_")
    }
}

private fun formatConditionKeyForDisplay(key: String): String {
    return when (key) {
        "diabetes_type1" -> "Diabetes Type 1"
        "diabetes_type2" -> "Diabetes Type 2"
        else -> key.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
