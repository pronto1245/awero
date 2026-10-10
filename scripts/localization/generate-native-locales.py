#!/usr/bin/env python3
"""Generate Android and iOS string resources from packages/localization/*.json."""

import argparse
import json
import pathlib
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
LOCALES = {
    "en": ("values", "en"),
    "ru": ("values-ru", "ru"),
    "pt-BR": ("values-pt-rBR", "pt-BR"),
    "fr": ("values-fr", "fr"),
    "de": ("values-de", "de"),
    "es": ("values-es", "es"),
}
MAP = json.loads((ROOT / "packages/localization/native-key-map.json").read_text())


def load_source(locale):
    path = ROOT / "packages/localization" / f"{locale}.json"

    def reject_duplicates(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f"duplicate translation key {key!r} in {path}")
            result[key] = value
        return result

    data = json.loads(path.read_text(), object_pairs_hook=reject_duplicates)
    if not data or any(not isinstance(key, str) or not isinstance(value, str) for key, value in data.items()):
        raise ValueError(f"{path} must contain a non-empty flat string dictionary")
    return data


def render_ios(data):
    ios_strings = ((key, value) for key, value in data.items() if not key.startswith("android."))
    return "".join(f"{json.dumps(key, ensure_ascii=False)} = {json.dumps(value, ensure_ascii=False)};\n" for key, value in sorted(ios_strings))


def render_android(data):
    resources = ET.Element("resources")
    for resource_id, key in sorted(MAP["android"].items()):
        if key not in data:
            raise ValueError(f"missing Android translation source key {key!r} (resource {resource_id})")
        value = data[key].replace("%@", "%s")
        item = ET.SubElement(resources, "string", {"name": resource_id})
        item.text = value
    return ET.tostring(resources, encoding="unicode", xml_declaration=False) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if generated native files are stale")
    args = parser.parse_args()
    loaded = {locale: load_source(locale) for locale in LOCALES}
    expected = set(loaded["en"])
    for locale, data in loaded.items():
        if set(data) != expected:
            raise ValueError(f"{locale} has different keys: missing={sorted(expected-set(data))}; extra={sorted(set(data)-expected)}")

    failures = []
    for locale, (android_dir, ios_dir) in LOCALES.items():
        data = loaded[locale]
        outputs = {
            ROOT / "apps/android/app/src/main/res" / android_dir / "strings.xml": render_android(data),
            ROOT / "apps/ios/AWERO" / f"{ios_dir}.lproj/Localizable.strings": render_ios(data),
        }
        for path, rendered in outputs.items():
            current = path.read_text() if path.exists() else None
            if current != rendered:
                if args.check:
                    failures.append(str(path.relative_to(ROOT)))
                else:
                    path.write_text(rendered)
    if failures:
        print("Generated locale resources are stale:", *failures, sep="\n- ", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, json.JSONDecodeError) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1)
