package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Widget 1: Dynamic JEE Exam Countdown.
 * Shows live "days remaining" for JEE Main and JEE Advanced, computed by
 * [com.zenith.launcher.ui.home.HomeViewModel] from the dates set in Settings.
 * Null means that exam's date hasn't been configured yet.
 */
@Composable
fun CountdownWidget(jeeMainDaysLeft: Long?, jeeAdvancedDaysLeft: Long?) {
    WidgetCard {
        Text("Exam Countdown", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CountdownColumn(label = "JEE Main", days = jeeMainDaysLeft)
            CountdownColumn(label = "JEE Advanced", days = jeeAdvancedDaysLeft)
        }
    }
}

@Composable
private fun CountdownColumn(label: String, days: Long?) {
    Column {
        Text(
            text = days?.toString() ?: "--",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (days == null) "$label - set date in Settings" else "days to $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
