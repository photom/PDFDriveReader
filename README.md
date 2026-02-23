# PDFDriveReader

**PDFDriveReader** is a modern, lightweight Android PDF reader built with DDD, Clean Architecture, and TDD. It features smart session restoration and multi-directional reading support.

## Key Features
- **Integrated Library**: Browse and manage your PDFs from one place. View document lists from local storage and Google Drive, complete with filenames and location paths.
- **Reading Modes**: Left-to-right, Right-to-left, and Top-to-bottom support.
- **Smart Session Restore**: Automatically reopens your last PDF at the exact page you left off.
- **Link Navigation**: Robust internal link support with navigation history.
- **Flexible Viewing**: High-performance rendering with pinch-to-zoom and gesture support.

## Documentation (Diátaxis)

Explore our documentation for more detailed information:

### [Tutorials](./docs/tutorials/getting-started.md)
- **Getting Started**: Learn how to open and navigate your first PDF.

### [How-to Guides](./docs/how-to-guides/navigation-and-reading.md)
- **Navigation & Reading**: Practical guides for zooming, reading directions, and link jumps.

### [Reference](./docs/reference/)
- **[UI Specification](./docs/reference/ui-specification.md)**: Technical details of UI components and interactions.
- **[Functional Requirements](./docs/reference/functional-requirements.md)**: Logic, permissions, and validation rules.

### [Explanation](./docs/explanation/architecture-overview.md)
- **Architecture Overview**: Deep dive into DDD, Clean Architecture, and TDD implementation.

---

## Technical Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: DDD + Clean Architecture
- **PDF Engine**: Android native `PdfRenderer`
- **Database**: SQLite (Room)
- **Integration**: Google Drive API
