# Project Structure

This document outlines the directory and file organization of the **PDFDriveReader** project, following Clean Architecture and Domain-Driven Design (DDD) principles.

## 1. Overview
The project is structured into three primary layers within the `app` module, ensuring a strict separation of concerns and a clear dependency direction: **UI -> Domain <- Data**.

## 2. Source Code Structure
Root Package: `com.hitsuji.pdfdrivereader`

### 2.1 Domain Layer (`domain/`)
The core of the application, containing business logic and rules. It has zero dependencies on Android frameworks.
- `model/`: Entities and Value Objects (e.g., `PdfDocument`, `PagePosition`).
- `repository/`: Repository interfaces defining the contracts for data access.
- `usecase/`: Pure logic interactors (e.g., `SyncLocalLibrary`, `OpenDocument`).

### 2.2 Data Layer (`data/`)
Implementations of domain abstractions and data sourcing.
- `local/`: Local persistence logic (Room database, DAOs, and SQLite entities).
- `remote/`: Cloud integration (Google Drive API service implementation).
- `repository/`: Concrete implementations of domain repository interfaces.
- `mapper/`: Translators between Data Transfer Objects (DTOs/Entities) and Domain Models.
- `renderer/`: Android-specific `PdfRenderer` wrappers.

### 2.3 Presentation Layer (`presentation/`)
User interface and interaction logic using Jetpack Compose.
- `library/`: Components, ViewModels, and States for the document list views.
- `reader/`: Components, ViewModels, and States for the immersive PDF reader.
- `theme/`: Material Design 3 theme definitions, colors, and typography.
- `common/`: Reusable UI components and modifiers.

### 2.4 Infrastructure & DI (`di/`, `system/`)
Cross-cutting concerns and framework-specific integrations.
- `di/`: Hilt modules for dependency injection.
- `system/`: WorkManager tasks for background syncing and system service wrappers.

## 3. Directory Tree
```text
app/src/main/java/com/hitsuji/pdfdrivereader/
├── data/
│   ├── local/
│   ├── mapper/
│   ├── remote/
│   ├── renderer/
│   └── repository/
├── di/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── common/
│   ├── library/
│   ├── reader/
│   └── theme/
└── system/
```

## 4. Resource Structure (`app/src/main/res/`)
- `drawable/`: Vector assets for reading directions (LTR, RTL, TTB) and app icons.
- `values/`: String resources for localization and Material 3 color definitions.

## 5. Documentation (`docs/`)
Follows the **Diátaxis** framework:
- `tutorials/`: Step-by-step guides.
- `how-to-guides/`: Goal-oriented recipes.
- `reference/`: Technical specifications and project structure.
- `explanation/`: Architectural decisions and design patterns.
```text
docs/
├── explanation/
│   ├── architecture-overview.md
│   ├── detailed-unit-test-plans.md
│   ├── entity-relationship-diagrams.md
│   ├── fake-google-drive-api-design.md
│   ├── system-architecture.md
│   └── testing-strategy.md
├── how-to-guides/
│   └── navigation-and-reading.md
├── reference/
│   ├── functional-requirements.md
│   ├── project-structure.md
│   ├── ui-specification.md
│   └── wireframes-ui-design.md
└── tutorials/
    └── getting-started.md
```
