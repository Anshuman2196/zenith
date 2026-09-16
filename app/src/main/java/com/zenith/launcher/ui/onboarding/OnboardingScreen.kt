package com.zenith.launcher.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.settings.SettingsViewModel

private data class OnboardingPage(val title: String, val body: String)

private val pages = listOf(
    OnboardingPage("A different kind of home screen", "Your phone can pull you from one thing to another. Zenith gives you a quieter place to begin."),
    OnboardingPage("Notice before you move", "Zenith adds small pauses around attention-heavy actions. Not to stop you — just to give the choice a moment to become conscious."),
    OnboardingPage("Your space, your choices", "Keep the tools that matter close: tasks, study targets, deadlines, your library, and Pomodoro. Arrange the space around how you actually study."),
    OnboardingPage("Make the next choice consciously", "That's the idea behind Zenith. Start simple. You can change everything later in Settings.")
)

@Composable
fun OnboardingScreen(settingsViewModel: SettingsViewModel, onFinished: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val current = pages[page]
    val isLast = page == pages.lastIndex

    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) IconButton(onClick = { page-- }) { Icon(Icons.Outlined.ArrowBack, "Previous") }
            else Spacer(Modifier.width(48.dp))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFinished) { Text("Skip") }
        }

        Spacer(Modifier.weight(1f))
        Text("ZENITH", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(28.dp))
        Text(current.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(current.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        if (isLast) {
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("What should Zenith call you? (optional)") },
                placeholder = { Text("Student") }, modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { index -> Text(if (index == page) "●" else "○", style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (isLast) {
                    name.trim().takeIf { it.isNotEmpty() }?.let(settingsViewModel::updateProfileName)
                    onFinished()
                } else page++
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (isLast) "Enter Zenith" else "Continue")
            if (!isLast) { Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.ArrowForward, null) }
        }
    }
}
