package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppThemeId(
    val id: Int,
    val title: String,
    val subtitle: String,
    val isDark: Boolean
) {
    ROYAL_DAYLIGHT(
        id = 1,
        title = "Theme 1 – Daylight Royal Blue",
        subtitle = "High-Contrast Light • Slate & Royal Blue",
        isDark = false
    ),
    MIDNIGHT_CARBON(
        id = 2,
        title = "Theme 2 – Midnight Carbon Cyan",
        subtitle = "AMOLED Dark • Carbon & Electric Cyan",
        isDark = true
    ),
    ALKALINE_EMERALD(
        id = 3,
        title = "Theme 3 – Alkaline Emerald & Teal",
        subtitle = "Crisp Light • Mint-White & Deep Teal",
        isDark = false
    ),
    NAVY_GOLD(
        id = 4,
        title = "Theme 4 – High-Visibility Navy & Gold",
        subtitle = "Warm Ivory • Midnight Navy & Gold",
        isDark = false
    ),
    TWILIGHT_OCEAN(
        id = 5,
        title = "Theme 5 – Twilight Ocean & Mint",
        subtitle = "Deep Ocean Dark • Elevated Navy & Mint",
        isDark = true
    );

    companion object {
        fun fromId(id: Int): AppThemeId = entries.firstOrNull { it.id == id } ?: ROYAL_DAYLIGHT
    }
}

@Immutable
data class WaterRiderColors(
    val themeId: AppThemeId,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryAccent: Color,
    val secondaryAccentText: Color,
    val onSecondaryAccent: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val topBarContainer: Color,
    val topBarTitle: Color,
    val topBarSubtitle: Color,
    val navBarContainer: Color,
    val navBarSelectedIcon: Color,
    val navBarSelectedText: Color,
    val navBarIndicator: Color,
    val navBarUnselected: Color,
    val paid: Color,
    val onPaid: Color,
    val paidContainer: Color,
    val utang: Color,
    val onUtang: Color,
    val utangContainer: Color,
    val utangSubtext: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color
)

val Theme1DaylightRoyal = WaterRiderColors(
    themeId = AppThemeId.ROYAL_DAYLIGHT,
    isDark = false,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    border = Color(0xFF94A3B8),
    textPrimary = Color(0xFF090D16),
    textSecondary = Color(0xFF1E293B),
    primaryAccent = Color(0xFF0047AB),
    onPrimaryAccent = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF003380),
    secondaryAccent = Color(0xFF0F766E),
    secondaryAccentText = Color(0xFF0F766E),
    onSecondaryAccent = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    topBarContainer = Color(0xFF0047AB),
    topBarTitle = Color(0xFFFFFFFF),
    topBarSubtitle = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFFFFFFFF),
    navBarSelectedIcon = Color(0xFF0047AB),
    navBarSelectedText = Color(0xFF0047AB),
    navBarIndicator = Color(0xFFDBEAFE),
    navBarUnselected = Color(0xFF090D16),
    paid = Color(0xFF047857),
    onPaid = Color(0xFFFFFFFF),
    paidContainer = Color(0xFFD1FAE5),
    utang = Color(0xFFB91C1C),
    onUtang = Color(0xFFFFFFFF),
    utangContainer = Color(0xFFFEE2E2),
    utangSubtext = Color(0xFF7F1D1D),
    expense = Color(0xFFC2410C),
    onExpense = Color(0xFFFFFFFF),
    expenseContainer = Color(0xFFFFEDD5)
)

val Theme2MidnightCarbon = WaterRiderColors(
    themeId = AppThemeId.MIDNIGHT_CARBON,
    isDark = true,
    background = Color(0xFF090D16),
    surface = Color(0xFF131B2E),
    surfaceVariant = Color(0xFF1B263F),
    border = Color(0xFF22D3EE),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFF1F5F9),
    primaryAccent = Color(0xFF06B6D4),
    onPrimaryAccent = Color(0xFF090D16),
    primaryContainer = Color(0xFF0E7490),
    onPrimaryContainer = Color(0xFFECFEFF),
    secondaryAccent = Color(0xFF22D3EE),
    secondaryAccentText = Color(0xFF67E8F9),
    onSecondaryAccent = Color(0xFF090D16),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    topBarContainer = Color(0xFF131B2E),
    topBarTitle = Color(0xFFFFFFFF),
    topBarSubtitle = Color(0xFF22D3EE),
    navBarContainer = Color(0xFF131B2E),
    navBarSelectedIcon = Color(0xFF090D16),
    navBarSelectedText = Color(0xFF22D3EE),
    navBarIndicator = Color(0xFF06B6D4),
    navBarUnselected = Color(0xFFE2E8F0),
    paid = Color(0xFF34D399),
    onPaid = Color(0xFF090D16),
    paidContainer = Color(0xFF064E3B),
    utang = Color(0xFFF87171),
    onUtang = Color(0xFF090D16),
    utangContainer = Color(0xFF7F1D1D),
    utangSubtext = Color(0xFFFECACA),
    expense = Color(0xFFFBBF24),
    onExpense = Color(0xFF090D16),
    expenseContainer = Color(0xFF78350F)
)

val Theme3AlkalineEmerald = WaterRiderColors(
    themeId = AppThemeId.ALKALINE_EMERALD,
    isDark = false,
    background = Color(0xFFF0FDF4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECFDF5),
    border = Color(0xFF0D9488),
    textPrimary = Color(0xFF090D16),
    textSecondary = Color(0xFF0F172A),
    primaryAccent = Color(0xFF0F766E),
    onPrimaryAccent = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondaryAccent = Color(0xFF0D9488),
    secondaryAccentText = Color(0xFF0F766E),
    onSecondaryAccent = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    topBarContainer = Color(0xFF0F766E),
    topBarTitle = Color(0xFFFFFFFF),
    topBarSubtitle = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFFFFFFFF),
    navBarSelectedIcon = Color(0xFF0F766E),
    navBarSelectedText = Color(0xFF0F766E),
    navBarIndicator = Color(0xFFCCFBF1),
    navBarUnselected = Color(0xFF090D16),
    paid = Color(0xFF047857),
    onPaid = Color(0xFFFFFFFF),
    paidContainer = Color(0xFFD1FAE5),
    utang = Color(0xFFB91C1C),
    onUtang = Color(0xFFFFFFFF),
    utangContainer = Color(0xFFFEE2E2),
    utangSubtext = Color(0xFF7F1D1D),
    expense = Color(0xFFC2410C),
    onExpense = Color(0xFFFFFFFF),
    expenseContainer = Color(0xFFFFEDD5)
)

val Theme4NavyGold = WaterRiderColors(
    themeId = AppThemeId.NAVY_GOLD,
    isDark = false,
    background = Color(0xFFFEFCE8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFEF9C3),
    border = Color(0xFF94A3B8),
    textPrimary = Color(0xFF090D16),
    textSecondary = Color(0xFF1E293B),
    primaryAccent = Color(0xFF1E3A8A),
    onPrimaryAccent = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondaryAccent = Color(0xFFF59E0B),
    secondaryAccentText = Color(0xFF92400E),
    onSecondaryAccent = Color(0xFF090D16),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    topBarContainer = Color(0xFF1E3A8A),
    topBarTitle = Color(0xFFFFFFFF),
    topBarSubtitle = Color(0xFFFDE68A),
    navBarContainer = Color(0xFFFFFFFF),
    navBarSelectedIcon = Color(0xFF1E3A8A),
    navBarSelectedText = Color(0xFF1E3A8A),
    navBarIndicator = Color(0xFFFDE68A),
    navBarUnselected = Color(0xFF090D16),
    paid = Color(0xFF047857),
    onPaid = Color(0xFFFFFFFF),
    paidContainer = Color(0xFFD1FAE5),
    utang = Color(0xFFB91C1C),
    onUtang = Color(0xFFFFFFFF),
    utangContainer = Color(0xFFFEE2E2),
    utangSubtext = Color(0xFF7F1D1D),
    expense = Color(0xFFB45309),
    onExpense = Color(0xFFFFFFFF),
    expenseContainer = Color(0xFFFEF3C7)
)

val Theme5TwilightOcean = WaterRiderColors(
    themeId = AppThemeId.TWILIGHT_OCEAN,
    isDark = true,
    background = Color(0xFF0B192C),
    surface = Color(0xFF1E3E62),
    surfaceVariant = Color(0xFF16304F),
    border = Color(0xFF2DD4BF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFF1F5F9),
    primaryAccent = Color(0xFF2DD4BF),
    onPrimaryAccent = Color(0xFF0B192C),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondaryAccent = Color(0xFF5EEAD4),
    secondaryAccentText = Color(0xFF99F6E4),
    onSecondaryAccent = Color(0xFF0B192C),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    topBarContainer = Color(0xFF1E3E62),
    topBarTitle = Color(0xFFFFFFFF),
    topBarSubtitle = Color(0xFF2DD4BF),
    navBarContainer = Color(0xFF1E3E62),
    navBarSelectedIcon = Color(0xFF0B192C),
    navBarSelectedText = Color(0xFF2DD4BF),
    navBarIndicator = Color(0xFF2DD4BF),
    navBarUnselected = Color(0xFFFFFFFF),
    paid = Color(0xFF34D399),
    onPaid = Color(0xFF0B192C),
    paidContainer = Color(0xFF064E3B),
    utang = Color(0xFFF87171),
    onUtang = Color(0xFF0B192C),
    utangContainer = Color(0xFF7F1D1D),
    utangSubtext = Color(0xFFFECACA),
    expense = Color(0xFFFBBF24),
    onExpense = Color(0xFF0B192C),
    expenseContainer = Color(0xFF78350F)
)

typealias AppThemeColors = WaterRiderColors

fun paletteForTheme(themeId: AppThemeId): WaterRiderColors = when (themeId) {
    AppThemeId.ROYAL_DAYLIGHT -> Theme1DaylightRoyal
    AppThemeId.MIDNIGHT_CARBON -> Theme2MidnightCarbon
    AppThemeId.ALKALINE_EMERALD -> Theme3AlkalineEmerald
    AppThemeId.NAVY_GOLD -> Theme4NavyGold
    AppThemeId.TWILIGHT_OCEAN -> Theme5TwilightOcean
}

// Default Theme 1 constants (kept for non-composable/fallback references)
val RoyalBlue = Theme1DaylightRoyal.primaryAccent
val RoyalBlueContainer = Theme1DaylightRoyal.primaryContainer
val SeafoamTeal = Theme1DaylightRoyal.secondaryAccent
val SeafoamTealContainer = Theme1DaylightRoyal.secondaryContainer
val UtangRed = Theme1DaylightRoyal.utang
val UtangRedContainer = Theme1DaylightRoyal.utangContainer
val PaidGreen = Theme1DaylightRoyal.paid
val PaidGreenContainer = Theme1DaylightRoyal.paidContainer
val ExpenseOrange = Theme1DaylightRoyal.expense
val ExpenseOrangeContainer = Theme1DaylightRoyal.expenseContainer
