package dev.flondy.mynotes.ui.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.NoteTemplate
import dev.flondy.mynotes.data.NoteTemplates
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.IconTile

/** Vorlagen-Auswahl: öffnet den Editor mit vorausgefülltem Inhalt. */
@Composable
fun TemplatePickerDialog(
    onPick: (NoteTemplate) -> Unit,
    onDismiss: () -> Unit,
    /** 2.6: eigene Vorlagen (neueste oben). */
    custom: List<NoteTemplate> = emptyList(),
    onDelete: (String) -> Unit = {},
) {
    val lang = stringResource(R.string.template_lang)
    val templates = remember(lang) { NoteTemplates.forLanguage(lang) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("template_picker"),
        title = { Text(stringResource(R.string.templates_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (custom.isNotEmpty()) {
                    Text(stringResource(R.string.templates_mine), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
                    custom.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(t) }
                                .padding(vertical = 6.dp, horizontal = 6.dp).testTag("template_" + t.id),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(CategoryIcons.style(t.icon) ?: CategoryIcons.all.first().second, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(t.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            androidx.compose.material3.IconButton(onClick = { onDelete(t.id) }) {
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Delete, stringResource(R.string.template_delete))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
                }
                templates.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(t) }
                            .padding(vertical = 8.dp, horizontal = 6.dp).testTag("template_" + t.id),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(CategoryIcons.style(t.icon) ?: CategoryIcons.all.first().second, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
