# Detailed Unit Test Plans

This document provides a granular test plan for every module in the PDFDriveReader application, following the **Canon TDD** methodology.

---

## 1. Domain Layer Modules (`domain/`)

### 1.1 Entities & Value Objects
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
    - [ ] Initial: Shows `Loading` state.
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
    - [ ] `onPageChanged`: Triggers the `SaveReadingPosition` use case (debounced).
- **Navigation**
    - [ ] `onBackPressed`:
        - If UI is visible: Hides UI.
        - If UI is hidden: Navigates to Library Mode.
- **Reading Direction**
    - [ ] `onDirectionChanged`: Updates the `ReadingSettings` and triggers a persistence update.

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

---

## 5. Test Implementation Guidelines
- **Tools**: JUnit 5, MockK (for mocking interfaces), Kotlin Coroutines Test (for `runTest`).
- **Isolation**: Each module must be tested in isolation. Repositories should use an in-memory database or mocks for the network/filesystem.
- **Naming**: Use descriptive names: `testWhen[Condition]Then[ExpectedResult]`.
