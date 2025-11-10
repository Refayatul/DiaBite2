package com.example.diabite.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Define a simple data structure for history items
data class SearchHistoryItem(val foodName: String, val date: String, val result: String)

/**
 * Helper Composable for displaying a single history item in a styled card.
 */
@Composable
fun HistoryItemCard(item: SearchHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.foodName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Result Indicator
            Text(
                text = item.result,
                style = MaterialTheme.typography.labelLarge.copy(
                    // Assign colors based on the result for quick visual feedback
                    color = when {
                        item.result.startsWith("Safe") -> Color(0xFF1B5E20) // Dark Green
                        item.result.startsWith("Caution") -> Color(0xFFD32F2F) // Dark Red
                        item.result.startsWith("Moderate") -> Color(0xFFFFA000) // Dark Amber
                        item.result.startsWith("Avoid") -> Color(0xFFB71C1C) // Deep Red
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * History Screen UI (Internal Tab) - Displays recent search history.
 */
@Composable
fun HistoryScreenUI() {
    // Dummy Data for demonstration
    val historyItems = remember {
        listOf(
            SearchHistoryItem("Apple", "Oct 8, 2025", "Safe (GI: Low)"),
            SearchHistoryItem("White Bread", "Oct 7, 2025", "Caution (GI: High)"),
            SearchHistoryItem("Brown Rice", "Oct 6, 2025", "Moderate (GI: Medium)"),
            SearchHistoryItem("Banana", "Oct 5, 2025", "Safe (GI: Medium)"),
            SearchHistoryItem("Soda", "Oct 4, 2025", "Avoid (Sugar: High)"),
            SearchHistoryItem("Oatmeal", "Oct 3, 2025", "Safe (GI: Low)"),
            SearchHistoryItem("French Fries", "Oct 2, 2025", "Caution (High Fat)"),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recent Search History",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (historyItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "No History",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No recent searches found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(historyItems) { item ->
                    HistoryItemCard(item)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
