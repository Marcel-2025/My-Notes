package dev.flondy.mynotes.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.components.EmptyState
import dev.flondy.mynotes.ui.components.formatRelative

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    notes: List<Note>,
    retentionDays: Int,
    onBack: () -> Unit,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                actions = {
                    if (notes.isNotEmpty()) TextButton(onClick = { confirmEmpty = true }) {
                        Text(stringResource(R.string.trash_empty), color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (notes.isEmpty()) {
                EmptyState(Icons.Outlined.DeleteOutline, stringResource(R.string.trash_empty_title), stringResource(R.string.trash_info, retentionDays), Modifier.padding(top = 48.dp))
            } else {
                LazyColumn(
                    Modifier.widthIn(max = 760.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.trash_info, retentionDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                    items(notes, key = { it.id }) { note ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().animateItem(),
                        ) {
                            Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        note.title.ifBlank { stringResource(R.string.untitled_note) },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    val preview = if (note.isChecklist) ChecklistCodec.toShareText(note.content) else note.content
                                    if (preview.isNotBlank()) {
                                        Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    note.deletedAt?.let {
                                        Text(stringResource(R.string.trash_deleted_at, formatRelative(it)), style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onRestore(note) }) {
                                    Icon(Icons.Outlined.RestoreFromTrash, stringResource(R.string.action_restore))
                                }
                                IconButton(onClick = { confirmDeleteId = note.id }) {
                                    Icon(Icons.Outlined.DeleteForever, stringResource(R.string.action_delete_forever), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = stringResource(R.string.trash_empty),
            text = stringResource(R.string.trash_empty_confirm, notes.size),
            confirmLabel = stringResource(R.string.action_delete_forever),
            onConfirm = onEmptyTrash,
            onDismiss = { confirmEmpty = false },
            destructive = true,
        )
    }
    notes.firstOrNull { it.id == confirmDeleteId }?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.action_delete_forever),
            text = stringResource(R.string.delete_forever_confirm),
            confirmLabel = stringResource(R.string.action_delete_forever),
            onConfirm = { onDeleteForever(note) },
            onDismiss = { confirmDeleteId = null },
            destructive = true,
        )
    }
}
