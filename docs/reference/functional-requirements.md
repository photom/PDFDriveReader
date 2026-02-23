# Functional Requirements

## Core Operations

### Document Discovery & Library Management
The application must maintain an up-to-date list of PDF files from two primary sources:

#### 1. Local PDF Discovery
- **Scanning**: Periodically scan the device's public `Documents` and `Downloads` folders for `.pdf` files.
- **Display**: For each file, the app must retrieve and display the **File Name** and the **Relative Path** (e.g., `/Downloads/Invoices/`).
- **Permissions**: Use Scoped Storage APIs to access files without requesting legacy external storage permissions.

#### 2. Google Drive (Cloud) Discovery
- **API Access**: Use the Google Drive API to query for files with the MIME type `application/pdf`.
- **Display**: For each file, the app must retrieve and display the **File Name** and the **Parent Folder Name** as the location.
- **Sync**: Support pull-to-refresh to fetch updated lists from the cloud.

### Document Validation
Before opening any PDF file (from local or Drive), the app must validate the format:
1. **Invalid Format**: Show an error message (e.g., "The PDF file is invalid and cannot be opened.").
2. **Invalid Format**: Do not attempt to render the file.

### Required Permissions
- **Files and Media**: No special permissions are required on modern Android versions (using SAF and Scoped Storage).
- **Network**: Required for Google Drive API access and authentication.
- **Accounts**: Required for Google Sign-In and cloud synchronization.

### SQLite Cache Schema
| Column | Description |
| --- | --- |
| `file_path` | Absolute path or URI to the PDF file. |
| `file_name` | The name of the document. |
| `current_page` | The index of the last displayed page. |
| `last_modified` | Timestamp for sorting recently opened files. |
