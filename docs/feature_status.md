# Feature status against the product specification

This inventory reflects the Android source currently in this repository. “Implemented” means a code path exists; it does not mean it has passed migration, accessibility, or physical-device acceptance checks. The Android API 36 debug build succeeds and all 58 JVM unit tests pass; the Python suite passes 11 tests. No physical-device verification is recorded.

## Implemented in the Android source

- Notification listener capture, local classification and scoring, explanations, event-key deduplication, repost updates, system-removal state, app exclusion, pause capture, and manual category/priority corrections.
- Smart Inbox search/filtering, details, pin/read/archive/snooze/delete controls, retention cleanup, OTP redaction controls, and privacy disclosure/permission guidance.
- Sound Studio rules for app, category, sender, keyword, priority, and focus mode; rule editing, file and built-in sound presets, preview, volume, vibration pattern, repeat count, cooldown, per-rule quiet-hours exception, ringer/DND checks, and audio focus handling. Live capture and the dry-run simulator share a side-effect-free sound decision policy.
- Light Studio rules, local preview, overlay permission flow, edge-pattern controller, and a global enable setting.
- Dry-run rule simulator for priority, sound, and light decisions. It does not persist the sample or execute alerts.
- Study, Work, Sleep, Personal, and Custom focus modes, schedules, package/category allowlists.
- Explicit notification feedback, manually corrected categories/priorities, and a confirmation step before saving suggested rules.
- Rule-only versioned JSON import/export for custom priority, sound, and light rules, with size/count/field checks, no device-specific sound URI export, preview/confirmation, and transactional replacement.
- Vico priority distribution chart from local saved counts, with an accompanying accessible text breakdown and empty state.
- Optional biometric app lock that gates the Compose app surface on app resume and prevents app-switcher screenshots; database encryption remains unimplemented.
- Local aggregate priority, OTP, feedback, burst, exact-duplicate, and manual-override counts; rates use the retained, non-archived inbox population. Exact duplicates use the same package/title/body normalization and three-minute clusters as Inbox grouping. Analytics includes a seven-day local notification timeline and searchable retained transaction ledger with extracted merchant labels and currency-aware amounts.
- Heuristic burst flagging for similar notifications from one app in a three-minute window. Once the detector recognizes a burst, additional PrioSort sound/light effects are suppressed. The Inbox also collapses exact same-app title/body duplicates within three minutes into expandable groups; original records and actions remain available.
- Conservative deadline parsing with explicit resolved timestamps where available, unresolved/ambiguous status for vague signals, and user-confirmed local reminders for action/deadline alerts.
- Constrained natural-language priority-rule drafting, parsed locally into typed fields and shown for review before save.
- Transaction analytics grouped by extracted currency; old records without a known currency remain labeled as unknown instead of being misreported as INR.

## Partial or not implemented

- Approximate bursts that are similar but not exact duplicates are labeled but remain separate rows. Exact-duplicate analytics counts redundant copies with the Inbox's three-minute grouping rule, divided by the retained non-archived inbox population; this metric has not yet been validated against a representative real-world dataset.
- Optional on-device ML has no Android runtime model, feature flag, model card, or measured on-device benefit. The Python model is an offline developer utility only.
- Reminders are approximate WorkManager schedules and can be deferred by Android power policy; the app does not create calendar events or complete notification actions.
- Sound fallback profiles, a curated sound library beyond Android's current notification/ringtone/alarm defaults, and direct conflict management are not implemented; conflicts are displayed in the simulator and resolved deterministically by specificity and rule ID.
- Optional diagnostics consent and database encryption are not implemented. The database remains app-private and relies on Android's sandbox; biometric app lock is a visual access gate, not storage encryption.
- Exact-duplicate rate counts redundant copies over all retained inbox notifications, including rows without title/body text; the merchant parser is heuristic and should be evaluated across supported banking notification formats.
- Searchable settings, reduce-motion preference, and an in-app text scale (85%–130%, compounded with Android's scale) are implemented; a complete screen-reader/accessibility audit remains outstanding.

## Acceptance work still required

- Add Room migration and database round-trip coverage for schema 10. The current API 36 debug build and all 58 JVM unit tests pass using JDK 17. A verified debug-signed APK is available at `artifacts/PrioSort-debug.apk`; it is for sideloading/testing and is not a production-signed release.
- Verify listener permission grant/revocation, update/removal behavior, overlay behavior, audio/vibration policy, process restart, reboot, and retention on supported physical devices.
- Verify biometric lock success/cancel/failure, app background/restore, process recreation, biometric enrollment changes, and locked app-switcher snapshots on supported Android versions.
- Check all screens with Android accessibility tools and large font settings.
- Complete production privacy review, release signing, store disclosures, and operational support details before public distribution.
