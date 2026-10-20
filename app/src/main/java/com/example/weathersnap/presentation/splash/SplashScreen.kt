package com.example.weathersnap.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weathersnap.R
import com.example.weathersnap.ui.theme.ForestBlackBackground
import com.example.weathersnap.ui.theme.SageHelperText
import com.example.weathersnap.ui.theme.WarmCreamText
import kotlinx.coroutines.delay

/**
 * Premium Branded Splash Screen for WeatherSnap.
 *
 * Displays the official WeatherSnap logo centered with natural aspect ratio,
 * a tasteful deep forest-green atmospheric lighting gradient, and the
 * mandatory developer attribution "From KAVTORS".
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        // Elegant duration: brief enough for instant feel, long enough for brand recognition
        delay(800L)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF143522), // Subtle upper forest glow
                        ForestBlackBackground, // Deep botanical background
                        Color(0xFF070F0A)  // Grounding shadow
                    )
                )
            )
    ) {
        // Main Center Content: Logo, App Title, Tagline
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha.value)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WeatherSnap Official Logo with subtle elevation & corner curve
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(36.dp),
                        ambientColor = Color(0xFFC5E84A).copy(alpha = 0.25f),
                        spotColor = Color(0xFF000000).copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF0D2517)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_weathersnap),
                    contentDescription = "WeatherSnap Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "WeatherSnap",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = WarmCreamText,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // App Tagline
            Text(
                text = "Live crowd weather & field notes",
                fontSize = 13.sp,
                color = SageHelperText,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Attribution: Mandatory "From KAVTORS"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(contentAlpha.value)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "From KAVTORS",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = WarmCreamText.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
