package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.DeepAmberGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.ObsidianVoid

/**
 * Custom Crypto Luxury Loading Theme:
 * Rotating geometric gold vault prism with orbital energy rings and shimmer skeletons.
 */
@Composable
fun CryptoVaultLoader(
  modifier: Modifier = Modifier,
  size: Dp = 76.dp,
) {
  val transition = rememberInfiniteTransition(label = "cryptoVaultLoader")
  val outerAngle by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
      label = "outerRing",
    )
  val innerAngle by
    transition.animateFloat(
      initialValue = 360f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
      label = "innerRing",
    )
  val pulse by
    transition.animateFloat(
      initialValue = 0.65f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
      label = "pulse",
    )

  Canvas(modifier = modifier.size(size)) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val r = this.size.minDimension / 2f

    // Ambient gold halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(MetallicGold.copy(alpha = 0.28f * pulse), Color.Transparent),
          center = Offset(cx, cy),
          radius = r,
        ),
      radius = r,
    )

    // Outer segmented orbital ring
    rotate(outerAngle, pivot = Offset(cx, cy)) {
      drawArc(
        brush = Brush.sweepGradient(listOf(Color.Transparent, MetallicGold, ChampagneGold, Color.Transparent)),
        startAngle = 0f,
        sweepAngle = 240f,
        useCenter = false,
        style = Stroke(width = r * 0.085f, cap = StrokeCap.Round),
      )
    }

    // Inner counter-rotating ring
    rotate(innerAngle, pivot = Offset(cx, cy)) {
      val inset = r * 0.22f
      drawArc(
        color = DeepAmberGold.copy(alpha = 0.85f),
        startAngle = 30f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
        style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
      )
    }

    // Central faceted diamond emblem
    val diamondRadius = r * 0.34f * pulse
    val diamondPath =
      Path().apply {
        moveTo(cx, cy - diamondRadius)
        lineTo(cx + diamondRadius * 0.85f, cy)
        lineTo(cx, cy + diamondRadius)
        lineTo(cx - diamondRadius * 0.85f, cy)
        close()
      }
    drawPath(
      path = diamondPath,
      brush = Brush.linearGradient(listOf(ChampagneGold, MetallicGold, DeepAmberGold)),
    )
  }
}

@Composable
fun CryptoFullScreenLoader(
  message: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(ObsidianVoid, MidnightOnyx, ObsidianVoid)
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp),
    ) {
      CryptoVaultLoader(size = 92.dp)
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = ChampagneGold,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun CryptoGoldShimmerCard(
  modifier: Modifier = Modifier,
  height: Dp = 180.dp,
) {
  val transition = rememberInfiniteTransition(label = "goldShimmer")
  val shift by
    transition.animateFloat(
      initialValue = -300f,
      targetValue = 900f,
      animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
      label = "shimmerOffset",
    )

  val brush =
    Brush.linearGradient(
      colors =
        listOf(
          CharcoalGlass,
          Color(0xFF2A2416),
          MetallicGold.copy(alpha = 0.22f),
          CharcoalGlass,
        ),
      start = Offset(shift, 0f),
      end = Offset(shift + 320f, 160f),
    )

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .clip(RoundedCornerShape(18.dp))
        .background(brush)
  )
}
