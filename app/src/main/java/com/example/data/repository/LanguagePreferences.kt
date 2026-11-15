package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cryptoPrefsDataStore by preferencesDataStore(name = "crypto_prefs")

class LanguagePreferences(private val context: Context) {
  private val languageKey = stringPreferencesKey("app_language")

  val languageFlow: Flow<AppLanguage> =
    context.cryptoPrefsDataStore.data.map { prefs ->
      when (prefs[languageKey]) {
        AppLanguage.EN.code -> AppLanguage.EN
        else -> AppLanguage.AR
      }
    }

  suspend fun setLanguage(language: AppLanguage) {
    context.cryptoPrefsDataStore.edit { prefs -> prefs[languageKey] = language.code }
  }
}
