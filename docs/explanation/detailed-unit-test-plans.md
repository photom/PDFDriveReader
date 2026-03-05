# Detailed Unit Test Plans

This document provides a granular test plan for every module in the PDFDriveReader application, following the **Canon TDD** methodology.

---

## 1. Domain Layer Modules (`domain/`)

### 1.1 Entities & Value Objects
- **`DocumentMetadata`**
    - [ ] `id` is properly assigned and immutable.
    - [ ] `isCached` correctly reflects the presence of the file in the cloud cache.
- **`PdfDocument` (Aggregate Root)**
    - [ ] `id` is properly assigned and immutable.
    - [ ] `close()` correctly triggers the release of underlying resources.
    - [ ] **Pre-loading**: `pageSizes` list accurately reflects the size of all pages when initialized.
- **`ReadingSettings` (Value Object)**
    - [ ] Equality: Two settings with the same direction/zoom are equal.
    - [ ] Default: Initialized with LTR and 100% zoom.
- **`PagePosition` (Value Object)**
    - [ ] Validation: Page index cannot be negative.
    - [ ] Validation: Zoom level must be between 1.0 and 5.0.

### 1.2 Use Cases (Business Logic)
- **`GetDocumentsUseCase`**
    - [ ] Logic: Verifies the resulting list is sorted primary by `locationPath` and secondary by `fileName`.
- **`SyncLocalLibrary`**
    - [ ] Success: Returns `DomainResult.Success` when the repository sync finishes without exceptions.
    - [ ] Error: Returns `DomainResult.Error` and handles repository exceptions gracefully when a failure occurs.
- **`OpenDocument`**
    - [ ] Flow: Fetches metadata -> fetches settings -> returns initialized Document.
    - [ ] Logic: If no settings exist, it applies defaults.
- **`SaveReadingPosition`**
    - [ ] Action: Correctly maps PagePosition to the repository call.
- **`SearchLibrary`**
    - [ ] Filtering: Partial match on filename.
    - [ ] Filtering: Partial match on location path.
    - [ ] Formatting: Ignores case during matching.
    - [ ] Edge Case: Returns all files for empty or whitespace-only queries.
    - [ ] Edge Case: Correctly handles special regex characters (e.g., `*`, `?`, `[`) without crashing.

- **`SyncEngine` (Concurrency & Edge Cases)**
    - [ ] Race Condition: Concurrent local and cloud syncs do not duplicate database entries.
    - [ ] Network Interruption: Sync gracefully stops and retains partial results if the connection is lost.
    - [ ] Auth Revocation: If the token is revoked during a sync, the engine emits `AuthError` and clears sensitive credentials.

---

## 2. Data Layer Modules (`data/`)

### 2.1 Repository Implementations
- **`PdfRepositoryImpl`**
    - [ ] `getDocuments()`: Correctly transforms Flow of DB entities to Domain entities.
    - [ ] `savePosition()`: Verifies the DAO's `upsert` is called with correct parameters.
    - [ ] `syncLocal()`: Mocks the file scanner and verifies the database is updated with new files.

### 2.2 Local File Scanner
- **`LocalFileScanner`**
    - [ ] `scanDirectory(uri)`: Verifies it recursively lists all PDF documents within a SAF Tree URI.
    - [ ] `scanDirectory(uri)`: Verifies it handles nested directories.
    - [ ] `scanDirectory(uri)`: Verifies it filters non-PDF files correctly.
    - [ ] `scanDirectory(uri)`: Verifies it handles revoked or invalid URIs gracefully.

### 2.3 Mappers
- **`DocumentMapper`**
    - [ ] `toDomain()`: Correctly maps all SQLite columns to the `DocumentMetadata` entity.
    - [ ] `fromDomain()`: Correctly prepares the Entity for SQLite insertion.

---

## 3. Presentation Layer Modules (`ui/`)

### 3.1 LibraryViewModel
- **Actions**
    - [ ] `onRefresh`: Triggers the `SyncLocalLibrary` use case for all stored SAF directories.
    - [ ] `onFolderPicked`: Verifies that adding a new SAF directory triggers an immediate scan.
    - [ ] `onFilesPicked`: Verifies that picking multiple PDF files via SAF correctly adds them to the database.
    - [ ] `onSearchQueryChanged`: Updates the filtered list in real-time.
    - [ ] `onTabSelected`: Switches the view between Local and Cloud sources.

### 3.2 ReaderViewModel
- **UI State (Immersive)**
    - [ ] Initial: `isUiVisible` is `false`.
    - [ ] `onToggleUI`: Flips the `isUiVisible` state.
    - [ ] `onPageChanged`: Triggers the `SaveReadingPosition` use case and updates `currentPageBitmap`.
- **Loading & Error Handling**
    - [ ] `loadDocument`: Verifies `isLoading` starts as `true`.
    - [ ] `loadDocument`: Verifies `isLoading` becomes `false` after successful load.
    - [ ] `loadDocument`: Verifies `isLoading` becomes `false` and `errorMessage` is set when an exception occurs.
- **Cache & Job Management**
    - [ ] `onPageChanged`: Verifies that a 5-page sliding window is maintained in `pageCache`.
    - [ ] `onPageChanged`: Verifies that heavy rendering is deferred until explicit calls (supporting UI "onRelease" logic).
    - [ ] `loadPageIntoCache`: Verifies that rendering resolution preserves the aspect ratio of the original PDF page.
    - [ ] `onPageChanged`: Verifies that any previous active caching job is cancelled when a new page is selected.
- **Navigation & Paging**
    - [ ] `onPageChanged`: Correctly handles out-of-bounds page indices.
    - [ ] `onDirectionChanged`: Updates the `ReadingDirection` and triggers a persistence update.
    - [ ] UI Verification: Verifies that the slider row displays both the current page and max page labels.
    - [ ] Theme Support: Verifies that text labels use `onSurface` color for visibility in Dark Mode.

### 3.3 MainViewModel
- **Navigation State**
    - [ ] Session: Verifies that the `AppSession` is correctly loaded from the repository.
    - [ ] URI Encoding: Verifies that document URIs are Base64 encoded for navigation to ensure compatibility with SAF URIs containing special characters.

### 3.4 Reader Continuous Mode (New Feature)
- **Scroll Behavior**
    - [ ] Verification: Verifies that `LazyRow` and `LazyColumn` are used instead of `Pager` components.
    - [ ] Verification: Verifies that no snapping (PagerFlingBehavior) is applied to the scrollable container.
    - [ ] Verification: Verifies that the document stays at its current position after a user lifts their finger during a swipe.
- **Page Concatenation & Layout**
    - [ ] Verification: Verifies that previous, current, and next pages are simultaneously visible in the composition (pre-rendered).
    - [ ] Verification: Verifies that the concatenation persists even during zoom actions.
    - [ ] Verification: Verifies that `PdfPageDisplay` uses the pre-loaded page dimensions to enforce a strict aspect ratio.
- **Zooming & Scaling**
    - [ ] Verification: Verifies that the zoom level correctly affects the rendering resolution of the PDF bitmaps.
    - [ ] Verification: Verifies that the continuous flow between pages is preserved during zoom.
    - [ ] Verification: Verifies that a double-tap gesture resets the zoom level to 1.0.
    - [ ] Verification: Verifies that panning at high zoom level correctly transitions to page swiping when content boundaries are reached.
    - [ ] Verification: Verifies that the zoom level remains stable after releasing fingers from the display.
    - [ ] Verification: Verifies that existing bitmaps are preserved in cache until higher-resolution versions are ready during zoom.

### 3.5 Reader Smoothness and Stability (Performance)
- **Rendering & Concurrency**
    - [ ] Verification: Verifies that rapid zoom/page changes do not flood the system with rendering requests (Debouncing/Conflation).
    - [ ] Verification: Verifies that rendering occurs on `Dispatchers.Default` and does not block the Main UI thread.
    - [ ] Verification: Verifies that high-zoom panning remains responsive by prioritizing the current viewport's rendering.
- **Gesture Reliability**
    - [ ] Verification: Verifies that rapid swiping does not cause the application to hang or crash.
    - [ ] Verification: Verifies that the "Gesture hand-off" between panning and swiping is fluid and without "dead zones".

---

## 4. Infrastructure & System Modules

### 4.1 SQLite / Room (DAOs)
- **`PdfDao`**
    - [ ] `upsertMetadata`: Handles conflicts by replacing old data.
    - [ ] `getMetadataByUri`: Returns null if file is not in cache.

### 4.2 PDF Renderer Wrapper
- **`PdfRendererWrapper`**
    - [ ] `renderPage`: Generates a Bitmap of the requested size.
    - [ ] `getPageCount`: Correctly reports the total pages in the PDF.

### 4.3 Google Drive Service
- **`GoogleDriveServiceImpl`**
    - [ ] `listFiles`: Verifies that parent folder IDs are resolved to folder names via the API.
    - [ ] `listFiles`: Verifies that the internal folder cache prevents redundant API calls for the same parent ID.
    - [ ] `listFiles`: Verifies that documents without parents default to "My Drive".

---

## 5. Test Implementation Guidelines
- **Tools**: JUnit 5, MockK (for mocking interfaces), Kotlin Coroutines Test (for `runTest`).
- **Isolation**: Each module must be tested in isolation. Repositories should use an in-memory database or mocks for the network/filesystem.
- **Naming**: Use descriptive names: `testWhen[Condition]Then[ExpectedResult]`.

### 3.6 Seamless Reader Interaction (New UX)
- **Viewport Consistency**
    - [ ] Verification: Verifies that zoom applies to the entire `LazyList` container, allowing multiple pages to be scaled together.
    - [ ] Verification: Verifies that pinch-to-zoom correctly calculates the focal point between two fingers and keeps it visually stationary.
    - [ ] Verification: Verifies that zoom gestures work reliably regardless of pointer count.
- **Integrated Axis Scrolling**
    - [ ] **TTB**: Verifies that vertical delta transitions from viewport panning to `LazyList` scrolling when zoomed edges are reached.
    - [ ] **LTR/RTL**: Verifies that horizontal delta transitions from viewport panning to `LazyList` scrolling when zoomed edges are reached.
- **Inertia & Physics**
    - [ ] Verification: Verifies that release velocity is used to initialize a decay animation.
    - [ ] Verification: Verifies that the momentum is distributed between viewport offset and list scroll position based on edge clamping.
    - [ ] Verification: Verifies that the document does not automatically snap or move after the momentum finishes.
