package com.hitsuji.pdfdrivereader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.pdfdrivereader.domain.model.AppSession
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for managing the main application state and initial navigation.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appConfigRepository: AppConfigurationRepository
) : ViewModel() {

    /**
     * Observable state of the initial application session.
     */
    val session: StateFlow<AppSession?> = appConfigRepository.getSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
