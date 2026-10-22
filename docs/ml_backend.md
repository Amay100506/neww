# Offline Python ML utilities

The `python/` directory contains a development-only classifier and analysis tools. It is not a server backend, is not bundled into Android, and has no connection to PrioSort's notification listener. Android classification and priority scoring run locally in Kotlin.

The included model uses synthetic development data. It is useful for experimenting with feature extraction and evaluation scripts, but its metrics do not establish real-world accuracy or suitability for high-stakes decisions.

Install and run the optional offline tools from the repository root:

```shell
python -m pip install -r requirements-dev.txt
python -m pytest python/tests -q
python python/scripts/simulate_priority.py
streamlit run python/dashboard/app.py
```

Do not put real notification text or OTPs into the synthetic development utilities unless you have separately reviewed their input/output behavior and the local machine's data handling.
