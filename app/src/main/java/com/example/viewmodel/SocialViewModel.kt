package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminAlert
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.data.repository.CryptoSocialRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SocialViewModel(
  private val socialRepository: CryptoSocialRepository,
  private val currentUserId: String,
) : ViewModel() {

  val currentUserPublic: StateFlow<PublicUser?> =
    socialRepository
      .observeCurrentUserPublic(currentUserId)
      .catch { error ->
        Log.w(TAG, "Error observing current public profile", error)
        emit(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val currentUserPrivate: StateFlow<PrivateUser?> =
    socialRepository
      .observePrivateUser(currentUserId)
      .catch { error ->
        Log.w(TAG, "Error observing private info", error)
        emit(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val publicUsersState: StateFlow<UiState<List<PublicUser>>> =
    socialRepository
      .observePublicUsers()
      .map<List<PublicUser>, UiState<List<PublicUser>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load creators"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val followingIds: StateFlow<Set<String>> =
    socialRepository
      .observeFollowingIds(currentUserId)
      .catch { emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val userChats: StateFlow<List<ChatThread>> =
    socialRepository
      .observeUserChats(currentUserId)
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val _activeChatId = MutableStateFlow<String?>(null)
  val activeChatMessages: StateFlow<List<ChatMessage>> =
    _activeChatId
      .flatMapLatest { chatId ->
        if (chatId.isNullOrBlank()) flowOf(emptyList())
        else socialRepository.observeChatMessages(chatId, currentUserId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val userAlerts: StateFlow<List<AdminAlert>> =
    socialRepository
      .observeUserAlerts(currentUserId)
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  fun ensureInitialized(firebaseUser: FirebaseUser, preferredLang: String) {
    viewModelScope.launch { socialRepository.ensureUserProfileExists(firebaseUser, preferredLang) }
  }

  fun toggleFollow(targetUserId: String) {
    viewModelScope.launch { socialRepository.toggleFollow(targetUserId) }
  }

  fun updateProfile(
    displayName: String,
    username: String,
    bio: String,
    avatarUrl: String,
    avatarMediaType: String,
    bannerUrl: String,
    bannerMediaType: String,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val res =
        socialRepository.updatePublicProfile(
          displayName = displayName,
          username = username,
          bio = bio,
          avatarUrl = avatarUrl,
          avatarMediaType = avatarMediaType,
          bannerUrl = bannerUrl,
          bannerMediaType = bannerMediaType,
        )
      onComplete(res.isSuccess)
    }
  }

  fun updateContactInfo(
    email: String,
    phoneNumber: String,
    preferredLanguage: String,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val res = socialRepository.updatePrivateContactInfo(email, phoneNumber, preferredLanguage)
      onComplete(res.isSuccess)
    }
  }

  fun selectChat(chatId: String?) {
    _activeChatId.value = chatId
  }

  fun openChatWith(peerUser: PublicUser, onOpened: (String) -> Unit) {
    val me =
      currentUserPublic.value
        ?: PublicUser(userId = currentUserId, displayName = "Creator")
    viewModelScope.launch {
      socialRepository.openOrCreateChat(me, peerUser).onSuccess { chatId ->
        _activeChatId.value = chatId
        onOpened(chatId)
      }
    }
  }

  fun sendMessage(chatThread: ChatThread, text: String, onError: (String) -> Unit) {
    if (text.isBlank()) return
    val me =
      currentUserPublic.value
        ?: PublicUser(userId = currentUserId, displayName = "Creator")
    viewModelScope.launch {
      socialRepository.sendMessage(chatThread, me, text).onFailure {
        onError(it.localizedMessage ?: "Failed to send message")
      }
    }
  }

  fun markAlertRead(alertId: String) {
    viewModelScope.launch { socialRepository.markAlertRead(alertId) }
  }

  private companion object {
    const val TAG = "SocialVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
