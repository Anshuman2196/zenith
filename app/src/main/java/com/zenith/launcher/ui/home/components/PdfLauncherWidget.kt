package com.zenith.launcher.ui.home.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.PdfLink

/** Widget 5: Quick Access PDF Launcher - one-tap shortcuts to notes, papers, formula sheets. */
@Composable
fun PdfLauncherWidget(
    links: List<PdfLink>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current

    WidgetCard {
        WidgetHeaderRow(title = "Study PDFs", onAddClick = onAddClick)

        if (links.isEmpty()) {
            EmptyHint("No PDFs linked yet - tap + to add one")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                links.forEach { link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openPdf(context, link.uriString) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(link.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                        IconButton(onClick = { onDelete(link.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Opens a linked PDF with whatever PDF viewer the user has installed. */
private fun openPdf(context: Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
