# Testing Strategy & Test Plans

This document outlines the testing strategy for PDFDriveReader, ensuring high reliability and maintainability through a combination of Unit and End-to-End (E2E) tests.

## 1. Unit Testing Plan (Pure Kotlin)
Target: **Domain Layer** (Use Cases, Entities) and **Data Layer** (Mappers, Repository Logic).
Location: `app/src/test/java/`

### Test List: Domain & Use Cases
- [ ] **SyncLocalLibrary**:
    - Returns a list of PDFs when local storage contains valid files.
    - Returns an empty list when no PDFs are found.
    - Filters out invalid/corrupt PDF files.
- [ ] **OpenDocument**:
    - Correctly loads metadata for a valid URI.
    - Throws `InvalidPdfException` for a corrupted file.
    - Restores the saved `PagePosition` and `ReadingDirection`.
- [ ] **NavigationHistory**:
    - `push` adds the current page to the stack.
    - `pop` returns the last page and removes it.
    - Returning "Back" when history is empty returns `null`.

### Test List: Data Layer (Fakes & Mocks)
- [ ] **PdfRepository**:
    - `getDocuments` emits the cached list from SQLite.
    - `savePosition` updates the database record correctly.
- [ ] **Mappers**:
    - Convert `RoomPdfEntity` to `DocumentMetadata` entity without data loss.

---

## 2. Integration & Instrumented Testing Plan
Target: **Data Layer** (Room DB) and **PDF Rendering**.
Location: `app/src/androidTest/java/`

- [ ] **Room Database (DAOs)**:
    - Test CRUD operations for `DOCUMENT_METADATA` and `READING_SESSION`.
    - Verify that `reading_direction` and `zoom_level` are persisted correctly.
- [ ] **PdfRendererWrapper**:
    - Verify that a bitmap is generated for a valid PDF page.
    - Verify that resources (PFD) are closed after rendering.

---

## 3. End-to-End (E2E) UI Testing Plan
Target: **Library Mode** and **Reader Mode** flows.
Tool: **Compose UI Test** / **Espresso**.

### Flow: Library to Reader
1. **Initial Launch**: Verify app starts in Library Mode (Local tab).
2. **Search**: Type a query and verify the list filters correctly.
3. **Open Document**: Tap a list item and verify transition to Reader Mode.
4. **Reader Interaction**:
    - Single tap shows the UI overlay.
    - Toggle reading direction and verify the UI updates (e.g., scrollbar moves).
5. **Session Restore**:
    - Close the app in Reader Mode.
    - Relaunch and verify it opens directly to the same page/zoom level.

### Flow: Reader Interactions & Edge Cases
1. **Orientation Change**:
    - Open a PDF to page 15, rotate to Landscape.
    - Verify the page remains at 15 and the UI overlay state (visible/hidden) is preserved.
2. **Missing File**:
    - Delete a PDF from storage manually.
    - Tap the file in the app's Library.
    - Verify an error SnackBar appears and the list entry is removed or marked "Unavailable."
3. **Back Navigation**:
    - Tap a link to jump to page 100.
    - Swipe Back (system gesture).
    - Verify it returns to the previous page (Link History) rather than exiting to the Library.

### Flow: Google Drive Integration
1. **Sign-In**: Tap "Sign in with Google" and verify the authenticated state.
2. **Syncing**: Trigger a refresh and verify the **Syncing Icon** appears.
3. **Download**: Tap a cloud PDF and verify the download progress/transition.

---

## 4. TDD Workflow (Canon TDD)
For every new feature or bug fix:
1. **Red**: Write a failing test in the appropriate layer.
2. **Green**: Implement the minimal logic to pass.
3. **Refactor**: Clean up code and ensure test independence.
4. **Verify**: Run the full suite to prevent regressions.

## 5. Automation & Quality Gates
- **Local**: `gradlew test` and `gradlew connectedCheck`.
- **CI (Optional)**: Run unit tests on every PR.
- **Coverage**: Aim for 100% coverage of the **Domain Layer**.
