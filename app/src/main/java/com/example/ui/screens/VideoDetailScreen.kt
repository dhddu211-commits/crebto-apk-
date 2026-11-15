package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoComment
import com.example.data.model.VideoPost
import com.example.data.util.ViralBoostRanker
import com.example.ui.components.AnimatedMediaAvatar
import com.example.ui.components.CryptoVideoPlayer
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

@Composable
fun VideoDetailScreen(
  video: VideoPost,
  currentUserId: String,
  isLiked: Boolean,
  isFavorited: Boolean,
  isReposted: Boolean,
  isFollowing: Boolean,
  comments: List<VideoComment>,
  isAdmin: Boolean,
  onBack: () -> Unit,
  onRecordView: () -> Unit,
  onToggleLike: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleRepost: () -> Unit,
  onToggleFollow: () -> Unit,
  onAddComment: (String) -> Unit,
  onOpenCreatorProfile: (String) -> Unit,
  onMessageCreator: (String) -> Unit,
  onAdminBanVideo: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val strings = LocalCryptoStrings.current
  val context = LocalContext.current
  var commentInput by remember { mutableStateOf("") }

  LaunchedEffect(video.id) { onRecordView() }

  Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    // Top Video Player
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(ObsidianVoid)) {
      CryptoVideoPlayer(
        mediaUrl = video.mediaUrl,
        isActive = true,
        contentScale = ContentScale.Fit,
        fallbackImageUrl = video.thumbnailUrl,
        modifier = Modifier.fillMaxSize(),
      )
      IconButton(
        onClick = onBack,
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .padding(10.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(ObsidianVoid.copy(alpha = 0.65f))
            .testTag("video_detail_back"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.close, tint = WarmIvory)
      }
    }

    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
      item {
        Column(modifier = Modifier.padding(16.dp)) {
          if (ViralBoostRanker.isFreshViralBoostActive(video)) {
            Surface(
              color = MetallicGold.copy(alpha = 0.18f),
              shape = RoundedCornerShape(50),
              border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = MetallicGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.viralBoostBadge, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          Text(
            text = video.title,
            style = MaterialTheme.typography.headlineMedium,
            color = WarmIvory,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${video.viewsCount} ${strings.viewsLabel} • ${video.category}",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSand,
          )
          if (video.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(video.description, style = MaterialTheme.typography.bodyMedium, color = WarmIvory)
          }
          if (video.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = video.tags.joinToString(" ") { "#$it" },
              style = MaterialTheme.typography.bodyMedium,
              color = ChampagneGold,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            DetailActionChip(
              icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              label = "${video.likesCount}",
              active = isLiked,
              activeTint = CrimsonBan,
              onClick = onToggleLike,
            )
            DetailActionChip(
              icon = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              label = "${video.favoritesCount}",
              active = isFavorited,
              activeTint = MetallicGold,
              onClick = onToggleFavorite,
            )
            DetailActionChip(
              icon = Icons.Default.Repeat,
              label = "${video.repostsCount}",
              active = isReposted,
              activeTint = EmeraldLive,
              onClick = onToggleRepost,
            )
            DetailActionChip(
              icon = Icons.Default.Share,
              label = strings.share,
              active = false,
              activeTint = ChampagneGold,
              onClick = {
                val shareIntent =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, video.title)
                    putExtra(Intent.EXTRA_TEXT, "${video.title} - ${video.authorName}\n${video.mediaUrl}")
                  }
                context.startActivity(Intent.createChooser(shareIntent, strings.share))
              },
            )
            if (isAdmin) {
              DetailActionChip(
                icon = Icons.Default.Block,
                label = strings.banVideo,
                active = true,
                activeTint = CrimsonBan,
                onClick = onAdminBanVideo,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = HairlineGoldBorder)
          Spacer(modifier = Modifier.height(14.dp))

          // Creator Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).clickable { onOpenCreatorProfile(video.authorId) },
            ) {
              AnimatedMediaAvatar(
                mediaUrl = video.authorAvatarUrl,
                mediaType = video.authorAvatarType,
                size = 46.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = video.authorName,
                style = MaterialTheme.typography.titleMedium,
                color = WarmIvory,
                fontWeight = FontWeight.Bold,
              )
            }
            if (video.authorId != currentUserId) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { onMessageCreator(video.authorId) },
                  colors = ButtonDefaults.buttonColors(containerColor = CharcoalGlass, contentColor = ChampagneGold),
                  shape = RoundedCornerShape(50),
                ) {
                  Text(strings.startChatWithCreator)
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

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "${strings.commentsTitle} (${comments.size})",
            style = MaterialTheme.typography.titleLarge,
            color = ChampagneGold,
          )
        }
      }

      if (comments.isEmpty()) {
        item {
          Text(
            text = strings.noCommentsYet,
            color = MutedSand,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          )
        }
      } else {
        items(comments, key = { it.id }) { c ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            AnimatedMediaAvatar(mediaUrl = c.authorAvatarUrl, mediaType = "image", size = 34.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(c.authorName, color = ChampagneGold, style = MaterialTheme.typography.labelLarge)
              Text(c.text, color = WarmIvory, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }

    // Comment Input Bar
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(MidnightOnyx)
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = commentInput,
        onValueChange = { commentInput = it },
        placeholder = { Text(strings.writeCommentHint) },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(
        onClick = {
          if (commentInput.isNotBlank()) {
            onAddComment(commentInput)
            commentInput = ""
          }
        },
        modifier = Modifier.size(48.dp).clip(CircleShape).background(MetallicGold),
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, strings.send, tint = ObsidianVoid)
      }
    }
  }
}

@Composable
private fun DetailActionChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean,
  activeTint: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = CharcoalGlass,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, if (active) activeTint else HairlineGoldBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) activeTint else WarmIvory,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(label, style = MaterialTheme.typography.labelMedium, color = WarmIvory)
    }
  }
}
