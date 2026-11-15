package com.example.data.util

import com.example.data.model.VideoPost
import java.util.concurrent.TimeUnit

/**
 * Rapid-Exposure Discovery Algorithm ("Viral Boost"):
 * Guarantees that every newly published video receives a substantial initial discovery boost
 * so it surfaces prominently across users' feeds to accelerate creator growth and fame.
 */
object ViralBoostRanker {

  private const val NEW_VIDEO_WINDOW_HOURS = 48L
  private const val INITIAL_EXPOSURE_VIEWS_CAP = 500L

  fun computeDiscoveryScore(video: VideoPost, nowMillis: Long = System.currentTimeMillis()): Double {
    val createdMillis = video.createdAt?.toDate()?.time ?: nowMillis
    val ageHours = TimeUnit.MILLISECONDS.toHours((nowMillis - createdMillis).coerceAtLeast(0L))

    // Strong freshness boost for every newly uploaded video to surface across feeds immediately
    val freshnessBoost =
      when {
        ageHours < 1L -> 3500.0
        ageHours < 6L -> 2800.0
        ageHours < 24L -> 2000.0
        ageHours <= NEW_VIDEO_WINDOW_HOURS -> 1200.0
        else -> 250.0 / (1.0 + (ageHours - NEW_VIDEO_WINDOW_HOURS) * 0.05)
      }

    // Extra rapid-discovery guarantee for videos still building initial reach
    val lowReachMultiplier = if (video.viewsCount < INITIAL_EXPOSURE_VIEWS_CAP) 1.35 else 1.0

    val engagementScore =
      (video.likesCount * 3.0) +
        (video.commentsCount * 4.5) +
        (video.repostsCount * 6.0) +
        (video.favoritesCount * 4.0) +
        (video.viewsCount * 0.4)

    return (freshnessBoost + engagementScore + video.viralBoostScore * 0.5) * lowReachMultiplier
  }

  fun isFreshViralBoostActive(video: VideoPost, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val createdMillis = video.createdAt?.toDate()?.time ?: nowMillis
    val ageHours = TimeUnit.MILLISECONDS.toHours((nowMillis - createdMillis).coerceAtLeast(0L))
    return ageHours <= NEW_VIDEO_WINDOW_HOURS || video.viewsCount < INITIAL_EXPOSURE_VIEWS_CAP
  }

  fun rankFeed(videos: List<VideoPost>, nowMillis: Long = System.currentTimeMillis()): List<VideoPost> {
    return videos.sortedByDescending { computeDiscoveryScore(it, nowMillis) }
  }
}
