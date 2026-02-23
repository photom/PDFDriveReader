# System Architecture

This document provides a detailed view of the PDFDriveReader architecture, including its layers, data flow, and component relationships, following Clean Architecture and DDD principles.

## 1. Clean Architecture Layers
The system is divided into three primary layers to ensure a strict separation of concerns and dependency inversion.

```mermaid
graph TD
    subgraph Presentation_Layer
        VM[ViewModels]
        UI[Compose UI]
        ST[UI State]
    end

    subgraph Domain_Layer
        UC[Use Cases]
        ENT[Entities]
        RI[Repository Interfaces]
    end

    subgraph Data_Layer
        REPO[Repository Impls]
        LOCAL[Local DataSource / Room]
        CLOUD[Cloud DataSource / Drive API]
        RENDER[PDF Renderer]
    end

    UI --> VM
    VM --> UC
    UC --> ENT
    UC --> RI
    REPO -- implements --> RI
    REPO --> LOCAL
    REPO --> CLOUD
    REPO --> RENDER
```

## 2. Data Flow Diagram (DFD)
This diagram shows how a PDF document moves from its source (Local Storage or Google Drive) to the user's screen.

```mermaid
graph LR
    subgraph Sources
        Storage[(Local File)]
        DriveApi[Google Drive API]
    end

    subgraph Data_Processing
        Sync[Sync Engine]
        Cache[(SQLite Cache)]
        Renderer[PdfRenderer]
    end

    subgraph Domain
        DocList[Document List]
        Bitmap[Page Bitmap]
    end

    subgraph UI
        ListView[Library View]
        ReaderView[Reader View]
    end

    Storage --> Sync
    DriveApi --> Sync
    Sync --> Cache
    Cache --> DocList
    DocList --> ListView

    Storage -- Open --> Renderer
    Renderer --> Bitmap
    Bitmap --> ReaderView
```

## 3. Detailed Class Diagram
This diagram defines the relationships and responsibilities of the key components in the system.

```mermaid
classDiagram
    class LibraryViewModel {
        -SyncLocalLibrary syncLocal
        -SyncCloudLibrary syncCloud
        +StateFlow~LibraryState~ state
        +refresh()
    }

    class ReaderViewModel {
        -OpenDocument openDoc
        -SaveReadingPosition savePos
        +StateFlow~ReaderState~ state
        +toggleUI()
        +onPageChanged(index)
    }

    class SyncLocalLibrary {
        -PdfRepository repository
        +execute()
    }

    class OpenDocument {
        -PdfRepository repository
        +execute(uri) PdfDocument
    }

    class PdfRepository {
        <<interface>>
        +getDocuments() Flow~List~DocumentMetadata~~
        +getDocument(uri) PdfDocument
        +savePosition(id, pos)
        +syncCloud()
    }

    class RoomPdfRepository {
        -PdfDao dao
        -GoogleDriveService drive
        -PdfRendererWrapper renderer
    }

    class PdfDocument {
        <<AggregateRoot>>
        +String id
        +renderPage(index) Bitmap
        +close()
    }

    LibraryViewModel --> SyncLocalLibrary
    ReaderViewModel --> OpenDocument
    SyncLocalLibrary --> PdfRepository
    OpenDocument --> PdfRepository
    RoomPdfRepository --|> PdfRepository
    RoomPdfRepository --> PdfDocument : creates
```

## 4. Dependency Injection (DI) Strategy
The system uses **Hilt** (built on Dagger) for dependency injection to ensure modularity and testability.
- **Singletons**: `RoomDatabase`, `GoogleDriveService`, and `SyncEngine` are managed as singletons.
- **Activity/ViewModel Scopes**: `PdfRepository` and `UseCases` are injected into ViewModels.
- **Testing**: Hilt allows replacing real DataSources with `Mocks` or `Fakes` during UI and integration testing.

## 5. Error Handling Model
The system uses a **Result Pattern** to propagate errors from the Data layer to the UI.
- **SyncResult**: `Success`, `Error(type, message)`, `Loading`.
  - *Error Types*: `NetworkError`, `AuthExpired`, `QuotaExceeded`.
- **RenderResult**: `Success(Bitmap)`, `Error(Exception)`.
  - *Error Types*: `InvalidPdf`, `FileLocked`, `OutOfMemory`.

## 6. Concurrency Strategy (Dispatchers)
To maintain a 60FPS UI, the app follows a strict dispatcher mapping:
- **Dispatchers.Main**: UI updates, Compose state transitions.
- **Dispatchers.IO**: SQLite queries, File I/O, Network calls (Drive API).
- **Dispatchers.Default**: PDF Rendering (CPU intensive), Bitmap processing, Thumbnail scaling.

## 7. Memory Management Policy
The app actively manages memory to prevent `OutOfMemory` errors during intensive reading:
- **LruCache**: Bitmaps for the current page and 1 page ahead/behind are kept in an `LruCache` (max 25% of available heap).
- **Purge Logic**: When a new document is opened, the previous document's `LruCache` is explicitly cleared.
- **OnLowMemory**: The app observes `ComponentCallbacks2` and clears all bitmap caches when the system is low on memory.

## 8. Interaction Summary
1.  **Dependency Rule**: Dependencies only point inwards. The `Presentation` layer depends on `Domain`, and the `Data` layer depends on `Domain` (interfaces).
2.  **Reactive Streams**: The `Data` layer exposes `Flow` objects from the SQLite cache, which the `Domain` use cases pass to the `ViewModels`. This ensures the UI is always in sync with the persistent state.
3.  **Immersive State**: The `ReaderViewModel` holds the `isUiVisible` boolean state, which is toggled by user taps and used by the `ReaderView` to show/hide overlays.
