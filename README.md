# AERIS AI

AERIS AI is an Android assistant project built with Kotlin and Jetpack Compose. Its development direction is privacy-conscious and local-first, with a focus on chat, voice interaction, conversation memory, and device-aware assistance.

> **Status: Work in progress.** AERIS AI v1.0.1 has a verified signed debug APK and a passing unit-test suite. It is not a production release. Some capabilities remain incomplete or unavailable; offline AI inference, speech recognition, persistent memory, and device actions must be verified individually before being advertised as production-ready.

## Project overview

| Item | Current project configuration |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Application ID | `com.aeris.ai` |
| Minimum Android version | Android 8.0 (API 26) |
| Compile SDK | 36 |
| Target SDK | 36 |
| Version name | 1.0.1 (Gradle configuration) |

The Android app module includes dependencies for Compose, Navigation, Room, DataStore, WorkManager, CameraX, Retrofit/OkHttp, Firebase AI, and Android testing. A dependency being present does not mean its related feature is complete or enabled.

## Intended direction

AERIS is being developed toward:

- An assistant chat experience
- Voice input and spoken responses
- Searchable conversation history and user-controlled memory
- On-device capabilities where device support allows
- Clear permission and privacy controls
- Hardware-aware behavior for Android devices

These are project goals, not a claim that every feature is currently functional.

## Build locally

### Requirements

- Android Studio with Android SDK Platform 36
- A JDK compatible with the Android Gradle Plugin configured by the project
- Internet access for the initial Gradle dependency download

### Build a debug APK

Run this command from the repository root:

```bash
./gradlew :app:assembleDebug
```

The debug APK is normally created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

You can also open the repository in Android Studio, allow Gradle sync to complete, and run the `app` configuration on a compatible Android device or emulator.

## Configuration and secrets

The app uses the Gradle Secrets plugin and supports a local `.env` file with `.env.example` as the template.

- Never commit API keys, passwords, signing keys, service-account files, or private tokens.
- Keep real credentials in local untracked configuration or your CI secret store.
- Google Services configuration is optional at the Gradle configuration level, but features that depend on Firebase may require valid project configuration.

## Release status

AERIS AI v1.0.1 debug APK was assembled successfully and its APK archive and Android signing were verified. The unit-test suite completed successfully (53 tests); a physical-device installation/smoke test has not yet been performed. The APK is signed with a debug key, not a production release key. The build also reported that `google-services.json` is missing, so Firebase-dependent features need valid Firebase configuration before they can be considered ready. The llama.cpp JNI inference bridge is not implemented, so offline local AI inference remains unavailable.

## Contributing

1. Create a branch for your change.
2. Keep changes focused and do not commit secrets or generated build outputs.
3. Run the relevant Gradle tests and build before opening a pull request.
4. Describe what changed and what was actually tested. Mark incomplete work clearly.

## License

No license is currently specified in this repository. Unless a license is added, do not assume that the source is available for reuse, redistribution, or modification under an open-source license.
