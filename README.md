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

## Contributing

Wahon is in active beta, and core architecture/runtime work is still moving fast.
Before implementing large features, open an issue or discussion so we can align on direction and avoid duplicate work.

By submitting a contribution, you agree to the project [Contributor License Agreement](CLA.md).

See [CONTRIBUTING.md](CONTRIBUTING.md) for workflow, development expectations, and review policy.

## Disclaimer

The developer(s) of this application does not have any affiliation with the
content providers available, and this application hosts zero content. See
[DISCLAIMER.md](DISCLAIMER.md) for details.

## License

- App source code is licensed under [GNU GPL v3.0](LICENSE).
- Contributions require agreement with the project [CLA](CLA.md).
- Translation assets are licensed separately under [Apache License 2.0](licenses/LICENSE-TRANSLATIONS-APACHE-2.0.txt).
