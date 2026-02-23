# Architecture: DDD & Clean Architecture

PDFDriveReader is built using **Domain-Driven Design (DDD)** and **Clean Architecture** principles to ensure maintainability, testability, and a clear separation of concerns.

## Layered Structure

### 1. Domain Layer (Pure Kotlin)
Contains the core business logic and entities.
- **Entities**: `PdfDocument`, `PagePosition`, `LibrarySection`.
- **Use Cases**: `SyncLocalLibrary`, `SyncCloudLibrary`, `OpenDocument`, `SaveReadingPosition`.
- **Repository Interfaces**: Define contracts for fetching document lists and binary data.

### 2. Data Layer
Implementations of the domain interfaces.
- **SQLite (Room)**: Persistent storage for reading sessions, document metadata, and a cached index of the Library.
- **Background Sync Engine**: Orchestrates background tasks (e.g., using `WorkManager`) for Google Drive metadata fetching without blocking the UI.
- **PDF Core**: Integration with Android's `PdfRenderer`.

### 3. Presentation Layer (Jetpack Compose)
Modern declarative UI.
- **ViewModels**: 
  - Observe background sync status to toggle the **Syncing Icon** visibility in Library Mode.
  - Automatically refresh UI lists when the background indexer updates the SQLite cache.
  - Manage **Immersive UI State** in Reader Mode, handling the visibility toggle of the menu overlay based on user input events.
- **UI Components**: 
  - **LibraryView**: Tabbed navigation for Local and Cloud sources.
  - **ReaderView**: Immersive PDF rendering with a toggleable Overlay for controls.

## Key Design Patterns
- **Repository Pattern**: Abstracts the source of PDF data (Local vs. Drive).
- **MVI (Model-View-Intent)**: Unidirectional data flow for handling UI states like loading, rendering, and errors.
- **TDD (Test-Driven Development)**: All business logic is covered by unit tests before implementation.
