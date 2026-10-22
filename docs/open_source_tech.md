# Open-source technology register

## Android runtime

| Area | Dependency | Use |
|---|---|---|
| Build | Android Gradle Plugin 8.9.1, Gradle 8.11.1 | Android packaging |
| Language and Compose compiler | Kotlin 2.0.0, Kotlin Compose plugin 2.0.0, KSP 2.0.0-1.0.24 | Kotlin, Jetpack Compose and Room code generation |
| UI | Compose BOM 2024.06.00, Material 3, Navigation Compose 2.7.7 | Native interface and navigation |
| Local storage | Room 2.6.1 schema 10, DataStore Preferences 1.1.1 | Notification history, transaction merchant metadata, rules and preferences |
| Background work | Kotlin Coroutines 1.8.1, WorkManager 2.9.0 | Reactive flows and deferrable maintenance/reminders |
| Serialization | Gson 2.10.1 | Bounded, versioned rules-only backup |
| Device authentication | AndroidX Biometric 1.1.0 | Optional app-surface lock using system biometric prompts |
| Analytics charts | Vico Compose and Compose M3 1.15.0 | Visual priority distribution from saved local counts |
| Unit tests | JUnit 4.13.2 | JVM tests |

Android dependencies are declared in `android/app/build.gradle.kts`; verify resolved transitive dependencies and notices for each release. Python tools under `python/` are offline-only and do not connect to the Android app. Their packages are listed in `requirements-dev.txt` and `python/requirements.txt`.

## Reviewed, not bundled

- Hush and NotifSilencer informed product research only; no source code from either project is copied into PrioSort.
- LiteRT and the GLiNER2.5 Decide model were evaluated as possible future on-device classification technology. They are not included because the current model evidence is synthetic and does not justify shipping an unvalidated runtime/model.

The current app uses deterministic local rules and keeps notification content on device. Vico uses locally saved priority counts and is accompanied by a text breakdown and accessible description. Its license and the direct dependency notices are recorded in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
