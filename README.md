# Metronome

Metronome is a native Android practice app for running structured rhythm or exercise sessions.

Instead of using a single fixed timer, the app lets you define a list of sections. Each section contains:

- `repeatNum`: how many beats/repetitions to count
- `length`: interval between repetitions, in milliseconds
- `delay`: rest time after the section, in milliseconds

During playback, the app announces counts with Android Text-to-Speech, highlights the active section, and can loop user-selected background music from local device storage.

## What the App Does

- Plays a multi-section metronome/training routine
- Announces counts with TTS in Chinese
- Supports start, stop, and pause/resume
- Lets users edit and persist section definitions locally
- Supports batch-updating the repeat count across all sections
- Lets users choose local background music from MediaStore
- Checks a remote `version.json` file and triggers in-app APK update download

## Main User Flow

1. Launch into `PlayActivity`
2. Review the configured section list
3. Optionally open section settings and add/edit sections
4. Optionally select background music from local storage
5. Start playback
6. The app counts repetitions, waits for each section delay, then advances to the next section

## Project Overview

This repository is a single-module Android app:

- Gradle root project: `Metronome`
- App module: `app`
- Package: `com.lttclaw.metronome`
- Min SDK: `24`
- Target/Compile SDK: `34`
- Current app version: `1.0.4` (`versionCode = 4`)

The active launcher activity is `PlayActivity`.

`MainActivity` still exists in the codebase, but based on the manifest it is not the launcher and appears to be an older standalone counter/timer screen kept for legacy reference.

## Architecture

The app uses a lightweight MVVM-style structure:

- UI layer: Activities under `app/src/main/java/com/lttclaw/metronome/ui`
- ViewModels: playback, section editing, and music selection state under `.../viewmodel`
- Models: `Section`, `MusicItem`, `Version`
- Persistence: `SPUtils` shared preferences store section data and background music selection
- Networking: Retrofit-based version check in `.../network`
- Binding/UI helpers: Android Data Binding + BRV RecyclerView helpers

Important classes:

- `PlayActivity`: main playback screen
- `PlayViewModel`: countdown logic, TTS, MediaPlayer integration, version check
- `SectionListActivity`: edit and save section list
- `SelectMusicActivity`: choose a background music file from local storage
- `VersionService`: fetches remote version metadata for update checks

## Project Structure

```text
Metronome/
|- app/
|  |- src/main/java/com/lttclaw/metronome/
|  |  |- MainApplication.kt
|  |  |- MainActivity.kt
|  |  |- model/
|  |  |- network/
|  |  |- ui/
|  |  \- viewmodel/
|  \- src/main/res/
|- build.gradle.kts
|- settings.gradle.kts
\- gradle.properties
```

## Key Dependencies

- AndroidX AppCompat, Lifecycle, Fragment, Navigation
- Kotlin coroutines
- Android Data Binding
- [JetpackMvvm](https://github.com/hegaojian/JetpackMvvm)
- [BRV](https://github.com/liangjingkanji/BRV)
- [PermissionX](https://github.com/guolindev/PermissionX)
- [AppUpdate](https://github.com/Azhon/AppUpdate)
- Blankj `utilcodex`
- Material Dialogs

## Build and Run

### Requirements

- Android Studio with AGP 8.4.2 support
- JDK 8 or newer
- Android SDK 34

### Debug Build

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

### Install to a Device

```bash
./gradlew installDebug
```

## Permissions

The app requests:

- `INTERNET` for version checking and APK updates
- `WAKE_LOCK` to help keep the session active
- `READ_MEDIA_AUDIO` on Android 13+
- `READ_EXTERNAL_STORAGE` on Android 12 and below

## Data and Storage

The app stores the following locally in shared preferences:

- Section list JSON
- Background music URI
- Background music display name

There is no database layer in the current implementation.

## Remote Update Check

The app currently checks:

- Base URL: `http://www.smallfurrypaw.top/`
- Endpoint: `files/version.json`

`VersionService` expects a response shaped like:

```json
{
  "errorCode": 0,
  "errorMsg": "",
  "data": {
    "apkUrl": "https://example.com/app.apk",
    "versionCode": 5,
    "versionName": "1.0.5",
    "description": "Update notes",
    "apkSize": "12 MB"
  }
}
```

## Development Notes

- Section timing values are stored in milliseconds.
- `PlayViewModel` drives the playback flow with `CountDownTimer`, `TextToSpeech`, and `MediaPlayer`.
- RecyclerView item rendering is handled through BRV with data binding.
- The repository currently includes release signing configuration in `app/build.gradle.kts`. Replace that with your own secure signing setup before publishing or sharing the project.
- The repository also contains generated `app/build/` artifacts. Those are not source files and usually should not be committed.

## Known Maintenance Observations

- The visible UI strings in the source files appear to target Chinese users.
- Some source/resource files appear to have encoding issues when read in a UTF-8 terminal, so verify file encodings before doing broad string cleanup.
- Test coverage is minimal; only template unit/instrumentation tests are present.

