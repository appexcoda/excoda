package app.excoda.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5D5FEF),
    secondary = Color(0xFF00BFA6),
    tertiary = Color(0xFFEE5396)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4B6FF),
    secondary = Color(0xFF40E0D0),
    tertiary = Color(0xFFFF7FBF)
)

@Composable
fun ExcodaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = ExcodaTypography,
        content = content
    )
}