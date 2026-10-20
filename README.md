# PrioSort

## Intelligent Android Notification Prioritization and Reordering System

> **Core Principle:** Importance should determine presentation order, not arrival time alone.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-API%2026--34-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Python](https://img.shields.io/badge/Python-3.11%2B-blue.svg)](https://www.python.org)
[![Scikit-Learn](https://img.shields.io/badge/Scikit--Learn-1.3%2B-orange.svg)](https://scikit-learn.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

---

## 1. Executive Summary

Modern smartphones receive notifications from banking apps, messaging clients, educational portals, shopping apps, social networks, and system services. Traditional chronological arrival order fails when an advertisement arrives first and an important verification code arrives last.

**PrioSort** solves this through on-device contextual classification, explainable mathematical priority scoring, and dynamic reordering in a dedicated **Smart Inbox**.

### The Canonical Demonstration (Section 1.3):

| Arrival Order | Notification | Priority Level | Smart Inbox Position | Justification |
| :---: | :--- | :---: | :---: | :--- |
| **1** | Shopping sale advertisement | **$P_4$ (Minimal)** | **5 (Last)** | Promotional wording, no action/deadline |
| **2** | Ordinary WhatsApp message | **$P_2$ (Medium)** | **4** | Routine personal conversation |
| **3** | Important bank transaction | **$P_1$ (High)** | **2** | High-value debit alert ($\ge 2000$) |
| **4** | College submission deadline | **$P_1$ (High)** | **3** | Imminent deadline due today |
| **5** | Banking OTP | **$P_0$ (Critical)** | **1 (Top)** | Verification passcode, urgent action |

Even though the Banking OTP arrived **5th (latest)**, PrioSort immediately promotes it to **Position #1**!

---

## 2. Project Architecture

```
PrioSort/
|
├── README.md                     # Root project documentation
├── requirements-dev.txt          # Python dependencies
├── .gitignore                    # Version control exclusions
|
├── android/                      # Native Android Application
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle.kts      # Compose, Room, Lifecycle, Coroutines
│       ├── proguard-rules.pro
│       └── src/
│           ├── main/
│           │   ├── AndroidManifest.xml
│           │   ├── java/com/priosort/app/
│           │   │   ├── MainActivity.kt               # Jetpack Compose Navigation & Entrypoint
│           │   │   ├── PrioSortApplication.kt        # Room database DI & Demo Seed Data
│           │   │   ├── capture/                      # NotificationListenerService & Mapping
│           │   │   ├── domain/                       # Core Entities, Categories, Priorities
│           │   │   ├── intelligence/                 # OtpDetector, Banking, Deadlines, PriorityEngine
│           │   │   ├── sorting/                      # Multi-key NotificationSorter
│           │   │   ├── data/                         # Room SQLite Database & Repositories
│           │   │   ├── ui/                           # Jetpack Compose M3 Screens & Components
│           │   │   └── util/                         # TextRedactor & TimeUtils
│           │   └── res/                              # Android Resources (strings, colors, themes)
│           └── test/                                 # Unit & Reordering Tests
│
├── python/                       # ML, Evaluation & Dashboard
│   ├── requirements.txt
│   ├── data/                     # Synthetic Train/Test Datasets & Evaluation Reports
│   ├── models/                   # Serialized ML Models (TF-IDF + Logistic Regression)
│   ├── priosort_ml/              # Preprocessing, Classifier, Evaluator, Priority Simulator
│   ├── scripts/                  # Dataset generator, Training, Evaluation, Simulation scripts
│   ├── dashboard/                # Interactive Streamlit Demonstration Dashboard
│   └── tests/                    # Pytest verification suite
│
└── docs/                         # Detailed Specifications
    ├── architecture.md           # System architecture & component interaction
    ├── priority_policy.md        # Scoring formula S and rule hierarchy
    ├── privacy.md                # Privacy guarantees, local-first storage, redaction
    ├── testing.md                # Comprehensive test strategy
    ├── model_evaluation.md       # ML evaluation report and metrics
    └── demo_guide.md             # Step-by-step walkthrough & verification guide
```

---

## 3. Priority Scoring Model

Base priority is computed via:

$$S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R$$

Where:
- **$U$ (Urgency):** $0$ to $100$ based on intrinsic event urgency.
- **$A$ (Action Requirement):** $100$ (explicit required), $75$ (review requested), $50$ (potential), $0$ (none).
- **$D$ (Deadline Proximity):** Imminent $<1$h ($100$), $<24$h ($90$), $1-2$d ($70$), $3-7$d ($40$), $>7$d ($20$).
- **$I$ (User Importance):** User-configured app/category weight ($0$ to $100$, default $50$).
- **$R$ (Recency Decay):** $R = 100 \times \exp(-\text{age\_hours} / \tau)$ with configurable half-life $\tau = 24.0$h.

### Deterministic Rule Precedence Hierarchy:
1. High-confidence OTP / Verification Code $\to$ Override to $P_0$ (Critical)
2. Credible Fraud Alert $\to$ Override to $P_0$ (Critical)
3. Payment Failure or Urgent Submission Deadline $\to$ Promote to at least $P_1$ (High)
4. Important Financial Transaction Alert $\to$ Promote to at least $P_1$ (High)
5. Promotional Marketing Content $\to$ Demote to $P_4$ (Minimal)
6. Personal Messages / Routine Receipts $\to$ Ensure at least $P_2$ (Medium)

---

## 4. Quick Start & Verification

### 4.1 Run Python Tests & Executive Summary Simulation

```bash
# 1. Install Python requirements
pip install -r requirements-dev.txt

# 2. Run automated tests
pytest python/tests

# 3. Verify Executive Summary reordering in 2 seconds
python python/scripts/simulate_priority.py

# 4. Launch interactive demonstration dashboard
streamlit run python/dashboard/app.py
```

### 4.2 Building and Running Android App

Open the `android/` directory in Android Studio (Giraffe, Hedgehog, Iguana, Jellyfish, or newer) or build from terminal:
```bash
cd android
./gradlew testDebugUnitTest
```

---

## 5. Privacy & Android Platform Guarantees

- **No Remote Transmission:** All notifications are analyzed locally on-device.
- **OTP Redaction:** Passcodes can be automatically redacted (`••••••`) in SQLite storage and UI display.
- **Android Platform Honesty:** Android does not permit arbitrary reordering of the system notification shade by third-party apps. PrioSort delivers its reordering reliably inside its dedicated Smart Inbox.


---

## 6. Advanced Features Added

- 🎯 **Contextual Focus Modes (Work, Sleep, Personal):**
  Dynamic time-of-day shifting: elevates work alerts during office hours, suppresses non-urgent buzzes during sleep, while **$P_0$ Critical OTPs/Fraud alerts remain strictly immune to suppression**.
- 🛡️ **Spam Burst & Near-Duplicate Suppression:**
  Aggregates rapid successive alerts (e.g. 5 messages in 2 minutes) into a single card with an aggregated counter badge.
- 📋 **Dual Inbox Presentation (Ranked Stream vs Priority Tiers):**
  Toggle between a unified sorted stream and structured Priority Section blocks ($P_0$ Critical down to $P_4$ Minimal).
- 🔑 **One-Tap Safe "Copy OTP" Action:**
  Extracts and allows 1-tap clipboard copying of verification passcodes while upholding screen redaction.
- ⚙️ **User Custom Rules Engine:**
  Allows users to define custom keyword $\to$ priority/category mappings stored in Room DB.
- 🧪 **Interactive In-App Notification Sandbox:**
  Test arbitrary notification titles, bodies, and package presets directly in the app to watch real-time reordering live.
- 💾 **JSON Data Export & Import:**
  Complete dataset export with automated sensitive data redaction.

- ⏰ **Smart Notification Snooze & Alarm Escalation (`SnoozePreset`):**
  Temporarily hide non-urgent items for 15 minutes, 1 hour, or tomorrow morning; automatically resurfaces with increased urgency when the snooze window expires.
- 🚚 **Delivery & Logistics Tracker Parser (`DeliveryTracker`):**
  Distinguishes package transitions (Out for delivery today $\to$ $P_1$ High, Delivery failed $\to$ $P_1$ High, Shipped $\to$ $P_2$, Delivered $\to$ $P_3$).
- 💳 **On-Device Financial Ledger & Spend Analytics (`FinanceLedger`):**
  Parses debit and credit amounts, merchants, and accounts completely offline to render a local spend summary and financial ledger.
- ⭐ **VIP Senders & Critical Contacts Whitelist (`VipManager`):**
  Protects messages from family, doctors, or supervisors by elevating VIP communications to High priority regardless of generic category scores.
- 📦 **Notification Archive & Trash Recovery:**
  Archive completed notifications with one tap without deleting them from SQLite history.
