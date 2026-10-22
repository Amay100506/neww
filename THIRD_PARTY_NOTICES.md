# Third-party notices

PrioSort uses third-party open-source software. The following direct Android dependencies are used by the project; most are distributed under Apache License 2.0. JUnit 4.13.2 is a test-only dependency under Eclipse Public License 1.0.

| Component family | Direct version(s) | License |
|---|---|---|
| Android Gradle Plugin | 8.9.1 | Apache-2.0 |
| Kotlin Android / Compose plugins / KSP | 2.0.0 / 2.0.0 / 2.0.0-1.0.24 | Apache-2.0 |
| AndroidX Core, Activity, Biometric, Fragment, Lifecycle, WorkManager, DataStore, Compose, Navigation, Room | See `android/app/build.gradle.kts` | Apache-2.0 |
| Kotlin Coroutines | 1.8.1 | Apache-2.0 |
| Gson | 2.10.1 | Apache-2.0 |
| Vico Compose / Compose M3 | 1.15.0 | Apache-2.0 |
| JUnit | 4.13.2 (test only) | EPL-1.0 |
| AndroidX Test / Espresso | 1.1.5 / 3.5.1 (test only) | Apache-2.0 |

The local Python utilities declare version ranges rather than a lock file. Their direct dependencies and upstream license metadata must be resolved at release time, along with all transitive Android and Python packages. Generate and review a complete dependency notice for the exact release artifacts before publishing. Vico is bundled as a direct Android dependency; no Hush, NotifSilencer, LiteRT, or external model source/artifact is bundled in the APK.

License sources: [AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt), [Gson](https://github.com/google/gson), [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines), [JUnit 4](https://github.com/junit-team/junit4/blob/main/pom.xml), [Hush](https://github.com/vssinghh/hush), [NotifSilencer](https://github.com/chantleyw/NotifSilencer), [LiteRT](https://github.com/google-ai-edge/LiteRT/blob/main/LICENSE), [GLiNER2.5 Decide model card and notices](https://huggingface.co/litert-community/GLiNER2.5-Decide-LiteRT), and [Vico](https://github.com/patrykandpatrick/vico/blob/master/LICENSE).
