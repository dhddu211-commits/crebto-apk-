package com.example.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlResolverTest {
  @Test
  fun normalizeInputUrl_driveDropboxAndYoutube() {
    assertEquals(
      "https://drive.google.com/uc?export=download&id=1AbCdEfG_99",
      MediaUrlResolver.normalizeInputUrl("  https://drive.google.com/file/d/1AbCdEfG_99/view?usp=sharing  "),
    )
    assertEquals(
      "https://www.dropbox.com/s/xyz/clip.mp4?raw=1",
      MediaUrlResolver.normalizeInputUrl("https://www.dropbox.com/s/xyz/clip.mp4?dl=0"),
    )
    assertEquals(
      "https://youtu.be/dQw4w9WgXcQ",
      MediaUrlResolver.normalizeInputUrl("youtu.be/dQw4w9WgXcQ"),
    )
  }

  @Test
  fun resolve_youtubeAndHlsAndPhoto() {
    val yt = MediaUrlResolver.resolve("https://www.youtube.com/shorts/abc123XYZ00")
    assertTrue(yt is ResolvedMedia.YouTubeEmbed)
    assertEquals("abc123XYZ00", (yt as ResolvedMedia.YouTubeEmbed).videoId)
    assertEquals("https://img.youtube.com/vi/abc123XYZ00/hqdefault.jpg", yt.thumbnailUrl)

    val hls = MediaUrlResolver.resolve("https://cdn.example.com/live/stream.m3u8")
    assertTrue(hls is ResolvedMedia.DirectStream)
    assertEquals("application/x-mpegURL", (hls as ResolvedMedia.DirectStream).mimeType)

    val photo = MediaUrlResolver.resolve("https://example.com/pic.jpg?w=800")
    assertTrue(photo is ResolvedMedia.ImageMedia)
  }
}
