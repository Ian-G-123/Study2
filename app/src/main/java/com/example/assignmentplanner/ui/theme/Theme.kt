package com.example.assignmentplanner.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

var LightColorScheme = lightColorScheme(
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

var DarkColorScheme = darkColorScheme(primary = DarkPrimary, onPrimary = DarkOnPrimary, background = DarkBackground,
    onBackground = DarkOnSurface, surface = DarkSurface, onSurface = DarkOnSurface, error = RedError)

var PastelColorScheme = lightColorScheme(
    primary = PastelBlue,
    onPrimary = Color(0xFF444444),
    primaryContainer = PastelBlue.copy(alpha = 0.3f),
    secondary = PastelPink,
    onSecondary = Color(0xFF444444),
    tertiary = PastelYellow,
    background = PastelBackground,
    onBackground = Color(0xFF4A4A4A),
    surface = Color.White,
    onSurface = Color(0xFF4A4A4A)
)

var OceanColorScheme = lightColorScheme(primary = OceanPrimary, onPrimary = OceanOnPrimary, secondary = OceanSecondary,
    tertiary = OceanTertiary, background = OceanBackground, surface = Color.White)

var MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary, onPrimary = MidnightOnPrimary,
    secondary = MidnightSecondary, tertiary = MidnightTertiary,
    background = MidnightBackground, surface = MidnightSurface,
    onBackground = MidnightOnBackground, onSurface = MidnightOnBackground)

var colorSchemeCallCount = 0

@Composable
fun StudySquaredTheme(
    appTheme: AppTheme = AppTheme.Light,
    content: @Composable () -> Unit
)
{
    var colorScheme = LightColorScheme
    colorSchemeCallCount = colorSchemeCallCount + 1

    if (appTheme == AppTheme.Light) colorScheme = LightColorScheme
    else if (appTheme == AppTheme.Dark) colorScheme = DarkColorScheme
    else if (appTheme == AppTheme.Pastel)
    {
        colorScheme = PastelColorScheme
    }
    else if (appTheme == AppTheme.Ocean) { colorScheme = OceanColorScheme }
    else if (appTheme == AppTheme.Midnight)
    {
        colorScheme = MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
