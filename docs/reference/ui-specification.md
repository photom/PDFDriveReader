# User Interface Specification

## Application Modes

### 1. Library Mode (Document Selection)
The entry point of the application, used for browsing and selecting PDFs.
- **Views**: Local Storage list and Google Drive list.
- **Top Bar**: Always visible, containing a search icon and app title.
- **List Item Details**: Displays **File Name** and **Location Path**.

### 2. Reader Mode (PDF Viewing)
A distraction-free environment for document consumption.
- **Immersive View**: By default, the UI (including the top bar and menu button) is **hidden** to maximize the reading area.
- **Menu Interaction**:
  - **Show Menu**: A single tap on the document area while the UI is hidden reveals the **Menu Icon** (top-right).
  - **Hide Menu**: A single tap on the document area while the UI is visible hides the **Menu Icon** and overlays.
  - **Menu Actions**: `Close Reader` (return to Library), `Reading Direction`, and `Bookmarks`.

## Interaction Models
| Feature | Behavior |
| --- | --- |
| Mode Transition | Tapping a PDF in Library Mode opens Reader Mode. |
| UI Visibility | In Reader Mode, a single tap toggles the menu/UI overlay (Show if hidden, Hide if shown). |
| Scroll | Swipe gestures in both modes. |
| Zoom | Pinch gestures in Reader Mode (100% - 500%). |
| Zoom Reset | Double-tap in Reader Mode to 100%. |

| Zoom | Pinch gestures (100% - 500%). |
| Zoom Reset | Double-tap to 100%. |
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
- **Last File**: Automatically opens the previously viewed PDF on app launch.
- **Last Position**: Navigates to the exact page where the user left off.
- **Cache**: Stores file path, name, and current page in an SQLite database.
