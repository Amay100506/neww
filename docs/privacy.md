# PrioSort Privacy and Security Architecture

## 1. Local-First Processing Guarantee

PrioSort is architected with a strict privacy-first principle:
- **Zero Cloud Dependencies:** The core application executes all notification capture, categorization, priority scoring, explainability generation, and SQLite persistence on the local Android device.
- **No Third-Party APIs:** PrioSort does not transmit notification bodies, titles, metadata, or auth tokens to remote LLMs or external cloud services.
- **Works Fully Offline:** All intelligence features operate without active cellular or Wi-Fi connectivity.

---

## 2. On-Device Sensitive Data Redaction

Verification codes, one-time passwords, and banking identifiers contain sensitive access privileges. PrioSort implements proactive masking via `TextRedactor`:
- **OTP Code Masking:** When redaction is enabled (default), detected numeric passcodes are masked with bullets (`••••••`) prior to persistence in Room SQLite and before rendering in UI cards.
- **Card and Account Masking:** Financial card and bank account numbers are masked to only their trailing four digits (`••••8912`).

---

## 3. Privacy Controls Available to the User

1. **Explicit Permission Grant:**
   PrioSort only captures notifications after the user explicitly grants Notification Listener access through Android's dedicated system settings (`ACTION_NOTIFICATION_LISTENER_SETTINGS`).
2. **Global Capture Pause:**
   Users can pause notification ingestion at any time with a single toggle in the Settings screen. When paused, incoming notification events are discarded immediately.
3. **Application Exclusions:**
   Users can exclude specific sensitive applications (e.g. password managers, health apps, private messengers). Excluded app notifications are filtered out before reaching the intelligence pipeline.
4. **Configurable Data Retention:**
   Users can configure automated cleanup between $1$ and $30$ days. Expired records are deleted from SQLite.
5. **Complete Data Deletion:**
   The Settings screen provides a dedicated "Clear All Local Data" action that completely wipes all stored notifications and priority scores.
6. **No Cloud Backup Leakage:**
   PrioSort explicitly configures `data_extraction_rules.xml` and `backup_rules.xml` to prevent Android cloud backup from copying the internal SQLite database off the device.
