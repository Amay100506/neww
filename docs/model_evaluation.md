# Python classifier evaluation: development fixture only

The checked-in report was generated from a **synthetic** corpus created by `python/scripts/generate_dataset.py`. It contains 503 generated examples; the test partition has 101 examples. The report records 100% category accuracy and 100% OTP precision/recall on that split.

Those scores only show that the current model handles the generated examples. They do not establish production accuracy. Generated wording and category cues can be easier than real notifications, and similar templates can appear across the train/test partitions. Do not use the reported percentages as a public product claim or safety guarantee.

The Python classifier is not integrated into the Android app. The Android app currently uses deterministic Kotlin patterns and package hints. The Python data, model, analysis service, and Streamlit dashboard are offline development tools.

## Evidence required for a production claim

- A larger, independently sourced and privacy-reviewed corpus that reflects supported apps, languages, notification styles, and Android versions.
- Splits by source app, template, and time so near-duplicate examples cannot cross train/test boundaries.
- Per-class precision/recall, calibration, false-positive analysis, confidence intervals, and a separately held-out evaluation set.
- A documented consent, retention, anonymization, and deletion process for any user-derived evaluation data.
- Device-level latency and memory measurements for whichever inference path is actually shipped.
