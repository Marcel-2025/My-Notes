package dev.flondy.mynotes.ui.premium

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.R
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.ui.theme.BrandYellow
import dev.flondy.mynotes.ui.theme.LocalAppExtras

private val PremiumBg = Color(0xFF2A1710)
private val PremiumSurface = Color(0xFF3A241A)

/**
 * Premium-Seite für den bestehenden Einmalkauf „remove_ads“.
 * Preis kommt ausschließlich aus Google Play (ProductDetails) – keine erfundenen Preise.
 */
@Composable
fun PremiumScreen(
    state: BillingManager.State,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    // Beim Öffnen Verbindung/Preis bei Bedarf neu anfragen
    androidx.compose.runtime.LaunchedEffect(Unit) { onRefresh() }
    val animate = LocalAppExtras.current.animationsEnabled
    val transition = rememberInfiniteTransition(label = "premium")
    val glow by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "glow",
    )
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "spin",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF5A3426), Color(0xFF3E2219), PremiumBg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, stringResource(R.string.action_close), tint = Color.White) }
            }
            Column(
                Modifier.widthIn(max = 520.dp).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animiertes Premium-Emblem
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
                    Box(
                        Modifier
                            .size(132.dp)
                            .rotate(if (animate) spin else 0f)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(BrandYellow.copy(alpha = if (animate) glow else 0.5f), Color.Transparent, BrandYellow.copy(alpha = 0.3f), Color.Transparent)))
                    )
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFF59D), BrandYellow, Color(0xFFE0B800)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.WorkspacePremium, null, Modifier.size(48.dp), tint = Color(0xFF1A1806))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(if (state.adFree) R.string.premium_thanks_title else R.string.premium_title),
                    style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (state.adFree) R.string.premium_thanks_subtitle else R.string.premium_subtitle),
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))

                // Vorteilskarte
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(PremiumSurface)
                        .border(1.dp, BrandYellow.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Benefit(Icons.Outlined.Block, stringResource(R.string.premium_benefit_no_ads), stringResource(R.string.premium_benefit_no_ads_desc))
                    Benefit(Icons.Outlined.Bolt, stringResource(R.string.premium_benefit_focus), stringResource(R.string.premium_benefit_focus_desc))
                    Benefit(Icons.Outlined.Lock, stringResource(R.string.premium_benefit_forever), stringResource(R.string.premium_benefit_forever_desc))
                    Benefit(Icons.Outlined.Favorite, stringResource(R.string.premium_benefit_support), stringResource(R.string.premium_benefit_support_desc))
                }
                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = when {
                        state.adFree -> 0
                        state.purchasePending -> 1
                        else -> 2
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "premiumCta",
                ) { mode ->
                    when (mode) {
                        0 -> Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BrandYellow.copy(alpha = 0.12f)).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = BrandYellow)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.premium_status_active), color = BrandYellow, style = MaterialTheme.typography.titleMedium)
                        }
                        1 -> Text(
                            stringResource(R.string.premium_pending),
                            color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = onPurchase,
                                enabled = state.connected && state.productAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandYellow, contentColor = Color(0xFF1A1806),
                                    disabledContainerColor = BrandYellow.copy(alpha = 0.3f), disabledContentColor = Color(0xFF1A1806).copy(alpha = 0.6f),
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                            ) {
                                if (state.productQueried && !state.productAvailable) {
                                    Text(stringResource(R.string.premium_unavailable))
                                } else if (!state.connected || !state.productAvailable) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1A1806))
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(R.string.premium_loading_price))
                                } else {
                                    Text(
                                        stringResource(R.string.premium_buy_with_price, state.formattedPrice ?: ""),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.premium_one_time),
                                style = MaterialTheme.typography.labelLarge, color = BrandYellow, textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRestore) { Text(stringResource(R.string.premium_restore), color = Color.White.copy(alpha = 0.8f)) }
                Text(
                    stringResource(R.string.premium_legal),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(BrandYellow.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = BrandYellow, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
        }
    }
}
