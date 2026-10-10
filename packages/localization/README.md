# AWERO localization

The six locale JSON dictionaries in this package are the single maintained translation source for English (`en`), Russian (`ru`), Brazilian Portuguese (`pt-BR`), French (`fr`), German (`de`), and Spanish (`es`). Each locale has the same canonical key set. `native-key-map.json` maps Android resource identifiers to canonical keys; iOS keys use the canonical identifiers directly.

Generate Android XML and iOS `.strings` resources with:

```sh
python3 scripts/localization/generate-native-locales.py
```

CI runs the same generator in `--check` mode and fails if either platform's checked-in resources are stale, a locale has missing/extra keys, or a JSON file contains duplicate keys. Edit translations in the locale JSON files, then regenerate native resources. Do not edit generated native strings directly.
