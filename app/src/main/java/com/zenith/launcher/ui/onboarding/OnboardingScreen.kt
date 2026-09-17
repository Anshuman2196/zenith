package com.zenith.launcher.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.ui.settings.SettingsViewModel

private enum class OnboardingStep(val eyebrow: String, val title: String) {
    WELCOME("WELCOME TO ZENITH", "Your home screen, with a little more intention."),
    NOTICE("THE IDEA", "Catch the moment before it becomes automatic."),
    FOCUS("FOCUS", "Decide what gets to stay close."),
    PAUSE("PAUSES", "Choose where Zenith should slow things down."),
    HOME("YOUR SPACE", "Put the useful things where you can see them."),
    PERSONALIZE("MAKE IT YOURS", "A few choices, then you're in."),
    NAME("ALMOST THERE", "What should we call you?")
}

private val steps = OnboardingStep.entries

@Composable
fun OnboardingScreen(settingsViewModel: SettingsViewModel, onFinished: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val state by settingsViewModel.uiState.collectAsState()
    val step = steps[index]

    fun finish() {
        name.trim().takeIf { it.isNotEmpty() }?.let(settingsViewModel::updateProfileName)
        onFinished()
    }

    fun next() {
        if (index == steps.lastIndex) finish() else index++
    }

    BackHandler(enabled = index > 0) { index-- }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0F))
                .navigationBarsPadding()
        ) {
            OnboardingGlow(step = index)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                TopBar(index = index, onBack = { index-- }, onSkip = ::finish)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(index) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, amount -> totalDrag += amount },
                                onDragEnd = {
                                    if (totalDrag < -90f && index < steps.lastIndex) index++
                                    else if (totalDrag > 90f && index > 0) index--
                                }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 8 }) togetherWith
                                (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 10 })
                        },
                        label = "onboarding-step"
                    ) { current ->
                        when (current) {
                            OnboardingStep.WELCOME -> WelcomeStep()
                            OnboardingStep.NOTICE -> NoticeStep()
                            OnboardingStep.FOCUS -> AppChoiceStep(
                                icon = Icons.Outlined.School,
                                title = "What belongs in Focus?",
                                body = "Pick the apps you want available when you intentionally enter Focus Mode.",
                                apps = state.installedApps,
                                selected = state.focusAllowedApps,
                                emptyText = "You can choose these later from Settings → Apps.",
                                onToggle = settingsViewModel::toggleFocusAllowedApp
                            )
                            OnboardingStep.PAUSE -> AppChoiceStep(
                                icon = Icons.Outlined.PauseCircleOutline,
                                title = "Where would a pause help?",
                                body = "Pick apps where you want Zenith to create a short moment to notice before opening them.",
                                apps = state.installedApps,
                                selected = state.distractionApps,
                                emptyText = "You can choose these later from Settings → Apps.",
                                onToggle = settingsViewModel::toggleDistractionApp
                            )
                            OnboardingStep.HOME -> HomeStep(
                                visibility = state.widgetVisibility,
                                onVisibilityChange = settingsViewModel::setWidgetVisibility
                            )
                            OnboardingStep.PERSONALIZE -> PersonalizeStep()
                            OnboardingStep.NAME -> NameStep(name = name, onNameChange = { name = it })
                        }
                    }
                }

                ProgressDots(index)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = ::next,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (index == steps.lastIndex) "Enter Zenith" else buttonLabel(step))
                    if (index != steps.lastIndex) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Swipe left or right to move through the intro",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.48f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TopBar(index: Int, onBack: () -> Unit, onSkip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (index > 0) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSkip) { Text("Skip") }
    }
}

@Composable
private fun ProgressDots(index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.indices.forEach { item ->
            val active = item == index
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (active) 24.dp else 6.dp, 6.dp)
                    .clip(CircleShape)
                    .background(if (active) Color.White else Color.White.copy(alpha = 0.25f))
            )
        }
    }
}

private fun buttonLabel(step: OnboardingStep): String = when (step) {
    OnboardingStep.WELCOME -> "Let's look around"
    OnboardingStep.NOTICE -> "Show me"
    OnboardingStep.FOCUS, OnboardingStep.PAUSE -> "Continue"
    OnboardingStep.HOME -> "Shape my home"
    OnboardingStep.PERSONALIZE -> "One last choice"
    OnboardingStep.NAME -> "Enter Zenith"
}

@Composable
private fun OnboardingGlow(step: Int) {
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(260.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse, alpha = 0.16f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.TopEnd)
                .padding(top = (step * 2).dp)
        )
    }
}

@Composable
private fun StepHeader(icon: @Composable () -> Unit, eyebrow: String, title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(18.dp))
        Text(eyebrow, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.62f))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.76f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeStep() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            StepHeader(
                icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(38.dp)) },
                eyebrow = OnboardingStep.WELCOME.eyebrow,
                title = OnboardingStep.WELCOME.title,
                body = "Zenith turns your home screen into a place to see what matters, notice what is happening, and choose what comes next."
            )
            Spacer(Modifier.height(28.dp))
            InteractivePhonePreview()
            Spacer(Modifier.height(18.dp))
            FeatureRow(Icons.Outlined.School, "Study tools", "Targets, deadlines, backlog and Pomodoro")
            FeatureRow(Icons.Outlined.Swipe, "Gentle friction", "Small pauses when you choose them")
            FeatureRow(Icons.Outlined.Tune, "Your rules", "You decide what stays visible and what gets a pause")
        }
    }
}

@Composable
private fun InteractivePhonePreview() {
    var selected by remember { mutableIntStateOf(0) }
    val labels = listOf("Today", "Focus", "Tasks")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zenith", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("9:41", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            Text("A little more intention.", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selected == index,
                        onClick = { selected = index },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(targetState = selected, label = "preview") { tab ->
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.07f)) {
                    Text(
                        listOf("Your day at a glance.", "One thing can be enough for now.", "A clear next step is easier to see.")[tab],
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeStep() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            StepHeader(
                icon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(38.dp)) },
                eyebrow = OnboardingStep.NOTICE.eyebrow,
                title = OnboardingStep.NOTICE.title,
                body = "Zenith is not here to tell you what to do. It can simply make an automatic moment a little easier to notice."
            )
            Spacer(Modifier.height(28.dp))
            AwarenessFlow()
            Spacer(Modifier.height(18.dp))
            QuoteCard("Impulse → Notice → Choose → Act", "The pause is the space between the first feeling and the next action.")
        }
    }
}

@Composable
private fun AwarenessFlow() {
    val transition = rememberInfiniteTransition(label = "flow")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart),
        label = "flow-offset"
    )
    val items = listOf("Impulse", "Notice", "Choose", "Act")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEachIndexed { index, item ->
            val emphasis = if (((offset * 4f).toInt() % 4) == index) 1f else 0.62f
            Surface(
                modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = emphasis),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(14.dp))
                    Text(item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("0${index + 1}", color = Color.White.copy(alpha = 0.42f))
                }
            }
        }
    }
}

@Composable
private fun AppChoiceStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    apps: List<AppInfo>,
    selected: Set<String>,
    emptyText: String,
    onToggle: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        StepHeader(icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(38.dp)) }, eyebrow = if (title.startsWith("What")) OnboardingStep.FOCUS.eyebrow else OnboardingStep.PAUSE.eyebrow, title = title, body = body)
        Spacer(Modifier.height(18.dp))
        if (apps.isEmpty()) {
            QuoteCard("Nothing to pick yet", emptyText)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Tap to choose · ${selected.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(apps, key = { it.packageName + it.activityClassName }) { app ->
                    val isSelected = app.packageName in selected
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onToggle(app.packageName) },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.07f))
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Icon(if (isSelected) Icons.Outlined.Check else Icons.Outlined.Apps, contentDescription = null, tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStep(visibility: WidgetVisibility, onVisibilityChange: ((WidgetVisibility) -> WidgetVisibility) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            StepHeader(
                icon = { Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(38.dp)) },
                eyebrow = OnboardingStep.HOME.eyebrow,
                title = OnboardingStep.HOME.title,
                body = "Zenith is made from small pieces. Turn on the ones you want, then arrange them later on your home screen."
            )
            Spacer(Modifier.height(22.dp))
        }
        item { WidgetToggle("Deadlines", "Your three nearest dates", visibility.deadlinesEnabled) { onVisibilityChange { it.copy(deadlinesEnabled = !visibility.deadlinesEnabled) } } }
        item { WidgetToggle("Study target", "A direction, not a verdict", visibility.targetsEnabled) { onVisibilityChange { it.copy(targetsEnabled = !visibility.targetsEnabled) } } }
        item { WidgetToggle("Pomodoro", "Work and pause in one place", visibility.pomodoroEnabled) { onVisibilityChange { it.copy(pomodoroEnabled = !visibility.pomodoroEnabled) } } }
        item { WidgetToggle("Backlog", "Things you want out of your head", visibility.backlogEnabled) { onVisibilityChange { it.copy(backlogEnabled = !visibility.backlogEnabled) } } }
        item { QuoteCard("Nothing is permanent", "You can hide, move and resize widgets any time from Home layout in Settings.") }
    }
}

@Composable
private fun WidgetToggle(label: String, subtitle: String, enabled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = if (enabled) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.07f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = if (enabled) 0.16f else 0.07f)), contentAlignment = Alignment.Center) {
                Icon(if (enabled) Icons.Outlined.Check else Icons.Outlined.Widgets, contentDescription = null)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
            }
            Text(if (enabled) "On" else "Off", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun PersonalizeStep() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = { Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(38.dp)) },
            eyebrow = OnboardingStep.PERSONALIZE.eyebrow,
            title = OnboardingStep.PERSONALIZE.title,
            body = "The rest of Zenith lives in settings. You can change the look, gestures, apps, widgets and attention behavior whenever you want."
        )
        Spacer(Modifier.height(28.dp))
        listOf(
            Icons.Outlined.DarkMode to "Appearance",
            Icons.Outlined.Widgets to "Home layout",
            Icons.Outlined.FilterAlt to "Apps & pauses",
            Icons.Outlined.Swipe to "Gestures & attention"
        ).forEach { (icon, label) ->
            FeatureRow(icon, label, "Change it later")
        }
        Spacer(Modifier.height(12.dp))
        QuoteCard("You are always in charge", "Zenith is a tool for awareness, not a judge of how you spend your time.")
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = { Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(38.dp)) },
            eyebrow = OnboardingStep.NAME.eyebrow,
            title = OnboardingStep.NAME.title,
            body = "This is optional. Leave it blank and Zenith will simply say Student."
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            label = { Text("Your name") },
            placeholder = { Text("Student", color = Color.White.copy(alpha = 0.45f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        AnimatedVisibility(visible = name.isNotBlank(), enter = fadeIn() + scaleIn(), exit = fadeOut()) {
            QuoteCard("Nice to meet you, ${name.trim()}", "You can change this later in Profile.")
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.09f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun QuoteCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.075f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.68f))
        }
    }
}
