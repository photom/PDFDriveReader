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
- **Scanning**: Periodically scan the device using the `MediaStore` API. The app queries both the `MediaStore.Files` and `MediaStore.Downloads` collections to discover `.pdf` files across public directories.
- **Display**: For each file, the app must retrieve and display the **File Name** and the **Relative Path** (e.g., `/Downloads/Invoices/`).
- **Permissions & Limitations**: The app relies on `READ_EXTERNAL_STORAGE` for broader access on older Android versions. On modern Android versions (API 30+), Scoped Storage policies restrict the `MediaStore` from returning non-media files (like PDFs) created by other applications. For full visibility of external PDFs on API 30+, future implementations should adopt SAF folder selection (`ACTION_OPEN_DOCUMENT_TREE`) or consider requesting `MANAGE_EXTERNAL_STORAGE`.

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
...

### Resource Management (PdfDocument Lifecycle)
To prevent memory leaks and file handle exhaustion:
- **Renderer Scope**: The `PdfRenderer` and its `ParcelFileDescriptor` must be strictly managed by the `ReaderViewModel`.
- **Release Condition**: Resources must be released (closed) when:
  - The user exits Reader Mode.
  - The app is moved to the background for an extended period (via `onCleared` or `Lifecycle` observers).
  - A new document is opened, replacing the current one.
- **Safety**: Use `try-finally` blocks or Kotlin's `.use {}` extension to ensure `close()` is called even if rendering fails.

### Cover Detection & View Mode
Some PDFs contain covers (first and/or last page) that differ in physical dimensions from the rest of the document's pages.
- **Cover Detection Logic**: Upon initialization, the app must load the dimensions of every page. If the first page and/or the last page has a different size compared to the majority (the rest) of the pages, they are marked as covers.
- **View Modes**: 
  - **w/o cover mode**: The reader skips rendering the cover pages and only displays the non-cover pages.
  - **w/ cover mode**: The reader displays all pages in the document.
- **Toggle Mechanism**: A toggle menu in the Reader UI allows the user to switch between these modes dynamically.

### Cloud File Materialization (Downloading)
Since `PdfRenderer` requires local file access, documents from Google Drive must be "materialized" before viewing:
- **Download Cache**: Cloud files must be downloaded to the app's internal `cacheDir/cloud_docs/` directory.
- **On-Demand Loading**: Downloading should be triggered when the user selects a cloud document in the Library.
- **State Feedback**: The UI must show a "Downloading..." progress indicator during this phase.
- **Persistence**: Materialized files should be kept in the cache until the space is needed, using the `file_uri` (Drive ID) as the filename.
- **Cache Tracking**: The Library list must dynamically check for the existence of the cached file and mark the item as "Cached" in the UI.

### Sliding Page Cache & Job Management
To ensure a smooth swiping experience and optimal resource usage:
- **Sliding Window**: The app must maintain a 5-page cache window `[Current-2, Current-1, Current, Current+1, Current+2]`.
- **Pre-emptive Caching**: When the current page changes, the app must automatically trigger background rendering for the new neighbors to avoid showing loading spinners during normal reading.
- **Job Cancellation**: If a user swiping causes multiple page changes in rapid succession, any active background caching job must be explicitly cancelled before starting a new one.
- **Exception Safety**: The system must distinguish between fatal rendering errors and expected job cancellations during navigation.

### High-Zoom Rendering (Tiling Strategy)
To support 100%-500% zoom without `OutOfMemory` errors:
- **Dynamic Scaling**: Render the page bitmap at the current zoom level's resolution.
- **Tiling**: For zoom levels > 200%, the renderer must split the page into **tiles** (e.g., 512x512px) and only render the tiles currently visible in the viewport.
- **Bitmap Pooling**: Reuse `Bitmap` objects using a `BitmapPool` to reduce Garbage Collection (GC) pressure.

### Thumbnail Generation
For the Library Mode UI:
- **Lazy Generation**: Generate a 128dp x 128dp thumbnail of the first page when a document is first discovered.
- **Disk Cache**: Store thumbnails in the app's internal `cache` directory, indexed by the `file_uri`.
- **Cleanup**: Delete thumbnails if the document is removed from the SQLite cache.

### Search & Filtering Logic
The Library search functionality must follow these rules:
- **Scope**: Filters the currently active tab list (Local or Cloud).
- **Matching**: Case-insensitive partial matching against both the **File Name** and the **Location Path**.
- **Performance**: Filtering must be performed in real-time on the UI thread for small lists, or debounced (300ms) and offloaded to a background thread for lists exceeding 100 items.

### SQLite Cache Schema

#### Document Table (`DOCUMENT_METADATA`)
| Column | Description |
| --- | --- |
| `file_uri` | Persistent URI or Drive ID (Primary Key). |
| `file_name` | The name of the document. |
| `current_page` | The index of the last displayed page. |
| `reading_direction` | String (LTR, RTL, TTB). |
| `zoom_level` | Float (e.g., 1.5 for 150%). |
| `last_modified` | Timestamp for sorting recently opened files. |

#### Navigation History Table (`NAV_HISTORY`)
| Column | Description |
| --- | --- |
| `file_uri` | Foreign Key to DOCUMENT_METADATA. |
| `page_index` | The page the user was on. |
| `timestamp` | For stack ordering (LIFO). |

### Error Propagation & Diagnostics
To facilitate troubleshooting and robust state management:
- **Logging Level**: All critical state changes (Auth, Sync Start/End, File Open) must be logged at the `DEBUG` level using the tag "PDFDriveReader".
- **Uncaught Failures**: Exceptions in background coroutines must be caught and logged at the `ERROR` level with stack traces.
- **State Integrity**: If a Google Sign-In result is received but the internal `driveService` fails to initialize, the `authState` must be explicitly reset to `false` to avoid a "stuck" UI state.
- **Result Types**: Repositories and UseCases must return a `Result<T>` or a sealed `Outcome` class rather than throwing exceptions, allowing the ViewModel to update the UI error state.

### Text Selection & Copy (Android 15+)
- **Availability**: The application must support text selection and copying natively using `PdfRenderer.Page.selectContent()` or `getTextContents()` on Android 15 (API 35) and above.
- **Graceful Degradation**: On devices running Android 14 or lower (API < 35), text selection features should be safely disabled or gracefully fallback without crashing, since the `PdfRenderer` APIs for text extraction are not available.
- **Behavior**: Long-pressing on text should trigger a selection mode. Visual drag handles must appear at the start and end of the selection, allowing the user to drag to adjust the selection boundaries dynamically. A context menu with a "Copy" action must be presented upon successful selection. During this mode, primary reader gestures (panning, pagination, zooming) must be disabled.
- **State Management**: The current text selection bounds (including the exact start and stop coordinates) and extracted text must be managed by the Reader state. Dragging a handle updates the coordinates and triggers a re-selection. Switching pages, closing the document, or tapping outside the currently selected bounds should clear the active selection. Tapping inside the selected bounds should keep the selection active.
