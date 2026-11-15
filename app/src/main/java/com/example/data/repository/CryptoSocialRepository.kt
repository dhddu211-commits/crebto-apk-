package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminAlert
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRecord
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

class CryptoSocialRepository(
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

  suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser, preferredLanguage: String): Result<Unit> {
    val uid = firebaseUser.uid
    val publicRef = db.collection("users_public").document(uid)
    val privateRef = db.collection("users_private").document(uid)
    return try {
      val publicSnap = publicRef.get().await()
      if (!publicSnap.exists()) {
        val rawName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Crypto Creator"
        val cleanHandle =
          (firebaseUser.email?.substringBefore("@") ?: "user_${uid.take(6)}")
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .let { if (it.length < 2) "user_${uid.take(6)}" else it.take(32) }
        publicRef
          .set(
            mapOf(
              "userId" to uid,
              "displayName" to rawName.take(80),
              "username" to cleanHandle,
              "bio" to "",
              "avatarUrl" to (firebaseUser.photoUrl?.toString()?.take(1000) ?: ""),
              "avatarMediaType" to "image",
              "bannerUrl" to "",
              "bannerMediaType" to "image",
              "followersCount" to 0L,
              "followingCount" to 0L,
              "likesCount" to 0L,
              "isBanned" to false,
              "isVerified" to false,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      val privateSnap = privateRef.get().await()
      if (!privateSnap.exists()) {
        privateRef
          .set(
            mapOf(
              "userId" to uid,
              "email" to (firebaseUser.email ?: ""),
              "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
              "preferredLanguage" to preferredLanguage,
              "isBanned" to false,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, publicRef.path)
      Result.failure(e)
    }
  }

  fun observePublicUsers(): Flow<List<PublicUser>> {
    val path = "users_public"
    return db
      .collection(path)
      .whereEqualTo("isBanned", false)
      .snapshots()
      .map { snap -> snap.toObjects(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeCurrentUserPublic(userId: String): Flow<PublicUser?> {
    val path = "users_public/$userId"
    return db
      .collection("users_public")
      .document(userId)
      .snapshots()
      .map { snap -> snap.toObject(PublicUser::class.java, ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        throw error
      }
  }

  fun observePrivateUser(userId: String): Flow<PrivateUser?> {
    val path = "users_private/$userId"
    return db
      .collection("users_private")
      .document(userId)
      .snapshots()
      .map { snap -> snap.toObject(PrivateUser::class.java, ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun updatePublicProfile(
    displayName: String,
    username: String,
    bio: String,
    avatarUrl: String,
    avatarMediaType: String,
    bannerUrl: String,
    bannerMediaType: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val ref = db.collection("users_public").document(uid)
    return try {
      ref
        .update(
          mapOf(
            "displayName" to displayName.trim().take(80),
            "username" to username.trim().take(40),
            "bio" to bio.trim().take(300),
            "avatarUrl" to avatarUrl.trim().take(1000),
            "avatarMediaType" to avatarMediaType,
            "bannerUrl" to bannerUrl.trim().take(1000),
            "bannerMediaType" to bannerMediaType,
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

  suspend fun updatePrivateContactInfo(
    email: String,
    phoneNumber: String,
    preferredLanguage: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val ref = db.collection("users_private").document(uid)
    return try {
      ref
        .update(
          mapOf(
            "email" to email.trim().take(200),
            "phoneNumber" to phoneNumber.trim().take(40),
            "preferredLanguage" to preferredLanguage,
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

  fun observeFollowingIds(userId: String): Flow<Set<String>> {
    val path = "follows"
    return db
      .collection(path)
      .whereEqualTo("followerId", userId)
      .snapshots()
      .map { snap -> snap.toObjects(FollowRecord::class.java).mapTo(mutableSetOf()) { it.followingId } }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun toggleFollow(targetUserId: String): Result<Boolean> {
    val uid = requireUserId()
    if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot follow self"))
    val followId = "${uid}_${targetUserId}"
    val followRef = db.collection("follows").document(followId)
    val myRef = db.collection("users_public").document(uid)
    val targetRef = db.collection("users_public").document(targetUserId)

    return try {
      val nowFollowing =
        db
          .runTransaction { tx ->
            val followSnap = tx.get(followRef)
            val mySnap = tx.get(myRef)
            val targetSnap = tx.get(targetRef)
            val myFollowing = mySnap.getLong("followingCount") ?: 0L
            val targetFollowers = targetSnap.getLong("followersCount") ?: 0L

            if (followSnap.exists()) {
              tx.delete(followRef)
              if (mySnap.exists() && myFollowing > 0L) {
                tx.update(
                  myRef,
                  mapOf(
                    "followingCount" to myFollowing - 1L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              if (targetSnap.exists() && targetFollowers > 0L) {
                tx.update(
                  targetRef,
                  mapOf(
                    "followersCount" to targetFollowers - 1L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              false
            } else {
              tx.set(
                followRef,
                mapOf(
                  "followerId" to uid,
                  "followingId" to targetUserId,
                  "createdAt" to FieldValue.serverTimestamp(),
                ),
              )
              if (mySnap.exists()) {
                tx.update(
                  myRef,
                  mapOf(
                    "followingCount" to myFollowing + 1L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              if (targetSnap.exists()) {
                tx.update(
                  targetRef,
                  mapOf(
                    "followersCount" to targetFollowers + 1L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                  ),
                )
              }
              true
            }
          }
          .await()
      Result.success(nowFollowing)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, followRef.path)
      Result.failure(e)
    }
  }

  fun observeUserChats(userId: String): Flow<List<ChatThread>> {
    val path = "chats"
    return db
      .collection(path)
      .whereArrayContains("participantIds", userId)
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

  fun observeChatMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> {
    val path = "chats/$chatId/messages"
    return db
      .collection("chats")
      .document(chatId)
      .collection("messages")
      .whereArrayContains("participantIds", currentUserId)
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

  suspend fun openOrCreateChat(currentUser: PublicUser, peerUser: PublicUser): Result<String> {
    val uid = requireUserId()
    val sortedIds = listOf(uid, peerUser.userId).sorted()
    val chatId = sortedIds.joinToString("_")
    val chatRef = db.collection("chats").document(chatId)
    return try {
      val snap = chatRef.get().await()
      if (!snap.exists()) {
        val p1Name = if (sortedIds[0] == uid) currentUser.displayName else peerUser.displayName
        val p2Name = if (sortedIds[1] == uid) currentUser.displayName else peerUser.displayName
        chatRef
          .set(
            mapOf(
              "participantIds" to sortedIds,
              "participant1Name" to p1Name.ifBlank { "User 1" }.take(80),
              "participant2Name" to p2Name.ifBlank { "User 2" }.take(80),
              "lastMessage" to "",
              "lastSenderId" to uid,
              "isBanned" to false,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, chatRef.path)
      Result.failure(e)
    }
  }

  suspend fun sendMessage(
    chatThread: ChatThread,
    sender: PublicUser,
    text: String,
  ): Result<Unit> {
    val uid = requireUserId()
    if (chatThread.isBanned) {
      return Result.failure(IllegalStateException("This chat has been banned by administration."))
    }
    val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val chatRef = db.collection("chats").document(chatThread.id)
    val msgRef = chatRef.collection("messages").document(messageId)
    return try {
      val batch = db.batch()
      batch.set(
        msgRef,
        mapOf(
          "chatId" to chatThread.id,
          "participantIds" to chatThread.participantIds,
          "senderId" to uid,
          "senderName" to sender.displayName.ifBlank { "User" }.take(80),
          "text" to text.trim().take(1000),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        chatRef,
        mapOf(
          "lastMessage" to text.trim().take(500),
          "lastSenderId" to uid,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      Result.failure(e)
    }
  }

  fun observeUserAlerts(userId: String): Flow<List<AdminAlert>> {
    val path = "admin_alerts"
    return db
      .collection(path)
      .whereEqualTo("targetUserId", userId)
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

  suspend fun markAlertRead(alertId: String) {
    requireUserId()
    val ref = db.collection("admin_alerts").document(alertId)
    try {
      ref
        .update(
          mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
    }
  }
}
