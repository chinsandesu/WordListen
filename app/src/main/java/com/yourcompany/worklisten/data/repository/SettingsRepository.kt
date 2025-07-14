package com.yourcompany.worklisten.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
    }

    val backgroundImageUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKGROUND_IMAGE_URI]
        }

    suspend fun saveBackgroundImageUri(uri: String?) {
        context.dataStore.edit { settings ->
            if (uri != null) {
                settings[PreferencesKeys.BACKGROUND_IMAGE_URI] = uri
            } else {
                settings.remove(PreferencesKeys.BACKGROUND_IMAGE_URI)
            }
        }
    }
} 