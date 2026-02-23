# Entity & Relationship Diagrams

This document defines the core domain entities and their relationships within the **Library** and **Reader** modes, as well as the global state that coordinates them.

## 1. Global Application State
The `AppState` aggregate root manages the high-level transition between the two primary modes.

```mermaid
classDiagram
    class AppMode {
        <<enumeration>>
        LIBRARY
        READER
    }

    class AppState {
        <<AggregateRoot>>
        +AppMode currentMode
        +String activeDocumentId
        +restoreLastSession()
    }

    AppState o-- AppMode
```

## 2. Library Mode Entities
In Library Mode, the system focuses on document discovery, metadata indexing, and cloud synchronization.

```mermaid
classDiagram
    class LibrarySection {
        <<enumeration>>
        LOCAL
        CLOUD
    }

    class DocumentMetadata {
        <<Entity>>
        +String id
        +String fileName
        +String locationPath
        +SourceType source
        +DateTime lastOpened
    }

    class SourceType {
        <<enumeration>>
        LOCAL_STORAGE
        GOOGLE_DRIVE
    }

    class SyncStatus {
        <<ValueObject>>
        +Boolean isSyncing
        +DateTime lastSyncTime
    }

    class GoogleAuthSession {
        <<Entity>>
        +Boolean isAuthenticated
        +String userEmail
        +signIn()
        +signOut()
    }

    DocumentMetadata o-- SourceType
    LibraryViewModel --> DocumentMetadata : observes
    LibraryViewModel --> SyncStatus : reflects
    LibraryViewModel --> GoogleAuthSession : checks
```

## 3. Reader Mode Entities
Reader Mode manages the immersive experience, per-document settings, and navigation history.

```mermaid
classDiagram
    class ReadingSession {
        <<AggregateRoot>>
        +String documentId
        +PagePosition currentPosition
        +ReadingSettings settings
        +NavigationHistory history
    }

    class PagePosition {
        <<ValueObject>>
        +Int pageIndex
        +Float zoomLevel
    }

    class ReadingSettings {
        <<ValueObject>>
        +ReadingDirection direction
        +Float savedZoom
    }

    class ReadingDirection {
        <<enumeration>>
        LTR
        RTL
        TTB
    }

    class NavigationHistory {
        <<Entity>>
        +Stack~PagePosition~ historyStack
        +push(position)
        +pop() PagePosition
    }

    ReadingSession *-- PagePosition
    ReadingSession *-- ReadingSettings
    ReadingSession *-- NavigationHistory
    ReadingSettings o-- ReadingDirection
```

## 4. Cross-Mode Relationship (ER Diagram)
This diagram illustrates how data persists across both modes via the shared SQLite cache.

```mermaid
erDiagram
    DOCUMENT_METADATA ||--o| READING_SESSION : "has one"
    DOCUMENT_METADATA {
        string file_uri PK
        string file_name
        string source_type
        datetime last_modified
    }
    READING_SESSION {
        string file_uri FK
        int current_page
        string reading_direction
        float zoom_level
    }
    APP_STATE {
        string last_mode
        string active_file_uri FK
    }
    APP_STATE |o--|| DOCUMENT_METADATA : "tracks"
```

## 5. Mode Transition Logic
The following sequence illustrates the transition from Library to Reader.

```mermaid
sequenceDiagram
    participant U as User
    participant LV as LibraryView
    participant VM as LibraryViewModel
    participant AS as AppState
    participant RV as ReaderView

    U->>LV: Taps Document Item
    LV->>VM: onDocumentSelected(docId)
    VM->>AS: setMode(READER, docId)
    AS-->>RV: triggers navigation
    RV->>RV: loadDocument(docId)
    RV->>RV: applySavedSettings(direction, page, zoom)
    Note over RV: Immersive UI (Hidden)
```

## Summary of Relations
- **One-to-One**: Every `DocumentMetadata` entry can have exactly one `ReadingSession` record (storing its specific settings).
- **Global Dependency**: Both `LibraryView` and `ReaderView` depend on the `AppState` to determine which UI to render and which file to load.
- **Composition**: A `ReadingSession` **owns** its `NavigationHistory` and `ReadingSettings`; they do not exist independently of a document session.
