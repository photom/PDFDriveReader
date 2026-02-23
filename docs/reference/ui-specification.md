# User Interface Specification

## Application Modes

### 1. Library Mode (Document Selection)
The entry point of the application, used for browsing and selecting PDFs.
- **Views**: Local Storage list and Google Drive list.
- **Top Bar**: Always visible.
  - **App Title**: Left-aligned.
  - **Search Icon**: Reveals a real-time filter bar for the current list.
  - **Syncing Icon**: Shown only during active background sync.
- **Google Drive Tab**: 
  - **Unauthenticated State**: Shows a "Sign in with Google" button and an explanation.
  - **Authenticated State**: Shows the list of cloud PDFs and a "Sign Out" option in the menu.
- **List Item Details**: Displays **File Name** and **Location Path**.

### 2. Reader Mode (PDF Viewing)
A distraction-free environment for document consumption.
- **Immersive View**: UI hidden by default.
- **Menu Interaction**: 
  - **Show Menu**: A single tap while UI is hidden.
  - **Hide Menu**: A single tap while UI is visible.
- **Back Navigation**: 
  - If **UI is visible**: System Back hides the UI overlay.
  - If **UI is hidden**: System Back exits to Library Mode.
- **Menu Actions**: `Close Reader`, `Reading Direction`, and `Bookmarks`.

## Interaction Models
| Feature | Behavior |
| --- | --- |
| Mode Transition | Tapping a PDF in Library Mode opens Reader Mode. |
| UI Visibility | In Reader Mode, a single tap toggles the menu/UI overlay. |
| Scroll | Swipe gestures in both modes. |
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
- **Last File**: Automatically opens the previously viewed PDF if Reader Mode is the active mode on launch.
- **Cache**: Stores active mode, file metadata, and settings in an SQLite database.
