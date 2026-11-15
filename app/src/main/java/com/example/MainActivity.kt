package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthManager
import com.example.auth.GoogleAuthManager.authStateFlow
import com.example.data.model.AppLanguage
import com.example.data.model.PublicUser
import com.example.data.model.UiState
import com.example.data.model.VideoPost
import com.example.data.repository.CryptoAdminRepository
import com.example.data.repository.CryptoFeedRepository
import com.example.data.repository.CryptoSocialRepository
import com.example.data.repository.LanguagePreferences
import com.example.data.util.NetworkStatusMonitor
import com.example.ui.components.CryptoFullScreenLoader
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatsAndAlertsScreen
import com.example.ui.screens.CreateStudioScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchExploreScreen
import com.example.ui.screens.VideoDetailScreen
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.strings.stringsFor
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory
import com.example.ui.theme.CryptoTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.FeedViewModel
import com.example.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class MainTab {
  HOME,
  DISCOVER,
  CREATE,
  INBOX,
  PROFILE,
  ADMIN,
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CryptoTheme {
        CryptoAppRoot()
      }
    }
  }
}

@Composable
fun CryptoAppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val languagePrefs = remember(context) { LanguagePreferences(context) }
  val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.AR)
  val strings = remember(language) { stringsFor(language) }
  val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var checkingSilentAuth by remember { mutableStateOf(auth.currentUser == null) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      val cm = CredentialManager.create(context)
      GoogleAuthManager.attemptAutoSignIn(
        context = context,
        credentialManager = cm,
        onAuthSuccess = { checkingSilentAuth = false },
        onUnauthenticated = { checkingSilentAuth = false },
        scope = scope,
      )
    } else {
      checkingSilentAuth = false
    }
  }

  val toggleLanguage: () -> Unit = {
    val next = if (language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
    scope.launch { languagePrefs.setLanguage(next) }
  }

  CompositionLocalProvider(
    LocalCryptoStrings provides strings,
    LocalLayoutDirection provides layoutDirection,
  ) {
    val user = currentUser
    if (checkingSilentAuth) {
      CryptoFullScreenLoader(message = strings.loadingCrypto)
    } else if (user == null) {
      AuthScreen(
        onAuthSuccess = { checkingSilentAuth = false },
        onToggleLanguage = toggleLanguage,
      )
    } else {
      key(user.uid) {
        AuthenticatedCryptoShell(
          firebaseUser = user,
          language = language,
          onToggleLanguage = toggleLanguage,
        )
      }
    }
  }
}

@Composable
private fun AuthenticatedCryptoShell(
  firebaseUser: FirebaseUser,
  language: AppLanguage,
  onToggleLanguage: () -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val currentUserId = firebaseUser.uid
  val isAdmin = remember(firebaseUser) { GoogleAuthManager.isAdminUser(firebaseUser) }

  val feedViewModel: FeedViewModel =
    viewModel(
      key = "feed_$currentUserId",
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            FeedViewModel(CryptoFeedRepository(db), currentUserId)
          }
        },
    )

  val socialViewModel: SocialViewModel =
    viewModel(
      key = "social_$currentUserId",
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            SocialViewModel(CryptoSocialRepository(db), currentUserId)
          }
        },
    )

  // Instantiate AdminViewModel ONLY for verified dhddu211@gmail.com
  val adminViewModel: AdminViewModel? =
    if (isAdmin) {
      viewModel(
        key = "admin_$currentUserId",
        factory =
          viewModelFactory {
            initializer {
              val app = checkNotNull(this[APPLICATION_KEY])
              val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
              AdminViewModel(CryptoAdminRepository(db))
            }
          },
      )
    } else {
      null
    }

  LaunchedEffect(currentUserId) {
    socialViewModel.ensureInitialized(firebaseUser, language.code)
  }

  val currentUserPublic by socialViewModel.currentUserPublic.collectAsStateWithLifecycle()
  val currentUserPrivate by socialViewModel.currentUserPrivate.collectAsStateWithLifecycle()
  val publicUsersState by socialViewModel.publicUsersState.collectAsStateWithLifecycle()
  val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
  val userChats by socialViewModel.userChats.collectAsStateWithLifecycle()
  val activeMessages by socialViewModel.activeChatMessages.collectAsStateWithLifecycle()
  val userAlerts by socialViewModel.userAlerts.collectAsStateWithLifecycle()

  val videosState by feedViewModel.rankedVideosState.collectAsStateWithLifecycle()
  val storiesState by feedViewModel.storiesState.collectAsStateWithLifecycle()
  val likedIds by feedViewModel.likedVideoIds.collectAsStateWithLifecycle()
  val favoriteIds by feedViewModel.favoriteVideoIds.collectAsStateWithLifecycle()
  val repostedIds by feedViewModel.repostedVideoIds.collectAsStateWithLifecycle()
  val activeComments by feedViewModel.activeComments.collectAsStateWithLifecycle()
  val searchQuery by feedViewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by feedViewModel.selectedCategory.collectAsStateWithLifecycle()
  val filteredVideos by feedViewModel.filteredVideos.collectAsStateWithLifecycle()
  val isOnline by
    remember(context) { NetworkStatusMonitor.observeIsOnline(context) }
      .collectAsStateWithLifecycle(initialValue = true)

  var activeTab by remember { mutableStateOf(MainTab.HOME) }
  var detailVideo by remember { mutableStateOf<VideoPost?>(null) }
  var viewingCreatorId by remember { mutableStateOf<String?>(null) }
  var activeChatId by remember { mutableStateOf<String?>(null) }

  val allPublicUsers = (publicUsersState as? UiState.Success)?.data.orEmpty()
  val allPublicVideos = (videosState as? UiState.Success)?.data.orEmpty()

  val signOutAction: () -> Unit = {
    val cm = CredentialManager.create(context)
    GoogleAuthManager.signOut(cm, scope)
  }

  if (currentUserPublic?.isBanned == true && !isAdmin) {
    Box(
      modifier = Modifier.fillMaxSize().background(ObsidianVoid).windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.Block, null, tint = CrimsonBan, modifier = Modifier.height(64.dp))
        Text(strings.accountBannedTitle, style = MaterialTheme.typography.headlineMedium, color = CrimsonBan)
        Text(strings.accountBannedSubtitle, style = MaterialTheme.typography.bodyLarge, color = WarmIvory)
        Button(
          onClick = signOutAction,
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
        ) {
          Text(strings.signOut)
        }
      }
    }
    return
  }

  val navItems =
    buildList {
      add(Triple(MainTab.HOME, strings.navHome, Icons.Default.Home))
      add(Triple(MainTab.DISCOVER, strings.navDiscover, Icons.Default.Search))
      add(Triple(MainTab.CREATE, strings.navCreate, Icons.Default.AddBox))
      add(Triple(MainTab.INBOX, strings.navInbox, Icons.AutoMirrored.Filled.Chat))
      add(Triple(MainTab.PROFILE, strings.navProfile, Icons.Default.Person))
      if (isAdmin) {
        add(Triple(MainTab.ADMIN, strings.navAdmin, Icons.Default.AdminPanelSettings))
      }
    }

  BackHandler(
    enabled = detailVideo != null || viewingCreatorId != null || activeChatId != null || activeTab != MainTab.HOME
  ) {
    when {
      detailVideo != null -> {
        detailVideo = null
        feedViewModel.openCommentsForVideo(null)
      }
      viewingCreatorId != null -> viewingCreatorId = null
      activeChatId != null -> {
        activeChatId = null
        socialViewModel.selectChat(null)
      }
      activeTab != MainTab.HOME -> activeTab = MainTab.HOME
    }
  }

  val openMessageWithCreator: (String) -> Unit = { creatorUid ->
    val peer =
      allPublicUsers.firstOrNull { it.userId == creatorUid }
        ?: PublicUser(userId = creatorUid, displayName = "Creator")
    socialViewModel.openChatWith(peer) { chatId ->
      detailVideo = null
      viewingCreatorId = null
      activeChatId = chatId
      activeTab = MainTab.INBOX
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    val useNavigationRail = maxWidth >= 700.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail && detailVideo == null && viewingCreatorId == null) {
        NavigationRail(
          containerColor = MidnightOnyx,
          modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
          Spacer(modifier = Modifier.height(16.dp))
          navItems.forEach { (tab, label, icon) ->
            NavigationRailItem(
              selected = activeTab == tab,
              onClick = { activeTab = tab },
              icon = { Icon(icon, label) },
              label = { Text(label) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = ObsidianVoid,
                  selectedTextColor = ChampagneGold,
                  indicatorColor = MetallicGold,
                  unselectedIconColor = MutedSand,
                  unselectedTextColor = MutedSand,
                ),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = ObsidianVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
          if (!useNavigationRail && detailVideo == null && viewingCreatorId == null) {
            NavigationBar(
              containerColor = MidnightOnyx,
              windowInsets = WindowInsets.navigationBars,
            ) {
              navItems.forEach { (tab, label, icon) ->
                NavigationBarItem(
                  selected = activeTab == tab,
                  onClick = { activeTab = tab },
                  icon = { Icon(icon, label) },
                  label = { Text(label, maxLines = 1) },
                  modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                  colors =
                    NavigationBarItemDefaults.colors(
                      selectedIconColor = ObsidianVoid,
                      selectedTextColor = ChampagneGold,
                      indicatorColor = MetallicGold,
                      unselectedIconColor = MutedSand,
                      unselectedTextColor = MutedSand,
                    ),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          val openVideo = detailVideo
          val openCreator = viewingCreatorId

          if (openVideo != null) {
            LaunchedEffect(openVideo.id) { feedViewModel.openCommentsForVideo(openVideo.id) }
            val latestVideo = allPublicVideos.firstOrNull { it.id == openVideo.id } ?: openVideo
            VideoDetailScreen(
              video = latestVideo,
              currentUserId = currentUserId,
              isLiked = latestVideo.id in likedIds,
              isFavorited = latestVideo.id in favoriteIds,
              isReposted = latestVideo.id in repostedIds,
              isFollowing = latestVideo.authorId in followingIds,
              comments = activeComments,
              isAdmin = isAdmin,
              onBack = {
                detailVideo = null
                feedViewModel.openCommentsForVideo(null)
              },
              onRecordView = { feedViewModel.recordView(latestVideo.id) },
              onToggleLike = { feedViewModel.toggleLike(latestVideo) },
              onToggleFavorite = { feedViewModel.toggleFavorite(latestVideo) },
              onToggleRepost = { feedViewModel.toggleRepost(latestVideo) },
              onToggleFollow = { socialViewModel.toggleFollow(latestVideo.authorId) },
              onAddComment = { text ->
                val me = currentUserPublic ?: PublicUser(userId = currentUserId, displayName = firebaseUser.displayName ?: "User")
                feedViewModel.addComment(latestVideo.id, me, text)
              },
              onOpenCreatorProfile = { creatorId ->
                detailVideo = null
                viewingCreatorId = creatorId
              },
              onMessageCreator = openMessageWithCreator,
              onAdminBanVideo = {
                adminViewModel?.toggleVideoBan(latestVideo, "Banned by Admin")
                detailVideo = null
              },
            )
          } else if (openCreator != null) {
            val creator =
              allPublicUsers.firstOrNull { it.userId == openCreator }
                ?: PublicUser(userId = openCreator, displayName = "Creator")
            ProfileScreen(
              profileUser = creator,
              privateUser = null,
              isOwnProfile = creator.userId == currentUserId,
              isFollowing = creator.userId in followingIds,
              allVideos = allPublicVideos,
              favoriteIds = favoriteIds,
              repostedIds = repostedIds,
              onBack = { viewingCreatorId = null },
              onToggleFollow = { socialViewModel.toggleFollow(creator.userId) },
              onMessageUser = { openMessageWithCreator(creator.userId) },
              onOpenVideo = {
                viewingCreatorId = null
                detailVideo = it
              },
              onSaveProfile = { _, _, _, _, _, _, _, _, _ -> },
              onToggleLanguage = onToggleLanguage,
              onSignOut = signOutAction,
            )
          } else {
            when (activeTab) {
              MainTab.HOME -> {
                HomeFeedScreen(
                  videosState = videosState,
                  storiesState = storiesState,
                  currentUser = currentUserPublic,
                  likedIds = likedIds,
                  favoriteIds = favoriteIds,
                  repostedIds = repostedIds,
                  followingIds = followingIds,
                  activeComments = activeComments,
                  isAdmin = isAdmin,
                  isOnline = isOnline,
                  onToggleLike = { feedViewModel.toggleLike(it) },
                  onToggleFavorite = { feedViewModel.toggleFavorite(it) },
                  onToggleRepost = { feedViewModel.toggleRepost(it) },
                  onToggleFollow = { socialViewModel.toggleFollow(it) },
                  onOpenComments = { feedViewModel.openCommentsForVideo(it) },
                  onAddComment = { vidId, text ->
                    val me = currentUserPublic ?: PublicUser(userId = currentUserId, displayName = firebaseUser.displayName ?: "User")
                    feedViewModel.addComment(vidId, me, text)
                  },
                  onOpenVideoDetail = { detailVideo = it },
                  onOpenCreatorProfile = { viewingCreatorId = it },
                  onCreateStoryClick = { activeTab = MainTab.CREATE },
                  onRecordView = { feedViewModel.recordView(it) },
                  onRecordStoryView = { feedViewModel.recordStoryView(it) },
                  onAdminBanVideo = { adminViewModel?.toggleVideoBan(it, "Banned from Feed") },
                  onAdminBanStory = { adminViewModel?.toggleStoryBan(it, "Banned Story by Admin") },
                  onMessageCreator = openMessageWithCreator,
                )
              }

              MainTab.DISCOVER -> {
                SearchExploreScreen(
                  searchQuery = searchQuery,
                  selectedCategory = selectedCategory,
                  videos = filteredVideos,
                  creators = allPublicUsers,
                  onQueryChange = { feedViewModel.updateSearchQuery(it) },
                  onCategoryChange = { feedViewModel.updateSelectedCategory(it) },
                  onOpenVideo = { detailVideo = it },
                  onOpenCreator = { viewingCreatorId = it },
                )
              }

              MainTab.CREATE -> {
                val me =
                  currentUserPublic
                    ?: PublicUser(
                      userId = currentUserId,
                      displayName = firebaseUser.displayName ?: "Creator",
                    )
                CreateStudioScreen(
                  currentUser = me,
                  onPublishVideo = { type, title, desc, tags, cat, url, dur, ok, err ->
                    feedViewModel.publishVideo(me, type, title, desc, tags, cat, url, "", dur, ok, err)
                  },
                  onPublishStory = { url, mediaType, caption, ok, err ->
                    feedViewModel.publishStory(me, url, mediaType, caption, ok, err)
                  },
                  onPublishedNavigateHome = { activeTab = MainTab.HOME },
                )
              }

              MainTab.INBOX -> {
                val activeChat = userChats.firstOrNull { it.id == activeChatId }
                ChatsAndAlertsScreen(
                  currentUserId = currentUserId,
                  chats = userChats,
                  activeChat = activeChat,
                  activeMessages = activeMessages,
                  alerts = userAlerts,
                  onSelectChat = { chat ->
                    activeChatId = chat?.id
                    socialViewModel.selectChat(chat?.id)
                  },
                  onSendMessage = { chat, text ->
                    socialViewModel.sendMessage(chat, text) {}
                  },
                  onMarkAlertRead = { socialViewModel.markAlertRead(it) },
                )
              }

              MainTab.PROFILE -> {
                val me =
                  currentUserPublic
                    ?: PublicUser(
                      userId = currentUserId,
                      displayName = firebaseUser.displayName ?: "Creator",
                      username = firebaseUser.email?.substringBefore("@") ?: "user",
                    )
                ProfileScreen(
                  profileUser = me,
                  privateUser = currentUserPrivate,
                  isOwnProfile = true,
                  isFollowing = false,
                  allVideos = allPublicVideos,
                  favoriteIds = favoriteIds,
                  repostedIds = repostedIds,
                  onToggleFollow = {},
                  onMessageUser = {},
                  onOpenVideo = { detailVideo = it },
                  onSaveProfile = { dn, un, bio, avUrl, avType, bnUrl, bnType, em, ph ->
                    socialViewModel.updateProfile(dn, un, bio, avUrl, avType, bnUrl, bnType) {}
                    socialViewModel.updateContactInfo(em, ph, language.code) {}
                  },
                  onToggleLanguage = onToggleLanguage,
                  onSignOut = signOutAction,
                )
              }

              MainTab.ADMIN -> {
                if (isAdmin && adminViewModel != null) {
                  val admVideos by adminViewModel.allVideos.collectAsStateWithLifecycle()
                  val admStories by adminViewModel.allStories.collectAsStateWithLifecycle()
                  val admUsers by adminViewModel.allUsers.collectAsStateWithLifecycle()
                  val admChats by adminViewModel.allChats.collectAsStateWithLifecycle()
                  val inspectedId by adminViewModel.inspectedChatId.collectAsStateWithLifecycle()
                  val inspectedMsgs by adminViewModel.inspectedChatMessages.collectAsStateWithLifecycle()

                  AdminDashboardScreen(
                    allVideos = admVideos,
                    allStories = admStories,
                    allUsers = admUsers,
                    allChats = admChats,
                    inspectedChatId = inspectedId,
                    inspectedMessages = inspectedMsgs,
                    onToggleVideoBan = { v, reason -> adminViewModel.toggleVideoBan(v, reason) },
                    onToggleStoryBan = { s, reason -> adminViewModel.toggleStoryBan(s, reason) },
                    onToggleUserBan = { u, reason -> adminViewModel.toggleUserBan(u, reason) },
                    onToggleUserVerified = { u -> adminViewModel.toggleUserVerified(u) },
                    onToggleChatBan = { c, reason -> adminViewModel.toggleChatBan(c, reason) },
                    onInspectChat = { id -> adminViewModel.inspectChat(id) },
                    onSendDirectAlert = { uid, title, msg, sev ->
                      adminViewModel.sendDirectAlert(uid, title, msg, sev) {}
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
