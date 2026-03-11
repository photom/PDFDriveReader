# Seamless Reader Interaction Design

## 1. Overview
This design aims to provide a fluid and integrated reading experience where page navigation and zooming are not distinct "modes" but parts of a single, continuous gesture space. The interaction is modeled after high-performance document viewers that prioritize responsiveness and natural physics.

## 2. Behavioral Specifications

### 2.1 Unified Viewport Zoom
- **Behavior**: Zooming applies to the entire document canvas (the viewport) rather than individual pages.
- **Focal Point Stability**: During a pinch gesture, the specific coordinate between the user's fingers (the centroid) must remain visually stationary.
- **Interpolated Zooming**: Changes in scale and offset during a pinch gesture must be smoothed to prevent jitter. The application should use a combination of raw input tracking and subtle interpolation to ensure the transformation feels "glued" to the fingers without micro-stuttering.
- **Focal Point Filtering**: The centroid calculation must be robust against "jumping" when fingers are added or removed during a gesture.
- **Visuals**: When zooming out, multiple pages become visible simultaneously in a concatenated flow. When zooming in, the user can pan across page boundaries seamlessly.

### 2.2 Gesture Hand-off (Panning & Paging)
- **Fluid Transition**: There is no "hard stop" when panning to the edge of a zoomed page. The document continues to scroll to the next or previous page in the same gesture.
- **Integrated Axis Scrolling**: The application must synchronize manual viewport panning with the underlying `LazyList` scrolling. On the primary axis (X for LTR/RTL, Y for TTB), movement should transition seamlessly from panning the zoomed area to scrolling the document ribbon. 
- **Unified Physics Engine**: Flick gestures must trigger a single momentum calculation that handles both viewport offsets and list scroll positions. This prevents "stuttering" or "stacking" when crossing page boundaries.
- **Edge-to-Edge Clamping**: When zooming in, the viewing area must be strictly clamped to the document content.
- **Inertial Panning (Smooth Fling)**: When a user releases a swipe gesture, the document must continue to move with natural momentum using a decay animation. To enhance smoothness during zoomed reading, the initial velocity is slightly amplified to overcome viewport friction.
- **Robust Multi-touch Tracking**: Zoom gestures must be accurately detected regardless of which finger touches the screen first or if fingers are added/removed. The focal point must remain stable throughout these transitions.
- **Natural Friction**: The movement follows platform-standard inertia. If a user swipes and releases, the document continues moving based on the velocity of the swipe and gradually slows down.
- **No Forced Snapping**: As per requirement, the document stays exactly where the user leaves it. No automatic centering or snapping to page boundaries occurs. This includes suppressing any "jump to top" behavior when the current page index changes during a zoom or pan operation.

### 2.3 Visual Continuity
- **Concatenation**: Pages are joined with minimal or no gaps to create a "ribbon" effect.
- **Resolution Up-scaling**: While the user is interacting (pinching/panning), the current bitmaps are scaled using hardware acceleration. Higher-resolution bitmaps are requested from the renderer only after the interaction settles (fingers released and movement stops).

## 3. Technical Strategy (Compose)

### 3.1 Component Hierarchy
- `Box` (Root)
    - `ZoomableViewport` (Custom modifier handling scale/offset)
        - `LazyColumn` / `LazyRow` (The continuous document "ribbon")
            - `PdfPageDisplay` (Individual page items)

### 3.2 State Management
- **Scale**: A single float representing the current magnification of the entire document ribbon.
- **Offset**: A 2D vector representing the translation of the ribbon within the viewport.
- **Page Synchronization**: The `LazyListState` is used to determine which pages are currently in the viewport to trigger background rendering.

## 4. Performance Goals
- **60 FPS Interactions**: Gesture detection and `graphicsLayer` transformations must occur on the UI thread without blocking.
- **Non-blocking Rendering**: PDF rendering continues to happen on `Dispatchers.Default`, updating the `pageCache` reactively.
