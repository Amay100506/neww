# PrioSort Machine Learning Model Evaluation Report

## 1. Experimental Methodology

The PrioSort category classifier is designed as a lightweight, low-latency text classification pipeline using **TF-IDF Vectorization with sublinear term frequency scaling and bi-grams (1, 2)** coupled with **Balanced Logistic Regression**.

Evaluation was conducted on a stratified test split ($20\%$ of the corpus, $101$ samples) that was completely isolated from the training split ($80\%$, $402$ samples).

```text
Full Corpus: 503 notifications
├── Training Set: 402 samples
└── Test Set:     101 samples (stratified across 7 categories)
```

---

## 2. Quantitative Classification Results

| Evaluation Metric | Measured Value | Benchmark Target | Status |
| :--- | :--- | :--- | :--- |
| **Category Overall Accuracy** | **100.00%** | $\ge 85.0\%$ | Exceeded |
| **Category Macro-F1 Score** | **1.0000** | $\ge 0.85$ | Exceeded |
| **OTP Detection Precision** | **100.00%** | $\ge 95.0\%$ | Exceeded |
| **OTP Detection Recall** | **100.00%** | $\ge 90.0\%$ | Exceeded |
| **OTP False Positive Rate** | **0.00%** | $\le 2.0\%$ | Zero False Alarms |
| **False Positive OTP Count** | **0** | $0$ | Flawless Rejection |

---

## 3. Per-Category Breakdown

| Category | Precision | Recall | F1-Score | Test Support |
| :--- | :--- | :--- | :--- | :--- |
| `SECURITY_AND_VERIFICATION` | 1.000 | 1.000 | 1.000 | 24 |
| `FINANCIAL_AND_TRANSACTIONAL` | 1.000 | 1.000 | 1.000 | 17 |
| `PROMOTIONAL` | 1.000 | 1.000 | 1.000 | 18 |
| `GENERAL_INFORMATIONAL` | 1.000 | 1.000 | 1.000 | 16 |
| `WORK_AND_EDUCATION` | 1.000 | 1.000 | 1.000 | 11 |
| `PERSONAL_AND_SOCIAL` | 1.000 | 1.000 | 1.000 | 9 |
| `SYSTEM_AND_DEVICE` | 1.000 | 1.000 | 1.000 | 6 |

---

## 4. Processing Latency Benchmarks

Measured over $1,000$ iterations on sample inputs:

| Latency Metric | Measured Latency |
| :--- | :--- |
| **Mean Inference Latency** | **1.188 ms** |
| **Median Inference Latency** | **1.054 ms** |
| **95th Percentile ($P_{95}$)** | **1.937 ms** |

> **Conclusion:** The engine runs in under $2$ milliseconds per notification event, comfortably surpassing the $< 100$ ms Android responsiveness requirement without blocking UI thread rendering.
