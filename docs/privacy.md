# PrioSort privacy and data handling

**Effective date:** October 22, 2026
**Product:** PrioSort for Android

PrioSort is a local notification inbox. Android provides notification content only after the user grants Notification Access. Enable it only if you want PrioSort to process notifications from other apps.

## Information accessed and processing

The listener can receive notification title/text, posting time, app package and app label, and may read sender identity when the source app exposes it. PrioSort analyzes this information locally for category, priority, OTP, financial-event and action signals. When the user schedules a reminder, WorkManager stores only the local notification record ID; the eventual reminder contains the source app label and generic reminder wording, not the captured message. Notification text and OTPs are not sent to a remote service. The Android app does not request network access. Python tools in this repository are offline development utilities and are not connected to the Android app.

## Storage and redaction

Notification records and custom rules are stored in the app-private Room database. Preferences are stored locally with DataStore. The app disables Android backup. The database is not separately encrypted by PrioSort and relies on Android's app sandbox and device security. The optional biometric app lock hides the app interface and app-switcher screenshots but does not encrypt data. When Hide verification codes is enabled, PrioSort attempts to mask recognized codes and account/card patterns in saved text fields. This heuristic can miss sensitive content. With redaction disabled, original text may be stored. Some locally derived metadata, including transaction amounts, parsed currency and heuristic merchant labels, can also be retained.

## Retention and controls

The retention period is selected in Settings. Cleanup uses WorkManager and app startup, but Android may defer scheduled work. Users can pause capture, exclude apps, change redaction and retention settings, revoke Notification Access in Android settings, or delete saved records from Settings. These controls affect future processing or local records; revoking access does not itself erase existing records.

## Platform and launch notes

Notification access can be interrupted by Android or device-vendor background policies. Custom sounds are additional PrioSort alerts; source apps and Android control their original notification sounds. Selected audio files can be deleted or become inaccessible. Before public distribution, the publisher must add its legal identity and a monitored privacy contact, host this policy at a stable URL, and complete accurate store Data safety disclosures.
