package net.tnose.app.trisquel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ダークモードの色に何がふさわしいか不明なので以下は一時的なもの
private val DarkColorScheme = darkColorScheme(
    primary = Green,
    secondary = GreenDark,
    tertiary = GreenAccent
)

private val LightColorScheme = lightColorScheme(
    primary = Green,
    secondary = GreenDark,
    tertiary = GreenAccent

    // Other default colors to override
    // background = Color(0xFFFFFBFE),
    // surface = Color(0xFFFFFBFE),
    // onPrimary = Color.White,
    // onSecondary = Color.White,
    // onTertiary = Color.White,
    // onBackground = Color(0xFF1C1B1F),
    // onSurface = Color(0xFF1C1B1F),
)

@Composable
fun TrisquelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        /*dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }*/

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}