package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PublicUser
import com.example.data.model.VideoPost
import com.example.data.util.MediaUrlResolver
import com.example.data.util.ViralBoostRanker
import com.example.ui.components.AnimatedMediaAvatar
import com.example.ui.components.rememberCryptoImageLoader
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory

@Composable
fun SearchExploreScreen(
  searchQuery: String,
  selectedCategory: String,
  videos: List<VideoPost>,
  creators: List<PublicUser>,
  onQueryChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onOpenVideo: (VideoPost) -> Unit,
  onOpenCreator: (String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val imageLoader = rememberCryptoImageLoader()
  val categories = listOf("ALL", "Crypto", "Tech", "Gaming", "Music", "Comedy", "News")
  val matchingCreators =
    creators.filter {
      searchQuery.isBlank() ||
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true)
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(ObsidianVoid),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text(strings.searchHint) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = ChampagneGold) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("search_input"),
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { cat ->
          val label = if (cat == "ALL") strings.allCategories else cat
          val isSelected = selectedCategory == cat
          FilterChip(
            selected = isSelected,
            onClick = { onCategoryChange(cat) },
            label = { Text(label) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MetallicGold,
                selectedLabelColor = ObsidianVoid,
                containerColor = CharcoalGlass,
                labelColor = WarmIvory,
              ),
          )
        }
      }
    }

    if (matchingCreators.isNotEmpty()) {
      item {
        Text(
          text = strings.trendingCreators,
          style = MaterialTheme.typography.titleLarge,
          color = ChampagneGold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(matchingCreators.take(15), key = { it.userId }) { user ->
            Surface(
              onClick = { onOpenCreator(user.userId) },
              color = CharcoalGlass,
              shape = RoundedCornerShape(18.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
              modifier = Modifier.width(136.dp),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp),
              ) {
                AnimatedMediaAvatar(
                  mediaUrl = user.avatarUrl,
                  mediaType = user.avatarMediaType,
                  size = 58.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, null, tint = MetallicGold, modifier = Modifier.size(14.dp))
                  }
                }
                Text(
                  text = "@${user.username}",
                  style = MaterialTheme.typography.labelMedium,
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

    item {
      Text(
        text = strings.trendingVideos,
        style = MaterialTheme.typography.titleLarge,
        color = ChampagneGold,
      )
    }

    items(videos, key = { it.id }) { video ->
      val thumb =
        remember(video.id, video.thumbnailUrl, video.mediaUrl, video.videoType) {
          when {
            video.videoType == "photo" -> video.mediaUrl
            video.thumbnailUrl.isNotBlank() -> video.thumbnailUrl
            else -> MediaUrlResolver.extractThumbnailUrl(video.mediaUrl)
          }
        }
      Surface(
        onClick = { onOpenVideo(video) },
        color = CharcoalGlass,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        modifier = Modifier.fillMaxWidth().testTag("search_video_${video.id}"),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightOnyx),
            contentAlignment = Alignment.Center,
          ) {
            if (thumb.isNotBlank()) {
              AsyncImage(
                model = thumb,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Icon(
              imageVector = if (video.videoType == "photo") Icons.Default.Image else Icons.Default.PlayCircleOutline,
              contentDescription = null,
              tint = ChampagneGold,
              modifier = Modifier.size(28.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = video.title,
              style = MaterialTheme.typography.titleMedium,
              color = WarmIvory,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${video.authorName} • ${video.videoType.uppercase()} • ${video.viewsCount} ${strings.viewsLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = MutedSand,
              maxLines = 1,
            )
          }
          if (ViralBoostRanker.isFreshViralBoostActive(video)) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = strings.viralBoostBadge,
              tint = MetallicGold,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}
