package com.zenith.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.ui.settings.SettingsViewModel

private enum class OnboardingStep {
    WELCOME, AWARENESS, FOCUS, DISTRACTIONS, HOME, WIDGETS, NAME
}

private val steps = OnboardingStep.entries

@Composable
fun OnboardingScreen(settingsViewModel: SettingsViewModel, onFinished: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var demoHeight by remember { mutableIntStateOf(104) }
    val state by settingsViewModel.uiState.collectAsState()
    val step = steps[stepIndex]
    val isLast = step == OnboardingStep.NAME

    fun finish() {
        name.trim().takeIf { it.isNotEmpty() }?.let(settingsViewModel::updateProfileName)
        onFinished()
    }

    CompositionLocalProvider(LocalContentColor provides androidx.compose.ui.graphics.Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stepIndex > 0) {
                IconButton(onClick = { stepIndex-- }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = ::finish) { Text("Skip") }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.AWARENESS -> AwarenessStep()
                OnboardingStep.FOCUS -> AppChoiceStep(
                    title = "Choose what Focus means to you",
                    body = "These are the apps Zenith can keep available while Focus Mode is active. You decide what belongs in that space.",
                    apps = state.installedApps,
                    selected = state.focusAllowedApps,
                    emptyText = "No apps are available yet. You can choose them later in Settings → Apps.",
                    onToggle = settingsViewModel::toggleFocusAllowedApp
                )
                OnboardingStep.DISTRACTIONS -> AppChoiceStep(
                    title = "Choose where you want a pause",
                    body = "These apps can receive a small pause before opening during Focus Mode. They aren't bad apps — they're simply apps you've chosen to notice.",
                    apps = state.installedApps,
                    selected = state.distractionApps,
                    emptyText = "No apps are available yet. You can choose them later in Settings → Apps.",
                    onToggle = settingsViewModel::toggleDistractionApp
                )
                OnboardingStep.HOME -> HomeStep()
                OnboardingStep.WIDGETS -> WidgetsStep(
                    visibility = state.widgetVisibility,
                    demoHeight = demoHeight,
                    onHeightChange = { demoHeight = it },
                    onVisibilityChange = settingsViewModel::setWidgetVisibility
                )
                OnboardingStep.NAME -> NameStep(name = name, onNameChange = { name = it })
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == stepIndex) 22.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (index == stepIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (isLast) finish() else stepIndex++ },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isLast) "Enter Zenith" else if (step == OnboardingStep.FOCUS || step == OnboardingStep.DISTRACTIONS) "Continue" else "Continue")
                if (!isLast) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
        }
    }
}

@Composable
private fun StepShell(
    eyebrow: String,
    title: String,
    body: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.height(20.dp))
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
private fun WelcomeStep() = StepShell(
    eyebrow = "Welcome to Zenith",
    title = "A different kind of home screen",
    body = "Your phone can pull you from one thing to another. Zenith gives you a calmer place to begin — with the things you care about closer to the surface.",
    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    InfoCard("Your home, your choices", "Zenith doesn't decide what you should do. You choose what belongs here, what Focus Mode means, and what deserves a pause.")
}

@Composable
private fun AwarenessStep() = StepShell(
    eyebrow = "The idea behind Zenith",
    title = "Notice before you move",
    body = "Sometimes we reach for an app without really deciding to. Zenith can create a small moment between the impulse and the action — enough time to notice and choose.",
    icon = { Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("Impulse", "Notice", "Choose", "Act").forEachIndexed { index, label ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(label, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
            }
            if (index < 3) Text("↓", color = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun AppChoiceStep(
    title: String,
    body: String,
    apps: List<AppInfo>,
    selected: Set<String>,
    emptyText: String,
    onToggle: (String) -> Unit
) = StepShell(
    eyebrow = "Make it yours",
    title = title,
    body = body,
    icon = { Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    if (apps.isEmpty()) {
        InfoCard("You can do this later", emptyText)
    } else {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (selected.isEmpty()) "Nothing selected yet" else "${selected.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                apps.sortedBy { it.label.lowercase() }.take(40).forEach { app ->
                    val checked = app.packageName in selected
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(app.packageName) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) { Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (checked) Icon(Icons.Outlined.Check, contentDescription = "Selected")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("You can change these choices later in Settings → Apps.", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeStep() = StepShell(
    eyebrow = "Your home space",
    title = "Keep what matters close",
    body = "Zenith is built from small pieces of information you choose to keep nearby — deadlines, study targets, your backlog, library links, Pomodoro, and more.",
    icon = { Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("DEADLINES", "STUDY TARGET", "BACKLOG", "POMODORO").forEachIndexed { index, label ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Text(if (index == 0) "12 days" else "Ready", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun WidgetsStep(
    visibility: WidgetVisibility,
    demoHeight: Int,
    onHeightChange: (Int) -> Unit,
    onVisibilityChange: ((WidgetVisibility) -> WidgetVisibility) -> Unit
) = StepShell(
    eyebrow = "Shape the space",
    title = "Enable, hide, move, resize",
    body = "Every widget is optional. Turn on what helps, hide what doesn't, and resize the space until it feels right. You can change everything later in Settings → Widgets.",
    icon = { Icon(Icons.Outlined.DragHandle, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Study target", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.DragHandle, contentDescription = "Resize handle")
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(demoHeight.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("80 / 100", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Drag the handle to resize", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onHeightChange(if (demoHeight >= 184) 104 else demoHeight + 40) }) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = "Resize")
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetChip("Deadlines", visibility.deadlinesEnabled) { onVisibilityChange { it.copy(deadlinesEnabled = !visibility.deadlinesEnabled) } }
            WidgetChip("Pomodoro", visibility.pomodoroEnabled) { onVisibilityChange { it.copy(pomodoroEnabled = !visibility.pomodoroEnabled) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetChip("Backlog", visibility.backlogEnabled) { onVisibilityChange { it.copy(backlogEnabled = !visibility.backlogEnabled) } }
            WidgetChip("Library", visibility.libraryEnabled) { onVisibilityChange { it.copy(libraryEnabled = !visibility.libraryEnabled) } }
        }
    }
}

@Composable
private fun WidgetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, color = Color.White) }, leadingIcon = if (selected) {
        { Icon(Icons.Outlined.Check, contentDescription = null) }
    } else null)
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) = StepShell(
    eyebrow = "One last thing",
    title = "Make it a little more personal",
    body = "What should Zenith call you? You can skip this and change it later.",
    icon = { Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(30.dp)) }
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        singleLine = true,
        label = { Text("Your name", color = Color.White) },
        placeholder = { Text("Student", color = Color.White.copy(alpha = 0.7f)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.55f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
            cursorColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

