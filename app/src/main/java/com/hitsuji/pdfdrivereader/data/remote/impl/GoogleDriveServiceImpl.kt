package com.hitsuji.pdfdrivereader.data.remote.impl

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [GoogleDriveService] using Google Play Services.
 * 
 * @property context Application context for accessing Google SignIn.
 */
@Singleton
class GoogleDriveServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GoogleDriveService {

    private val _authState = MutableStateFlow(false)
    override val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private var driveService: Drive? = null

    init {
        checkExistingAccount()
    }

    private fun checkExistingAccount() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            Log.d("PDFDriveReader", "Found existing account: ${account.email}")
            initializeDriveService(account)
            _authState.value = true
        } else {
            Log.d("PDFDriveReader", "No existing account found")
        }
    }

    private fun initializeDriveService(account: GoogleSignInAccount) {
        Log.d("PDFDriveReader", "Initializing Drive Service for ${account.email}")
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_READONLY)
        )
        credential.selectedAccount = account.account
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("PDFDriveReader").build()
    }

    override fun getSignInIntent(): android.content.Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    override fun handleSignInResult(account: Any) {
        if (account is GoogleSignInAccount) {
            Log.d("PDFDriveReader", "Handling sign-in result for ${account.email}")
            initializeDriveService(account)
            _authState.value = true
        } else {
            Log.e("PDFDriveReader", "handleSignInResult called with invalid account type: ${account.javaClass.simpleName}")
        }
    }

    override suspend fun signOut() {
        Log.d("PDFDriveReader", "Signing out...")
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        driveService = null
        _authState.value = false
    }

    override suspend fun listFiles(): List<DocumentMetadata> = withContext(Dispatchers.IO) {
        val service = driveService ?: run {
            Log.w("PDFDriveReader", "listFiles called but driveService is null")
            return@withContext emptyList()
        }
        
        try {
            Log.d("PDFDriveReader", "Querying Google Drive files...")
            val result = service.files().list()
                .setQ("mimeType = 'application/pdf' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, parents)")
                .execute()

            Log.d("PDFDriveReader", "Found ${result.files.size} PDF files on Drive")
            result.files.map { file ->
                DocumentMetadata(
                    id = file.id,
                    fileName = file.name,
                    locationPath = "Google Drive",
                    source = SourceType.GOOGLE_DRIVE
                )
            }
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "Error listing Drive files", e)
            emptyList()
        }
    }

    override suspend fun downloadFile(fileId: String, destination: String) = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext
        Log.d("PDFDriveReader", "Downloading file $fileId to $destination")
        val outputStream = FileOutputStream(destination)
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
    }
}
