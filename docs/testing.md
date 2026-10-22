# Testing status and release test plan

## Automated tests in this repository

Android JVM unit tests cover priority scoring, OTP detection, redaction, sorting, focus/burst rules, deadlines, banking classification, delivery, finance parsing, sound rules, and other local logic. Python unit tests cover the synthetic-data simulator and offline analysis pipeline. These tests do not exercise Android's notification listener against a device, Compose navigation, permission grant/revocation, or OEM background behavior.

Latest recorded run: `testDebugUnitTest assembleDebug` on JDK 17 with Android API 36 completed successfully (58 JVM tests, 0 failures); `python -m pytest python/tests -q` completed successfully (11 tests). The generated debug APK passed `apksigner verify`. These results do not replace device acceptance or a production release build.

Run the current suites with:

```shell
cd android
gradlew.bat testDebugUnitTest
cd ../
python -m pytest python/tests -q
```

The Python suite exercises offline utilities only. There is no HTTP API, remote
account authentication, Android connection setting, or remote inference
fallback in this project. The optional biometric prompt is a local app lock;
Python tools are not connected to the Android runtime.

## Required before public release

- Add Android instrumentation/UI tests for first-run disclosure, accepting/declining, Notification Access enabled/revoked, capture pause, redaction, retention, delete, and list/detail navigation.
- Verify biometric lock enablement, successful/failed/cancelled prompts, returning from background, process recreation, app-switcher screenshots, and behavior after biometric enrollment changes. Confirm notification content remains hidden before unlock and do not treat the app lock as database encryption.
- Verify notification posts, updates, removals, grouped and expanded text, duplicate event keys, and app exclusions on real Android devices.
- Test Android API 26, API 34, and API 36, including at least one device from a manufacturer with aggressive background restrictions. API 26 is the minimum supported version; API 36 is the configured target SDK.
- Test database upgrades from every published version before changing the Room schema. Do not use destructive migration in a public release.
- Add and run a Room migration/round-trip test for the current schema version 10. Schema snapshots for versions 1, 2, and 10 are present; intermediate versions 3–9 are not yet checked in.
- Re-evaluate the Python model on an independently sourced, privacy-reviewed dataset. The current synthetic test split is a development fixture only.
- Run a release build and review Android lint, dependency/security scans, crash behavior, accessibility, and release signing.
