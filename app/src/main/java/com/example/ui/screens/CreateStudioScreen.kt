package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PublicUser
import com.example.data.util.CloudMediaStorage
import com.example.ui.components.CryptoVaultLoader
import com.example.ui.components.CryptoVideoPlayer
import com.example.ui.components.rememberCryptoImageLoader
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory
import kotlinx.coroutines.launch

@Composable
fun CreateStudioScreen(
  currentUser: PublicUser?,
  onPublishVideo: (
    videoType: String,
    title: String,
    description: String,
    tags: List<String>,
    category: String,
    mediaUrl: String,
    durationSeconds: Long,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) -> Unit,
  onPublishStory: (
    mediaUrl: String,
    mediaType: String,
    caption: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) -> Unit,
  onPublishedNavigateHome: () -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val imageLoader = rememberCryptoImageLoader()

  var mode by remember { mutableStateOf("reel") } // "reel", "long", "story"
  var mediaUrl by remember { mutableStateOf("") }
  var pickedIsVideo by remember { mutableStateOf(true) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var tagsText by remember { mutableStateOf("كريبتو, ترند") }
  var category by remember { mutableStateOf("Crypto") }
  var isUploading by remember { mutableStateOf(false) }
  var uploadProgress by remember { mutableFloatStateOf(0f) }
  var isPublishing by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  // Zero-permission Android Photo Picker -> Cloud Storage HTTPS upload
  val mediaPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isUploading = true
          uploadProgress = 0f
          isError = false
          statusMessage = strings.uploadingToCloud
          val folder =
            when (mode) {
              "story" -> "stories"
              "long" -> "videos"
              else -> "reels"
            }
          runCatching {
            CloudMediaStorage.uploadPickedMedia(context, uri, folder) { progress ->
              uploadProgress = progress
            }
          }
            .onSuccess { uploaded ->
              isUploading = false
              mediaUrl = uploaded.downloadUrl
              pickedIsVideo = uploaded.isVideo
              isError = false
              statusMessage = strings.cloudUploadReady
            }
            .onFailure { err ->
              isUploading = false
              isError = true
              statusMessage = err.localizedMessage ?: "Cloud upload failed"
            }
        }
      }
    }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(ObsidianVoid)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = strings.createTitle,
      style = MaterialTheme.typography.headlineMedium,
      color = ChampagneGold,
      fontWeight = FontWeight.Bold,
    )

    // Viral Boost Guarantee Banner
    Surface(
      color = CharcoalGlass,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Default.LocalFireDepartment, null, tint = MetallicGold, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(strings.viralBoostExplainer, style = MaterialTheme.typography.bodyMedium, color = ChampagneGold)
      }
    }

    // Mode Selector
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      listOf("reel" to strings.postTypeReel, "long" to strings.postTypeLong, "story" to strings.postTypeStory)
        .forEach { (key, label) ->
          FilterChip(
            selected = mode == key,
            onClick = { mode = key },
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

    // Pick From Mobile Button -> Uploads straight to Cloud Storage
    OutlinedButton(
      onClick = {
        val request =
          if (mode == "story") {
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
          } else {
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
          }
        mediaPicker.launch(request)
      },
      enabled = !isUploading && !isPublishing,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("pick_mobile_media_btn"),
    ) {
      Icon(Icons.Default.PermMedia, null, tint = ChampagneGold)
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text =
          if (isUploading) "${strings.uploadingToCloud} ${(uploadProgress * 100).toInt()}%"
          else strings.pickFromMobile,
        color = ChampagneGold,
        fontWeight = FontWeight.Bold,
      )
    }

    if (isUploading) {
      LinearProgressIndicator(
        progress = { uploadProgress.coerceIn(0.05f, 1f) },
        color = MetallicGold,
        trackColor = CharcoalGlass,
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
      )
    }

    OutlinedTextField(
      value = mediaUrl,
      onValueChange = {
        mediaUrl = it
        pickedIsVideo = CloudMediaStorage.isVideoUri(it) || mode != "story"
      },
      label = { Text(strings.orPasteVideoUrl) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("media_url_input"),
    )

    if (mediaUrl.isNotBlank()) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, HairlineGoldBorder, RoundedCornerShape(18.dp))
      ) {
        if (pickedIsVideo) {
          CryptoVideoPlayer(
            mediaUrl = mediaUrl,
            isActive = true,
            mutedByDefault = true,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          AsyncImage(
            model = mediaUrl,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }

    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      label = { Text(strings.videoTitleHint) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("create_title_input"),
    )

    if (mode != "story") {
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(strings.videoDescHint) },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = tagsText,
        onValueChange = { tagsText = it },
        label = { Text(strings.tagsHint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Crypto", "Tech", "Gaming", "Music").forEach { cat ->
          FilterChip(
            selected = category == cat,
            onClick = { category = cat },
            label = { Text(cat) },
          )
        }
      }
    }

    if (statusMessage != null) {
      Surface(
        color = (if (isError) CrimsonBan else EmeraldLive).copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = statusMessage.orEmpty(),
          color = if (isError) CrimsonBan else EmeraldLive,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    Button(
      onClick = {
        if (mediaUrl.isBlank() || title.isBlank()) {
          isError = true
          statusMessage = "${strings.videoTitleHint} & ${strings.pickFromMobile}"
          return@Button
        }
        if (!CloudMediaStorage.isHttpUrl(mediaUrl)) {
          isError = true
          statusMessage = strings.httpsUrlRequiredHint
          return@Button
        }
        isPublishing = true
        statusMessage = null
        isError = false
        if (mode == "story") {
          onPublishStory(
            mediaUrl,
            if (pickedIsVideo) "video" else "image",
            title,
            {
              isPublishing = false
              statusMessage = strings.publishSuccess
              onPublishedNavigateHome()
            },
            { err ->
              isPublishing = false
              isError = true
              statusMessage = err
            },
          )
        } else {
          val tags = tagsText.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
          onPublishVideo(
            mode,
            title,
            description,
            tags,
            category,
            mediaUrl,
            if (mode == "reel") 30L else 180L,
            {
              isPublishing = false
              statusMessage = strings.publishSuccess
              onPublishedNavigateHome()
            },
            { err ->
              isPublishing = false
              isError = true
              statusMessage = err
            },
          )
        }
      },
      enabled = !isPublishing && !isUploading,
      colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag("publish_now_button"),
    ) {
      if (isPublishing) {
        CryptoVaultLoader(size = 26.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(strings.publishing, fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Default.CloudUpload, null)
        Spacer(modifier = Modifier.width(10.dp))
        Text(strings.publishNow, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
