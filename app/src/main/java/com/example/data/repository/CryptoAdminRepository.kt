package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAlert
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.PublicUser
import com.example.data.model.StoryItem
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

/**
 * Exclusive administrative control repository for verified admin account dhddu211@gmail.com.
 */
class CryptoAdminRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  fun observeAllVideosAdmin(): Flow<List<VideoPost>> {
    val path = "videos"
    return db
      .collection(path)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(VideoPost::class.java, ServerTimestampBehavior.ESTIMATE)
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

  fun observeAllStoriesAdmin(): Flow<List<StoryItem>> {
    val path = "stories"
    return db
      .collection(path)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(StoryItem::class.java, ServerTimestampBehavior.ESTIMATE)
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

  fun observeAllUsersAdmin(): Flow<List<PublicUser>> {
    val path = "users_public"
    return db
      .collection(path)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE)
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

  fun observeAllChatsAdmin(): Flow<List<ChatThread>> {
    val path = "chats"
    return db
      .collection(path)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(ChatThread::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeChatMessagesAdmin(chatId: String): Flow<List<ChatMessage>> {
    val path = "chats/$chatId/messages"
    return db
      .collection("chats")
      .document(chatId)
      .collection("messages")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeAllAlertsAdmin(): Flow<List<AdminAlert>> {
    val path = "admin_alerts"
    return db
      .collection(path)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(AdminAlert::class.java, ServerTimestampBehavior.ESTIMATE)
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

  suspend fun setVideoBanned(videoId: String, banned: Boolean, reason: String): Result<Unit> {
    val ref = db.collection("videos").document(videoId)
    return try {
      ref
        .update(
          mapOf(
            "isBanned" to banned,
            "banReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun setStoryBanned(storyId: String, banned: Boolean, reason: String): Result<Unit> {
    val ref = db.collection("stories").document(storyId)
    return try {
      ref
        .update(
          mapOf(
            "isBanned" to banned,
            "banReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun setUserBanned(userId: String, banned: Boolean, reason: String): Result<Unit> {
    val ref = db.collection("users_public").document(userId)
    return try {
      ref
        .update(
          mapOf(
            "isBanned" to banned,
            "banReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun setUserVerified(userId: String, verified: Boolean): Result<Unit> {
    val ref = db.collection("users_public").document(userId)
    return try {
      ref
        .update(
          mapOf(
            "isVerified" to verified,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun setChatBanned(chatId: String, banned: Boolean, reason: String): Result<Unit> {
    val ref = db.collection("chats").document(chatId)
    return try {
      ref
        .update(
          mapOf(
            "isBanned" to banned,
            "banReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun sendDirectAlert(
    targetUserId: String,
    title: String,
    message: String,
    severity: String = "warning",
  ): Result<String> {
    val adminEmail = auth.currentUser?.email ?: "dhddu211@gmail.com"
    val alertId = "alert_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val ref = db.collection("admin_alerts").document(alertId)
    return try {
      ref
        .set(
          mapOf(
            "targetUserId" to targetUserId,
            "title" to title.trim().take(120),
            "message" to message.trim().take(1000),
            "severity" to severity,
            "sentByEmail" to adminEmail,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(alertId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ref.path)
      Result.failure(e)
    }
  }
}
