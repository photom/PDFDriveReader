# Local Storage Sync via SAF (Storage Access Framework)

## 1. Overview
To comply with Scoped Storage requirements on Android 13+ while providing an "automatic" feel, the application allows users to grant access to specific directories (e.g., "Documents", "Downloads", or the entire Internal Storage root) once. The application then maintains persistent access to these "Sync Roots" and automatically discovers PDF files within them.

## 2. Behavioral Specifications

### 2.1 Directory & File Selection
- **Action**: User clicks "Add Folder" or "Add Files".
- **Mechanism (Folders)**: The app launches `ACTION_OPEN_DOCUMENT_TREE` for recursive scanning.
- **Mechanism (Files)**: The app launches `ACTION_OPEN_DOCUMENT` with `EXTRA_ALLOW_MULTIPLE` and a `application/pdf` MIME filter.
- **Permission**: The app calls `contentResolver.takePersistableUriPermission` for all picked URIs (trees or files).

### 2.2 Automatic Discovery & Registration
- **Recursive Scan (Trees)**: The application uses `DocumentFile.fromTreeUri()` to recursively traverse granted directories.
- **Direct Registration (Files)**: Files picked via `ACTION_OPEN_DOCUMENT` are immediately added to the local database.
- **Persistence**: Picked URIs are stored, ensuring they appear in the library even after the app is restarted.

### 2.3 Persistence
- **Storage**: The granted Tree URIs are stored in the `AppConfigurationRepository` (DataStore or Room).
- **De-duplication**: Files are tracked by their unique SAF URI to prevent duplicates in the UI.

## 3. Technical Strategy

### 3.1 Domain Layer
- **New Entity**: `SyncDirectory` (id, uri, name).
- **Use Case**: `AddSyncDirectoryUseCase`, `SyncLocalLibraryUseCase` (updated to loop through stored directories).

### 3.2 Data Layer
- **LocalFileScanner**: Updated to accept `Uri` and use `DocumentFile` instead of `java.io.File`.
- **Repository**: Store picked URIs in `AppConfigurationRepository`.

### 3.3 UI Layer
- **LibraryScreen**: Replace "Pick File" logic with "Add Folder" to enable the automatic scanning of entire sub-trees.
