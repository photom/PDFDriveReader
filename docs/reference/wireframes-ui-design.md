# Wireframes & UI Design

This document provides visual wireframes and layout specifications for the PDFDriveReader application, following Material Design 3 (M3) principles.

## 1. Library Mode (Document Selection)

### Main Layout
The Library uses a tabbed navigation to switch between local and cloud sources.

```mermaid
graph TD
    subgraph Library_Mode
        TopBar["[ App Title ]  [ Search ]  [ Syncing Icon ]"]
        Tabs["[ LOCAL STORAGE ]  [ GOOGLE DRIVE ]"]
        List["(Scrollable List)
        -----------------------
        [Icon] File Name
               /path/to/file/
        -----------------------
        [Icon] File Name
               /path/to/file/
        -----------------------"]
        EmptyState["(If no files)
        'No PDFs found'
        [ Scan / Refresh Button ]"]
    end
    TopBar --> Tabs
    Tabs --> List
    List -.-> EmptyState
```

### Search Bar (Active)
When the search icon is tapped, the Top Bar transforms into a filter field.

```mermaid
graph LR
    SearchActive["[ <- ] [ Search in current tab...        ] [ X ]"]
```

### Google Drive - Unauthenticated
If the user is not signed in, the Google Drive tab displays a call-to-action.

```mermaid
graph TD
    subgraph Google_Drive_Tab
        Message["Access your cloud PDFs"]
        SignInBtn["[ Sign in with Google ]"]
    end
```

---

## 2. Reader Mode (PDF Viewing)

### Immersive View (Default)
The screen is dedicated entirely to the PDF content. No UI elements are visible.

```mermaid
graph TD
    subgraph Reader_Mode_Immersive
        PDFContent["[ Full Screen PDF Page Content ]"]
        Scrollbar["[ Vertical/Horizontal Scrollbar (Visible only during scroll) ]"]
    end
```

### UI Overlay (After Single Tap)
Tapping the screen reveals the navigation and menu controls.

```mermaid
graph TD
    subgraph Reader_Mode_Overlay
        TopOverlay["[ Back Arrow ]                   [ Menu Icon ]"]
        PDFContent["[ Dimmed PDF Content ]"]
        BottomOverlay["Page 12 of 50  [==========o=========]"]
    end
```

---

## 3. Navigation & Utilities

### Reader Mode Menu
Appears in the top-right corner when the menu icon is tapped.

```mermaid
graph TD
    subgraph Reader_Menu
        Close["Close Reader"]
        Direction["Reading Direction >"]
        Bookmarks["Bookmarks"]
        Settings["Settings"]
    end
```

### Bookmarks View
A list of saved positions within a specific document.

```mermaid
graph TD
    subgraph Bookmarks_View
        TopBar["[ Back ]  Bookmarks"]
        List["(Scrollable List)
        -----------------------
        Page 12 - 'Chapter 2 Start'
        -----------------------
        Page 45 - 'Technical Spec'
        -----------------------"]
        Empty["(If no bookmarks)
        'No bookmarks saved yet'"]
    end
```

### Settings Screen
Global preferences for the application.

```mermaid
graph TD
    subgraph Settings_View
        TopBar["[ Back ]  Settings"]
        General["General
        - Default Reading Mode (Dropdown)
        - Theme (Light/Dark/System)"]
        Storage["Storage
        - Clear Cache
        - Local Scanning Frequency"]
        About["About
        - Version 1.0.0
        - Licenses"]
    end
```

### Onboarding & Permissions
The first-run experience explaining why the app needs storage access.

```mermaid
graph TD
    subgraph Onboarding_View
        Logo["[ App Icon ]"]
        Welcome["Welcome to PDFDriveReader"]
        Desc["To show your PDFs, we need access 
        to your device storage."]
        Action["[ GRANT ACCESS / GET STARTED ]"]
    end
```

---

## 4. Interaction Specs (UX)

| Component | Interaction | Visual Feedback |
| --- | --- | --- |
| **List Item** | Tap | Ripple effect + Transition to Reader Mode |
| **PDF Page** | Single Tap | Toggle UI Overlay (Fade In/Out) |
| **PDF Page** | Double Tap | Reset Zoom to 100% (Animated) |
| **Scrollbar** | Drag | Haptic feedback on page change |
| **Sync Icon** | Visible | Rotating animation (Indeterminate) |

---

## 5. Transition Animations
Consistent motion is used to guide the user's focus and reinforce the reading direction.
- **Reading Directions**:
  - **LTR / RTL**: **Horizontal Slide** animation. Pages slide in from the right (LTR) or left (RTL) with a subtle shadow overlay to simulate depth.
  - **TTB**: **Smooth Vertical Scroll**. No discrete page transitions; the document flows as a single continuous canvas.
- **UI Overlay**:
  - **Toggle Action**: The Top Bar and Bottom Progress Bar use a **Fade-In / Fade-Out** combined with a **Vertical Slide** (8dp) for a "floating" appearance.
- **Mode Transition**: 
  - **Library to Reader**: **Shared Element Transition** on the PDF thumbnail or an **Expand** animation originating from the tapped list item.

---

## 6. Theme & Visual Style (Material 3)
The app uses **Material Design 3** with support for **Dynamic Color** (Android 12+) and a dedicated dark mode.

### Color Palette (Base)
| Element | Light Mode | Dark Mode |
| --- | --- | --- |
| **Primary** | #6750A4 (Deep Purple) | #D0BCFF (Light Purple) |
| **Surface** | #FFFBFE | #1C1B1F |
| **On Surface** | #1C1B1F | #E6E1E5 |
| **Accent** | #03DAC6 (Teal) | #03DAC6 (Teal) |

### Reading Direction Icons
| Mode | Icon (Material Symbols) | Description |
| --- | --- | --- |
| **LTR** | `menu_book` (Left-to-Right) | Standard book icon. |
| **RTL** | `auto_stories` (Right-to-Left) | Book icon flipped or multi-story. |
| **TTB** | `article` or `expand_more` | Vertical document flow icon. |

---

## 7. Layout Grid & Spacing (M3)
- **Margins**: 16dp (Screen edges)
- **Padding**: 8dp (Between list items)
- **Touch Targets**: All buttons/icons are minimum 48x48dp.
- **Typography**:
    - **App Title**: Title Large (22sp)
    - **File Name**: Body Large (16sp)
    - **File Path**: Body Small (12sp, Dimmed)
