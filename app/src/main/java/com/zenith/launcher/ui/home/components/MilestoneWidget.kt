package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.MilestoneTarget
import com.zenith.launcher.util.CountdownUtil
import kotlin.math.roundToInt

/**
 * Widget: "Milestone Target" - the aspirant's self-set goal for their next big mock test. Shows
 * the target score, an arc gauge for how close their last mock score got them there, and (if a
 * test date is set) a days-left hint. Tap the pencil to edit any of it.
 */
@Composable
fun MilestoneWidget(target: MilestoneTarget, onChange: (MilestoneTarget) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    val daysLeft = target.nextTestDateMillis?.let { CountdownUtil.daysRemaining(it) }

    WidgetCard {
        MilestoneHeaderRow(target = target, onEditClick = { showEditDialog = true })
        Spacer(Modifier.height(4.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                target.testName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Target: ${target.targetScore}/${target.maxScore}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            ReadinessGauge(readiness = target.readiness)
            Spacer(Modifier.height(8.dp))
            val summary = buildString {
                append(if (target.lastScore != null) "Last Mock Score: ${target.lastScore}" else "No mock scores logged yet")
                if (daysLeft != null) append(" | Next Test: $daysLeft Days")
            }
            Text(
                summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showEditDialog) {
        EditMilestoneDialog(
            target = target,
            onDismiss = { showEditDialog = false },
            onConfirm = { onChange(it); showEditDialog = false }
        )
    }
}

@Composable
private fun MilestoneHeaderRow(target: MilestoneTarget, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Milestone Target", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit milestone target", modifier = Modifier.size(16.dp))
        }
    }
}

/** A ~270-degree arc gauge (matches the reference design) showing readiness as a percentage. */
@Composable
private fun ReadinessGauge(readiness: Float) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(120.dp).rotate(135f)) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val sweep = 270f
            val inset = stroke.width / 2
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke
            )
            drawArc(
                color = progressColor,
                startAngle = 0f,
                sweepAngle = sweep * readiness,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(readiness * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Prep Readiness",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditMilestoneDialog(
    target: MilestoneTarget,
    onDismiss: () -> Unit,
    onConfirm: (MilestoneTarget) -> Unit
) {
    var testName by remember { mutableStateOf(target.testName) }
    var targetScore by remember { mutableStateOf(target.targetScore.toString()) }
    var maxScore by remember { mutableStateOf(target.maxScore.toString()) }
    var lastScore by remember { mutableStateOf(target.lastScore?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit milestone target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = testName, onValueChange = { testName = it },
                    label = { Text("Test name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetScore, onValueChange = { targetScore = it.filter(Char::isDigit) },
                    label = { Text("Target score") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxScore, onValueChange = { maxScore = it.filter(Char::isDigit) },
                    label = { Text("Max possible score") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastScore, onValueChange = { lastScore = it.filter(Char::isDigit) },
                    label = { Text("Your last mock score (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    target.copy(
                        testName = testName.ifBlank { "Comprehensive Mock Test" },
                        targetScore = targetScore.toIntOrNull() ?: target.targetScore,
                        maxScore = maxScore.toIntOrNull() ?: target.maxScore,
                        lastScore = lastScore.toIntOrNull()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
