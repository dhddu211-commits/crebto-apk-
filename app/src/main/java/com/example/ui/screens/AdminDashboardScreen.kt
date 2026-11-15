package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.PublicUser
import com.example.data.model.StoryItem
import com.example.data.model.VideoPost
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.AmberWarn
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
fun AdminDashboardScreen(
  allVideos: List<VideoPost>,
  allStories: List<StoryItem>,
  allUsers: List<PublicUser>,
  allChats: List<ChatThread>,
  inspectedChatId: String?,
  inspectedMessages: List<ChatMessage>,
  onToggleVideoBan: (VideoPost, String) -> Unit,
  onToggleStoryBan: (StoryItem, String) -> Unit,
  onToggleUserBan: (PublicUser, String) -> Unit,
  onToggleUserVerified: (PublicUser) -> Unit,
  onToggleChatBan: (ChatThread, String) -> Unit,
  onInspectChat: (String?) -> Unit,
  onSendDirectAlert: (targetUserId: String, title: String, message: String, severity: String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var alertTargetUser by remember { mutableStateOf<Pair<String, String>?>(null) } // uid to name

  Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Default.AdminPanelSettings, null, tint = MetallicGold, modifier = Modifier.size(28.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(strings.adminTitle, style = MaterialTheme.typography.titleLarge, color = ChampagneGold, fontWeight = FontWeight.Bold)
        Text("dhddu211@gmail.com", style = MaterialTheme.typography.labelSmall, color = EmeraldLive)
      }
    }

    val tabs = listOf(strings.adminOverview, strings.adminContent, strings.adminUsers, strings.adminChats)
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
      tabs.forEachIndexed { idx, label ->
        Tab(
          selected = selectedTab == idx,
          onClick = { selectedTab = idx },
          text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
      }
    }

    when (selectedTab) {
      0 -> {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
              AdminMetricCard("Users", allUsers.size.toString(), "${allUsers.count { it.isBanned }} banned", Modifier.weight(1f))
              AdminMetricCard("Videos", allVideos.size.toString(), "${allVideos.count { it.isBanned }} banned", Modifier.weight(1f))
            }
          }
          item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
              AdminMetricCard("Stories", allStories.size.toString(), "${allStories.count { it.isBanned }} banned", Modifier.weight(1f))
              AdminMetricCard("Chats", allChats.size.toString(), "${allChats.count { it.isBanned }} banned", Modifier.weight(1f))
            }
          }
        }
      }

      1 -> {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Text(strings.tabVideos, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
          }
          items(allVideos, key = { "adm_v_${it.id}" }) { video ->
            Surface(
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (video.isBanned) CrimsonBan else HairlineGoldBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(video.title, color = WarmIvory, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                  Text("${video.authorName} • ${video.videoType}", color = MutedSand, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                  onClick = { onToggleVideoBan(video, "Admin moderation") },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = if (video.isBanned) EmeraldLive else CrimsonBan,
                      contentColor = WarmIvory,
                    ),
                  modifier = Modifier.testTag("admin_toggle_video_${video.id}"),
                ) {
                  Text(if (video.isBanned) strings.unbanVideo else strings.banVideo)
                }
              }
            }
          }

          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings.storiesTitle, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
          }
          items(allStories, key = { "adm_s_${it.id}" }) { story ->
            Surface(
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (story.isBanned) CrimsonBan else HairlineGoldBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(story.authorName, color = WarmIvory, style = MaterialTheme.typography.titleMedium)
                  Text(story.caption.ifBlank { story.mediaType }, color = MutedSand, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Button(
                  onClick = { onToggleStoryBan(story, "Admin moderation") },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = if (story.isBanned) EmeraldLive else CrimsonBan,
                      contentColor = WarmIvory,
                    ),
                ) {
                  Text(if (story.isBanned) strings.unbanStory else strings.banStory)
                }
              }
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(allUsers, key = { "adm_u_${it.userId}" }) { user ->
            Surface(
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (user.isBanned) CrimsonBan else HairlineGoldBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, color = WarmIvory, style = MaterialTheme.typography.titleMedium)
                    Text("@${user.username}", color = ChampagneGold, style = MaterialTheme.typography.bodyMedium)
                  }
                  if (user.isVerified) {
                    Icon(Icons.Default.Verified, null, tint = MetallicGold)
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(onClick = { onToggleUserVerified(user) }) {
                    Icon(Icons.Default.CheckCircle, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.verifyUser, color = ChampagneGold)
                  }
                  OutlinedButton(onClick = { alertTargetUser = user.userId to user.displayName }) {
                    Icon(Icons.Default.NotificationsActive, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.sendAlert, color = AmberWarn)
                  }
                  Button(
                    onClick = { onToggleUserBan(user, "Policy violation") },
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = if (user.isBanned) EmeraldLive else CrimsonBan,
                        contentColor = WarmIvory,
                      ),
                  ) {
                    Text(if (user.isBanned) strings.unbanUser else strings.banUser)
                  }
                }
              }
            }
          }
        }
      }

      3 -> {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(allChats, key = { "adm_c_${it.id}" }) { chat ->
            Surface(
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (chat.isBanned) CrimsonBan else HairlineGoldBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = "${chat.participant1Name} ↔ ${chat.participant2Name}",
                  color = ChampagneGold,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = chat.lastMessage.ifBlank { "..." },
                  color = MutedSand,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 2,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = { onInspectChat(if (inspectedChatId == chat.id) null else chat.id) },
                  ) {
                    Icon(Icons.Default.Visibility, null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.inspectMessages, color = ChampagneGold)
                  }
                  Button(
                    onClick = { onToggleChatBan(chat, "Illegal conversation detected by Admin") },
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = if (chat.isBanned) EmeraldLive else CrimsonBan,
                        contentColor = WarmIvory,
                      ),
                  ) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (chat.isBanned) strings.unbanChat else strings.banChat)
                  }
                }

                if (inspectedChatId == chat.id) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(
                    color = MidnightOnyx,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (inspectedMessages.isEmpty()) {
                        Text(strings.noMessagesYet, color = MutedSand)
                      } else {
                        inspectedMessages.forEach { msg ->
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Text(
                              text = "${msg.senderName}: ${msg.text}",
                              color = WarmIvory,
                              style = MaterialTheme.typography.bodyMedium,
                              modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { alertTargetUser = msg.senderId to msg.senderName }) {
                              Text(strings.sendAlert, color = AmberWarn)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  alertTargetUser?.let { (uid, name) ->
    SendDirectAlertDialog(
      targetName = name,
      onDismiss = { alertTargetUser = null },
      onSend = { title, body, severity ->
        onSendDirectAlert(uid, title, body, severity)
        alertTargetUser = null
      },
    )
  }
}

@Composable
private fun AdminMetricCard(
  title: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = CharcoalGlass,
    shape = RoundedCornerShape(18.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, color = MutedSand, style = MaterialTheme.typography.labelLarge)
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, color = ChampagneGold, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
      Text(subtitle, color = CrimsonBan, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
private fun SendDirectAlertDialog(
  targetName: String,
  onDismiss: () -> Unit,
  onSend: (title: String, body: String, severity: String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }
  var severity by remember { mutableStateOf("warning") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MidnightOnyx,
    titleContentColor = ChampagneGold,
    title = { Text("${strings.sendAlert}: $targetName") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.alertTitleHint) },
          singleLine = true,
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          label = { Text(strings.alertBodyHint) },
          minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("warning", "critical", "ban_notice").forEach { sev ->
            FilterChip(
              selected = severity == sev,
              onClick = { severity = sev },
              label = { Text(sev) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && body.isNotBlank()) {
            onSend(title, body, severity)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
      ) {
        Text(strings.send, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel, color = MutedSand) }
    },
  )
}
