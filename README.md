# OPTOMETRIST 57 – Android project

Native Android starter project for an offline optometry register.

## Included in this first source milestone

- Kotlin + Jetpack Compose Android application shell.
- Local Room/SQLite database for patients and consultations.
- Automatic sequential receipt numbering.
- Consultation entry for OD/OS SPH, CYL, AX, DP, ADD, VD, VA and notes.
- Remembered consultation location (until changed).
- Original source photo copied into app-private storage.
- Compact thermal receipt rendered as a 57 mm-class PNG (456 px at 203 dpi).
- PDF receipt generation and Android Share intents for PNG/PDF.
- RawBT handoff through Android Share; RawBT handles the Bluetooth printer connection.
- Local consultation history with re-share/reprint actions.

## Not yet implemented

- Offline handwriting AI/OCR. The current build deliberately does not pretend to read handwriting. This needs a selected and bundled model, tokenizer/pre/post-processing, and validation against real anonymized sample forms.
- Camera capture inside the app (the first milestone imports a photo using Android's photo picker).
- Encrypted export/restore and patient search/edit screens.
- Printer-specific calibration (paper width, printable dot width, margins and density vary by model).

## Build

Open this folder in Android Studio (current stable version), allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Requirements: Android Studio with Android SDK Platform 36 and JDK 17. The build environment used to prepare this source archive did not have the Android SDK/Gradle installed, so no APK has been compiled or device-tested here.

## RawBT

The app shares a PNG image with MIME type `image/png` using Android `ACTION_SEND`. Select RawBT in the Android share sheet. The PDF is also generated and can be shared separately for archiving or other print workflows. Confirm RawBT's image reception and printer paper settings on the actual printer before operational use.

## Data and privacy

The Room database and copied photos are stored in the app's private internal storage. Android backup is disabled in the manifest. Uninstalling the app can delete its local data; implement and test encrypted export/restore before using this as the only patient record.

## Clinical safety

This is an early software prototype, not a validated medical device or clinical decision-support system. OCR output must remain untrusted until manually checked. Missing values remain blank; no prescription value should be inferred or silently corrected.
