package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.R
import com.example.data.util.MediaUrlResolver
import com.example.data.util.ResolvedMedia
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory

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
  isPhoto: Boolean = false,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()

  val resolvedMedia = remember(mediaUrl, isPhoto) { MediaUrlResolver.resolve(mediaUrl, forcePhoto = isPhoto) }

  if (mediaUrl.isBlank()) {
    Box(modifier = modifier.background(ObsidianVoid), contentAlignment = Alignment.Center) {
      Image(
        painter = painterResource(id = R.drawable.img_default_banner),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
    return
  }

  when (resolvedMedia) {
    is ResolvedMedia.ImageMedia -> {
      Box(modifier = modifier.background(ObsidianVoid), contentAlignment = Alignment.Center) {
        AsyncImage(
          model = ImageRequest.Builder(context).data(resolvedMedia.imageUrl).crossfade(true).build(),
          imageLoader = imageLoader,
          contentDescription = null,
          contentScale = contentScale,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    is ResolvedMedia.YouTubeEmbed -> {
      WebEmbedPlayerBox(
        embedUrl = resolvedMedia.embedUrl,
        baseUrl = "https://www.youtube.com",
        originalUrl = resolvedMedia.originalUrl,
        thumbnailUrl = fallbackImageUrl.ifBlank { resolvedMedia.thumbnailUrl },
        isActive = isActive,
        showControls = showControls,
        contentScale = contentScale,
        modifier = modifier,
      )
    }

    is ResolvedMedia.VimeoEmbed -> {
      WebEmbedPlayerBox(
        embedUrl = resolvedMedia.embedUrl,
        baseUrl = "https://player.vimeo.com",
        originalUrl = resolvedMedia.originalUrl,
        thumbnailUrl = fallbackImageUrl,
        isActive = isActive,
        showControls = showControls,
        contentScale = contentScale,
        modifier = modifier,
      )
    }

    is ResolvedMedia.DirectStream -> {
      var isMuted by remember(resolvedMedia.streamUrl) { mutableStateOf(mutedByDefault) }
      var isPausedByUser by remember(resolvedMedia.streamUrl) { mutableStateOf(false) }
      var hasError by remember(resolvedMedia.streamUrl) { mutableStateOf(false) }
      var isBuffering by remember(resolvedMedia.streamUrl) { mutableStateOf(true) }
      var retryToken by remember(resolvedMedia.streamUrl) { mutableIntStateOf(0) }

      val latestIsActive by rememberUpdatedState(isActive)
      val latestIsPaused by rememberUpdatedState(isPausedByUser)

      val exoPlayer =
        remember(resolvedMedia.streamUrl) {
          val httpFactory =
            DefaultHttpDataSource.Factory()
              .setAllowCrossProtocolRedirects(true)
              .setConnectTimeoutMs(15_000)
              .setReadTimeoutMs(15_000)
              .setUserAgent("CryptoApp/1.0 (Linux; Android)")
          val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
          val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
          val mediaItem =
            MediaItem.Builder()
              .setUri(Uri.parse(resolvedMedia.streamUrl))
              .apply { resolvedMedia.mimeType?.let { setMimeType(it) } }
              .build()

          ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
              repeatMode = Player.REPEAT_MODE_ONE
              volume = if (mutedByDefault) 0f else 1f
              setMediaItem(mediaItem)
              prepare()
            }
        }

      DisposableEffect(exoPlayer) {
        val playerListener =
          object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
              isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
              if (playbackState == Player.STATE_READY) {
                hasError = false
              }
            }

            override fun onPlayerError(error: PlaybackException) {
              hasError = true
              isBuffering = false
            }
          }
        exoPlayer.addListener(playerListener)
        val lifecycleObserver =
          LifecycleEventObserver { _, event ->
            when (event) {
              Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
              Lifecycle.Event.ON_RESUME -> if (latestIsActive && !latestIsPaused) exoPlayer.play()
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

      LaunchedEffect(exoPlayer, isActive, isPausedByUser, retryToken) {
        exoPlayer.playWhenReady = isActive && !isPausedByUser
        if (!isActive || isPausedByUser) exoPlayer.pause() else exoPlayer.play()
      }

      LaunchedEffect(exoPlayer, isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }

      if (hasError) {
        Box(modifier = modifier.background(ObsidianVoid), contentAlignment = Alignment.Center) {
          if (fallbackImageUrl.isNotBlank()) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(fallbackImageUrl).crossfade(true).build(),
              imageLoader = imageLoader,
              contentDescription = null,
              contentScale = contentScale,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_default_banner),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              alpha = 0.25f,
              modifier = Modifier.fillMaxSize(),
            )
          }

          if (showControls) {
            Surface(
              color = CharcoalGlass.copy(alpha = 0.92f),
              shape = RoundedCornerShape(18.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
              modifier = Modifier.padding(20.dp),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = ChampagneGold,
                  modifier = Modifier.size(34.dp),
                )
                Text(
                  text = strings.videoPlaybackError,
                  color = WarmIvory,
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = {
                      hasError = false
                      isBuffering = true
                      exoPlayer.prepare()
                      exoPlayer.playWhenReady = true
                      retryToken++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                  ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.retryLabel, fontWeight = FontWeight.Bold)
                  }
                  OutlinedButton(
                    onClick = {
                      runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolvedMedia.originalUrl)))
                      }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
                  ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openLinkLabel, color = ChampagneGold)
                  }
                }
              }
            }
          }
        }
        return
      }

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
              if (fallbackImageUrl.isNotBlank()) {
                AsyncImage(
                  model = fallbackImageUrl,
                  imageLoader = imageLoader,
                  contentDescription = null,
                  contentScale = contentScale,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              if (isActive) {
                CryptoVaultLoader(size = 48.dp)
              }
            }
          },
        )

        if (isBuffering && isActive && showControls) {
          Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center,
          ) {
            CryptoVaultLoader(size = 42.dp)
          }
        }

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
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebEmbedPlayerBox(
  embedUrl: String,
  baseUrl: String,
  originalUrl: String,
  thumbnailUrl: String,
  isActive: Boolean,
  showControls: Boolean,
  contentScale: ContentScale,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()

  Box(modifier = modifier.background(ObsidianVoid)) {
    if (!isActive) {
      if (thumbnailUrl.isNotBlank()) {
        AsyncImage(
          model = thumbnailUrl,
          imageLoader = imageLoader,
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
      return@Box
    }

    val html =
      remember(embedUrl) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #07070A; overflow: hidden; }
            iframe { width: 100%; height: 100%; border: 0; }
          </style>
        </head>
        <body>
          <iframe src="$embedUrl" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
        </body>
        </html>
        """.trimIndent()
      }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(embedUrl) {
      onDispose {
        webViewRef?.apply {
          stopLoading()
          loadUrl("about:blank")
          onPause()
          destroy()
        }
        webViewRef = null
      }
    }

    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams =
            ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          setBackgroundColor(android.graphics.Color.BLACK)
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.mediaPlaybackRequiresUserGesture = false
          settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          webChromeClient = WebChromeClient()
          webViewClient = WebViewClient()
          loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
          webViewRef = this
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    if (showControls) {
      Surface(
        onClick = {
          runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(originalUrl)))
          }
        },
        color = ObsidianVoid.copy(alpha = 0.72f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = strings.openLinkLabel,
            tint = ChampagneGold,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = strings.openLinkLabel,
            style = MaterialTheme.typography.labelSmall,
            color = ChampagneGold,
          )
        }
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
