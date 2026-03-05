# Reader Continuous Mode Design

## 1. Overview
The Reader Continuous Mode transforms the PDF viewing experience from discrete, snapping pages to a fluid, continuous scroll. This mode eliminates the "pagination" concept in favor of a concatenated layout where pages are joined together smoothly.

## 2. Key Requirements
- **No Pagination**: Pages are not treated as separate screens but as elements in a continuous scrollable container.
- **Pre-loaded Page Canvas**: When a document is loaded, the application retrieves the exact dimensions of every page. This allows the scrollable container to allocate precise physical space for the entire document upfront, preventing layout shifts and treating the document as "one big page" with defined gaps between the individual page items.
- **Page Concatenation**: The current page is always accompanied by its immediate neighbors (previous and next), even when the user is not swiping or is zooming.
- **Smooth Scrolling**: Swiping moves the document smoothly without snapping to page boundaries.
- **Natural Inertia**: When a user lifts their finger during a swipe, the document stays at its current position (or continues moving with natural inertia if provided by the platform) without automatically centering a page.
- **Seamless Zoom**: Zooming should affect the concatenated pages as a single unit or ensure that the transition between zoomed pages remains seamless.
- **Double Tap to Reset**: A double-tap gesture on the document must immediately reset the zoom level to 1.0 and center the content.
- **Smooth Panning & Swiping**: Even at high zoom levels, panning within the zoomed area and swiping between pages must remain fluid and responsive.

## 3. Architectural Changes

### 3.1 Presentation Layer (Compose)
- **Unified Scrollable Container**: Replace `HorizontalPager` and `VerticalPager` with `LazyRow` (for LTR/RTL) and `LazyColumn` (for TTB).
- **Proportional Layout**: The `PdfPageDisplay` components utilize the pre-loaded page dimensions to enforce a strict aspect ratio, ensuring the scrollbar and content boundaries are accurate before bitmaps are rendered.
- **Custom Scroll Logic**: Ensure `flingBehavior` is set to standard (non-snapping) scrolling.
- **Concatenation Logic**: Ensure that at least 3 pages (previous, current, next) are always kept in the composition and rendered.
- **Zooming**: Implement a zoomable modifier that wraps the scrollable container or individual items in a way that preserves the continuous feel. 

### 3.2 View Model State
- **Scroll Position Tracking**: Instead of just `currentPage`, track a more granular scroll position to restore state accurately.
- **Dynamic Pre-rendering**: The 5-page sliding window cache will be maintained to ensure that neighbor pages are always ready for concatenation.

### 3.3 Domain Layer
- **PdfDocument Entity**: Expanded to include a `pageSizes` list representing the physical dimensions of every page in the document, populated during initialization.

## 4. Technical Implementation Details

### 4.1 Layout Selection
- **TTB (Top-to-Bottom)**:
    - **Implementation**: `LazyColumn` with standard vertical scrolling.
    - **Seamlessness**: Pages are concatenated vertically. Panning at high zoom allows moving vertically across boundaries and horizontally within the zoomed width.
- **LTR (Left-to-Right)**:
    - **Implementation**: `LazyRow` with standard horizontal scrolling.
    - **Seamlessness**: Pages are concatenated horizontally. Swiping moves the document from left to right.
- **RTL (Right-to-Left)**:
    - **Implementation**: `LazyRow` with `reverseLayout = true`.
    - **Seamlessness**: Pages are concatenated horizontally. Swiping moves the document from right to left (Manga/Arabic style). The "ribbon" start is on the right side of the viewport.

### 4.2 Zooming Strategy (All Directions)
The viewport zoom logic is direction-agnostic. It applies a `graphicsLayer` transformation to the entire scrollable container (Row/Column).
- **Coordinate Consistency**: The focal point calculation must correctly map the viewport centroid to the logical coordinate of the ribbon, regardless of whether it's a `LazyRow` or `LazyColumn`, or if it's reversed (RTL).

### 4.3 Page Change Detection
Since we are using `Lazy` layouts, we will use `LazyListState.firstVisibleItemIndex` or a more complex calculation based on the center of the viewport to determine the "current" page for the UI (e.g., the slider).

## 5. Security & Performance
- **Bitmap Management**: Ensure that large zoomed bitmaps are managed carefully using the existing `LruCache` to avoid `OutOfMemoryError`.
- **Thread Safety**: Continue performing rendering on `Dispatchers.Default` as per the system architecture.
