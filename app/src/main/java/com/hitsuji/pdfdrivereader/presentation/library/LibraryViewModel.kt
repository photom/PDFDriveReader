package com.hitsuji.pdfdrivereader.presentation.library

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.DomainResult
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.GetDocumentsUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncCloudLibraryUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncLocalLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Library screen.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val syncLocalLibraryUseCase: SyncLocalLibraryUseCase,
    private val syncCloudLibraryUseCase: SyncCloudLibraryUseCase,
    private val driveService: GoogleDriveService,
    private val appConfigRepository: AppConfigurationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    /**
     * Observable [StateFlow] representing if any synchronization is in progress.
     */
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val snackbarHostState = SnackbarHostState()

    val isGoogleAuthenticated: StateFlow<Boolean> = driveService.authState

    init {
        observeDocuments()
        observeAuthState()
        refreshLibrary()
        persistMode()
    }

    private fun persistMode() {
        viewModelScope.launch {
            appConfigRepository.saveMode(AppMode.LIBRARY)
            appConfigRepository.saveLastUri(null)
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            driveService.authState.collect { authenticated ->
                if (authenticated) {
                    refreshLibrary()
                }
            }
        }
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            getDocumentsUseCase().collect { documents ->
                _state.value = if (documents.isEmpty()) {
                    LibraryState.Empty
                } else {
                    LibraryState.Success(documents)
                }
            }
        }
    }

    fun getSignInIntent(): Any = driveService.getSignInIntent()

    fun onSignInResult(account: Any) {
        driveService.handleSignInResult(account)
    }

    fun signOut() {
        viewModelScope.launch {
            driveService.signOut()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val localResult = syncLocalLibraryUseCase()
                handleResult(localResult)

                if (isGoogleAuthenticated.value) {
                    val cloudResult = syncCloudLibraryUseCase()
                    handleResult(cloudResult)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun handleResult(result: DomainResult<Unit>) {
        if (result is DomainResult.Error) {
            viewModelScope.launch {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }
}
