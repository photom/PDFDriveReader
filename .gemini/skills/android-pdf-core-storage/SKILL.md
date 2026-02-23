---
name: android-pdf-core-storage
description: Expert guidance on Android PDF rendering (PdfRenderer) and storage integration (Scoped Storage/SAF and Google Drive). Use when implementing document opening, cloud syncing, or high-performance page rendering.
---

# Android PDF Core & Storage

This skill provides the technical foundation for building the rendering engine and storage layer of an Android PDF reader.

## Core Workflows

### 1. Document Access Strategy
Decide on the source of the PDF and handle the appropriate lifecycle:
- **Local/SAF**: Use for user-picked files and "Scoped Storage" compliance. See [storage-access-framework.md](references/storage-access-framework.md).
- **Google Drive**: Use for cloud-synced documents. Requires OAuth2 and the Drive API. See [google-drive.md](references/google-drive.md).

### 2. High-Performance Rendering
Use the native `PdfRenderer` for low-latency display.
- **Background Threading**: Never render on the UI thread.
- **Bitmap Management**: Implement bitmap pooling to avoid memory spikes.
- **Details**: See [pdf-renderer.md](references/pdf-renderer.md).

### 3. Memory & Performance Guardrails
PDFs can be memory-intensive. Follow these patterns:
- **Page Pre-fetching**: Render the next/previous pages in a low-priority background thread.
- **Tile Rendering**: For zoom levels > 2x, split the page into tiles to keep bitmap sizes manageable.
- **Close Handles**: Always close `PdfRenderer.Page`, `PdfRenderer`, and `ParcelFileDescriptor` in `finally` blocks or using `.use {}`.

## Architecture Integration (DDD/Clean Arch)
- **Domain Layer**: Define a `PdfDocument` entity and a `PdfRepository` interface.
- **Data Layer**: Implement `PdfRepository` using `PdfRenderer` and `ContentResolver`.
- **UI Layer**: Use a `ViewModel` to coordinate the rendering thread and update a `StateFlow<Bitmap?>` or `Flow<PagingData<Page>>`.

## Security
- **Scoped Storage**: Do not request `READ_EXTERNAL_STORAGE`. Rely on SAF URIs.
- **Credential Storage**: Use `EncryptedSharedPreferences` or the `DataStore` with `Android KeyStore` to store OAuth tokens.
