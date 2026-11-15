package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAlert
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MidnightOnyx
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory

@Composable
fun ChatsAndAlertsScreen(
  currentUserId: String,
  chats: List<ChatThread>,
  activeChat: ChatThread?,
  activeMessages: List<ChatMessage>,
  alerts: List<AdminAlert>,
  onSelectChat: (ChatThread?) -> Unit,
  onSendMessage: (ChatThread, String) -> Unit,
  onMarkAlertRead: (String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  var selectedTab by remember { mutableIntStateOf(0) }

  if (activeChat != null) {
    BackHandler { onSelectChat(null) }
    ChatThreadView(
      currentUserId = currentUserId,
      chat = activeChat,
      messages = activeMessages,
      onBack = { onSelectChat(null) },
      onSendMessage = { text -> onSendMessage(activeChat, text) },
    )
    return
  }

  Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    val unreadCount = alerts.count { !it.isRead }
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
        text = { Text(strings.chatsTab, fontWeight = FontWeight.Bold) },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Text(
            text = if (unreadCount > 0) "${strings.alertsTab} ($unreadCount)" else strings.alertsTab,
            fontWeight = FontWeight.Bold,
          )
        },
      )
    }

    if (selectedTab == 0) {
      if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(strings.noChatsYet, color = MutedSand, style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(chats, key = { it.id }) { chat ->
            val peerName =
              if (chat.participantIds.firstOrNull() == currentUserId) chat.participant2Name
              else chat.participant1Name
            Surface(
              onClick = { onSelectChat(chat) },
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border =
                androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (chat.isBanned) CrimsonBan else HairlineGoldBorder,
                ),
              modifier = Modifier.fillMaxWidth().testTag("chat_item_${chat.id}"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = if (chat.isBanned) Icons.Default.Block else Icons.AutoMirrored.Filled.Chat,
                  contentDescription = null,
                  tint = if (chat.isBanned) CrimsonBan else ChampagneGold,
                  modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(peerName, color = WarmIvory, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = if (chat.isBanned) strings.chatBannedBanner else chat.lastMessage.ifBlank { "..." },
                    color = if (chat.isBanned) CrimsonBan else MutedSand,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }
    } else {
      if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(strings.noAlertsYet, color = MutedSand, style = MaterialTheme.typography.bodyLarge)
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(alerts, key = { it.id }) { alert ->
            val tint =
              when (alert.severity) {
                "critical", "ban_notice" -> CrimsonBan
                else -> AmberWarn
              }
            Surface(
              onClick = { if (!alert.isRead) onMarkAlertRead(alert.id) },
              color = CharcoalGlass,
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.7f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(
                  imageVector = if (alert.severity == "warning") Icons.Default.Warning else Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = tint,
                  modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(alert.title, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(alert.message, color = WarmIvory, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatThreadView(
  currentUserId: String,
  chat: ChatThread,
  messages: List<ChatMessage>,
  onBack: () -> Unit,
  onSendMessage: (String) -> Unit,
) {
  val strings = LocalCryptoStrings.current
  var input by remember { mutableStateOf("") }
  val peerName =
    if (chat.participantIds.firstOrNull() == currentUserId) chat.participant2Name
    else chat.participant1Name

  Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(MidnightOnyx).padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.close, tint = WarmIvory)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(peerName, style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
    }

    if (chat.isBanned) {
      Surface(
        color = CrimsonBan.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Block, null, tint = CrimsonBan)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${strings.chatBannedBanner}${if (chat.banReason.isNotBlank()) ": ${chat.banReason}" else ""}",
            color = CrimsonBan,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == currentUserId
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
          Surface(
            color = if (isMine) MetallicGold else CharcoalGlass,
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(
              text = msg.text,
              color = if (isMine) ObsidianVoid else WarmIvory,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
          }
        }
      }
    }

    if (!chat.isBanned) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MidnightOnyx).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = input,
          onValueChange = { input = it },
          placeholder = { Text(strings.typeMessageHint) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (input.isNotBlank()) {
              onSendMessage(input)
              input = ""
            }
          },
          modifier = Modifier.size(48.dp).clip(CircleShape).background(MetallicGold),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, strings.send, tint = ObsidianVoid)
        }
      }
    }
  }
}
