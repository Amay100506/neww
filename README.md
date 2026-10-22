# PrioSort

PrioSort is a local-first Android notification inbox. With the user's explicit Notification Access permission it captures notifications, classifies and sorts them in its own inbox, and can play additional sounds based on locally stored rules. It does not reorder the Android notification shade or replace sounds controlled by other apps.

## Android features

- Kotlin, Jetpack Compose, Material 3, Room, DataStore, coroutines and StateFlow.
- Five navigation tabs: Smart Inbox, Sound Studio, Light Studio, Analytics and Settings. Settings also links to the rule simulator.
- Local priority classification, OTP redaction, app exclusions, pinning, snoozing, read state, category preferences, VIP contacts and custom rules.
- Sound rules can match an app, category, available sender identity, priority, focus mode or text keyword. Rules can be edited, previewed, and use a selected file or Android's built-in notification/ringtone/alarm sound, rule/global volume, vibration patterns, repeat count, per-rule cooldown and a per-rule quiet-hours exception. Android may revoke access to selected files; reselect the sound in Sound Studio if playback fails.
- Focus modes, edge-light rules, local rule simulation, feedback-backed rule suggestions, and heuristic burst detection are integrated into the Android runtime. Exact same-app title/body duplicates can be expanded from a grouped Inbox row; similar burst alerts are labeled and later PrioSort sound/light effects are suppressed.
- Explicit action/deadline signals can offer a user-confirmed local reminder. Reminder notifications contain only the source app name, not the notification text; Android may defer scheduled delivery.
- Priority rules can also be drafted using a constrained local natural-language format, previewed as typed fields, and saved only after the user confirms.
- Financial analytics keeps amounts separated by parsed currency, stores heuristic merchant labels locally, charts the last seven days of saved notifications, provides a searchable retained transaction ledger, and reports redundant exact duplicates using the Inbox grouping rules. If the source notification does not identify a currency, PrioSort labels it unknown rather than assuming INR.
- Analytics includes a Vico chart for the actual saved priority distribution, with a text breakdown and screen-reader description; an empty inbox is shown without fabricated chart data.
- Rule backup validates bounded rule definitions, previews the replacement, and asks for confirmation. It excludes notification history and device-specific sound file URIs.
- Appearance settings provide dark/light/system theme, reduced motion, and an adjustable in-app text scale (85%–130%) that compounds with Android's font-size setting.
- Optional biometric app lock gates PrioSort screens on resume and hides app-switcher snapshots. It does not encrypt the local database.
- Notification text is processed on the device. The Android app has no network permission and does not transmit notification text or OTPs. Python code is an offline developer tool and is not connected to the Android runtime.
- Notification access must be enabled separately in Android Settings. Custom sounds are additional PrioSort alerts; the source app may still play its own sound. Android/OEM battery policies may delay listener or scheduled cleanup work.

## Requirements and build

- Android Studio with Android SDK 36 and JDK 17 (or use a compatible Gradle environment).
- Minimum Android 8.0 (API 26).

```shell
cd android
./gradlew testDebugUnitTest assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`, open PrioSort, review the disclosure and enable Notification Access in Android Settings. Verify capture, sound playback and permission behavior on the target device; emulators and OEM builds differ.

The current API 36 debug build is available at `artifacts/PrioSort-debug.apk`; its signature was verified and its SHA-256 is in `artifacts/PrioSort-debug.apk.sha256`. This is a debug-signed APK for testing and sideloading, not a production-signed release. Device and accessibility acceptance are still required before public distribution.

## Tests

Android JVM unit tests:

```shell
cd android
./gradlew testDebugUnitTest
```

Offline Python utility tests (if Python tools are used):

```shell
python -m pip install -r requirements-dev.txt
python -m pytest python/tests -q
```

Python utilities provide synthetic-data training/evaluation, CLI analysis and a dashboard. Their synthetic evaluation is not evidence of production model accuracy and they do not receive data from the Android app.

## Privacy and launch notes

See [docs/privacy.md](docs/privacy.md), [docs/architecture.md](docs/architecture.md) and [docs/model_evaluation.md](docs/model_evaluation.md). Constrained natural-language priority-rule drafting is available locally; optional on-device ML is not wired into the Android app. Before public distribution, add a monitored privacy contact, host the policy, complete store Data safety disclosures, configure release signing, and perform device/accessibility testing. Do not treat notification classification as a high-stakes security decision.

See [docs/feature_status.md](docs/feature_status.md) for the implementation inventory and remaining acceptance work.
See [docs/open_source_tech.md](docs/open_source_tech.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the dependency stack and licensing notes.
