package com.example.privateoml.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.theme.BgBase
import com.example.privateoml.theme.Primary
import com.example.privateoml.theme.Secondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onNavigateNext: () -> Unit,
  modifier: Modifier = Modifier
) {
  var startAnimation by remember { mutableStateOf(false) }
  
  val alphaAnim by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
    label = "alpha"
  )
  
  val scaleAnim by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.8f,
    animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
    label = "scale"
  )

  LaunchedEffect(key1 = true) {
    startAnimation = true
    delay(2000)
    onNavigateNext()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(BgBase, Color(0xFF0D0E15))
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .scale(scaleAnim)
        .alpha(alphaAnim),
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(100.dp)
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Logo",
          tint = Primary,
          modifier = Modifier.size(80.dp)
        )
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      Text(
        text = "Private OML",
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
      )
      
      Spacer(modifier = Modifier.height(8.dp))
      
      Text(
        text = "Secure Autonomous Portal",
        color = Secondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp
      )
    }
  }
}
