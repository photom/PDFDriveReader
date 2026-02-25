package com.hitsuji.pdfdrivereader.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.AppSession
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * DataStore-backed implementation of [AppConfigurationRepository].
 */
@Singleton
class AppConfigurationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppConfigurationRepository {

    private val KEY_MODE = stringPreferencesKey("last_mode")
    private val KEY_URI = stringPreferencesKey("last_uri")

    override fun getSession(): Flow<AppSession> {
        return context.dataStore.data.map { prefs ->
            val modeStr = prefs[KEY_MODE] ?: AppMode.LIBRARY.name
            AppSession(
                lastMode = AppMode.valueOf(modeStr),
                lastUri = prefs[KEY_URI]
            )
        }
    }

    override suspend fun saveMode(mode: AppMode): Unit = withContext(Dispatchers.IO) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODE] = mode.name
        }
    }

    override suspend fun saveLastUri(uri: String?): Unit = withContext(Dispatchers.IO) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[KEY_URI] = uri
            } else {
                prefs.remove(KEY_URI)
            }
        }
    }
}
