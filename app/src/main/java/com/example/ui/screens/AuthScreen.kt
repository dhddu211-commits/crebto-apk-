package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthManager
import com.example.ui.components.CryptoVaultLoader
import com.example.ui.strings.LocalCryptoStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalGlass
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.HairlineGoldBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmIvory

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onToggleLanguage: () -> Unit,
) {
  val strings = LocalCryptoStrings.current
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Image(
      painter = painterResource(id = R.drawable.img_auth_hero),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  ObsidianVoid.copy(alpha = 0.55f),
                  ObsidianVoid.copy(alpha = 0.85f),
                  ObsidianVoid,
                )
            )
          )
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        OutlinedButton(
          onClick = onToggleLanguage,
          modifier = Modifier.testTag("auth_language_toggle"),
          shape = RoundedCornerShape(50),
          border = androidx.compose.foundation.BorderStroke(1.dp, HairlineGoldBorder),
        ) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = strings.switchLanguage,
            tint = ChampagneGold,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.switchLanguage, color = ChampagneGold)
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 520.dp),
      ) {
        CryptoVaultLoader(size = 96.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = strings.appName,
          style = MaterialTheme.typography.displayLarge,
          color = ChampagneGold,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = strings.tagline,
          style = MaterialTheme.typography.bodyLarge,
          color = WarmIvory,
          textAlign = TextAlign.Center,
        )
      }

      Column(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (errorMsg != null) {
          Surface(
            color = CrimsonBan.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          ) {
            Text(
              text = errorMsg.orEmpty(),
              color = CrimsonBan,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Button(
          onClick = {
            errorMsg = null
            isLoading = true
            GoogleAuthManager.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMsg = msg
              },
              scope = coroutineScope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MetallicGold,
              contentColor = ObsidianVoid,
            ),
          shape = RoundedCornerShape(18.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CryptoVaultLoader(size = 28.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(strings.signingIn, fontWeight = FontWeight.Bold)
          } else {
            Icon(
              imageVector = Icons.Default.Login,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = strings.signInWithGoogle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(CharcoalGlass.copy(alpha = 0.8f))
              .border(1.dp, HairlineGoldBorder, RoundedCornerShape(16.dp))
              .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = MetallicGold,
              modifier = Modifier.size(18.dp).padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = strings.authExtraProvidersNote,
              style = MaterialTheme.typography.bodyMedium,
              color = MutedSand,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}
