# FreeScanner: Build Spec for Coding Agents
Working name. A free, open-source, fully offline document scanner for Android. No ads, no account, no tracking, no bloat.

## 1. What we are building
A CamScanner alternative that does the essential things well: scan documents with the camera, clean them up, organize them, and export or edit PDFs. Everything runs on the device. The app never needs the internet and has no cloud, login, or analytics.
Target: Android (minSdk 26, targetSdk latest stable), Kotlin, Jetpack Compose. Published on GitHub and F-Droid. License: GPLv3 or Apache 2.0.
Non-goals (do not build): cloud sync, accounts, subscriptions, ads, AI chat, solver, translation, slides, whiteboard, timestamp camera, print, To Word/Excel/PPT, ID photo maker, photo restore.
Hard rules:
• No Firebase, no Google Play Services dependency, no ML Kit, no analytics or crash-reporting SDKs. It must build and run on F-Droid.
• Only the CAMERA permission. Storage goes through the system file picker (SAF) and app-private storage.
• Small APK. Enable R8 and resource shrinking. Ship per-ABI splits.
• No copied CamScanner names, icons, or UI assets.

## 2. Tech stack
• Language/UI: Kotlin, Jetpack Compose, Material 3, Navigation Compose
• Architecture: MVVM with a repository layer, Kotlin coroutines and Flow
• Camera: CameraX
• Image processing: OpenCV for Android / Native bitmap & canvas processing (edge detection, perspective warp, filters)
• Database: Room
• PDF: Android PdfDocument for creating, PdfRenderer for rendering pages, PDF manipulations
• OCR: Tesseract via tesseract4android, with English bundled and other languages importable
• Images: Coil
• Tests: JUnit, Turbine, Compose UI tests

## 3. Project structure
Single Gradle module to start, with clear packages:
• ui/ screens and components (home, camera, crop, edit, library, viewer, tools, settings)
• data/ Room entities and DAOs, repositories, file storage
• domain/ use cases (scan pipeline, export, merge, OCR)
• imaging/ image processing wrappers: edge detection, perspective warp, filters
• pdf/ PDF create, merge, split, encrypt, compress, watermark, sign
• ocr/ Tesseract wrapper and language pack manager
Keep imaging/, pdf/, and ocr/ free of Android UI code so they can be unit tested.

## 4. Data model
• Folder: id, name, createdAt
• Document: id, title, folderId (nullable), createdAt, updatedAt, pageCount, thumbnailPath, isLocked
• Page: id, documentId, orderIndex, originalPath, processedPath, cornersJson, filter, rotation, ocrText (nullable)
Original images are always kept so edits are non-destructive. The processed image is regenerated from the original, the crop corners, the filter, and the rotation. All files live in app-private storage under documents/<docId>/.

## 5. Screens
1. Home: big Scan button, Import Images, Import Files, search bar, recent documents, a Tools shortcut.
2. Camera: live preview with edge overlay, auto-capture toggle, flash, batch mode with a page counter, a capture button, and a Done button.
3. Crop: four draggable corner handles with a magnifier, auto-detected on load, plus rotate.
4. Edit: filter strip (Original, Auto, Magic Color, B&W, Grayscale), brightness and contrast, rotate, and an add-page button.
5. Library: list or grid, folders, sort, multi-select, rename, delete, move.
6. Document viewer: page pager, reorder pages, delete page, extract pages, Share, Export, OCR.
7. Tools: Merge, Extract Pages, Reorder, PDF to Images, Compress, Lock, Unlock, Sign, Watermark, ID Card, Extract Text.
8. Settings: default filter, default PDF page size (A4 or Letter), image quality, theme (system, light, dark), OCR languages, About and licenses.

## 6. Features and acceptance criteria
### Smart Scan
• Edge detection via grayscale, blur, Canny, contours, then the largest quadrilateral.
• Perspective correction with perspective transform and warp.
• Filters are done with adaptive threshold (B&W), CLAHE and shadow removal (Auto), and saturation and contrast boost (Magic Color).
• Done when: a photo of a skewed page on a desk becomes a flat, readable page in under 2 seconds on a mid-range phone.
### Import Images / Import Files
• Multi-select from the system picker. Images go through the same crop and filter flow. PDFs are rendered to page images and become editable documents.
### Library
• Room-backed list with search by title and OCR text, folders, sorting, multi-select actions.
### Export
• PDF (one file, chosen page size, JPEG quality setting) and images (JPG or PNG). Share through the system share sheet or save through SAF.
### Merge, Reorder, Extract Pages, PDF to Images
• Operate on documents in the library or on imported PDFs. Show a progress indicator and never block the UI thread.
### Extract Text (OCR)
• Tesseract on the processed page image. Show editable text, with Copy and Share as .txt. Optional: embed an invisible text layer in the exported PDF so it is searchable.
• Language packs (.traineddata) are importable from a file. English ships in the app.
### Sign
• Draw a signature on a canvas, save it as a transparent PNG, then place, resize, and flatten it onto a page.
### Lock / Unlock
• PDF protection with AES password security.
### Compress
• Re-encode page images at lower JPEG quality and optionally downscale. Offer Low, Medium, and High presets and show the estimated size.
### Add Watermark
• Text watermark with size, opacity, angle, and position. Applied when exporting.
### ID Card
• Capture the front and back, auto-crop each, and place both on one A4 page.

## 7. Build order (milestones)
• M0: project setup, theme, navigation, Room, CI
• M1: scan pipeline (camera, crop, filters, save pages)
• M2: library, document viewer, import
• M3: PDF export, merge, reorder, extract, PDF to images
• M4: OCR
• M5: Sign, Lock, Compress, Watermark, ID Card
• M6: polish, accessibility, shrinker config, F-Droid metadata, README
