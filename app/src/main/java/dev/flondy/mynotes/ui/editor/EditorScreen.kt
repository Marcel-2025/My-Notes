package dev.flondy.mynotes.ui.editor

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.EditorState
import dev.flondy.mynotes.R
import dev.flondy.mynotes.SaveStatus
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.data.ChecklistItem
import dev.flondy.mynotes.ui.components.CategoryChips
import dev.flondy.mynotes.ui.components.ColorPickerRow
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.components.SheetLabel
import dev.flondy.mynotes.ui.components.noteContainerColor
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion

data class EditorActions(
    val onTitle: (String) -> Unit,
    val onContent: (String) -> Unit,
    val onItems: (List<ChecklistItem>) -> Unit,
    val onTogglePin: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onColor: (Int) -> Unit,
    val onCategory: (Int?) -> Unit,
    val onToggleType: () -> Unit,
    val onShare: () -> Unit,
    val onExportTxt: () -> Unit,
    val onClose: () -> Unit,
    val onDiscard: () -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    categories: List<Category>,
    hideCheckedItems: Boolean,
    actions: EditorActions,
) {
    BackHandler(onBack = actions.onClose)
    val background by animateColorAsState(noteContainerColor(state.color), Motion.tween(300), label = "editorBg")
    var menuOpen by remember { mutableStateOf(false) }
    var showStyleSheet by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (state.focusTitle) runCatching { titleFocus.requestFocus() }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = actions.onClose, modifier = Modifier.testTag("editor_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back_save))
                    }
                },
                title = { SaveIndicator(state.saveStatus, state.noteId != null) },
                actions = {
                    IconButton(onClick = actions.onTogglePin) {
                        Icon(
                            if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            stringResource(if (state.isPinned) R.string.action_unpin else R.string.action_pin),
                        )
                    }
                    IconButton(onClick = actions.onToggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            stringResource(if (state.isFavorite) R.string.action_unfavorite else R.string.action_favorite),
                            tint = if (state.isFavorite) Color(0xFFF5C518) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showStyleSheet = true }) {
                        Icon(Icons.Outlined.Palette, stringResource(R.string.label_color_category))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.a11y_more)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (state.isChecklist) R.string.action_to_text else R.string.action_to_checklist)) },
                                leadingIcon = { Icon(if (state.isChecklist) Icons.AutoMirrored.Outlined.Notes else Icons.Outlined.Checklist, null) },
                                onClick = { menuOpen = false; actions.onToggleType() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                enabled = !state.isBlank,
                                onClick = { menuOpen = false; actions.onShare() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_txt)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null) },
                                enabled = !state.isBlank,
                                onClick = { menuOpen = false; actions.onExportTxt() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_discard)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, null) },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_move_to_trash), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                PlainField(
                    value = state.title,
                    onChange = actions.onTitle,
                    placeholder = stringResource(R.string.editor_title_hint),
                    style = MaterialTheme.typography.headlineSmall,
                    singleLine = false,
                    imeAction = ImeAction.Next,
                    onIme = { runCatching { contentFocus.requestFocus() } },
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus).testTag("editor_title"),
                )
                MetaLine(state, categories)
                Spacer(Modifier.height(12.dp))
                val inSpec = Motion.tween<Float>(200)
                val outSpec = Motion.tween<Float>(150)
                AnimatedContent(
                    targetState = state.isChecklist,
                    transitionSpec = { fadeIn(inSpec) togetherWith fadeOut(outSpec) },
                    label = "editorType",
                ) { checklist ->
                    if (checklist) {
                        ChecklistEditor(state.items, hideCheckedItems, actions.onItems)
                    } else {
                        PlainField(
                            value = state.content,
                            onChange = actions.onContent,
                            placeholder = stringResource(R.string.editor_content_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            singleLine = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp)
                                .focusRequester(contentFocus)
                                .testTag("editor_content"),
                        )
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
                SheetLabel(stringResource(R.string.label_color))
                ColorPickerRow(state.color, actions.onColor)
                SheetLabel(stringResource(R.string.label_category))
                CategoryChips(categories, state.categoryId, actions.onCategory)
            }
        }
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            text = stringResource(R.string.discard_text),
            confirmLabel = stringResource(R.string.action_discard),
            onConfirm = actions.onDiscard,
            onDismiss = { confirmDiscard = false },
            destructive = true,
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.trash_confirm_title),
            text = stringResource(R.string.trash_confirm_text),
            confirmLabel = stringResource(R.string.action_move_to_trash),
            onConfirm = actions.onDelete,
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus, persisted: Boolean) {
    val spec = Motion.tween<Float>(250)
    AnimatedContent(
        targetState = if (status == SaveStatus.SAVED || (status == SaveStatus.IDLE && persisted)) SaveStatus.SAVED else status,
        transitionSpec = { fadeIn(spec) togetherWith fadeOut(spec) },
        label = "save",
    ) { s ->
        when (s) {
            SaveStatus.SAVED -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudDone, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.editor_saved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SaveStatus.PENDING -> Text(stringResource(R.string.editor_saving), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SaveStatus.IDLE -> Spacer(Modifier.size(1.dp))
        }
    }
}

@Composable
private fun MetaLine(state: EditorState, categories: List<Category>) {
    val context = LocalContext.current
    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
    val parts = buildList {
        state.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.name }?.let(::add)
        state.updatedAt?.let { add(context.getString(R.string.meta_edited, DateUtils.formatDateTime(context, it, flags))) }
        state.createdAt?.takeIf { it != state.updatedAt }?.let { add(context.getString(R.string.meta_created, DateUtils.formatDateTime(context, it, flags))) }
        if (state.isChecklist) {
            val done = state.items.count { it.checked }
            add(context.getString(R.string.checklist_progress, done, state.items.count { it.text.isNotBlank() }))
        } else if (state.content.isNotBlank()) {
            val words = state.content.trim().split(Regex("\\s+")).size
            add(context.resources.getQuantityString(R.plurals.word_count, words, words))
        }
    }
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString("  ·  "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onIme: (() -> Unit)? = null,
    textDecoration: TextDecoration? = null,
) {
    val color = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style.copy(color = color, textDecoration = textDecoration),
        singleLine = singleLine,
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onIme?.invoke() }, onDone = { onIme?.invoke() }),
        modifier = modifier.semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        },
    )
}

/** Checklisten-Editor: abhaken, hinzufügen (Enter), löschen, per Griff verschieben. */
@Composable
private fun ChecklistEditor(items: List<ChecklistItem>, hideChecked: Boolean, onItems: (List<ChecklistItem>) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val focusRequesters = remember { mutableStateMapOf<Long, FocusRequester>() }
    var focusId by remember { mutableStateOf<Long?>(null) }
    val heights = remember { mutableStateMapOf<Long, Int>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var current by remember { mutableStateOf(items) }
    current = items

    LaunchedEffect(focusId) {
        val id = focusId ?: return@LaunchedEffect
        runCatching { focusRequesters[id]?.requestFocus() }
        focusId = null
    }

    fun insertAfter(index: Int) {
        val newItem = ChecklistItem(ChecklistCodec.newId(), "", false)
        onItems(current.toMutableList().apply { add(index + 1, newItem) })
        focusId = newItem.id
    }

    val visible = if (hideChecked) items.filterNot { it.checked } else items
    val hiddenCount = items.size - visible.size
    Column {
        visible.forEach { item ->
            androidx.compose.runtime.key(item.id) {
                val index = current.indexOfFirst { it.id == item.id }
                val requester = focusRequesters.getOrPut(item.id) { FocusRequester() }
                val isDragging = draggingId == item.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .clip(MaterialTheme.shapes.small)
                        .background(if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .onSizeChanged { heights[item.id] = it.height },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DragIndicator,
                        contentDescription = stringResource(R.string.a11y_reorder_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp)
                            .pointerInput(item.id) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        draggingId = item.id; dragOffset = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { draggingId = null; dragOffset = 0f },
                                    onDragCancel = { draggingId = null; dragOffset = 0f },
                                    onVerticalDrag = { change, dy ->
                                        change.consume()
                                        dragOffset += dy
                                        val list = current
                                        val i = list.indexOfFirst { it.id == item.id }
                                        if (dragOffset > 0 && i < list.lastIndex) {
                                            val h = heights[list[i + 1].id] ?: return@detectVerticalDragGestures
                                            if (dragOffset > h / 2f) {
                                                onItems(list.toMutableList().apply { add(i + 1, removeAt(i)) })
                                                dragOffset -= h
                                            }
                                        } else if (dragOffset < 0 && i > 0) {
                                            val h = heights[list[i - 1].id] ?: return@detectVerticalDragGestures
                                            if (-dragOffset > h / 2f) {
                                                onItems(list.toMutableList().apply { add(i - 1, removeAt(i)) })
                                                dragOffset += h
                                            }
                                        }
                                    },
                                )
                            },
                    )
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { checked ->
                            onItems(current.map { if (it.id == item.id) it.copy(checked = checked) else it })
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LocalAppExtras.current.accent,
                            checkmarkColor = LocalAppExtras.current.onAccent,
                        ),
                    )
                    PlainField(
                        value = item.text,
                        onChange = { text ->
                            // Einfügen mehrerer Zeilen → mehrere Einträge
                            if (text.contains('\n')) {
                                val lines = text.split('\n')
                                val list = current.toMutableList()
                                val i = list.indexOfFirst { it.id == item.id }
                                list[i] = item.copy(text = lines.first())
                                val extra = lines.drop(1).map { ChecklistItem(ChecklistCodec.newId(), it, false) }
                                list.addAll(i + 1, extra)
                                onItems(list)
                                extra.lastOrNull()?.let { focusId = it.id }
                            } else {
                                onItems(current.map { if (it.id == item.id) it.copy(text = text) else it })
                            }
                        },
                        placeholder = stringResource(R.string.checklist_item_hint),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.checked) 0.5f else 1f)
                        ),
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                        singleLine = true,
                        imeAction = ImeAction.Next,
                        onIme = { insertAfter(index) },
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp).focusRequester(requester),
                    )
                    IconButton(onClick = { onItems(current.filterNot { it.id == item.id }) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.a11y_remove_item), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { insertAfter(current.lastIndex) }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(LocalAppExtras.current.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = LocalAppExtras.current.onAccent)
            }
            Spacer(Modifier.width(16.dp))
            Text(stringResource(R.string.checklist_add_item), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hiddenCount > 0) {
            Text(
                stringResource(R.string.checklist_hidden_done, hiddenCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}
