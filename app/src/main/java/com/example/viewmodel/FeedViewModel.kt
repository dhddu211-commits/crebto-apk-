package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PublicUser
import com.example.data.model.StoryItem
import com.example.data.model.UiState
import com.example.data.model.VideoComment
import com.example.data.model.VideoPost
import com.example.data.repository.CryptoFeedRepository
import com.example.data.util.ViralBoostRanker
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FeedViewModel(
  private val feedRepository: CryptoFeedRepository,
  private val currentUserId: String,
) : ViewModel() {

  val rankedVideosState: StateFlow<UiState<List<VideoPost>>> =
    feedRepository
      .observePublicVideos()
      .map<List<VideoPost>, UiState<List<VideoPost>>> { videos ->
        UiState.Success(ViralBoostRanker.rankFeed(videos))
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing ranked videos", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load feed"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val storiesState: StateFlow<UiState<List<StoryItem>>> =
    feedRepository
      .observeActiveStories()
      .map<List<StoryItem>, UiState<List<StoryItem>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing stories", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load stories"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val likedVideoIds: StateFlow<Set<String>> =
    feedRepository
      .observeLikedVideoIds(currentUserId)
      .catch { emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val favoriteVideoIds: StateFlow<Set<String>> =
    feedRepository
      .observeFavoriteVideoIds(currentUserId)
      .catch { emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val repostedVideoIds: StateFlow<Set<String>> =
    feedRepository
      .observeRepostedVideoIds(currentUserId)
      .catch { emit(emptySet()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  private val _selectedVideoForComments = MutableStateFlow<String?>(null)
  val activeComments: StateFlow<List<VideoComment>> =
    _selectedVideoForComments
      .flatMapLatest { videoId ->
        if (videoId.isNullOrBlank()) flowOf(emptyList())
        else feedRepository.observeComments(videoId).catch { emit(emptyList()) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow("ALL")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  val filteredVideos: StateFlow<List<VideoPost>> =
    combine(rankedVideosState, _searchQuery, _selectedCategory) { state, query, category ->
      val all = (state as? UiState.Success)?.data.orEmpty()
      val q = query.trim().lowercase()
      all.filter { video ->
        val matchesCategory = category == "ALL" || video.category.equals(category, ignoreCase = true)
        val matchesQuery =
          q.isEmpty() ||
            video.title.lowercase().contains(q) ||
            video.description.lowercase().contains(q) ||
            video.authorName.lowercase().contains(q) ||
            video.tags.any { it.lowercase().contains(q) }
        matchesCategory && matchesQuery
      }
    }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val _actionStatus = MutableStateFlow<String?>(null)
  val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun updateSelectedCategory(category: String) {
    _selectedCategory.value = category
  }

  fun openCommentsForVideo(videoId: String?) {
    _selectedVideoForComments.value = videoId
  }

  fun clearActionStatus() {
    _actionStatus.value = null
  }

  fun toggleLike(video: VideoPost) {
    viewModelScope.launch { feedRepository.toggleLike(video) }
  }

  fun toggleFavorite(video: VideoPost) {
    viewModelScope.launch { feedRepository.toggleFavorite(video) }
  }

  fun toggleRepost(video: VideoPost) {
    viewModelScope.launch { feedRepository.toggleRepost(video) }
  }

  fun addComment(videoId: String, author: PublicUser, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch { feedRepository.addComment(videoId, author, text) }
  }

  fun recordView(videoId: String) {
    viewModelScope.launch { feedRepository.recordVideoView(videoId) }
  }

  fun recordStoryView(storyId: String) {
    viewModelScope.launch { feedRepository.recordStoryView(storyId) }
  }

  fun publishVideo(
    author: PublicUser,
    videoType: String,
    title: String,
    description: String,
    tags: List<String>,
    category: String,
    mediaUrl: String,
    thumbnailUrl: String,
    durationSeconds: Long,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val result =
        feedRepository.publishVideo(
          author = author,
          videoType = videoType,
          title = title,
          description = description,
          tags = tags,
          category = category,
          mediaUrl = mediaUrl,
          thumbnailUrl = thumbnailUrl,
          durationSeconds = durationSeconds,
        )
      result.onSuccess { onSuccess() }.onFailure { onError(it.localizedMessage ?: "Publish failed") }
    }
  }

  fun publishStory(
    author: PublicUser,
    mediaUrl: String,
    mediaType: String,
    caption: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val result = feedRepository.publishStory(author, mediaUrl, mediaType, caption)
      result.onSuccess { onSuccess() }.onFailure { onError(it.localizedMessage ?: "Story failed") }
    }
  }

  private companion object {
    const val TAG = "FeedVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
