---
name: kotlin-android-implementation
description: Unified expert guidance on Kotlin programming and Android implementation standards. Covers official coding conventions, test method naming, KDoc policies, Clean Architecture boundaries, and TDD workflows.
---

# Kotlin & Android Implementation Standards

This skill provides a single source of truth for coding standards, documentation policies, and architectural constraints within the PDFDriveReader project.

## 1. Kotlin Coding Conventions & Style
Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

### General Rules
- **Naming**: `PascalCase` for classes, `camelCase` for methods/variables.
- **Immutability**: Always prefer `val` over `var`. Use `immutable` collections by default.
- **Null Safety**: Avoid non-null assertions (`!!`). Use safe calls (`?.`), Elvis operator (`?:`), or `let`.
- **Visibility**: Use `internal` for implementation-specific classes to encapsulate them within the module.

### Test Method Naming
Use backticks to provide descriptive, sentence-like names for all unit and integration tests.
- **Example**: `@Test fun `PagePosition should throw exception when pageIndex is negative`() { ... }`

## 2. Commenting & Documentation (KDoc)
Every architectural component must be documented using **KDoc** (`/** ... */`).

### Required Coverage
- **Interfaces**: Describe the contract and its role in the architecture.
- **Classes**: Describe the primary responsibility and threading model.
- **Public Methods**: 
    - Describe behavior and purpose.
    - Use `@param` for every parameter.
    - Use `@return` for the return value.
    - Document exceptions or error results.

## 3. Architectural Policies (Clean Architecture)
- **Unidirectional Flow**: UI -> ViewModel -> UseCase -> Repository.
- **Dependency Inversion**: High-level modules (Domain) must not depend on low-level modules (Data/UI). Use interfaces.
- **Pure Domain**: The `domain/` package must remain pure Kotlin. No dependencies on Android frameworks (`Context`, `Uri`, `Bitmap`, etc.).
- **Naming Conventions**:
    - UseCases: Suffix with `UseCase` (e.g., `OpenDocumentUseCase`).
    - Repository Impl: Prefix with technology (e.g., `RoomPdfRepository`).
    - Mappers: Suffix with `Mapper` (e.g., `DocumentMetadataMapper`).

## 4. Technical Constraints
- **Concurrency**: Use **Kotlin Coroutines**. Repositories must handle context switching (e.g., `withContext(Dispatchers.IO)`).
- **Error Handling**: Use sealed `Result` or `Either` patterns. Avoid using exceptions for control flow.
- **Resource Safety**: Always use `.use {}` or `try-finally` when handling I/O, `PdfRenderer`, or `ParcelFileDescriptor`.

## 5. Testing (Canon TDD)
- **Zero-Code Policy**: Never write implementation code without a failing test first.
- **Isolation**: Use Fakes (preferred) or Mocks. Tests must be fast and independent of the Android framework where possible.

## References
- [Official Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Implementation Standards (Legacy)](../android-implementation-standards/SKILL.md)
- [Kotlin Standards (Legacy)](../kotlin/SKILL.md)
