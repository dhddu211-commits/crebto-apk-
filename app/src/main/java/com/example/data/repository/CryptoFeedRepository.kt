package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PublicUser
import com.example.data.model.RepostRecord
import com.example.data.model.StoryItem
import com.example.data.model.VideoComment
import com.example.data.model.VideoInteraction
import com.example.data.model.VideoPost
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CryptoFeedRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observePublicVideos(): Flow<List<VideoPost>> {
    val path = "videos"
    return db
      .collection(path)
      .whereEqualTo("isBanned", false)
      .snapshots()
      .map { snap ->
        snap.toObjects(VideoPost::class.java, ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeActiveStories(): Flow<List<StoryItem>> {
    val path = "stories"
    return db
      .collection(path)
      .whereEqualTo("isBanned", false)
      .snapshots()
      .map { snap ->
        val now = System.currentTimeMillis()
        snap
          .toObjects(StoryItem::class.java, ServerTimestampBehavior.ESTIMATE)
          .filter { it.expiresAtMillis > now }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeComments(videoId: String): Flow<List<VideoComment>> {
    val path = "videos/$videoId/comments"
    return db
      .collection("videos")
      .document(videoId)
      .collection("comments")
      .whereEqualTo("videoId", videoId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(VideoComment::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeLikedVideoIds(userId: String): Flow<Set<String>> {
    val path = "likes"
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(VideoInteraction::class.java).mapTo(mutableSetOf()) { it.videoId }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeFavoriteVideoIds(userId: String): Flow<Set<String>> {
    val path = "favorites"
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(VideoInteraction::class.java).mapTo(mutableSetOf()) { it.videoId }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeRepostedVideoIds(userId: String): Flow<Set<String>> {
    val path = "reposts"
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snap ->
        snap.toObjects(RepostRecord::class.java).mapTo(mutableSetOf()) { it.videoId }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun publishVideo(
    author: PublicUser,
    videoType: String,
    title: String,
    description: String,
    tags: List<String>,
    category: String,
    mediaUrl: String,
    thumbnailUrl: String,
    durationSeconds: Long,
  ): Result<String> {
    val uid = requireUserId()
    val docId = "vid_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("videos").document(docId)
    val sanitizedTags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.take(10)
    val payload =
      mapOf(
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Creator" }.take(80),
        "authorAvatarUrl" to author.avatarUrl.take(1000),
        "authorAvatarType" to author.avatarMediaType,
        "videoType" to videoType,
        "title" to title.trim().take(150),
        "description" to description.trim().take(1000),
        "tags" to sanitizedTags,
        "category" to category.trim().ifBlank { "Crypto" }.take(50),
        "mediaUrl" to mediaUrl.trim().take(1500),
        "thumbnailUrl" to thumbnailUrl.trim().take(1500),
        "durationSeconds" to durationSeconds.coerceAtLeast(0L),
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "repostsCount" to 0L,
        "favoritesCount" to 0L,
        "viewsCount" to 0L,
        "viralBoostScore" to 2500L,
        "isBanned" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun publishStory(
    author: PublicUser,
    mediaUrl: String,
    mediaType: String,
    caption: String,
  ): Result<String> {
    val uid = requireUserId()
    val docId = "story_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("stories").document(docId)
    val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
    val payload =
      mapOf(
        "authorId" to uid,
        "authorName" to author.displayName.ifBlank { "Creator" }.take(80),
        "authorAvatarUrl" to author.avatarUrl.take(1000),
        "authorAvatarType" to author.avatarMediaType,
        "mediaUrl" to mediaUrl.trim().take(1500),
        "mediaType" to mediaType,
        "caption" to caption.trim().take(300),
        "viewsCount" to 0L,
        "isBanned" to false,
        "expiresAtMillis" to expiresAt,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordVideoView(videoId: String) {
    requireUserId()
    val videoRef = db.collection("videos").document(videoId)
    try {
      videoRef
        .update(
          mapOf(
            "viewsCount" to FieldValue.increment(1L),
            "viralBoostScore" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
    }
  }

  suspend fun recordStoryView(storyId: String) {
    requireUserId()
    val storyRef = db.collection("stories").document(storyId)
    try {
      storyRef
        .update(
          mapOf(
            "viewsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
    }
  }

  suspend fun toggleLike(video: VideoPost): Result<Boolean> {
    val uid = requireUserId()
    val likeId = "${uid}_${video.id}"
    val likeRef = db.collection("likes").document(likeId)
    val videoRef = db.collection("videos").document(video.id)
    val authorRef = db.collection("users_public").document(video.authorId)
    return try {
      val nowLiked =
        db
          .runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            val videoSnap = tx.get(videoRef)
            val authorSnap = tx.get(authorRef)
            val currentLikes = videoSnap.getLong("likesCount") ?: 0L
            val currentBoost = videoSnap.getLong("viralBoostScore") ?: 0L
            val currentAuthorLikes = authorSnap.getLong("likesCount") ?: 0L

            if (likeSnap.exists()) {
              tx.delete(likeRef)
              tx.update(
                videoRef,
                mapOf(
                  "likesCount" to (currentLikes - 1L).coerceAtLeast(0L),
                  "viralBoostScore" to (currentBoost - 2L).coerceAtLeast(0L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              if (authorSnap.exists() && currentAuthorLikes > 0L) {
                tx.update(
                  authorRef,
                  mapOf(
                    "likesCount" to (currentAuthorLikes - 1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              false
            } else {
              tx.set(
                likeRef,
                mapOf(
                  "userId" to uid,
                  "videoId" to video.id,
                  "createdAt" to FieldValue.serverTimestamp(),
                ),
              )
              tx.update(
                videoRef,
                mapOf(
                  "likesCount" to currentLikes + 1L,
                  "viralBoostScore" to currentBoost + 3L,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              if (authorSnap.exists()) {
                tx.update(
                  authorRef,
                  mapOf(
                    "likesCount" to currentAuthorLikes + 1L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              true
            }
          }
          .await()
      Result.success(nowLiked)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, likeRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleFavorite(video: VideoPost): Result<Boolean> {
    val uid = requireUserId()
    val favId = "${uid}_${video.id}"
    val favRef = db.collection("favorites").document(favId)
    val videoRef = db.collection("videos").document(video.id)
    return try {
      val nowFavorited =
        db
          .runTransaction { tx ->
            val favSnap = tx.get(favRef)
            val videoSnap = tx.get(videoRef)
            val currentCount = videoSnap.getLong("favoritesCount") ?: 0L
            val currentBoost = videoSnap.getLong("viralBoostScore") ?: 0L
            if (favSnap.exists()) {
              tx.delete(favRef)
              tx.update(
                videoRef,
                mapOf(
                  "favoritesCount" to (currentCount - 1L).coerceAtLeast(0L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              false
            } else {
              tx.set(
                favRef,
                mapOf(
                  "userId" to uid,
                  "videoId" to video.id,
                  "createdAt" to FieldValue.serverTimestamp(),
                ),
              )
              tx.update(
                videoRef,
                mapOf(
                  "favoritesCount" to currentCount + 1L,
                  "viralBoostScore" to currentBoost + 4L,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              true
            }
          }
          .await()
      Result.success(nowFavorited)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, favRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleRepost(video: VideoPost): Result<Boolean> {
    val uid = requireUserId()
    val repostId = "${uid}_${video.id}"
    val repostRef = db.collection("reposts").document(repostId)
    val videoRef = db.collection("videos").document(video.id)
    return try {
      val nowReposted =
        db
          .runTransaction { tx ->
            val snap = tx.get(repostRef)
            val videoSnap = tx.get(videoRef)
            val currentCount = videoSnap.getLong("repostsCount") ?: 0L
            val currentBoost = videoSnap.getLong("viralBoostScore") ?: 0L
            if (snap.exists()) {
              tx.delete(repostRef)
              tx.update(
                videoRef,
                mapOf(
                  "repostsCount" to (currentCount - 1L).coerceAtLeast(0L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              false
            } else {
              tx.set(
                repostRef,
                mapOf(
                  "userId" to uid,
                  "videoId" to video.id,
                  "isPublic" to true,
                  "createdAt" to FieldValue.serverTimestamp(),
                ),
              )
              tx.update(
                videoRef,
                mapOf(
                  "repostsCount" to currentCount + 1L,
                  "viralBoostScore" to currentBoost + 6L,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
              true
            }
          }
          .await()
      Result.success(nowReposted)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, repostRef.path)
      Result.failure(e)
    }
  }

  suspend fun addComment(videoId: String, author: PublicUser, text: String): Result<Unit> {
    val uid = requireUserId()
    val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val videoRef = db.collection("videos").document(videoId)
    val commentRef = videoRef.collection("comments").document(commentId)
    return try {
      db
        .runTransaction { tx ->
          val videoSnap = tx.get(videoRef)
          val currentComments = videoSnap.getLong("commentsCount") ?: 0L
          val currentBoost = videoSnap.getLong("viralBoostScore") ?: 0L
          tx.set(
            commentRef,
            mapOf(
              "videoId" to videoId,
              "authorId" to uid,
              "authorName" to author.displayName.ifBlank { "User" }.take(80),
              "authorAvatarUrl" to author.avatarUrl.take(1000),
              "text" to text.trim().take(500),
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
          tx.update(
            videoRef,
            mapOf(
              "commentsCount" to currentComments + 1L,
              "viralBoostScore" to currentBoost + 4L,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path)
      Result.failure(e)
    }
  }
}
