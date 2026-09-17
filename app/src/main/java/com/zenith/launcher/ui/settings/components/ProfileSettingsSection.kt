package com.zenith.launcher.ui.settings.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/** Profile Settings: edit + save the student's name, which drives the Home screen greeting. */
@Composable
fun ProfileSettingsSection(currentName: String, onSave: (String) -> Unit) {
    var draftName by remember(currentName) { mutableStateOf(currentName) }

    SettingsSectionCard(title = "Profile") {
        OutlinedTextField(
            value = draftName,
            onValueChange = { draftName = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSave(draftName.trim()) }, enabled = draftName.isNotBlank() && draftName != currentName) {
                Text("Save")
            }
        }
    }
}
