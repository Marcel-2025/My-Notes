package dev.flondy.mynotes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.R

/** Kontextmenü einer Notiz als Bottom Sheet (Lang drücken oder ⋮). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionsSheet(
    note: Note,
    categories: List<Category>,
    canMove: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onDuplicate: () -> Unit,
    onColor: (Int) -> Unit,
    onCategory: (Int?) -> Unit,
    onShare: () -> Unit,
    onExportTxt: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTrash: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Text(
                note.title.ifBlank { stringResource(R.string.untitled_note) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            SheetActionRow(
                if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                stringResource(if (note.isPinned) R.string.action_unpin else R.string.action_pin),
            ) { onTogglePin(); onDismiss() }
            SheetActionRow(
                if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                stringResource(if (note.isFavorite) R.string.action_unfavorite else R.string.action_favorite),
            ) { onToggleFavorite(); onDismiss() }
            SheetActionRow(
                if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                stringResource(if (note.isArchived) R.string.action_unarchive else R.string.action_archive),
            ) { onToggleArchive(); onDismiss() }
            SheetActionRow(Icons.Outlined.ContentCopy, stringResource(R.string.action_duplicate)) { onDuplicate(); onDismiss() }
            SheetActionRow(Icons.Outlined.Share, stringResource(R.string.action_share)) { onShare(); onDismiss() }
            SheetActionRow(Icons.AutoMirrored.Outlined.InsertDriveFile, stringResource(R.string.action_export_txt)) { onExportTxt(); onDismiss() }
            if (canMove) {
                SheetActionRow(Icons.Filled.ArrowUpward, stringResource(R.string.action_move_up)) { onMoveUp() }
                SheetActionRow(Icons.Filled.ArrowDownward, stringResource(R.string.action_move_down)) { onMoveDown() }
            }

            SheetLabel(stringResource(R.string.label_color))
            ColorPickerRow(selected = note.color, onSelect = onColor)

            SheetLabel(stringResource(R.string.label_category))
            CategoryChips(categories, note.categoryId, onCategory)

            Spacer(Modifier.height(12.dp))
            SheetActionRow(Icons.Outlined.Delete, stringResource(R.string.action_move_to_trash), tint = MaterialTheme.colorScheme.error) {
                onTrash(); onDismiss()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
