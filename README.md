# Amblify Android Project (V1.0)
Dynamic AI Ambient Wallpaper Manager for Android.

## Building the APK (.apk)

### Option 1: Android Studio (Recommended)
1. Extract this zip file into a folder.
2. Open **Android Studio** (Ladybug / Koala / Jellyfish).
3. Select **File > Open** and choose the extracted folder.
4. Allow Gradle sync to complete.
5. In the top menu, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Once built, click **locate** or find the APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Command Line (Gradle)
```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

The output debug APK will be created at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing the APK to a Device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture & Features
- **WorkManager (`WallpaperWorker.kt`)**: Periodic background ambient rotation with network & battery constraints.
- **BootReceiver (`BootReceiver.kt`)**: Restores periodic worker schedule upon device restart.
- **Jetpack Compose (`MainActivity.kt`)**: Modern declarative UI with edge-to-edge system bars and Material You palette theming.
- **WallpaperRepository (`WallpaperRepository.kt`)**: Manages curated ambient presets, user preferences, and time-of-day matching.
