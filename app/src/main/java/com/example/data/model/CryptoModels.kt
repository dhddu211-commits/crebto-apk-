package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class AppLanguage(val code: String) {
  AR("ar"),
  EN("en"),
}

@IgnoreExtraProperties
data class PublicUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val username: String = "",
  val bio: String = "",
  val avatarUrl: String = "",
  val avatarMediaType: String = "image", // "image" | "video"
  val bannerUrl: String = "",
  val bannerMediaType: String = "image", // "image" | "video"
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val likesCount: Long = 0L,
  val isBanned: Boolean = false,
  val banReason: String = "",
  val isVerified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PrivateUser(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val phoneNumber: String = "",
  val preferredLanguage: String = "ar",
  val isBanned: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VideoPost(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatarUrl: String = "",
  val authorAvatarType: String = "image",
  val videoType: String = "reel", // "reel" | "long"
  val title: String = "",
  val description: String = "",
  val tags: List<String> = emptyList(),
  val category: String = "Crypto",
  val mediaUrl: String = "",
  val thumbnailUrl: String = "",
  val durationSeconds: Long = 0L,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val repostsCount: Long = 0L,
  val favoritesCount: Long = 0L,
  val viewsCount: Long = 0L,
  val viralBoostScore: Long = 1000L,
  val isBanned: Boolean = false,
  val banReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VideoComment(
  @DocumentId val id: String = "",
  val videoId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatarUrl: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StoryItem(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val authorAvatarUrl: String = "",
  val authorAvatarType: String = "image",
  val mediaUrl: String = "",
  val mediaType: String = "image", // "image" | "video"
  val caption: String = "",
  val viewsCount: Long = 0L,
  val isBanned: Boolean = false,
  val banReason: String = "",
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VideoInteraction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RepostRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val videoId: String = "",
  val isPublic: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FollowRecord(
  @DocumentId val id: String = "",
  val followerId: String = "",
  val followingId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatThread(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participant1Name: String = "",
  val participant2Name: String = "",
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val isBanned: Boolean = false,
  val banReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val participantIds: List<String> = emptyList(),
  val senderId: String = "",
  val senderName: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminAlert(
  @DocumentId val id: String = "",
  val targetUserId: String = "",
  val title: String = "",
  val message: String = "",
  val severity: String = "warning", // "warning" | "critical" | "ban_notice"
  val sentByEmail: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
