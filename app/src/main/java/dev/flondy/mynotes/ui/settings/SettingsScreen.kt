package dev.flondy.mynotes.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import dev.flondy.mynotes.BuildConfig
import dev.flondy.mynotes.R
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.icons.AppIcon
import dev.flondy.mynotes.icons.AppIconManager
import dev.flondy.mynotes.settings.AccentColor
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.settings.CardDensity
import dev.flondy.mynotes.settings.SortMode
import dev.flondy.mynotes.settings.ThemeMode
import dev.flondy.mynotes.ui.components.ConfirmDialog
import dev.flondy.mynotes.ui.home.sortLabel
import dev.flondy.mynotes.ui.theme.LocalAppExtras

data class SettingsActions(
    val onBack: () -> Unit,
    val onOpenShop: () -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit,
    val onAccent: (AccentColor) -> Unit,
    val onCardDensity: (CardDensity) -> Unit,
    val onAnimations: (Boolean) -> Unit,
    val onImmersive: (Boolean) -> Unit,
    val onAppIcon: (AppIcon) -> Unit,
    val onAdFreeBadge: (Boolean) -> Unit,
    val onSortMode: (SortMode) -> Unit,
    val onHideChecked: (Boolean) -> Unit,
    val onOpenTrash: () -> Unit,
    val onOpenCategories: () -> Unit,
    val onExportBackup: () -> Unit,
    val onImportBackup: () -> Unit,
    val onOpenPremium: () -> Unit,
    val onRestorePurchases: () -> Unit,
    val onPrivacyPolicy: () -> Unit,
    val onPrivacyOptions: () -> Unit,
    val onDeleteAllNotes: () -> Unit,
    val onRate: () -> Unit,
    val onFeedback: () -> Unit,
    val onLandingPage: () -> Unit,
    val onDebugAdFree: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    premium: BillingManager.State,
    privacyOptionsRequired: Boolean,
    trashCount: Int,
    actions: SettingsActions,
) {
    BackHandler(onBack = actions.onBack)
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLanguage = remember { dev.flondy.mynotes.settings.AppLanguage.current(context) }

    dev.flondy.mynotes.ui.components.LeatherPage(title = stringResource(R.string.settings_title), onBack = actions.onBack) { _ ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = dev.flondy.mynotes.ui.components.rememberWindowLayout().readingMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 32.dp),
            ) {
                // ---------------- Premium
                PremiumCard(premium, actions.onOpenPremium)

                // ---------------- Darstellung
                SectionTitle(stringResource(R.string.section_appearance))
                SettingsGroup {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            val modes = ThemeMode.entries
                            modes.forEachIndexed { i, mode ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { actions.onThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                                ) {
                                    Text(stringResource(when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }))
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(stringResource(R.string.setting_accent), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AccentColor.entries.forEach { accent ->
                                val selected = settings.accent == accent
                                val name = stringResource(when (accent) {
                                    AccentColor.YELLOW -> R.string.accent_yellow
                                    AccentColor.MINT -> R.string.accent_mint
                                    AccentColor.LAVENDER -> R.string.accent_lavender
                                    AccentColor.CORAL -> R.string.accent_coral
                                })
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(accent.argb))
                                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        .clickable(role = Role.RadioButton) { actions.onAccent(accent) }
                                        .semantics { contentDescription = name; this.selected = selected },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, null, tint = Color(0xFF1A1806))
                                }
                            }
                        }
                    }
                    Divider()
                    NavItem(Icons.Outlined.Palette, stringResource(R.string.settings_design), stringResource(R.string.settings_design_desc), actions.onOpenShop)
                    SwitchItem(Icons.Outlined.ViewAgenda, stringResource(R.string.setting_compact), stringResource(R.string.setting_compact_desc),
                        settings.cardDensity == CardDensity.COMPACT) { actions.onCardDensity(if (it) CardDensity.COMPACT else CardDensity.COMFORTABLE) }
                    SwitchItem(Icons.Outlined.Animation, stringResource(R.string.setting_animations), stringResource(R.string.setting_animations_desc),
                        settings.animationsEnabled, actions.onAnimations)
                    SwitchItem(Icons.Outlined.Fullscreen, stringResource(R.string.setting_immersive), stringResource(R.string.setting_immersive_desc),
                        settings.immersiveMode, actions.onImmersive)
                    if (premium.adFree) {
                        SwitchItem(Icons.Outlined.WorkspacePremium, stringResource(R.string.setting_ad_free_badge), stringResource(R.string.setting_ad_free_badge_desc),
                            settings.showAdFreeBadge, actions.onAdFreeBadge)
                    }
                }

                if (AppIconManager.isSupported) {
                    SectionTitle(stringResource(R.string.setting_app_icon))
                    SettingsGroup {
                        AppIconPicker(AppIcon.fromName(settings.appIcon), actions.onAppIcon)
                        Text(
                            stringResource(R.string.setting_app_icon_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        )
                    }
                }

                // ---------------- Sprache
                SectionTitle(stringResource(R.string.setting_language))
                SettingsGroup {
                    NavItem(
                        Icons.Outlined.Language,
                        stringResource(R.string.setting_language),
                        if (currentLanguage.isEmpty()) stringResource(R.string.language_system)
                        else dev.flondy.mynotes.settings.AppLanguage.nativeName(currentLanguage),
                        onClick = { showLanguageDialog = true },
                    )
                }

                // ---------------- Notizen
                SectionTitle(stringResource(R.string.section_notes))
                SettingsGroup {
                    NavItem(Icons.Outlined.Sort, stringResource(R.string.setting_default_sort), stringResource(sortLabel(settings.sortMode)), onClick = { showSortDialog = true })
                    SwitchItem(Icons.Outlined.CheckBox, stringResource(R.string.setting_hide_checked), stringResource(R.string.setting_hide_checked_desc),
                        settings.hideCheckedItems, actions.onHideChecked)
                    NavItem(Icons.Outlined.Category, stringResource(R.string.categories_title), null, actions.onOpenCategories)
                    NavItem(Icons.Outlined.Delete, stringResource(R.string.trash_title),
                        stringResource(R.string.trash_summary, trashCount, settings.trashRetentionDays), actions.onOpenTrash)
                }

                SectionTitle(stringResource(R.string.section_backup))
                SettingsGroup {
                    NavItem(Icons.Outlined.Upload, stringResource(R.string.backup_export), stringResource(R.string.backup_export_desc), actions.onExportBackup)
                    NavItem(Icons.Outlined.Download, stringResource(R.string.backup_import), stringResource(R.string.backup_import_desc), actions.onImportBackup)
                }

                // ---------------- Premium
                SectionTitle(stringResource(R.string.section_premium))
                SettingsGroup {
                    NavItem(
                        Icons.Outlined.WorkspacePremium,
                        stringResource(if (premium.adFree) R.string.premium_status_active else R.string.premium_unlock),
                        stringResource(if (premium.adFree) R.string.premium_status_active_desc else R.string.premium_one_time_short),
                        actions.onOpenPremium,
                    )
                    NavItem(Icons.Outlined.Restore, stringResource(R.string.premium_restore), stringResource(R.string.premium_restore_desc), actions.onRestorePurchases)
                }

                // ---------------- Datenschutz
                SectionTitle(stringResource(R.string.section_privacy))
                SettingsGroup {
                    NavItem(Icons.Outlined.Policy, stringResource(R.string.privacy_policy), null, actions.onPrivacyPolicy, external = true)
                    if (privacyOptionsRequired && !premium.adFree) {
                        NavItem(Icons.Outlined.PrivacyTip, stringResource(R.string.privacy_consent), stringResource(R.string.privacy_consent_desc), actions.onPrivacyOptions)
                    }
                    NavItem(Icons.Outlined.DeleteSweep, stringResource(R.string.delete_all_notes), stringResource(R.string.delete_all_notes_desc), tint = MaterialTheme.colorScheme.error,
                        onClick = { confirmDeleteAll = true })
                }

                // ---------------- Über
                SectionTitle(stringResource(R.string.section_about))
                SettingsGroup {
                    NavItem(Icons.Outlined.Info, stringResource(R.string.about_version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", null)
                    NavItem(Icons.Outlined.Shop, stringResource(R.string.about_rate), stringResource(R.string.about_rate_desc), actions.onRate, external = true)
                    NavItem(Icons.Outlined.Email, stringResource(R.string.about_feedback), stringResource(R.string.about_feedback_desc), actions.onFeedback, external = true)
                    NavItem(Icons.Outlined.Language, stringResource(R.string.about_website), null, actions.onLandingPage, external = true)
                    NavItem(Icons.Outlined.Description, stringResource(R.string.about_licenses), null, { showLicenses = true })
                    Text(
                        stringResource(R.string.about_developer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                if (BuildConfig.DEBUG) {
                    SectionTitle("Debug")
                    SettingsGroup {
                        SwitchItem(Icons.Outlined.BugReport, "Werbefrei simulieren", "Nur in Debug-Builds sichtbar – im Release-Build entfernt.",
                            settings.debugForceAdFree, actions.onDebugAdFree)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        val activity = context as? android.app.Activity
        val options = listOf("") + dev.flondy.mynotes.settings.AppLanguage.supported
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.setting_language)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { tag ->
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.RadioButton) {
                                showLanguageDialog = false
                                if (activity != null) dev.flondy.mynotes.settings.AppLanguage.set(activity, tag)
                            }.testTag("language_" + tag.ifEmpty { "system" }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = currentLanguage == tag, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(
                                if (tag.isEmpty()) stringResource(R.string.language_system)
                                else dev.flondy.mynotes.settings.AppLanguage.nativeName(tag),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.setting_default_sort)) },
            text = {
                Column {
                    SortMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { actions.onSortMode(mode); showSortDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.sortMode == mode, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(stringResource(sortLabel(mode)))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSortDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.about_licenses)) },
            text = { Text(stringResource(R.string.licenses_text), style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (confirmDeleteAll) {
        ConfirmDialog(
            title = stringResource(R.string.delete_all_notes),
            text = stringResource(R.string.delete_all_confirm),
            confirmLabel = stringResource(R.string.action_move_to_trash),
            onConfirm = actions.onDeleteAllNotes,
            onDismiss = { confirmDeleteAll = false },
            destructive = true,
        )
    }
}

@Composable
private fun PremiumCard(premium: BillingManager.State, onClick: () -> Unit) {
    val gold = LocalAppExtras.current.premiumGold
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = LocalAppExtras.current.leather,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(gold.copy(alpha = 0.22f), Color.Transparent)))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(gold), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = Color(0xFF1A1806))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (premium.adFree) R.string.premium_status_active else R.string.premium_card_title),
                    style = MaterialTheme.typography.titleMedium, color = Color.White,
                )
                Text(
                    stringResource(if (premium.adFree) R.string.premium_status_active_desc else R.string.premium_card_subtitle),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AppIconPicker(selected: AppIcon, onSelect: (AppIcon) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon.entries.forEach { icon ->
            val isSel = icon == selected
            val label = stringResource(icon.labelRes)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) { onSelect(icon) }
                    .semantics { contentDescription = label; this.selected = isSel }
                    .padding(6.dp),
            ) {
                val bitmap = remember(icon) {
                    ContextCompat.getDrawable(context, icon.previewRes)?.toBitmap(192, 192)?.asImageBitmap()
                }
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(if (isSel) 3.dp else 1.dp, if (isSel) LocalAppExtras.current.accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                ) {
                    if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = LocalAppExtras.current.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalAppExtras.current.cardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun NavItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    external: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (tint == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else tint)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (external) Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SwitchItem(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}
