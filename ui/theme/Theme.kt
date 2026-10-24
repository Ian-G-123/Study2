package com.example.assignmentplanner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/** default light scheme */
private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary = BlueSecondary, 
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = BlueOnContainer,
    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    tertiaryContainer = OrangeContainer,
    onTertiaryContainer = OrangeOnContainer,
    error = RedError,
    onError = RedOnError,
    errorContainer = RedContainer,
    onErrorContainer = RedOnContainer,
    background = NeutralBackground,
    onBackground = NeutralOnSurface,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = BlueContainer,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline
)

/** default dark scheme */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = RedError
)

/** placeholder pastel scheme */
private val PastelColorScheme = lightColorScheme(
    primary = PastelBlue,
    secondary = PastelPink,
    tertiary = PastelYellow,
    background = PastelGreen,
    surface = NeutralSurface
)

/** main theme entry */
@Composable
fun AssignmentPlannerTheme(
    appTheme: AppTheme = AppTheme.Light,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> DarkColorScheme
        AppTheme.Pastel -> PastelColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
