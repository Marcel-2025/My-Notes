package dev.flondy.mynotes.ui.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.R
import dev.flondy.mynotes.data.NoteTemplate
import dev.flondy.mynotes.data.NoteTemplates
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.ui.components.CategoryIcons
import dev.flondy.mynotes.ui.components.IconTile
import dev.flondy.mynotes.ui.components.LeatherPage
import dev.flondy.mynotes.ui.theme.Entitlements
import dev.flondy.mynotes.ui.theme.LocalAppExtras
import dev.flondy.mynotes.ui.theme.PaperPattern
import dev.flondy.mynotes.ui.theme.ShopPreview
import dev.flondy.mynotes.ui.theme.Skin
import dev.flondy.mynotes.ui.theme.SkinPalette
import dev.flondy.mynotes.ui.theme.paperPattern
import kotlinx.coroutines.delay

data class ShopActions(
    val onBack: () -> Unit,
    val onUseSkin: (Skin) -> Unit,
    val onUsePattern: (PaperPattern) -> Unit,
    val onTemplate: (NoteTemplate) -> Unit,
    /** Nur in Test-Builds gesetzt: alle Premium-Inhalte freischalten. */
    val onDebugUnlock: ((Boolean) -> Unit)? = null,
    /** 2.5: Kaufangebote (null = Plattform ohne Käufe). */
    val offers: ShopOffers? = null,
)

/** Kaufangebote im Shop. Preise kommen immer aus dem Store; null = Produkt (noch) nicht angelegt → „Bald verfügbar“. */
data class ShopOffers(
    val designPackPrice: String? = null,
    val plusMonthlyPrice: String? = null,
    val plusYearlyPrice: String? = null,
    val designPack: Boolean = false,
    val plusActive: Boolean = false,
    /** Android: Premium Plus enthält auch „keine Werbung“. */
    val plusIncludesNoAds: Boolean = false,
    /** Windows: nicht aus dem Microsoft Store installiert → keine Käufe möglich. */
    val storeUnavailable: Boolean = false,
    val onBuyPack: () -> Unit = {},
    val onBuyPlus: (yearly: Boolean) -> Unit = {},
    val onRestore: (() -> Unit)? = null,
    val onManageSubs: (() -> Unit)? = null,
)

/**
 * Design-Shop (2.4): Designs, Papiermuster und Vorlagen mit Live-Vorschau.
 * Premium-Inhalte: 30 Sekunden ansehen; „Freischalten“ folgt mit dem Store-Kauf (keine Preise in der App erfunden).
 */
@Composable
fun ShopScreen(settings: AppSettings, actions: ShopActions) {
    val lang = stringResource(R.string.template_lang)
    val templates = remember(lang) { NoteTemplates.forLanguage(lang) }
    val currentSkin = Entitlements.effectiveSkin(settings.skin, settings.unlocked)
    val currentPattern = Entitlements.effectivePattern(settings.paperPattern, settings.unlocked)

    // Vorschau-Zeitgeber
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ShopPreview.active) {
        while (ShopPreview.active) {
            now = System.currentTimeMillis()
            if (ShopPreview.secondsLeft(now) <= 0) ShopPreview.stop()
            delay(250)
        }
    }

    LeatherPage(title = stringResource(R.string.shop_title), onBack = { ShopPreview.stop(); actions.onBack() }) { bottom ->
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(ShopPreview.active, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                PreviewBanner(now)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.fillMaxSize().widthIn(max = 1360.dp).testTag("shop_grid"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { Intro(actions, settings) }
                actions.offers?.let { o -> item(span = { GridItemSpan(maxLineSpan) }) { PremiumOffers(o) } }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.shop_section_skins)) }
                itemsIndexed(Skin.entries, key = { _, s -> "skin_" + s.name }) { i, skin ->
                    val unlocked = Entitlements.skinUnlocked(skin, settings.unlocked)
                    ShopCard(
                        index = i, title = stringResource(skin.title), premium = skin.premium, unlocked = unlocked,
                        active = currentSkin == skin && ShopPreview.skin == null, previewing = ShopPreview.skin == skin,
                        tag = "shop_skin_" + skin.name.lowercase(),
                        onUse = { ShopPreview.stop(); actions.onUseSkin(skin) },
                        onPreview = { ShopPreview.start(skin = skin) },
                        onUnlock = actions.offers?.takeIf { it.designPackPrice != null && !it.storeUnavailable }?.onBuyPack,
                    ) { SkinPreview(skin.p) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.shop_section_paper)) }
                itemsIndexed(PaperPattern.entries, key = { _, p -> "paper_" + p.name }) { i, p ->
                    val unlocked = Entitlements.patternUnlocked(p, settings.unlocked)
                    ShopCard(
                        index = i, title = stringResource(p.title), premium = p.premium, unlocked = unlocked,
                        active = currentPattern == p && ShopPreview.pattern == null, previewing = ShopPreview.pattern == p,
                        tag = "shop_paper_" + p.name.lowercase(),
                        onUse = { ShopPreview.stop(); actions.onUsePattern(p) },
                        onPreview = { ShopPreview.start(pattern = p) },
                        onUnlock = actions.offers?.takeIf { it.designPackPrice != null && !it.storeUnavailable }?.onBuyPack,
                    ) { PaperPreview(p) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.shop_section_templates)) }
                itemsIndexed(templates, key = { _, t -> "tpl_" + t.id }) { i, t -> TemplateCard(t, i) { actions.onTemplate(t) } }
            }
        }
    }
}

@Composable
private fun Intro(actions: ShopActions, settings: AppSettings) {
    val extras = LocalAppExtras.current
    Column {
        Text(stringResource(R.string.shop_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(extras.accent.copy(alpha = if (extras.isDark) 0.16f else 0.28f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = if (extras.isDark) extras.accent else extras.leather)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(if (actions.offers != null) R.string.shop_premium_info_25 else R.string.shop_premium_info), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        if (actions.onDebugUnlock != null) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.debug_unlock_all), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = Entitlements.ALL in settings.unlocked, onCheckedChange = actions.onDebugUnlock)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PreviewBanner(now: Long) {
    val extras = LocalAppExtras.current
    val name = ShopPreview.skin?.let { stringResource(it.title) } ?: ShopPreview.pattern?.let { stringResource(it.title) } ?: ""
    val left = ShopPreview.secondsLeft(now)
    val progress by animateFloatAsState(left / ShopPreview.SECONDS.toFloat(), tween(250, easing = LinearEasing), label = "preview")
    Column(Modifier.fillMaxWidth().background(extras.accent).testTag("shop_preview_banner")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, null, tint = extras.onAccent)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.shop_preview_running, name, left), color = extras.onAccent,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { ShopPreview.stop() }) { Text(stringResource(R.string.shop_preview_end), color = extras.onAccent) }
        }
        Box(Modifier.fillMaxWidth(progress).height(3.dp).background(extras.onAccent.copy(alpha = 0.6f)))
    }
}

/** Karte im Shop: gestaffeltes Einblenden, Premium-Abzeichen mit Glanz, Auswahlrahmen animiert. */
@Composable
private fun ShopCard(
    index: Int, title: String, premium: Boolean, unlocked: Boolean, active: Boolean, previewing: Boolean, tag: String,
    onUse: () -> Unit, onPreview: () -> Unit, onUnlock: (() -> Unit)? = null, preview: @Composable () -> Unit,
) {
    val extras = LocalAppExtras.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!extras.animationsEnabled) { appear.snapTo(1f); return@LaunchedEffect }
        delay(40L * index.coerceAtMost(10)); appear.animateTo(1f, tween(320))
    }
    val ring by animateColorAsState(if (active || previewing) extras.accent else extras.cardBorder, tween(250), label = "ring")
    val ringWidth by animateDpAsState(if (active || previewing) 3.dp else 1.dp, tween(250), label = "ringW")
    Surface(
        shape = RoundedCornerShape(22.dp), color = extras.card, border = BorderStroke(ringWidth, ring),
        shadowElevation = if (active) 8.dp else 2.dp,
        modifier = Modifier.graphicsLayer {
            alpha = appear.value; translationY = (1f - appear.value) * 30f; scaleX = 0.96f + 0.04f * appear.value; scaleY = scaleX
        }.testTag(tag),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                preview()
                Row(Modifier.align(Alignment.BottomStart).padding(10.dp)) { PriceBadge(premium) }
                if (premium && !unlocked) {
                    Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                        .size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(5.dp))
                }
                if (active) Icon(Icons.Filled.CheckCircle, null, tint = extras.accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp).clip(CircleShape).background(Color.White))
            }
            Column(Modifier.padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                when {
                    active -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.shop_in_use)) }
                    unlocked -> Button(onClick = onUse, modifier = Modifier.fillMaxWidth().testTag(tag + "_use"),
                        colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent)) { Text(stringResource(R.string.shop_use)) }
                    else -> {
                        Button(onClick = onPreview, modifier = Modifier.fillMaxWidth().testTag(tag + "_preview"),
                            colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent)) {
                            Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.shop_preview))
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { onUnlock?.invoke() }, enabled = onUnlock != null, modifier = Modifier.fillMaxWidth().testTag(tag + "_unlock")) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (onUnlock != null) R.string.shop_unlock else R.string.shop_unlock_soon), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(stringResource(R.string.shop_locked_hint), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

/** Kostenlos / Premium – Premium mit wanderndem Glanz. */
@Composable
private fun PriceBadge(premium: Boolean) {
    val extras = LocalAppExtras.current
    val shine = if (premium && extras.animationsEnabled) {
        val t = rememberInfiniteTransition(label = "shine")
        t.animateFloat(-1f, 2f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "shine").value
    } else -1f
    val base = if (premium) Color(0xFFF6C445) else Color(0xFF6DAA2C)
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(base)
            .background(Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                start = Offset(shine * 160f - 60f, 0f), end = Offset(shine * 160f + 20f, 60f),
            ))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(stringResource(if (premium) R.string.shop_premium else R.string.shop_free), style = MaterialTheme.typography.labelMedium,
            color = if (premium) Color(0xFF3E2219) else Color.White)
    }
}

/** Mini-Vorschau eines Designs: Leder-Kopf, Papier, zwei Karten, Akzent-Knopf. */
@Composable
fun SkinPreview(k: SkinPalette, dark: Boolean = LocalAppExtras.current.isDark) {
    val bg = if (dark) k.nightBg else k.paper
    val card = if (dark) k.nightCard else k.card
    val ink = if (dark) k.nightText else k.ink
    val soft = if (dark) k.nightTextSoft else k.inkSoft
    Canvas(Modifier.fillMaxSize().background(bg)) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(if (dark) k.nightLeather else k.leather, k.leatherDark)), size = androidx.compose.ui.geometry.Size(w, h * 0.32f))
        // Ziernaht
        val y = h * 0.32f - 6f
        var x = 10f
        while (x < w - 10f) { drawLine(k.accent.copy(alpha = 0.7f), Offset(x, y), Offset(x + 7f, y), 2f); x += 13f }
        drawRoundRect(k.onLeather.copy(alpha = 0.9f), Offset(16f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.4f, 12f), androidx.compose.ui.geometry.CornerRadius(6f))
        val cw = (w - 48f) / 2
        for (i in 0..1) {
            val cx = 16f + i * (cw + 16f); val cy = h * 0.42f
            drawRoundRect(card, Offset(cx, cy), androidx.compose.ui.geometry.Size(cw, h * 0.48f), androidx.compose.ui.geometry.CornerRadius(14f))
            drawRoundRect(ink.copy(alpha = 0.85f), Offset(cx + 12f, cy + 14f), androidx.compose.ui.geometry.Size(cw * 0.55f, 9f), androidx.compose.ui.geometry.CornerRadius(4f))
            for (l in 0..2) drawRoundRect(soft.copy(alpha = 0.5f), Offset(cx + 12f, cy + 34f + l * 14f), androidx.compose.ui.geometry.Size(cw * (0.75f - l * 0.15f), 6f), androidx.compose.ui.geometry.CornerRadius(3f))
        }
        drawCircle(k.accent, 18f, Offset(w - 30f, h - 26f))
        drawCircle(k.onAccent, 4f, Offset(w - 30f, h - 26f))
    }
}

/** Mini-Vorschau eines Papiermusters. */
@Composable
fun PaperPreview(p: PaperPattern) {
    val extras = LocalAppExtras.current
    Box(Modifier.fillMaxSize().background(extras.card).paperPattern(p, extras.cardBorder, extras.accent))
}

@Composable
private fun TemplateCard(t: NoteTemplate, index: Int, onUse: () -> Unit) {
    val extras = LocalAppExtras.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!extras.animationsEnabled) { appear.snapTo(1f); return@LaunchedEffect }
        delay(40L * index); appear.animateTo(1f, tween(320))
    }
    Surface(
        shape = RoundedCornerShape(22.dp), color = extras.card, border = BorderStroke(1.dp, extras.cardBorder), shadowElevation = 2.dp,
        modifier = Modifier.graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 30f }.testTag("shop_tpl_" + t.id),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(CategoryIcons.style(t.icon) ?: CategoryIcons.all.first().second, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                PriceBadge(false)
            }
            Spacer(Modifier.height(10.dp))
            val preview = if (t.isChecklist) t.items.take(4).joinToString("\n") { "☐ $it" }
            else t.text.replace("**", "").lines().filter { it.isNotBlank() }.take(4).joinToString("\n")
            Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4,
                modifier = Modifier.height(72.dp))
            Spacer(Modifier.height(8.dp))
            Button(onClick = onUse, modifier = Modifier.fillMaxWidth().testTag("shop_tpl_use_" + t.id),
                colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent)) {
                Text(stringResource(R.string.template_use))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
/** Premium-Bereich: Design-Paket (Einmalkauf) und Premium Plus (Abo, monatlich/jährlich). */
@Composable
private fun PremiumOffers(o: ShopOffers) {
    val extras = LocalAppExtras.current
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.shop_section_premium))
        Spacer(Modifier.height(10.dp))
        if (o.storeUnavailable) {
            Text(stringResource(R.string.shop_store_unavailable), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
        }
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OfferCard(
                title = stringResource(R.string.shop_pack_title), desc = stringResource(R.string.shop_pack_desc),
                tag = "offer_pack", highlight = false, owned = o.designPack, ownedLabel = stringResource(R.string.shop_owned),
            ) {
                val price = o.designPackPrice
                Button(onClick = o.onBuyPack, enabled = price != null && !o.storeUnavailable, modifier = Modifier.fillMaxWidth().testTag("offer_pack_buy"),
                    colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent)) {
                    Text(if (price != null) stringResource(R.string.shop_buy_once, price) else stringResource(R.string.shop_not_available))
                }
            }
            OfferCard(
                title = stringResource(R.string.shop_plus_title),
                desc = stringResource(if (o.plusIncludesNoAds) R.string.shop_plus_desc_ads else R.string.shop_plus_desc),
                tag = "offer_plus", highlight = true, owned = o.plusActive, ownedLabel = stringResource(R.string.shop_plus_active),
            ) {
                val m = o.plusMonthlyPrice; val y = o.plusYearlyPrice
                Button(onClick = { o.onBuyPlus(false) }, enabled = m != null && !o.storeUnavailable, modifier = Modifier.fillMaxWidth().testTag("offer_plus_monthly"),
                    colors = ButtonDefaults.buttonColors(containerColor = extras.accent, contentColor = extras.onAccent)) {
                    Text(if (m != null) stringResource(R.string.shop_plus_monthly, m) else stringResource(R.string.shop_not_available))
                }
                if (y != null) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { o.onBuyPlus(true) }, enabled = !o.storeUnavailable, modifier = Modifier.fillMaxWidth().testTag("offer_plus_yearly")) {
                        Text(stringResource(R.string.shop_plus_yearly, y))
                    }
                }
                Text(stringResource(R.string.shop_sub_terms), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(Modifier.padding(top = 6.dp)) {
            o.onRestore?.let { TextButton(onClick = it, modifier = Modifier.testTag("offer_restore")) { Text(stringResource(R.string.shop_restore)) } }
            if (o.plusActive) o.onManageSubs?.let { TextButton(onClick = it) { Text(stringResource(R.string.shop_manage_subs)) } }
        }
    }
}

@Composable
private fun OfferCard(
    title: String, desc: String, tag: String, highlight: Boolean, owned: Boolean, ownedLabel: String,
    buttons: @Composable () -> Unit,
) {
    val extras = LocalAppExtras.current
    Surface(
        shape = RoundedCornerShape(22.dp), color = extras.card,
        border = BorderStroke(if (highlight) 2.dp else 1.dp, if (highlight) extras.accent else extras.cardBorder),
        shadowElevation = if (highlight) 6.dp else 2.dp,
        modifier = Modifier.widthIn(min = 260.dp, max = 420.dp).testTag(tag),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WorkspacePremium, null, tint = if (extras.isDark) extras.accent else extras.leather)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                PriceBadge(true)
            }
            Spacer(Modifier.height(6.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (owned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = extras.leaf)
                    Spacer(Modifier.width(6.dp))
                    Text(ownedLabel, style = MaterialTheme.typography.labelLarge)
                }
            } else buttons()
        }
    }
}
