package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PrivateUser
import com.example.data.model.PublicUser
import com.example.data.model.VideoPost
import com.example.data.util.CloudMediaStorage
import com.example.ui.components.AnimatedMediaAvatar
import com.example.ui.components.AnimatedMediaBanner
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  profileUser: PublicUser,
  privateUser: PrivateUser?,
  isOwnProfile: Boolean,
  isFollowing: Boolean,
  allVideos: List<VideoPost>,
  favoriteIds: Set<String>,
  repostedIds: Set<String>,
  onBack: (() -> Unit)? = null,
  onToggleFollow: () -> Unit,
  onMessageUser: () -> Unit,
  onOpenVideo: (VideoPost) -> Unit,
  onSaveProfile: (
    displayName: String,
    username: String,
    bio: String,
    avatarUrl: String,
    avatarMediaType: String,
    bannerUrl: String,
    bannerMediaType: String,
    email: String,
    phoneNumber: String,
  ) -> Unit,
  onToggleLanguage: () -> Unit,
  onSignOut: () -> Unit,
) {
  if (onBack != null) {
    BackHandler(onBack = onBack)
  }

  val strings = LocalCryptoStrings.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }

  val userLongVideos = remember(allVideos, profileUser.userId) {
    allVideos.filter { it.authorId == profileUser.userId && it.videoType == "long" }
  }
  val userReels = remember(allVideos, profileUser.userId) {
    allVideos.filter { it.authorId == profileUser.userId && it.videoType == "reel" }
  }
  val userReposts = remember(allVideos, repostedIds) {
    allVideos.filter { it.id in repostedIds }
  }
  val userFavorites = remember(allVideos, favoriteIds) {
    allVideos.filter { it.id in favoriteIds }
  }

  val activeList =
    when (selectedTab) {
      0 -> userLongVideos
      1 -> userReels
      2 -> userReposts
      else -> userFavorites
    }

  LazyColumn(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    item {
      // Animated Video / Image Profile Banner + Overlapping Animated Video Avatar
      Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedMediaBanner(
          mediaUrl = profileUser.bannerUrl,
          mediaType = profileUser.bannerMediaType,
          modifier = Modifier.fillMaxWidth().height(176.dp),
        )
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(176.dp)
              .background(
                Brush.verticalGradient(
                  listOf(ObsidianVoid.copy(alpha = 0.25f), ObsidianVoid.copy(alpha = 0.85f))
                )
              )
        )

        if (onBack != null) {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(ObsidianVoid.copy(alpha = 0.6f)),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.close, tint = WarmIvory)
          }
        }

        if (isOwnProfile) {
          Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            IconButton(
              onClick = onToggleLanguage,
              modifier = Modifier.size(40.dp).clip(CircleShape).background(ObsidianVoid.copy(alpha = 0.6f)),
            ) {
              Icon(Icons.Default.Language, strings.switchLanguage, tint = ChampagneGold)
            }
            IconButton(
              onClick = onSignOut,
              modifier =
                Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(ObsidianVoid.copy(alpha = 0.6f))
                  .testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, strings.signOut, tint = CrimsonBan)
            }
          }
        }

        AnimatedMediaAvatar(
          mediaUrl = profileUser.avatarUrl,
          mediaType = profileUser.avatarMediaType,
          size = 96.dp,
          hasStoryRing = true,
          modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp).offset(y = 42.dp),
        )
      }

      Spacer(modifier = Modifier.height(48.dp))

      Column(modifier = Modifier.padding(horizontal = 18.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = profileUser.displayName.ifBlank { "Creator" },
                style = MaterialTheme.typography.headlineMedium,
                color = WarmIvory,
                fontWeight = FontWeight.Bold,
              )
              if (profileUser.isVerified) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Verified, null, tint = MetallicGold, modifier = Modifier.size(20.dp))
              }
            }
            Text(
              text = "@${profileUser.username}",
              style = MaterialTheme.typography.bodyMedium,
              color = ChampagneGold,
            )
          }

          if (isOwnProfile) {
            OutlinedButton(
              onClick = { showEditDialog = true },
              shape = RoundedCornerShape(50),
              border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
              modifier = Modifier.testTag("edit_profile_button"),
            ) {
              Icon(Icons.Default.Edit, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.editProfile, color = ChampagneGold)
            }
          } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(onClick = onMessageUser, shape = RoundedCornerShape(50)) {
                Text(strings.startChatWithCreator, color = ChampagneGold)
              }
              Button(
                onClick = onToggleFollow,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) CharcoalGlass else MetallicGold,
                    contentColor = if (isFollowing) WarmIvory else ObsidianVoid,
                  ),
                shape = RoundedCornerShape(50),
              ) {
                Text(if (isFollowing) strings.following else strings.follow, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Animated media badges
        if (profileUser.avatarMediaType == "video" || profileUser.bannerMediaType == "video") {
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (profileUser.avatarMediaType == "video") {
              MediaTypeBadge(strings.videoAvatarActive)
            }
            if (profileUser.bannerMediaType == "video") {
              MediaTypeBadge(strings.videoBannerActive)
            }
          }
        }

        if (profileUser.bio.isNotBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(profileUser.bio, style = MaterialTheme.typography.bodyMedium, color = MutedSand)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Row
        Surface(
          color = CharcoalGlass,
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            StatBlock(value = profileUser.followersCount.toString(), label = strings.followersLabel)
            StatBlock(value = profileUser.followingCount.toString(), label = strings.followingLabel)
            StatBlock(value = profileUser.likesCount.toString(), label = strings.likesTotalLabel)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      val tabs =
        if (isOwnProfile) {
          listOf(strings.myVideos, strings.myReels, strings.myReposts, strings.myFavorites)
        } else {
          listOf(strings.myVideos, strings.myReels)
        }

      TabRow(
        selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex),
        containerColor = MidnightOnyx,
        contentColor = ChampagneGold,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.coerceAtMost(tabs.lastIndex)]),
            color = MetallicGold,
          )
        },
      ) {
        tabs.forEachIndexed { idx, label ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
        }
      }
    }

    items(activeList, key = { "${selectedTab}_${it.id}" }) { video ->
      Surface(
        onClick = { onOpenVideo(video) },
        color = CharcoalGlass,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PlayCircleOutline, null, tint = ChampagneGold, modifier = Modifier.size(38.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(video.title, color = WarmIvory, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
              text = "${video.viewsCount} ${strings.viewsLabel} • ${video.likesCount} ❤",
              color = MutedSand,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  if (showEditDialog && isOwnProfile) {
    EditProfileDialog(
      user = profileUser,
      privateUser = privateUser,
      onDismiss = { showEditDialog = false },
      onSave = { dn, un, bio, avUrl, avType, bnUrl, bnType, em, ph ->
        onSaveProfile(dn, un, bio, avUrl, avType, bnUrl, bnType, em, ph)
        showEditDialog = false
      },
    )
  }
}

@Composable
private fun MediaTypeBadge(text: String) {
  Surface(
    color = CharcoalGlass,
    shape = RoundedCornerShape(50),
    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Default.Videocam, null, tint = MetallicGold, modifier = Modifier.size(13.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(text, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
    }
  }
}

@Composable
private fun StatBlock(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = ChampagneGold, fontWeight = FontWeight.Bold)
    Text(label, style = MaterialTheme.typography.labelMedium, color = MutedSand)
  }
}

@Composable
private fun EditProfileDialog(
  user: PublicUser,
  privateUser: PrivateUser?,
  onDismiss: () -> Unit,
  onSave: (
    displayName: String,
    username: String,
    bio: String,
    avatarUrl: String,
    avatarMediaType: String,
    bannerUrl: String,
    bannerMediaType: String,
    email: String,
    phoneNumber: String,
  ) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var displayName by remember { mutableStateOf(user.displayName) }
  var username by remember { mutableStateOf(user.username) }
  var bio by remember { mutableStateOf(user.bio) }
  var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
  var avatarType by remember { mutableStateOf(user.avatarMediaType) }
  var bannerUrl by remember { mutableStateOf(user.bannerUrl) }
  var bannerType by remember { mutableStateOf(user.bannerMediaType) }
  var email by remember { mutableStateOf(privateUser?.email.orEmpty()) }
  var phone by remember { mutableStateOf(privateUser?.phoneNumber.orEmpty()) }
  var isUploadingMedia by remember { mutableStateOf(false) }
  var uploadProgress by remember { mutableFloatStateOf(0f) }
  var uploadStatus by remember { mutableStateOf<String?>(null) }

  val avatarPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isUploadingMedia = true
          uploadProgress = 0f
          uploadStatus = strings.uploadingToCloud
          runCatching {
            CloudMediaStorage.uploadPickedMedia(context, uri, "avatars") { p ->
              uploadProgress = p
            }
          }
            .onSuccess {
              isUploadingMedia = false
              avatarUrl = it.downloadUrl
              avatarType = if (it.isVideo) "video" else "image"
              uploadStatus = strings.cloudUploadReady
            }
            .onFailure {
              isUploadingMedia = false
              uploadStatus = it.localizedMessage
            }
        }
      }
    }

  val bannerPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isUploadingMedia = true
          uploadProgress = 0f
          uploadStatus = strings.uploadingToCloud
          runCatching {
            CloudMediaStorage.uploadPickedMedia(context, uri, "banners") { p ->
              uploadProgress = p
            }
          }
            .onSuccess {
              isUploadingMedia = false
              bannerUrl = it.downloadUrl
              bannerType = if (it.isVideo) "video" else "image"
              uploadStatus = strings.cloudUploadReady
            }
            .onFailure {
              isUploadingMedia = false
              uploadStatus = it.localizedMessage
            }
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MidnightOnyx,
    titleContentColor = ChampagneGold,
    title = { Text(strings.editProfile, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (isUploadingMedia) {
          LinearProgressIndicator(
            progress = { uploadProgress.coerceIn(0.05f, 1f) },
            color = MetallicGold,
            trackColor = CharcoalGlass,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
          )
        }
        uploadStatus?.let { msg ->
          Text(
            text = msg,
            style = MaterialTheme.typography.labelMedium,
            color = ChampagneGold,
          )
        }

        OutlinedButton(
          onClick = {
            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
          },
          enabled = !isUploadingMedia,
          modifier = Modifier.fillMaxWidth().testTag("pick_avatar_media_btn"),
        ) {
          Text(strings.pickVideoOrImageAvatar, color = ChampagneGold)
        }
        OutlinedTextField(
          value = avatarUrl,
          onValueChange = {
            avatarUrl = it
            avatarType = if (CloudMediaStorage.isVideoUri(it)) "video" else "image"
          },
          label = { Text("Avatar HTTPS URL (${avatarType})") },
          singleLine = true,
        )

        OutlinedButton(
          onClick = {
            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
          },
          enabled = !isUploadingMedia,
          modifier = Modifier.fillMaxWidth().testTag("pick_banner_media_btn"),
        ) {
          Text(strings.pickVideoOrImageBanner, color = ChampagneGold)
        }
        OutlinedTextField(
          value = bannerUrl,
          onValueChange = {
            bannerUrl = it
            bannerType = if (CloudMediaStorage.isVideoUri(it)) "video" else "image"
          },
          label = { Text("Banner HTTPS URL (${bannerType})") },
          singleLine = true,
        )

        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text(strings.displayNameLabel) },
          singleLine = true,
        )
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text(strings.usernameLabel) },
          singleLine = true,
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text(strings.bioLabel) },
          maxLines = 3,
        )

        Text(strings.contactBindingTitle, style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(strings.emailLabel) },
          singleLine = true,
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(strings.phoneLabel) },
          singleLine = true,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(displayName, username, bio, avatarUrl, avatarType, bannerUrl, bannerType, email, phone)
        },
        enabled = !isUploadingMedia,
        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
      ) {
        Text(strings.saveChanges, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel, color = MutedSand) }
    },
  )
}
