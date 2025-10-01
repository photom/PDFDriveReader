# PDFDriveReader

## PDF Reader

## Reference

### UI specs

- Uses [PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer).
- The menu button is located in the top-right corner.
- The menu includes an item to open a new activity that allows selecting a PDF from storage.
- The menu includes a selection item to choose the reading direction: left-to-right, right-to-left, or top-to-bottom.
- Users can scroll using swipe gestures.
- Users can zoom using pinch gestures.
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
  - There are a few seconds of delay before it disappears.
- If jumping via an internal link, the previous position is saved. Swiping back returns to the previous position.
- The currently displayed page number is registered in an SQLite cache along with the file path and name.
- When the app starts, it shows the previously opened PDF file and navigates to the page that was displayed at the previous launch.

### Function specs

- Before read a PDF file, validate the pdf file format.
- If the PDF file is invalid, show an error message and do not open the file.


### About development

## How to Use

## Tutorial

This app is a basic reader. There are no tutorials.

## Explanation
