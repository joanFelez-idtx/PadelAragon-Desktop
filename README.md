# PadelAragon Desktop

Windows/Linux/macOS desktop port of [PadelAragon](https://github.com/JoanFelez/PadelAragon), built with
[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) for the JVM/desktop target.

This project reuses the original app's data models, HTML parsers, repositories, use cases, and most of the
Jetpack Compose UI. The few Android-only pieces (Room database bootstrap, favorites storage, navigation,
theming, entry point) were re-implemented for desktop:

| Concern              | Android                          | Desktop                                        |
|-----------------------|-----------------------------------|-------------------------------------------------|
| Persistence           | Room + Android `Context`          | Room (JVM target) + `BundledSQLiteDriver`, DB file under `~/.padelaragon` |
| Favorites storage     | `SharedPreferences`               | `java.util.prefs.Preferences`                   |
| Navigation            | `androidx.navigation-compose`     | Simple in-memory back stack (`AppNavHost.kt`)   |
| Theme                 | Dynamic status bar coloring       | Static Material3 color scheme                   |
| Entry point           | `MainActivity` / `Application`    | `main()` + Compose Desktop `Window`             |
| Logging               | `android.util.Log`                | `Logger` (println-based)                        |

## Running locally

```bash
./gradlew run
```

## Building the Windows installer (.exe)

Native installers must be built on the target OS. A GitHub Actions workflow
(`.github/workflows/windows-exe.yml`) runs on `windows-latest` and builds the `.exe`/`.msi` automatically on
every push to `main` and on demand (workflow_dispatch), uploading the installer as a workflow artifact.

To build it yourself on a Windows machine:

```powershell
.\gradlew.bat packageExe
```

The installer will be generated under `build\compose\binaries\main\exe`.

## Data & network

The app is a purely read-side client of the public padelfederacion.es pages (via OkHttp + Jsoup), matching
the same scraping/parsing logic as the Android app.
