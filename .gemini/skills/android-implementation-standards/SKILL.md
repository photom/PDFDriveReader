---
name: android-implementation-standards
description: Project-specific coding standards, commenting policies, and technical constraints for PDFDriveReader. Use when implementing new classes, methods, or refactoring existing code to ensure consistency and maintainability.
---

# Android Implementation Standards

This skill defines the technical standards and "rules of the road" for all code implementation within the PDFDriveReader project.

## 1. Commenting & Documentation Policy
Every architectural component must be documented using **KDoc** (`/** ... */`) to ensure clarity for current and future developers.

### Required Documentation
- **Interfaces**: Must describe the contract, the intended responsibility of the abstraction, and its role in the architecture.
- **Classes**: Must describe the primary responsibility of the class and its threading model (if applicable).
- **Public Methods**: 
    - Describe the behavior and purpose.
    - Document every parameter (`@param`).
    - Document the return value (`@return`).
    - Document potential exceptions or error results.

### Example
```kotlin
/**
 * Repository interface for managing PDF document persistence and metadata.
 */
interface PdfRepository {
    /**
     * Fetches a specific document by its unique URI.
     * 
     * @param uri The persistent URI or Drive ID of the document.
     * @return The initialized [PdfDocument] aggregate root.
     * @throws DocumentNotFoundException if the file is missing from storage.
     */
    suspend fun getDocument(uri: String): PdfDocument
}
```

## 2. Implementation Policies

### Clean Architecture & Boundaries
- **Unidirectional Flow**: UI -> ViewModel -> UseCase -> Repository.
- **Dependency Inversion**: ViewModels must only depend on UseCase interfaces or abstract classes. UseCases must only depend on Repository interfaces.
- **No Android in Domain**: The `domain/` package must remain pure Kotlin. No `Context`, `Uri`, or `Bitmap` (use abstractions or value objects like `FilePath`).

### Kotlin & Style
- Follow the [kotlin](../kotlin/SKILL.md) skill for coding conventions and test method naming.
- **Immutability**: Prefer `val` over `var`. Use `data class` for all models and value objects.
- **Visibility**: Use `internal` for implementation classes to encapsulate them within the module.
- **Naming**:
    - UseCase: Suffix with `UseCase` (e.g., `OpenDocumentUseCase`).
    - Repository Impl: Prefix with the technology (e.g., `RoomPdfRepository`).
    - Mappers: Suffix with `Mapper` (e.g., `DocumentMetadataMapper`).

### Error Handling
- Use a sealed `Result` or `Either` pattern for returning data from the Data/Domain layers.
- Do not use `try-catch` for control flow; use it only at the boundaries where external systems (I/O, Network) interact.

### Concurrency
- Use **Kotlin Coroutines**.
- ViewModels launch in `viewModelScope`.
- Repositories must switch to appropriate dispatchers (`Dispatchers.IO` for DB/Network, `Dispatchers.Default` for heavy computation).

### Testing (Canon TDD)
- **Zero-Code Policy**: No implementation code should be written without a corresponding failing test first.
- **Isolation**: Unit tests must not depend on Android Frameworks. Use Fakes (preferred) or Mocks for dependencies.
