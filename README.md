# Wahon

A free and open source cross-platform manga reader built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- Cross-platform: Android and iOS
- Plugin-based extension system for content sources
- Local file reading (CBZ, CBR, PDF)
- Material Design 3 UI with dark/light theme support
- Clean Architecture with Unidirectional Data Flow

## Tech Stack

- **Kotlin Multiplatform (KMP)** — shared business logic
- **Compose Multiplatform (CMP)** — shared UI
- **Ktor** — networking
- **SQLDelight** — local database
- **Koin** — dependency injection
- **Coil 3** — image loading
- **Voyager** — navigation
- **QuickJS** — sandboxed extension runtime

## Building

### Android

```bash
./gradlew :composeApp:assembleDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and build.

## Disclaimer

The developer(s) of this application does not have any affiliation with the
content providers available, and this application hosts zero content. See
[DISCLAIMER.md](DISCLAIMER.md) for details.

## License

[Apache License 2.0](LICENSE)
