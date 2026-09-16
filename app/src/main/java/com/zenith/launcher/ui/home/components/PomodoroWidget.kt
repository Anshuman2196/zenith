package com.zenith.launcher.ui.home.components

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.AlarmItem
import com.zenith.launcher.ui.home.LauncherCopy
import com.zenith.launcher.util.TimerAlarmScheduler
import kotlinx.coroutines.delay

private enum class TimerMode { POMODORO, STOPWATCH, ALARM }
private enum class PomodoroPhase(val label: String) { STUDY("Focus"), SHORT_BREAK("Short break"), LONG_BREAK("Long break") }

/**
 * A deliberate study/break timer: completing a phase pauses for confirmation instead of silently
 * rolling into the next one. A system alarm is scheduled while it runs, so completion is still
 * announced when Zenith is in the background.
 *
 * While a Pomodoro session is actually running, switching to the Stopwatch or Alarm tab is
 * disabled - this is one of the ways a running session locks the rest of Home down (see
 * [com.zenith.launcher.ui.home.HomeScreen]).
 */
@Composable
fun PomodoroWidget(
    alarms: List<AlarmItem>,
    onAddAlarm: (hour: Int, minute: Int, label: String) -> Unit,
    onToggleAlarm: (id: String, enabled: Boolean) -> Unit,
    onDeleteAlarm: (id: String) -> Unit,
    onPomodoroRunningChanged: (Boolean) -> Unit = {},
    onPomodoroProtectionChanged: (Boolean) -> Unit = {}
) {
    var mode by rememberSaveable { mutableStateOf(TimerMode.POMODORO) }
    var isPomodoroRunning by rememberSaveable { mutableStateOf(false) }

    WidgetCard {
        Text("Study timer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = mode.ordinal) {
            Tab(
                selected = mode == TimerMode.POMODORO,
                onClick = { mode = TimerMode.POMODORO },
                text = { Text("Pomodoro") }
            )
            Tab(
                selected = mode == TimerMode.STOPWATCH,
                onClick = { if (!isPomodoroRunning) mode = TimerMode.STOPWATCH },
                enabled = !isPomodoroRunning,
                text = { Text("Stopwatch") }
            )
            Tab(
                selected = mode == TimerMode.ALARM,
                onClick = { if (!isPomodoroRunning) mode = TimerMode.ALARM },
                enabled = !isPomodoroRunning,
                text = { Text("Alarm") }
            )
        }
        Spacer(Modifier.height(12.dp))
        when (mode) {
            TimerMode.POMODORO -> PomodoroSection(
                onPomodoroRunningChanged = {
                    isPomodoroRunning = it
                    onPomodoroRunningChanged(it)
                },
                onPomodoroProtectionChanged = onPomodoroProtectionChanged
            )
            TimerMode.STOPWATCH -> StopwatchSection()
            TimerMode.ALARM -> AlarmSection(
                alarms = alarms,
                onAdd = onAddAlarm,
                onToggle = onToggleAlarm,
                onDelete = onDeleteAlarm
            )
        }
    }
}

@Composable
private fun StopwatchSection() {
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isRunning) { while (isRunning) { delay(1000); elapsedSeconds++ } }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(formatSeconds(elapsedSeconds), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { isRunning = !isRunning }) { Text(if (isRunning) "Pause" else "Start") }
            OutlinedButton(onClick = { isRunning = false; elapsedSeconds = 0 }) { Text("Reset") }
        }
    }
}

@Composable
private fun PomodoroSection(
    onPomodoroRunningChanged: (Boolean) -> Unit,
    onPomodoroProtectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var focusMinutes by rememberSaveable { mutableIntStateOf(25) }
    var shortBreakMinutes by rememberSaveable { mutableIntStateOf(5) }
    var longBreakMinutes by rememberSaveable { mutableIntStateOf(15) }
    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.STUDY) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(focusMinutes * 60) }
    var completedFocusSessions by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var awaitingNextPhase by rememberSaveable { mutableStateOf(false) }
    var isRinging by rememberSaveable { mutableStateOf(false) }
    var stopPauseSeconds by rememberSaveable { mutableIntStateOf(0) }
    var stopPauseMessage by rememberSaveable { mutableStateOf("") }
    var stopPauseKind by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(isRunning) { onPomodoroRunningChanged(isRunning) }
    LaunchedEffect(stopPauseSeconds, awaitingNextPhase) { onPomodoroProtectionChanged(stopPauseSeconds > 0 || awaitingNextPhase) }
    DisposableEffect(Unit) { onDispose { onPomodoroProtectionChanged(false) } }
    DisposableEffect(Unit) { onDispose { onPomodoroRunningChanged(false) } }

    fun phaseDuration(current: PomodoroPhase) = when (current) {
        PomodoroPhase.STUDY -> focusMinutes * 60
        PomodoroPhase.SHORT_BREAK -> shortBreakMinutes * 60
        PomodoroPhase.LONG_BREAK -> longBreakMinutes * 60
    }
    fun advance() {
        phase = if (phase == PomodoroPhase.STUDY) {
            completedFocusSessions++
            if (completedFocusSessions % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
        } else PomodoroPhase.STUDY
        secondsLeft = phaseDuration(phase)
        awaitingNextPhase = true
    }

    LaunchedEffect(isRunning, phase, secondsLeft) {
        if (!isRunning || stopPauseSeconds > 0) return@LaunchedEffect
        TimerAlarmScheduler.schedule(context, secondsLeft, "${phase.label} is complete")
        while (isRunning && secondsLeft > 0 && stopPauseSeconds == 0) { delay(1000); secondsLeft-- }
        if (isRunning && secondsLeft == 0 && stopPauseSeconds == 0) {
            TimerAlarmScheduler.cancel(context)
            isRunning = false
            stopPauseSeconds = 7
            stopPauseKind = 2
            stopPauseMessage = LauncherCopy.pomodoroComplete[completedFocusSessions % LauncherCopy.pomodoroComplete.size]
        }
    }

    LaunchedEffect(stopPauseSeconds) {
        if (stopPauseSeconds <= 0) return@LaunchedEffect
        while (stopPauseSeconds > 0) { delay(1000); stopPauseSeconds-- }
        val message = stopPauseMessage
        val kind = stopPauseKind
        stopPauseMessage = ""
        stopPauseKind = 0
        if (kind == 2) {
            isRinging = true
            advance()
        } else {
            isRinging = false
            phase = PomodoroPhase.STUDY
            secondsLeft = focusMinutes * 60
            completedFocusSessions = 0
            awaitingNextPhase = false
        }
    }
    // Keep the completion signal audible until the user explicitly resets or starts the break.
    // A new ToneGenerator is short-lived per pulse, avoiding a retained audio resource.
    LaunchedEffect(isRinging) {
        while (isRinging) {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
            delay(750)
            tone.release()
            delay(250)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (stopPauseSeconds > 0) {
            Text(if (stopPauseKind == 2) "Let it land" else "Before you stop", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stopPauseSeconds.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(stopPauseMessage, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        Text(phase.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(formatSeconds(secondsLeft), style = MaterialTheme.typography.headlineMedium)
        if (awaitingNextPhase) Text(LauncherCopy.pomodoroTransition[completedFocusSessions % LauncherCopy.pomodoroTransition.size], style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                enabled = stopPauseSeconds == 0,
                onClick = {
                    // Pausing remains immediate; only an explicit stop/reset gets the reflective pause.
                    isRunning = !isRunning
                    awaitingNextPhase = false
                    isRinging = false
                }
            ) { Text(if (isRunning) "Pause" else if (awaitingNextPhase) "Start ${phase.label}" else "Start") }
            OutlinedButton(
                enabled = stopPauseSeconds == 0,
                onClick = {
                    TimerAlarmScheduler.cancel(context)
                    if (isRunning) {
                        isRunning = false
                        stopPauseSeconds = 7
                        stopPauseKind = 1
                        stopPauseMessage = LauncherCopy.pomodoroStop[completedFocusSessions % LauncherCopy.pomodoroStop.size]
                    } else {
                        awaitingNextPhase = false
                        phase = PomodoroPhase.STUDY
                        secondsLeft = focusMinutes * 60
                        completedFocusSessions = 0
                        isRinging = false
                    }
                }
            ) { Text("Stop") }
        }
        if (!isRunning) {
            Spacer(Modifier.height(8.dp))
            DurationStepper("Focus", focusMinutes) { focusMinutes = it; if (phase == PomodoroPhase.STUDY) secondsLeft = it * 60 }
            DurationStepper("Short break", shortBreakMinutes) { shortBreakMinutes = it; if (phase == PomodoroPhase.SHORT_BREAK) secondsLeft = it * 60 }
            DurationStepper("Long break", longBreakMinutes) { longBreakMinutes = it; if (phase == PomodoroPhase.LONG_BREAK) secondsLeft = it * 60 }
            Text("A long break follows every 4 focus sessions.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Multiple one-time-per-day alarms, each independently toggled and scheduled through
 * [TimerAlarmScheduler] - replaces what used to be a single ephemeral alarm slot. The
 * [LaunchedEffect] below keeps the system [android.app.AlarmManager] in sync with the persisted
 * list every time it changes, so toggling one off (or deleting it) actually cancels it rather
 * than just hiding it from the UI.
 */
@Composable
private fun AlarmSection(
    alarms: List<AlarmItem>,
    onAdd: (hour: Int, minute: Int, label: String) -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onDelete: (id: String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(alarms) {
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                TimerAlarmScheduler.scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute, alarm.label)
            } else {
                TimerAlarmScheduler.cancelAlarm(context, alarm.id)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Alarms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        if (alarms.isEmpty()) {
            Text(
                "No alarms yet — tap \"Add alarm\" below.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                alarms.sortedWith(compareBy({ it.hour }, { it.minute })).forEach { alarm ->
                    key(alarm.id) {
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { enabled -> onToggle(alarm.id, enabled) },
                            onDelete = { onDelete(alarm.id) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showAddDialog = true }) { Text("Add alarm") }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { hour, minute, label ->
                onAdd(hour, minute, label)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AlarmRow(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.titleMedium)
            Text(alarm.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete alarm at ${alarm.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (hour: Int, minute: Int, label: String) -> Unit) {
    val now = java.util.Calendar.getInstance()
    var hour by rememberSaveable { mutableIntStateOf(now.get(java.util.Calendar.HOUR_OF_DAY)) }
    var minute by rememberSaveable { mutableIntStateOf(now.get(java.util.Calendar.MINUTE)) }
    var label by rememberSaveable { mutableStateOf("Zenith alarm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add alarm") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { hour = (hour + 23) % 24 }) { Text("−") }
                    Text("Hour")
                    IconButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                    IconButton(onClick = { minute = (minute + 59) % 60 }) { Text("−") }
                    Text("Min")
                    IconButton(onClick = { minute = (minute + 1) % 60 }) { Text("+") }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute, label.ifBlank { "Zenith alarm" }) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DurationStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("$label (min)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > 1) onChange(value - 1) }) { Text("−") }
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onChange((value + 1).coerceAtMost(120)) }) { Text("+") }
    }
}
private fun formatSeconds(totalSeconds: Int) = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
