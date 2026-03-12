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
import com.hitsuji.pdfdrivereader.domain.usecase.SearchLibraryUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncCloudLibraryUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncLocalLibraryUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.AddLocalPdfsUseCase
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
    private val searchLibraryUseCase: SearchLibraryUseCase,
    private val addLocalPdfsUseCase: AddLocalPdfsUseCase,
    private val driveService: GoogleDriveService,
    private val appConfigRepository: AppConfigurationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(1) // Default to Google Drive (1)
    /**
     * Observable [StateFlow] representing the currently selected tab.
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    /**
     * Observable [StateFlow] representing if any synchronization is in progress.
     */
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _expandedDirectory = MutableStateFlow<String?>(null)
    val expandedDirectory: StateFlow<String?> = _expandedDirectory.asStateFlow()

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
                Log.d("PDFDriveReader", "Auth state changed: authenticated=$authenticated")
                if (authenticated) {
                    refreshLibrary()
                }
            }
        }
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            getDocumentsUseCase().combine(_searchQuery) { documents, query ->
                searchLibraryUseCase(documents, query)
            }.collect { filteredDocuments ->
                Log.d("PDFDriveReader", "Observed ${filteredDocuments.size} documents from repository")
                _state.value = if (filteredDocuments.isEmpty()) {
                    LibraryState.Empty
                } else {
                    LibraryState.Success(filteredDocuments)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getSignInIntent(): Any = driveService.getSignInIntent()

    fun onSignInResult(account: Any) {
        Log.d("PDFDriveReader", "onSignInResult processing...")
        driveService.handleSignInResult(account)
    }

    fun signOut() {
        viewModelScope.launch {
            driveService.signOut()
        }
    }

    /**
     * Updates the currently selected tab index.
     */
    fun onTabSelected(index: Int) {
        _selectedTab.value = index
        _expandedDirectory.value = null
    }

    fun onDirectoryTapped(directory: String) {
        _expandedDirectory.value = if (_expandedDirectory.value == directory) null else directory
    }

    fun addSyncDirectory(uri: String) {
        viewModelScope.launch {
            appConfigRepository.addSyncDirectory(uri)
            refreshLibrary()
        }
    }

    fun addManualSyncFiles(uris: List<String>) {
        viewModelScope.launch {
            addLocalPdfsUseCase(uris)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                Log.d("PDFDriveReader", "Refreshing local library...")
                val localResult = syncLocalLibraryUseCase()
                handleResult(localResult)

                if (isGoogleAuthenticated.value) {
                    Log.d("PDFDriveReader", "Refreshing cloud library...")
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
            Log.e("PDFDriveReader", "Operation failed: ${result.message}", result.error)
            viewModelScope.launch {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }
}
