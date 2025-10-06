# PDFDriveReader

**PDFDriveReader** is a lightweight Android PDF reader with automatic session restoration and multi-directional reading support. Key features include:

- **Three reading modes**: Left-to-right (English novels), right-to-left (Arabic/Japanese), and top-to-bottom (technical documents)
- **Smart session restore**: Automatically reopens your last PDF at the exact page you left off
- **Link navigation with history**: Jump through internal PDF links and swipe back to return to your previous position
- **Flexible viewing**: Pinch to zoom (100%-500%), double-tap to reset, swipe to scroll

## Reference

### UI specs

- Uses [PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer).
- The menu button is located in the top-right corner.
- The menu includes an item to open a new activity that allows selecting a PDF from storage.
- The menu includes a selection item to choose the reading direction: left-to-right, right-to-left, or top-to-bottom.
  - left-to-right is for mainly the English novel reading direction.
  - right-to-left is for mainly the Arabic or Japanese novel reading direction.
  - top-to-bottom is for mainly the Technical document reading direction.
- Users can scroll using swipe gestures.
- Users can zoom using pinch gestures (100%-500%).
  - Double-tapping resets the zoom ratio to the default (100%).
- Left-to-right and Right-to-left views:
  - Users can scroll using the scrollbar located at the bottom of the view.
  - The scrollbar displays the total number of pages and the current page number to its left.
  - The scrollbar is horizontal.
- Top-to-bottom view:
  - Users can scroll using the scrollbar located on the right side of the view.
  - The scrollbar is vertical.
  - The scrollbar displays the total number of pages and the current page number above it.
- The scrollbar appears during a scrolling gesture.
  - There are 2 seconds of delay before it disappears.
- If jumping via an internal link, the previous position is saved. Swiping back returns to the previous position.
- The currently displayed page number is registered in an SQLite cache along with the file path and name.
- When the app starts, it shows the previously opened PDF file and navigates to the page that was displayed at the previous launch.

### Function specs

- Before reading a PDF file, validate the pdf file format.
- If the PDF file is invalid, show an error message and do not open the file.

### Required permissions

No special permissions are required.

## How to use

This app is a basic PDF reader. There is no 'How to Use' guide.

## Tutorial

This app is a basic PDF reader. There is no tutorial.
