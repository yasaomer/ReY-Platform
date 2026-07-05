package com.example.privateoml.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.theme.*

@Composable
fun PremiumButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  colors: ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Primary,
    contentColor = Color.Black
  )
) {
  Button(
    onClick = onClick,
    modifier = modifier
      .height(52.dp)
      .shadow(8.dp, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    enabled = enabled,
    colors = colors
  ) {
    Text(
      text = text,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  enabled: Boolean = true
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(text = label, color = TextSecondary) },
    modifier = modifier.fillMaxWidth(),
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    singleLine = true,
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = Primary,
      unfocusedBorderColor = BgCard,
      focusedLabelColor = Primary,
      cursorColor = Primary,
      focusedTextColor = TextPrimary,
      unfocusedTextColor = TextPrimary
    )
  )
}

@Composable
fun PremiumCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .shadow(4.dp, RoundedCornerShape(16.dp))
      .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = BgCard
    ),
    content = content
  )
}

@Composable
fun StatsCard(
  title: String,
  value: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  trend: String? = null,
  trendColor: Color = Secondary
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(key1 = true) {
    visible = true
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.9f, animationSpec = tween(600))
  ) {
    PremiumCard(modifier = modifier) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Icon(imageVector = icon, contentDescription = title, tint = Primary, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
          text = value,
          color = TextPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold
        )

        if (trend != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = trend,
            color = trendColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
fun PremiumLineChart(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxWidth()) {
    val width = size.width
    val height = size.height
    
    // Draw grid lines
    val gridCount = 4
    for (i in 0..gridCount) {
      val y = height * (i.toFloat() / gridCount)
      drawLine(
        color = Color(0x0DFFFFFF),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1f
      )
    }

    // Chart points coordinates (mock dashboard weekly telemetry uploads)
    val points = listOf(
      Offset(0f, height * 0.8f),
      Offset(width * 0.2f, height * 0.5f),
      Offset(width * 0.4f, height * 0.6f),
      Offset(width * 0.6f, height * 0.2f),
      Offset(width * 0.8f, height * 0.3f),
      Offset(width, height * 0.1f)
    )

    // Draw gradient path underneath the line
    val fillPath = Path().apply {
      moveTo(points.first().x, height)
      points.forEach { lineTo(it.x, it.y) }
      lineTo(points.last().x, height)
      close()
    }
    
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(Primary.copy(alpha = 0.25f), Color.Transparent),
        startY = 0f,
        endY = height
      )
    )

    // Draw connection line
    val strokePath = Path().apply {
      moveTo(points.first().x, points.first().y)
      for (i in 1 until points.size) {
        lineTo(points[i].x, points[i].y)
      }
    }

    drawPath(
      path = strokePath,
      color = Primary,
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw glowing circles on points
    points.forEach { point ->
      drawCircle(
        color = Secondary,
        radius = 4.dp.toPx(),
        center = point
      )
      drawCircle(
        color = Secondary.copy(alpha = 0.3f),
        radius = 8.dp.toPx(),
        center = point
      )
    }
  }
}

@Composable
fun PremiumDonutChart(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val radius = size.minDimension / 2
    val strokeWidth = 24.dp.toPx()
    val arcSize = radius * 2 - strokeWidth
    val topLeftOffset = Offset(
      (size.width - arcSize) / 2,
      (size.height - arcSize) / 2
    )

    // Storage sections: Sync (70%), Backups (20%), Free (10%)
    val slices = listOf(
      Triple(252f, Primary, "Sync"),
      Triple(72f, Secondary, "Backups"),
      Triple(36f, Tertiary, "Free")
    )

    var currentAngle = -90f
    slices.forEach { (sweep, color, _) ->
      drawArc(
        color = color,
        startAngle = currentAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeftOffset,
        size = Size(arcSize, arcSize),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
      currentAngle += sweep
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomSheet(
  onDismiss: () -> Unit,
  title: String,
  content: @Composable ColumnScope.() -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = BgSurface,
    contentColor = TextPrimary,
    dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) },
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(16.dp))
      content()
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
