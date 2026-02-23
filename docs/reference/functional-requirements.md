# Functional Requirements

## Core Operations

### Mode & UI State Management

#### 1. Application State
The app must persist and track which mode is active (**Library** vs. **Reader**).
- If the app is closed while in Reader Mode, it must relaunch directly into Reader Mode for the last opened document (Session Restore).

#### 2. Reader Mode UI Visibility (Immersive)
To provide a distraction-free experience:
- **Default State**: Upon opening a document, the UI overlay (Menu button, Page indicators) must be hidden.
- **Toggle Logic**:
  - If the UI is **hidden**: A single tap on the display area must **show** the UI.
  - If the UI is **visible**: A single tap on the display area (outside of menu interactive elements) must **hide** the UI.
- **Timeout**: (Optional) The UI overlay may automatically hide after 5 seconds of inactivity.
- **Gesture Conflict**: The app must distinguish between a **Single Tap** (UI toggle) and a **Scroll/Pinch** (Navigation). Taps on links within the PDF must prioritize link navigation over UI toggling.

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
