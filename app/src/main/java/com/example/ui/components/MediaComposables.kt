package com.example.ui.components

import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.R
import com.example.data.util.CloudMediaStorage
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianVoid

@Composable
fun rememberCryptoImageLoader(): ImageLoader {
  val context = LocalContext.current
  return remember(context) {
    ImageLoader.Builder(context)
      .components {
        if (Build.VERSION.SDK_INT >= 28) {
          add(ImageDecoderDecoder.Factory())
        } else {
          add(GifDecoder.Factory())
        }
      }
      .build()
  }
}

@OptIn(UnstableApi::class)
@Composable
fun CryptoVideoPlayer(
  mediaUrl: String,
  isActive: Boolean,
  modifier: Modifier = Modifier,
  mutedByDefault: Boolean = false,
  contentScale: ContentScale = ContentScale.Crop,
  showControls: Boolean = true,
  fallbackImageUrl: String = "",
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val isVideo = remember(mediaUrl) {
    val clean = mediaUrl.substringBefore("?").lowercase()
    val looksLikeImage =
      clean.endsWith(".jpg") ||
        clean.endsWith(".jpeg") ||
        clean.endsWith(".png") ||
        clean.endsWith(".webp") ||
        clean.endsWith(".gif")
    mediaUrl.isNotBlank() &&
      !looksLikeImage &&
      (CloudMediaStorage.isVideoUri(mediaUrl) ||
        mediaUrl.startsWith("content://") ||
        mediaUrl.startsWith("file://") ||
        mediaUrl.startsWith("http"))
  }

  var isMuted by remember(mediaUrl) { mutableStateOf(mutedByDefault) }
  var isPausedByUser by remember(mediaUrl) { mutableStateOf(false) }
  var hasError by remember(mediaUrl) { mutableStateOf(false) }

  if (!isVideo || hasError) {
    Box(modifier = modifier.background(ObsidianVoid), contentAlignment = Alignment.Center) {
      if (fallbackImageUrl.isNotBlank() || mediaUrl.isNotBlank()) {
        AsyncImage(
          model =
            ImageRequest.Builder(context)
              .data(fallbackImageUrl.ifBlank { mediaUrl })
              .crossfade(true)
              .build(),
          imageLoader = rememberCryptoImageLoader(),
          contentDescription = null,
          contentScale = contentScale,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.img_default_banner),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
    return
  }

  val exoPlayer =
    remember(mediaUrl) {
      ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        volume = if (mutedByDefault) 0f else 1f
        setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
        prepare()
      }
    }

  DisposableEffect(exoPlayer) {
    val playerListener =
      object : Player.Listener {
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
          hasError = true
        }
      }
    exoPlayer.addListener(playerListener)
    val lifecycleObserver =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
          Lifecycle.Event.ON_RESUME -> if (isActive && !isPausedByUser) exoPlayer.play()
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
      exoPlayer.removeListener(playerListener)
      exoPlayer.release()
    }
  }

  LaunchedEffect(isActive, isPausedByUser) {
    exoPlayer.playWhenReady = isActive && !isPausedByUser
    if (!isActive || isPausedByUser) exoPlayer.pause() else exoPlayer.play()
  }

  LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }

  Box(
    modifier =
      modifier
        .background(ObsidianVoid)
        .then(
          if (showControls) {
            Modifier.clickable { isPausedByUser = !isPausedByUser }
          } else {
            Modifier
          }
        )
  ) {
    ContentFrame(
      player = exoPlayer,
      modifier = Modifier.fillMaxSize(),
      surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
      contentScale = contentScale,
      shutter = {
        Box(
          modifier = Modifier.fillMaxSize().background(ObsidianVoid),
          contentAlignment = Alignment.Center,
        ) {
          CryptoVaultLoader(size = 48.dp)
        }
      },
    )

    if (showControls) {
      if (isPausedByUser) {
        Box(
          modifier =
            Modifier
              .align(Alignment.Center)
              .size(64.dp)
              .clip(CircleShape)
              .background(ObsidianVoid.copy(alpha = 0.65f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = ChampagneGold,
            modifier = Modifier.size(36.dp),
          )
        }
      }

      IconButton(
        onClick = { isMuted = !isMuted },
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(ObsidianVoid.copy(alpha = 0.55f)),
      ) {
        Icon(
          imageVector =
            if (isMuted) Icons.AutoMirrored.Filled.VolumeOff
            else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = "Toggle audio",
          tint = ChampagneGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

@Composable
fun AnimatedMediaAvatar(
  mediaUrl: String,
  mediaType: String,
  size: Dp,
  modifier: Modifier = Modifier,
  hasStoryRing: Boolean = false,
  contentDescription: String? = null,
) {
  val context = LocalContext.current
  val imageLoader = rememberCryptoImageLoader()
  val borderBrush =
    if (hasStoryRing) {
      Brush.sweepGradient(listOf(MetallicGold, ChampagneGold, Color(0xFF10B981), MetallicGold))
    } else {
      Brush.linearGradient(listOf(MetallicGold.copy(alpha = 0.8f), ChampagneGold.copy(alpha = 0.5f)))
    }

  Box(
    modifier =
      modifier
        .size(size)
        .border(width = if (hasStoryRing) 2.5.dp else 1.5.dp, brush = borderBrush, shape = CircleShape)
        .padding(if (hasStoryRing) 3.dp else 1.5.dp)
        .clip(CircleShape)
        .background(CharcoalGlass),
    contentAlignment = Alignment.Center,
  ) {
    if (mediaType == "video" && mediaUrl.isNotBlank()) {
      CryptoVideoPlayer(
        mediaUrl = mediaUrl,
        isActive = true,
        mutedByDefault = true,
        contentScale = ContentScale.Crop,
        showControls = false,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
      )
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(2.dp)
            .size(16.dp)
            .clip(CircleShape)
            .background(ObsidianVoid.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Videocam,
          contentDescription = contentDescription,
          tint = ChampagneGold,
          modifier = Modifier.size(10.dp),
        )
      }
    } else if (mediaUrl.isNotBlank()) {
      AsyncImage(
        model = ImageRequest.Builder(context).data(mediaUrl).crossfade(true).build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_default_avatar),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
      )
    }
  }
}

@Composable
fun AnimatedMediaBanner(
  mediaUrl: String,
  mediaType: String,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(0.dp),
) {
  val context = LocalContext.current
  val imageLoader = rememberCryptoImageLoader()

  Box(modifier = modifier.clip(shape).background(CharcoalGlass)) {
    if (mediaType == "video" && mediaUrl.isNotBlank()) {
      CryptoVideoPlayer(
        mediaUrl = mediaUrl,
        isActive = true,
        mutedByDefault = true,
        contentScale = ContentScale.Crop,
        showControls = false,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (mediaUrl.isNotBlank()) {
      AsyncImage(
        model = ImageRequest.Builder(context).data(mediaUrl).crossfade(true).build(),
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_default_banner),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
