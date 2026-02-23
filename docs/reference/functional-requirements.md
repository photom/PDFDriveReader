# Functional Requirements

## Core Operations

### Mode & UI State Management

#### 1. Application Mode Persistence
The app must persist the active mode (**Library** vs. **Reader**) across sessions.
- **Library Mode Restore**: If the app is terminated while browsing the library, it must relaunch into the Library view (local or cloud tab as previously selected).
- **Reader Mode Restore**: If the app is terminated while reading a document, it must relaunch directly into the Reader view for that document, restoring the exact page and zoom level.

#### 2. Reader Mode UI Visibility (Immersive)
To provide a distraction-free experience:
- **Default State**: Upon opening a document (either via selection or restore), the UI overlay (Menu button, Page indicators) must be hidden.
- **Toggle Logic**:
  - If the UI is **hidden**: A single tap on the display area must **show** the UI.
  - If the UI is **visible**: A single tap on the display area (outside of menu interactive elements) must **hide** the UI.
- **Timeout**: (Optional) The UI overlay may automatically hide after 5 seconds of inactivity.
- **Gesture Conflict**: The app must distinguish between a **Single Tap** (UI toggle) and a **Scroll/Pinch** (Navigation). Taps on links within the PDF must prioritize link navigation over UI toggling.

### Document Discovery & Library Management
The application must maintain an up-to-date list of PDF files from two primary sources:

#### 1. Local PDF Discovery
- **Scanning**: Periodically scan the device's public `Documents` and `Downloads` folders for `.pdf` files.
- **Display**: For each file, the app must retrieve and display the **File Name** and the **Relative Path** (e.g., `/Downloads/Invoices/`).
- **Permissions**: Use Scoped Storage APIs to access files without requesting legacy external storage permissions.

#### 2. Google Drive (Cloud) Discovery
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

### SQLite Cache Schema
| Column | Description |
| --- | --- |
| `file_path` | Absolute path or URI to the PDF file. |
| `file_name` | The name of the document. |
| `current_page` | The index of the last displayed page. |
| `last_modified` | Timestamp for sorting recently opened files. |
