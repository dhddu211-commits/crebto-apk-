package com.example.data.util

import android.net.Uri
import androidx.media3.common.MimeTypes

sealed interface ResolvedMedia {
  data class DirectStream(
    val streamUrl: String,
    val mimeType: String? = null,
    val originalUrl: String = streamUrl,
  ) : ResolvedMedia

  data class YouTubeEmbed(
    val videoId: String,
    val embedUrl: String,
    val thumbnailUrl: String,
    val originalUrl: String,
  ) : ResolvedMedia

  data class VimeoEmbed(
    val videoId: String,
    val embedUrl: String,
    val originalUrl: String,
  ) : ResolvedMedia

  data class ImageMedia(
    val imageUrl: String,
  ) : ResolvedMedia
}

object MediaUrlResolver {
  private val DRIVE_FILE_REGEX = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
  private val DRIVE_OPEN_REGEX = Regex("""drive\.google\.com/open\?.*?id=([a-zA-Z0-9_-]+)""")
  private val YOUTUBE_WATCH_REGEX = Regex("""(?:youtube\.com|music\.youtube\.com)/watch\?(?:[^#]*&)?v=([a-zA-Z0-9_-]{6,})""")
  private val YOUTUBE_SHORT_PATH_REGEX = Regex("""(?:youtu\.be/|youtube\.com/(?:shorts|embed|live|v)/)([a-zA-Z0-9_-]{6,})""")
  private val VIMEO_REGEX = Regex("""(?:vimeo\.com/(?:video/)?|player\.vimeo\.com/video/)(\d+)""")
  private val GITHUB_BLOB_REGEX = Regex("""^https?://github\.com/([^/]+)/([^/]+)/blob/(.+)$""")

  fun normalizeInputUrl(rawUrl: String): String {
    var trimmed = rawUrl.trim().replace("\n", "").replace("\r", "")
    if (trimmed.isBlank()) return ""

    if (!trimmed.startsWith("http://", ignoreCase = true) &&
      !trimmed.startsWith("https://", ignoreCase = true) &&
      !trimmed.startsWith("content://", ignoreCase = true) &&
      !trimmed.startsWith("file://", ignoreCase = true)
    ) {
      if (trimmed.contains(".")) {
        trimmed = "https://$trimmed"
      }
    }

    // Google Drive share link -> direct download link
    DRIVE_FILE_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
      return "https://drive.google.com/uc?export=download&id=$fileId"
    }
    DRIVE_OPEN_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
      return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    // Dropbox share link -> raw media stream
    if (trimmed.contains("dropbox.com", ignoreCase = true)) {
      return when {
        trimmed.contains("dl=0") -> trimmed.replace("dl=0", "raw=1")
        trimmed.contains("raw=1") || trimmed.contains("dl=1") -> trimmed
        trimmed.contains("?") -> "$trimmed&raw=1"
        else -> "$trimmed?raw=1"
      }
    }

    // GitHub blob link -> raw.githubusercontent.com
    GITHUB_BLOB_REGEX.find(trimmed)?.let { match ->
      val user = match.groupValues[1]
      val repo = match.groupValues[2]
      val rest = match.groupValues[3]
      return "https://raw.githubusercontent.com/$user/$repo/$rest"
    }

    return trimmed
  }

  fun extractYouTubeVideoId(url: String): String? {
    val normalized = url.trim()
    YOUTUBE_WATCH_REGEX.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
    YOUTUBE_SHORT_PATH_REGEX.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
    return null
  }

  fun extractVimeoVideoId(url: String): String? =
    VIMEO_REGEX.find(url.trim())?.groupValues?.getOrNull(1)

  fun extractThumbnailUrl(rawUrl: String): String {
    val normalized = normalizeInputUrl(rawUrl)
    extractYouTubeVideoId(normalized)?.let { ytId ->
      return "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
    }
    return ""
  }

  fun isLikelyImageUrl(rawUrl: String): Boolean {
    val clean = rawUrl.trim().substringBefore("?").substringBefore("#").lowercase()
    val fullLower = rawUrl.trim().lowercase()
    return clean.endsWith(".jpg") ||
      clean.endsWith(".jpeg") ||
      clean.endsWith(".png") ||
      clean.endsWith(".webp") ||
      clean.endsWith(".gif") ||
      clean.endsWith(".bmp") ||
      clean.endsWith(".heic") ||
      clean.endsWith(".avif") ||
      fullLower.contains("mime=image") ||
      fullLower.contains("%2fphotos%2f")
  }

  fun resolve(rawUrl: String, forcePhoto: Boolean = false): ResolvedMedia {
    val normalized = normalizeInputUrl(rawUrl)
    if (forcePhoto || (isLikelyImageUrl(normalized) && !CloudMediaStorage.isVideoUri(normalized))) {
      return ResolvedMedia.ImageMedia(normalized)
    }

    extractYouTubeVideoId(normalized)?.let { ytId ->
      return ResolvedMedia.YouTubeEmbed(
        videoId = ytId,
        embedUrl = "https://www.youtube.com/embed/$ytId?autoplay=1&playsinline=1&rel=0&modestbranding=1",
        thumbnailUrl = "https://img.youtube.com/vi/$ytId/hqdefault.jpg",
        originalUrl = normalized,
      )
    }

    extractVimeoVideoId(normalized)?.let { vimeoId ->
      return ResolvedMedia.VimeoEmbed(
        videoId = vimeoId,
        embedUrl = "https://player.vimeo.com/video/$vimeoId?autoplay=1&playsinline=1",
        originalUrl = normalized,
      )
    }

    val cleanLower = normalized.substringBefore("?").substringBefore("#").lowercase()
    val mimeType =
      when {
        cleanLower.endsWith(".m3u8") || normalized.lowercase().contains(".m3u8") ->
          MimeTypes.APPLICATION_M3U8
        cleanLower.endsWith(".mpd") || normalized.lowercase().contains(".mpd") ->
          MimeTypes.APPLICATION_MPD
        else -> null
      }

    // Re-parse with Uri to ensure valid spaces encoding if needed
    val safeUrl =
      if (normalized.contains(" ")) {
        normalized.replace(" ", "%20")
      } else {
        normalized
      }
    Uri.parse(safeUrl)

    return ResolvedMedia.DirectStream(
      streamUrl = safeUrl,
      mimeType = mimeType,
      originalUrl = normalized,
    )
  }
}
