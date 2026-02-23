# Functional Requirements

## Core Operations

### Mode & UI State Management

#### 1. Application Mode Persistence
The app must persist the active mode (**Library** vs. **Reader**) across sessions.
- **Library Mode Restore**: If the app is terminated while browsing the library, it must relaunch into the Library view.
- **Reader Mode Restore**: If the app is terminated while reading, it must relaunch directly into the Reader view for that document.

#### 2. Reader Mode UI Visibility (Immersive)
- **Default State**: UI overlay hidden.
- **Toggle Logic**: Single tap toggles the UI.
- **Back Navigation Logic**:
  - `onBackPressed` while **Link History** exists -> `Pop History & Jump back`.
  - `onBackPressed` while **UI is visible** (and history is empty) -> `Hide UI overlay`.
  - `onBackPressed` while **UI is hidden** (and history is empty) -> `Exit to Library Mode`.
- **Gesture Conflict**: The app must distinguish between a **Single Tap** (UI toggle) and a **Scroll/Pinch** (Navigation). Taps on links within the PDF must prioritize link navigation over UI toggling.

### Document Discovery & Library Management
The application must maintain an up-to-date list of PDF files from two primary sources:

#### 1. Local PDF Discovery
- **Scanning**: Periodically scan the device's public `Documents` and `Downloads` folders for `.pdf` files.
- **Display**: For each file, the app must retrieve and display the **File Name** and the **Relative Path** (e.g., `/Downloads/Invoices/`).
- **Permissions**: Use Scoped Storage APIs to access files without requesting legacy external storage permissions.

#### 2. Google Drive (Cloud) Discovery
- **Authentication Flow**:
  - The app must check for an active Google account session.
  - If unauthenticated, show a "Sign-In with Google" trigger in the Cloud tab.
  - Securely store OAuth2 tokens using Android KeyStore.
- **Background Processing**: Library synchronization must occur on a background thread (e.g., using `WorkManager` or a background service) to ensure the UI remains responsive.
- **Syncing Indicator**:
  - While a sync is in progress **and** the app is in **Library Mode**, a **Syncing Icon** must be displayed in the Top Bar.
  - If the app is in Reader Mode, the sync continues in the background silently.
- **Library Update**: Upon completion of the background sync, the SQLite cache and the Library Mode UI list must be automatically refreshed to show the updated file list.
- **Sync Trigger**: Pull-to-refresh or automatic periodic updates.

### Document Validation
Before opening any PDF file (from local or Drive), the app must validate the format:
1. **Invalid Format**: Show an error message (e.g., "The PDF file is invalid and cannot be opened.").
2. **Invalid Format**: Do not attempt to render the file.

### Required Permissions
- **Files and Media**: No special permissions are required on modern Android versions (using SAF and Scoped Storage).
- **Network**: Required for Google Drive API access and authentication.
- **Accounts**: Required for Google Sign-In and cloud synchronization.

### Per-Document Settings Persistence
The app must maintain a unique configuration for every document opened:
- **Reading Direction**: Persistent per-file. Default to `L-to-R`.
- **Zoom Level**: Persistent per-file. Default to `100%`.
- **Page Index**: Persistent per-file.

### SQLite Cache Schema
| Column | Description |
| --- | --- |
| `file_uri` | Persistent URI or Drive ID. |
| `file_name` | The name of the document. |
| `current_page` | The index of the last displayed page. |
| `reading_direction` | String (LTR, RTL, TTB). |
| `zoom_level` | Float (e.g., 1.5 for 150%). |
| `last_modified` | Timestamp for sorting. |
