package dev.flondy.mynotes.ui.editor

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.flondy.mynotes.Category
import dev.flondy.mynotes.EditorState
import dev.flondy.mynotes.R
import dev.flondy.mynotes.SaveStatus
import dev.flondy.mynotes.data.ChecklistCodec
import dev.flondy.mynotes.data.ChecklistItem
import dev.flondy.mynotes.data.RichText
import dev.flondy.mynotes.ui.components.AppFilterChip
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.ColorPickerRow
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.components.SheetLabel
import dev.flondy.mynotes.ui.components.TextInputDialog
import dev.flondy.mynotes.ui.components.leather
import dev.flondy.mynotes.ui.components.noteContainerColor
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.Motion

data class EditorActions(
    val onTitle: (String) -> Unit,
    val onContent: (String) -> Unit,
    val onItems: (List<ChecklistItem>) -> Unit,
    val onTogglePin: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onToggleArchive: () -> Unit,
    val onColor: (Int) -> Unit,
    val onCategory: (Int?) -> Unit,
    val onToggleChecklist: () -> Unit,
    val onTextToChecklist: () -> Unit,
    val onShare: () -> Unit,
    val onExportTxt: () -> Unit,
    val onDuplicate: () -> Unit,
    val onClose: () -> Unit,
    val onDiscard: () -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorState,
    categories: List<Category>,
    hideCheckedItems: Boolean,
    actions: EditorActions,
) {
    BackHandler(onBack = actions.onClose)
    val extras = LocalAppExtras.current
    val background by animateColorAsState(
        if (state.color == 0) MaterialTheme.colorScheme.background else noteContainerColor(state.color), Motion.tween(300), label = "editorBg",
    )
    var showStyleSheet by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) { if (state.focusTitle) runCatching { titleFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().leather(stitch = false)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                EditorTopBar(
                    state = state,
                    onBack = actions.onClose,
                    actions = actions,
                    onStyle = { showStyleSheet = true },
                    onDiscard = { confirmDiscard = true },
                )
            },
            bottomBar = {
                AnimatedVisibility(!imeVisible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    EditorBottomBar(
                        onDelete = { confirmDelete = true },
                        onDuplicate = actions.onDuplicate,
                        onSave = actions.onClose,
                        canDuplicate = !state.isBlank,
                    )
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(background)
                    .padding(bottom = padding.calculateBottomPadding())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                Column(
                    Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // ---- Titel & Kategorie
                    PaperSection {
                        FieldLabel(stringResource(R.string.editor_title_label))
                        TitleField(state.title, actions.onTitle, titleFocus) { runCatching { contentFocus.requestFocus() } }
                        Spacer(Modifier.height(14.dp))
                        FieldLabel(stringResource(R.string.label_category))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Gewählte Kategorie zuerst, damit sie ohne Scrollen sichtbar ist
                            val ordered = remember(categories, state.noteId) {
                                categories.sortedByDescending { it.id == state.categoryId }
                            }
                            ordered.forEach { c ->
                                AppFilterChip(state.categoryId == c.id, c.name, CategoryIcons.forCategory(c).icon) {
                                    actions.onCategory(if (state.categoryId == c.id) null else c.id)
                                }
                            }
                        }
                    }

                    // ---- Checkliste
                    val inSpec = Motion.tween<Float>(200)
                    val outSpec = Motion.tween<Float>(150)
                    AnimatedContent(
                        targetState = state.showChecklist,
                        transitionSpec = { fadeIn(inSpec) togetherWith fadeOut(outSpec) },
                        label = "checklistSection",
                    ) { show ->
                        if (show) {
                            PaperSection(padding = 8.dp) {
                                ChecklistEditor(state.items, hideCheckedItems, actions.onItems)
                            }
                        } else {
                            TextButton(onClick = actions.onToggleChecklist, modifier = Modifier.testTag("add_checklist")) {
                                Icon(Icons.Outlined.Checklist, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.editor_add_checklist), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // ---- Notiztext mit Formatierung
                    FieldLabel(stringResource(R.string.editor_text_label), Modifier.padding(start = 6.dp))
                    PaperSection(padding = 0.dp) {
                        RichTextEditor(state.content, actions.onContent, contentFocus)
                    }
                    MetaLine(state)
                    Spacer(Modifier.height(24.dp))
                }
                }
            }
        }
    }

    if (showStyleSheet) {
        StyleSheet(state, categories, actions) { showStyleSheet = false }
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
private fun EditorTopBar(
    state: EditorState,
    onBack: () -> Unit,
    actions: EditorActions,
    onStyle: () -> Unit,
    onDiscard: () -> Unit,
) {
    val extras = LocalAppExtras.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back_save), tint = extras.onLeather)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (state.noteId == null) R.string.editor_new_title else R.string.editor_edit_title),
                style = MaterialTheme.typography.titleLarge, color = extras.onLeather,
            )
            SaveIndicator(state.saveStatus, state.noteId != null)
        }
        IconButton(onClick = actions.onToggleFavorite) {
            Icon(
                if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                stringResource(if (state.isFavorite) R.string.action_unfavorite else R.string.action_favorite),
                tint = if (state.isFavorite) extras.accent else extras.onLeather,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.a11y_more), tint = extras.onLeather) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                @Composable
                fun entry(icon: ImageVector, label: Int, enabled: Boolean = true, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) =
                    DropdownMenuItem(
                        text = { Text(stringResource(label), color = color) },
                        leadingIcon = { Icon(icon, null, tint = color) },
                        enabled = enabled,
                        onClick = { menuOpen = false; onClick() },
                    )
                entry(if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, if (state.isPinned) R.string.action_unpin else R.string.action_pin, onClick = actions.onTogglePin)
                entry(if (state.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (state.isArchived) R.string.action_unarchive else R.string.action_archive, onClick = actions.onToggleArchive)
                entry(Icons.Outlined.Palette, R.string.label_color_category, onClick = onStyle)
                if (state.showChecklist) entry(Icons.Outlined.RemoveDone, R.string.editor_remove_checklist, onClick = actions.onToggleChecklist)
                else if (state.content.isNotBlank()) entry(Icons.Outlined.Checklist, R.string.action_to_checklist, onClick = actions.onTextToChecklist)
                entry(Icons.Outlined.Share, R.string.action_share, enabled = !state.isBlank, onClick = actions.onShare)
                entry(Icons.AutoMirrored.Outlined.InsertDriveFile, R.string.action_export_txt, enabled = !state.isBlank, onClick = actions.onExportTxt)
                entry(Icons.AutoMirrored.Outlined.Undo, R.string.action_discard, onClick = onDiscard)
            }
        }
    }
}

@Composable
private fun EditorBottomBar(onDelete: () -> Unit, onDuplicate: () -> Unit, onSave: () -> Unit, canDuplicate: Boolean) {
    val extras = LocalAppExtras.current
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomAction(Icons.Outlined.Delete, stringResource(R.string.action_delete), onDelete, Modifier.testTag("editor_delete"))
            BottomAction(Icons.Outlined.ContentCopy, stringResource(R.string.action_duplicate), onDuplicate, enabled = canDuplicate)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier.heightIn(min = 52.dp).testTag("editor_save"),
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BottomAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val c = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.35f)
        Icon(icon, null, tint = c)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c)
    }
}

@Composable
private fun PaperSection(padding: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable () -> Unit) {
    val extras = LocalAppExtras.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = extras.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(bottom = 6.dp))
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, focus: FocusRequester, onNext: () -> Unit) {
    val extras = LocalAppExtras.current
    val color = MaterialTheme.colorScheme.onSurface
    val hint = stringResource(R.string.editor_title_hint)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extras.sunken.copy(alpha = if (extras.isDark) 1f else 0.6f))
            .border(1.dp, extras.cardBorder, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.replace('\n', ' ')) },
            textStyle = MaterialTheme.typography.titleLarge.copy(color = color),
            singleLine = true,
            cursorBrush = SolidColor(extras.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier.weight(1f).padding(vertical = 14.dp).focusRequester(focus).testTag("editor_title").semantics { contentDescription = hint },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, stringResource(R.string.editor_clear_title), Modifier.size(14.dp))
                }
            }
        }
    }
}

/** Zeigt Formatierung direkt beim Tippen: Inhalt fett/kursiv/…, Steuerzeichen blass. */
private class RichTextTransformation(private val markerColor: Color, private val linkColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val spans = RichText.parse(text.text)
        if (spans.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val b = AnnotatedString.Builder(text)
        spans.forEach { s ->
            val style = when (s.style) {
                RichText.Style.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                RichText.Style.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                RichText.Style.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                RichText.Style.LINK -> SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            }
            b.addStyle(style, s.contentStart, s.contentEnd)
            b.addStyle(SpanStyle(color = markerColor), s.start, s.contentStart)
            b.addStyle(SpanStyle(color = markerColor), s.contentEnd, s.end)
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
private fun RichTextEditor(content: String, onChange: (String) -> Unit, focus: FocusRequester) {
    val extras = LocalAppExtras.current
    var value by remember { mutableStateOf(TextFieldValue(content, TextRange(content.length))) }
    // Änderungen von außen (z. B. Checkliste → Text) übernehmen
    LaunchedEffect(content) { if (content != value.text) value = TextFieldValue(content, TextRange(content.length)) }
    var askLink by remember { mutableStateOf(false) }

    fun apply(edit: RichText.Edit) {
        value = TextFieldValue(edit.text, TextRange(edit.selectionStart, edit.selectionEnd))
        onChange(edit.text)
        runCatching { focus.requestFocus() }
    }

    val transformation = remember(extras.isDark) {
        RichTextTransformation(markerColor = Color(0x66A8927F), linkColor = Color(0xFF2F6FB3).takeUnless { extras.isDark } ?: Color(0xFF8DC0F0))
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val sel = value.selection
        FormatButton(Icons.Filled.FormatBold, R.string.format_bold, highlighted = true) { apply(RichText.toggleWrap(value.text, sel.start, sel.end, "**")) }
        FormatButton(Icons.Filled.FormatItalic, R.string.format_italic) { apply(RichText.toggleWrap(value.text, sel.start, sel.end, "*")) }
        FormatButton(Icons.Filled.FormatUnderlined, R.string.format_underline) { apply(RichText.toggleWrap(value.text, sel.start, sel.end, "__")) }
        FormatButton(Icons.Filled.FormatListBulleted, R.string.format_bullets) { apply(RichText.toggleList(value.text, sel.start, sel.end, numbered = false)) }
        FormatButton(Icons.Filled.FormatListNumbered, R.string.format_numbers) { apply(RichText.toggleList(value.text, sel.start, sel.end, numbered = true)) }
        FormatButton(Icons.Filled.Link, R.string.format_link) { askLink = true }
    }
    HorizontalDivider(color = extras.cardBorder)
    val hint = stringResource(R.string.editor_content_hint)
    BasicTextField(
        value = value,
        onValueChange = { new ->
            // Aufzählungen beim Zeilenumbruch automatisch fortsetzen
            val cont = if (new.text != value.text) RichText.continueList(value.text, new.text) else null
            value = cont?.let { TextFieldValue(it.text, TextRange(it.selectionStart)) } ?: new
            if (value.text != content) onChange(value.text)
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(extras.accent),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(16.dp)
            .focusRequester(focus)
            .testTag("editor_content")
            .semantics { contentDescription = hint },
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        },
    )
    Text(
        pluralStringResource(R.plurals.char_count, value.text.length, value.text.length),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 10.dp),
    )

    if (askLink) {
        TextInputDialog(
            title = stringResource(R.string.format_link_title),
            initial = "",
            confirmLabel = stringResource(R.string.action_add),
            onConfirm = { url -> apply(RichText.insertLink(value.text, value.selection.start, value.selection.end, url)) },
            onDismiss = { askLink = false },
            maxLength = 300,
        )
    }
}

@Composable
private fun FormatButton(icon: ImageVector, label: Int, highlighted: Boolean = false, onClick: () -> Unit) {
    val extras = LocalAppExtras.current
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) extras.accent.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, stringResource(label), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus, persisted: Boolean) {
    val spec = Motion.tween<Float>(250)
    val extras = LocalAppExtras.current
    AnimatedContent(
        targetState = if (status == SaveStatus.SAVED || (status == SaveStatus.IDLE && persisted)) SaveStatus.SAVED else status,
        transitionSpec = { fadeIn(spec) togetherWith fadeOut(spec) },
        label = "save",
    ) { s ->
        val text = when (s) {
            SaveStatus.SAVED -> stringResource(R.string.editor_saved)
            SaveStatus.PENDING -> stringResource(R.string.editor_saving)
            SaveStatus.IDLE -> ""
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = extras.onLeather.copy(alpha = 0.7f))
    }
}

@Composable
private fun MetaLine(state: EditorState) {
    val context = LocalContext.current
    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
    val parts = buildList {
        state.updatedAt?.let { add(context.getString(R.string.meta_edited, DateUtils.formatDateTime(context, it, flags))) }
        state.createdAt?.takeIf { it != state.updatedAt }?.let { add(context.getString(R.string.meta_created, DateUtils.formatDateTime(context, it, flags))) }
        if (state.showChecklist) {
            add(context.getString(R.string.checklist_progress, state.items.count { it.checked }, state.items.count { it.text.isNotBlank() }))
        }
    }
    if (parts.isNotEmpty()) {
        Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleSheet(state: EditorState, categories: List<Category>, actions: EditorActions, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
            SheetLabel(stringResource(R.string.label_color))
            ColorPickerRow(state.color, actions.onColor)
            SheetLabel(stringResource(R.string.label_category))
            dev.flondy.mynotes.ui.components.CategoryChips(categories, state.categoryId, actions.onCategory)
        }
    }
}

/** Checklisten-Editor: abhaken, hinzufügen (Enter), löschen, per Griff verschieben. */
@Composable
private fun ChecklistEditor(items: List<ChecklistItem>, hideChecked: Boolean, onItems: (List<ChecklistItem>) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val extras = LocalAppExtras.current
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
        onItems(current.toMutableList().apply { add((index + 1).coerceIn(0, size), newItem) })
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
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f; shadowElevation = if (isDragging) 12f else 0f }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDragging) extras.cardHigh else Color.Transparent)
                        .onSizeChanged { heights[item.id] = it.height },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { checked -> onItems(current.map { if (it.id == item.id) it.copy(checked = checked) else it }) },
                        colors = CheckboxDefaults.colors(checkedColor = extras.accent, checkmarkColor = extras.onAccent,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    ItemField(
                        value = item.text,
                        checked = item.checked,
                        onChange = { text ->
                            if (text.contains('\n')) {
                                // Einfügen mehrerer Zeilen → mehrere Einträge
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
                        onNext = { insertAfter(index) },
                        modifier = Modifier.weight(1f).focusRequester(requester),
                    )
                    IconButton(onClick = { onItems(current.filterNot { it.id == item.id }) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.a11y_remove_item), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
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
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { insertAfter(current.lastIndex) }
                .padding(vertical = 12.dp, horizontal = 12.dp)
                .testTag("checklist_add"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.checklist_add_item), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hiddenCount > 0) {
            Text(stringResource(R.string.checklist_hidden_done, hiddenCount), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun ItemField(value: String, checked: Boolean, onChange: (String) -> Unit, onNext: () -> Unit, modifier: Modifier) {
    val hint = stringResource(R.string.checklist_item_hint)
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (checked) 0.5f else 1f)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = color, textDecoration = if (checked) TextDecoration.LineThrough else null),
        singleLine = true,
        cursorBrush = SolidColor(LocalAppExtras.current.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = modifier.padding(vertical = 10.dp).semantics { contentDescription = hint },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        },
    )
}
