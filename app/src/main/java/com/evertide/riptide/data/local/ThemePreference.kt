package com.evertide.riptide.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val THEME_KEY = stringPreferencesKey("theme")

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

@Singleton
class ThemePreferenceStore
@Inject
constructor(
        @ApplicationContext private val context: Context,
) {
    val theme: Flow<ThemePreference> =
            context.themeDataStore.data.map { prefs ->
                when (prefs[THEME_KEY]) {
                    "light" -> ThemePreference.LIGHT
                    "dark" -> ThemePreference.DARK
                    else -> ThemePreference.SYSTEM
                }
            }

    suspend fun setTheme(theme: ThemePreference) {
        context.themeDataStore.edit { it[THEME_KEY] = theme.name.lowercase() }
    }
}
