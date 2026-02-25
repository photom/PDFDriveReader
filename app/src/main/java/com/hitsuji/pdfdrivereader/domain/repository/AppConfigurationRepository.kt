package com.hitsuji.pdfdrivereader.domain.repository

import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.AppSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository for global application configuration and persistence state.
 */
interface AppConfigurationRepository {
    /**
     * Observes the current application session.
     */
    fun getSession(): Flow<AppSession>

    /**
     * Saves the current active mode.
     */
    suspend fun saveMode(mode: AppMode)

    /**
     * Saves the URI of the last opened document.
     */
    suspend fun saveLastUri(uri: String?)
}
