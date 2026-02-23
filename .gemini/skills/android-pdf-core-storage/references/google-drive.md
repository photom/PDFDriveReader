# Google Drive API for Android

## Authentication (Google Identity)
Use the **Google Identity Services (GIS)** to get an authorization token:
1. **Scopes**: `https://www.googleapis.com/auth/drive.readonly` or `https://www.googleapis.com/auth/drive.file`
2. **Credential Manager**: The modern way to handle authentication.
3. **GoogleSignInOptions**: The legacy but still common way to get an account.

## Drive API Library
Use `com.google.apis:google-api-services-drive:v3`.

```kotlin
val driveService = Drive.Builder(
    AndroidHttp.newCompatibleTransport(),
    GsonFactory(),
    googleAccountCredential
).setApplicationName("PDFDriveReader").build()
```

## Listing and Searching
Use `driveService.files().list()` with a `q` parameter to find PDF files:
`"mimeType = 'application/pdf' and trashed = false"`

## Reading Files (Direct Download)
For local rendering, download the file into a temporary `File` object and then open its `ParcelFileDescriptor`.

```kotlin
val outputStream = FileOutputStream(tempFile)
driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
```

## Progressive Reading
For large files, use **MediaHttpDownloader** with **Chunked Download** or byte-range requests if supported by the server/client.

## Syncing Metadata
Storing a `driveFileId` in your local database alongside the `safUri` allows the app to link local copies with their cloud sources.
