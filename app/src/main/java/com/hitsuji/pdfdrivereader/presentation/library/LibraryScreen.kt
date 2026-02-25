package com.hitsuji.pdfdrivereader.presentation.library

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import com.hitsuji.pdfdrivereader.presentation.theme.PdfDriveReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onDocumentClick: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isAuthenticated by viewModel.isGoogleAuthenticated.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Local Storage", "Google Drive")

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshLibrary()
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("PDFDriveReader", "authLauncher callback triggered! result=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("PDFDriveReader", "Auth Success: ${account.email}")
                viewModel.onSignInResult(account)
            } catch (e: ApiException) {
                Log.e("PDFDriveReader", "Auth Failed: StatusCode=${e.statusCode}", e)
            }
        } else {
            Log.w("PDFDriveReader", "Auth Cancelled or Failed")
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(viewModel.snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    IconButton(onClick = { /* TODO: Implement Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val isDriveTab = selectedTab == 1
                if (isDriveTab && !isAuthenticated) {
                    DriveAuthWall(
                        onSignInClick = {
                            val intent = viewModel.getSignInIntent() as Intent
                            authLauncher.launch(intent)
                        }
                    )
                } else {
                    LibraryContent(
                        state = state,
                        selectedTab = selectedTab,
                        onDocumentClick = onDocumentClick
                    )
                }
            }
        }
    }
}

@Composable
fun DriveAuthWall(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Access your cloud PDFs", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignInClick) {
            Text("Sign in with Google")
        }
    }
}

@Composable
fun LibraryContent(
    state: LibraryState,
    selectedTab: Int,
    onDocumentClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is LibraryState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is LibraryState.Empty -> {
                Text(
                    "No PDFs found",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LibraryState.Success -> {
                val filteredDocs = state.documents.filter { doc ->
                    if (selectedTab == 0) doc.source == SourceType.LOCAL_STORAGE
                    else doc.source == SourceType.GOOGLE_DRIVE
                }
                
                if (filteredDocs.isEmpty()) {
                    Text(
                        "No documents in this section",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    DocumentList(
                        documents = filteredDocs,
                        onDocumentClick = onDocumentClick
                    )
                }
            }
            is LibraryState.Error -> {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun DocumentList(
    documents: List<DocumentMetadata>,
    onDocumentClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(documents) { doc ->
            DocumentItem(doc, onDocumentClick)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun DocumentItem(
    document: DocumentMetadata,
    onDocumentClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDocumentClick(document.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.fileName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = document.locationPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (document.source == SourceType.GOOGLE_DRIVE && document.isCached) {
            Icon(
                Icons.Default.FileDownloadDone,
                contentDescription = "Available Offline",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    val docs = listOf(
        DocumentMetadata("1", "Clean_Architecture.pdf", "/Documents", SourceType.LOCAL_STORAGE, isCached = true),
        DocumentMetadata("2", "Design_Patterns.pdf", "Manga", SourceType.GOOGLE_DRIVE, isCached = true),
        DocumentMetadata("3", "Cloud_Native.pdf", "Books", SourceType.GOOGLE_DRIVE, isCached = false)
    )
    PdfDriveReaderTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(title = { Text("My Library") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("Local Storage") })
                    Tab(selected = false, onClick = {}, text = { Text("Google Drive") })
                }
                DocumentList(documents = docs, onDocumentClick = {})
            }
        }
    }
}
