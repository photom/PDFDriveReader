# User Interface Specification

## Core Components
- **Renderer**: Uses the native Android `PdfRenderer`.
- **Menu**: Accessible via a button in the top-right corner.
  - Action: `My Library` (Navigates to the document listing).
  - Action: `Reading Direction` (Selection list).

## Library View (PDF Selection)
The library is the central hub for managing and selecting documents.
- **Tabs/Sections**:
  - **Local Storage**: Displays all PDF files found on the device.
  - **Google Drive**: Displays PDF files synced from the user's Google Drive account.
- **List Item Details**:
  - **File Name**: Primary title of the document.
  - **Location Path**: 
    - *Local*: The directory path (e.g., `/Documents/Books/`).
    - *Google Drive*: The folder breadcrumb or parent folder name.
- **Search/Filter**: (Optional) Ability to search for files by name.

## Interaction Models
| Feature | Behavior |
| --- | --- |
| Library Selection | Tapping a list item opens the PDF in the reader. |
| Scroll | Swipe gestures in both the list and reader views. |
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
