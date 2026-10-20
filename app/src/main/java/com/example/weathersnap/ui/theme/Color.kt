package com.example.weathersnap.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// WeatherSnap Premium Brand Color Palette
// Inspired by the official WeatherSnap logo:
// Deep Forest Green, Vibrant Lime Leaf, Warm Cream, Droplet Sky Blue, Sun Gold
// ==============================================================================

// Primary Brand & Accents
val LimeAccent = Color(0xFFC5E84A)           // Vibrant lime leaf highlight & camera accent
val ForestGreenPrimary = Color(0xFF165C38)   // Rich botanical forest green
val ForestGreenContainer = Color(0xFF1B452E) // Lighter botanical green container
val ForestGreenLight = Color(0xFF28794C)     // Mid forest green for subtle highlights

// Backgrounds & Surfaces
val ForestBlackBackground = Color(0xFF0C1710)// Deep green-black background
val ForestSurface = Color(0xFF14241B)        // Layered dark forest card surface
val ForestSurfaceVariant = Color(0xFF1B3124) // Elevated input / chip surface
val ForestBorder = Color(0xFF254532)         // Restrained subtle border

// Typography & Content
val WarmCreamText = Color(0xFFF4F6EE)        // Camera cream high-contrast text
val SageHelperText = Color(0xFF9EB1A2)       // Soft muted botanical sage for secondary text
val ForestDeepDark = Color(0xFF09120D)       // Dark tone for text/icons on vibrant lime

// Weather Accents & Semantic
val SkyBlueAccent = Color(0xFF38A5FF)        // Rain droplet / sky blue
val SunnyYellowAccent = Color(0xFFFED813)    // Sun & golden rays
val ErrorRed = Color(0xFFFF6B6B)             // High-accessibility warning / error red

// Backward-compatible aliases for existing screen references
val GreenPrimary = LimeAccent
val GreenDarkBackground = ForestBlackBackground
val SurfaceDark = ForestSurface
val OnSurfaceText = WarmCreamText
val HelperText = SageHelperText
val LightBlue = SkyBlueAccent
val Orange = SunnyYellowAccent
val DarkGrey = ForestDeepDark
