package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminAlert
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.PublicUser
import com.example.data.model.StoryItem
import com.example.data.model.VideoPost
import com.example.data.repository.CryptoAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AdminViewModel(
  private val adminRepository: CryptoAdminRepository,
) : ViewModel() {

  val allVideos: StateFlow<List<VideoPost>> =
    adminRepository
      .observeAllVideosAdmin()
      .catch { error ->
        Log.w(TAG, "Admin observe videos error", error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val allStories: StateFlow<List<StoryItem>> =
    adminRepository
      .observeAllStoriesAdmin()
      .catch { error ->
        Log.w(TAG, "Admin observe stories error", error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val allUsers: StateFlow<List<PublicUser>> =
    adminRepository
      .observeAllUsersAdmin()
      .catch { error ->
        Log.w(TAG, "Admin observe users error", error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val allChats: StateFlow<List<ChatThread>> =
    adminRepository
      .observeAllChatsAdmin()
      .catch { error ->
        Log.w(TAG, "Admin observe chats error", error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val allAlerts: StateFlow<List<AdminAlert>> =
    adminRepository
      .observeAllAlertsAdmin()
      .catch { error ->
        Log.w(TAG, "Admin observe alerts error", error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val _inspectedChatId = MutableStateFlow<String?>(null)
  val inspectedChatId: StateFlow<String?> = _inspectedChatId.asStateFlow()

  val inspectedChatMessages: StateFlow<List<ChatMessage>> =
    _inspectedChatId
      .flatMapLatest { chatId ->
        if (chatId.isNullOrBlank()) flowOf(emptyList())
        else adminRepository.observeChatMessagesAdmin(chatId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  fun inspectChat(chatId: String?) {
    _inspectedChatId.value = chatId
  }

  fun toggleVideoBan(video: VideoPost, reason: String) {
    viewModelScope.launch {
      adminRepository.setVideoBanned(video.id, !video.isBanned, reason)
    }
  }

  fun toggleStoryBan(story: StoryItem, reason: String) {
    viewModelScope.launch {
      adminRepository.setStoryBanned(story.id, !story.isBanned, reason)
    }
  }

  fun toggleUserBan(user: PublicUser, reason: String) {
    viewModelScope.launch {
      adminRepository.setUserBanned(user.userId, !user.isBanned, reason)
    }
  }

  fun toggleUserVerified(user: PublicUser) {
    viewModelScope.launch {
      adminRepository.setUserVerified(user.userId, !user.isVerified)
    }
  }

  fun toggleChatBan(chat: ChatThread, reason: String) {
    viewModelScope.launch {
      adminRepository.setChatBanned(chat.id, !chat.isBanned, reason)
    }
  }

  fun sendDirectAlert(
    targetUserId: String,
    title: String,
    message: String,
    severity: String,
    onDone: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val res = adminRepository.sendDirectAlert(targetUserId, title, message, severity)
      onDone(res.isSuccess)
    }
  }

  private companion object {
    const val TAG = "AdminVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
