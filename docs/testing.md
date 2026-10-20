# PrioSort Testing Strategy & Verification Guide

## 1. Testing Pyramid

PrioSort maintains automated tests across both its Python ML/evaluation module and its Kotlin Android application:

```text
               +--------------------------------------+
               |    Android End-to-End Simulation     |
               | (Executive Summary 5-Item Reordering)|
               +--------------------------------------+
               |      Kotlin Unit & Rule Tests        |
               | (OtpDetector, Banking, Priority, Sort)|
               +--------------------------------------+
               |     Python ML Evaluation & Pytest    |
               |  (Accuracy, Macro-F1, OTP Precision) |
               +--------------------------------------+
```

---

## 2. Python ML & Priority Simulation Tests

### 2.1 Running the Pytest Suite
```bash
python -m pytest python/tests
```
Verifies:
- Genuine OTP detection with diverse phrasing.
- Strict rejection of false positives (order IDs, tracking numbers, booking references).
- Mathematical formula evaluation $S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R$.
- Rule precedence overrides (OTP $\to P_0$, Promotional $\to P_4$).
- Smart Inbox sorting invariants.

### 2.2 Running Executive Summary Simulation
```bash
python python/scripts/simulate_priority.py
```
Outputs the traditional Android chronological arrival order alongside the PrioSort Smart Inbox reordered positions, verifying that the Banking OTP (arrived 5th) ranks #1, while the shopping advertisement (arrived 1st) ranks #5.

---

## 3. Kotlin Android Unit Tests

Location: `android/app/src/test/java/com/priosort/app/`

1. `OtpDetectorTest`:
   - `testGenuineOtpDetection`: 5 diverse OTP formats.
   - `testFalsePositiveRejection`: 5 non-OTP reference numbers.
   - `testEmptyOrNullText`: Null/blank text handling.
2. `BankingClassifierTest`:
   - `testFraudAlert`: Fraud pattern detection.
   - `testPaymentFailure`: Payment failure pattern.
   - `testImportantTransaction`: High-value debit alert $\ge 2000$.
   - `testRoutineTransaction`: Coffee debit via UPI.
   - `testBankingPromotion`: Pre-approved credit card advertisement.
3. `PriorityEngineTest`:
   - `testMathematicalFormulaCalculation`: Mathematical weights verification.
   - `testOtpRuleOverridePromotedToCritical`: Confirms promotion to $P_0$.
   - `testPromotionalDemotedToMinimal`: Confirms demotion to $P_4$.
4. `NotificationSorterTest`:
   - `testExecutiveSummaryReorderingScenario`: Exact simulation of the Section 1.3 5-notification scenario.
   - `testPinnedPrecedence`: Verifies user pin outranking priority ranks.
5. `TextRedactorTest`:
   - `testOtpRedaction`: Verification code masked with `••••••`.
   - `testCardMasking`: Card number masked to last 4 digits (`••••8912`).
6. `DeadlineDetectorTest`:
   - `testImminentExpiryMinutes`: Expiry in minutes scored 100.
   - `testDueToday`: Due today scored 90.
   - `testDueTomorrow`: Due tomorrow scored 70.
   - `testNoDeadline`: Absence of deadline scored 0.
