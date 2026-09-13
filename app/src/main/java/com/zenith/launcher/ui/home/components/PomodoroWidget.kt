package com.zenith.launcher.ui.home.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

private enum class TimerMode { POMODORO, STOPWATCH }
private enum class PomodoroPhase { STUDY, BREAK }

/**
 * Widget 2: Study Timer / Pomodoro.
 * Two tabs: a plain count-up Stopwatch, and a Pomodoro timer that alternates
 * Study <-> Break for user-configurable durations (default 25 min study / 5 min break).
 * State is local to this composable - a running timer is transient session state, not
 * something that needs to survive an app restart.
 */
@Composable
fun PomodoroWidget() {
    var mode by rememberSaveable { mutableStateOf(TimerMode.POMODORO) }

    WidgetCard {
        Text("Study Timer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = mode.ordinal) {
            Tab(selected = mode == TimerMode.POMODORO, onClick = { mode = TimerMode.POMODORO }, text = { Text("Pomodoro") })
            Tab(selected = mode == TimerMode.STOPWATCH, onClick = { mode = TimerMode.STOPWATCH }, text = { Text("Stopwatch") })
        }

        Spacer(Modifier.height(12.dp))

        when (mode) {
            TimerMode.STOPWATCH -> StopwatchSection()
            TimerMode.POMODORO -> PomodoroSection()
        }
    }
}

@Composable
private fun StopwatchSection() {
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedSeconds++
        }
    }

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
private fun PomodoroSection() {
    var studyMinutes by rememberSaveable { mutableIntStateOf(25) }
    var breakMinutes by rememberSaveable { mutableIntStateOf(5) }

    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.STUDY) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(studyMinutes * 60) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, phase) {
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        if (isRunning && secondsLeft == 0) {
            phase = if (phase == PomodoroPhase.STUDY) PomodoroPhase.BREAK else PomodoroPhase.STUDY
            secondsLeft = if (phase == PomodoroPhase.STUDY) studyMinutes * 60 else breakMinutes * 60
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (phase == PomodoroPhase.STUDY) "Study session" else "Break time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(formatSeconds(secondsLeft), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { isRunning = !isRunning }) { Text(if (isRunning) "Pause" else "Start") }
            OutlinedButton(onClick = {
                isRunning = false
                phase = PomodoroPhase.STUDY
                secondsLeft = studyMinutes * 60
            }) { Text("Reset") }
        }

        Spacer(Modifier.height(12.dp))
        // Duration steppers - only editable while stopped, to avoid confusing mid-session jumps.
        if (!isRunning) {
            DurationStepper(label = "Study (min)", value = studyMinutes, onChange = {
                studyMinutes = it
                if (phase == PomodoroPhase.STUDY) secondsLeft = it * 60
            })
            DurationStepper(label = "Break (min)", value = breakMinutes, onChange = {
                breakMinutes = it
                if (phase == PomodoroPhase.BREAK) secondsLeft = it * 60
            })
        }
    }
}

@Composable
private fun DurationStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > 1) onChange(value - 1) }) { Text("-") }
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
