# PrioSort Demonstration & Walkthrough Guide

## 1. Quick Verification: Python Simulation

To verify the core reordering principle in 5 seconds from your terminal:

```bash
python python/scripts/simulate_priority.py
```

### Expected Output:
```text
================================================================================
 PRIOSORT: SIMULATION OF EXECUTIVE SUMMARY (SECTION 1.3)
================================================================================

[CHRONOLOGICAL ARRIVAL ORDER (Traditional Android shade)]
Arrival  App              Title                        Received Time
--------------------------------------------------------------------------------
#1       Flipkart         Mega Sale is LIVE!           +1 min
#2       WhatsApp         Rahul                        +2 min
#3       HDFC Bank        Account Debited              +3 min
#4       Google Classroom Assignment Submission Deadline +4 min
#5       HDFC Bank        HDFC NetBanking OTP          +5 min

================================================================================
 [PRIOSORT SMART INBOX: DYNAMICALLY REORDERED]
================================================================================
Position   Arrival   Priority     Score    App              Notification Title
--------------------------------------------------------------------------------
Position #1  Arrival #5  P0 (Critical) 92.5     HDFC Bank        HDFC NetBanking OTP
          |-- Reason: Confidently identified OTP / verification code promoted to Critical (P0)
Position #2  Arrival #4  P1 (High)    71.49    Google Classroom Assignment Submission Deadline
          |-- Reason: Explicit deadline detected (Urgency: 80)
Position #3  Arrival #3  P1 (High)    31.99    HDFC Bank        Account Debited
          |-- Reason: Important financial transaction alert promoted to High (P1)
Position #4  Arrival #2  P2 (Medium)  25.99    WhatsApp         Rahul
          |-- Reason: Personal messaging notification assigned to Medium (P2)
Position #5  Arrival #1  P4 (Minimal) 15.48    Flipkart         Mega Sale is LIVE!
          |-- Reason: Promotional advertisement demoted to Minimal (P4)
================================================================================
```

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
   Install `app-debug.apk` onto an Android emulator or device running Android 8.0 (API 26) through Android 14 (API 34).
2. **Grant Notification Access:**
   The Smart Inbox displays an access banner. Tap "Grant" to open Android's Notification Access settings and toggle PrioSort ON.
3. **Simulate Notification Stream:**
   Tap the floating action button **"Simulate Stream"** in the Smart Inbox. The 5 canonical notifications are immediately injected:
   - Notice that the **HDFC OTP arrives 5th/last**, but **instantly appears at Position #1 (P0 Critical)**!
   - The **Flipkart Sale ad arrives 1st**, but **moves to Position #5 (P4 Minimal)**!
4. **Inspect Explainability:**
   Tap any notification card (e.g. the HDFC OTP or the Google Classroom deadline) to view the detail screen.
   - Inspect the mathematical formula breakdown ($S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R$).
   - Review the rule override explanation.
5. **Test User Feedback:**
   In the detail screen, change the priority from High ($P_1$) to Critical ($P_0$) or Low ($P_3$). Return to the Smart Inbox—observe that the list is re-ranked immediately without restarting the application!
6. **Verify Privacy Settings:**
   Switch to the **Settings** tab:
   - Toggle "Mask Sensitive OTP Codes" ON/OFF.
   - Adjust the retention duration.
   - Check the **Analytics** tab to view priority distributions.
