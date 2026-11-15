package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PublicUser
import com.example.data.model.StoryItem
import com.example.data.model.UiState
import com.example.data.model.VideoComment
import com.example.data.model.VideoPost
import com.example.data.util.MediaUrlResolver
import com.example.data.util.ViralBoostRanker
import com.example.ui.components.AnimatedMediaAvatar
import com.example.ui.components.CryptoGoldShimmerCard
import com.example.ui.components.CryptoVideoPlayer
import com.example.ui.components.rememberCryptoImageLoader
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
  videosState: UiState<List<VideoPost>>,
  storiesState: UiState<List<StoryItem>>,
  currentUser: PublicUser?,
  likedIds: Set<String>,
  favoriteIds: Set<String>,
  repostedIds: Set<String>,
  followingIds: Set<String>,
  activeComments: List<VideoComment>,
  isAdmin: Boolean,
  isOnline: Boolean = true,
  onToggleLike: (VideoPost) -> Unit,
  onToggleFavorite: (VideoPost) -> Unit,
  onToggleRepost: (VideoPost) -> Unit,
  onToggleFollow: (String) -> Unit,
  onOpenComments: (String?) -> Unit,
  onAddComment: (String, String) -> Unit,
  onOpenVideoDetail: (VideoPost) -> Unit,
  onOpenCreatorProfile: (String) -> Unit,
  onCreateStoryClick: () -> Unit,
  onRecordView: (String) -> Unit,
  onRecordStoryView: (String) -> Unit,
  onAdminBanVideo: (VideoPost) -> Unit,
  onAdminBanStory: (StoryItem) -> Unit,
  onMessageCreator: (String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val context = LocalContext.current

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Reels, 1 = Long Videos
  var viewingStory by remember { mutableStateOf<StoryItem?>(null) }
  var commentsVideoId by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    // Top Brand Header + Live Online & Viral Indicators
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = strings.appName,
          style = MaterialTheme.typography.headlineMedium,
          color = ChampagneGold,
          fontWeight = FontWeight.Bold,
        )
        Surface(
          color = (if (isOnline) EmeraldLive else CrimsonBan).copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
          border =
            androidx.compose.foundation.BorderStroke(
              1.dp,
              (if (isOnline) EmeraldLive else CrimsonBan).copy(alpha = 0.5f),
            ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
              contentDescription = null,
              tint = if (isOnline) EmeraldLive else CrimsonBan,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isOnline) strings.onlineLiveBadge else "Offline",
              style = MaterialTheme.typography.labelSmall,
              color = if (isOnline) EmeraldLive else CrimsonBan,
            )
          }
        }
      }
      Surface(
        color = CharcoalGlass,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = MetallicGold,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = strings.viralBoostBadge,
            style = MaterialTheme.typography.labelMedium,
            color = ChampagneGold,
          )
        }
      }
    }

    if (!isOnline) {
      Surface(
        color = CrimsonBan.copy(alpha = 0.18f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(
          text = strings.offlineWarning,
          style = MaterialTheme.typography.labelMedium,
          color = CrimsonBan,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    }

    // Stories Horizontal Bar
    val stories = (storiesState as? UiState.Success)?.data.orEmpty()
    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      item {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable { onCreateStoryClick() }.testTag("add_story_button"),
        ) {
          Box(contentAlignment = Alignment.BottomEnd) {
            AnimatedMediaAvatar(
              mediaUrl = currentUser?.avatarUrl.orEmpty(),
              mediaType = currentUser?.avatarMediaType ?: "image",
              size = 58.dp,
              hasStoryRing = false,
            )
            Box(
              modifier =
                Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(MetallicGold)
                  .border(1.5.dp, ObsidianVoid, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = strings.addStory,
                tint = ObsidianVoid,
                modifier = Modifier.size(14.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = strings.addStory,
            style = MaterialTheme.typography.labelMedium,
            color = WarmIvory,
            maxLines = 1,
          )
        }
      }

      items(stories, key = { it.id }) { story ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier
              .width(68.dp)
              .clickable {
                viewingStory = story
                onRecordStoryView(story.id)
              }
              .testTag("story_item_${story.id}"),
        ) {
          AnimatedMediaAvatar(
            mediaUrl = story.authorAvatarUrl,
            mediaType = story.authorAvatarType,
            size = 58.dp,
            hasStoryRing = true,
            contentDescription = story.authorName,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = story.authorName,
            style = MaterialTheme.typography.labelMedium,
            color = WarmIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // Triple Mode Tabs: Reels (TikTok), Videos (YouTube), Photos
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MidnightOnyx,
      contentColor = ChampagneGold,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = MetallicGold,
        )
      },
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        modifier = Modifier.testTag("tab_reels"),
        text = { Text(strings.tabReels, fontWeight = FontWeight.Bold) },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        modifier = Modifier.testTag("tab_videos"),
        text = { Text(strings.tabVideos, fontWeight = FontWeight.Bold) },
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        modifier = Modifier.testTag("tab_photos"),
        text = { Text(strings.tabPhotos, fontWeight = FontWeight.Bold) },
      )
    }

    when (videosState) {
      is UiState.Loading -> {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          CryptoGoldShimmerCard(height = 220.dp)
          CryptoGoldShimmerCard(height = 220.dp)
        }
      }

      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(videosState.message, color = CrimsonBan, modifier = Modifier.padding(24.dp))
        }
      }

      is UiState.Success -> {
        val allVideos = videosState.data
        val reels = remember(allVideos) { allVideos.filter { it.videoType == "reel" } }
        val longVideos = remember(allVideos) { allVideos.filter { it.videoType == "long" } }
        val photos = remember(allVideos) { allVideos.filter { it.videoType == "photo" } }

        val sharePost: (VideoPost) -> Unit = { post ->
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, post.title)
              putExtra(Intent.EXTRA_TEXT, "${post.title} - ${post.authorName} on Crypto\n${post.mediaUrl}")
            }
          context.startActivity(Intent.createChooser(shareIntent, strings.share))
        }

        if (allVideos.isEmpty()) {
          EmptyFeedCard(onCreateClick = onCreateStoryClick)
        } else if (selectedTab == 0) {
          if (reels.isEmpty()) {
            EmptyFeedCard(onCreateClick = onCreateStoryClick)
          } else {
            ReelsPagerFeed(
              reels = reels,
              currentUserId = currentUser?.userId.orEmpty(),
              likedIds = likedIds,
              favoriteIds = favoriteIds,
              repostedIds = repostedIds,
              followingIds = followingIds,
              isAdmin = isAdmin,
              onToggleLike = onToggleLike,
              onToggleFavorite = onToggleFavorite,
              onToggleRepost = onToggleRepost,
              onToggleFollow = onToggleFollow,
              onOpenComments = {
                commentsVideoId = it
                onOpenComments(it)
              },
              onShare = sharePost,
              onOpenCreatorProfile = onOpenCreatorProfile,
              onRecordView = onRecordView,
              onAdminBanVideo = onAdminBanVideo,
            )
          }
        } else if (selectedTab == 1) {
          if (longVideos.isEmpty()) {
            EmptyFeedCard(onCreateClick = onCreateStoryClick)
          } else {
            LongVideosFeed(
              videos = longVideos,
              isAdmin = isAdmin,
              onOpenVideoDetail = onOpenVideoDetail,
              onOpenCreatorProfile = onOpenCreatorProfile,
              onAdminBanVideo = onAdminBanVideo,
            )
          }
        } else {
          if (photos.isEmpty()) {
            EmptyFeedCard(onCreateClick = onCreateStoryClick)
          } else {
            PhotosFeed(
              photos = photos,
              likedIds = likedIds,
              favoriteIds = favoriteIds,
              repostedIds = repostedIds,
              isAdmin = isAdmin,
              onToggleLike = onToggleLike,
              onToggleFavorite = onToggleFavorite,
              onToggleRepost = onToggleRepost,
              onOpenComments = {
                commentsVideoId = it
                onOpenComments(it)
              },
              onShare = sharePost,
              onOpenPhotoDetail = onOpenVideoDetail,
              onOpenCreatorProfile = onOpenCreatorProfile,
              onAdminBanPhoto = onAdminBanVideo,
            )
          }
        }
      }
    }
  }

  // Story Viewer Dialog
  viewingStory?.let { story ->
    StoryViewerDialog(
      story = story,
      isAdmin = isAdmin,
      onDismiss = { viewingStory = null },
      onBanStory = {
        onAdminBanStory(story)
        viewingStory = null
      },
      onMessageCreator = {
        viewingStory = null
        onMessageCreator(story.authorId)
      },
    )
  }

  // Comments ModalBottomSheet
  commentsVideoId?.let { videoId ->
    ModalBottomSheet(
      onDismissRequest = {
        commentsVideoId = null
        onOpenComments(null)
      },
      containerColor = MidnightOnyx,
      contentColor = WarmIvory,
    ) {
      CommentsSheetContent(
        comments = activeComments,
        onSendComment = { text -> onAddComment(videoId, text) },
      )
    }
  }
}

@Composable
private fun ReelsPagerFeed(
  reels: List<VideoPost>,
  currentUserId: String,
  likedIds: Set<String>,
  favoriteIds: Set<String>,
  repostedIds: Set<String>,
  followingIds: Set<String>,
  isAdmin: Boolean,
  onToggleLike: (VideoPost) -> Unit,
  onToggleFavorite: (VideoPost) -> Unit,
  onToggleRepost: (VideoPost) -> Unit,
  onToggleFollow: (String) -> Unit,
  onOpenComments: (String) -> Unit,
  onShare: (VideoPost) -> Unit,
  onOpenCreatorProfile: (String) -> Unit,
  onRecordView: (String) -> Unit,
  onAdminBanVideo: (VideoPost) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val pagerState = rememberPagerState(pageCount = { reels.size })

  LaunchedEffect(pagerState.currentPage, reels) {
    reels.getOrNull(pagerState.currentPage)?.let { onRecordView(it.id) }
  }

  VerticalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize().testTag("reels_vertical_pager"),
  ) { page ->
    val video = reels[page]
    val isActive = pagerState.currentPage == page
    val isLiked = video.id in likedIds
    val isFav = video.id in favoriteIds
    val isReposted = video.id in repostedIds
    val isFollowing = video.authorId in followingIds

    Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
      CryptoVideoPlayer(
        mediaUrl = video.mediaUrl,
        isActive = isActive,
        contentScale = ContentScale.Crop,
        fallbackImageUrl = video.thumbnailUrl,
        modifier = Modifier.fillMaxSize(),
      )

      // Bottom Scrim
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(220.dp)
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, ObsidianVoid.copy(alpha = 0.88f))
              )
            )
      )

      // Right-Side Social Action Rail
      Column(
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(modifier = Modifier.clickable { onOpenCreatorProfile(video.authorId) }) {
          AnimatedMediaAvatar(
            mediaUrl = video.authorAvatarUrl,
            mediaType = video.authorAvatarType,
            size = 46.dp,
            hasStoryRing = true,
          )
        }

        ReelActionButton(
          icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
          label = video.likesCount.toString(),
          tint = if (isLiked) CrimsonBan else WarmIvory,
          testTag = "like_reel_${video.id}",
          onClick = { onToggleLike(video) },
        )

        ReelActionButton(
          icon = Icons.AutoMirrored.Filled.Comment,
          label = video.commentsCount.toString(),
          tint = WarmIvory,
          testTag = "comment_reel_${video.id}",
          onClick = { onOpenComments(video.id) },
        )

        ReelActionButton(
          icon = if (isFav) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
          label = video.favoritesCount.toString(),
          tint = if (isFav) MetallicGold else WarmIvory,
          testTag = "favorite_reel_${video.id}",
          onClick = { onToggleFavorite(video) },
        )

        ReelActionButton(
          icon = Icons.Default.Repeat,
          label = video.repostsCount.toString(),
          tint = if (isReposted) EmeraldLive else WarmIvory,
          testTag = "repost_reel_${video.id}",
          onClick = { onToggleRepost(video) },
        )

        ReelActionButton(
          icon = Icons.Default.Share,
          label = strings.share,
          tint = ChampagneGold,
          testTag = "share_reel_${video.id}",
          onClick = { onShare(video) },
        )

        if (isAdmin) {
          ReelActionButton(
            icon = Icons.Default.Block,
            label = strings.banVideo,
            tint = CrimsonBan,
            testTag = "admin_ban_reel_${video.id}",
            onClick = { onAdminBanVideo(video) },
          )
        }
      }

      // Left-Side Creator & Metadata Overlay
      Column(
        modifier =
          Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, end = 86.dp, bottom = 22.dp),
      ) {
        if (ViralBoostRanker.isFreshViralBoostActive(video)) {
          Surface(
            color = MetallicGold.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
          ) {
            Text(
              text = strings.viralBoostBadge,
              style = MaterialTheme.typography.labelSmall,
              color = ChampagneGold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "@${video.authorName}",
            style = MaterialTheme.typography.titleMedium,
            color = WarmIvory,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onOpenCreatorProfile(video.authorId) },
          )
          if (video.authorId != currentUserId) {
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
              onClick = { onToggleFollow(video.authorId) },
              color = if (isFollowing) CharcoalGlass else MetallicGold,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = if (isFollowing) strings.following else strings.follow,
                style = MaterialTheme.typography.labelMedium,
                color = if (isFollowing) WarmIvory else ObsidianVoid,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = video.title,
          style = MaterialTheme.typography.bodyLarge,
          color = WarmIvory,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (video.tags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = video.tags.joinToString(" ") { "#$it" },
            style = MaterialTheme.typography.bodyMedium,
            color = ChampagneGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun ReelActionButton(
  icon: ImageVector,
  label: String,
  tint: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick).testTag(testTag),
  ) {
    Box(
      modifier =
        Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(ObsidianVoid.copy(alpha = 0.55f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = WarmIvory)
  }
}

@Composable
private fun LongVideosFeed(
  videos: List<VideoPost>,
  isAdmin: Boolean,
  onOpenVideoDetail: (VideoPost) -> Unit,
  onOpenCreatorProfile: (String) -> Unit,
  onAdminBanVideo: (VideoPost) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()

  LazyColumn(
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("long_videos_list"),
  ) {
    items(videos, key = { it.id }) { video ->
      Surface(
        onClick = { onOpenVideoDetail(video) },
        color = CharcoalGlass,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        modifier = Modifier.fillMaxWidth().testTag("video_card_${video.id}"),
      ) {
        Column {
          val cardThumb =
            remember(video.id, video.thumbnailUrl, video.mediaUrl) {
              video.thumbnailUrl.ifBlank { MediaUrlResolver.extractThumbnailUrl(video.mediaUrl) }
            }
          Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MidnightOnyx)) {
            if (cardThumb.isNotBlank()) {
              AsyncImage(
                model = cardThumb,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              CryptoVideoPlayer(
                mediaUrl = video.mediaUrl,
                isActive = false,
                mutedByDefault = true,
                showControls = false,
                modifier = Modifier.fillMaxSize(),
              )
            }

            Box(
              modifier =
                Modifier
                  .align(Alignment.Center)
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(ObsidianVoid.copy(alpha = 0.72f))
                  .border(1.5.dp, MetallicGold, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(32.dp),
              )
            }

            if (ViralBoostRanker.isFreshViralBoostActive(video)) {
              Surface(
                color = ObsidianVoid.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
              ) {
                Text(
                  text = strings.viralBoostBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = ChampagneGold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            if (isAdmin) {
              IconButton(
                onClick = { onAdminBanVideo(video) },
                modifier =
                  Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ObsidianVoid.copy(alpha = 0.75f)),
              ) {
                Icon(
                  imageVector = Icons.Default.Block,
                  contentDescription = strings.banVideo,
                  tint = CrimsonBan,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AnimatedMediaAvatar(
              mediaUrl = video.authorAvatarUrl,
              mediaType = video.authorAvatarType,
              size = 44.dp,
              modifier = Modifier.clickable { onOpenCreatorProfile(video.authorId) },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = WarmIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${video.authorName} • ${video.viewsCount} ${strings.viewsLabel} • ${video.likesCount} ❤",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PhotosFeed(
  photos: List<VideoPost>,
  likedIds: Set<String>,
  favoriteIds: Set<String>,
  repostedIds: Set<String>,
  isAdmin: Boolean,
  onToggleLike: (VideoPost) -> Unit,
  onToggleFavorite: (VideoPost) -> Unit,
  onToggleRepost: (VideoPost) -> Unit,
  onOpenComments: (String) -> Unit,
  onShare: (VideoPost) -> Unit,
  onOpenPhotoDetail: (VideoPost) -> Unit,
  onOpenCreatorProfile: (String) -> Unit,
  onAdminBanPhoto: (VideoPost) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()

  LazyColumn(
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("photos_feed_list"),
  ) {
    items(photos, key = { it.id }) { photo ->
      val isLiked = photo.id in likedIds
      val isFav = photo.id in favoriteIds
      val isReposted = photo.id in repostedIds

      Surface(
        onClick = { onOpenPhotoDetail(photo) },
        color = CharcoalGlass,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        modifier = Modifier.fillMaxWidth().testTag("photo_card_${photo.id}"),
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).clickable { onOpenCreatorProfile(photo.authorId) },
            ) {
              AnimatedMediaAvatar(
                mediaUrl = photo.authorAvatarUrl,
                mediaType = photo.authorAvatarType,
                size = 38.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = photo.authorName,
                  style = MaterialTheme.typography.titleSmall,
                  color = WarmIvory,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = photo.category,
                  style = MaterialTheme.typography.labelSmall,
                  color = ChampagneGold,
                )
              }
            }
            if (ViralBoostRanker.isFreshViralBoostActive(photo)) {
              Surface(
                color = ObsidianVoid.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
              ) {
                Text(
                  text = strings.viralBoostBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = ChampagneGold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }

          Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(MidnightOnyx)) {
            AsyncImage(
              model = photo.mediaUrl,
              imageLoader = imageLoader,
              contentDescription = photo.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            if (isAdmin) {
              IconButton(
                onClick = { onAdminBanPhoto(photo) },
                modifier =
                  Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ObsidianVoid.copy(alpha = 0.75f)),
              ) {
                Icon(
                  imageVector = Icons.Default.Block,
                  contentDescription = strings.banVideo,
                  tint = CrimsonBan,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }

          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
              text = photo.title,
              style = MaterialTheme.typography.titleMedium,
              color = WarmIvory,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (photo.description.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = photo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(onClick = { onToggleLike(photo) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.like,
                    tint = if (isLiked) CrimsonBan else WarmIvory,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("${photo.likesCount}", color = WarmIvory, style = MaterialTheme.typography.labelMedium)
                }
              }
              IconButton(onClick = { onOpenComments(photo.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = strings.comment,
                    tint = WarmIvory,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("${photo.commentsCount}", color = WarmIvory, style = MaterialTheme.typography.labelMedium)
                }
              }
              IconButton(onClick = { onToggleRepost(photo) }) {
                Icon(
                  imageVector = Icons.Default.Repeat,
                  contentDescription = strings.repost,
                  tint = if (isReposted) EmeraldLive else WarmIvory,
                  modifier = Modifier.size(20.dp),
                )
              }
              IconButton(onClick = { onToggleFavorite(photo) }) {
                Icon(
                  imageVector = if (isFav) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = strings.favorite,
                  tint = if (isFav) MetallicGold else WarmIvory,
                  modifier = Modifier.size(20.dp),
                )
              }
              IconButton(onClick = { onShare(photo) }) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = strings.share,
                  tint = ChampagneGold,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyFeedCard(onCreateClick: () -> Unit) {
  val strings = LocalCryptoStrings.current
  Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Surface(
      color = CharcoalGlass,
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(28.dp),
      ) {
        Icon(
          imageVector = Icons.Default.LocalFireDepartment,
          contentDescription = null,
          tint = MetallicGold,
          modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = strings.emptyFeedTitle,
          style = MaterialTheme.typography.titleLarge,
          color = ChampagneGold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = strings.emptyFeedSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSand,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onCreateClick,
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
        ) {
          Text(strings.navCreate, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun StoryViewerDialog(
  story: StoryItem,
  isAdmin: Boolean,
  onDismiss: () -> Unit,
  onBanStory: () -> Unit,
  onMessageCreator: () -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
      if (story.mediaType == "video") {
        CryptoVideoPlayer(
          mediaUrl = story.mediaUrl,
          isActive = true,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        AsyncImage(
          model = story.mediaUrl,
          imageLoader = imageLoader,
          contentDescription = story.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(ObsidianVoid.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          AnimatedMediaAvatar(
            mediaUrl = story.authorAvatarUrl,
            mediaType = story.authorAvatarType,
            size = 40.dp,
            hasStoryRing = true,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(story.authorName, color = WarmIvory, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Visibility, null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${story.viewsCount}", color = MutedSand, style = MaterialTheme.typography.labelSmall)
            }
          }
        }

        Row {
          if (isAdmin) {
            IconButton(onClick = onBanStory, modifier = Modifier.testTag("admin_ban_story_btn")) {
              Icon(Icons.Default.Block, strings.banStory, tint = CrimsonBan)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, strings.close, tint = WarmIvory)
          }
        }
      }

      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianVoid.copy(alpha = 0.9f))))
            .padding(20.dp),
      ) {
        if (story.caption.isNotBlank()) {
          Text(story.caption, color = WarmIvory, style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(12.dp))
        }
        Button(
          onClick = onMessageCreator,
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.replyToStory, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun CommentsSheetContent(
  comments: List<VideoComment>,
  onSendComment: (String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  var input by remember { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
    Text(
      text = strings.commentsTitle,
      style = MaterialTheme.typography.titleLarge,
      color = ChampagneGold,
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (comments.isEmpty()) {
      Text(
        text = strings.noCommentsYet,
        color = MutedSand,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 24.dp),
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(comments, key = { it.id }) { c ->
          Row(verticalAlignment = Alignment.Top) {
            AnimatedMediaAvatar(
              mediaUrl = c.authorAvatarUrl,
              mediaType = "image",
              size = 34.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(c.authorName, color = ChampagneGold, style = MaterialTheme.typography.labelLarge)
              Text(c.text, color = WarmIvory, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        placeholder = { Text(strings.writeCommentHint) },
        modifier = Modifier.weight(1f).testTag("comment_input"),
        singleLine = true,
      )
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(
        onClick = {
          if (input.isNotBlank()) {
            onSendComment(input)
            input = ""
          }
        },
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MetallicGold)
            .testTag("send_comment_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, strings.send, tint = ObsidianVoid)
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}
