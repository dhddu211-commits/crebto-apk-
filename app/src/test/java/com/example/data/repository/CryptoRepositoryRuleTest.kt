package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PublicUser
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun observePublicVideos_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = CryptoFeedRepository(firestore, auth)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublicVideos().first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = checkNotNull(thrown?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $thrown"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun publishVideoAndAnimatedProfile_authenticatedOwner_succeedsAndEmits() = runBlocking {
    val uid = signInTestUser("creator_${UUID.randomUUID()}@test.com")
    val socialRepo = CryptoSocialRepository(firestore, auth)
    val feedRepo = CryptoFeedRepository(firestore, auth)

    val user = checkNotNull(auth.currentUser)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { socialRepo.ensureUserProfileExists(user, "ar").isSuccess })

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        socialRepo
          .updatePublicProfile(
            displayName = "Crypto King",
            username = "cryptoking_${uid.take(4)}",
            bio = "Video creator",
            avatarUrl = "https://example.com/avatar.mp4",
            avatarMediaType = "video",
            bannerUrl = "https://example.com/banner.mp4",
            bannerMediaType = "video",
          )
          .isSuccess
      }
    )

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) {
        socialRepo.observeCurrentUserPublic(uid).first { it != null && it.avatarMediaType == "video" }
      }
    assertEquals("video", profile?.bannerMediaType)

    val publishRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        feedRepo.publishVideo(
          author = profile ?: PublicUser(userId = uid, displayName = "Crypto King"),
          videoType = "reel",
          title = "Viral Crypto Launch",
          description = "New upload",
          tags = listOf("crypto", "reels"),
          category = "Tech",
          mediaUrl = "https://example.com/reel.mp4",
          thumbnailUrl = "",
          durationSeconds = 20L,
        )
      }
    assertTrue(publishRes.isSuccess)
    val videoId = publishRes.getOrThrow()

    val videos =
      withTimeout(FLOW_TIMEOUT_MS) {
        feedRepo.observePublicVideos().first { list -> list.any { it.id == videoId } }
      }
    assertTrue(videos.any { it.id == videoId && it.title == "Viral Crypto Launch" })
  }

  @Test
  fun nonAdminUser_banVideoAndReadOtherPrivatePii_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val socialRepo = CryptoSocialRepository(firestore, auth)
    val feedRepo = CryptoFeedRepository(firestore, auth)
    val aliceUser = checkNotNull(auth.currentUser)
    withTimeout(DEFAULT_TIMEOUT_MS) { socialRepo.ensureUserProfileExists(aliceUser, "ar").getOrThrow() }

    val videoId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        feedRepo
          .publishVideo(
            author = PublicUser(userId = aliceUid, displayName = "Alice"),
            videoType = "long",
            title = "Alice Long Video",
            description = "Desc",
            tags = emptyList(),
            category = "Crypto",
            mediaUrl = "https://example.com/long.mp4",
            thumbnailUrl = "",
            durationSeconds = 120L,
          )
          .getOrThrow()
      }

    // Switch to Bob (non-admin)
    signInTestUser("bob_${UUID.randomUUID()}@test.com")
    val adminRepo = CryptoAdminRepository(firestore, auth)
    val banRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.setVideoBanned(videoId, true, "Attempt") }
    assertTrue(banRes.isFailure)
    val ex = checkNotNull(banRes.exceptionOrNull()?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.code)

    // Bob cannot read Alice's private PII
    val bobSocialRepo = CryptoSocialRepository(firestore, auth)
    var piiError: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobSocialRepo.observePrivateUser(aliceUid).first() }
    } catch (e: Exception) {
      piiError = e
    }
    assertNotNull(piiError)
    val piiFsEx = checkNotNull(piiError?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, piiFsEx.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
