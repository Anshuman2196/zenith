package com.zenith.launcher.ui.home.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.home.ZenithCopy
import com.zenith.launcher.util.StudyTimerScheduler
import kotlinx.coroutines.delay


private enum class PomodoroPhase(val label: String) { STUDY("Focus"), SHORT_BREAK("Short break"), LONG_BREAK("Long break") }

/**
 * A deliberate study/break timer: completing a phase pauses for confirmation instead of silently
 * rolling into the next one. A system alarm is scheduled while it runs, so completion is still
 * announced when Zenith is in the background.
 *
 * While a Pomodoro session is actually running, the rest of Home is intentionally locked down
 * (see [com.zenith.launcher.ui.home.HomeScreen]).
 */
@Composable
fun PomodoroWidget(
    onPomodoroRunningChanged: (Boolean) -> Unit = {},
    onPomodoroProtectionChanged: (Boolean) -> Unit = {},
    onPomodoroPauseChanged: (Boolean, Int, String) -> Unit = { _, _, _ -> }
) {
    WidgetCard {
        Text("Study timer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PomodoroSection(
            onPomodoroRunningChanged = onPomodoroRunningChanged,
            onPomodoroProtectionChanged = onPomodoroProtectionChanged,
            onPomodoroPauseChanged = onPomodoroPauseChanged
        )
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun DurationStepper(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                enabled = value > 1,
                onClick = { onValueChange((value - 1).coerceAtLeast(1)) }
            ) { Text("−") }
            Text(
                text = "$value min",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(64.dp)
            )
            TextButton(
                enabled = value < 120,
                onClick = { onValueChange((value + 1).coerceAtMost(120)) }
            ) { Text("+") }
        }
    }
}

@Composable
private fun PomodoroSection(
    onPomodoroRunningChanged: (Boolean) -> Unit,
    onPomodoroProtectionChanged: (Boolean) -> Unit,
    onPomodoroPauseChanged: (Boolean, Int, String) -> Unit
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
    LaunchedEffect(stopPauseSeconds, awaitingNextPhase) {
        onPomodoroProtectionChanged(stopPauseSeconds > 0 || awaitingNextPhase)
        onPomodoroPauseChanged(stopPauseSeconds > 0, stopPauseSeconds, stopPauseMessage)
    }
    DisposableEffect(Unit) {
        onDispose {
            onPomodoroProtectionChanged(false)
            onPomodoroPauseChanged(false, 0, "")
        }
    }
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
        StudyTimerScheduler.schedule(context, secondsLeft, "${phase.label} is complete")
        while (isRunning && secondsLeft > 0 && stopPauseSeconds == 0) { delay(1000); secondsLeft-- }
        if (isRunning && secondsLeft == 0 && stopPauseSeconds == 0) {
            StudyTimerScheduler.cancel(context)
            isRunning = false
            stopPauseSeconds = 7
            stopPauseKind = 2
            stopPauseMessage = ZenithCopy.pomodoroComplete.random()
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
        Text(phase.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(formatSeconds(secondsLeft), style = MaterialTheme.typography.headlineMedium)
        if (awaitingNextPhase) Text(ZenithCopy.pomodoroTransition.random(), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                enabled = stopPauseSeconds == 0,
                onClick = {
                    // Pause remains immediate; only an explicit Stop gets the reflective seven-second pause.
                    isRunning = !isRunning
                    awaitingNextPhase = false
                    isRinging = false
                }
            ) { Text(if (isRunning) "Pause" else if (awaitingNextPhase) "Start ${phase.label}" else "Start") }
            OutlinedButton(
                enabled = stopPauseSeconds == 0,
                onClick = {
                    StudyTimerScheduler.cancel(context)
                    if (isRunning) {
                        isRunning = false
                        stopPauseSeconds = 7
                        stopPauseKind = 1
                        stopPauseMessage = ZenithCopy.pomodoroStop.random()
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
