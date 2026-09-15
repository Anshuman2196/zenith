package com.zenith.launcher.ui.home.components

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.util.TimerAlarmScheduler
import kotlinx.coroutines.delay

private enum class TimerMode { POMODORO, STOPWATCH, ALARM }
private enum class PomodoroPhase(val label: String) { STUDY("Focus"), SHORT_BREAK("Short break"), LONG_BREAK("Long break") }

/** A deliberate study/break timer: completing a phase pauses for confirmation instead of silently
 * rolling into the next one. A system alarm is scheduled while it runs, so completion is still
 * announced when Zenith is in the background. */
@Composable
fun PomodoroWidget(onPomodoroRunningChanged: (Boolean) -> Unit = {}) {
    var mode by rememberSaveable { mutableStateOf(TimerMode.POMODORO) }
    WidgetCard {
        Text("Study timer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = mode.ordinal) {
            Tab(mode == TimerMode.POMODORO, { mode = TimerMode.POMODORO }, text = { Text("Pomodoro") })
            Tab(mode == TimerMode.STOPWATCH, { mode = TimerMode.STOPWATCH }, text = { Text("Stopwatch") })
            Tab(mode == TimerMode.ALARM, { mode = TimerMode.ALARM }, text = { Text("Alarm") })
        }
        Spacer(Modifier.height(12.dp))
        when (mode) {
            TimerMode.POMODORO -> PomodoroSection(onPomodoroRunningChanged)
            TimerMode.STOPWATCH -> StopwatchSection()
            TimerMode.ALARM -> AlarmSection()
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
private fun PomodoroSection(onPomodoroRunningChanged: (Boolean) -> Unit) {
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

    LaunchedEffect(isRunning) { onPomodoroRunningChanged(isRunning) }
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
        if (!isRunning) return@LaunchedEffect
        TimerAlarmScheduler.schedule(context, secondsLeft, "${phase.label} is complete")
        while (isRunning && secondsLeft > 0) { delay(1000); secondsLeft-- }
        if (isRunning && secondsLeft == 0) {
            TimerAlarmScheduler.cancel(context)
            isRunning = false
            isRinging = true
            advance()
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
        Text(phase.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(formatSeconds(secondsLeft), style = MaterialTheme.typography.headlineMedium)
        if (awaitingNextPhase) Text("Take a moment — start when ready.", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { isRunning = !isRunning; awaitingNextPhase = false; isRinging = false }) { Text(if (isRunning) "Pause" else if (awaitingNextPhase) "Start ${phase.label}" else "Start") }
            OutlinedButton(onClick = {
                isRunning = false; awaitingNextPhase = false; phase = PomodoroPhase.STUDY
                secondsLeft = focusMinutes * 60; completedFocusSessions = 0; isRinging = false; TimerAlarmScheduler.cancel(context)
            }) { Text("Reset") }
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

@Composable
private fun AlarmSection() {
    val context = LocalContext.current
    val now = java.util.Calendar.getInstance()
    var hour by rememberSaveable { mutableIntStateOf(now.get(java.util.Calendar.HOUR_OF_DAY)) }
    var minute by rememberSaveable { mutableIntStateOf(now.get(java.util.Calendar.MINUTE)) }
    var scheduled by rememberSaveable { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Alarm", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { hour = (hour + 23) % 24 }) { Text("−") }
            Text("Hour")
            IconButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
            IconButton(onClick = { minute = (minute + 59) % 60 }) { Text("−") }
            Text("Min")
            IconButton(onClick = { minute = (minute + 1) % 60 }) { Text("+") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = {
                TimerAlarmScheduler.scheduleAlarm(context, hour, minute, "Zenith alarm")
                scheduled = true
            }) { Text(if (scheduled) "Alarm set" else "Set alarm") }
            if (scheduled) OutlinedButton(onClick = { TimerAlarmScheduler.cancelAlarm(context); scheduled = false }) { Text("Cancel") }
        }
        Text(if (scheduled) "Your alarm will ring at %02d:%02d.".format(hour, minute) else "Set a one-time alarm for your next reminder.", style = MaterialTheme.typography.labelSmall)
    }
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
