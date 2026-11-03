# FreeScanner

FreeScanner is an offline Android document scanner. It keeps scans in app-private storage, uses Android's system document picker for imports and exports, and requests camera permission only when scanning.

## Features

- Capture single or multi-page scans with CameraX.
- Detect page corners, correct perspective, and apply document filters on device.
- Import multiple images or PDF files with the Android system picker.
- Organize documents and folders, edit pages, and export or share PDFs.
- Search documents by title.

## Build

Open this folder in Android Studio and let it sync the Gradle project. Then run the `app` configuration on an Android 8.0+ device or emulator. The project targets Android API 36 and requires a compatible Android SDK and JDK.

Build a debug APK from the command line with `./gradlew assembleDebug`. The universal APK is written to `app/build/outputs/apk/debug/app-universal-debug.apk`.

## Privacy

The app has no account, cloud sync, analytics, crash reporting, or internet permission. Camera access is optional and used only for scanning. Imported documents are read through Android's Storage Access Framework and copied into app-private storage.

OCR is not enabled yet; the app reports that the local recognition engine is missing instead of storing fabricated text.
