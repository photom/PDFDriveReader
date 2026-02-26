# User Interface Specification

## Application Modes

### 1. Library Mode (Document Selection)
The entry point of the application, used for browsing and selecting PDFs.
- **Views**: Local Storage list and Google Drive list.
- **Top Bar**: Always visible.
  - **App Title**: Left-aligned.
  - **Search Icon**: Reveals a filter bar that searches within the **currently active tab** (Local or Cloud).
  - **Syncing Icon**: Shown only during active background sync.
- **Sorting**: By default, lists are sorted alphabetically by **Location Path** (folder), and then by **File Name**.
- **List Item Details**:
  - **File Name**: Primary title of the document.
  - **Location Path**: 
    - *Local*: The relative directory path (e.g., `/Documents/Books/`).
    - *Google Drive*: The name of the parent folder (e.g., `Manga`, `My Drive`).
  - **Cached Status**: Google Drive items must display an "Offline" icon (e.g., a checkmark or cloud-download icon) if they are already downloaded to the local cache.
- **Google Drive Tab**: ...

### 2. Reader Mode (PDF Viewing)
A distraction-free environment for document consumption.
- **Immersive View**: UI hidden by default.
- **Scroll Physics**:
  - **LTR / RTL**: **Paginated** (snaps to page boundaries) to simulate a physical book.
  - **TTB**: **Continuous** scrolling for a vertical document flow.
- **Rendering Proportions**:
  - **Aspect Ratio**: The app must preserve the original width-to-height ratio of each page.
  - **Best Fit**: Pages must be scaled to fit the display width or height, whichever is reached first, without stretching or distortion.
- **Page Indicators**: Displayed as "Page [Current] of [Total]" using **1-indexed** numbering (e.g., Page 1 of 50).
- **Menu Interaction**: ...
- **Back Navigation**: 
  - If **Link History exists**: System Back returns to the previous position.
  - If **Link History is empty** and **UI is visible**: System Back hides the UI overlay.
  - If **Link History is empty** and **UI is hidden**: System Back exits to Library Mode.
  - **Session Restore Case**: If the app launched directly into Reader Mode (backstack empty), the first "Exit" action (System Back or Close Reader) must navigate to **Library Mode** instead of closing the application.

## Interaction Models
| Feature | Behavior |
| --- | --- |
| Mode Transition | Tapping a PDF in Library Mode opens Reader Mode. |
| UI Visibility | In Reader Mode, a single tap toggles the menu/UI overlay. |
| Scroll | Swipe gestures in both modes. |
| Page Slider | **Dragging**: Updates the page number indicator in real-time for immediate feedback. |
| Page Slider | **Release**: Triggers the actual page jump and document rendering upon finger release to prevent hanging. |
| Zoom | Pinch gestures in Reader Mode (100% - 500%). |
| Zoom Reset | Double-tap in Reader Mode to 100%. |
| Navigation History | Link jumps save the previous position; swipe back to return. |

## Scrollbars
- **Horizontal (L-to-R / R-to-L)**:
  - Position: Bottom.
  - Labels: Current Page / Total Pages (to the left).
- **Vertical (Top-to-Bottom)**:
  - Position: Right.
  - Labels: Current Page / Total Pages (above the bar).
- **Persistence**: Visible during scroll + 2-second delay after gesture completion.

## Persistence & State
- **Last Active Mode**: Remembers if the app was in Library or Reader Mode.
- **Per-Document Settings**: The app stores the following specifically for each document:
  - **Reading Direction**: (L-to-R, R-to-L, or T-to-B).
  - **Last Page**: The exact page index.
  - **Zoom Level**: The user's last used zoom percentage (e.g., 150%).
- **Navigation History**: Link backstack is persisted across backgrounding but cleared when a different document is opened.
- **Last File**: Automatically opens the previously viewed PDF if Reader Mode is the active mode on launch.
- **Cache**: Stores active mode, file metadata (via `file_uri`), and settings in an SQLite database.

## Error Feedback & User Notification
To ensure the user is never left without feedback during failed operations:

### 1. Authentication Failures
- **Visual Feedback**: If Sign-In fails, a **SnackBar** must appear with a descriptive message (e.g., "Sign-in failed. Please check your connection.").
- **State Recovery**: The "Sign In" button must remain enabled and interactive after a failed attempt.
- **Detailed Error**: In debug builds, the SnackBar should include the error code (e.g., "Google API Error: 12500").

### 2. Synchronization Errors
- **Syncing Icon**: If a sync fails, the `Syncing Icon` should transition to an `Error Icon` (e.g., a warning triangle) for 3 seconds before disappearing.
- **Empty States**: If a sync completes but returns 0 results due to an error, the empty state text should change from "No PDFs found" to "Failed to fetch documents. Tap to retry."

### 3. Rendering Failures
- **Placeholder**: If a specific page fails to render, display a gray box with a "Refresh" icon in the center.
- **Notification**: Show a toast message: "Error rendering page [X]".

## Feedback & Animation
- **Indeterminate Loading**: All asynchronous operations (Library Sync, Document Download, Page Rendering) must display a **cycling indeterminate progress indicator** to inform the user that the process is active.
- **Centering**: Loading indicators must be centered within their respective viewports (Library list or Reader canvas) to maintain visual balance and prevent clipping.
