# Android PdfRenderer API Reference

## Core Lifecycle
1. **Open ParcelFileDescriptor**: `context.contentResolver.openFileDescriptor(uri, "r")`
2. **Initialize Renderer**: `PdfRenderer(pfd)`
3. **Open Page**: `renderer.openPage(index)`
4. **Render**: `page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)`
5. **Close Page**: `page.close()`
6. **Close Renderer**: `renderer.close()`

## Important Constraints
- **Threading**: `PdfRenderer` is NOT thread-safe. All calls (open, render, close) for a single instance must happen on the same thread (usually a dedicated background thread).
- **Page Access**: Only one page can be open at a time per renderer instance.
- **Bitmap Config**: Use `Bitmap.Config.ARGB_8888`. Ensure the bitmap is cleared (white background) before rendering if the PDF has transparency.

## Memory Management
- **Bitmap Pooling**: Reuse bitmaps of the same size to avoid GC pressure.
- **Tiling**: For high zoom levels, render tiles instead of the full page bitmap to stay within memory limits.
- **Scaling**: Calculate the destination bitmap size based on the device DPI and current zoom level:
  ```kotlin
  val densityDpi = context.resources.displayMetrics.densityDpi
  val width = (page.width * densityDpi / 72)
  val height = (page.height * densityDpi / 72)
  ```
