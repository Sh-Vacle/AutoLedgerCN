package com.autoledgercn.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autoledgercn.R
import com.autoledgercn.domain.model.ThemeMode
import com.autoledgercn.ui.theme.paletteFor

enum class MascotPage { HOME, RECORDS, STATS, PENDING, SETTINGS }

enum class MascotEmptyType { HOME, RECORDS, STATS, PENDING, SEARCH, DIAGNOSTICS, MISC }

enum class MascotNavItem { HOME, RECORDS, STATS, PENDING, SETTINGS }

object MascotAssetNames {
    val onboardingTop = listOf(
        "mascot_yier_home_happy",
        "mascot_bubu_home_happy"
    )

    val themeCards = listOf(
        "mascot_yier_wallet_excited",
        "mascot_bubu_wallet_excited"
    )

    val pageHeaders = listOf(
        "mascot_yier_saving_love",
        "mascot_bubu_saving_love",
        "mascot_yier_budget_love",
        "mascot_bubu_budget_coin",
        "mascot_yier_growth_excited",
        "mascot_bubu_growth_excited",
        "mascot_yier_ok_thumb",
        "mascot_bubu_ok_thumb",
        "mascot_yier_success_celebrate",
        "mascot_bubu_success_celebrate"
    )

    val emptyStates = listOf(
        "mascot_yier_empty_home_wallet",
        "mascot_bubu_empty_home_wallet",
        "mascot_yier_empty_records_smile",
        "mascot_bubu_empty_records_smile",
        "mascot_yier_empty_stats_smile",
        "mascot_bubu_empty_stats_smile",
        "mascot_yier_empty_pending_clean",
        "mascot_bubu_empty_pending_clean",
        "mascot_yier_ok_thumb",
        "mascot_bubu_ok_thumb",
        "mascot_yier_success_celebrate",
        "mascot_bubu_success_celebrate"
    )

    val navIcons = listOf(
        "ic_nav_home_yier",
        "ic_nav_home_bubu",
        "ic_nav_records_yier",
        "ic_nav_records_bubu",
        "ic_nav_stats_yier",
        "ic_nav_stats_bubu",
        "ic_nav_pending_yier",
        "ic_nav_pending_bubu",
        "ic_nav_settings_yier",
        "ic_nav_settings_bubu"
    )
}

@DrawableRes
fun mascotOnboardingTopRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.BUBU -> R.drawable.mascot_bubu_home_happy
    ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_home_happy
}

@DrawableRes
fun mascotThemeCardRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.BUBU -> R.drawable.mascot_bubu_wallet_excited
    ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_wallet_excited
}

@Composable
fun MascotOnboardingTopImage(mode: ThemeMode, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(mascotOnboardingTopRes(mode)),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MascotThemeCardImage(mode: ThemeMode, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(mascotThemeCardRes(mode)),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MascotHeader(
    title: String,
    subtitle: String?,
    mode: ThemeMode,
    page: MascotPage,
    modifier: Modifier = Modifier,
    imageSize: Dp = page.defaultHeaderImageSize(),
    action: (@Composable () -> Unit)? = null
) {
    val p = paletteFor(mode)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = p.surfaceAlt),
        border = BorderStroke(1.dp, p.secondary.copy(alpha = 0.30f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                }
                if (action != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    action()
                }
            }
            Image(
                painter = painterResource(mascotHeaderRes(mode, page)),
                contentDescription = null,
                modifier = Modifier.size(imageSize),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun MascotEmptyState(
    title: String,
    subtitle: String?,
    mode: ThemeMode,
    type: MascotEmptyType,
    modifier: Modifier = Modifier,
    imageSize: Dp = type.defaultEmptyImageSize(),
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val p = paletteFor(mode)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = p.surface,
        border = BorderStroke(1.dp, p.secondary.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(mascotEmptyRes(mode, type)),
                contentDescription = null,
                modifier = Modifier.size(imageSize),
                contentScale = ContentScale.Fit
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            if (actionText != null && onAction != null) {
                Button(onClick = onAction) { Text(actionText) }
            }
        }
    }
}

@Composable
fun MascotNavIcon(
    item: MascotNavItem,
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val size = if (selected) 28.dp else 24.dp
    Image(
        painter = painterResource(mascotNavRes(mode, item)),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@DrawableRes
fun mascotHeaderRes(mode: ThemeMode, page: MascotPage): Int = when (page) {
    MascotPage.HOME -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_saving_love
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_saving_love
    }
    MascotPage.RECORDS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_budget_coin
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_budget_love
    }
    MascotPage.STATS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_growth_excited
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_growth_excited
    }
    MascotPage.PENDING -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_ok_thumb
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_ok_thumb
    }
    MascotPage.SETTINGS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_success_celebrate
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_success_celebrate
    }
}

@DrawableRes
fun mascotEmptyRes(mode: ThemeMode, type: MascotEmptyType): Int = when (type) {
    MascotEmptyType.HOME -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_empty_home_wallet
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_empty_home_wallet
    }
    MascotEmptyType.RECORDS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_empty_records_smile
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_empty_records_smile
    }
    MascotEmptyType.STATS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_empty_stats_smile
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_empty_stats_smile
    }
    MascotEmptyType.PENDING -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_empty_pending_clean
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_empty_pending_clean
    }
    MascotEmptyType.SEARCH -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_ok_thumb
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_ok_thumb
    }
    MascotEmptyType.DIAGNOSTICS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_success_celebrate
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_success_celebrate
    }
    MascotEmptyType.MISC -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_ok_thumb
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.mascot_yier_ok_thumb
    }
}

@DrawableRes
fun mascotNavRes(mode: ThemeMode, item: MascotNavItem): Int = when (item) {
    MascotNavItem.HOME -> when (mode) {
        ThemeMode.BUBU -> R.drawable.ic_nav_home_bubu
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.ic_nav_home_yier
    }
    MascotNavItem.RECORDS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.mascot_bubu_checklist_happy
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.ic_nav_records_yier
    }
    MascotNavItem.STATS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.ic_nav_stats_bubu
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.ic_nav_stats_yier
    }
    MascotNavItem.PENDING -> when (mode) {
        ThemeMode.BUBU -> R.drawable.ic_nav_pending_bubu
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.ic_nav_pending_yier
    }
    MascotNavItem.SETTINGS -> when (mode) {
        ThemeMode.BUBU -> R.drawable.ic_nav_settings_bubu
        ThemeMode.YIER, ThemeMode.COUPLE -> R.drawable.ic_nav_settings_yier
    }
}

private fun MascotPage.defaultHeaderImageSize(): Dp = when (this) {
    MascotPage.HOME -> 96.dp
    MascotPage.RECORDS -> 76.dp
    MascotPage.STATS -> 88.dp
    MascotPage.PENDING -> 88.dp
    MascotPage.SETTINGS -> 80.dp
}

private fun MascotEmptyType.defaultEmptyImageSize(): Dp = when (this) {
    MascotEmptyType.HOME -> 132.dp
    MascotEmptyType.RECORDS -> 132.dp
    MascotEmptyType.STATS -> 124.dp
    MascotEmptyType.PENDING -> 124.dp
    MascotEmptyType.SEARCH -> 112.dp
    MascotEmptyType.DIAGNOSTICS -> 104.dp
    MascotEmptyType.MISC -> 120.dp
}
