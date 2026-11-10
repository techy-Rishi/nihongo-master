package com.nihongo.master.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================================
// ADVANCED COLOR THEORY PALETTE (60-30-10 RULE + SEMANTIC GRAMMAR TOKENS)
// =========================================================================
object DuoColors {
    // 10% Action & Semantic Grammar Colors (High-Contrast Visual Anchors)
    val FeatherGreen = Color(0xFF58CC02)       // Verbs / Action / Correct / Primary CTA
    val FeatherGreenDark = Color(0xFF46A302)   // 3D Bottom Lip for Green
    val FeatherGreenBg = Color(0xFFD7FFB8)

    val MacawBlue = Color(0xFF1CB0F6)          // Subject / Topic / Audio / Info
    val MacawBlueDark = Color(0xFF1899D6)      // 3D Bottom Lip for Blue
    val MacawBlueBg = Color(0xFFDDF4FF)

    val LilacPurple = Color(0xFFCE82FF)        // Particles (は・が・を・に・で) / Grammar
    val LilacPurpleDark = Color(0xFFA568CC)    // 3D Bottom Lip for Purple
    val LilacPurpleBg = Color(0xFFF3E5FF)

    val FoxOrange = Color(0xFFFF9600)          // Object / Time / Place / Streak Fire
    val FoxOrangeDark = Color(0xFFCC7800)      // 3D Bottom Lip for Orange
    val FoxOrangeBg = Color(0xFFFFF0D5)

    val CardinalRed = Color(0xFFFF4B4B)        // Mistakes / Common Error / Mic Recording
    val CardinalRedDark = Color(0xFFEA2B2B)    // 3D Bottom Lip for Red
    val CardinalRedBg = Color(0xFFFFDFE0)

    val BeeGold = Color(0xFFFFC800)            // XP / Crowns / Mastery / Star
    val BeeGoldDark = Color(0xFFD9A600)
    val BeeGoldBg = Color(0xFFFFF9DB)

    // 60% Dominant Neutral Canvas & 30% Structural Card Surfaces
    val LightCanvas = Color(0xFFF7F9FA)
    val LightCard = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE5E5E5)
    val LightLip = Color(0xFFD4D4D4)

    val DarkCanvas = Color(0xFF131F24)         // Duolingo Signature Night Slate
    val DarkCard = Color(0xFF1B2B32)
    val DarkBorder = Color(0xFF2B414B)
    val DarkLip = Color(0xFF0E171B)
}

private val LightColors = lightColorScheme(
    primary = DuoColors.FeatherGreen,
    onPrimary = Color.White,
    primaryContainer = DuoColors.FeatherGreenBg,
    onPrimaryContainer = Color(0xFF1F4D00),
    secondary = DuoColors.MacawBlue,
    onSecondary = Color.White,
    secondaryContainer = DuoColors.MacawBlueBg,
    onSecondaryContainer = Color(0xFF003A59),
    tertiary = DuoColors.LilacPurple,
    onTertiary = Color.White,
    tertiaryContainer = DuoColors.LilacPurpleBg,
    onTertiaryContainer = Color(0xFF3E1063),
    background = DuoColors.LightCanvas,
    onBackground = Color(0xFF1E293B),
    surface = DuoColors.LightCard,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF64748B)
)

private val DarkColors = darkColorScheme(
    primary = DuoColors.FeatherGreen,
    onPrimary = Color(0xFF0F2900),
    primaryContainer = Color(0xFF234D08),
    onPrimaryContainer = DuoColors.FeatherGreenBg,
    secondary = DuoColors.MacawBlue,
    onSecondary = Color(0xFF00263B),
    secondaryContainer = Color(0xFF0F4C6E),
    onSecondaryContainer = DuoColors.MacawBlueBg,
    tertiary = DuoColors.LilacPurple,
    onTertiary = Color(0xFF2A0845),
    tertiaryContainer = Color(0xFF53227A),
    onTertiaryContainer = DuoColors.LilacPurpleBg,
    background = DuoColors.DarkCanvas,
    onBackground = Color(0xFFF1F5F9),
    surface = DuoColors.DarkCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF22363F),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun NihongoMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

/** Legacy helper preserved for backward compatibility */
@Composable
fun subTabColors(selected: Boolean): ButtonColors =
    if (selected) ButtonDefaults.buttonColors(
        containerColor = DuoColors.MacawBlue,
        contentColor = Color.White
    )
    else ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

/** Dark text for cards that use fixed light pastel backgrounds (works in dark mode too). */
val OnPastel = Color(0xFF131F24)

// =========================================================================
// DUOLINGO-STYLE 3D TACTILE CARD & BUTTON COMPONENTS
// =========================================================================

@Composable
fun Duo3DCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = if (isSystemInDarkTheme()) DuoColors.DarkBorder else DuoColors.LightBorder,
    lipColor: Color = borderColor,
    cornerRadius: Dp = 18.dp,
    lipHeight: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(lipColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(bottom = lipHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(2.dp, borderColor), shape),
            content = content
        )
    }
}

@Composable
fun Duo3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topColor: Color = DuoColors.FeatherGreen,
    lipColor: Color = DuoColors.FeatherGreenDark,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp
) {
    val actualTop = if (enabled) topColor else Color(0xFFCBD5E1)
    val actualLip = if (enabled) lipColor else Color(0xFF94A3B8)
    val actualText = if (enabled) textColor else Color(0xFF64748B)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(actualLip)
            .clickable(enabled = enabled) { onClick() }
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(actualTop)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = actualText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DuoSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = DuoColors.MacawBlue,
    activeLipColor: Color = DuoColors.MacawBlueDark
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = selectedIndex == idx
            val top = if (selected) activeColor else MaterialTheme.colorScheme.surface
            val lip = if (selected) activeLipColor else if (isSystemInDarkTheme()) DuoColors.DarkBorder else DuoColors.LightBorder
            val txtColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            val shape = RoundedCornerShape(14.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(lip)
                    .clickable { onSelect(idx) }
                    .padding(bottom = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(top)
                        .border(1.5.dp, lip, shape)
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = txtColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SemanticGrammarChip(
    label: String,
    role: String,
    bgColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(shape)
            .background(borderColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(bottom = 3.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .border(1.5.dp, borderColor, shape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = OnPastel,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Text(
                text = role,
                color = OnPastel.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
