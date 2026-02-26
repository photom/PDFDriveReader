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
    - [ ] Success: Returns list when repository finds PDFs.
    - [ ] Empty: Returns empty list when repository is empty.
    - [ ] Error: Handles repository exceptions gracefully.
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

### 2.2 Mappers
- **`DocumentMapper`**
    - [ ] `toDomain()`: Correctly maps all SQLite columns to the `DocumentMetadata` entity.
    - [ ] `fromDomain()`: Correctly prepares the Entity for SQLite insertion.

---

## 3. Presentation Layer Modules (`ui/`)

### 3.1 LibraryViewModel
- **States**
    - [ ] Initial: Verifies the state starts as `LibraryState.Loading`.
    - [ ] Success: Displays the document list.
    - [ ] Empty: Displays "No PDFs found" state.
- **Actions**
    - [ ] `onRefresh`: Triggers the `SyncLocalLibrary` use case.
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
    - [ ] `onPageChanged`: Verifies that a 3-page sliding window is maintained in `pageCache`.
    - [ ] `onPageChanged`: Verifies that any previous active caching job is cancelled when a new page is selected.
- **Navigation & Paging**
    - [ ] `onPageChanged`: Correctly handles out-of-bounds page indices.
    - [ ] `onDirectionChanged`: Updates the `ReadingDirection` and triggers a persistence update.
    - [ ] `onDirectionChanged`: Verifies that the UI state reflects the new direction immediately.

### 3.3 MainViewModel
- **Navigation State**
    - [ ] Session: Verifies that the `AppSession` is correctly loaded from the repository.
    - [ ] Logic: Verifies the logic for choosing the starting destination (Library vs. Reader) based on session data.

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
