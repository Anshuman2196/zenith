package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.home.ExamCountdown
import com.zenith.launcher.ui.home.LauncherCopy

/**
 * Widget: Deadlines - live "days remaining" for every deadline the user has added (see
 * Settings > Deadlines; there's no fixed exam list anymore, this simply reflects whatever's there).
 * Null [ExamCountdown.daysLeft] means that deadline's date hasn't been set yet.
 */
@Composable
fun CountdownWidget(exams: List<ExamCountdown>) {
    WidgetCard {
        Text("Deadlines", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        if (exams.isEmpty()) {
            Text(
                LauncherCopy.emptyDeadlines[java.time.LocalDate.now().dayOfYear % LauncherCopy.emptyDeadlines.size],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Two per row keeps this widget from growing unbounded tall with many exams,
                // while still reading naturally left-to-right for the common 1-3 exam case.
                exams.chunked(2).forEach { rowOfExams ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        rowOfExams.forEach { exam -> CountdownColumn(exam) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownColumn(exam: ExamCountdown) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = exam.daysLeft?.toString() ?: "--",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (exam.daysLeft == null) "${exam.name} - set date in Settings" else "days until ${exam.name}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
