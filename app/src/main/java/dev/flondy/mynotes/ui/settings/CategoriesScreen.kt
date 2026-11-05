package dev.flondy.mynotes.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.R
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.components.EmptyState
import dev.flondy.mynotes.ui.components.IconTile
import dev.flondy.mynotes.ui.components.LeatherPage
import dev.flondy.mynotes.ui.components.TextInputDialog
import dev.flondy.mynotes.ui.theme.LocalAppExtras

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    counts: Map<Int, Int>,
    onBack: () -> Unit,
    onAdd: (name: String, icon: String) -> Unit,
    onUpdate: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    BackHandler(onBack = onBack)
    var adding by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val extras = LocalAppExtras.current

    LeatherPage(
        title = stringResource(R.string.categories_title),
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.category_add), style = MaterialTheme.typography.labelLarge) },
                containerColor = extras.accent,
                contentColor = extras.onAccent,
            )
        },
    ) { bottom ->
        if (categories.isEmpty()) {
            EmptyState(Icons.Outlined.Label, stringResource(R.string.categories_empty_title), stringResource(R.string.categories_empty_subtitle), Modifier.padding(top = 32.dp))
        } else {
            LazyColumn(
                Modifier.widthIn(max = dev.flondy.mynotes.ui.components.rememberWindowLayout().readingMaxWidth).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(categories, key = { it.id }) { c ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = extras.card,
                        border = androidx.compose.foundation.BorderStroke(1.dp, extras.cardBorder),
                        shadowElevation = 1.dp,
                        onClick = { editId = c.id },
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconTile(CategoryIcons.forCategory(c), size = 48.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                val n = counts[c.id] ?: 0
                                Text(pluralStringResource(R.plurals.note_count, n, n), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editId = c.id }) { Icon(Icons.Outlined.Edit, stringResource(R.string.category_edit)) }
                            IconButton(onClick = { deleteId = c.id }) { Icon(Icons.Outlined.Delete, stringResource(R.string.category_delete)) }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        CategoryDialog(stringResource(R.string.category_add), "", "idea", stringResource(R.string.action_add),
            onConfirm = { name, icon -> onAdd(name, icon) }, onDismiss = { adding = false })
    }
    categories.firstOrNull { it.id == editId }?.let { c ->
        CategoryDialog(stringResource(R.string.category_edit), c.name, CategoryIcons.keyFor(c), stringResource(R.string.action_save),
            onConfirm = { name, icon -> onUpdate(c.copy(name = name, icon = icon)) }, onDismiss = { editId = null })
    }
    categories.firstOrNull { it.id == deleteId }?.let { c ->
        ConfirmDialog(
            title = stringResource(R.string.category_delete),
            text = stringResource(R.string.category_delete_confirm, c.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { onDelete(c) },
            onDismiss = { deleteId = null },
            destructive = true,
        )
    }
}

/** Name + Icon wählen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    confirmLabel: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    val extras = LocalAppExtras.current
    TextInputDialog(
        title = title,
        initial = initialName,
        confirmLabel = confirmLabel,
        onConfirm = { name -> onConfirm(name, icon) },
        onDismiss = onDismiss,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryIcons.all.forEach { (key, style) ->
                val sel = key == icon
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(if (sel) 3.dp else 0.dp, if (sel) extras.accent else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton) { icon = key }
                        .semantics { contentDescription = key; selected = sel }
                        .padding(3.dp),
                ) {
                    IconTile(style, size = 40.dp)
                }
            }
        }
    }
}
