# Storage Access Framework (SAF) & Scoped Storage

## Opening Documents
Use `ActivityResultContracts.OpenDocument()` to let the user pick a file. This provides a persistent `Uri`.

```kotlin
val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let {
        // Persist permissions if needed for future access
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        
        // Use uri
    }
}
openDocumentLauncher.launch(arrayOf("application/pdf"))
```

## Reading Content
Always use the `ContentResolver` to get a `ParcelFileDescriptor`.

```kotlin
val pfd = context.contentResolver.openFileDescriptor(uri, "r")
```

## Persistent Access
If the app needs to remember the file across reboots, store the URI string in a database and ensure `takePersistableUriPermission` was called.

## Document Metadata
Query the `DocumentsContract` to get the display name and size:
```kotlin
context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (cursor.moveToFirst()) {
        val name = cursor.getString(nameIndex)
    }
}
```
