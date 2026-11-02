package com.trustmesh.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StitchColorScheme = darkColorScheme(
    primary          = StitchPrimaryTeal,
    onPrimary        = Color.White,
    primaryContainer = StitchCardDark,
    onPrimaryContainer = StitchMintGlow,
    secondary        = StitchMintGlow,
    onSecondary      = StitchBgDark,
    background       = StitchBgDark,
    onBackground     = StitchTextPrimary,
    surface          = StitchSurfaceDark,
    onSurface        = StitchTextPrimary,
    surfaceVariant   = StitchCardDark,
    onSurfaceVariant = StitchTextSecondary,
    outline          = StitchDividerDark
)

@Composable
fun TrustMeshTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = StitchColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                // Enable edge-to-edge layout
                WindowCompat.setDecorFitsSystemWindows(window, false)
                // Transparent status bar & dark navigation bar for deep cyber dark theme
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                // False for dark background (light text in status & nav bar)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
