# Reader Continuous Mode Design

## 1. Overview
The Reader Continuous Mode transforms the PDF viewing experience from discrete, snapping pages to a fluid, continuous scroll. This mode eliminates the "pagination" concept in favor of a concatenated layout where pages are joined together smoothly.

## 2. Key Requirements
- **No Pagination**: Pages are not treated as separate screens but as elements in a continuous scrollable container.
- **Page Concatenation**: The current page is always accompanied by its immediate neighbors (previous and next), even when the user is not swiping or is zooming.
- **Smooth Scrolling**: Swiping moves the document smoothly without snapping to page boundaries.
- **Natural Inertia**: When a user lifts their finger during a swipe, the document stays at its current position (or continues moving with natural inertia if provided by the platform) without automatically centering a page.
- **Seamless Zoom**: Zooming should affect the concatenated pages as a single unit or ensure that the transition between zoomed pages remains seamless.
- **Double Tap to Reset**: A double-tap gesture on the document must immediately reset the zoom level to 1.0 and center the content.
- **Smooth Panning & Swiping**: Even at high zoom levels, panning within the zoomed area and swiping between pages must remain fluid and responsive.

## 3. Architectural Changes

### 3.1 Presentation Layer (Compose)
- **Unified Scrollable Container**: Replace `HorizontalPager` and `VerticalPager` with `LazyRow` (for LTR/RTL) and `LazyColumn` (for TTB).
- **Custom Scroll Logic**: Ensure `flingBehavior` is set to standard (non-snapping) scrolling.
- **Concatenation Logic**: Ensure that at least 3 pages (previous, current, next) are always kept in the composition and rendered.
- **Zooming**: Implement a zoomable modifier that wraps the scrollable container or individual items in a way that preserves the continuous feel. 
    - *Decision*: A zoomable wrapper around the `Lazy` layout is preferred for a truly continuous zoom, but if that's technically challenging with `Lazy` layouts, an item-level zoom that synchronizes across visible items will be used.

### 3.2 View Model State
- **Scroll Position Tracking**: Instead of just `currentPage`, track a more granular scroll position to restore state accurately.
- **Dynamic Pre-rendering**: The 5-page sliding window cache will be maintained to ensure that neighbor pages are always ready for concatenation.

## 4. Technical Implementation Details

### 4.1 Layout Selection
- **TTB (Top-to-Bottom)**: `LazyColumn`.
- **LTR (Left-to-Right)**: `LazyRow`.
- **RTL (Right-to-Left)**: `LazyRow` with `reverseLayout = true`.

### 4.2 Zooming Strategy
We will use a custom `zoomable` modifier. To maintain concatenation, the zoom should ideally apply to the viewport. 
However, for PDF rendering, high-quality zoom often requires re-rendering at higher resolutions. The `PdfRendererWrapper` already supports rendering at target widths/heights. 

### 4.3 Page Change Detection
Since we are using `Lazy` layouts, we will use `LazyListState.firstVisibleItemIndex` or a more complex calculation based on the center of the viewport to determine the "current" page for the UI (e.g., the slider).

## 5. Security & Performance
- **Bitmap Management**: Ensure that large zoomed bitmaps are managed carefully using the existing `LruCache` to avoid `OutOfMemoryError`.
- **Thread Safety**: Continue performing rendering on `Dispatchers.Default` as per the system architecture.
