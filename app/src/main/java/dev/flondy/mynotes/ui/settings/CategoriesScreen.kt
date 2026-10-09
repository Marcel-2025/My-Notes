package dev.flondy.mynotes.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.R
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.components.EmptyState
import dev.flondy.mynotes.ui.components.TextInputDialog
import dev.flondy.mynotes.ui.theme.LocalAppExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Category, String) -> Unit,
    onDelete: (Category) -> Unit,
) {
    BackHandler(onBack = onBack)
    var adding by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val extras = LocalAppExtras.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.category_add)) },
                containerColor = extras.accent,
                contentColor = extras.onAccent,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (categories.isEmpty()) {
                EmptyState(Icons.Outlined.Label, stringResource(R.string.categories_empty_title), stringResource(R.string.categories_empty_subtitle))
            } else {
                LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 120.dp)) {
                    items(categories, key = { it.id }) { c ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).animateItem(),
                        ) {
                            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Label, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(16.dp))
                                Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { renameId = c.id }) { Icon(Icons.Outlined.Edit, stringResource(R.string.category_rename)) }
                                IconButton(onClick = { deleteId = c.id }) { Icon(Icons.Outlined.Delete, stringResource(R.string.category_delete)) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(stringResource(R.string.category_add), "", stringResource(R.string.action_add), onAdd) { adding = false }
    }
    categories.firstOrNull { it.id == renameId }?.let { c ->
        TextInputDialog(stringResource(R.string.category_rename), c.name, stringResource(R.string.action_save), { onRename(c, it) }) { renameId = null }
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
