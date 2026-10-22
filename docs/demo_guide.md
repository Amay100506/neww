# PrioSort Demonstration & Walkthrough Guide

## 1. Quick Verification: Python Simulation

To verify the core reordering principle in 5 seconds from your terminal:

```bash
python python/scripts/simulate_priority.py
```

The script prints the five synthetic notifications in arrival order, then prints
their classification, score, priority, explanations, and sorted order. It also
prints checks that the OTP is first and the promotion is last. Exact scores and
tie ordering may change with the simulator or model.

---

## 2. Interactive Web Dashboard (Streamlit)

Launch the demonstration dashboard:

```bash
streamlit run python/dashboard/app.py
```

Features in the Dashboard:
1. **Smart Inbox Simulator:** View side-by-side comparison between the arrival stream and PrioSort's dynamically sorted list.
2. **Executive Summary Scenario:** One-click automated verification of the 5-notification priority hierarchy.
3. **Evaluation & Metrics:** Interactive charts of accuracy, macro-F1, per-class metrics, and OTP detector confusion matrix.
4. **Single Notification Inspector:** Type any title, body, and package name to see real-time classification, formula breakdown ($S, U, A, D, I, R$), and explainability trace.

---

## 3. Android Application Demonstration

1. **Install and Launch:**
   Install the supplied debug APK onto an Android emulator or device running Android 8.0 (API 26) or newer. The app targets API 36.
2. **Grant Notification Access:**
   The Smart Inbox displays a capture status card. Tap **Enable** to open Android's Notification Access settings and turn PrioSort on. Review and accept the in-app disclosure first.
3. **Capture real notifications:**
   Post notifications from other apps while PrioSort has Notification Access. They appear in the Smart Inbox after local processing. The Android app does not include a simulated-stream button. For a reproducible five-item example, run the separate Python simulation in section 1; it does not insert records into Android.
4. **Inspect Explainability:**
   Tap any notification card (e.g. the HDFC OTP or the Google Classroom deadline) to view the detail screen.
   - Inspect the mathematical formula breakdown ($S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R$).
   - Review the rule override explanation.
5. **Test manual priority:**
   Open a notification's detail screen and choose a priority in its priority section. Return to the Smart Inbox; the saved priority is reflected in the list.
6. **Verify Privacy Settings:**
   Switch to the **Settings** tab:
   - Toggle **Hide verification codes** ON/OFF. This applies heuristic masking to recognized codes and account patterns in stored text.
   - Adjust the retention duration.
   - Check the **Analytics** tab to view priority distributions.
